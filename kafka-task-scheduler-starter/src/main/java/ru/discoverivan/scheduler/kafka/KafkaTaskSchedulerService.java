package ru.discoverivan.scheduler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.concurrent.ListenableFutureCallback;
import ru.discoverivan.scheduler.ScheduledTaskExecutor;
import ru.discoverivan.scheduler.SchedulingTask;
import ru.discoverivan.scheduler.TaskSchedulerService;
import ru.discoverivan.scheduler.kafka.config.KafkaTaskSchedulerProperties;
import ru.discoverivan.scheduler.kafka.config.KafkaTaskSchedulerProperties.TopicProperties;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Сервис планировщика задач использующий Kafka
 */
@SuppressWarnings("NullableProblems")
@Slf4j
public class KafkaTaskSchedulerService implements TaskSchedulerService {
    private static final String SCHEDULER_PREFIX = "discoverivan.task-scheduler";
    private static final long POLLING_INTERVAL_MAX_DELTA = TimeUnit.SECONDS.toMillis(10);

    private static final ObjectMapper JACKSON_OBJECT_MAPPER;

    static {
        JACKSON_OBJECT_MAPPER = new ObjectMapper();
        JACKSON_OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    private final KafkaTaskSchedulerProperties schedulerProperties;
    private final GenericApplicationContext applicationContext;
    private final Map<String, ScheduledTaskExecutor<?, ?>> scheduledTaskExecutors;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTemplate<String, String> kafkaTransactionalTemplate;
    private final KafkaTransactionManager<String, String> kafkaTransactionManager;

    @SuppressWarnings("java:S1214")
    private interface HeaderName {
        String TASK_TYPE = "TaskType";
        String TASK_EXECUTION_TIME = "TaskExecutionTime";
    }

    public KafkaTaskSchedulerService(KafkaTaskSchedulerProperties schedulerProperties,
                                     GenericApplicationContext applicationContext,
                                     List<ScheduledTaskExecutor<?, ?>> scheduledTaskExecutors,
                                     ConsumerFactory<String, String> kafkaConsumerFactory,
                                     ProducerFactory<String, String> kafkaProducerFactory,
                                     ProducerFactory<String, String> kafkaTransactionalProducerFactory) {
        this.schedulerProperties = schedulerProperties;
        this.applicationContext = applicationContext;
        this.consumerFactory = kafkaConsumerFactory;
        this.kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
        this.kafkaTransactionalTemplate = new KafkaTemplate<>(kafkaTransactionalProducerFactory);
        this.kafkaTransactionManager = new KafkaTransactionManager<>(kafkaTransactionalProducerFactory);

        this.scheduledTaskExecutors = scheduledTaskExecutors.stream()
                .collect(Collectors.toMap(ScheduledTaskExecutor::getWaitingTaskType, taskExecutor -> taskExecutor));
    }


    @PostConstruct
    private void runKafkaScheduler() {
        TopicProperties prevKafkaTopic = null;
        for (TopicProperties kafkaTopic : schedulerProperties.getTopics()) {
            ContainerProperties containerProperties = getContainerProperties(prevKafkaTopic, kafkaTopic);
            applicationContext.registerBean("kafkaSchedulerListener_" + kafkaTopic.getName(),
                    ConcurrentMessageListenerContainer.class,
                    () -> {
                        ConcurrentMessageListenerContainer<String, String> container =
                                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
                        container.setConcurrency(kafkaTopic.getConcurrency());
                        return container;
                    });
            log.info("Зарегистрирован listener для kafka topic '{}' с интервалом обновления {} мс.",
                    kafkaTopic.getName(), containerProperties.getIdleBetweenPolls());
            prevKafkaTopic = kafkaTopic;
        }
    }

    private ContainerProperties getContainerProperties(TopicProperties prevKafkaTopic, TopicProperties currentKafkaTopic) {
        ContainerProperties containerProperties = new ContainerProperties(currentKafkaTopic.getName());
        containerProperties.setGroupId(SCHEDULER_PREFIX + "_" + currentKafkaTopic.getName());
        containerProperties.setTransactionManager(kafkaTransactionManager);
        if (currentKafkaTopic.getDelay().toMillis() > 0) {
            long topicPollingInternal = currentKafkaTopic.getDelay().toMillis() - prevKafkaTopic.getDelay().toMillis();
            containerProperties.setIdleBetweenPolls(topicPollingInternal);
            containerProperties.setKafkaConsumerProperties(getKafkaConsumerTopicSpecialProperties(topicPollingInternal));
            containerProperties.setMessageListener((MessageListener<String, String>) this::moveTaskToMostSuitableTopic);
        } else {
            containerProperties.setMessageListener((MessageListener<String, String>) this::executeTask);
        }
        return containerProperties;
    }

    private Properties getKafkaConsumerTopicSpecialProperties(long topicPollingInternal) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, Long.toString(topicPollingInternal + POLLING_INTERVAL_MAX_DELTA));
        return properties;
    }

    @Override
    public <K, V> void schedule(SchedulingTask<K, V> task, long delayInterval, TimeUnit delayTimeUnit) {
        schedule(task, LocalDateTime.now().plus(delayInterval, delayTimeUnit.toChronoUnit()));
    }

    @Override
    public <K, V> void schedule(SchedulingTask<K, V> task, LocalDateTime executionTime) {
        try {
            String topic = getMostSuitableTopic(executionTime);
            ProducerRecord<String, String> producerRecord = buildKafkaProducerRecord(task, executionTime, topic);
            kafkaTemplate.send(producerRecord).addCallback(buildScheduleTaskLoggingCallback(task, topic));
            log.info("Асинхронная отправка задачи '{}' в kafka topic '{}'.", task.getKey(), topic);
        } catch (JsonProcessingException cause) {
            log.error("Произошла ошибка в процессе сериализации данных в рамках задачи '{}'.", task.getKey());
            throw new IllegalArgumentException("Произошла ошибка в процессе сериализации данных.", cause);
        }
    }

    private <K, V> ListenableFutureCallback<? super SendResult<String, String>> buildScheduleTaskLoggingCallback(SchedulingTask<K, V> task, String topic) {
        return new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable cause) {
                log.error(String.format("Произошла ошибка при попытке размещения задачи '%s' в kafka topic '%s'.",
                        task.getKey(), topic), cause);
            }

            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("Задача '{}' успешно размещена в kafka topic '{}'.", task.getKey(), topic);
            }
        };
    }

    private <K, V> ProducerRecord<String, String> buildKafkaProducerRecord(SchedulingTask<K, V> task, LocalDateTime executionTime, String topic) throws JsonProcessingException {
        String jsonKey = JACKSON_OBJECT_MAPPER.writeValueAsString(task.getKey());
        String jsonValue = JACKSON_OBJECT_MAPPER.writeValueAsString(task.getValue());
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, jsonKey, jsonValue);
        producerRecord.headers().add(HeaderName.TASK_TYPE, JACKSON_OBJECT_MAPPER.writeValueAsString(task.getType()).getBytes(UTF_8));
        producerRecord.headers().add(HeaderName.TASK_EXECUTION_TIME, JACKSON_OBJECT_MAPPER.writeValueAsString(executionTime).getBytes(UTF_8));
        return producerRecord;
    }

    @SuppressWarnings({"ConstantConditions", "java:S2259"})
    private String getMostSuitableTopic(LocalDateTime executionTime) {
        long millisUntilExecutionTime = LocalDateTime.now().until(executionTime, ChronoUnit.MILLIS);
        if (millisUntilExecutionTime <= 0) return schedulerProperties.getTopics().get(0).getName();

        long minTimeDelta = Long.MAX_VALUE;
        TopicProperties mostSuitableTopic = null;
        for (TopicProperties schedulerTopic : schedulerProperties.getTopics()) {
            long timeDelta = Math.abs(schedulerTopic.getDelay().toMillis() - millisUntilExecutionTime);
            if (timeDelta < minTimeDelta) {
                minTimeDelta = timeDelta;
                mostSuitableTopic = schedulerTopic;
            }
        }
        return mostSuitableTopic.getName();
    }

    private void moveTaskToMostSuitableTopic(ConsumerRecord<String, String> consumerRecord) {
        log.info("Старт процесса перемещения задачи '{}', вычитанной из kafka topic '{}'.", consumerRecord.key(), consumerRecord.topic());

        LocalDateTime executionTime = getKafkaHeaderValue(consumerRecord.headers(), HeaderName.TASK_EXECUTION_TIME, LocalDateTime.class);
        String outboundTopic = getMostSuitableTopic(executionTime);
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(outboundTopic, consumerRecord.key(), consumerRecord.value());
        for (Header header : consumerRecord.headers()) {
            producerRecord.headers().add(header);
        }

        log.info("Отправка задачи '{}' в kafka topic '{}'.", consumerRecord.key(), outboundTopic);
        kafkaTransactionalTemplate.send(producerRecord).addCallback(buildMoveTaskLoggingCallback(consumerRecord, outboundTopic));

        log.info("Завершение транзакции в рамках задачи '{}'.\nВ рамках транзакции производилось вычитывание задачи " +
                "из kafka topic '{}' и отправка её в kafka topic '{}').", consumerRecord.key(), consumerRecord.topic(), outboundTopic);
    }

    @SuppressWarnings("unchecked")
    private <T> T getKafkaHeaderValue(Headers recordHeaders, String headerName, Class<?> headerClass) {
        Iterator<Header> headerIterator = recordHeaders.headers(headerName).iterator();
        if (!headerIterator.hasNext()) {
            throw new IllegalArgumentException("Вычитанную задачу не удалось обработать по причине отсутствия у неё " +
                    "заголовка '" + headerName + "' с помощью которого идентифицируется тип задачи, " +
                    "что необходимо для десериализации ключа и значения запланированной задачи.");
        }

        try {
            return (T) JACKSON_OBJECT_MAPPER.readValue(headerIterator.next().value(), headerClass);
        } catch (IOException cause) {
            log.error("Произошла ошибка в процессе десериализации заголовка '{}'.", headerName);
            throw new IllegalArgumentException("Произошла ошибка в процессе десериализации заголовка '" + headerName + "'.", cause);
        }
    }

    private ListenableFutureCallback<SendResult<String, String>> buildMoveTaskLoggingCallback(ConsumerRecord<String, String> consumerRecord, String outboundTopic) {
        return new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable cause) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Произошла ошибка при попытке перемещения задачи '%s' из kafka topic '%s' в kafka topic '%s'.",
                            consumerRecord.key(), consumerRecord.topic(), outboundTopic), cause);
                }
            }

            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("Задача '{}' успешно размещена в kafka topic '{}'.", consumerRecord.key(), outboundTopic);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <K, V> void executeTask(ConsumerRecord<String, String> consumerRecord) {
        log.info("Исполнение задачи '{}', вычитанной из kafka topic '{}'.", consumerRecord.key(), consumerRecord.topic());
        String taskType = getKafkaHeaderValue(consumerRecord.headers(), HeaderName.TASK_TYPE, String.class);
        ScheduledTaskExecutor<K, V> scheduledTaskExecutor = (ScheduledTaskExecutor<K, V>) scheduledTaskExecutors.get(taskType);

        K key = deserializeTaskData(consumerRecord.key(), consumerRecord.key(), scheduledTaskExecutor.getTaskKeyClass());
        if (key == null) return;

        V value = deserializeTaskData(consumerRecord.key(), consumerRecord.value(), scheduledTaskExecutor.getTaskValueClass());
        if (value == null) return;

        scheduledTaskExecutor.execute(key, value);
    }

    private <T> T deserializeTaskData(String key, String deserializingData, Class<T> dataClass) {
        try {
            return JACKSON_OBJECT_MAPPER.readValue(deserializingData, dataClass);
        } catch (JsonProcessingException cause) {
            log.error(String.format("Произошла ошибка в процессе десериализации данных в рамках задачи '%s'.\n" +
                    "Задача будет пропущена и в дальшейней не будет повторно обрабатываться.", key), cause);
            return null;
        }
    }
}
