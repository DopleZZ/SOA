package ru.itmo.soa.shop;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import ru.itmo.soa.shop.exception.ApiExceptionMapper;
import ru.itmo.soa.shop.param.ParamConverters;
import ru.itmo.soa.shop.resource.ShopResource;

import java.util.Set;

@ApplicationPath("/")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(ShopResource.class, ApiExceptionMapper.class, ParamConverters.class);
    }
}
