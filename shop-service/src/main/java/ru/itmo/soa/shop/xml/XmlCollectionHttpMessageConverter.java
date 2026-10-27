package ru.itmo.soa.shop.xml;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XmlCollectionHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    private final Map<Class<?>, JAXBContext> contexts = new ConcurrentHashMap<>();

    public XmlCollectionHttpMessageConverter() {
        super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) {
        throw new UnsupportedOperationException("Чтение коллекций не поддерживается");
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Collection<?> items = (Collection<?>) object;
        Class<?> elementType = null;
        for (Object item : items) {
            elementType = item.getClass();
            break;
        }
        if (elementType == null) {
            writeDeclaration(outputMessage);
            return;
        }
        try {
            Marshaller marshaller = marshaller(elementType);
            Writer writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8);
            writer.write(XML_DECLARATION);
            for (Object item : items) {
                marshaller.marshal(item, writer);
            }
            writer.flush();
        } catch (JAXBException e) {
            throw new HttpMessageNotWritableException("Не удалось сериализовать коллекцию в XML", e);
        }
    }

    private void writeDeclaration(HttpOutputMessage outputMessage) throws IOException {
        Writer writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8);
        writer.write(XML_DECLARATION);
        writer.flush();
    }

    private Marshaller marshaller(Class<?> elementType) {
        try {
            JAXBContext context = contexts.computeIfAbsent(elementType, XmlCollectionHttpMessageConverter::newContext);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            return marshaller;
        } catch (JAXBException e) {
            throw new IllegalStateException("Не удалось создать marshaller для " + elementType, e);
        }
    }

    private static JAXBContext newContext(Class<?> type) {
        try {
            return JAXBContext.newInstance(type);
        } catch (JAXBException e) {
            throw new IllegalStateException("Не удалось создать JAXBContext для " + type, e);
        }
    }
}
