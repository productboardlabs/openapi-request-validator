package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PatternValidationTest {

    private static SwaggerRequestResponseValidator createValidator(final String schema) {
        return SwaggerRequestResponseValidator.createFor(schema).build();
    }

    private static SimpleRequest createTestRequest(final String patternInline, final String patternByRef) {
        return new SimpleRequest.Builder(Request.Method.POST, "/pattern")
                .withBody("{\n" +
                        "\"patternInline\": \"" + patternInline + "\",\n" +
                        "\"patternByRef\": \"" + patternByRef + "\"\n" +
                        "}")
                .build();
    }

    private static SimpleRequest createInvalidTestRequest() {
        return createTestRequest("foo", "foo");
    }

    private static SimpleRequest createValidTestRequest() {
        return createTestRequest("ba", "ab");
    }

    @Test
    public void test_invalidPatternValues_v2() {
        // setup:
        final Request request = createInvalidTestRequest();
        final SwaggerRequestResponseValidator classUnderTest = createValidator("/oai/api-with-pattern.json");

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(true));

        // and: 'both fields have pattern validation errors'
        final Map<String, String> pathKeyMap = result.getMessages().stream()
                .collect(Collectors.toMap(
                    // get the path for the failing pattern fields
                    message -> StringUtils.removePattern(message.getMessage(), "(.*\\[|\\].*)"),
                    ValidationReport.Message::getKey
                ));
        assertThat(pathKeyMap, equalTo(
                ImmutableMap.of(
                        "Path '/patternInline'", "validation.schema.pattern",
                        "Path '/patternByRef'", "validation.schema.pattern"
                )
        ));
    }

    @Test
    public void test_validPatternValues_v2() {
        // setup:
        final Request request = createValidTestRequest();
        final SwaggerRequestResponseValidator classUnderTest = createValidator("/oai/api-with-pattern.json");

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(false));
    }
}
