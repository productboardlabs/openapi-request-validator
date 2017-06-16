//CHECKSTYLE:OFF // HideUtilityClassConstructor: this "utility" class has to have a public constructor for starting the test web service
package com.atlassian.oai.validator.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SwaggerRequestValidationApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SwaggerRequestValidationApplication.class, args);
    }
}
//CHECKSTYLE:ON
