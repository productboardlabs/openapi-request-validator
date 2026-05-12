package com.atlassian.oai.validator.example.simple;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{pathVariable}")
    public Map<String, Object> get(@RequestHeader("headerValue") final String headerValue,
                                   @PathVariable("pathVariable") final String pathVariable,
                                   @RequestParam("requestParam") final String requestParam) {
        if (sendInvalidResponse()) {
            return Collections.emptyMap();
        }
        return Map.of("headerValue", headerValue, "pathVariable", pathVariable, "requestParam", requestParam);
    }

    @PostMapping(consumes = "application/json")
    public Map<String, Object> post(@RequestBody final Map<String, Object> body) {
        if (sendInvalidResponse()) {
            return Collections.emptyMap();
        }
        return body;
    }

    @PutMapping("/{pathVariable}")
    public Map<String, Object> put(@RequestBody final Map<String, Object> body,
                                   @PathVariable("pathVariable") final String pathVariable) {
        if (sendInvalidResponse()) {
            return Collections.emptyMap();
        }
        final java.util.HashMap<String, Object> result = new java.util.HashMap<>(body);
        result.put("pathVariable", pathVariable);
        return result;
    }

    @DeleteMapping("/{pathVariable}")
    public ResponseEntity<Void> delete(@PathVariable("pathVariable") final String pathVariable) {
        if (sendInvalidResponse()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/post/blob", consumes = "application/octet-stream")
    public Map<String, Object> post(@RequestBody final byte[] blob) {
        return Map.of("size", blob.length);
    }
}
