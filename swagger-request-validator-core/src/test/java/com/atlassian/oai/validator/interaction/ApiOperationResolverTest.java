package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.Request;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

import static com.atlassian.oai.validator.model.Request.Method.DELETE;
import static com.atlassian.oai.validator.model.Request.Method.GET;
import static com.atlassian.oai.validator.model.Request.Method.PATCH;
import static com.atlassian.oai.validator.model.Request.Method.POST;
import static com.atlassian.oai.validator.model.Request.Method.PUT;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ApiOperationResolverTest {

    private static final String FILENAME_API_WITH_POST = "oai/v2/api-operation-finder-test.json";

    private static ApiOperationResolver classUnderTest;

    @BeforeClass
    public static void init() {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);

        final SwaggerParseResult swaggerParseResult = new OpenAPIParser().readLocation(FILENAME_API_WITH_POST, null, parseOptions);
        final OpenAPI api = swaggerParseResult.getOpenAPI();
        classUnderTest = new ApiOperationResolver(api, null, true);
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {

        // Assertions based on the description in the API spec
        return Arrays.asList(new Object[][]{
                {"matches_get_withPathParam", GET, "/Id", matches("GET:/{id}")},
                {"matches_delete_withNoPathParam", DELETE, "/delete", matches("DELETE:/delete")},
                {"matches_put_withPathParams", PUT, "/id/action", matches("PUT:/{id}/{action}")},

                {"matches_whenMultipleOperations_onSamePath", POST, "/update/id", matches("POST:/update/{id}")},
                {"matches_whenMultipleOperations_onSamePath", PATCH, "/update/id", matches("PATCH:/update/{id}")},
                {"matches_whenPathsCollide_butOperationsDiffer", GET, "/delete", matches("GET:/{id}")},

                {"matches_mostSpecificPath_whenMultiplePotentialMatches", GET, "/pathparams/withmorespecific/id.json", matches("GET:/pathparams/withmorespecific/{id}.json")},
                {"matches_nonParamPath_whenPotentialParameterisedMatch", GET, "/", matches("GET:/")},
                {"matches_exactMath_inPreferenceToParameterizedMatch", PUT, "/specific/path", matches("PUT:/specific/path")},

                {"matches_caseInsensitive_pathParts", POST, "/UPDaTE/id", matches("POST:/update/{id}")},

                {"matches_whenPathParams_notWholePathPart", GET, "/pathparams/withextension/foop.json", matches("GET:/pathparams/withextension/{id}.json")},
                {"matches_whenMultiplePathParams_inSamePart", GET, "/pathparams/withmultiple/foop-blarp.json", matches("GET:/pathparams/withmultiple/{id}-{name}.json")},

                {"doesNotMatch_whenNoPathMatches", GET, "/not/a/match", missingPath()},
                {"doesNotMatch_whenNoPathMatches_whenSimilarToActualPath", POST, "/updates/{id}/{action}", missingPath()},
                {"doesNotMatch_whenTrailingSlashOnRequest", GET, "/path/without/trailing/slash/", missingPath()},
                {"doesNotMatch_whenTrailingSlashMissingOnRequest", GET, "/path/with/trailing/slash", missingPath()},

                {"doesNotMatch_whenMethodNotAllowed", DELETE, "/id", operationNotAllowed()},
                {"doesNotMatch_whenMethodNotAllowed_multiplePathParams", GET, "/update/id/action", operationNotAllowed()},

                {"matches_whenPathContainsDot", GET, "/path/with/dot/v1.0/id", matches("GET:/path/with/dot/v1.0/{id}")},
        });
    }

    @Parameter
    public String testName;

    @Parameter(1)
    public Request.Method requestMethod;

    @Parameter(2)
    public String requestPath;

    @Parameter(3)
    public BiConsumer<Request.Method, String> expectation;

    @Test
    public void test() {
        expectation.accept(requestMethod, requestPath);
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
        assertTrue(format("Path not found on %s", expDescription), apiOperationMatch.isPathFound());
        assertTrue(format("Operation not allowed on %s", expDescription), apiOperationMatch.isOperationAllowed());
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
