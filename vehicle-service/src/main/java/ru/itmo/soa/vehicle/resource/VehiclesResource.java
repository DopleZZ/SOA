package ru.itmo.soa.vehicle.resource;

import jakarta.ws.rs.core.Context;
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
    public VehiclePage getVehicles(String pageNumberRaw, String pageSizeRaw, List<String> sort, List<String> filter) {
        int pageNumber = parsePositiveInt(pageNumberRaw, "pageNumber");
        int pageSize = parsePositiveInt(pageSizeRaw, "pageSize");

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
        return page;
    }

    @Override
    public Vehicle getVehicleById(Long id) {
        return store.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
    }

    @Override
    public Vehicle updateVehicle(Long id, VehicleInput vehicleInput) {
        store.findById(id).orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle replacement = VehicleValidator.fromInputForReplace(vehicleInput);
        return store.update(id, replacement)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
    }

    @Override
    public Vehicle partialUpdateVehicle(Long id, VehiclePatch vehiclePatch) {
        Vehicle existing = store.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle merged = VehicleValidator.applyPatch(existing, vehiclePatch);
        return store.update(id, merged)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
    }

    @Override
    public void deleteVehicle(Long id) {
        if (!store.deleteById(id)) {
            throw ApiException.gone("Объект с id=" + id + " не найден или уже удалён");
        }
    }

    @Override
    public SumResult sumEnginePower() {
        double sum = store.findAll().stream()
                .mapToDouble(Vehicle::getEnginePower)
                .sum();
        return new SumResult(sum);
    }

    @Override
    public IdGroupCount groupCountById() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Vehicle vehicle : store.findAll()) {
            counts.merge(vehicle.getId(), 1L, Long::sum);
        }
        IdGroupCount result = new IdGroupCount();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            result.getEntries().add(new IdGroupCountEntry(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @Override
    public List<Vehicle> vehiclesByTypeGreaterThan(String type) {
        VehicleType threshold;
        try {
            threshold = VehicleType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessableEntity("Значение type вне допустимого перечня: " + type);
        }
        return store.findAll().stream()
                .filter(vehicle -> vehicle.getType().ordinal() > threshold.ordinal())
                .collect(Collectors.toList());
    }

    private int parsePositiveInt(String raw, String paramName) {
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Нечисловое значение параметра " + paramName + ": " + raw);
        }
        if (value < 1) {
            throw ApiException.badRequest("Параметр " + paramName + " должен быть не меньше 1");
        }
        return value;
    }
}
