package ru.itmo.soa.vehicle.store;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import ru.itmo.soa.vehicle.exception.ApiException;
import ru.itmo.soa.vehicle.model.FuelType;
import ru.itmo.soa.vehicle.model.VehicleType;
import ru.itmo.soa.vehicle.query.VehicleField;

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
            List<Object> values = node.getArguments().stream()
                    .map(argument -> typedValue(field, argument))
                    .toList();
            Predicate in = path.in(values);
            return "=in=".equals(operator.getSymbol()) ? in : builder.not(in);
        }

        String value = node.getArguments().get(0);
        return switch (operator.getSymbol()) {
            case "==" -> equality(field, path, value);
            case "!=" -> builder.not(equality(field, path, value));
            case "=gt=" -> compare(field, path, value, Comparison.GREATER);
            case "=ge=" -> compare(field, path, value, Comparison.GREATER_OR_EQUAL);
            case "=lt=" -> compare(field, path, value, Comparison.LESS);
            case "=le=" -> compare(field, path, value, Comparison.LESS_OR_EQUAL);
            default -> throw ApiException.badRequest("Неподдерживаемый оператор фильтрации: " + operator.getSymbol());
        };
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
            int ordinal = (Integer) enumOrdinal(field, value);
            return switch (comparison) {
                case GREATER -> builder.greaterThan(path.as(Integer.class), ordinal);
                case GREATER_OR_EQUAL -> builder.greaterThanOrEqualTo(path.as(Integer.class), ordinal);
                case LESS -> builder.lessThan(path.as(Integer.class), ordinal);
                case LESS_OR_EQUAL -> builder.lessThanOrEqualTo(path.as(Integer.class), ordinal);
            };
        }
        Object typed = typedValue(field, value);
        if (typed instanceof Double number) {
            return switch (comparison) {
                case GREATER -> builder.greaterThan(path.as(Double.class), number);
                case GREATER_OR_EQUAL -> builder.greaterThanOrEqualTo(path.as(Double.class), number);
                case LESS -> builder.lessThan(path.as(Double.class), number);
                case LESS_OR_EQUAL -> builder.lessThanOrEqualTo(path.as(Double.class), number);
            };
        }
        if (typed instanceof OffsetDateTime date) {
            return switch (comparison) {
                case GREATER -> builder.greaterThan(path.as(OffsetDateTime.class), date);
                case GREATER_OR_EQUAL -> builder.greaterThanOrEqualTo(path.as(OffsetDateTime.class), date);
                case LESS -> builder.lessThan(path.as(OffsetDateTime.class), date);
                case LESS_OR_EQUAL -> builder.lessThanOrEqualTo(path.as(OffsetDateTime.class), date);
            };
        }
        throw ApiException.badRequest("Поле не поддерживает сравнение: " + field.getFieldName());
    }

    private Object typedValue(VehicleField field, String value) {
        return switch (field.getKind()) {
            case NUMERIC -> parseNumeric(value);
            case DATE -> parseDate(value);
            case ENUM -> enumConstant(field, value);
            default -> value;
        };
    }

    private Object enumConstant(VehicleField field, String value) {
        try {
            return switch (field) {
                case TYPE -> VehicleType.valueOf(value);
                case FUEL_TYPE -> FuelType.valueOf(value);
                default -> throw ApiException.badRequest("Поле не является перечислением: " + field.getFieldName());
            };
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessableEntity("Значение вне перечня: " + value);
        }
    }

    private Object enumOrdinal(VehicleField field, String value) {
        return ((Enum<?>) enumConstant(field, value)).ordinal();
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
