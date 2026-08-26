package cn.ituknown.generator.request;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 请求基类，提供通用的请求追踪字段。
 */
@Getter
@Setter
public abstract class AbstractRequest implements Serializable {
    private static final long serialVersionUID = -2587342987834431391L;

    /**
     * 客户端应用ID
     */
    private String appId;

    /**
     * 请求ID
     */
    private String requestId;
}