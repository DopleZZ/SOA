package ru.itmo.soa.vehicle.query;

import ru.itmo.soa.vehicle.model.Vehicle;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public enum VehicleField {

    ID("id", Kind.NUMERIC, v -> (double) v.getId()),
    NAME("name", Kind.STRING, v -> v.getName()),
    COORDINATES_X("coordinates.x", Kind.NUMERIC, v -> v.getCoordinates().getX()),
    COORDINATES_Y("coordinates.y", Kind.NUMERIC, v -> (double) v.getCoordinates().getY()),
    CREATION_DATE("creationDate", Kind.STRING, v -> v.getCreationDate()),
    ENGINE_POWER("enginePower", Kind.NUMERIC, v -> v.getEnginePower()),
    TYPE("type", Kind.STRING, v -> v.getType().name()),
    FUEL_TYPE("fuelType", Kind.STRING, v -> v.getFuelType().name());

    public enum Kind {
        NUMERIC, STRING
    }

    private final String fieldName;
    private final Kind kind;
    private final Function<Vehicle, Object> accessor;

    VehicleField(String fieldName, Kind kind, Function<Vehicle, Object> accessor) {
        this.fieldName = fieldName;
        this.kind = kind;
        this.accessor = accessor;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Kind getKind() {
        return kind;
    }

    public Object valueOf(Vehicle vehicle) {
        return accessor.apply(vehicle);
    }

    public String stringValueOf(Vehicle vehicle) {
        return String.valueOf(valueOf(vehicle));
    }

    public double numericValueOf(Vehicle vehicle) {
        return (double) valueOf(vehicle);
    }

    public static Optional<VehicleField> byName(String name) {
        return Arrays.stream(values())
                .filter(field -> field.fieldName.equals(name))
                .findFirst();
    }
}
