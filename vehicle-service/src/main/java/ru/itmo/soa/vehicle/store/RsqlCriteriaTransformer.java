package ru.itmo.soa.vehicle.store;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import ru.itmo.soa.vehicle.exception.ApiException;
import ru.itmo.soa.vehicle.model.FuelType;
import ru.itmo.soa.vehicle.model.VehicleType;
import ru.itmo.soa.vehicle.query.VehicleField;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RsqlCriteriaTransformer implements RSQLVisitor<Predicate, Void> {

    private final CriteriaBuilder builder;
    private final Root<VehicleEntity> root;

    public static Predicate toPredicate(Node node, CriteriaBuilder builder, Root<VehicleEntity> root) {
        return node.accept(new RsqlCriteriaTransformer(builder, root), null);
    }

    private RsqlCriteriaTransformer(CriteriaBuilder builder, Root<VehicleEntity> root) {
        this.builder = builder;
        this.root = root;
    }

    @Override
    public Predicate visit(AndNode node, Void parameter) {
        return visitLogical(node, parameter);
    }

    @Override
    public Predicate visit(OrNode node, Void parameter) {
        return visitLogical(node, parameter);
    }

    private Predicate visitLogical(LogicalNode node, Void parameter) {
        List<Predicate> children = node.getChildren().stream()
                .map(child -> child.accept(this, parameter))
                .collect(Collectors.toList());
        return node.getOperator() == LogicalOperator.OR
                ? builder.or(children.toArray(new Predicate[0]))
                : builder.and(children.toArray(new Predicate[0]));
    }

    @Override
    public Predicate visit(ComparisonNode node, Void parameter) {
        VehicleField field = VehicleField.byName(node.getSelector());
        if (field == null) {
            throw ApiException.badRequest("Неизвестное поле фильтрации: " + node.getSelector());
        }
        Path<Object> path = root.get(field.getAttributeName());
        ComparisonOperator operator = node.getOperator();

        if (operator.isMultiValue()) {
            List<Object> values = new ArrayList<>();
            for (String argument : node.getArguments()) {
                values.add(typedValue(field, argument));
            }
            Predicate in = path.in(values);
            return "=in=".equals(operator.getSymbol()) ? in : builder.not(in);
        }

        String value = node.getArguments().get(0);
        String symbol = operator.getSymbol();
        if ("==".equals(symbol)) {
            return equality(field, path, value);
        }
        if ("!=".equals(symbol)) {
            return builder.not(equality(field, path, value));
        }
        if ("=gt=".equals(symbol)) {
            return compare(field, path, value, Comparison.GREATER);
        }
        if ("=ge=".equals(symbol)) {
            return compare(field, path, value, Comparison.GREATER_OR_EQUAL);
        }
        if ("=lt=".equals(symbol)) {
            return compare(field, path, value, Comparison.LESS);
        }
        if ("=le=".equals(symbol)) {
            return compare(field, path, value, Comparison.LESS_OR_EQUAL);
        }
        throw ApiException.badRequest("Неподдерживаемый оператор фильтрации: " + operator.getSymbol());
    }

    private enum Comparison {
        GREATER, GREATER_OR_EQUAL, LESS, LESS_OR_EQUAL
    }

    private Predicate equality(VehicleField field, Path<Object> path, String value) {
        if (field.getKind() == VehicleField.Kind.STRING && value.contains("*")) {
            return builder.like(path.as(String.class), value.replace('*', '%'));
        }
        return builder.equal(path, typedValue(field, value));
    }

    private Predicate compare(VehicleField field, Path<Object> path, String value, Comparison comparison) {
        if (field.getKind() == VehicleField.Kind.ENUM) {
            int ordinal = ((Enum<?>) enumConstant(field, value)).ordinal();
            switch (comparison) {
                case GREATER:
                    return builder.greaterThan(path.as(Integer.class), ordinal);
                case GREATER_OR_EQUAL:
                    return builder.greaterThanOrEqualTo(path.as(Integer.class), ordinal);
                case LESS:
                    return builder.lessThan(path.as(Integer.class), ordinal);
                case LESS_OR_EQUAL:
                    return builder.lessThanOrEqualTo(path.as(Integer.class), ordinal);
                default:
                    throw ApiException.badRequest("Поле не поддерживает сравнение: " + field.getFieldName());
            }
        }
        Object typed = typedValue(field, value);
        if (typed instanceof Double) {
            Double number = (Double) typed;
            switch (comparison) {
                case GREATER:
                    return builder.greaterThan(path.as(Double.class), number);
                case GREATER_OR_EQUAL:
                    return builder.greaterThanOrEqualTo(path.as(Double.class), number);
                case LESS:
                    return builder.lessThan(path.as(Double.class), number);
                case LESS_OR_EQUAL:
                    return builder.lessThanOrEqualTo(path.as(Double.class), number);
                default:
                    throw ApiException.badRequest("Поле не поддерживает сравнение: " + field.getFieldName());
            }
        }
        if (typed instanceof OffsetDateTime) {
            OffsetDateTime date = (OffsetDateTime) typed;
            switch (comparison) {
                case GREATER:
                    return builder.greaterThan(path.as(OffsetDateTime.class), date);
                case GREATER_OR_EQUAL:
                    return builder.greaterThanOrEqualTo(path.as(OffsetDateTime.class), date);
                case LESS:
                    return builder.lessThan(path.as(OffsetDateTime.class), date);
                case LESS_OR_EQUAL:
                    return builder.lessThanOrEqualTo(path.as(OffsetDateTime.class), date);
                default:
                    throw ApiException.badRequest("Поле не поддерживает сравнение: " + field.getFieldName());
            }
        }
        throw ApiException.badRequest("Поле не поддерживает сравнение: " + field.getFieldName());
    }

    private Object typedValue(VehicleField field, String value) {
        switch (field.getKind()) {
            case NUMERIC:
                return parseNumeric(value);
            case DATE:
                return parseDate(value);
            case ENUM:
                return enumConstant(field, value);
            default:
                return value;
        }
    }

    private Object enumConstant(VehicleField field, String value) {
        if (field != VehicleField.TYPE && field != VehicleField.FUEL_TYPE) {
            throw ApiException.badRequest("Поле не является перечислением: " + field.getFieldName());
        }
        try {
            return field == VehicleField.TYPE
                    ? VehicleType.valueOf(value)
                    : FuelType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessableEntity("Значение вне перечня: " + value);
        }
    }

    private Object parseNumeric(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Нечисловое значение в параметре filter: " + value);
        }
    }

    private Object parseDate(String value) {
        try {
            return OffsetDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Значение creationDate должно быть датой в формате ISO-8601: " + value);
        }
    }
}
