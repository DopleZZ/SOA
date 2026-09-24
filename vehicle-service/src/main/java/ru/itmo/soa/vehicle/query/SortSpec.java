package ru.itmo.soa.vehicle.query;

import ru.itmo.soa.vehicle.exception.ApiException;
import ru.itmo.soa.vehicle.model.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class SortSpec {

    private static final Pattern PATTERN = Pattern.compile(
            "^(id|name|coordinates\\.x|coordinates\\.y|creationDate|enginePower|type|fuelType)(,(asc|desc))?$");

    private SortSpec() {
    }

    public static Comparator<Vehicle> parse(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            sortParams = List.of("id,asc");
        }

        Comparator<Vehicle> comparator = null;
        for (String raw : sortParams) {
            if (raw == null || !PATTERN.matcher(raw).matches()) {
                throw ApiException.badRequest("Некорректный параметр sort: " + raw);
            }
            String[] parts = raw.split(",", 2);
            VehicleField field = VehicleField.byName(parts[0])
                    .orElseThrow(() -> ApiException.badRequest("Неизвестное поле сортировки: " + parts[0]));
            boolean descending = parts.length > 1 && "desc".equals(parts[1]);

            Comparator<Vehicle> fieldComparator = field.getKind() == VehicleField.Kind.NUMERIC
                    ? Comparator.comparingDouble(field::numericValueOf)
                    : Comparator.comparing(field::stringValueOf);
            if (descending) {
                fieldComparator = fieldComparator.reversed();
            }

            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }
        return comparator;
    }
}
