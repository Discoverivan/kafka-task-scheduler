package ru.discoverivan.scheduler.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.discoverivan.scheduler.SchedulingTask;
import ru.discoverivan.scheduler.TaskSchedulerService;
import ru.discoverivan.scheduler.app.dto.ScheduleTaskRequest;
import ru.discoverivan.scheduler.app.dto.ScheduleTaskResponse;
import ru.discoverivan.scheduler.app.scheduler.SchedulerAppTask;

import java.util.UUID;

/**
 * Сервис, выполняющий задачи связанные с планированием и исполнением задач
 */
@Component
@RequiredArgsConstructor
public class SchedulerService {
    private final TaskSchedulerService taskSchedulerService;

    public ResponseEntity<ScheduleTaskResponse> schedule(ScheduleTaskRequest request) {
        String uuid = UUID.randomUUID().toString();

        SchedulingTask<String, SchedulerAppTask> schedulingTask = SchedulingTask.<String, SchedulerAppTask>builder()
                .type(SchedulerAppTask.class.getSimpleName())
                .key(uuid)
                .value(SchedulerAppTask.builder()
                        .data(request.getData())
                        .build())
                .build();
        try {
            taskSchedulerService.schedule(schedulingTask, request.getDelay().getInterval(), request.getDelay().getTimeUnit());
            return ResponseEntity.ok()
                    .body(ScheduleTaskResponse.builder()
                            .success(true)
                            .id(uuid)
                            .build()
                    );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ScheduleTaskResponse.builder()
                            .error(ScheduleTaskResponse.Error.builder()
                                    .code("SCHEDULING_ERROR")
                                    .description(e.getMessage())
                                    .build())
                            .build()
                    );
        }
    }
}
