package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ValidatedPactProviderRuleTest {

    @Test
    public void constructor_withoutHostAndPort() {
        // given:
        final String swaggerJsonUrl = "oai/api-users.json";
        final String basePathOverride = "/api";
        final String providerId = "Users";
        final Object target = this;

        // when:
        final ValidatedPactProviderRule result = new ValidatedPactProviderRule(swaggerJsonUrl, basePathOverride,
                providerId, target);

        // then:
        assertThat(result, notNullValue());
        assertThat(result.getConfig(), notNullValue());
    }

    @Test
    public void constructor_withHostAndPort() {
        // given:
        final String swaggerJsonUrl = "oai/api-users.json";
        final String basePathOverride = "/api";
        final String providerId = "Users";
        final String host = "example.com";
        final Integer port = 8888;
        final Object target = this;

        // when:
        final ValidatedPactProviderRule result = new ValidatedPactProviderRule(swaggerJsonUrl, basePathOverride,
                providerId, host, port, target);

        // then:
        assertThat(result, notNullValue());
        assertThat(result.getConfig(), notNullValue());
        assertThat(result.getConfig().getHostname(), is(host));
        assertThat(result.getConfig().getPort(), is(port));
    }

    @Test
    public void constructor_withPreConfigiredValidator() {
        // given:
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createForSpecificationUrl("oai/api-users.json")
                .withBasePathOverride("/api")
                .build();
        final String providerId = "Users";
        final String host = "example.com";
        final Integer port = 8888;
        final Object target = this;

        // when:
        final ValidatedPactProviderRule result = new ValidatedPactProviderRule(validator, providerId, host, port, target);

        // then:
        assertThat(result, notNullValue());
        assertThat(result.getConfig(), notNullValue());
        assertThat(result.getConfig().getHostname(), is(host));
        assertThat(result.getConfig().getPort(), is(port));
    }
}
