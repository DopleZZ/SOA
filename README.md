# SOA lab2

Два REST-сервиса на JAX-RS (RESTEasy/WildFly, XML-only) и клиентское веб-приложение.

## Структура

- `vehicle-service.yaml`, `shop-service.yaml` — спецификации OpenAPI (лаба 1).
- `vehicle-service/` — первый сервис: CRUD + фильтрация/сортировка/постраничный
  вывод коллекции `Vehicle`, агрегаты (`sum-engine-power`, `group-count-by-id`,
  `by-type-greater`). Хранилище — in-memory.
- `shop-service/` — второй сервис (`/shop/search/...`), проксирует запросы в
  `vehicle-service` по HTTPS и транслирует его ошибки в 502/503/504.
- `client/` — веб-клиент (Node.js/Express, EJS, без клиентского JS), покрывает
  весь API обоих сервисов и человекочитаемо показывает ошибки.
- `deploy/` — генерация самоподписанных сертификатов и CLI-скрипты для
  настройки HTTPS-only на двух инстансах WildFly (см. `deploy/README.md`).

Хендлеры (`*Api` интерфейсы с JAX-RS аннотациями) для обоих сервисов
сгенерированы из спецификаций через `openapi-generator` (генератор
`jaxrs-spec`, `interfaceOnly=true`, `useJakartaEe=true`) и затем донастроены
вручную (генератор не проставляет JAXB/XML-биндинги и не даёт нужного
контроля над кодами ошибок — see `vehicle-service/src/main/java/.../api`,
`shop-service/src/main/java/.../api`). Остальной код (модели, валидация,
хранилище, прокси-клиент, обработка ошибок) написан вручную по спецификации.

## Сборка

```bash
mvn package
```

Получаются `vehicle-service/target/vehicle-service.war` и
`shop-service/target/shop-service.war` — деплоятся на два независимых
инстанса WildFly (см. `deploy/README.md` про HTTPS-only и сертификаты).

## Клиент локально

```bash
cd client
npm install
VEHICLE_SERVICE_BASE_URL=https://<host-A>:8443 \
SHOP_SERVICE_BASE_URL=https://<host-B>:8443 \
VEHICLE_SERVICE_CA=../deploy/keystores/vehicle-service.pem \
SHOP_SERVICE_CA=../deploy/keystores/shop-service.pem \
npm start
```

Открыть `http://localhost:3000`.
