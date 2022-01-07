package ru.discoverivan.scheduler.app.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.discoverivan.scheduler.ScheduledTaskExecutor;

/**
 * TaskExecutor для задач типа {@link SchedulerAppTask}
 */
@Slf4j
@Component
public class SchedulerAppTaskExecutor implements ScheduledTaskExecutor<String, SchedulerAppTask> {
    @Override
    public void execute(String key, SchedulerAppTask value) {
        log.info("Вызван executor");
    }

    @Override
    public String getWaitingTaskType() {
        return SchedulerAppTask.class.getSimpleName();
    }

    @Override
    public Class<String> getTaskKeyClass() {
        return String.class;
    }

    @Override
    public Class<SchedulerAppTask> getTaskValueClass() {
        return SchedulerAppTask.class;
    }
}
