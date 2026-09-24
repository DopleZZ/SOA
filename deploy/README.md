# Развёртывание на двух инстансах WildFly (HTTPS-only)

## 1. Подготовка

Нужны два независимых инстанса WildFly (два хоста, два докер-контейнера или два
локальных `WILDFLY_HOME` с разными `-Djboss.socket.binding.port-offset`):

- **instance A** — для `vehicle-service.war`;
- **instance B** — для `shop-service.war`, обращается к instance A по HTTPS.

## 2. Ключи и сертификаты

```bash
VEHICLE_SERVICE_SAN="dns:localhost,dns:<host-A>,ip:127.0.0.1" \
SHOP_SERVICE_SAN="dns:localhost,dns:<host-B>,ip:127.0.0.1" \
./generate-keystores.sh
```

`<host-A>` — это то имя/адрес, по которому instance B фактически будет
обращаться к instance A (значение `vehicle.service.baseUrl`, см. ниже). Если
SAN сертификата ему не соответствует, TLS-рукопожатие падает с
`SSLPeerUnverifiedException`, и shop-service корректно отдаёт клиенту 502
(это проверено: так и происходит, если имя хоста в сертификате не совпадает
с именем, используемым для подключения).

Создаст `deploy/keystores/`:

- `vehicle-keystore.jks` — приватный ключ и самоподписанный сертификат instance A;
- `shop-keystore.jks` — приватный ключ и самоподписанный сертификат instance B;
- `vehicle-truststore.jks` — публичный сертификат vehicle-service, которым
  shop-service будет проверять TLS-соединение при вызове API первого сервиса.

Скопировать:

```
vehicle-keystore.jks      -> instanceA/standalone/configuration/
shop-keystore.jks         -> instanceB/standalone/configuration/
vehicle-truststore.jks    -> instanceB/standalone/configuration/
```

## 3. Настройка HTTPS и отключение HTTP

Оба скрипта работают в offline-режиме (`embed-server`), сервер в этот момент
должен быть **остановлен**:

```bash
instanceA/bin/jboss-cli.sh --file=vehicle-service-https.cli
instanceB/bin/jboss-cli.sh --file=shop-service-https.cli
```

Каждый скрипт:

- регистрирует `key-store` / `key-manager` / `server-ssl-context` в подсистеме
  `elytron` на основе соответствующего `.jks`;
- настраивает `https-listener` на стандартный `https` socket-binding (порт
  8443 по умолчанию) на использование этого `server-ssl-context` (современный
  WildFly уже создаёт `https-listener` по умолчанию с dev-сертификатом — скрипт
  это обнаруживает и просто переключает его `ssl-context`; на старом WildFly,
  где этого листенера нет, скрипт добавит его сам);
- удаляет `/subsystem=ejb3/service=remote` и
  `/subsystem=remoting/http-connector=http-remoting-connector` — они по
  умолчанию завязаны на `http-listener`, и без этого шага после удаления
  HTTP-листенера сервер не поднимется (проверено: без этих двух строк boot
  падает с `WFLYCTL0412: Required services that are not installed`);
- **удаляет** `http-listener` и `http` socket-binding — после этого сервер
  вообще не слушает HTTP, доступ возможен только по HTTPS;
- для instance B дополнительно прописывает системные свойства, которые читает
  `VehicleServiceClient`: адрес vehicle-service и путь/пароль к truststore.

Если оба инстанса запускаются на одной машине, у instance B нужно выставить
`-Djboss.socket.binding.port-offset=100` (тогда его https будет на 8543), а в
`vehicle.service.baseUrl` для instance B указывать реальный адрес instance A
(`https://<host-A>:8443`).

## 4. Деплой WAR

```bash
(cd .. && ./gradlew build)
instanceA/bin/jboss-cli.sh -c --command="deploy ../vehicle-service/build/libs/vehicle-service.war"
instanceB/bin/jboss-cli.sh -c --command="deploy ../shop-service/build/libs/shop-service.war"
```

## 5. Проверка

```bash
curl -k https://<host-A>:8443/api/vehicles
curl -k https://<host-B>:8443/shop/search/by-type/PLANE

# HTTP должен быть недоступен:
curl http://<host-A>:8080/api/vehicles   # connection refused
```
