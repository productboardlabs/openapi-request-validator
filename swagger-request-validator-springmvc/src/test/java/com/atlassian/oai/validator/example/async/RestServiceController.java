package com.atlassian.oai.validator.example.async;

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
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/spring", produces = "application/json")
public class RestServiceController {
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private static Boolean sendInvalidResponse() {
        return "true".equals(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getHeader("invalidResponse"));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{pathVariable}", produces = "application/json")
    public DeferredResult<Map<String, Object>> get(@RequestHeader("headerValue") final String headerValue,
                                   @PathVariable("pathVariable") final String pathVariable,
                                   @RequestParam("requestParam") final String requestParam) {
        if (sendInvalidResponse()) {
            return defer(Collections.emptyMap());
        }

        if (pathVariable.equals("timeout")) {
            return new DeferredResult<>(1L);
        }
        return defer(ImmutableMap.of("headerValue", headerValue, "pathVariable", pathVariable, "requestParam", requestParam));
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<Map<String, Object>> post(@RequestBody final Map<String, Object> body) {
        if (sendInvalidResponse()) {
            return defer(Collections.emptyMap());
        }
        return defer(body);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{pathVariable}", produces = "application/json")
    public DeferredResult<Map<String, Object>> put(@RequestBody final Map<String, Object> body,
                                   @PathVariable("pathVariable") final String pathVariable) {
        if (sendInvalidResponse()) {
            return defer(Collections.emptyMap());
        }
        return defer(new ImmutableMap.Builder<String, Object>().putAll(body).put("pathVariable", pathVariable).build());
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{pathVariable}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> delete(@PathVariable("pathVariable") final String pathVariable) {
        if (sendInvalidResponse()) {
            return defer(new ResponseEntity<>(HttpStatus.OK));
        }
        return defer(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    private static <T> DeferredResult<T> defer(final T t) {
        final DeferredResult<T> result = new DeferredResult<>(10_000L);
        executor.schedule(() -> result.setResult(t), 1L, TimeUnit.MILLISECONDS);
        return result;
    }
}
