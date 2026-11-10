# payments-platform

Общая инфраструктурная библиотека сервисов `payments-app-org`: outbox, DLT для Kafka, доменные ошибки и обработка ошибок формата запроса, время (`Clock`), хэши, ShedLock, ResilientCalls, кэш, `IdempotentResults`, схема ошибок для Swagger, метрики для Prometheus и трассировка (OpenTelemetry). Подключается зависимостью и включается автоконфигурацией Spring Boot. Предметного кода здесь нет — только инфраструктура.

```xml
<dependency>
    <groupId>com.paymentsapp</groupId>
    <artifactId>platform</artifactId>
    <version>1.2.1</version>
</dependency>
```

## Наблюдаемость (с 1.1.0)

- **Метрики**: реестр Prometheus и метка `application` (= `spring.application.name`) на всех метриках. Сервис открывает `/actuator/prometheus`: `management.endpoints.web.exposure.include: health,info,prometheus`.
- **Трассы**: Micrometer Tracing → OpenTelemetry → OTLP. Куда отправлять — `management.opentelemetry.tracing.export.otlp.endpoint`, доля запросов — `management.tracing.sampling.probability`; задаёт окружение (в Kubernetes — `payments-deploy/envs/<окружение>/env.yaml`). Адрес не задан — трассы никуда не отправляются, ошибок нет.
- **Без `/actuator/**`** (с 1.2.0): пробы Kubernetes и сбор метрик Prometheus не создают трасс и метрик запросов — в Jaeger только настоящие запросы. Служебные команды Redis (`INFO` проверки здоровья, `HELLO`/`CLIENT`/`PING` при подключении) тоже без трасс (с 1.2.1); команды Redis внутри запросов и задач остаются в их трассах.
- **Логи в JSON** (ECS) с `traceId` и `spanId` — тоже настройка окружения: `logging.structured.format.console: ecs`.

## Новая версия

1. Изменения — PR в `main` (CI: тесты и Trivy).
2. Поднять версию в `pom.xml` (совместимое — `1.1.0`, исправление — `1.0.1`, несовместимое — `2.0.0`).
3. После слияния — тег `vX.Y.Z` на коммит в `main`: CI проверит, что версия совпадает с `pom.xml`, и опубликует её в GitHub Packages. Опубликованную версию не перезаписываем.
4. Сервисы переходят на новую версию сами: меняют `platform.version` в своём `pom.xml`.

Подробно о каждой части — [payments-local/CLAUDE.md](https://github.com/payments-app-org/payments-local/blob/main/CLAUDE.md), раздел «payments-platform».
