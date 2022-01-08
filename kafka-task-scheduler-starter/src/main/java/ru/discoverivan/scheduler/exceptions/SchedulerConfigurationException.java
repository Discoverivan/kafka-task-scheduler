package ru.discoverivan.scheduler.exceptions;

/**
 * Exception возникающий в случае некорректной конфигурации планировщика
 */
public class SchedulerConfigurationException extends RuntimeException {
    public SchedulerConfigurationException(String message) {
        super(message);
    }

    public SchedulerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
