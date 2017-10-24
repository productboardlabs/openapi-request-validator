package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
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
        final SwaggerRequestResponseValidator classUnderTest = SwaggerRequestResponseValidator
                .createFor("/oai/api-users.json")
                .withWhitelist(ValidationWhitelist.empty()
                        .withRule("Ignore paths", WhitelistRules.messageContains("No API path"))
                        .withRule("Ignore NewUser entity errors", WhitelistRules.isEntity("NewUser")))
                .build();

        ValidationReport report = classUnderTest.validateRequest(SimpleRequest.Builder.get("/non-existent-path").build());
        assertThat(report.getMessages(), hasItem(whitelisted("No API path found that matches request", "Ignore paths")));

        ValidationReport report2 = classUnderTest.validateRequest(SimpleRequest.Builder.post("/users").withBody("{}").build());
        assertThat(report2.getMessages(), hasItem(whitelisted("Object has missing required properties", "Ignore NewUser entity errors")));
    }

    @Test
    public void whitelistedResponseFailuresShouldBeIgnored() {
        final SwaggerRequestResponseValidator classUnderTest = SwaggerRequestResponseValidator
                .createFor("/oai/api-users.json")
                .withWhitelist(ValidationWhitelist.empty()
                        .withRule("Ignore PATCH operation missing", WhitelistRules.messageContains("PATCH operation not allowed"))
                        .withRule("Ignore schema type", WhitelistRules.messageHasKey("validation.schema.type")))
                .build();

        ValidationReport report = classUnderTest.validateResponse("/users", Request.Method.PATCH, SimpleResponse.Builder.serverError().build());
        assertThat(report.getMessages(), hasItem(whitelisted("PATCH operation not allowed on path '/users'", "Ignore PATCH operation missing")));

        ValidationReport report2 = classUnderTest.validateResponse("/users", Request.Method.GET, SimpleResponse.Builder.ok().withBody("{}").build());
        assertThat(report2.getMessages(), hasItem(whitelisted("Instance type (object) does not match any allowed primitive type", "Ignore schema type")));
    }

    private Matcher<ValidationReport.Message> whitelisted(String messageText, String whitelistRule) {
        return new TypeSafeMatcher<ValidationReport.Message>() {
            @Override
            protected boolean matchesSafely(ValidationReport.Message message) {
                return message.getMessage().contains(messageText) &&
                        message.getLevel() == IGNORE &&
                        message.getAdditionalInfo().stream().anyMatch(info -> info.toLowerCase().startsWith("whitelisted by: " + whitelistRule.toLowerCase()));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a message whitelisted by '" + whitelistRule + "'");
            }
        };
    }
}
