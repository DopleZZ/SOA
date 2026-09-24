package ru.itmo.soa.shop.client;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import ru.itmo.soa.shop.exception.ApiException;
import ru.itmo.soa.shop.client.model.VehiclePage;
import ru.itmo.soa.shop.model.Coordinates;
import ru.itmo.soa.shop.model.FuelType;
import ru.itmo.soa.shop.model.Vehicle;
import ru.itmo.soa.shop.model.VehicleType;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class VehicleServiceClient implements AutoCloseable {

    private static final String DEFAULT_BASE_URL = "https://localhost:8443";
    private static final int PAGE_SIZE = 200;

    private static final VehicleServiceClient INSTANCE = new VehicleServiceClient();

    public static VehicleServiceClient getInstance() {
        return INSTANCE;
    }

    private final Client client;
    private final WebTarget vehiclesTarget;

    private VehicleServiceClient() {
        String baseUrl = configValue("vehicle.service.baseUrl", "VEHICLE_SERVICE_BASE_URL", DEFAULT_BASE_URL);

        ClientBuilder builder = ClientBuilder.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        SSLContext sslContext = buildTrustingSslContext();
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }

        this.client = builder.build();
        this.vehiclesTarget = client.target(baseUrl).path("/api/vehicles");
    }

    public List<Vehicle> search(List<String> filters) {
        List<Vehicle> result = new ArrayList<>();
        int pageNumber = 1;
        while (true) {
            WebTarget target = vehiclesTarget
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", PAGE_SIZE);
            for (String filter : filters) {
                target = target.queryParam("filter", filter);
            }
            final WebTarget requestTarget = target;
            VehiclePage page = call(() -> requestTarget.request(MediaType.APPLICATION_XML).get(VehiclePage.class));
            page.getItems().stream().map(VehicleServiceClient::toShopVehicle).forEach(result::add);
            if (page.getTotalPages() == null || pageNumber >= page.getTotalPages() || page.getItems().isEmpty()) {
                break;
            }
            pageNumber++;
        }
        return result;
    }

    private static Vehicle toShopVehicle(ru.itmo.soa.shop.client.model.Vehicle source) {
        return new Vehicle()
                .id(source.getId())
                .name(source.getName())
                .coordinates(new Coordinates()
                        .x(source.getCoordinates().getX())
                        .y(source.getCoordinates().getY()))
                .creationDate(source.getCreationDate())
                .enginePower(source.getEnginePower())
                .type(VehicleType.fromValue(source.getType().toString()))
                .fuelType(FuelType.fromValue(source.getFuelType().toString()));
    }

    @Override
    public void close() {
        client.close();
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (ProcessingException e) {
            if (isTimeout(e)) {
                throw ApiException.gatewayTimeout("Таймаут при обращении к vehicle-service");
            }
            throw ApiException.badGateway("Не удалось обратиться к vehicle-service: " + e.getMessage());
        } catch (WebApplicationException e) {
            throw ApiException.serviceUnavailable("vehicle-service вернул ошибку: HTTP " + e.getResponse().getStatus());
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private SSLContext buildTrustingSslContext() {
        String truststorePath = configValue("vehicle.service.truststore.path", "VEHICLE_SERVICE_TRUSTSTORE", null);
        if (truststorePath == null) {
            return null;
        }
        String password = configValue("vehicle.service.truststore.password", "VEHICLE_SERVICE_TRUSTSTORE_PASSWORD", "changeit");

        try (InputStream in = Files.newInputStream(Path.of(truststorePath))) {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(in, password.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить truststore vehicle-service из " + truststorePath, e);
        }
    }

    private static String configValue(String systemProperty, String envVar, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null) {
            value = System.getenv(envVar);
        }
        return value != null ? value : defaultValue;
    }
}
