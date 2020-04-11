package com.atlassian.oai.validator.examples.whitelist;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.NamedWhitelistRule;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
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
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static org.junit.Assert.assertThat;

/**
 * In this example we create a validator with two whitelist rules.
 * <p>
 * Validation errors matched by the rules will have their level changed to {@link ValidationReport.Level#IGNORE}
 * and will contain additional info with the rule name that caused the whitelisting.
 */
public class WhitelistingValidationErrorsTestExample {

    private final OpenApiInteractionValidator validator = OpenApiInteractionValidator
            .createForSpecificationUrl("http://petstore.swagger.io/v2/swagger.json")
            .withWhitelist(ValidationErrorsWhitelist.create()
                    .withRule(
                            "Ignore missing security when getting store inventory",
                            allOf(
                                    messageHasKey("validation.request.security.missing"),
                                    pathContains("/store/inventory"),
                                    methodIs(GET)))
                    .withRule(
                            "Ignore invalid format of order id for GET and POST to /store/order/{orderId}",
                            allOf(
                                    messageContains("Instance type .* does not match any allowed primitive type .*"),
                                    pathContains("/store/order/\\{orderId}"),
                                    anyOf(methodIs(GET), methodIs(POST)))))
            .build();

    @Test
    public void whitelistedMessagesAreIgnored() {
        assertThat(validator.validateRequest(SimpleRequest.Builder.get("/v2/store/inventory").build()),
                hasErrorsIgnoredBy("Ignore missing security when getting store inventory"));

        assertThat(validator.validateRequest(SimpleRequest.Builder.get("/v2/store/order/fhtagn").build()),
                hasErrorsIgnoredBy("Ignore invalid format of order id for GET and POST to /store/order/{orderId}"));
    }

    private Matcher<ValidationReport> hasErrorsIgnoredBy(final String whitelistRule) {
        return new TypeSafeMatcher<ValidationReport>() {
            @Override
            protected boolean matchesSafely(final ValidationReport report) {
                return report.getMessages().stream()
                        .allMatch(message ->
                                message.getLevel() == ValidationReport.Level.IGNORE &&
                                        message.getContext().get()
                                                .getAppliedWhitelistRule()
                                                .map(NamedWhitelistRule::getName).orElse("")
                                                .equals(whitelistRule)
                        );
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Report with all messages ignored by the following rule: " + whitelistRule);
            }
        };
    }

}
