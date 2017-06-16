package com.atlassian.oai.validator.examples.mockmvc;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
public class PetController {
    @RequestMapping(value = "/pet/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String findPet(final @PathVariable(value = "id") String id) {
        return "{"
                + "  \"id\": 1,"
                + "  \"category\": {"
                + "    \"id\": 0,"
                + "    \"name\": \"string\""
                + "  },"
                + "  \"name\": \"doggie\","
                + "  \"photoUrls\": ["
                + "    \"string\""
                + "  ],"
                + "  \"tags\": ["
                + "    {"
                + "      \"id\": 2,"
                + "      \"name\": \"string\""
                + "    }"
                + "  ],"
                + "  \"status\": \"available\""
                + "}";
    }
}
