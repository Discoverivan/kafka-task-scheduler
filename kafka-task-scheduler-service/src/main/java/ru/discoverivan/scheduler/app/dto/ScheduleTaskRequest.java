package ru.discoverivan.scheduler.app.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class ScheduleTaskRequest {
    private String data;
    private Delay delay;

    @Getter
    @Setter
    public static class Delay {
        private int interval;
        private TimeUnit timeUnit;
    }
}
