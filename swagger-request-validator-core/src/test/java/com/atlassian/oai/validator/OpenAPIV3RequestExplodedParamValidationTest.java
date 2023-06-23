package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.junit.Assert.assertTrue;

public class OpenAPIV3RequestExplodedParamValidationTest {

    private final OpenApiInteractionValidator openApi3Validator =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-exploded-query-params.yaml").build();

    private static SimpleRequest buildValidBuildsRequest() {
        return SimpleRequest.Builder
                .get("/api/builds")
                .withQueryParam("sinceBuild", "someBuild")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildInvalidBuildsRequest() {
        return SimpleRequest.Builder
                .get("/api/builds")
                .withQueryParam("since", "someString")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildInvalidMoreBuildsRequest() {
        return SimpleRequest.Builder
                .get("/api/more-builds")
                .withQueryParam("sinceBuild", "someBuild")
                .withQueryParam("since", "2021-01-01")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildValidMoreBuildsRequest() {
        return SimpleRequest.Builder
                .get("/api/more-builds")
                .withQueryParam("sinceBuild", "someBuild")
                .withQueryParam("since", "2021-01-01")
                .withQueryParam("maxBuilds", "10")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildValidBuildsWithRefRequest() {
        return SimpleRequest.Builder
                .get("/api/builds-with-ref")
                .withQueryParam("since", "2021-01-01")
                .withQueryParam("maxBuilds", "10")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildValidQueryParameterRequiredTrue() {
        return SimpleRequest.Builder
                .get("/api/query-parameter-required-true")
                .withContentType("application/json")
                .withQueryParam("from", "2021-01-01")
                .withQueryParam("to", "2021-01-02")
                .build();
    }

    private static SimpleRequest buildInvalidValidQueryParameterRequiredTrueBothMissing() {
        return SimpleRequest.Builder
                .get("/api/query-parameter-required-true")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildInvalidValidQueryParameterRequiredTrueOneMissing() {
        return SimpleRequest.Builder
                .get("/api/query-parameter-required-true")
                .withContentType("application/json")
                .withQueryParam("from", "2021-01-01")
                .build();
    }

    private static SimpleRequest buildValidQueryParameterRequiredFalseBothProvided() {
        return SimpleRequest.Builder
                .get("/api/query-parameter-required-false")
                .withContentType("application/json")
                .withQueryParam("from", "2021-01-01")
                .withQueryParam("to", "2021-01-02")
                .build();
    }

    private static SimpleRequest buildValidQueryParameterRequiredFalseBothMissing() {
        return SimpleRequest.Builder
                .get("/api/query-parameter-required-false")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildInvalidQueryParameterRequiredFalse() {
        return SimpleRequest.Builder
                .get("/api/query-parameter-required-false")
                .withContentType("application/json")
                .withQueryParam("from", "2021-01-01")
                .build();
    }

    private static SimpleRequest buildInvalidBuildsWithRefRequest() {
        return SimpleRequest.Builder
                .get("/api/builds-with-ref")
                .withQueryParam("maxBuilds", "10")
                .withContentType("application/json")
                .build();
    }

    private static SimpleRequest buildValidRequestWithArrayQueryParams() {
        return SimpleRequest.Builder
            .get("/api/data")
            .withQueryParam("id", "1")
            .withQueryParam("outcomes", "SUCCESS", "FAILURE", "SKIPPED")
            .withContentType("application/json")
            .build();
    }

    private static SimpleRequest buildInvalidRequestWithArrayQueryParams() {
        return SimpleRequest.Builder
            .get("/api/data")
            .withQueryParam("id", "1")
            .withQueryParam("outcomes", "SUCCESS", "FAILURE", "SKIPPED", "INVALID-OUTCOME")
            .withContentType("application/json")
            .build();
    }

    @Test
    public void valid_OpenApi3() {
        // given:
        final Request request = buildValidBuildsRequest();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void valid_arrayQueryParam_OpenApi3() {
        // given:
        final Request request = buildValidRequestWithArrayQueryParams();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void invalid_arrayQueryParam_OpenApi3() {
        // given:
        final Request request = buildInvalidRequestWithArrayQueryParams();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.schema.enum");
    }
    
    @Test
    public void invalid_OpenApi3() {
        // given:
        final Request request = buildInvalidBuildsRequest();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.schema.format.date");
    }

    @Test
    public void invalid_missing_required_param_OpenApi3() {
        // given:
        final Request request = buildInvalidMoreBuildsRequest();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.query.missing");
    }

    @Test
    public void valid_required_params_OpenApi3() {
        // given:
        final Request request = buildValidMoreBuildsRequest();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void valid_with_ref_OpenApi3() {
        // given:
        final Request request = buildValidBuildsWithRefRequest();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void invalid_with_ref_OpenApi3() {
        // given:
        final Request request = buildInvalidBuildsWithRefRequest();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.query.missing");
    }

    @Test
    public void valid_query_parameter_required_true_OpenApi3() {
        // given:
        final Request request = buildValidQueryParameterRequiredTrue();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void invalid_query_parameter_required_true_both_missing_OpenApi3() {
        // given:
        final Request request = buildInvalidValidQueryParameterRequiredTrueBothMissing();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.query.missing");
    }

    @Test
    public void invalid_query_parameter_required_true_one_missing_OpenApi3() {
        // given:
        final Request request = buildInvalidValidQueryParameterRequiredTrueOneMissing();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.query.missing");
    }

    @Test
    public void valid_query_parameter_required_false_both_provided_OpenApi3() {
        // given:
        final Request request = buildValidQueryParameterRequiredFalseBothProvided();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void valid_query_parameter_required_false_both_missing_OpenApi3() {
        // given:
        final Request request = buildValidQueryParameterRequiredFalseBothMissing();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertPass(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void invalid_query_parameter_required_false_one_missing_OpenApi3() {
        // given:
        final Request request = buildInvalidQueryParameterRequiredFalse();

        // when:
        final ValidationReport result = openApi3Validator.validateRequest(request);

        // then:
        assertFail(result, "validation.request.parameter.query.missing");
    }
}
