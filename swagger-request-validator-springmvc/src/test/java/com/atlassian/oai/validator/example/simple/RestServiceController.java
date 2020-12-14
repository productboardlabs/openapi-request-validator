package com.atlassian.oai.validator.example.simple;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping(value = "/spring", produces = "application/json")
public class RestServiceController {

    private static boolean sendInvalidResponse() {
        return "true".equals(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getHeader("invalidResponse"));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{pathVariable}", produces = "application/json")
    public Map<String, Object> get(@RequestHeader("headerValue") final String headerValue,
                                   @PathVariable("pathVariable") final String pathVariable,
                                   @RequestParam("requestParam") final String requestParam) {
        if (sendInvalidResponse()) {
            return Collections.emptyMap();
        }
        return ImmutableMap.of("headerValue", headerValue, "pathVariable", pathVariable, "requestParam", requestParam);
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> post(@RequestBody final Map<String, Object> body) {
        if (sendInvalidResponse()) {
            return Collections.emptyMap();
        }
        return body;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{pathVariable}", produces = "application/json")
    public Map<String, Object> put(@RequestBody final Map<String, Object> body,
                                   @PathVariable("pathVariable") final String pathVariable) {
        if (sendInvalidResponse()) {
            return Collections.emptyMap();
        }
        return new ImmutableMap.Builder<String, Object>().putAll(body).put("pathVariable", pathVariable).build();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{pathVariable}", produces = "application/json")
    public ResponseEntity<Void> delete(@PathVariable("pathVariable") final String pathVariable) {
        if (sendInvalidResponse()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
