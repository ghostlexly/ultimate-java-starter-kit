package com.lunisoft.javastarter.module.demo.usecase.enqueuejob;

import java.time.Duration;

import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DemoJobRunrEnqueueJob {

  private static final Logger log = LoggerFactory.getLogger(DemoJobRunrEnqueueJob.class);

  @Job(name = "demo-job", retries = 3)
  public void execute(String example) throws InterruptedException {
    log.info("Job started with example: {} !", example);

    Thread.sleep(Duration.ofSeconds(5).toMillis());

    log.info("Job completed !");
  }
}
