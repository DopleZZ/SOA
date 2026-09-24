package ru.itmo.soa.vehicle.query;

import cz.jirutka.rsql.parser.ast.Node;

import java.util.List;

public record VehicleQuery(List<SortKey> sorts, Node filter) {
}
