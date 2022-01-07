package ru.discoverivan.scheduler.app.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ScheduleTaskResponse {
    private boolean success;
    private String id;
    private Error error;

    @Getter
    @Setter
    @Builder
    public static class Error {
        private String code;
        private String description;
    }
}
