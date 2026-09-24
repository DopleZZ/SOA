package ru.itmo.soa.vehicle.query;

import ru.itmo.soa.vehicle.exception.ApiException;
import ru.itmo.soa.vehicle.model.Vehicle;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class FilterSpec {

    private static final Set<String> OPERATORS = Set.of("eq", "ne", "gt", "gte", "lt", "lte", "like");

    private FilterSpec() {
    }

    public static Predicate<Vehicle> parse(List<String> filterParams) {
        Predicate<Vehicle> result = vehicle -> true;
        if (filterParams == null) {
            return result;
        }
        for (String raw : filterParams) {
            result = result.and(parseOne(raw));
        }
        return result;
    }

    private static Predicate<Vehicle> parseOne(String raw) {
        if (raw == null) {
            throw ApiException.badRequest("Некорректный параметр filter");
        }
        String[] parts = raw.split(":", 3);
        if (parts.length != 3) {
            throw ApiException.badRequest("Некорректный параметр filter: " + raw);
        }
        String fieldName = parts[0];
        String operator = parts[1];
        String value = parts[2];

        VehicleField field = VehicleField.byName(fieldName)
                .orElseThrow(() -> ApiException.badRequest("Неизвестное поле фильтрации: " + fieldName));
        if (!OPERATORS.contains(operator)) {
            throw ApiException.badRequest("Неизвестный оператор фильтрации: " + operator);
        }
        if ("like".equals(operator) && field.getKind() != VehicleField.Kind.STRING) {
            throw ApiException.badRequest("Оператор like применим только к строковым полям: " + fieldName);
        }

        if (field.getKind() == VehicleField.Kind.NUMERIC) {
            double literal = parseDouble(value, raw);
            return vehicle -> {
                int cmp = Double.compare(field.numericValueOf(vehicle), literal);
                return applyNumericOperator(operator, cmp);
            };
        }

        return vehicle -> {
            String actual = field.stringValueOf(vehicle);
            return applyStringOperator(operator, actual, value);
        };
    }

    private static double parseDouble(String value, String raw) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Нечисловое значение в параметре filter: " + raw);
        }
    }

    private static boolean applyNumericOperator(String operator, int cmp) {
        return switch (operator) {
            case "eq" -> cmp == 0;
            case "ne" -> cmp != 0;
            case "gt" -> cmp > 0;
            case "gte" -> cmp >= 0;
            case "lt" -> cmp < 0;
            case "lte" -> cmp <= 0;
            default -> false;
        };
    }

    private static boolean applyStringOperator(String operator, String actual, String literal) {
        int cmp = actual.compareTo(literal);
        return switch (operator) {
            case "eq" -> cmp == 0;
            case "ne" -> cmp != 0;
            case "gt" -> cmp > 0;
            case "gte" -> cmp >= 0;
            case "lt" -> cmp < 0;
            case "lte" -> cmp <= 0;
            case "like" -> actual.contains(literal);
            default -> false;
        };
    }
}
