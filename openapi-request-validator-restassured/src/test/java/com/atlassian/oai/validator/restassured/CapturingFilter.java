package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

class CapturingFilter implements Filter {

    private Request request;
    private Response response;

    @Override
    public io.restassured.response.Response filter(final FilterableRequestSpecification requestSpec,
                                                   final FilterableResponseSpecification responseSpec,
                                                   final FilterContext ctx) {
        this.request = RestAssuredRequest.of(requestSpec);
        final io.restassured.response.Response result = ctx.next(requestSpec, responseSpec);
        this.response = RestAssuredResponse.of(result);
        return result;
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }
}
