package ru.discoverivan.scheduler.kafka.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * DTO отложенного задания для тестов
 */
@Getter
@Setter
@Builder
@Jacksonized
public class TestTask {
    private LocalDateTime executionTime;
    private String data;
}
