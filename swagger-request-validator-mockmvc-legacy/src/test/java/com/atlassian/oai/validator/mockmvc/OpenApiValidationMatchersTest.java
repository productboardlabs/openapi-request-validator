package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiMatchers.OpenApiValidationException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OpenApiValidationMatchersTest {

    private MockMvc mvc;

    @Before
    public void setup() {
        final TestController testController = new TestController();
        mvc = MockMvcBuilders.standaloneSetup(testController).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withNullString_throwsException() throws Exception {
        mvc
                .perform(get("/path"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid((String) null));
    }

    @Test(expected = Exception.class)
    public void create_withEmpty_throwsException() throws Exception {
        mvc
                .perform(get("/path"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(""));
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

    @Test(expected = OpenApiValidationException.class)
    public void match_throwsException_ifValidationFails() throws Exception {
        mvc
                .perform(get("/hello/bill"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("api.json"))
                .andExpect(content().string("{\"msg\":\"Hello bill!\"}")); // Wrong field name
    }

    @Test(expected = OpenApiValidationException.class)
    public void match_validationTakesMethodIntoAccount() throws Exception {
        mvc
                .perform(post("/hello/bob"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(openApi().isValid("api.json"))
                .andExpect(content().string("{\"message\":\"Hello bob!\"}"));
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
