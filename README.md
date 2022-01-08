# Kafka Task Scheduler - распределенный планировщик заданий

## Состав репозитория

1. **Kafka Task Scheduler Starter** - стартер Spring Boot, который автоматически поднимает бин TaskSchedulerService
2. **Kafka Task Scheduler Service** - приложение, которое используется для демонстрации работы стартера
3. **Kafka Docker** - конфигурация для сборки образа Docker, в котором поднимается Kafka. Используется для целей разработки.

## Инструкция по использованию

### Подключение зависимости

В секции `dependencies` вашего `pom.xml` файла укажите:

```xml
<dependency>
    <groupId>ru.discoverivan.scheduler</groupId>
    <artifactId>kafka-task-scheduler-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Конфигурирование

В файле `application.yml` или `application-<PROFILE>.yml` необходимо добавить секцию конфигурации:

```yaml
discoverivan.task-scheduler.kafka:
  topics:
    - name: APP.SCHEDULER.EXECUTION
      delay: 0
    - name: APP.SCHEDULER.1MIN
      delay: 1m
  connection:
    producer:
      "[bootstrap.servers]": localhost:9092
    consumer:
      "[bootstrap.servers]": localhost:9092
```

### Использование планировщика задач
Планировщик поддерживает работу сразу с множеством типов задач. У каждого типа задач необходимо создать свой `ScheduledTaskExecutor`. 
Задача представляет собой пару: **ключ-значение**.

#### Объявление сервиса

В необходимом месте нужно объявить `TaskSchedulerService` произвести связывание бина. Примеры объявления сервиса:
*С помощью аннотации `@Autowired`:*

```java
@Autowired
private TaskSchedulerService taskSchedulerService;
```

*С использованием Lombok:*

```java

@RequiredArgsConstructor
public class MyService {
    private final TaskSchedulerService taskSchedulerService;
    
    ...
}
```

#### Планирование задачи
Для планирования задачи необоходимо создать экземляр класса `SchedulingTask<K, V>` и вызвать метод `schedule`
у `TaskSchedulerService`:

```java
SchedulingTask<String, String> schedulingTask = SchedulingTask.<String, String>builder()
        .type("MyTestTaskType")  // тип задачи, для этого типа необходимо реализовать ScheduledTaskExecutor
        .key("testKey")
        .value("testValue")
        .build();

taskSchedulerService.schedule(schedulingTask, 10, TimeUnit.SECONDS); // Откладывает задачу на 10 секунд
```

#### Реализация ScheduledTaskExecutor
```java
@Component
public class MyScheduledTaskExecutor implements ScheduledTaskExecutor<String, SchedulerAppTask> {
    @Override
    public void execute(String key, SchedulerAppTask value) {
        // тут можно выполнять обработку поступившей задачи
    }

    @Override
    public String getWaitingTaskType() {
        return "MyTestTaskType"; // Тип задач, обрабатываемых этим ScheduledTaskExecutor
    }

    @Override
    public Class<String> getTaskKeyClass() {
        return String.class;
    }

    @Override
    public Class<SchedulerAppTask> getTaskValueClass() {
        return String.class;
    }
}
```

## Поднятие Docker-образа Kafka для целей разработки
### Использование готового образа
#### Для скачивания образа выполнить команду:
```shell
docker pull ghcr.io/discoverivan/kafka:latest
```
#### Для запуска образа выполнить команду:
```shell
docker run -d -p 2181:2181 -p 9092:9092 ghcr.io/discoverivan/kafka:latest
```

### Самостоятельная сборка образа
В репозитории есть папка `dev-environment/kafka`. 
#### Для сборки образа выполнить команду:
```shell
docker build . -t kafka:latest
```
#### Для запуска образа выполнить команду:
```shell
docker run -d -p 2181:2181 -p 9092:9092 kafka:latest
```
