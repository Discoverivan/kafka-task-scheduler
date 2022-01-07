package ru.discoverivan.scheduler.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import ru.discoverivan.scheduler.ScheduledTaskExecutor;
import ru.discoverivan.scheduler.TaskSchedulerService;
import ru.discoverivan.scheduler.kafka.KafkaTaskSchedulerService;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Класс автаконфигурации стартера. Выполняет инициализацию {@link TaskSchedulerService}, попутно создавая фабрики подключений к Kafka
 */
@Configuration
@ConditionalOnProperty(prefix = "discoverivan.task-scheduler.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KafkaTaskSchedulerProperties.class)
public class KafkaTaskSchedulerAutoConfiguration {
    public static final String SCHEDULER_PREFIX = "discoverivan.task-scheduler";

    @Bean
    public TaskSchedulerService createKafkaScheduler(
            List<ScheduledTaskExecutor<?, ?>> scheduledTaskExecutors,
            KafkaTaskSchedulerProperties properties,
            GenericApplicationContext applicationContext
    ) {
        if (scheduledTaskExecutors.isEmpty()) {
            throw new IllegalArgumentException("Не найден ни один обработчик событий шедулера!");
        }

        return new KafkaTaskSchedulerService(
                properties,
                applicationContext,
                scheduledTaskExecutors,
                createConsumerFactory(properties),
                createProducerFactory(properties),
                createTransactionalProducerFactory(properties)
        );
    }

    private ProducerFactory<String, String> createProducerFactory(KafkaTaskSchedulerProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.getConnection().getProducer());
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    public ProducerFactory<String, String> createTransactionalProducerFactory(KafkaTaskSchedulerProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.getConnection().getProducer());
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configs.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, SCHEDULER_PREFIX);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    public ConsumerFactory<String, String> createConsumerFactory(KafkaTaskSchedulerProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.getConnection().getConsumer());
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configs.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT));
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }
}
