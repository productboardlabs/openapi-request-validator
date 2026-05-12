package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiMatchers.OpenApiValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OpenApiValidationMatchersTest {

    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        final TestController testController = new TestController();
        mvc = MockMvcBuilders.standaloneSetup(testController).build();
    }

    @Test
    public void create_withNullString_throwsException() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> mvc
                .perform(get("/path"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid((String) null))
        );
    }

    @Test
    public void create_withEmpty_throwsException() throws Exception {
        assertThrows(Exception.class, () -> mvc
                .perform(get("/path"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(""))
        );
    }

    @Test
    public void match_returnsResponse_ifValidationSucceeds() throws Exception {
        mvc
                .perform(get("/hello/bob"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("api.json"))
                .andExpect(content().string("{\"message\":\"Hello bob!\"}"));
    }

    @Test
    public void match_returnsResponse_ifValidationSucceedsEmptyBody() throws Exception {
        mvc
                .perform(get("/hello/empty"))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("api.json"))
                .andExpect(content().string(""));
    }

    @Test
    public void match_throwsException_ifValidationFails() throws Exception {
        assertThrows(OpenApiValidationException.class, () -> mvc
                .perform(get("/hello/bill"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("api.json"))
                .andExpect(content().string("{\"msg\":\"Hello bill!\"}")) // Wrong field name
        );
    }

    @Test
    public void match_validationTakesMethodIntoAccount() throws Exception {
        assertThrows(OpenApiValidationException.class, () -> mvc
                .perform(post("/hello/bob"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(openApi().isValid("api.json"))
                .andExpect(content().string("{\"message\":\"Hello bob!\"}"))
        );
    }

    @Test
    public void match_canUsePreConfiguredValidator() throws Exception {
        mvc
                .perform(get("/hello/bob"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OpenApiInteractionValidator.createForSpecificationUrl("api.json").build()))
                .andExpect(content().string("{\"message\":\"Hello bob!\"}"));
    }
}
