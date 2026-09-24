package ru.itmo.soa.vehicle;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import ru.itmo.soa.vehicle.exception.ApiExceptionMapper;
import ru.itmo.soa.vehicle.resource.VehiclesResource;

import java.util.Set;

@ApplicationPath("/")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(VehiclesResource.class, ApiExceptionMapper.class);
    }
}
