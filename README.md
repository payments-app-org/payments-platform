# payments-platform

Общая инфраструктурная библиотека сервисов `payments-app-org`: outbox, DLT для Kafka, доменные ошибки и обработка ошибок формата запроса, время (`Clock`), хэши, ShedLock, ResilientCalls, кэш, `IdempotentResults`, схема ошибок для Swagger. Подключается зависимостью и включается автоконфигурацией Spring Boot. Предметного кода здесь нет — только инфраструктура.

```xml
<dependency>
    <groupId>com.paymentsapp</groupId>
    <artifactId>platform</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Новая версия

1. Изменения — PR в `main` (CI: тесты и Trivy).
2. Поднять версию в `pom.xml` (совместимое — `1.1.0`, исправление — `1.0.1`, несовместимое — `2.0.0`).
3. После слияния — тег `vX.Y.Z` на коммит в `main`: CI проверит, что версия совпадает с `pom.xml`, и опубликует её в GitHub Packages. Опубликованную версию не перезаписываем.
4. Сервисы переходят на новую версию сами: меняют `platform.version` в своём `pom.xml`.

Подробно о каждой части — [payments-local/CLAUDE.md](https://github.com/payments-app-org/payments-local/blob/main/CLAUDE.md), раздел «payments-platform».
