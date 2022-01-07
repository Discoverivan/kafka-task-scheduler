package ru.discoverivan.scheduler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Сервис планировщика задач. Выполняет функцию регистрации задач, требующих отложенного исполнения.
 */
public interface TaskSchedulerService {
    <K, V> void schedule(SchedulingTask<K, V> task, long delayInterval, TimeUnit delayTimeUnit);

    <K, V> void schedule(SchedulingTask<K, V> task, LocalDateTime executionTime);
}
