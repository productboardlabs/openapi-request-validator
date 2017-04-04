package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.Request;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class ApiOperationResolverTest {

    private static final String FILENAME_API_WITH_POST = "schema/api-operation-finder-test.json";

    private ApiOperationResolver classUnderTest;

    @Before
    public void setup() throws IOException, URISyntaxException {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(FILENAME_API_WITH_POST, null, true);
        final Swagger swagger = swaggerParseResult.getSwagger();
        this.classUnderTest = new ApiOperationResolver(swagger, null);
    }

    @Test
    public void apiOperationFound() {
        assertApiOperationFound("/Id", Request.Method.GET, "GET:/{id}");
        assertApiOperationFound("/delete", Request.Method.GET, "GET:/{id}");
        assertApiOperationFound("/delete", Request.Method.DELETE, "DELETE:/delete");
        assertApiOperationFound("/Id/Action", Request.Method.PUT, "PUT:/{id}/{action}");
        assertApiOperationFound("/update/Id", Request.Method.POST, "POST:/update/{id}");
        assertApiOperationFound("/update/Id", Request.Method.PATCH, "PATCH:/update/{id}");
        assertApiOperationFound("/update/Id/Action", Request.Method.POST, "POST:/update/{id}/{action}");

        // request paths can be upper cased, too
        assertApiOperationFound("/DELETE", Request.Method.DELETE, "DELETE:/delete");
        assertApiOperationFound("/UPDATE/Id", Request.Method.POST, "POST:/update/{id}");
        assertApiOperationFound("/UPDATE/Id/Action", Request.Method.POST, "POST:/update/{id}/{action}");
    }

    @Test
    public void missingRequestPath() {
        assertMissingRequestPath("/", Request.Method.GET);
        assertMissingRequestPath("/modify/Id/Action", Request.Method.POST);
        assertMissingRequestPath("/very/long/request/path", Request.Method.PATCH);
    }

    @Test
    public void operationNotAllowed() {
        assertOperationNotAllowed("/Id", Request.Method.DELETE);
        assertOperationNotAllowed("/Id", Request.Method.PATCH);
        assertOperationNotAllowed("/Id'/Action", Request.Method.GET);
        assertOperationNotAllowed("/update/Id/Action", Request.Method.GET);
    }

    private void assertApiOperationFound(final String requestPath, final Request.Method requestMethod,
                                         final String expDescription) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        Assert.assertTrue(apiOperationMatch.isPathFound());
        Assert.assertTrue(apiOperationMatch.isOperationAllowed());
        Assert.assertEquals(apiOperationMatch.getApiOperation().getOperation().getDescription(), expDescription);
    }

    private void assertMissingRequestPath(final String requestPath, final Request.Method requestMethod) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        Assert.assertFalse(apiOperationMatch.isPathFound());
        Assert.assertFalse(apiOperationMatch.isOperationAllowed());
    }

    private void assertOperationNotAllowed(final String requestPath, final Request.Method requestMethod) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        Assert.assertTrue(apiOperationMatch.isPathFound());
        Assert.assertFalse(apiOperationMatch.isOperationAllowed());
    }
}
