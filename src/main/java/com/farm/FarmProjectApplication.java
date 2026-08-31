package com.farm;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FarmProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarmProjectApplication.class, args);
	}

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
		return factory -> factory.addConnectorCustomizers((Connector connector) -> {
			// This is the specific setting for Tomcat 11+
			// It allows up to 100 different parts in one form submit
			connector.setProperty("maxMultipartNames", "100");
		});
	}
}