package ru.itmo.soa.vehicle.query;

import java.util.Arrays;

public enum VehicleField {

    ID("id", "id", Kind.NUMERIC),
    NAME("name", "name", Kind.STRING),
    COORDINATES_X("coordinates.x", "coordX", Kind.NUMERIC),
    COORDINATES_Y("coordinates.y", "coordY", Kind.NUMERIC),
    CREATION_DATE("creationDate", "creationDate", Kind.DATE),
    ENGINE_POWER("enginePower", "enginePower", Kind.NUMERIC),
    TYPE("type", "type", Kind.ENUM),
    FUEL_TYPE("fuelType", "fuelType", Kind.ENUM);

    public enum Kind {
        NUMERIC, STRING, DATE, ENUM
    }

    private final String fieldName;
    private final String attributeName;
    private final Kind kind;

    VehicleField(String fieldName, String attributeName, Kind kind) {
        this.fieldName = fieldName;
        this.attributeName = attributeName;
        this.kind = kind;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Kind getKind() {
        return kind;
    }

    public static VehicleField byName(String name) {
        return Arrays.stream(values())
                .filter(field -> field.fieldName.equals(name))
                .findFirst()
                .orElse(null);
    }
}
