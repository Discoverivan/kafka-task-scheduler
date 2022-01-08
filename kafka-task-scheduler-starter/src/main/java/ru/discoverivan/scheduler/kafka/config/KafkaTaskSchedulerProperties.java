package ru.discoverivan.scheduler.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.discoverivan.scheduler.exceptions.SchedulerConfigurationException;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Параметры паланировщика задач
 */
@Getter
@Setter
@ConfigurationProperties("discoverivan.task-scheduler.kafka")
public class KafkaTaskSchedulerProperties {
    private boolean enabled = true;
    private boolean enabledHealthIndicator = true;
    private List<TopicProperties> topics;
    private ConnectionProperties connection;

    @PostConstruct
    private void postProcessProperties(){
        topics = topics.stream().sorted(Comparator.comparing(TopicProperties::getDelay)).distinct().collect(Collectors.toList());

        if (topics.get(0).getDelay().toMillis() != 0){
            throw new SchedulerConfigurationException("В списке топиков не найден execution-топик (топик с нулевой задержкой)");
        }
        if (topics.size() < 2){
            throw new SchedulerConfigurationException("В списке топиков отсутствуют топики с задержкой");
        }
    }

    @Getter
    @Setter
    public static class TopicProperties {
        private String name;
        private Duration delay;
        private int concurrency = 1;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Duration getDelay() {
            return delay;
        }

        public void setDelay(Duration delay) {
            this.delay = delay;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopicProperties that = (TopicProperties) o;
            return delay.equals(that.delay);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delay);
        }
    }

    @Getter
    @Setter
    public static class ConnectionProperties {
        private Map<String, Object> producer;
        private Map<String, Object> consumer;

        public Map<String, Object> getProducer() {
            return producer;
        }

        public void setProducer(Map<String, Object> producer) {
            this.producer = producer;
        }

        public Map<String, Object> getConsumer() {
            return consumer;
        }

        public void setConsumer(Map<String, Object> consumer) {
            this.consumer = consumer;
        }
    }
}
