package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class SwaggerV2Base64ValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v2/api-string-byte-pattern.json").build();

    @Test
    @Ignore
    public void validBase64() {
        // given:
        final Request request = SimpleRequest.Builder
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
                .withHeader("headerByteArray", "QXJyYXkx", "QXJyYXky")
                .withHeader("refHeaderByte", "QmFzZTY0")
                .withHeader("refHeaderByteArray", "QXJyYXkx", "QXJyYXky")
                .withHeader("headerPattern", "a")
                .withHeader("headerPatternArray", "aa", "aaa")
                .withHeader("refHeaderPattern", "a")
                .withHeader("refHeaderPatternArray", "aa", "aaa")
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

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        assertPass(result);
    }

    @Test
    public void invalidBase64() {
        // given:
        final Request request = SimpleRequest.Builder
                .post("/test/b/c,d/b/c,d/b/c,d/b/c,d")
                .withContentType("application/json")
                .withQueryParam("queryByte", "b")
                .withQueryParam("queryByteArray", "c", "d")
                .withQueryParam("refQueryByte", "b")
                .withQueryParam("refQueryByteArray", "c", "d")
                .withQueryParam("queryPattern", "b")
                .withQueryParam("queryPatternArray", "c", "d")
                .withQueryParam("refQueryPattern", "b")
                .withQueryParam("refQueryPatternArray", "c", "d")
                .withHeader("headerByte", "b")
                .withHeader("headerByteArray", "c", "d")
                .withHeader("refHeaderByte", "b")
                .withHeader("refHeaderByteArray", "c", "d")
                .withHeader("headerPattern", "b")
                .withHeader("headerPatternArray", "c", "d")
                .withHeader("refHeaderPattern", "b")
                .withHeader("refHeaderPatternArray", "c", "d")
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

        // when:
        final ValidationReport result = classUnderTest.validateRequest(request);

        // then:
        final List<String> list = result.getMessages().stream()
                .map(message ->
                        message.getContext().get().getParameter()
                                .map(parameter -> parameter.getName() + ": " + message.getMessage())
                                .orElseGet(() -> message.getMessage())
                )
                .collect(Collectors.toList());
        Assert.assertThat(list, Matchers.containsInAnyOrder(
                "headerByte: Not a valid base64 string",
                "headerByteArray: Not a valid base64 string",
                "headerByteArray: Not a valid base64 string",
                "refHeaderByte: Not a valid base64 string",
                "refHeaderByteArray: Not a valid base64 string",
                "refHeaderByteArray: Not a valid base64 string",
                "headerPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "headerPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "headerPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "refHeaderPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "refHeaderPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "refHeaderPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "pathByte: Not a valid base64 string",
                "pathByteArray: Not a valid base64 string",
                "pathByteArray: Not a valid base64 string",
                "refPathByte: Not a valid base64 string",
                "refPathByteArray: Not a valid base64 string",
                "refPathByteArray: Not a valid base64 string",
                "pathPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "pathPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "pathPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "refPathPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "refPathPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "refPathPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "[Path '/byte'] Not a valid base64 string",
                "[Path '/byteArray/0'] Not a valid base64 string",
                "[Path '/byteArray/1'] Not a valid base64 string",
                "[Path '/pattern'] ECMA 262 regex \"a+\" does not match input string \"b\"",
                "[Path '/patternArray/0'] ECMA 262 regex \"a+\" does not match input string \"c\"",
                "[Path '/patternArray/1'] ECMA 262 regex \"a+\" does not match input string \"d\"",
                "[Path '/ref/byte'] Not a valid base64 string",
                "[Path '/ref/byteArray/0'] Not a valid base64 string",
                "[Path '/ref/byteArray/1'] Not a valid base64 string",
                "[Path '/ref/pattern'] ECMA 262 regex \"a+\" does not match input string \"b\"",
                "[Path '/ref/patternArray/0'] ECMA 262 regex \"a+\" does not match input string \"c\"",
                "[Path '/ref/patternArray/1'] ECMA 262 regex \"a+\" does not match input string \"d\"",
                "queryByte: Not a valid base64 string",
                "queryByteArray: Not a valid base64 string",
                "queryByteArray: Not a valid base64 string",
                "refQueryByte: Not a valid base64 string",
                "refQueryByteArray: Not a valid base64 string",
                "refQueryByteArray: Not a valid base64 string",
                "queryPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "queryPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "queryPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\"",
                "refQueryPattern: ECMA 262 regex \"a+\" does not match input string \"b\"",
                "refQueryPatternArray: ECMA 262 regex \"a+\" does not match input string \"c\"",
                "refQueryPatternArray: ECMA 262 regex \"a+\" does not match input string \"d\""
        ));
    }
}
