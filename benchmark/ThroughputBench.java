import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * 吞吐压测工具: 多线程对取号接口发起长连接压测, 统计成功/失败/批量不足额/吞吐/平均延迟
 * 客户端开销已尽量压缩(连接复用、响应轻量解析), 用于逼近服务端真实容量
 *
 * 参数: path body threads seconds [batchSize]
 *   path       接口路径, 如 /api/common/takeSegment 或 /api/common/takeSegment/10
 *   body       请求体, 如 {"bizGroup":"commons","bizTag":"benchmark"}
 *   threads    并发线程数
 *   seconds    持续时长(秒)
 *   batchSize  可选; 提供时统计批量响应不足额(返回数量不足 batchSize)的请求数
 *
 * 运行示例:
 *   javac ThroughputBench.java
 *   java -Dhttp.maxConnections=256 ThroughputBench /api/common/takeSegment/10 '{"bizGroup":"commons","bizTag":"benchmark"}' 64 30 10
 *
 * 注意: 必须加 -Dhttp.maxConnections, JDK 长连接池默认每主机仅 5 条连接, 不加会把吞吐压在假瓶颈上
 */
public class ThroughputBench {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        String body = args[1];
        int threads = Integer.parseInt(args[2]);
        long seconds = Long.parseLong(args[3]);
        int batchSize = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        byte[] payload = body.getBytes("UTF-8");
        URL url = new URL("http://localhost:8080" + path);
        AtomicLong ok = new AtomicLong(), fail = new AtomicLong(), shortBatch = new AtomicLong();
        LongAdder totalLatency = new LongAdder();
        long deadline = System.currentTimeMillis() + seconds * 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                byte[] buf = new byte[4096];
                while (System.currentTimeMillis() < deadline) {
                    long t0 = System.nanoTime();
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
                            ok.incrementAndGet();
                            if (batchSize > 0) {
                                countShortBatch(resp.toString("UTF-8"), batchSize, shortBatch);
                            }
                        } else {
                            fail.incrementAndGet();
                        }
                    } catch (Exception e) {
                        fail.incrementAndGet();
                    }
                    totalLatency.add(System.nanoTime() - t0);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(seconds + 60, TimeUnit.SECONDS);
        long done = ok.get() + fail.get();
        System.out.printf("path=%s threads=%d ok=%d fail=%d shortBatch=%d qps=%.0f meanLatencyMs=%.2f%n",
                path, threads, ok.get(), fail.get(), shortBatch.get(), done / (double) seconds,
                totalLatency.sum() / 1e6 / Math.max(done, 1));
    }

    /**
     * 统计批量响应不足额: 按业务数据里的逗号数判断返回数量是否达到期望值
     */
    private static void countShortBatch(String resp, int batchSize, AtomicLong shortBatch) {
        int start = resp.indexOf("\"data\":[");
        if (start < 0) return;
        int end = resp.indexOf(']', start);
        int commas = 0;
        for (int k = start; k < end; k++) {
            if (resp.charAt(k) == ',') commas++;
        }
        if (commas != batchSize - 1) shortBatch.incrementAndGet();
    }
}
