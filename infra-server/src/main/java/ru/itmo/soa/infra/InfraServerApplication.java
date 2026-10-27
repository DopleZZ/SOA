package ru.itmo.soa.infra;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigServer
@EnableEurekaServer
@EnableZuulProxy
public class InfraServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraServerApplication.class, args);
    }

    @Bean
    public static BeanPostProcessor zuulRoutesFirst() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof ZuulHandlerMapping) {
                    ((ZuulHandlerMapping) bean).setOrder(-1);
                }
                return bean;
            }
        };
    }
}
