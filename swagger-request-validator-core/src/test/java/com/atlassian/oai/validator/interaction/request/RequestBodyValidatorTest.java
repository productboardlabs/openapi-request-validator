package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFailWithoutContext;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestBodyValidatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SchemaValidator schemaValidator;

    @InjectMocks
    private RequestBodyValidator requestBodyValidator;

    @Before
    public void setup() {
        when(schemaValidator.validate(any(), any())).thenReturn(ValidationReport.empty());
    }

    @Test
    public void validate_fails_whenUnexpectedRequestBodyFound() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("application/json")
                .withBody("{\"foo\": \"bar\"}")
                .build();

        assertFailWithoutContext(requestBodyValidator.validateRequestBody(request, null),
                "validation.request.body.unexpected");

    }

    @Test
    public void validate_passes_whenNoRequestBodyFound_butNoneDefinedInSpec() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("application/json")
                .build();

        assertPass(requestBodyValidator.validateRequestBody(request, null));

    }

    @Test
    public void validate_fails_whenNoRequestBodyFound_butIsRequiredInSpec_json() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("application/json")
                .build();

        final RequestBody apiRequestBody = new RequestBody().required(true);

        assertFail(requestBodyValidator.validateRequestBody(request, apiRequestBody),
                "validation.request.body.missing");
    }

    @Test
    public void validate_fails_whenNoRequestBodyFound_butIsRequiredInSpec_nonJson() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("text/xml")
                .build();

        final RequestBody apiRequestBody = new RequestBody().required(true);

        assertFail(requestBodyValidator.validateRequestBody(request, apiRequestBody),
                "validation.request.body.missing");
    }

    @Test
    public void validate_passes_whenNoRequestBodyFound_andIsNotRequiredInSpec() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("application/json")
                .build();

        final RequestBody apiRequestBody = new RequestBody().required(false);

        assertPass(requestBodyValidator.validateRequestBody(request, apiRequestBody));
    }

    @Test
    public void validate_passes_whenContentTypeDoesNotMatchDefinedMediaTypes() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("application/json")
                .withBody("{\"foo\": \"bar\"}")
                .build();

        final RequestBody apiRequestBody = new RequestBody()
                .content(new Content()
                        .addMediaType("text/*", new MediaType())
                        .addMediaType("application/something", new MediaType())
                );

        assertPass(requestBodyValidator.validateRequestBody(request, apiRequestBody));
    }

    @Test
    public void validate_doesNotValidateSchema_whenNonJsonContentType() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("text/plain")
                .withBody("Some text")
                .build();

        final RequestBody apiRequestBody = new RequestBody()
                .content(new Content()
                        .addMediaType("text/*", new MediaType())
                        .addMediaType("application/something", new MediaType())
                );

        assertPass(requestBodyValidator.validateRequestBody(request, apiRequestBody));
        verify(schemaValidator, never()).validate(any(), any());
    }

    @Test
    public void validate_validatesSchema_whenJsonContentType() {
        final Request request = SimpleRequest.Builder
                .post("/somewhere")
                .withContentType("application/json")
                .withBody("{\"foo\": \"bar\"}")
                .build();

        final RequestBody apiRequestBody = new RequestBody()
                .content(new Content()
                        .addMediaType("text/*", new MediaType())
                        .addMediaType("application/json", new MediaType())
                );

        assertPass(requestBodyValidator.validateRequestBody(request, apiRequestBody));
        verify(schemaValidator).validate(any(), any());
    }
}
