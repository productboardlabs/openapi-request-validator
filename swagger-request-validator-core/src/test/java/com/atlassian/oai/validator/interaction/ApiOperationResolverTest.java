package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.ApiPath;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

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

    private static final String FILE_NAME_SPEC_WITH_MANY_COMPLEX_PATHS = "oai/api-operation-finder-test.json";
    private static final String FILE_NAME_SPEC_WITH_DYNAMIC_PATHS_THAT_ALWAYS_MATCH= "oai/api-operation-finder-always-finds-a-dynamic-match.json";
    private static final String FILE_NAME_SPEC_WITH_DYNAMIC_PATHS_THAT_ALL_HAVE_PREFIXES= "oai/api-operation-finder-dynamic-paths-all-have-prefixes.json";

    private static ApiOperationResolver resolverWithManyComplexPathsInSpec;
    private static ApiOperationResolver resolverWithPathThatAlwaysMatches;
    private static ApiOperationResolver resolverWithPathThatAllHavePrefixes;

    @BeforeClass
    public static void init() {
        resolverWithManyComplexPathsInSpec = createResolver(FILE_NAME_SPEC_WITH_MANY_COMPLEX_PATHS);
        resolverWithPathThatAlwaysMatches = createResolver(FILE_NAME_SPEC_WITH_DYNAMIC_PATHS_THAT_ALWAYS_MATCH);
        resolverWithPathThatAllHavePrefixes = createResolver(FILE_NAME_SPEC_WITH_DYNAMIC_PATHS_THAT_ALL_HAVE_PREFIXES);
    }

    private static ApiOperationResolver createResolver(final String fileName) {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(fileName, null, true);
        final Swagger swagger = swaggerParseResult.getSwagger();
        return new ApiOperationResolver(swagger, null);
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

                {"matches_caseInsensitive_pathParts", POST, "/UPDaTE/id", matches("POST:/update/{id}")},

                {"matches_whenPathParams_notWholePathPart", GET, "/pathparams/withextension/foop.json", matches("GET:/pathparams/withextension/{id}.json")},
                {"matches_whenMultiplePathParams_inSamePart", GET, "/pathparams/withmultiple/foop-blarp.json", matches("GET:/pathparams/withmultiple/{id}-{name}.json")},

                {"matches_whenPrefixOfDynamicPathWithoutPathMatcher", GET, "tree/category/", matches("GET:/tree/{categoryName}")},

                {"matches_whenPrefixOfDynamicPathWithPathMatcher", GET, "tree/category/", matches("GET:/tree/{categoryName}", ApiPath::matchesDynamicPath)},

                {"matches_withDyanmicPath", GET, "tree/category/folderFoo/folderBar", matches("GET:/tree/{categoryName}/{path}", ApiPath::matchesDynamicPath)},
                {"matches_withDyanmicPathAndFileName", GET, "tree/category/folderFoo/file.txt", matches("GET:/tree/{categoryName}/{path}", ApiPath::matchesDynamicPath)},

                {"doesNotMatch_whenNoPathMatches", GET, "/not/a/match", missingPath()},
                {"doesNotMatch_whenNoPathMatches_whenSimilarToActualPath", POST, "/updates/{id}/{action}", missingPath()},
                {"doesNotMatch_whenNoPathMatches_with_Dynamic_Paths", GET, "/not-tree/subFolder",
                    missingPath(() -> resolverWithPathThatAllHavePrefixes, ApiPath::matchesDynamicPath)},

                {"doesNotMatch_whenMethodNotAllowed", DELETE, "/id", operationNotAllowed()},
                {"doesNotMatch_whenMethodNotAllowed_multiplePathParams", GET, "/update/id/action", operationNotAllowed()},
                {"doesNotMatch_whenMethodNotAllowed_with_Dynamic_Paths", GET, "/not-tree/subFolder", //matches /{id}/{action} but GET is not allowed
                    operationNotAllowed(() -> resolverWithPathThatAlwaysMatches, ApiPath::matchesDynamicPath)},
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
        return (operation, path) -> assertApiOperationFound(resolverWithManyComplexPathsInSpec, path, operation, expectedMatch);
    }

    private static BiConsumer<Request.Method, String> matches(final String expectedMatch, final BiPredicate<ApiPath, NormalisedPath> matcher) {
        return (operation, path) -> assertApiOperationFound(resolverWithManyComplexPathsInSpec, path, operation, expectedMatch, matcher);
    }

    private static BiConsumer<Request.Method, String> missingPath() {
        return (operation, path) -> assertMissingRequestPath(resolverWithManyComplexPathsInSpec, path, operation);
    }

    private static BiConsumer<Request.Method, String> missingPath(final Supplier<ApiOperationResolver> classUnderTest, final BiPredicate<ApiPath, NormalisedPath> matcher) {
        return (operation, path) -> assertMissingRequestPath(classUnderTest.get(), path, operation, matcher);
    }

    private static BiConsumer<Request.Method, String> operationNotAllowed(final Supplier<ApiOperationResolver> classUnderTest, final BiPredicate<ApiPath, NormalisedPath> matcher) {
        return (operation, path) -> assertOperationNotAllowed(classUnderTest.get(), path, operation, matcher);
    }

    private static BiConsumer<Request.Method, String> operationNotAllowed() {
        return (operation, path) -> assertOperationNotAllowed(resolverWithManyComplexPathsInSpec, path, operation);
    }

    private static void assertApiOperationFound(final ApiOperationResolver classUnderTest,
                                                final String requestPath,
                                                final Request.Method requestMethod,
                                                final String expDescription) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        assertTrue(format("Path not found on %s", expDescription), apiOperationMatch.isPathFound());
        assertTrue(format("Operation not allowed on %s", expDescription), apiOperationMatch.isOperationAllowed());
        assertThat(apiOperationMatch.getApiOperation().getOperation().getDescription(), is(expDescription));
    }

    private static void assertApiOperationFound(final ApiOperationResolver classUnderTest,
                                                final String requestPath,
                                                final Request.Method requestMethod,
                                                final String expDescription,
                                                final BiPredicate<ApiPath, NormalisedPath> matcher) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod, matcher);
        assertTrue(format("Path not found on %s", expDescription), apiOperationMatch.isPathFound());
        assertTrue(format("Operation not allowed on %s", expDescription), apiOperationMatch.isOperationAllowed());
        assertThat(apiOperationMatch.getApiOperation().getOperation().getDescription(), is(expDescription));
    }

    private static void assertMissingRequestPath(final ApiOperationResolver classUnderTest,
                                                 final String requestPath,
                                                 final Request.Method requestMethod) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        assertFalse(apiOperationMatch.isPathFound());
        assertFalse(apiOperationMatch.isOperationAllowed());
    }

    private static void assertMissingRequestPath(final ApiOperationResolver classUnderTest,
                                                 final String requestPath,
                                                 final Request.Method requestMethod,
                                                 final BiPredicate<ApiPath, NormalisedPath> matcher) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod, matcher);
        assertFalse(apiOperationMatch.isPathFound());
        assertFalse(apiOperationMatch.isOperationAllowed());
    }

    private static void assertOperationNotAllowed(final ApiOperationResolver classUnderTest,
                                                  final String requestPath,
                                                  final Request.Method requestMethod) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        assertTrue(apiOperationMatch.isPathFound());
        assertFalse(apiOperationMatch.isOperationAllowed());
    }

    private static void assertOperationNotAllowed(final ApiOperationResolver classUnderTest,
                                                  final String requestPath,
                                                  final Request.Method requestMethod,
                                                  final BiPredicate<ApiPath, NormalisedPath> matcher) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod, matcher);
        assertTrue(apiOperationMatch.isPathFound());
        assertFalse(apiOperationMatch.isOperationAllowed());
    }
}
