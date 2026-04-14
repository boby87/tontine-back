package cm.ftg.tontine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableRetry
public class AsyncConfig {

    @Bean
    public ExecutorService notificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
