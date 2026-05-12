package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.Request;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.atlassian.oai.validator.model.Request.Method.DELETE;
import static com.atlassian.oai.validator.model.Request.Method.GET;
import static com.atlassian.oai.validator.model.Request.Method.PATCH;
import static com.atlassian.oai.validator.model.Request.Method.POST;
import static com.atlassian.oai.validator.model.Request.Method.PUT;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiOperationResolverTest {

    private static final String FILENAME_API_WITH_POST = "oai/v2/api-operation-finder-test.json";

    private static ApiOperationResolver classUnderTest;

    @BeforeAll
    public static void init() {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);

        final SwaggerParseResult swaggerParseResult = new OpenAPIParser().readLocation(FILENAME_API_WITH_POST, null, parseOptions);
        final OpenAPI api = swaggerParseResult.getOpenAPI();
        classUnderTest = new ApiOperationResolver(api, null, true);
    }

    static Stream<TestCase> params() {
        // Assertions based on the description in the API spec
        return Stream.of(
                new TestCase("matches_get_withPathParam", GET, "/Id", matches("GET:/{id}")),
                new TestCase("matches_delete_withNoPathParam", DELETE, "/delete", matches("DELETE:/delete")),
                new TestCase("matches_put_withPathParams", PUT, "/id/action", matches("PUT:/{id}/{action}")),
                new TestCase("matches_whenMultipleOperations_onSamePath", POST, "/update/id", matches("POST:/update/{id}")),
                new TestCase("matches_whenMultipleOperations_onSamePath", PATCH, "/update/id", matches("PATCH:/update/{id}")),
                new TestCase("matches_whenPathsCollide_butOperationsDiffer", GET, "/delete", matches("GET:/{id}")),
                new TestCase("matches_mostSpecificPath_whenMultiplePotentialMatches", GET,
                        "/pathparams/withmorespecific/id.json", matches("GET:/pathparams/withmorespecific/{id}.json")),
                new TestCase("matches_nonParamPath_whenPotentialParameterisedMatch", GET, "/", matches("GET:/")),
                new TestCase("matches_exactMath_inPreferenceToParameterizedMatch", PUT, "/specific/path",
                        matches("PUT:/specific/path")),
                new TestCase("matches_caseInsensitive_pathParts", POST, "/UPDaTE/id", matches("POST:/update/{id}")),
                new TestCase("matches_whenPathParams_notWholePathPart", GET, "/pathparams/withextension/foop.json",
                        matches("GET:/pathparams/withextension/{id}.json")),
                new TestCase("matches_whenMultiplePathParams_inSamePart", GET, "/pathparams/withmultiple/foop-blarp.json",
                        matches("GET:/pathparams/withmultiple/{id}-{name}.json")),
                new TestCase("doesNotMatch_whenNoPathMatches", GET, "/not/a/match", missingPath()),
                new TestCase("doesNotMatch_whenNoPathMatches_whenSimilarToActualPath", POST, "/updates/{id}/{action}",
                        missingPath()),
                new TestCase("doesNotMatch_whenTrailingSlashOnRequest", GET, "/path/without/trailing/slash/",
                        missingPath()),
                new TestCase("doesNotMatch_whenTrailingSlashMissingOnRequest", GET, "/path/with/trailing/slash",
                        missingPath()),
                new TestCase("doesNotMatch_whenMethodNotAllowed", DELETE, "/id", operationNotAllowed()),
                new TestCase("doesNotMatch_whenMethodNotAllowed_multiplePathParams", GET, "/update/id/action",
                        operationNotAllowed()),
                new TestCase("matches_whenPathContainsDot", GET, "/path/with/dot/v1.0/id",
                        matches("GET:/path/with/dot/v1.0/{id}"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    public void test(final TestCase testCase) {
        testCase.expectation().accept(testCase.requestMethod(), testCase.requestPath());
    }

    record TestCase(String testName, Request.Method requestMethod, String requestPath,
                    BiConsumer<Request.Method, String> expectation) {
        @Override
        public String toString() {
            return testName;
        }
    }

    private static BiConsumer<Request.Method, String> matches(final String expectedMatch) {
        return (operation, path) -> assertApiOperationFound(path, operation, expectedMatch);
    }

    private static BiConsumer<Request.Method, String> missingPath() {
        return (operation, path) -> assertMissingRequestPath(path, operation);
    }

    private static BiConsumer<Request.Method, String> operationNotAllowed() {
        return (operation, path) -> assertOperationNotAllowed(path, operation);
    }

    private static void assertApiOperationFound(final String requestPath,
                                                final Request.Method requestMethod,
                                                final String expDescription) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        assertTrue(apiOperationMatch.isPathFound(), format("Path not found on %s", expDescription));
        assertTrue(apiOperationMatch.isOperationAllowed(), format("Operation not allowed on %s", expDescription));
        assertThat(apiOperationMatch.getApiOperation().getOperation().getDescription(), is(expDescription));
    }

    private static void assertMissingRequestPath(final String requestPath,
                                                 final Request.Method requestMethod) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        assertFalse(apiOperationMatch.isPathFound());
        assertFalse(apiOperationMatch.isOperationAllowed());
    }

    private static void assertOperationNotAllowed(final String requestPath,
                                                  final Request.Method requestMethod) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        assertTrue(apiOperationMatch.isPathFound());
        assertFalse(apiOperationMatch.isOperationAllowed());
    }
}
