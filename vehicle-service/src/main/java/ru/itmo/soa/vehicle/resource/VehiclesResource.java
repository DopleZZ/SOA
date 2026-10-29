package ru.itmo.soa.vehicle.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class VehiclesResource implements VehiclesApi {

    private final VehicleRepository repository;

    public VehiclesResource(VehicleRepository repository) {
        this.repository = repository;
    }

    @Override
    public ResponseEntity<Vehicle> createVehicle(VehicleInput vehicleInput) {
        Vehicle model = VehicleValidator.toNewVehicle(vehicleInput);
        VehicleEntity saved = repository.save(VehicleMapper.toEntity(model));
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(VehicleMapper.toModel(saved));
    }

    @Override
    public ResponseEntity<VehiclePage> getVehicles(Integer pageNumber, Integer pageSize, List<String> sort, List<String> filter) {
        requirePositive(pageNumber, "pageNumber");
        requirePositive(pageSize, "pageSize");

        VehicleQuery query = VehicleQueryParser.parse(rawValues("sort"), rawValues("filter"));
        Page page = repository.search(pageNumber, pageSize, query);

        VehiclePage result = new VehiclePage();
        result.setPageNumber(pageNumber);
        result.setPageSize(pageSize);
        result.setTotalElements(page.totalElements());
        result.setTotalPages((int) Math.ceil((double) page.totalElements() / pageSize));
        result.setItems(page.models());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Vehicle> getVehicleById(Long id) {
        Vehicle vehicle = repository.findById(id)
                .map(VehicleMapper::toModel)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        return ResponseEntity.ok(vehicle);
    }

    @Override
    public ResponseEntity<Vehicle> updateVehicle(Long id, VehicleInput vehicleInput) {
        VehicleEntity existing = repository.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle model = VehicleValidator.fromInputForReplace(vehicleInput);
        VehicleEntity replacement = VehicleMapper.toEntity(model);
        replacement.setId(id);
        replacement.setCreationDate(existing.getCreationDate());
        VehicleEntity updated = repository.save(replacement);
        return ResponseEntity.ok(VehicleMapper.toModel(updated));
    }

    @Override
    public ResponseEntity<Vehicle> partialUpdateVehicle(Long id, VehiclePatch vehiclePatch) {
        VehicleEntity existing = repository.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден"));
        Vehicle merged = VehicleValidator.applyPatch(VehicleMapper.toModel(existing), vehiclePatch);
        VehicleEntity updated = repository.save(VehicleMapper.apply(existing, merged));
        return ResponseEntity.ok(VehicleMapper.toModel(updated));
    }

    @Override
    public ResponseEntity<Void> deleteVehicle(Long id) {
        VehicleEntity existing = repository.findById(id)
                .orElseThrow(() -> ApiException.gone("Объект с id=" + id + " не найден или уже удалён"));
        repository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SumResult> sumEnginePower() {
        return ResponseEntity.ok(new SumResult().sum(repository.sumEnginePower()));
    }

    @Override
    public ResponseEntity<IdGroupCount> groupCountById() {
        IdGroupCount result = new IdGroupCount();
        repository.groupCountById().forEach((id, count) ->
                result.addEntriesItem(new IdGroupCountEntry().id(id).count(count)));
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<Vehicle>> vehiclesByTypeGreaterThan(VehicleType type) {
        List<Vehicle> vehicles = repository.findByTypeGreaterThan(type).stream()
                .map(VehicleMapper::toModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vehicles);
    }

    private void requirePositive(int value, String paramName) {
        if (value < 1) {
            throw ApiException.badRequest("Параметр " + paramName + " должен быть не меньше 1");
        }
    }

    private static List<String> rawValues(String name) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String[] values = attrs.getRequest().getParameterValues(name);
        return values != null ? Arrays.asList(values) : null;
    }
}
