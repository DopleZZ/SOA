package ru.itmo.soa.vehicle.store;

import ru.itmo.soa.vehicle.model.Coordinates;
import ru.itmo.soa.vehicle.model.Vehicle;

import java.time.OffsetDateTime;

public final class VehicleMapper {

    private VehicleMapper() {
    }

    public static VehicleEntity toEntity(Vehicle model) {
        VehicleEntity entity = new VehicleEntity();
        apply(entity, model);
        return entity;
    }

    public static VehicleEntity apply(VehicleEntity entity, Vehicle model) {
        entity.setName(model.getName());
        entity.setCoordX(model.getCoordinates().getX());
        entity.setCoordY(model.getCoordinates().getY());
        entity.setCreationDate(OffsetDateTime.parse(model.getCreationDate()));
        entity.setEnginePower(model.getEnginePower());
        entity.setType(model.getType());
        entity.setFuelType(model.getFuelType());
        return entity;
    }

    public static Vehicle toModel(VehicleEntity entity) {
        Vehicle model = new Vehicle();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setCoordinates(new Coordinates().x(entity.getCoordX()).y(entity.getCoordY()));
        model.setCreationDate(entity.getCreationDate().toString());
        model.setEnginePower(entity.getEnginePower());
        model.setType(entity.getType());
        model.setFuelType(entity.getFuelType());
        return model;
    }
}
