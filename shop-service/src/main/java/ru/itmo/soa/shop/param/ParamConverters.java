package ru.itmo.soa.shop.param;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.soa.shop.exception.ApiException;
import ru.itmo.soa.shop.model.VehicleType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

@Provider
public class ParamConverters implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        String paramName = paramName(annotations);
        if (rawType == Double.class) {
            return (ParamConverter<T>) numeric(Double::valueOf, paramName);
        }
        if (rawType == VehicleType.class) {
            return (ParamConverter<T>) new ParamConverter<VehicleType>() {
                @Override
                public VehicleType fromString(String value) {
                    try {
                        return VehicleType.fromValue(value);
                    } catch (IllegalArgumentException e) {
                        throw ApiException.unprocessableEntity("Значение " + paramName + " вне допустимого перечня: " + value);
                    }
                }

                @Override
                public String toString(VehicleType value) {
                    return value.toString();
                }
            };
        }
        return null;
    }

    private static <N extends Number> ParamConverter<N> numeric(Function<String, N> parser, String paramName) {
        return new ParamConverter<>() {
            @Override
            public N fromString(String value) {
                try {
                    return parser.apply(value);
                } catch (NumberFormatException e) {
                    throw ApiException.badRequest("Нечисловое значение параметра " + paramName + ": " + value);
                }
            }

            @Override
            public String toString(N value) {
                return value.toString();
            }
        };
    }

    private static String paramName(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof QueryParam queryParam) {
                return queryParam.value();
            }
            if (annotation instanceof PathParam pathParam) {
                return pathParam.value();
            }
        }
        return "";
    }
}
