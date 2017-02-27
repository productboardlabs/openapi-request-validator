package com.atlassian.oai.validator.restassured;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

class CapturingFilter implements Filter {

    private RestAssuredRequest request;
    private RestAssuredResponse response;

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext ctx) {
        this.request = new RestAssuredRequest(requestSpec);
        final Response result = ctx.next(requestSpec, responseSpec);
        this.response = new RestAssuredResponse(result);
        return result;
    }

    public RestAssuredRequest getRequest() {
        return request;
    }

    public RestAssuredResponse getResponse() {
        return response;
    }
}
