package ru.itmo.soa.vehicle.store;

import ru.itmo.soa.vehicle.model.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

public final class Page {

    private final List<VehicleEntity> items;
    private final long totalElements;

    public Page(List<VehicleEntity> items, long totalElements) {
        this.items = items;
        this.totalElements = totalElements;
    }

    public List<VehicleEntity> items() {
        return items;
    }

    public long totalElements() {
        return totalElements;
    }

    public List<Vehicle> models() {
        return items.stream().map(VehicleMapper::toModel).collect(Collectors.toList());
    }
}
