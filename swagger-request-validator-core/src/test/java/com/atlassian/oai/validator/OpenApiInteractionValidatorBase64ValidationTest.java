package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class OpenApiInteractionValidatorBase64ValidationTest {

    private final OpenApiInteractionValidator swaggerValidator =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v2/api-string-byte-pattern.json").build();
    private final OpenApiInteractionValidator openApi3Validator =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-string-byte-pattern.yaml").build();

    private static SimpleRequest buildValidBase64Request() {
        return SimpleRequest.Builder
                .post("/test/QmFzZTY0/QXJyYXkx,QXJyYXky/QmFzZTY0/QXJyYXkx,QXJyYXky/a/aa,aaa/a/aa,aaa")
                .withContentType("application/json")
                .withQueryParam("queryByte", "QmFzZTY0")
                .withQueryParam("queryByteArray", "QXJyYXkx", "QXJyYXky")
                .withQueryParam("refQueryByte", "QmFzZTY0")
                .withQueryParam("refQueryByteArray", "QXJyYXkx", "QXJyYXky")
                .withQueryParam("queryPattern", "a")
                .withQueryParam("queryPatternArray", "aa", "aaa")
                .withQueryParam("refQueryPattern", "a")
                .withQueryParam("refQueryPatternArray", "aa", "aaa")
                .withHeader("headerByte", "QmFzZTY0")
                .withHeader("headerByteArray", "QXJyYXkx,QXJyYXky")
                .withHeader("refHeaderByte", "QmFzZTY0")
                .withHeader("refHeaderByteArray", "QXJyYXkx,QXJyYXky")
                .withHeader("headerPattern", "a")
                .withHeader("headerPatternArray", "aa,aaa")
                .withHeader("refHeaderPattern", "a")
                .withHeader("refHeaderPatternArray", "aa,aaa")
                .withBody("{\n" +
                        "  \"byte\": \"QmFzZTY0\",\n" +
                        "  \"byteArray\": [\n" +
                        "    \"QXJyYXkx\",\n" +
                        "    \"QXJyYXky\"\n" +
                        "  ],\n" +
                        "  \"pattern\": \"a\",\n" +
                        "  \"patternArray\": [\n" +
                        "    \"aa\",\n" +
                        "    \"aaa\"\n" +
                        "  ],\n" +
                        "  \"ref\": {\n" +
                        "    \"byte\": \"QmFzZTY0\",\n" +
                        "    \"byteArray\": [\n" +
                        "      \"QXJyYXkx\",\n" +
                        "      \"QXJyYXky\"\n" +
                        "    ],\n" +
                        "    \"pattern\": \"a\",\n" +
                        "    \"patternArray\": [\n" +
                        "      \"aa\",\n" +
                        "      \"aaa\"\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}")
                .build();
    }

    private static SimpleRequest buildInvalidBase64Request() {
        return SimpleRequest.Builder
                .post("/test/bb/c,d/b/c,d/b/c,d/b/c,d")
                .withContentType("application/json")
                .withQueryParam("queryByte", "b")
                .withQueryParam("queryByteArray", "c", "d")
                .withQueryParam("refQueryByte", "b")
                .withQueryParam("refQueryByteArray", "c", "d")
                .withQueryParam("queryPattern", "b")
                .withQueryParam("queryPatternArray", "c", "d")
                .withQueryParam("refQueryPattern", "b")
                .withQueryParam("refQueryPatternArray", "c", "d")
                .withHeader("headerByte", "b@@a")
                .withHeader("headerByteArray", "c,d")
                .withHeader("refHeaderByte", "bz!z")
                .withHeader("refHeaderByteArray", "c,d")
                .withHeader("headerPattern", "b")
                .withHeader("headerPatternArray", "c,d")
                .withHeader("refHeaderPattern", "b")
                .withHeader("refHeaderPatternArray", "c,d")
                .withBody("{\n" +
                        "  \"byte\": \"b\",\n" +
                        "  \"byteArray\": [\n" +
                        "    \"c\",\n" +
                        "    \"d\"\n" +
                        "  ],\n" +
                        "  \"pattern\": \"b\",\n" +
                        "  \"patternArray\": [\n" +
                        "    \"c\",\n" +
                        "    \"d\"\n" +
                        "  ],\n" +
                        "  \"ref\": {\n" +
                        "    \"byte\": \"b\",\n" +
                        "    \"byteArray\": [\n" +
                        "      \"c\",\n" +
                        "      \"d\"\n" +
                        "    ],\n" +
                        "    \"pattern\": \"b\",\n" +
                        "    \"patternArray\": [\n" +
                        "      \"c\",\n" +
                        "      \"d\"\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}")
                .build();
    }

    private static void assertInvalidBase64Result(final ValidationReport result) {
        final List<String> list = result.getMessages().stream()
                .map(message ->
                        message.getContext().get().getParameter()
                                .map(parameter -> parameter.getName() + ": " + message.getMessage())
                                .orElseGet(message::getMessage)
                )
                .collect(toList());
        Assert.assertThat(list, containsInAnyOrder(
                "headerByte: Not a valid base64 string (invalid character '@' at index 1)",
                "headerByteArray: Not a valid base64 string (invalid length: 1)",
                "headerByteArray: Not a valid base64 string (invalid length: 1)",
                "refHeaderByte: Not a valid base64 string (invalid character '!' at index 2)",
                "refHeaderByteArray: Not a valid base64 string (invalid length: 1)",
                "refHeaderByteArray: Not a valid base64 string (invalid length: 1)",
                "headerPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "headerPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "headerPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "refHeaderPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "refHeaderPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "refHeaderPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "pathByte: Not a valid base64 string (invalid length: 2)",
                "pathByteArray: Not a valid base64 string (invalid length: 1)",
                "pathByteArray: Not a valid base64 string (invalid length: 1)",
                "refPathByte: Not a valid base64 string (invalid length: 1)",
                "refPathByteArray: Not a valid base64 string (invalid length: 1)",
                "refPathByteArray: Not a valid base64 string (invalid length: 1)",
                "pathPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "pathPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "pathPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "refPathPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "refPathPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "refPathPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "[Path '/byte'] Not a valid base64 string (invalid length: 1)",
                "[Path '/byteArray/0'] Not a valid base64 string (invalid length: 1)",
                "[Path '/byteArray/1'] Not a valid base64 string (invalid length: 1)",
                "[Path '/pattern'] ECMA 262 regex \"a+\" does not match input string \"b\"",
                "[Path '/patternArray/0'] ECMA 262 regex \"a+\" does not match input string \"c\"",
                "[Path '/patternArray/1'] ECMA 262 regex \"a+\" does not match input string \"d\"",
                "[Path '/ref/byte'] Not a valid base64 string (invalid length: 1)",
                "[Path '/ref/byteArray/0'] Not a valid base64 string (invalid length: 1)",
                "[Path '/ref/byteArray/1'] Not a valid base64 string (invalid length: 1)",
                "[Path '/ref/pattern'] ECMA 262 regex \"a+\" does not match input string \"b\"",
                "[Path '/ref/patternArray/0'] ECMA 262 regex \"a+\" does not match input string \"c\"",
                "[Path '/ref/patternArray/1'] ECMA 262 regex \"a+\" does not match input string \"d\"",
                "queryByte: Not a valid base64 string (invalid length: 1)",
                "queryByteArray: Not a valid base64 string (invalid length: 1)",
                "queryByteArray: Not a valid base64 string (invalid length: 1)",
                "refQueryByte: Not a valid base64 string (invalid length: 1)",
                "refQueryByteArray: Not a valid base64 string (invalid length: 1)",
                "refQueryByteArray: Not a valid base64 string (invalid length: 1)",
                "queryPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "queryPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "queryPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "refQueryPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "refQueryPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "refQueryPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\""
        ));
    }

    @Test
    public void validBase64_Swagger() {
        // given:
        final Request request = buildValidBase64Request();

        // when:
        final ValidationReport result = swaggerValidator.validateRequest(request);

        // then:
        assertPass(result);
    }

    @Test
    public void validBase64_OpenApi3() {
        // given:
        final Request request = buildValidBase64Request();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
    }

    @Test
    public void invalidBase64_Swagger() {
        // given:
        final Request request = buildInvalidBase64Request();

        // when:
        final ValidationReport result = swaggerValidator.validateRequest(request);

        // then:
        assertInvalidBase64Result(result);
    }

    @Test
    public void invalidBase64_OpenApi3() {
        // given:
        final Request request = buildInvalidBase64Request();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertInvalidBase64Result(result);
    }
}
