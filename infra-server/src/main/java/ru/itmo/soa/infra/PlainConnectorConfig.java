package ru.itmo.soa.infra;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlainConnectorConfig {

    private final int plainPort;

    public PlainConnectorConfig(@Value("${infra.config.port:0}") int plainPort) {
        this.plainPort = plainPort;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> plainConnectorCustomizer() {
        return factory -> {
            if (plainPort > 0) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setPort(plainPort);
                factory.addAdditionalTomcatConnectors(connector);
            }
        };
    }
}
