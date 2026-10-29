package ru.itmo.soa.shop.xml;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StrictJaxbHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final Map<Class<?>, JAXBContext> contexts = new ConcurrentHashMap<>();

    public StrictJaxbHttpMessageConverter() {
        super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.isAnnotationPresent(javax.xml.bind.annotation.XmlRootElement.class);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try {
            byte[] body = inputMessage.getBody().readAllBytes();
            Unmarshaller unmarshaller = context(clazz).createUnmarshaller();
            unmarshaller.setEventHandler(event -> event.getLinkedException() == null);
            return unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(body)));
        } catch (JAXBException e) {
            throw new HttpMessageNotReadableException("Невалидный XML в теле запроса", e);
        }
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try {
            Marshaller marshaller = context(object.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            Writer writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8);
            marshaller.marshal(object, writer);
            writer.flush();
        } catch (JAXBException e) {
            throw new HttpMessageNotWritableException("Не удалось сериализовать объект в XML", e);
        }
    }

    @Override
    protected MediaType getDefaultContentType(Object object) {
        return MediaType.APPLICATION_XML;
    }

    @Override
    protected void addDefaultHeaders(HttpHeaders headers, Object object, MediaType contentType) {
        headers.setContentType(MediaType.APPLICATION_XML);
    }

    private JAXBContext context(Class<?> type) {
        return contexts.computeIfAbsent(type, t -> {
            try {
                return JAXBContext.newInstance(t);
            } catch (JAXBException e) {
                throw new IllegalStateException("Не удалось создать JAXBContext для " + t, e);
            }
        });
    }
}
