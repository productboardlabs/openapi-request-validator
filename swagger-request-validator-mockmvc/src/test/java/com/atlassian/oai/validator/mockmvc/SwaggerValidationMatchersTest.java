package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.mockmvc.SwaggerMatchers.SwaggerValidationException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.atlassian.oai.validator.mockmvc.SwaggerValidatorMatchers.swagger;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SwaggerValidationMatchersTest {

    private MockMvc mvc;

    @Before
    public void setup() {
        final TestController testController = new TestController();
        mvc = MockMvcBuilders.standaloneSetup(testController).build();
    }

    @Test(expected = NullPointerException.class)
    public void create_withNullString_throwsException() throws Exception {
        this.mvc
                .perform(get("/path"))
                .andExpect(status().isOk())
                .andExpect(swagger().isValid(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withEmpty_throwsException() throws Exception {
        this.mvc
                .perform(get("/path"))
                .andExpect(status().isOk())
                .andExpect(swagger().isValid(""));
    }

    @Test
    public void match_returnsResponse_ifValidationSucceeds() throws Exception {
        this.mvc
                .perform(get("/hello/bob"))
                .andExpect(status().isOk())
                .andExpect(swagger().isValid("api.json"))
                .andExpect(content().string("{\"message\":\"Hello bob!\"}"));
    }

    @Test
    public void match_returnsResponse_ifValidationSucceedsEmptyBody() throws Exception {
        this.mvc
                .perform(get("/hello/empty"))
                .andExpect(status().isNoContent())
                .andExpect(swagger().isValid("api.json"))
                .andExpect(content().string(""));
    }

    @Test (expected = SwaggerValidationException.class)
    public void match_throwsException_ifValidationFails() throws  Exception {
        this.mvc
                .perform(get("/hello/bill"))
                .andExpect(status().isOk())
                .andExpect(swagger().isValid("api.json"))
                .andExpect(content().string("{\"msg\":\"Hello bill!\"}")); // Wrong field name
    }

    @Test (expected = SwaggerValidationException.class)
    public void match_validationTakesMethodIntoAccount() throws Exception {
        this.mvc
                .perform(post("/hello/bob"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(swagger().isValid("api.json"))
                .andExpect(content().string("{\"message\":\"Hello bob!\"}"));
    }
}
