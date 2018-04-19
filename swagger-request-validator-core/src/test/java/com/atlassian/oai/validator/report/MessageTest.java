package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import org.junit.Test;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParam;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MessageTest {

    @Test
    public void mergingContext_appliesAdditionalContextToNewMsg_whenContextExists() {

        final Message msg = Message
                .create("test.key", "test.msg")
                .withContext(MessageContext.create().in(REQUEST).build())
                .build();

        final Message enhancedMsg = msg.withAdditionalContext(
                MessageContext.create()
                        .withParameter(stringParam())
                        .build());

        assertThat(enhancedMsg, not(is(msg)));

        final MessageContext context = enhancedMsg.getContext().orElse(null);
        assertThat(context, is(notNullValue()));
        assertThat(context.getLocation(), optionalWithValue(is(REQUEST)));
        assertThat(context.getParameter(), optionalWithValue(hasProperty("name", is("Test Parameter"))));
    }

    @Test
    public void mergingContext_appliesContextToNewMsg_whenContextDoesNotExist() {

        final Message msg = Message
                .create("test.key", "test.msg")
                .build();

        final Message enhancedMsg = msg.withAdditionalContext(MessageContext.create().in(REQUEST).build());

        assertThat(enhancedMsg, not(is(msg)));

        final MessageContext context = enhancedMsg.getContext().orElse(null);
        assertThat(context, is(notNullValue()));
        assertThat(context.getLocation(), optionalWithValue(is(REQUEST)));
        assertThat(context.getParameter(), emptyOptional());
    }

}
