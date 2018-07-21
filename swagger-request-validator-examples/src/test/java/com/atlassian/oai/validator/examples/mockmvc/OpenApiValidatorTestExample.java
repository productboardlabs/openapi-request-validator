package com.atlassian.oai.validator.examples.mockmvc;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OpenApiValidatorTestExample {
    private static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";

    /**
     * Test a GET with a valid request/response
     * <p>
     * This test is expected to PASS
     */
    @Test
    public void testGetValidPet() throws Exception {
        final PetController petController = new PetController();
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(petController).build();

        mockMvc
                .perform(get("/pet/1")
                .header("api_key", "API_KEY"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SWAGGER_JSON_URL))
                .andExpect(jsonPath("$.id", 1)
                        .value(equalTo(1)));
    }

    /**
     * Test a GET with an invalid request/response expectation.
     * <p>
     * This test will pass the business logic tests, but the validation filter will fail the test because even though
     * the server returned a valid result the request used a path parameter that does not match the schema defined
     * in the API specification.
     *
     * This test is expected to FAIL
     */
    @Test
    public void testGetWithInvalidId() throws Exception {
        final PetController petController = new PetController();
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(petController).build();

        mockMvc
                .perform(get("/pet/ONE")
                .header("api_key", "API_KEY"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SWAGGER_JSON_URL))
                .andExpect(jsonPath("$.id", 1)
                        .value(equalTo(1)));
    }
}
