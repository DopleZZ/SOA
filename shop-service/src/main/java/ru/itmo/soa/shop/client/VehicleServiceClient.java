package ru.itmo.soa.shop.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.itmo.soa.shop.client.model.VehiclePage;
import ru.itmo.soa.shop.exception.ApiException;
import ru.itmo.soa.shop.model.Coordinates;
import ru.itmo.soa.shop.model.FuelType;
import ru.itmo.soa.shop.model.Vehicle;
import ru.itmo.soa.shop.model.VehicleType;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class VehicleServiceClient {

    private static final String VEHICLE_SERVICE_URL = "http://vehicle-service/api/vehicles";
    private static final int PAGE_SIZE = 200;

    private final RestTemplate restTemplate;

    public VehicleServiceClient(@LoadBalanced RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Vehicle> search(List<String> filters) {
        List<Vehicle> result = new ArrayList<>();
        int pageNumber = 1;
        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(VEHICLE_SERVICE_URL)
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", PAGE_SIZE);
            for (String filter : filters) {
                builder.queryParam("filter", filter);
            }
            URI uri = builder.build().encode().toUri();
            VehiclePage page = call(() -> restTemplate.getForObject(uri, VehiclePage.class));
            if (page == null || page.getItems() == null) {
                break;
            }
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

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw ApiException.gatewayTimeout("Таймаут при обращении к vehicle-service");
            }
            throw ApiException.badGateway("Не удалось обратиться к vehicle-service: " + e.getMessage());
        } catch (HttpStatusCodeException e) {
            throw ApiException.serviceUnavailable(
                    "vehicle-service вернул ошибку: HTTP " + e.getRawStatusCode());
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
}
