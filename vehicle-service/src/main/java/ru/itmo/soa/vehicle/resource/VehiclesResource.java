package ru.itmo.soa.vehicle.resource;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import ru.itmo.soa.vehicle.api.VehiclesApi;
import ru.itmo.soa.vehicle.exception.ApiException;
import ru.itmo.soa.vehicle.model.IdGroupCount;
import ru.itmo.soa.vehicle.model.IdGroupCountEntry;
import ru.itmo.soa.vehicle.model.SumResult;
import ru.itmo.soa.vehicle.model.Vehicle;
import ru.itmo.soa.vehicle.model.VehicleInput;
import ru.itmo.soa.vehicle.model.VehiclePage;
import ru.itmo.soa.vehicle.model.VehiclePatch;
import ru.itmo.soa.vehicle.model.VehicleType;
import ru.itmo.soa.vehicle.query.FilterSpec;
import ru.itmo.soa.vehicle.query.SortSpec;
import ru.itmo.soa.vehicle.store.VehicleStore;
import ru.itmo.soa.vehicle.validation.VehicleValidator;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VehiclesResource implements VehiclesApi {

    private final VehicleStore store = VehicleStore.getInstance();

    @Context
    private UriInfo uriInfo;

    @Override
    public Response createVehicle(VehicleInput vehicleInput) {
        Vehicle vehicle = VehicleValidator.toNewVehicle(vehicleInput);
        Vehicle saved = store.save(vehicle);
        java.net.URI location = uriInfo.getBaseUriBuilder()
                .path(VehiclesApi.class)
                .path(VehiclesApi.class, "getVehicleById")
                .build(saved.getId());
        return Response.created(location).entity(saved).build();
    }

    @Override
    public Response getVehicles(Integer pageNumber, Integer pageSize, List<String> sort, List<String> filter) {
        requirePositive(pageNumber, "pageNumber");
        requirePositive(pageSize, "pageSize");

        Comparator<Vehicle> comparator = SortSpec.parse(sort);
        java.util.function.Predicate<Vehicle> predicate = FilterSpec.parse(filter);

        List<Vehicle> filtered = store.findAll().stream()
                .filter(predicate)
                .sorted(comparator)
                .collect(Collectors.toList());

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int fromIndex = Math.min((pageNumber - 1) * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        VehiclePage page = new VehiclePage();
        page.setPageNumber(pageNumber);
        page.setPageSize(pageSize);
        page.setTotalElements((long) totalElements);
        page.setTotalPages(totalPages);
        page.setItems(filtered.subList(fromIndex, toIndex));
        return Response.ok(page).build();
    }

    @Override
    public Response getVehicleById(Long id) {
        Vehicle vehicle = store.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        return Response.ok(vehicle).build();
    }

    @Override
    public Response updateVehicle(Long id, VehicleInput vehicleInput) {
        store.findById(id).orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle replacement = VehicleValidator.fromInputForReplace(vehicleInput);
        Vehicle updated = store.update(id, replacement)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        return Response.ok(updated).build();
    }

    @Override
    public Response partialUpdateVehicle(Long id, VehiclePatch vehiclePatch) {
        Vehicle existing = store.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle merged = VehicleValidator.applyPatch(existing, vehiclePatch);
        Vehicle updated = store.update(id, merged)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        return Response.ok(updated).build();
    }

    @Override
    public Response deleteVehicle(Long id) {
        if (!store.deleteById(id)) {
            throw ApiException.gone("Объект с id=" + id + " не найден или уже удалён");
        }
        return Response.noContent().build();
    }

    @Override
    public Response sumEnginePower() {
        double sum = store.findAll().stream()
                .mapToDouble(Vehicle::getEnginePower)
                .sum();
        return Response.ok(new SumResult().sum(sum)).build();
    }

    @Override
    public Response groupCountById() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Vehicle vehicle : store.findAll()) {
            counts.merge(vehicle.getId(), 1L, Long::sum);
        }
        IdGroupCount result = new IdGroupCount();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            result.addEntriesItem(new IdGroupCountEntry().id(entry.getKey()).count(entry.getValue()));
        }
        return Response.ok(result).build();
    }

    @Override
    public Response vehiclesByTypeGreaterThan(VehicleType type) {
        List<Vehicle> vehicles = store.findAll().stream()
                .filter(vehicle -> vehicle.getType().ordinal() > type.ordinal())
                .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<Vehicle>>(vehicles) {
        }).build();
    }

    private void requirePositive(int value, String paramName) {
        if (value < 1) {
            throw ApiException.badRequest("Параметр " + paramName + " должен быть не меньше 1");
        }
    }
}
