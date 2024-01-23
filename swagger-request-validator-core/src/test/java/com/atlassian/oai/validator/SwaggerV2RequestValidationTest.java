package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.request.CustomRequestValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonRequest;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonResponse;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadResource;

/**
 * Tests for Request validation behavior
 */
public class SwaggerV2RequestValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v2/api-users.json").build();

    private final Response validUserResponse =
            SimpleResponse.Builder.ok().withBody(loadJsonResponse("user-valid")).build();
    private final Response validUsersResponse =
            SimpleResponse.Builder.ok().withBody(loadJsonResponse("users-valid")).build();
    private final SimpleResponse okResponse = SimpleResponse.Builder.ok().build();

    @Test
    public void validate_withValidRequestResponse_shouldSucceed() {
        final Request request = SimpleRequest.Builder
                .get("/users/1")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidMissingBasicAuth_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users/1")
                .withAuthorization("NOT_BASIC EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.invalid");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.security.invalid");
    }

    @Test
    public void validate_withMissingBasicAuth_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users/1")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.security.missing");
    }

    @Test
    public void validate_withInvalidPathParam_shouldFail() {
        final Request request = SimpleRequest.Builder.get("/users/a").build();
        final Response response = SimpleResponse.Builder.badRequest().build();

        assertFail(classUnderTest.validate(request, response),
                "validation.request.parameter.schema.type");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withUnknownPath_shouldFail() {
        final Request request = SimpleRequest.Builder.patch("/peoples/1").build();

        assertFail(classUnderTest.validate(request, okResponse),
                "validation.request.path.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.path.missing");
    }

    @Test
    public void validate_withUnknownPathDueToEndingSlashWithStrictOperationPathMatching_shouldFail() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withStrictOperationPathMatching()
                .build();
        final Request request = SimpleRequest.Builder.get("/users/1/")
            .withAuthorization("Basic EncryptedUsernameAndPassword")
            .build();

        assertFail(classUnderTest.validate(request, okResponse), "validation.request.path.missing");
        assertFail(classUnderTest.validateRequest(request), "validation.request.path.missing");
    }

    @Test
    public void validate_withOperationPathTrailingSlashDifference_shouldSucceed() {
        final Request request = SimpleRequest.Builder.get("/users/1/")
            .withAuthorization("Basic EncryptedUsernameAndPassword")
            .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidRequestMethod_shouldFail() {
        final Request request = SimpleRequest.Builder.patch("/users/1").build();

        assertFail(classUnderTest.validate(request, okResponse),
                "validation.request.operation.notAllowed");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.operation.notAllowed");
    }

    @Test
    public void validate_withRequestMissingRequiredJsonBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertFail(classUnderTest.validate(request, okResponse),
                "validation.request.body.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.missing");
    }

    @Test
    public void validate_withValidFormDataBody_shouldPass() {
        final String formData = "email=abc%40gmail.com";
        final Request request = SimpleRequest.Builder
                .put("/users/1")
                .withContentType("application/x-www-form-urlencoded")
                .withBody(formData)
                .build();
        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequestMissingRequiredFormDataBody_shouldFail() {
        final String formData = "fmail=abc%40gmail.com";
        final Request request = SimpleRequest.Builder
                .put("/users/1")
                .withContentType("application/x-www-form-urlencoded")
                .withBody(formData)
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse), "validation.request.body.schema.required");
        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.required");
    }

    @Test
    public void validate_formData_manyValuesForSingleKey() {
        final String formData = "email=abc%40gmail.com&email=";
        final Request request = SimpleRequest.Builder
                .put("/users/1")
                .withBody(formData)
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/x-www-form-urlencoded")
                .build();
        assertFail(classUnderTest.validate(request, validUserResponse), "validation.request.body.schema.type");
        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.type");
    }

    @Test
    public void validate_withInvalidFormDataRequestBody_shouldFail() {
        final String formData = "malformed-form-url-encoded";
        final Request request = SimpleRequest.Builder
                .put("/users/1")
                .withContentType("application/x-www-form-urlencoded")
                .withBody(formData)
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse), "validation.request.body.schema.required");
        assertFail(classUnderTest.validateRequest(request), "validation.request.body.schema.required");
    }

    @Test
    @Ignore("Multipart form data not implemented yet")
    public void validate_withValidMultipartFormDataBody_shouldPass() {
        final String formData =
                "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"additionalMetadata\"\r\n" +
                        "\r\n" +
                        "abc@gmail.com\r\n" +
                        "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"imageFile\"; filename=\"myImageFile.jpg\"\r\n" +
                        "\r\n" +
                        "some content\r\n" +
                        "--------------------------3046b8889e52e808--";

        final Request request = SimpleRequest.Builder
                .post("/secure/users/1/upload")
                .withContentType("multipart/form-data; boundary=------------------------3046b8889e52e808")
                .withBody(formData)
                .build();
        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    @Ignore("Form data validation not yet implemented")
    public void validate_withRequestMissingRequiredMultipartFormDataBody_shouldFail() {
        final String formData =
                "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"additionalMetadata\"\r\n" +
                        "\r\n" +
                        "abc@gmail.com\r\n" +
                        "--------------------------3046b8889e52e808";

        final Request request = SimpleRequest.Builder
                .post("/secure/users/1/upload")
                .withContentType("multipart/form-data; boundary=------------------------3046b8889e52e808")
                .withBody(formData)
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse), "validation.request.parameter.missing");
        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.missing");
    }

    @Test
    @Ignore("Form data validation not yet implemented")
    public void validate_multipartFormData_manyValuesForSingleKey() {
        final String formData =
                "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"additionalMetadata\"\r\n" +
                        "\r\n" +
                        "abc@gmail.com\r\n" +
                        "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"imageFile\"; filename=\"myImageFile.jpg\"\r\n" +
                        "\r\n" +
                        "some content\r\n" +
                        "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"imageFile\"; filename=\"myImageFile.jpg\"\r\n" +
                        "\r\n" +
                        "\r\n" +
                        "--------------------------3046b8889e52e808";
        final Request request = SimpleRequest.Builder
                .post("/secure/users/1/upload")
                .withContentType("multipart/form-data; boundary=------------------------3046b8889e52e808")
                .withBody(formData)
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.parameter.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.missing");
    }

    @Test
    @Ignore("Form data validation not yet implemented")
    public void validate_withInvalidMultipartFormDataRequestBody_shouldFail() {
        final String formData =
                "--------------------------3046b8889e52e808\r\n" +
                        "Content-Disposition: form-data; name=\"additionalMetadata\"\r\n" +
                        "\r\n" +
                        "abc@gmai";
        final Request request = SimpleRequest.Builder
                .post("/secure/users/1/upload")
                .withContentType("multipart/form-data; boundary=------------------------3046b8889e52e808")
                .withBody(formData)
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.parameter.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withValidJsonBody_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withContentType("application/json;charset=utf8")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidJsonRequestBody_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/json;charset=utf8")
                .withBody(loadJsonRequest("newuser-invalid-missingrequired"))
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.body.schema.required");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.required");
    }

    @Test
    public void validate_authorizationHeaderIsChecked_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/secure/users/1")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_authorizationQueryParamInsteadOfHeader_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/secure/users/1")
                .withQueryParam("authorization", "token")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.security.missing");
    }

    @Test
    public void validate_authorizationHeaderIsChecked_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/secure/users/1")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.security.missing");
    }

    @Test
    public void validate_authorizationQueryParamIsChecked_shouldPass() {
        final Request request = SimpleRequest.Builder
                .put("/secure/users/1")
                .withQueryParam("authorization", "token")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_authorizationQueryParamIsChecked_shouldFail() {
        final Request request = SimpleRequest.Builder
                .put("/secure/users/1")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.security.missing");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.security.missing");
    }

    @Test
    public void validate_withRequestBody_shouldFail_whenNoneExpected() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .build();

        assertFail(classUnderTest.validate(request, validUsersResponse),
                "validation.request.body.unexpected");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.unexpected");
    }

    @Test
    public void validate_withValidQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("maxCount", "10")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidQueryParamFormat_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("maxCount", "a")
                .build();

        assertFail(classUnderTest.validate(request, validUsersResponse),
                "validation.request.parameter.schema.type");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withMissingQueryParam_shouldPass_whenOptional() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withArrayQueryParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("filter", "1,2,3")
                .build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withArrayQueryParam_shouldFail_whenInvalidFormat() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withQueryParam("filter", "1,\"bob\",3")
                .build();

        assertFail(classUnderTest.validate(request, validUsersResponse), "validation.request.parameter.schema.type");
        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_shouldNotSplitQueryParams_whenNotArrayType() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("sorting", "name,email")
                .build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withExtraQueryParams_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("foo", "bar")
                .withQueryParam("something", "else")
                .build();

        assertPass(classUnderTest.validate(request, validUsersResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMissingQueryParam_shouldFail_whenRequired() {
        final Request request = SimpleRequest.Builder
                .get("/healthcheck")
                .build();

        assertFail(classUnderTest.validate(request, okResponse), "validation.request.parameter.query.missing");
        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.query.missing");
    }

    @Test
    public void validate_withValidQueryParam_shouldPass_whenRequired() {
        final Request request = SimpleRequest.Builder
                .get("/healthcheck")
                .withQueryParam("type", "deep")
                .build();

        assertPass(classUnderTest.validate(request, okResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withNoContentType_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMatchingContentType_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withContentType("application/json;charset=UTF-8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withContentTypeButNoConsumes_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/healthcheck")
                .withQueryParam("type", "deep")
                .withContentType("application/json")
                .build();

        assertPass(classUnderTest.validate(request, okResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withNonMatchingContentType_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withContentType("text/html")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.contentType.notAllowed");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.contentType.notAllowed");
    }

    @Test
    public void validate_withInvalidContentType_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withContentType("foop")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.contentType.invalid");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.contentType.invalid");
    }

    @Test
    public void validate_withMatchingAccept_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withAccept("application/json;charset=UTF-8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withWildcardAccept_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAccept("*/*")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withApplicationJsonAcceptButEmptyConsumesInSpec_shouldPass() {
        final Request request = SimpleRequest.Builder
                .post("/users2")
                .withBody(loadJsonRequest("newuser-valid"))
                .withContentType("application/json;charset=UTF-8")
                .withAccept("application/json;charset=UTF-8")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultipleAcceptHeaders_shouldPass_whenOneMatches() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withAccept("text/html", "application/json;charset=UTF-8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultipleAcceptHeadersConcatenated_shouldPass_whenOneMatches() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withAccept("text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultipleAcceptHeadersSpaceBefore_shouldPass_whenOneMatches() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withAccept("text/html, application/xhtml+xml, application/xml, application/json;q=0.9, */*;q=0.8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultipleAcceptHeadersSpaceAfter_shouldPass_whenOneMatches() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withAccept("text/html ,application/xhtml+xml ,application/xml ,application/json;q=0.9 , */*;q=0.8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultipleAcceptHeadersSpaceAround_shouldPass_whenOneMatches() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withHeader("Authorization", "Basic EncryptedUsernameAndPassword")
                .withAccept("text/html ,application/xhtml+xml ,application/xml , application/json;q=0.9 , */*;q=0.8")
                .build();

        assertPass(classUnderTest.validate(request, validUserResponse));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withNonMatchingAccept_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAccept("text/html")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.accept.notAllowed");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.accept.notAllowed");
    }

    @Test
    public void validate_withMultipleAcceptHeaders_shouldFail_whenNoneMatch() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAccept("text/html", "application/binary")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.accept.notAllowed");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.accept.notAllowed");
    }

    @Test
    public void validate_withInvalidAccept_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/users")
                .withBody(loadJsonRequest("newuser-valid"))
                .withAccept("foop")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.accept.invalid");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.accept.invalid");
    }

    @Test
    public void validate_withValidHeaderParam_shouldPass() {
        final Request request = SimpleRequest.Builder
                .get("/healthcheck")
                .withQueryParam("type", "shallow")
                .withHeader("X-Max-Timeout", "30")
                .build();

        assertPass(classUnderTest.validate(request, SimpleResponse.Builder.ok().build()));
        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidHeaderParam_shouldFail() {
        final Request request = SimpleRequest.Builder
                .get("/healthcheck")
                .withQueryParam("type", "shallow")
                .withHeader("X-Max-Timeout", "30.0")
                .build();

        assertFail(classUnderTest.validate(request, validUserResponse),
                "validation.request.parameter.schema.type");
        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withRequiredHeader_shouldFail_whenMissing() {
        final Request request = SimpleRequest.Builder
                .get("/headers")
                .withHeader("x-not-listed", "30")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.header.missing");
    }

    @Test
    public void validate_withRequiredHeader_shouldPass_whenSupplied() {
        final Request request = SimpleRequest.Builder
                .get("/headers")
                .withHeader("X-Required-Header", "30")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRefParams_shouldPass_whenRequiredParamSupplied() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-ref-params.json")
                .build();

        final Request request = SimpleRequest.Builder
                .get("/myresource")
                .withQueryParam("queryparam", "value")
                .withHeader("headerparam", "value")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRefParams_shouldFail_whenRequiredParamMissing() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-ref-params.json")
                .build();

        final Request request = SimpleRequest.Builder
                .get("/myresource")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.query.missing");
    }

    @Test
    public void validate_withRefParams_shouldPass_whenSpecSuppliedAsString() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForInlineApiSpecification(loadResource("/oai/v2/api-ref-params.json"))
                .build();

        final Request request = SimpleRequest.Builder
                .get("/myresource")
                .withQueryParam("queryparam", "value")
                .withHeader("headerparam", "value")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withPartPathParams_shouldPass_whenValid() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-operation-finder-test.json")
                .build();

        final Request request = SimpleRequest.Builder
                .get("/pathparams/withextension/theid.json")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withPartPathParams_shouldFail_whenMissing() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-operation-finder-test.json")
                .build();

        final Request request = SimpleRequest.Builder
                .get("/pathparams/withextension/.json")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withMultiplePathParams_shouldPass_whenAllValid() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-operation-finder-test.json")
                .build();

        final Request request = SimpleRequest.Builder
                .get("/pathparams/withmultiple/theid-thename")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMultiplePathParams_shouldFail_whenMissing() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-operation-finder-test.json")
                .build();

        final Request request = SimpleRequest.Builder
                .get("/pathparams/withmultiple/-thename")
                .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withXmlBody_shouldNotApplySchemaValidation() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-non-json-body.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/results")
                .withContentType("text/xml")
                .withBody("<Result><id>100</id><name>Adam Andrews</name><score>86</score></Result>")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withPlainTextBody_shouldNotApplySchemaValidation() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-non-json-body.json")
                .build();

        final Request request = SimpleRequest.Builder
                .patch("/results/100")
                .withContentType("text/plain")
                .withBody("d101")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullablePrimitive_shouldPass_whenNull() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-nullable.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/nullablePrimitive")
                .withContentType("application/json")
                .withBody("null")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullablePrimitive_shouldPass_whenProvided() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-nullable.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/nullablePrimitive")
                .withContentType("application/json")
                .withBody("1")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullablePrimitive_shouldFail_whenMissing() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-nullable.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/nullablePrimitive")
                .withContentType("application/json")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.missing");
    }

    @Test
    public void validate_withRequiredNullableProperty_shouldPass_whenNull() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-nullable.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableProperty")
                .withContentType("application/json")
                .withBody("{ \"nullable\": null }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullableProperty_shouldPass_whenNonNull() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-nullable.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableProperty")
                .withContentType("application/json")
                .withBody("{ \"nullable\": \"foo\" }")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withRequiredNullableProperty_shouldFail_whenMissing() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-nullable.json")
                .build();

        final Request request = SimpleRequest.Builder
                .post("/nullableProperty")
                .withContentType("application/json")
                .withBody("{ }")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.body.schema.required");
    }

    @Test
    public void validate_withCustomValidation_shouldPass() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withCustomRequestValidation(new TestValidator())
                .build();

        final Request request = SimpleRequest.Builder
                .get("/extensions")
                .withHeader("Extension", "true")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withCustomValidation_shouldFail() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withCustomRequestValidation(new TestValidator())
                .build();

        final Request request = SimpleRequest.Builder
                .get("/extensions")
                .withHeader("Extension", "false")
                .build();

        assertFail(classUnderTest.validateRequest(request));
    }

    private class TestValidator implements CustomRequestValidator {
        @Override
        public ValidationReport validate(@Nonnull final Request request, @Nonnull final ApiOperation apiOperation) {
            final Optional<Object> extensionValue = apiOperation.getOperation().getExtensions().entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase("x-test-extension"))
                    .map(Map.Entry::getValue)
                    .findFirst();
            if (extensionValue.filter(value -> request.getHeaders().get("Extension").contains(value)).isPresent()) {
                return ValidationReport.empty();
            } else {
                return ValidationReport.singleton(ValidationReport.Message.create("test.extension", "Header extension didn't match expected value").build());
            }
        }
    }

    @Test
    public void validate_withUnexpectedQueryParam_shouldFail() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withLevelResolver(LevelResolver.create()
                        .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.ERROR)
                        .build()).build();

        final Request request = SimpleRequest.Builder.get("/users")
                .withAuthorization("Basic EncryptedUsernameAndPassword")
                .withQueryParam("UnexpectedParameter", "Value").build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.query.unexpected");
    }
}
