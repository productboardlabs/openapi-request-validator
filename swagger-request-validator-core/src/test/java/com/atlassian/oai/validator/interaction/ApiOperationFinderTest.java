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

public class ApiOperationFinderTest {

    private static final String FILENAME_API_WITH_POST = "schema/api-operation-finder-test.json";

    private ApiOperationFinder classUnderTest;

    @Before
    public void setup() throws IOException, URISyntaxException {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(FILENAME_API_WITH_POST, null, true);
        final Swagger swagger = swaggerParseResult.getSwagger();
        this.classUnderTest = new ApiOperationFinder(swagger, null);
    }

    private void assertApiOperationFound(final String requestPath, final Request.Method requestMethod,
                                         final String expDescription) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        Assert.assertTrue(apiOperationMatch.isPathFound());
        Assert.assertTrue(apiOperationMatch.isOperationAllowed());
        Assert.assertEquals(apiOperationMatch.getApiOperation().getOperation().getDescription(), expDescription);
    }

    private void assertApiOperationNotFound(final String requestPath, final Request.Method requestMethod,
                                            final boolean expPathFound, final boolean expOperationAllowed) {
        final ApiOperationMatch apiOperationMatch = classUnderTest.findApiOperation(requestPath, requestMethod);
        Assert.assertTrue(apiOperationMatch.isPathFound() == expPathFound);
        Assert.assertTrue(apiOperationMatch.isOperationAllowed() == expOperationAllowed);
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
        assertApiOperationNotFound("/", Request.Method.GET, false, false);
        assertApiOperationNotFound("/modify/Id/Action", Request.Method.POST, false, false);
        assertApiOperationNotFound("/very/long/request/path", Request.Method.PATCH, false, false);
    }

    @Test
    public void operationNotAllowed() {
        assertApiOperationNotFound("/Id", Request.Method.DELETE, true, false);
        assertApiOperationNotFound("/Id", Request.Method.PATCH, true, false);
        assertApiOperationNotFound("/Id'/Action", Request.Method.GET, true, false);
        assertApiOperationNotFound("/update/Id/Action", Request.Method.GET, true, false);
    }
}
