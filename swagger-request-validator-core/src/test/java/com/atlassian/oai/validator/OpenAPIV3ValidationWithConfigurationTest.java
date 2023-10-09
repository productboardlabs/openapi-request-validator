package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.response.ResponseValidator;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.atlassian.oai.validator.schema.ValidationConfiguration;
import org.junit.Test;

import java.lang.reflect.Field;

import static com.atlassian.oai.validator.model.Request.Method.GET;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OpenAPIV3ValidationWithConfigurationTest {

    @Test
    public void validateWithoutConfiguration() throws NoSuchFieldException, IllegalAccessException {
        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-users.yaml").build();
        final Response response = SimpleResponse.Builder
            .ok()
            .withContentType("application/json")
            .withBody(loadJsonResponse("users-valid"))
            .build();

        assertPass(classUnderTest.validateResponse("/users", GET, response));
        final Field responseValidatorField = OpenApiInteractionValidator.class.getDeclaredField("responseValidator");
        final Field schemaValidatorField = ResponseValidator.class.getDeclaredField("schemaValidator");
        final Field validationConfigurationField = SchemaValidator.class.getDeclaredField("validationConfiguration");
        responseValidatorField.setAccessible(true);
        schemaValidatorField.setAccessible(true);
        validationConfigurationField.setAccessible(true);

        final ResponseValidator responseValidator = (ResponseValidator) responseValidatorField.get(classUnderTest);
        final SchemaValidator schemaValidator = (SchemaValidator) schemaValidatorField.get(responseValidator);

        final ValidationConfiguration validationConfiguration = (ValidationConfiguration) validationConfigurationField.get(schemaValidator);

        assertNotNull(validationConfiguration);
        assertEquals(validationConfiguration.getMaxCacheSize(), 100);
    }

    @Test
    public void validateWithConfiguration() throws NoSuchFieldException, IllegalAccessException {
        final ValidationConfiguration validationConfiguration = new ValidationConfiguration();
        validationConfiguration.setMaxCacheSize(2);
        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-users.yaml").withSchemaValidationConfiguration(validationConfiguration).build();
        final Response response = SimpleResponse.Builder
            .ok()
            .withContentType("application/json")
            .withBody(loadJsonResponse("users-valid"))
            .build();

        assertPass(classUnderTest.validateResponse("/users", GET, response));

        final Field responseValidatorField = OpenApiInteractionValidator.class.getDeclaredField("responseValidator");
        final Field schemaValidatorField = ResponseValidator.class.getDeclaredField("schemaValidator");
        final Field validationConfigurationField = SchemaValidator.class.getDeclaredField("validationConfiguration");
        responseValidatorField.setAccessible(true);
        schemaValidatorField.setAccessible(true);
        validationConfigurationField.setAccessible(true);

        final ResponseValidator responseValidator = (ResponseValidator) responseValidatorField.get(classUnderTest);
        final SchemaValidator schemaValidator = (SchemaValidator) schemaValidatorField.get(responseValidator);

        final ValidationConfiguration configurationValue = (ValidationConfiguration) validationConfigurationField.get(schemaValidator);

        assertNotNull(validationConfiguration);
        assertEquals(configurationValue.getMaxCacheSize(), 2);
    }

}
