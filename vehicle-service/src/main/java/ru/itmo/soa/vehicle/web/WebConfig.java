package ru.itmo.soa.vehicle.web;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import ru.itmo.soa.vehicle.xml.StrictJaxbHttpMessageConverter;
import ru.itmo.soa.vehicle.xml.XmlCollectionHttpMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.http.MediaType;

@Configuration
public class WebConfig {

    @Bean
    public HttpMessageConverters httpMessageConverters() {
        return new HttpMessageConverters(false, Arrays.asList(converters()));
    }

    static HttpMessageConverter<?>[] converters() {
        return new HttpMessageConverter<?>[]{
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new XmlCollectionHttpMessageConverter(),
                new StrictJaxbHttpMessageConverter(),
                new WriteOnlyJsonHttpMessageConverter()
        };
    }

    static class WriteOnlyJsonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return false;
        }
    }
}
