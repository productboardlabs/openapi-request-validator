package com.atlassian.oai.validator.mockmvc;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @RequestMapping(value = "/path")
    public String path() {
        return "greeting";
    }

    @RequestMapping(value = "/hello/{name}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<String> hello(final @PathVariable(value = "name") String name) {
        if ("bill".equalsIgnoreCase(name)) {
            return new ResponseEntity<>("{\"msg\":\"Hello bill!\"}", HttpStatus.OK); // Invalid response
        } else if ("empty".equalsIgnoreCase(name)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Empty response
        } else {
            return new ResponseEntity<>("{\"message\":\"Hello " + name + "!\"}", HttpStatus.OK);
        }
    }
}
