package com.atlassian.oai.validator.examples.whitelist;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import io.swagger.models.HttpMethod;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.allOf;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.anyOf;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageContains;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageHasKey;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.methodIs;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.pathContains;
import static org.junit.Assert.assertThat;

/**
 * In this example we create a validator with two whitelist rules.
 * <p>
 * Validation errors matched by the rules will have their level changed to {@link ValidationReport.Level#IGNORE}
 * and will contain additional info with the rule name that caused the whitelisting.
 */
public class WhitelistingValidationErrorsTestExample {

    private final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator.createFor("http://petstore.swagger.io/v2/swagger.json")
        .withWhitelist(ValidationErrorsWhitelist.create()
            .withRule(
                "Ignore missing security when getting store inventory",
                allOf(
                    messageHasKey("validation.request.security.missing"),
                    pathContains("/store/inventory"),
                    methodIs(HttpMethod.GET)))
            .withRule(
                "Ignore invalid format of order id for GET and POST to /store/order/{orderId}",
                allOf(
                    messageContains("value '.*' for parameter 'orderId' does not match type 'integer"),
                    pathContains("/store/order/\\{orderId}"),
                    anyOf(
                        methodIs(HttpMethod.GET),
                        methodIs(HttpMethod.POST)))))
        .build();

    @Test
    public void whitelistedMessagesAreIgnored() {
        assertThat(validator.validateRequest(SimpleRequest.Builder.get("/v2/store/inventory").build()),
            isIgnoredBy("Ignore missing security when getting store inventory"));

        assertThat(validator.validateRequest(SimpleRequest.Builder.get("/v2/store/order/fhtagn").build()),
            isIgnoredBy("Ignore invalid format of order id for GET and POST to /store/order/{orderId}"));
    }

    private Matcher<ValidationReport> isIgnoredBy(final String whitelistRule) {
        return new TypeSafeMatcher<ValidationReport>() {
            @Override
            protected boolean matchesSafely(final ValidationReport report) {
                return report.getMessages().stream()
                    .allMatch(message ->
                        message.getLevel() == ValidationReport.Level.IGNORE &&
                            message.getAdditionalInfo().stream().anyMatch(info -> info.contains(whitelistRule)));
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Report with all messages ignored");
            }
        };
    }

}
