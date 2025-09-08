package com.ikea.warehouse_data_consumer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Warehouse data consumer service")
                        .version("1.0.0")
                        .description("REST API for consuming warehouse inventory and product data from Kafka topics and storing it into the database.")
                        .contact(new Contact()
                                .name("Mert UNSAL")
                                .email("mertunsal0@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:".concat(serverPort)).description("Local development server"),
                        new Server().url("https://api.warehouse.ikea.com").description("Production server")
                ));
    }
}
