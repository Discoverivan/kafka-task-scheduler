package ru.discoverivan.scheduler;

import lombok.Builder;
import lombok.Getter;

/**
 * Описание объекта откладываемого задания
 */
@Getter
@Builder
public class SchedulingTask<K, V> {
    private final String type;
    private final K key;
    private final V value;
}
