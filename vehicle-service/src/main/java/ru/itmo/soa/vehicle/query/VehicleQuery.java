package ru.itmo.soa.vehicle.query;

import cz.jirutka.rsql.parser.ast.Node;

import java.util.List;

public final class VehicleQuery {

    private final List<SortKey> sorts;
    private final Node filter;

    public VehicleQuery(List<SortKey> sorts, Node filter) {
        this.sorts = sorts;
        this.filter = filter;
    }

    public List<SortKey> sorts() {
        return sorts;
    }

    public Node filter() {
        return filter;
    }
}
