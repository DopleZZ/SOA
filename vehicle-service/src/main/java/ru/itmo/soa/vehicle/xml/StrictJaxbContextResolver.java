package ru.itmo.soa.vehicle.xml;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Provider
@Produces("application/xml")
public class StrictJaxbContextResolver implements ContextResolver<JAXBContext> {

    private final Map<Class<?>, JAXBContext> cache = new ConcurrentHashMap<>();

    @Override
    public JAXBContext getContext(Class<?> type) {
        return cache.computeIfAbsent(type, StrictJaxbContextResolver::create);
    }

    private static JAXBContext create(Class<?> type) {
        try {
            return new StrictContext(JAXBContext.newInstance(type));
        } catch (JAXBException e) {
            throw new IllegalStateException("Не удалось создать JAXBContext для " + type, e);
        }
    }

    private static final class StrictContext extends JAXBContext {

        private final JAXBContext delegate;

        private StrictContext(JAXBContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public Unmarshaller createUnmarshaller() throws JAXBException {
            Unmarshaller unmarshaller = delegate.createUnmarshaller();
            unmarshaller.setEventHandler(event -> event.getLinkedException() == null);
            return unmarshaller;
        }

        @Override
        public Marshaller createMarshaller() throws JAXBException {
            return delegate.createMarshaller();
        }
    }
}
