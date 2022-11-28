package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public class ValidationErrorWhitelistingTest {

    @Test
    public void whitelistedRequestFailuresShouldBeIgnored() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                // The "entityIs" rule uses the ref - make sure we don't resolve them away
                .withResolveRefs(false)
                .withWhitelist(ValidationErrorsWhitelist.create()
                        .withRule("Ignore paths", WhitelistRules.messageContains("No API path"))
                        .withRule("Ignore NewUser entity errors", WhitelistRules.entityIs("NewUser")))
                .build();

        final ValidationReport report = classUnderTest.validateRequest(SimpleRequest.Builder.get("/non-existent-path").build());
        assertThat(report.getMessages(), hasItem(whitelisted("No API path found that matches request", "Ignore paths")));

        final SimpleRequest usersRequest = SimpleRequest.Builder
                .post("/users")
                .withBody("{}")
                .withAuthorization("Basic foo")
                .withContentType("application/json")
                .build();
        final ValidationReport report2 = classUnderTest.validateRequest(usersRequest);
        assertThat(report2.getMessages(), hasItem(whitelisted("Object has missing required properties", "Ignore NewUser entity errors")));
    }

    @Test
    public void whitelistedResponseFailuresShouldBeIgnored() {
        final OpenApiInteractionValidator classUnderTest = OpenApiInteractionValidator
                .createForSpecificationUrl("/oai/v2/api-users.json")
                .withWhitelist(ValidationErrorsWhitelist.create()
                        .withRule("Ignore PATCH operation missing", WhitelistRules.messageContains("PATCH operation not allowed"))
                        .withRule("Ignore schema type", WhitelistRules.messageHasKey("validation.response.body.schema.type")))
                .build();

        final ValidationReport report = classUnderTest.validateResponse("/users", Request.Method.PATCH, SimpleResponse.Builder.serverError().build());
        assertThat(report.getMessages(), hasItem(whitelisted("PATCH operation not allowed on path '/users'", "Ignore PATCH operation missing")));

        final ValidationReport report2 = classUnderTest.validateResponse(
                "/users",
                Request.Method.GET,
                SimpleResponse.Builder.ok().withHeader("Content-Type", "application/json").withBody("{}").build()
        );
        assertThat(report2.getMessages(), hasItem(
                whitelisted("Instance type (object) does not match any allowed primitive type (allowed: [\"array\"])", "Ignore schema type"))
        );
    }

    private Matcher<ValidationReport.Message> whitelisted(final String messageText, final String whitelistRule) {
        return new TypeSafeMatcher<ValidationReport.Message>() {
            @Override
            protected boolean matchesSafely(final ValidationReport.Message message) {
                return message.getMessage().contains(messageText) &&
                        message.getLevel() == IGNORE &&
                        message.getContext().isPresent() &&
                        message.getContext().get().getAppliedWhitelistRule().isPresent() &&
                        message.getContext().get().getAppliedWhitelistRule().get().getName().equalsIgnoreCase(whitelistRule);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("a message whitelisted by '" + whitelistRule + "'");
            }
        };
    }
}
