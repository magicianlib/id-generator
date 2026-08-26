package cn.ituknown.generator.result;

import lombok.Getter;

/**
 * 偏移量分页元数据
 * <p>
 * {@code pages}（总页数）由 {@code total} 和 {@code pageSize} 自动计算得出，不需要调用方传入。
 * <p>
 * <b>跨进程序列化注意：</b>本类为不可变对象（仅全参构造、无无参构造、无 setter）。
 * 通过 Dubbo 远程调用传输时，依赖序列化框架对"无无参构造类"的底层反射重建能力
 * （Dubbo 默认的 Hessian2 可正常处理）；若改用其他序列化协议，需确认其对此类
 * 不可变对象的支持。
 *
 * @author magicianlib@gmail.com
 */
@Getter
public class Pagination {

    /**
     * 总条数
     */
    private final long total;
    /**
     * 每页数量
     */
    private final int pageSize;
    /**
     * 当前页码（从 1 开始）
     */
    private final int current;
    /**
     * 总页数（自动计算）
     */
    private final int pages;

    /**
     * @param total    总条数
     * @param pageSize 每页数量
     * @param current  当前页码（从 1 开始）
     */
    public Pagination(long total, int pageSize, int current) {
        this.total = total;
        this.pageSize = pageSize;
        this.current = current;
        this.pages = pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
    }
}