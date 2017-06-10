package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 *
 */
public class SwaggerMatchers {
    public ResultMatcher isValid(final String swaggerJsonUrl) {
        final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
                .createFor(swaggerJsonUrl)
                .build();

        return result -> {
            final MockHttpServletRequest request = result.getRequest();
            final MockHttpServletResponse response = result.getResponse();
            final ValidationReport validationReport = validator.validate(new MockMvcRequest(request), new MockMvcResponse(response));
            if (validationReport.hasErrors()) {
                throw new SwaggerValidationException(validationReport);
            }
        };
    }
}
