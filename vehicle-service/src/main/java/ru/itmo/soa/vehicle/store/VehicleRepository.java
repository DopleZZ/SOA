package ru.itmo.soa.vehicle.store;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import ru.itmo.soa.vehicle.model.VehicleType;
import ru.itmo.soa.vehicle.query.SortKey;
import ru.itmo.soa.vehicle.query.VehicleField;
import ru.itmo.soa.vehicle.query.VehicleQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Transactional
public class VehicleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public VehicleEntity save(VehicleEntity entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    public Optional<VehicleEntity> findById(long id) {
        return Optional.ofNullable(entityManager.find(VehicleEntity.class, id));
    }

    public void delete(VehicleEntity entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Page search(int pageNumber, int pageSize, VehicleQuery query) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<VehicleEntity> countRoot = countQuery.from(VehicleEntity.class);
        Predicate countFilter = query.filter() == null
                ? builder.conjunction()
                : RsqlCriteriaTransformer.toPredicate(query.filter(), builder, countRoot);
        countQuery.select(builder.count(countRoot)).where(countFilter);
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<VehicleEntity> itemQuery = builder.createQuery(VehicleEntity.class);
        Root<VehicleEntity> root = itemQuery.from(VehicleEntity.class);
        Predicate itemFilter = query.filter() == null
                ? builder.conjunction()
                : RsqlCriteriaTransformer.toPredicate(query.filter(), builder, root);
        itemQuery.select(root).where(itemFilter).orderBy(buildOrder(builder, root, query.sorts()));

        List<VehicleEntity> items = entityManager.createQuery(itemQuery)
                .setFirstResult((pageNumber - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return new Page(items, totalElements);
    }

    public double sumEnginePower() {
        return entityManager.createQuery(
                        "select coalesce(sum(v.enginePower), 0.0) from VehicleEntity v", Double.class)
                .getSingleResult();
    }

    public Map<Long, Long> groupCountById() {
        List<Object[]> rows = entityManager.createQuery(
                        "select v.id, count(v) from VehicleEntity v group by v.id order by v.id", Object[].class)
                .getResultList();
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    public List<VehicleEntity> findByTypeGreaterThan(VehicleType type) {
        return entityManager.createQuery(
                        "select v from VehicleEntity v where v.type > :type order by v.id asc", VehicleEntity.class)
                .setParameter("type", type)
                .getResultList();
    }

    private List<Order> buildOrder(CriteriaBuilder builder, Root<VehicleEntity> root, List<SortKey> sorts) {
        List<SortKey> keys = sorts.isEmpty() ? List.of(new SortKey(VehicleField.ID, false)) : sorts;
        List<Order> orders = new ArrayList<>();
        for (SortKey key : keys) {
            orders.add(key.descending()
                    ? builder.desc(root.get(key.field().getAttributeName()))
                    : builder.asc(root.get(key.field().getAttributeName())));
        }
        return orders;
    }
}
