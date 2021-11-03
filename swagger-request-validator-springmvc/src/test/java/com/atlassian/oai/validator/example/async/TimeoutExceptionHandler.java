package com.atlassian.oai.validator.example.async;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.Map;

@ControllerAdvice
public class TimeoutExceptionHandler {
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Map<String, String>> handle() {
        return ResponseEntity.ok(
                ImmutableMap.of("headerValue", "timeout", "pathVariable", "timeout", "requestParam", "timeout")
        );
    }
}
