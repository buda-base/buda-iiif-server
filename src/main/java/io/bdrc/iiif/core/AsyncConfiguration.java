package io.bdrc.iiif.core;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AsyncConfiguration implements WebMvcConfigurer {

  @Value("${iiifserv.async.worker-count:8}")
  private int workerCount;

  @Value("${iiifserv.async.queue-capacity:32}")
  private int queueCapacity;

  @Value("${iiifserv.async.priority-worker-count:4}")
  private int priorityWorkerCount;

  @Value("${iiifserv.async.priority-queue-capacity:16}")
  private int priorityQueueCapacity;

  @Bean("publicMvcTaskExecutor")
  public ThreadPoolTaskExecutor publicMvcTaskExecutor() {
    return taskExecutor(workerCount, queueCapacity, "mvcPublic-");
  }

  @Bean("priorityMvcTaskExecutor")
  public ThreadPoolTaskExecutor priorityMvcTaskExecutor() {
    return taskExecutor(priorityWorkerCount, priorityQueueCapacity, "mvcPriority-");
  }

  @Bean
  public AsyncTaskExecutor mvcTaskExecutor() {
    return new PriorityAsyncTaskExecutor(publicMvcTaskExecutor(), priorityMvcTaskExecutor());
  }

  private static ThreadPoolTaskExecutor taskExecutor(int workers, int queueSize, String threadNamePrefix) {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(workers);
    taskExecutor.setMaxPoolSize(workers);
    taskExecutor.setQueueCapacity(queueSize);
    taskExecutor.setThreadNamePrefix(threadNamePrefix);
    taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return taskExecutor;
  }

  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setTaskExecutor(mvcTaskExecutor());
  }

}