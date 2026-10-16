package io.bdrc.iiif.core;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AsyncConfiguration implements WebMvcConfigurer {

  @Value("${iiifserv.async.worker-count:8}")
  private int workerCount;

  @Value("${iiifserv.async.queue-capacity:32}")
  private int queueCapacity;

  @Bean
  public ThreadPoolTaskExecutor mvcTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(workerCount);
    taskExecutor.setMaxPoolSize(workerCount);
    taskExecutor.setQueueCapacity(queueCapacity);
    taskExecutor.setThreadNamePrefix("mvcTaskExecutor-");
    taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    return taskExecutor;
  }

  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setTaskExecutor(mvcTaskExecutor());
  }

}