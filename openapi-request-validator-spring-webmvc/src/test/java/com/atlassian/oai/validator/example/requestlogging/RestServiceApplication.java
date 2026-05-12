package com.atlassian.oai.validator.example.requestlogging;

import com.atlassian.oai.validator.example.simple.RestServiceController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RestServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
    }

    @Bean // reuse the controller of the simple example
    public RestServiceController restServiceController() {
        return new RestServiceController();
    }
}
