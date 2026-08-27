import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * 正确性校验压测工具: 收集每个响应实际发出的号, 结束后全量校验首号衔接、无重复、连续无跳号
 *
 * 参数: path body threads seconds expectFirst
 *   path        接口路径, 单号与批量接口均可
 *   body        请求体
 *   threads     并发线程数
 *   seconds     持续时长(秒)
 *   expectFirst 期望首号, 取压测开始前库内该序列的已分配最大值加一; 连续多轮时取上一轮实际末号加一
 *
 * 运行示例:
 *   javac StrictBench.java
 *   java -Dhttp.maxConnections=256 StrictBench /api/common/take-segment '{"bizGroup":"commons","bizTag":"benchmark"}' 32 15 2420001
 *
 * 注意: 必须加 -Dhttp.maxConnections, 理由同吞吐压测工具
 */
public class StrictBench {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        String body = args[1];
        int threads = Integer.parseInt(args[2]);
        long seconds = Long.parseLong(args[3]);
        long expectFirst = Long.parseLong(args[4]);
        byte[] payload = body.getBytes("UTF-8");
        URL url = new URL("http://localhost:8080" + path);
        AtomicLong ok = new AtomicLong(), fail = new AtomicLong(), shortBatch = new AtomicLong();
        List<Long> issued = Collections.synchronizedList(new ArrayList<>());
        String tail = path.substring(path.lastIndexOf('/') + 1);
        int expectCount = tail.matches("\\d+") ? Integer.parseInt(tail) : 1;
        long deadline = System.currentTimeMillis() + seconds * 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                byte[] buf = new byte[4096];
                while (System.currentTimeMillis() < deadline) {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput(true);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        try (OutputStream os = conn.getOutputStream()) { os.write(payload); }
                        int code = conn.getResponseCode();
                        ByteArrayOutputStream resp = new ByteArrayOutputStream(256);
                        try (InputStream is = conn.getInputStream()) {
                            int n;
                            while ((n = is.read(buf)) > 0) resp.write(buf, 0, n);
                        }
                        // 不调 disconnect, 让连接回到复用池
                        if (code == 200) {
                            String text = resp.toString("UTF-8");
                            if (text.contains("\"code\":0")) {
                                ok.incrementAndGet();
                                List<Long> ids = extractIds(text);
                                if (ids.size() != expectCount) shortBatch.incrementAndGet();
                                issued.addAll(ids);
                            } else {
                                fail.incrementAndGet();
                            }
                        } else {
                            fail.incrementAndGet();
                        }
                    } catch (Exception e) {
                        fail.incrementAndGet();
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(seconds + 60, TimeUnit.SECONDS);

        // 全量校验: 首号衔接、无重复、连续无跳号
        int total = issued.size();
        Set<Long> distinct = new HashSet<>(issued);
        List<Long> sorted = new ArrayList<>(issued);
        Collections.sort(sorted);
        long first = sorted.get(0), last = sorted.get(total - 1);
        boolean firstOk = first == expectFirst;
        boolean dupOk = distinct.size() == total;
        long gaps = 0, firstGapAfter = -1;
        for (int i = 1; i < total; i++) {
            if (sorted.get(i) != sorted.get(i - 1) + 1) {
                if (gaps == 0) firstGapAfter = sorted.get(i - 1);
                gaps += sorted.get(i) - sorted.get(i - 1) - 1;
            }
        }
        System.out.printf("path=%s threads=%d total=%d ok=%d fail=%d shortBatch=%d%n",
                path, threads, total, ok.get(), fail.get(), shortBatch.get());
        System.out.printf("first=%d (expect %d, %s) last=%d dup=%s gaps=%d%s%n",
                first, expectFirst, firstOk ? "OK" : "MISMATCH", last,
                dupOk ? "none OK" : "DUP FOUND", gaps,
                gaps > 0 ? " firstGapAfter=" + firstGapAfter : "");
        System.out.println(firstOk && dupOk && gaps == 0 && fail.get() == 0
                ? "VERDICT: PASS (首号衔接/无重复/连续无跳号 全部通过)" : "VERDICT: FAIL");
    }

    /**
     * 从响应体提取业务数据里的全部号: 兼容单号与数组两种形态
     */
    static List<Long> extractIds(String resp) {
        int start = resp.indexOf("\"data\":") + 7;
        List<Long> ids = new ArrayList<>();
        long v = 0;
        boolean inNum = false;
        for (int i = start; i < resp.length(); i++) {
            char c = resp.charAt(i);
            if (c >= '0' && c <= '9') {
                v = v * 10 + (c - '0');
                inNum = true;
            } else if (inNum) {
                ids.add(v);
                v = 0;
                inNum = false;
                if (c == ']') break;
            } else if (c == ']') {
                break;
            }
        }
        return ids;
    }
}
