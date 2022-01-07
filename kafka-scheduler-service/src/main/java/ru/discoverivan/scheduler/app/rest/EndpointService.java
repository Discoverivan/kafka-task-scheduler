package ru.discoverivan.scheduler.app.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.discoverivan.scheduler.app.dto.ScheduleTaskRequest;
import ru.discoverivan.scheduler.app.dto.ScheduleTaskResponse;
import ru.discoverivan.scheduler.app.service.SchedulerService;

/**
 * Endpoint-ы приложения
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class EndpointService {
    private final SchedulerService schedulerService;

    @PostMapping("/schedule")
    public ResponseEntity<ScheduleTaskResponse> schedule(@RequestBody ScheduleTaskRequest request){
        return schedulerService.schedule(request);
    }
}
