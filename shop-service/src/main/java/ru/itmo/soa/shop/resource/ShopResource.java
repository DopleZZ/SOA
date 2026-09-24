package ru.itmo.soa.shop.resource;

import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import ru.itmo.soa.shop.api.ShopApi;
import ru.itmo.soa.shop.client.VehicleServiceClient;
import ru.itmo.soa.shop.exception.ApiException;
import ru.itmo.soa.shop.model.Vehicle;
import ru.itmo.soa.shop.model.VehicleType;

import java.util.List;

public class ShopResource implements ShopApi {

    private final VehicleServiceClient vehicleServiceClient = VehicleServiceClient.getInstance();

    @Override
    public Response searchByType(VehicleType type) {
        return vehicles(vehicleServiceClient.search(List.of("type:eq:" + type)));
    }

    @Override
    public Response searchByEnginePowerRange(Double from, Double to) {
        if (from < 0 || to < 0) {
            throw ApiException.unprocessableEntity("Границы диапазона мощности не могут быть отрицательными");
        }
        if (from > to) {
            throw ApiException.unprocessableEntity("Нижняя граница диапазона не может быть больше верхней");
        }
        return vehicles(vehicleServiceClient.search(List.of("enginePower:gte:" + from, "enginePower:lte:" + to)));
    }

    private static Response vehicles(List<Vehicle> vehicles) {
        return Response.ok(new GenericEntity<List<Vehicle>>(vehicles) {
        }).build();
    }
}
