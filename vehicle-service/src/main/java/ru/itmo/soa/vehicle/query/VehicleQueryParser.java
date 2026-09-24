package ru.itmo.soa.vehicle.query;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import ru.itmo.soa.vehicle.exception.ApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class VehicleQueryParser {

    private static final Pattern SORT_PATTERN = Pattern.compile(
            "^(id|name|coordinates\\.x|coordinates\\.y|creationDate|enginePower|type|fuelType)(,(asc|desc))?$");

    private VehicleQueryParser() {
    }

    public static VehicleQuery parse(List<String> sortParams, List<String> filterParams) {
        return new VehicleQuery(parseSorts(sortParams), parseFilter(filterParams));
    }

    private static List<SortKey> parseSorts(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return List.of(new SortKey(VehicleField.ID, false));
        }
        List<SortKey> sorts = new ArrayList<>();
        for (String raw : sortParams) {
            if (raw == null || !SORT_PATTERN.matcher(raw).matches()) {
                throw ApiException.badRequest("Некорректный параметр sort: " + raw);
            }
            String[] parts = raw.split(",", 2);
            sorts.add(new SortKey(VehicleField.byName(parts[0]), parts.length > 1 && "desc".equals(parts[1])));
        }
        return List.copyOf(sorts);
    }

    private static Node parseFilter(List<String> filterParams) {
        if (filterParams == null || filterParams.isEmpty()) {
            return null;
        }
        try {
            return new RSQLParser(RSQLOperators.defaultOperators())
                    .parse(String.join(";", filterParams));
        } catch (RSQLParserException | IllegalArgumentException e) {
            throw ApiException.badRequest("Некорректный параметр filter: " + e.getMessage());
        }
    }
}
