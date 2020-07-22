package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRule;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OperationForWhitelisting {

    public static OperationForWhitelisting request() {
        return new OperationForWhitelisting(new SimpleRequest.Builder(Request.Method.GET, "/rest/api").build(), null);
    }

    public static OperationForWhitelisting response() {
        return new OperationForWhitelisting(null, new SimpleResponse.Builder(200).build());
    }

    private ValidationReport.Message message = ValidationReport.Message.create("message.key", "A default message").build();
    private ApiPath path = path("/rest/api");
    private PathItem.HttpMethod method = PathItem.HttpMethod.GET;
    private Request request;
    private Response response;
    private final Operation operation = new Operation();

    private OperationForWhitelisting(final Request request, final Response response) {
        this.request = request;
        this.response = response;
    }

    private static ApiPath path(final String path) {
        final ApiPath result = mock(ApiPath.class);
        when(result.normalised()).thenReturn(path);
        return result;
    }

    public OperationForWhitelisting withDocumentedResponse(final int status, final String entityReference) {
        ApiResponses apiResponses = operation.getResponses();
        if (apiResponses == null) {
            apiResponses = new ApiResponses();
            operation.setResponses(apiResponses);
        }
        apiResponses.addApiResponse(String.valueOf(status), new ApiResponse()
                .content(new Content()
                        .addMediaType("*/*", new MediaType()
                                .schema(new Schema().$ref("#/components/schemas/" + entityReference))
                        )
                )
        );
        return this;
    }

    public OperationForWhitelisting withDocumentedRequestBodyParameter(final String entityReference) {
        operation.setRequestBody(new RequestBody()
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema().$ref("#/components/schemas/" + entityReference))
                        )
                ));
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
        response = new SimpleResponse.Builder(status).build();
        return this;
    }

    public OperationForWhitelisting withMethod(final PathItem.HttpMethod method) {
        this.method = method;
        return this;
    }

    public OperationForWhitelisting withRequestHeaders(final ImmutableMap<String, List<String>> headers) {
        final SimpleRequest.Builder request = new SimpleRequest.Builder(this.request.getMethod(), this.request.getPath());
        headers.forEach(request::withHeader);
        this.request = request.build();
        return this;
    }

    public OperationForWhitelisting withRequestHeader(final String header, final List<String> values) {
        final SimpleRequest.Builder request = new SimpleRequest.Builder(this.request.getMethod(), this.request.getPath());
        request.withHeader(header, values);
        this.request = request.build();
        return this;
    }

    public OperationForWhitelisting withResponseHeaders(final ImmutableMap<String, List<String>> headers) {
        final SimpleResponse.Builder response = new SimpleResponse.Builder(this.response.getStatus());
        headers.forEach(response::withHeader);
        this.response = response.build();
        return this;
    }

    public OperationForWhitelisting withResponseHeader(final String header, final List<String> values) {
        final SimpleResponse.Builder response = new SimpleResponse.Builder(this.response.getStatus());
        response.withHeader(header, values);
        this.response = response.build();
        return this;
    }
}
