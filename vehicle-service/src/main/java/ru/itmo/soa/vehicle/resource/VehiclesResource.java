package ru.itmo.soa.vehicle.resource;

import jakarta.inject.Inject;
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
import ru.itmo.soa.vehicle.query.VehicleQuery;
import ru.itmo.soa.vehicle.query.VehicleQueryParser;
import ru.itmo.soa.vehicle.store.Page;
import ru.itmo.soa.vehicle.store.VehicleEntity;
import ru.itmo.soa.vehicle.store.VehicleMapper;
import ru.itmo.soa.vehicle.store.VehicleRepository;
import ru.itmo.soa.vehicle.validation.VehicleValidator;

import java.util.List;

public class VehiclesResource implements VehiclesApi {

    @Inject
    private VehicleRepository repository;

    @Inject

    @Context
    private UriInfo uriInfo;

    @Override
    public Response createVehicle(VehicleInput vehicleInput) {
        Vehicle model = VehicleValidator.toNewVehicle(vehicleInput);
        VehicleEntity saved = repository.save(VehicleMapper.toEntity(model));
        java.net.URI location = uriInfo.getBaseUriBuilder()
                .path(VehiclesApi.class)
                .path(VehiclesApi.class, "getVehicleById")
                .build(saved.getId());
        return Response.created(location).entity(VehicleMapper.toModel(saved)).build();
    }

    @Override
    public Response getVehicles(Integer pageNumber, Integer pageSize, List<String> sort, List<String> filter) {
        requirePositive(pageNumber, "pageNumber");
        requirePositive(pageSize, "pageSize");

        VehicleQuery query = VehicleQueryParser.parse(sort, filter);
        Page page = repository.search(pageNumber, pageSize, query);

        VehiclePage result = new VehiclePage();
        result.setPageNumber(pageNumber);
        result.setPageSize(pageSize);
        result.setTotalElements(page.totalElements());
        result.setTotalPages((int) Math.ceil((double) page.totalElements() / pageSize));
        result.setItems(page.models());
        return Response.ok(result).build();
    }

    @Override
    public Response getVehicleById(Long id) {
        Vehicle vehicle = repository.findById(id)
                .map(VehicleMapper::toModel)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        return Response.ok(vehicle).build();
    }

    @Override
    public Response updateVehicle(Long id, VehicleInput vehicleInput) {
        VehicleEntity existing = repository.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle model = VehicleValidator.fromInputForReplace(vehicleInput);
        VehicleEntity replacement = VehicleMapper.toEntity(model);
        replacement.setId(id);
        replacement.setCreationDate(existing.getCreationDate());
        VehicleEntity updated = repository.save(replacement);
        return Response.ok(VehicleMapper.toModel(updated)).build();
    }

    @Override
    public Response partialUpdateVehicle(Long id, VehiclePatch vehiclePatch) {
        VehicleEntity existing = repository.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle merged = VehicleValidator.applyPatch(VehicleMapper.toModel(existing), vehiclePatch);
        VehicleEntity updated = repository.save(VehicleMapper.apply(existing, merged));
        return Response.ok(VehicleMapper.toModel(updated)).build();
    }

    @Override
    public Response deleteVehicle(Long id) {
        VehicleEntity existing = repository.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден или уже удалён"));
        repository.delete(existing);
        return Response.noContent().build();
    }

    @Override
    public Response sumEnginePower() {
        return Response.ok(new SumResult().sum(repository.sumEnginePower())).build();
    }

    @Override
    public Response groupCountById() {
        IdGroupCount result = new IdGroupCount();
        repository.groupCountById().forEach((id, count) ->
                result.addEntriesItem(new IdGroupCountEntry().id(id).count(count)));
        return Response.ok(result).build();
    }

    @Override
    public Response vehiclesByTypeGreaterThan(VehicleType type) {
        List<Vehicle> vehicles = repository.findByTypeGreaterThan(type).stream()
                .map(VehicleMapper::toModel)
                .toList();
        return Response.ok(new GenericEntity<List<Vehicle>>(vehicles) {
        }).build();
    }

    private void requirePositive(int value, String paramName) {
        if (value < 1) {
            throw ApiException.badRequest("Параметр " + paramName + " должен быть не меньше 1");
        }
    }
}
