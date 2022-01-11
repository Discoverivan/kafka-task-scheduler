package ru.discoverivan.scheduler.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;
import ru.discoverivan.scheduler.SchedulingTask;
import ru.discoverivan.scheduler.TaskSchedulerService;
import ru.discoverivan.scheduler.kafka.common.TestScheduledTaskExecutor;
import ru.discoverivan.scheduler.kafka.common.TestTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Тесты работоспособности {@link TaskSchedulerService}
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestApplication.class)
@SuppressWarnings("java:S2699")
class KafkaTaskSchedulerServiceTest {
    @Autowired
    TestScheduledTaskExecutor scheduledTaskExecutor;

    @Autowired
    private TaskSchedulerService taskSchedulerService;

    /**
     * Тест проверяет получение задания на исполнение и временную точность планировщика
     */
    @SneakyThrows
    @Test
    void testScheduleForInterval() {
        String taskKey = UUID.randomUUID().toString();
        taskSchedulerService.schedule(buildSchedulingTask(taskKey), 10, TimeUnit.SECONDS);
        LocalDateTime expectedExecutionTime = LocalDateTime.now().plusSeconds(10);

        TestTask scheduledTask = getScheduledTaskFromTaskExecutor(taskKey, Duration.of(15, ChronoUnit.SECONDS));

        Assert.notNull(scheduledTask, "Отложенное задание не было передано на исполнение в указанный срок");

        long executionTimeAccuracy = Duration.between(expectedExecutionTime, scheduledTask.getExecutionTime()).toMillis();
        Assert.isTrue(Math.abs(executionTimeAccuracy) < 3000, "Отложенное задание было передано на исполнение с недостаточной точностью");
    }

    private SchedulingTask<String, TestTask> buildSchedulingTask(String taskKey) {
        return SchedulingTask.<String, TestTask>builder()
                .type(scheduledTaskExecutor.getWaitingTaskType())
                .key(taskKey)
                .value(TestTask.builder()
                        .data("TestTaskData")
                        .build())
                .build();
    }

    private TestTask getScheduledTaskFromTaskExecutor(String taskKey, Duration timeout) throws InterruptedException {
        while (timeout.toSeconds() > 0) {
            TestTask scheduledTask = scheduledTaskExecutor.getHandledTasks().get(taskKey);
            if (scheduledTask != null)
                return scheduledTask;
            TimeUnit.SECONDS.sleep(1);
            timeout = timeout.minusSeconds(1);
        }
        return null;
    }
}