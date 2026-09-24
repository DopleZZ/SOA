package ru.itmo.soa.vehicle.store;

import ru.itmo.soa.vehicle.model.Coordinates;
import ru.itmo.soa.vehicle.model.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class VehicleStore {

    private static final VehicleStore INSTANCE = new VehicleStore();

    private final Map<Long, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    private VehicleStore() {
    }

    public static VehicleStore getInstance() {
        return INSTANCE;
    }

    public Vehicle save(Vehicle vehicle) {
        long id = idGenerator.incrementAndGet();
        vehicle.setId(id);
        Vehicle stored = copyOf(vehicle);
        vehicles.put(id, stored);
        return copyOf(stored);
    }

    public Optional<Vehicle> findById(long id) {
        Vehicle vehicle = vehicles.get(id);
        return Optional.ofNullable(vehicle).map(VehicleStore::copyOf);
    }

    public Optional<Vehicle> update(long id, Vehicle newState) {
        Vehicle updated = vehicles.computeIfPresent(id, (key, existing) -> {
            Vehicle merged = copyOf(newState);
            merged.setId(existing.getId());
            merged.setCreationDate(existing.getCreationDate());
            return merged;
        });
        return Optional.ofNullable(updated).map(VehicleStore::copyOf);
    }

    public boolean deleteById(long id) {
        return vehicles.remove(id) != null;
    }

    public List<Vehicle> findAll() {
        List<Vehicle> result = new ArrayList<>(vehicles.size());
        for (Vehicle vehicle : vehicles.values()) {
            result.add(copyOf(vehicle));
        }
        result.sort(Comparator.comparingLong(Vehicle::getId));
        return result;
    }

    private static Vehicle copyOf(Vehicle source) {
        Vehicle copy = new Vehicle();
        copy.setId(source.getId());
        copy.setName(source.getName());
        Coordinates c = source.getCoordinates();
        copy.setCoordinates(new Coordinates(c.getX(), c.getY()));
        copy.setCreationDate(source.getCreationDate());
        copy.setEnginePower(source.getEnginePower());
        copy.setType(source.getType());
        copy.setFuelType(source.getFuelType());
        return copy;
    }
}
