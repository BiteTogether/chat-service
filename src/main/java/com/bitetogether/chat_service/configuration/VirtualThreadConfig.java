package com.bitetogether.chat_service.configuration;

import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Configuration
public class VirtualThreadConfig {

  @Bean(name = "virtualThreadScheduler", destroyMethod = "dispose")
  public Scheduler virtualThreadScheduler() {
    log.info("Initializing Virtual Thread Scheduler for reactive operations");

    return Schedulers.fromExecutorService(
        Executors.newVirtualThreadPerTaskExecutor(), "virtual-thread-scheduler");
  }
}
