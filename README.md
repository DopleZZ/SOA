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

Интерфейсы ресурсов (`VehiclesApi`, `ShopApi`) и все DTO генерируются из
спецификаций при каждой сборке плагином `org.openapi.generator` (генератор
`jaxrs-spec`, `interfaceOnly=true`, `returnResponse=true`, `useJakartaEe=true`)
в `*/build/generated/openapi` и в git не хранятся — у сгенерированных классов
стоит `@jakarta.annotation.Generated(value = "org.openapitools.codegen...")`.
shop-service дополнительно генерирует из `vehicle-service.yaml` DTO для своего
HTTP-клиента (`ru.itmo.soa.shop.client.model`). Настройки — в `build.gradle`.

`jaxrs-spec` не умеет JAXB-аннотации, поэтому два шаблона генератора
переопределены в `openapi-templates/`: `pojo.mustache` добавляет
`@XmlRootElement`/`@XmlElement`/`@XmlElementWrapper`, `enumOuterClass.mustache` —
`XmlAdapter`, который сообщает о значении вне перечня вместо молчаливого `null`.

Вручную написаны реализации интерфейсов (`resource/`), валидация, хранилище,
фильтрация/сортировка, прокси-клиент и обработка ошибок, а также
`ParamConverters` (400/422 вместо стандартного 404 для непреобразуемых
параметров пути/запроса) и `StrictJaxbContextResolver` (ошибки преобразования
значений в XML-теле → 400/422).

## Сборка

```bash
./gradlew build
```

Получаются `vehicle-service/build/libs/vehicle-service.war` и
`shop-service/build/libs/shop-service.war` — деплоятся на два независимых
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
