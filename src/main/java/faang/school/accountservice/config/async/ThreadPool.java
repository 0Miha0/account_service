package faang.school.accountservice.config.async;

import faang.school.accountservice.properties.CreateAccountThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
public class ThreadPool {

    private final CreateAccountThreadPoolProperties accountPoolProp;

    @Bean(name = "createAccountThreadPool")
    public Executor createAccountThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(accountPoolProp.getCorePoolSize());
        executor.setMaxPoolSize(accountPoolProp.getMaxPoolSize());
        executor.setQueueCapacity(accountPoolProp.getQueueCapacity());
        executor.setThreadNamePrefix(accountPoolProp.getNamePrefix());
        executor.initialize();
        return executor;
    }
}