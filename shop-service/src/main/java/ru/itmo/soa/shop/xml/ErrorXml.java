package ru.itmo.soa.shop.xml;

import ru.itmo.soa.shop.model.Error;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public final class ErrorXml {

    private static final JAXBContext CONTEXT = createContext();

    private ErrorXml() {
    }

    public static String write(Error error) {
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(error, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Не удалось сериализовать Error в XML", e);
        }
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(Error.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Не удалось создать JAXBContext для Error", e);
        }
    }
}
