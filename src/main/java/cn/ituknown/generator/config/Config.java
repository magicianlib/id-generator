package cn.ituknown.generator.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class Config {

    /**
     * 启用物理分页拦截, 数据库类型由数据源连接串自动识别
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setMaxPoolSize(Integer.MAX_VALUE);
        taskExecutor.setQueueCapacity(Integer.MAX_VALUE);
        taskExecutor.setKeepAliveSeconds(60);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        taskExecutor.setThreadNamePrefix("task-");
        taskExecutor.initialize();
        return taskExecutor;
    }
}