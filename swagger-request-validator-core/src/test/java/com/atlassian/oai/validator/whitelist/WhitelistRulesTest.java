package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRule;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import io.swagger.models.HttpMethod;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static com.atlassian.oai.validator.whitelist.OperationForWhitelisting.request;
import static com.atlassian.oai.validator.whitelist.OperationForWhitelisting.response;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.allOf;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.anyOf;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.isEntity;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.isRequest;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.isResponse;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageContains;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageHasKey;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.methodIs;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.pathContains;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.responseStatusIs;
import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.responseStatusTypeIs;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class WhitelistRulesTest {

    @Test
    public void testAllOf() throws Exception {
        WhitelistRule andRule = allOf(isEntity("MyEntity"),
                messageHasKey("my.key"),
                messageContains("\"value\""));

        assertThat(andRule, matches(response()
                .withResponse(200, "MyEntity")
                .withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]"))));

        assertThat(andRule, matches(request()
                .withRequestParameter("MyEntity")
                .withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]"))));

        assertThat(andRule, not(matches(request()
                .withRequestParameter("AnotherEntity")
                .withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]")))));

        assertThat(andRule, not(matches(request()
                .withRequestParameter("MyEntity")
                .withMessage(Message.create("my.key", "Another message")))));
    }

    @Test
    public void testAnyOf() throws Exception {
        WhitelistRule orRule = anyOf(isEntity("MyEntity"), isEntity("AnotherEntity"));
        assertThat(orRule, matches(request().withRequestParameter("MyEntity")));
        assertThat(orRule, matches(request().withRequestParameter("AnotherEntity")));

    }

    @Test
    public void testIsEntity() throws Exception {
        WhitelistRule rule = WhitelistRules.isEntity("MyEntity");
        assertThat(rule, matches(response().withResponse(200, "MyEntity")));
        assertThat(rule, not(matches(response()
                .withStatus(201)
                .withResponse(201, "AnotherEntity")
                .withResponse(200, "MyEntity"))));
        assertThat(rule, matches(response()
                .withStatus(201)
                .withResponse(201, "MyEntity")
                .withResponse(200, "AnotherEntity")));
        assertThat(rule, not(matches(response().withResponse(200, "NotMyEntity"))));
        assertThat(rule, matches(request().withRequestParameter("MyEntity")));
        assertThat(rule, not(matches(request().withRequestParameter("NotMyEntity"))));
    }

    @Test
    public void testMessageHasKey() throws Exception {
        WhitelistRule rule = WhitelistRules.messageHasKey("my.key");
        assertThat(rule, matches(response().withMessage(Message.create("my.key", "key: my.key"))));
        assertThat(rule, matches(response().withMessage(Message.create("MY.KEY", "key: MY.KEY"))));
        assertThat(rule, not(matches(response().withMessage(Message.create("not.my.key", "key: not.my.key")))));
    }

    @Test
    public void testMessageContains() throws Exception {
        assertThat(messageContains("not allowed.*\"value\""), matches(
                response().withMessage(Message.create("my.key", "Object instance has properties which are not allowed by the schema: [\"value\"]"))));
        assertThat(messageContains("not allowed.*\"value\""), not(matches(
                response().withMessage(Message.create("my.key", "not allowed")))));
    }

    @Test
    public void testPathContains() throws Exception {
        assertThat(pathContains("/path/to/my/api"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContains("/path/to/my/api.*"), matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi")));
        assertThat(pathContains("/path/to/my/api"), matches(response().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContains("/path/to/my/api$"), not(matches(request().withPath("jira/rest/api/2/path/to/my/api/subapi"))));
        assertThat(pathContains("/path/to/my/api/?$"), matches(request().withPath("jira/rest/api/2/path/to/my/api")));
        assertThat(pathContains("/path/to/my/api/?$"), matches(request().withPath("jira/rest/api/2/path/to/my/api/")));
        assertThat(pathContains("/path/to/my/api"), not(matches(request().withPath("jira/rest/api/2/path/to/another/api"))));
    }

    @Test
    public void testIsRequest() throws Exception {
        assertThat(isRequest(), matches(request()));
        assertThat(isRequest(), not(matches(response())));
    }

    @Test
    public void testIsResponse() throws Exception {
        assertThat(isResponse(), matches(response()));
        assertThat(isResponse(), not(matches(request())));
    }

    @Test
    public void testResponseStatusIs() throws Exception {
        assertThat(responseStatusIs(201), matches(response().withStatus(201)));
        assertThat(responseStatusIs(201), not(matches(response().withStatus(200))));
        assertThat(responseStatusIs(200), not(matches(request())));
    }

    @Test
    public void testResponseStatusTypeIs() throws Exception {
        assertThat(responseStatusTypeIs(StatusType.SUCCESS), matches(response().withStatus(231)));
        assertThat(responseStatusTypeIs(StatusType.SUCCESS), not(matches(response().withStatus(300))));
        assertThat(responseStatusTypeIs(StatusType.SUCCESS), not(matches(request())));
    }

    @Test
    public void testMethodIs() throws Exception {
        assertThat(methodIs(HttpMethod.PUT), matches(request().withMethod(HttpMethod.PUT)));
        assertThat(methodIs(HttpMethod.PUT), matches(response().withMethod(HttpMethod.PUT)));
        assertThat(methodIs(HttpMethod.PUT), not(matches(response().withMethod(HttpMethod.DELETE))));
    }

    private Matcher<WhitelistRule> matches(OperationForWhitelisting operation) {
        return new TypeSafeMatcher<WhitelistRule>() {
            @Override
            protected boolean matchesSafely(WhitelistRule whitelistRule) {
                return operation.isMatchedBy(whitelistRule);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("A rule that matches: " + operation);
            }
        };
    }
}