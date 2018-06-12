package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class EnumValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/schema/schema-with-enum.yaml").build();

    @Test
    public void testEnum() {
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

        // (!!) unfortunately fails with:
        // [Path '/arrayWithOuterEnum/0'] Instance value (100000) not found in enum (possible values: ["100000","200000","300000"]
    }
}
