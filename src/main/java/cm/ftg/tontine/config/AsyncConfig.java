package cm.ftg.tontine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableRetry
@EnableScheduling
public class AsyncConfig implements SchedulingConfigurer {

    @Bean
    public ExecutorService notificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Configure le scheduler pour utiliser des Virtual Threads
     * au lieu du pool par défaut à un seul thread.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(
            0, Thread.ofVirtual().name("scheduler-vt-", 0).factory()
        ));
    }
}
