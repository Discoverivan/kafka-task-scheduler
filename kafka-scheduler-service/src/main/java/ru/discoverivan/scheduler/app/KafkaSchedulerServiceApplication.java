package ru.discoverivan.scheduler.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaSchedulerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaSchedulerServiceApplication.class, args);
    }
}
