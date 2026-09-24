package ru.itmo.soa.vehicle.validation;

import ru.itmo.soa.vehicle.exception.ApiException;
import ru.itmo.soa.vehicle.model.Coordinates;
import ru.itmo.soa.vehicle.model.FuelType;
import ru.itmo.soa.vehicle.model.Vehicle;
import ru.itmo.soa.vehicle.model.VehicleInput;
import ru.itmo.soa.vehicle.model.VehiclePatch;
import ru.itmo.soa.vehicle.model.VehicleType;

import java.time.Instant;

public final class VehicleValidator {

    private VehicleValidator() {
    }

    public static Vehicle toNewVehicle(VehicleInput input) {
        if (input == null) {
            throw ApiException.unprocessableEntity("Тело запроса не может быть пустым");
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setName(validateName(input.getName()));
        vehicle.setCoordinates(validateCoordinates(input.getCoordinates()));
        vehicle.setEnginePower(validateEnginePower(input.getEnginePower()));
        vehicle.setType(validateType(input.getType()));
        vehicle.setFuelType(validateFuelType(input.getFuelType()));
        vehicle.setCreationDate(Instant.now().toString());
        return vehicle;
    }

    public static Vehicle fromInputForReplace(VehicleInput input) {
        return toNewVehicle(input);
    }

    public static Vehicle applyPatch(Vehicle existing, VehiclePatch patch) {
        if (patch == null || patch.isEmpty()) {
            throw ApiException.unprocessableEntity("Нужно передать хотя бы одно изменяемое поле");
        }
        Vehicle merged = new Vehicle();
        merged.setId(existing.getId());
        merged.setCreationDate(existing.getCreationDate());
        merged.setName(patch.getName() != null ? validateName(patch.getName()) : existing.getName());
        merged.setCoordinates(patch.getCoordinates() != null ? validateCoordinates(patch.getCoordinates()) : existing.getCoordinates());
        merged.setEnginePower(patch.getEnginePower() != null ? validateEnginePower(patch.getEnginePower()) : existing.getEnginePower());
        merged.setType(patch.getType() != null ? validateType(patch.getType()) : existing.getType());
        merged.setFuelType(patch.getFuelType() != null ? validateFuelType(patch.getFuelType()) : existing.getFuelType());
        return merged;
    }

    private static String validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw ApiException.unprocessableEntity("Поле name не может быть пустым");
        }
        return name;
    }

    private static Coordinates validateCoordinates(Coordinates coordinates) {
        if (coordinates == null || coordinates.getX() == null || coordinates.getY() == null) {
            throw ApiException.unprocessableEntity("Поле coordinates обязательно и должно содержать x и y");
        }
        if (coordinates.getY() <= -575) {
            throw ApiException.unprocessableEntity("Поле coordinates.y должно быть больше -575");
        }
        return coordinates;
    }

    private static Double validateEnginePower(Double enginePower) {
        if (enginePower == null || enginePower <= 0) {
            throw ApiException.unprocessableEntity("Поле enginePower должно быть больше 0");
        }
        return enginePower;
    }

    private static VehicleType validateType(String type) {
        if (type == null) {
            throw ApiException.unprocessableEntity("Поле type обязательно");
        }
        try {
            return VehicleType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessableEntity("Значение type вне допустимого перечня: " + type);
        }
    }

    private static FuelType validateFuelType(String fuelType) {
        if (fuelType == null) {
            throw ApiException.unprocessableEntity("Поле fuelType обязательно");
        }
        try {
            return FuelType.valueOf(fuelType);
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessableEntity("Значение fuelType вне допустимого перечня: " + fuelType);
        }
    }
}
