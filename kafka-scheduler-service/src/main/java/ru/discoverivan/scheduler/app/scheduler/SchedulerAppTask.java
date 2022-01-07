package ru.discoverivan.scheduler.app.scheduler;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

/**
 * DTO задачи, отправляемой в {@link ru.discoverivan.scheduler.TaskSchedulerService}
 */
@Getter
@Setter
@Builder
@Jacksonized
public class SchedulerAppTask {
    private String data;
}
