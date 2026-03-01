# common-libs

Общий модуль с DTO и enum'ами, используемыми оркестратором и стабами.

## Содержимое
- `dev.haritonenko.api.payment.*` — запросы/ответы для authorize/capture и статусы (`AuthorizationStatus`, `CaptureStatus`).
- `dev.haritonenko.api.warehouse.*` — запрос/ответ для пересчёта цены заказа.

## Сборка
```bash
../gradlew :common-libs:jar
```

## Использование
- Внутри монорепы — подключено как `project(":common-libs")`.
- Внешне — можно опубликовать в локальный Maven (`./gradlew :common-libs:publishToMavenLocal`) и подключать зависимостью `dev.haritonenko:common-libs:1.0.0`.
