package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRule;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.properties.RefProperty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OperationForWhitelisting {

    public static OperationForWhitelisting request() {
        return new OperationForWhitelisting(new SimpleRequest.Builder(Request.Method.GET, "/rest/api").build(), null);
    }

    public static OperationForWhitelisting response() {
        return new OperationForWhitelisting(null, new SimpleResponse.Builder(200).build());
    }

    private ValidationReport.Message message = ValidationReport.Message.create("message.key", "A default message");
    private NormalisedPath path = path("/rest/api");
    private HttpMethod method = HttpMethod.GET;
    private Request request;
    private Response response;
    private Operation operation = new Operation();

    public OperationForWhitelisting(final Request request, final Response response) {
        this.request = request;
        this.response = response;
    }

    private static NormalisedPath path(final String path) {
        final NormalisedPath result = mock(NormalisedPath.class);
        when(result.normalised()).thenReturn(path);
        return result;
    }

    public OperationForWhitelisting withResponse(final int status, final String entityReference) {
        operation.addResponse(String.valueOf(status), new io.swagger.models.Response().schema(
                new RefProperty("#/definitions/" + entityReference)));
        return this;
    }

    public OperationForWhitelisting withRequestParameter(final String entityReference) {
        operation.addParameter(new BodyParameter().schema(new RefModel("#/definitions/" + entityReference)));
        return this;
    }

    public boolean isMatchedBy(final WhitelistRule rule) {
        return rule.matches(message, new ApiOperation(path, path, method, operation), request, response);
    }

    @Override
    public String toString() {
        return method + " " + path.normalised() + ": " + message;
    }

    public OperationForWhitelisting withMessage(final ValidationReport.Message message) {
        this.message = message;
        return this;
    }

    public OperationForWhitelisting withPath(final String path) {
        this.path = path(path);
        return this;
    }

    public OperationForWhitelisting withStatus(final int status) {
        this.response = new SimpleResponse.Builder(status).build();
        return this;
    }

    public OperationForWhitelisting withMethod(final HttpMethod method) {
        this.method = method;
        return this;
    }
}
