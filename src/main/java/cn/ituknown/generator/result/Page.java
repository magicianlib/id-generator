package cn.ituknown.generator.result;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 偏移量分页数据封装
 * <p>
 * JSON 结构：
 * <pre>{@code
 * {
 *   "list": [],
 *   "pagination": {
 *     "total": 100,
 *     "pageSize": 10,
 *     "current": 1,
 *     "pages": 10
 *   }
 * }
 * }</pre>
 * <p>
 * {@code list} 为 null 时自动转为空集合；{@code pagination} 不允许为 null。
 * <p>
 * <b>跨进程序列化注意：</b>本类为 Java 记录类型，且其列表在某些场景下为不可变集合。
 * 通过 Dubbo 远程调用传输（作为方法参数或返回值）时，提供端与消费端均需使用
 * Apache Dubbo 3.3.0 及以上版本；自该版本起内置的 Hessian2 序列化才支持记录类型
 * 与不可变集合的序列化往返，更低版本会在消费端反序列化失败（提示无法实例化）。
 *
 * @param <T> 数据元素类型
 * @author magicianlib@gmail.com
 */
@Getter
public class Page<T> {
    private final List<T> list;
    private final Pagination pagination;

    /**
     * 原始构造器
     *
     * @param list       当前页数据，为 null 时自动转为空集合
     * @param pagination 分页元数据，不允许为 null
     * @throws NullPointerException pagination 为 null 时
     */
    public Page(List<T> list, Pagination pagination) {
        this.list = list != null ? list : Collections.emptyList();
        this.pagination = Objects.requireNonNull(pagination);
    }

    /**
     * 便捷构造器，内部创建 {@link Pagination}
     *
     * @param list     当前页数据，为 null 时自动转为空集合
     * @param current  当前页码（从 1 开始）
     * @param pageSize 每页数量
     * @param total    总条数
     */
    public static <T> Page<T> of(List<T> list, int current, int pageSize, long total) {
        return new Page<>(list != null ? list : Collections.emptyList(), new Pagination(total, pageSize, current));
    }

}