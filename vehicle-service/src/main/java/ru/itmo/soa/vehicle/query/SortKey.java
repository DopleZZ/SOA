package ru.itmo.soa.vehicle.query;

public final class SortKey {

    private final VehicleField field;
    private final boolean descending;

    public SortKey(VehicleField field, boolean descending) {
        this.field = field;
        this.descending = descending;
    }

    public VehicleField field() {
        return field;
    }

    public boolean descending() {
        return descending;
    }
}
