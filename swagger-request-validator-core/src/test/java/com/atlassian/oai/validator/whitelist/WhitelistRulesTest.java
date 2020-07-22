package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRule;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static com.atlassian.oai.validator.whitelist.OperationForWhitelisting.request;
import static com.atlassian.oai.validator.whitelist.OperationForWhitelisting.response;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.allOf;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.anyOf;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.entityIs;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.headerContains;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.headerContainsRegexp;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.headerContainsSubstring;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.isRequest;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.isResponse;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageContains;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageContainsRegexp;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageContainsSubstring;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageHasKey;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.methodIs;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.pathContains;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.pathContainsRegexp;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.pathContainsSubstring;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.responseStatusIs;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.responseStatusTypeIs;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class WhitelistRulesTest {

    @Test
    public void testAllOf() {
        final WhitelistRule andRule = allOf(entityIs("MyEntity"),
                messageHasKey("my.key"),
                messageContains("\"value\""));

        assertThat(andRule, matches(response()
                .withDocumentedResponse(200, "MyEntity")
                .withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]").build())));

        assertThat(andRule, matches(request()
                .withDocumentedRequestBodyParameter("MyEntity")
                .withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]").build())));

        assertThat(andRule, not(matches(request()
                .withDocumentedRequestBodyParameter("AnotherEntity")
                .withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]").build()))));

        assertThat(andRule, not(matches(request()
                .withDocumentedRequestBodyParameter("MyEntity")
                .withMessage(Message.create("my.key", "Another message").build()))));
    }

    @Test
    public void testAnyOf() {
        final WhitelistRule orRule = anyOf(entityIs("MyEntity"), entityIs("AnotherEntity"));
        assertThat(orRule, matches(request().withDocumentedRequestBodyParameter("MyEntity")));
        assertThat(orRule, matches(request().withDocumentedRequestBodyParameter("AnotherEntity")));

    }

    @Test
    public void testIsEntity() {
        final WhitelistRule rule = WhitelistRules.entityIs("MyEntity");
        assertThat(rule, matches(response().withStatus(200).withDocumentedResponse(200, "MyEntity")));
        assertThat(rule, not(matches(response()
                .withStatus(201)
                .withDocumentedResponse(201, "AnotherEntity")
                .withDocumentedResponse(200, "MyEntity"))));
        assertThat(rule, matches(response()
                .withStatus(201)
                .withDocumentedResponse(201, "MyEntity")
                .withDocumentedResponse(200, "AnotherEntity")));
        assertThat(rule, not(matches(response().withDocumentedResponse(200, "NotMyEntity"))));
        assertThat(rule, matches(request().withDocumentedRequestBodyParameter("MyEntity")));
        assertThat(rule, not(matches(request().withDocumentedRequestBodyParameter("NotMyEntity"))));
    }

    @Test
    public void testMessageHasKey() {
        final WhitelistRule rule = WhitelistRules.messageHasKey("my.key");
        assertThat(rule, matches(response().withMessage(Message.create("my.key", "key: my.key").build())));
        assertThat(rule, matches(response().withMessage(Message.create("MY.KEY", "key: MY.KEY").build())));
        assertThat(rule, not(matches(response().withMessage(Message.create("not.my.key", "key: not.my.key").build()))));
    }

    @Test
    public void testMessageContains() {
        assertThat(messageContains("not allowed.*\"value\""), matches(
                response().withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]").build())));
        assertThat(messageContains("not allowed.*\"value\""), not(matches(
                response().withMessage(Message.create("my.key", "not allowed").build()))));
    }

    @Test
    public void testMessageContainsRegexp() {
        assertThat(messageContainsRegexp("not allowed.*\"value\""), matches(
                response().withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]").build())));
        assertThat(messageContainsRegexp("not allowed.*\"value\""), not(matches(
                response().withMessage(Message.create("my.key", "not allowed").build()))));
    }

    @Test
    public void testMessageContainsSubstring() {
        assertThat(messageContainsSubstring("\"value\""), matches(
                response().withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]").build())));
        assertThat(messageContainsSubstring("\"value\""), not(matches(
                response().withMessage(Message.create("my.key", "not allowed").build()))));
    }

    @Test
    public void testPathContains() {
        assertThat(pathContains("/path/to/my/api"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContains("/path/to/my/api.*"), matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi")));
        assertThat(pathContains("/path/to/my/api"), matches(response().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContains("/path/to/my/api$"), not(matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi"))));
        assertThat(pathContains("/path/to/my/api/?$"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContains("/path/to/my/api/?$"), matches(request().withPath("jira/rest/api/2/path/to/my/api/")));
        assertThat(pathContains("/path/to/my/api"), not(matches(request().withPath("jira/rest/api/2/path/to/another/api"))));
    }

    @Test
    public void testPathContainsRegexp() {
        assertThat(pathContainsRegexp("/path/to/my/api"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContainsRegexp("/path/to/my/api.*"), matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi")));
        assertThat(pathContainsRegexp("/path/to/my/api"), matches(response().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContainsRegexp("/path/to/my/api$"), not(matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi"))));
        assertThat(pathContainsRegexp("/path/to/my/api/?$"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContainsRegexp("/path/to/my/api/?$"), matches(request().withPath("jira/rest/api/2/path/to/my/api/")));
        assertThat(pathContainsRegexp("/path/to/my/api"), not(matches(request().withPath("jira/rest/api/2/path/to/another/api"))));
    }

    @Test
    public void testPathContainsSubstring() {
        assertThat(pathContainsSubstring("/path/to/my/api"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContainsSubstring("/path/to/my/api"), matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi")));
        assertThat(pathContainsSubstring("/path/to/my/apis"), not(matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi"))));
    }

    @Test
    public void testIsRequest() {
        assertThat(isRequest(), matches(request()));
        assertThat(isRequest(), not(matches(response())));
    }

    @Test
    public void testIsResponse() {
        assertThat(isResponse(), matches(response()));
        assertThat(isResponse(), not(matches(request())));
    }

    @Test
    public void testResponseStatusIs() {
        assertThat(responseStatusIs(201), matches(response().withStatus(201)));
        assertThat(responseStatusIs(201), not(matches(response().withStatus(200))));
        assertThat(responseStatusIs(200), not(matches(request())));
    }

    @Test
    public void testResponseStatusTypeIs() {
        assertThat(responseStatusTypeIs(StatusType.SUCCESS), matches(response().withStatus(231)));
        assertThat(responseStatusTypeIs(StatusType.SUCCESS), not(matches(response().withStatus(300))));
        assertThat(responseStatusTypeIs(StatusType.SUCCESS), not(matches(request())));
    }

    @Test
    public void testMethodIs() {
        assertThat(methodIs(PUT), matches(request().withMethod(PUT)));
        assertThat(methodIs(PUT), matches(response().withMethod(PUT)));
        assertThat(methodIs(PUT), not(matches(response().withMethod(DELETE))));
    }

    @Test
    public void testHeaderContains() {
        final WhitelistRule notJson = headerContains("Content-Type", "application/json").not();
        assertThat(notJson, matches(request().withRequestHeaders(ImmutableMap.of())));
        assertThat(notJson, matches(request().withRequestHeaders(ImmutableMap.of("content-type", singletonList("multipart/form-data")))));
        assertThat(notJson, not(matches(request().withRequestHeaders(ImmutableMap.of("content-type", singletonList("application/json"))))));

        assertThat(notJson, matches(response().withResponseHeaders(ImmutableMap.of())));
        assertThat(notJson, matches(response().withResponseHeaders(ImmutableMap.of("content-type", singletonList("multipart/form-data")))));
        assertThat(notJson, not(matches(response().withResponseHeaders(ImmutableMap.of("content-type", singletonList("application/json"))))));
    }

    @Test
    public void testHeaderContainsRegexp() {
        final WhitelistRule notJson = headerContainsRegexp("Content-Type", "/json").not();
        assertThat(notJson, matches(request()));
        assertThat(notJson, matches(request().withRequestHeader("content-type", singletonList("multipart/form-data"))));
        assertThat(notJson, not(matches(request().withRequestHeader("content-type", singletonList("application/json")))));
        assertThat(notJson, not(matches(request().withRequestHeader("content-type", singletonList("text/json")))));

        assertThat(notJson, matches(response()));
        assertThat(notJson, matches(response().withResponseHeader("content-type", singletonList("multipart/form-data"))));
        assertThat(notJson, not(matches(response().withResponseHeader("content-type", singletonList("application/json")))));
    }

    @Test
    public void testHeaderContainsSubstring() {
        final WhitelistRule notJson = headerContainsSubstring("Content-Type", "/json").not();
        assertThat(notJson, matches(request()));
        assertThat(notJson, matches(request().withRequestHeader("content-type", singletonList("multipart/form-data"))));
        assertThat(notJson, not(matches(request().withRequestHeader("content-type", singletonList("application/json")))));
        assertThat(notJson, not(matches(request().withRequestHeader("content-type", singletonList("text/json")))));

        assertThat(notJson, matches(response()));
        assertThat(notJson, matches(response().withResponseHeader("content-type", singletonList("multipart/form-data"))));
        assertThat(notJson, not(matches(response().withResponseHeader("content-type", singletonList("application/json")))));
    }

    private Matcher<WhitelistRule> matches(final OperationForWhitelisting operation) {
        return new TypeSafeMatcher<WhitelistRule>() {
            @Override
            protected boolean matchesSafely(final WhitelistRule whitelistRule) {
                return operation.isMatchedBy(whitelistRule);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("A rule that matches: " + operation);
            }
        };
    }
}