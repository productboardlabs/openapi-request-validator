package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Collectors;

public class EnumValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/schema/schema-with-enum.yaml").build();

    @Test
    public void test_validEnumValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enum/1")
                .withHeader("headerEnum", "10")
                .withQueryParam("queryEnum", "100")
                .withBody("{\n" +
                        "\"innerEnum\": 1000,\n" +
                        "\"arrayWithInnerEnum\": [10000],\n" +
                        "\"arrayWithOuterEnum\": [100000]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        Assert.assertThat(result.hasErrors(), Matchers.is(false));
    }

    @Test
    public void test_invalidEnumValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enum/4")
                .withHeader("headerEnum", "40")
                .withQueryParam("queryEnum", "400")
                .withBody("{\n" +
                        "\"innerEnum\": 4000,\n" +
                        "\"arrayWithInnerEnum\": [40000],\n" +
                        "\"arrayWithOuterEnum\": [400000]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        Assert.assertThat(result.hasErrors(), Matchers.is(true));
        Assert.assertThat(result.getMessages(), Matchers.hasSize(6));
        Assert.assertThat(
                result.getMessages().stream()
                        .map(ValidationReport.Message::getMessage).sorted()
                        .collect(Collectors.toList()),
                Matchers.contains(
                        "Value '4' for parameter 'pathEnum' is not allowed. Allowed values are <[1, 2, 3]>.",
                        "Value '40' for parameter 'headerEnum' is not allowed. Allowed values are <[10, 20, 30]>.",
                        "Value '400' for parameter 'queryEnum' is not allowed. Allowed values are <[100, 200, 300]>.",
                        "[Path '/arrayWithInnerEnum/0'] Instance value (40000) not found in enum (possible values: [10000,20000,30000])",
                        "[Path '/arrayWithOuterEnum/0'] Instance value (400000) not found in enum (possible values: [100000,200000,300000])",
                        "[Path '/innerEnum'] Instance value (4000) not found in enum (possible values: [1000,2000,3000])"
                )
        );
    }
}
