package ru.discoverivan.scheduler.kafka.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.discoverivan.scheduler.ScheduledTaskExecutor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Тестовый TaskExecutor, который сохраняет полученные задания
 */
@Slf4j
@Getter
@Component
public class TestScheduledTaskExecutor implements ScheduledTaskExecutor<String, TestTask> {
    private final Map<String, TestTask> handledTasks = new HashMap<>();

    @Override
    public void execute(String key, TestTask value) {
        log.info("На исполнение получена задача с ключем '{}'", key);
        value.setExecutionTime(LocalDateTime.now());
        handledTasks.put(key, value);
    }

    @Override
    public String getWaitingTaskType() {
        return "SchedulerTest";
    }

    @Override
    public Class<String> getTaskKeyClass() {
        return String.class;
    }

    @Override
    public Class<TestTask> getTaskValueClass() {
        return TestTask.class;
    }
}
