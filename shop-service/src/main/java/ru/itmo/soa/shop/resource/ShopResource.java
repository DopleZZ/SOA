package ru.itmo.soa.shop.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.soa.shop.api.ShopApi;
import ru.itmo.soa.shop.client.VehicleServiceClient;
import ru.itmo.soa.shop.exception.ApiException;
import ru.itmo.soa.shop.model.Vehicle;
import ru.itmo.soa.shop.model.VehicleType;

import java.util.List;

@RestController
public class ShopResource implements ShopApi {

    private final VehicleServiceClient vehicleServiceClient;

    public ShopResource(VehicleServiceClient vehicleServiceClient) {
        this.vehicleServiceClient = vehicleServiceClient;
    }

    @Override
    public ResponseEntity<List<Vehicle>> searchByType(VehicleType type) {
        return ResponseEntity.ok(vehicleServiceClient.search(List.of("type==" + type)));
    }

    @Override
    public ResponseEntity<List<Vehicle>> searchByEnginePowerRange(Double from, Double to) {
        if (from < 0 || to < 0) {
            throw ApiException.unprocessableEntity("Границы диапазона мощности не могут быть отрицательными");
        }
        if (from > to) {
            throw ApiException.unprocessableEntity("Нижняя граница диапазона не может быть больше верхней");
        }
        return ResponseEntity.ok(vehicleServiceClient.search(List.of("enginePower=ge=" + from + ";enginePower=le=" + to)));
    }
}
