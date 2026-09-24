package ru.itmo.soa.vehicle.store;

import ru.itmo.soa.vehicle.model.Vehicle;

import java.util.List;

public record Page(List<VehicleEntity> items, long totalElements) {

    public List<Vehicle> models() {
        return items.stream().map(VehicleMapper::toModel).toList();
    }
}
