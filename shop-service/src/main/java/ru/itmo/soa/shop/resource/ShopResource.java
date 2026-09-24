package ru.itmo.soa.shop.resource;

import ru.itmo.soa.shop.api.ShopApi;
import ru.itmo.soa.shop.client.VehicleServiceClient;
import ru.itmo.soa.shop.exception.ApiException;
import ru.itmo.soa.shop.model.Vehicle;
import ru.itmo.soa.shop.model.VehicleType;

import java.util.List;

public class ShopResource implements ShopApi {

    private final VehicleServiceClient vehicleServiceClient = VehicleServiceClient.getInstance();

    @Override
    public List<Vehicle> searchByType(String type) {
        try {
            VehicleType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessableEntity("Значение type вне допустимого перечня: " + type);
        }
        return vehicleServiceClient.search(List.of("type:eq:" + type));
    }

    @Override
    public List<Vehicle> searchByEnginePowerRange(String fromRaw, String toRaw) {
        double from = parseDouble(fromRaw, "from");
        double to = parseDouble(toRaw, "to");
        if (from < 0 || to < 0) {
            throw ApiException.unprocessableEntity("Границы диапазона мощности не могут быть отрицательными");
        }
        if (from > to) {
            throw ApiException.unprocessableEntity("Нижняя граница диапазона не может быть больше верхней");
        }
        return vehicleServiceClient.search(List.of("enginePower:gte:" + from, "enginePower:lte:" + to));
    }

    private double parseDouble(String value, String paramName) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Нечисловое значение параметра " + paramName + ": " + value);
        }
    }
}
