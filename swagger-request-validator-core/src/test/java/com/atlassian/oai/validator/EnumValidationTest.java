package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EnumValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator.createFor("/oai/api-with-enum.yaml").build();

    @Test
    public void test_validEnumStringValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumString/1")
                .withHeader("headerEnum", "10")
                .withQueryParam("queryEnum", "100")
                .withBody("{\n" +
                        "\"innerEnum\": \"1000\",\n" +
                        "\"arrayWithInnerEnum\": [\"10000\"],\n" +
                        "\"arrayWithOuterEnum\": [\"100000\"]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(false));
    }

    @Test
    public void test_invalidEnumStringValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumString/4")
                .withHeader("headerEnum", "40")
                .withQueryParam("queryEnum", "400")
                .withBody("{\n" +
                        "\"innerEnum\": \"4000\",\n" +
                        "\"arrayWithInnerEnum\": [\"40000\"],\n" +
                        "\"arrayWithOuterEnum\": [\"400000\"]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getMessages(), hasSize(6));
        assertThat(
                result.getMessages().stream()
                        .map(ValidationReport.Message::getMessage).sorted()
                        .collect(Collectors.toList()),
                contains(
                        "Value '4' for parameter 'pathEnum' is not allowed. Allowed values are <[1, 2]>.",
                        "Value '40' for parameter 'headerEnum' is not allowed. Allowed values are <[10, 20]>.",
                        "Value '400' for parameter 'queryEnum' is not allowed. Allowed values are <[100, 200]>.",
                        "[Path '/arrayWithInnerEnum/0'] Instance value (\"40000\") not found in enum (possible values: [\"10000\",\"20000\"])",
                        "[Path '/arrayWithOuterEnum/0'] Instance value (\"400000\") not found in enum (possible values: [\"100000\",\"200000\"])",
                        "[Path '/innerEnum'] Instance value (\"4000\") not found in enum (possible values: [\"1000\",\"2000\"])"
                )
        );
    }

    @Test
    public void test_validEnumIntegerValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumInteger/1")
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
        assertThat(result.hasErrors(), is(false));
    }

    @Test
    public void test_invalidEnumIntegerValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumInteger/4")
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
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getMessages(), hasSize(6));
        assertThat(
                result.getMessages().stream()
                        .map(ValidationReport.Message::getMessage).sorted()
                        .collect(Collectors.toList()),
                contains(
                        "Value '4' for parameter 'pathEnum' is not allowed. Allowed values are <[1, 2]>.",
                        "Value '40' for parameter 'headerEnum' is not allowed. Allowed values are <[10, 20]>.",
                        "Value '400' for parameter 'queryEnum' is not allowed. Allowed values are <[100, 200]>.",
                        "[Path '/arrayWithInnerEnum/0'] Instance value (40000) not found in enum (possible values: [10000,20000])",
                        "[Path '/arrayWithOuterEnum/0'] Instance value (400000) not found in enum (possible values: [100000,200000])",
                        "[Path '/innerEnum'] Instance value (4000) not found in enum (possible values: [1000,2000])"
                )
        );
    }

    @Test
    public void test_validEnumLongValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumLong/-1")
                .withHeader("headerEnum", "-10")
                .withQueryParam("queryEnum", "-100")
                .withBody("{\n" +
                        "\"innerEnum\": -1000,\n" +
                        "\"arrayWithInnerEnum\": [-10000],\n" +
                        "\"arrayWithOuterEnum\": [-100000]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(false));
    }

    @Test
    public void test_invalidEnumLongValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumLong/-4")
                .withHeader("headerEnum", "-40")
                .withQueryParam("queryEnum", "-400")
                .withBody("{\n" +
                        "\"innerEnum\": -4000,\n" +
                        "\"arrayWithInnerEnum\": [-40000],\n" +
                        "\"arrayWithOuterEnum\": [-400000]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getMessages(), hasSize(6));
        assertThat(
                result.getMessages().stream()
                        .map(ValidationReport.Message::getMessage).sorted()
                        .collect(Collectors.toList()),
                contains(
                        "Value '-4' for parameter 'pathEnum' is not allowed. Allowed values are <[-1, -2]>.",
                        "Value '-40' for parameter 'headerEnum' is not allowed. Allowed values are <[-10, -20]>.",
                        "Value '-400' for parameter 'queryEnum' is not allowed. Allowed values are <[-100, -200]>.",
                        "[Path '/arrayWithInnerEnum/0'] Instance value (-40000) not found in enum (possible values: [-10000,-20000])",
                        "[Path '/arrayWithOuterEnum/0'] Instance value (-400000) not found in enum (possible values: [-100000,-200000])",
                        "[Path '/innerEnum'] Instance value (-4000) not found in enum (possible values: [-1000,-2000])"
                )
        );
    }

    @Test
    public void test_validEnumFloatValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumFloat/0.1")
                .withHeader("headerEnum", "0.01")
                .withQueryParam("queryEnum", "0.001")
                .withBody("{\n" +
                        "\"innerEnum\": 0.0001,\n" +
                        "\"arrayWithInnerEnum\": [0.00001],\n" +
                        "\"arrayWithOuterEnum\": [0.000001]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(false));
    }

    @Test
    public void test_invalidEnumFloatValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumFloat/0.4")
                .withHeader("headerEnum", "0.04")
                .withQueryParam("queryEnum", "0.004")
                .withBody("{\n" +
                        "\"innerEnum\": 0.0004,\n" +
                        "\"arrayWithInnerEnum\": [0.00004],\n" +
                        "\"arrayWithOuterEnum\": [0.000004]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getMessages(), hasSize(6));
        assertThat(
                result.getMessages().stream()
                        .map(ValidationReport.Message::getMessage).sorted()
                        .collect(Collectors.toList()),
                contains(
                        "Value '0.004' for parameter 'queryEnum' is not allowed. Allowed values are <[0.001, 0.002]>.",
                        "Value '0.04' for parameter 'headerEnum' is not allowed. Allowed values are <[0.01, 0.02]>.",
                        "Value '0.4' for parameter 'pathEnum' is not allowed. Allowed values are <[0.1, 0.2]>.",
                        "[Path '/arrayWithInnerEnum/0'] Instance value (4.0E-5) not found in enum (possible values: [1.0E-5,2.0E-5])",
                        "[Path '/arrayWithOuterEnum/0'] Instance value (4.0E-6) not found in enum (possible values: [1.0E-6,2.0E-6])",
                        "[Path '/innerEnum'] Instance value (4.0E-4) not found in enum (possible values: [1.0E-4,2.0E-4])"
                )
        );
    }

    @Test
    public void test_validEnumDoubleValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumDouble/-0.1")
                .withHeader("headerEnum", "-0.01")
                .withQueryParam("queryEnum", "-0.001")
                .withBody("{\n" +
                        "\"innerEnum\": -0.0001,\n" +
                        "\"arrayWithInnerEnum\": [-0.00001],\n" +
                        "\"arrayWithOuterEnum\": [-0.000001]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(false));
    }

    @Test
    public void test_invalidEnumDoubleValues() {
        // setup:
        final Request request = new SimpleRequest.Builder(Request.Method.POST, "/enumDouble/-0.4")
                .withHeader("headerEnum", "-0.04")
                .withQueryParam("queryEnum", "-0.004")
                .withBody("{\n" +
                        "\"innerEnum\": -0.0004,\n" +
                        "\"arrayWithInnerEnum\": [-0.00004],\n" +
                        "\"arrayWithOuterEnum\": [-0.000004]\n" +
                        "}")
                .build();

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getMessages(), hasSize(6));
        assertThat(
                result.getMessages().stream()
                        .map(ValidationReport.Message::getMessage).sorted()
                        .collect(Collectors.toList()),
                contains(
                        "Value '-0.004' for parameter 'queryEnum' is not allowed. Allowed values are <[-0.001, -0.002]>.",
                        "Value '-0.04' for parameter 'headerEnum' is not allowed. Allowed values are <[-0.01, -0.02]>.",
                        "Value '-0.4' for parameter 'pathEnum' is not allowed. Allowed values are <[-0.1, -0.2]>.",
                        "[Path '/arrayWithInnerEnum/0'] Instance value (-4.0E-5) not found in enum (possible values: [-1.0E-5,-2.0E-5])",
                        "[Path '/arrayWithOuterEnum/0'] Instance value (-4.0E-6) not found in enum (possible values: [-1.0E-6,-2.0E-6])",
                        "[Path '/innerEnum'] Instance value (-4.0E-4) not found in enum (possible values: [-1.0E-4,-2.0E-4])"
                )
        );
    }
}
