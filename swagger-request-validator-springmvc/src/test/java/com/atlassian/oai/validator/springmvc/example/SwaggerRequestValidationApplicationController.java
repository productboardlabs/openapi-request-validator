package com.atlassian.oai.validator.springmvc.example;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/spring")
public class SwaggerRequestValidationApplicationController {

    @RequestMapping(method = RequestMethod.GET, value = "/{pathVariable}")
    public Map<String, Object> get(@RequestHeader("headerValue") final String headerValue,
                                   @PathVariable("pathVariable") final String pathVariable,
                                   @RequestParam("requestParam") final String requestParam) {
        return ImmutableMap.of("headerValue", headerValue, "pathVariable", pathVariable, "requestParam", requestParam);
    }

    @RequestMapping(method = RequestMethod.POST)
    public Map<String, Object> post(@RequestBody final Map<String, Object> body) {
        return body;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{pathVariable}")
    public Map<String, Object> put(@RequestBody final Map<String, Object> body,
                                   @PathVariable("pathVariable") final String pathVariable) {
        return new ImmutableMap.Builder<String, Object>().putAll(body).put("pathVariable", pathVariable).build();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{pathVariable}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("pathVariable") final String pathVariable) {
        // nothing to do here
    }
}
