package ru.discoverivan.scheduler;

/**
 * Интерфейс для создания пользовательского обработчика событий
 */
public interface ScheduledTaskExecutor<K, V> {
    void execute(K key, V value);

    /**
     * Используется для соотношения типа вычитанной задачи с java классом ключа и парного к нему значения.
     * Другими словами, соотношение выглядит следующим образом: {@code taskType: taskKey & taskValue}
     *
     * @return тип задачи
     */
    String getWaitingTaskType();

    /**
     * Используется для десериализации ключа.
     *
     * @return java класс ключа
     */
    Class<K> getTaskKeyClass();

    /**
     * Используется для десериализации данных.
     *
     * @return java класс данных
     */
    Class<V> getTaskValueClass();

    // TODO: возможно еще нужно запрашивать еще и deserializer для ключа и значения
}
