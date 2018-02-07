package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import io.swagger.models.parameters.PathParameter;
import org.junit.Test;

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
                .withContext(
                        MessageContext.create()
                                .withRequestPath("request.path")
                                .build()
                )
                .build();

        final Message enhancedMsg = msg.withAdditionalContext(
                MessageContext.create()
                        .withParameter(new PathParameter().name("test.param"))
                        .build());

        assertThat(enhancedMsg, not(is(msg)));

        final MessageContext context = enhancedMsg.getContext().orElse(null);
        assertThat(context, is(notNullValue()));
        assertThat(context.getRequestPath(), optionalWithValue(is("request.path")));
        assertThat(context.getParameter(), optionalWithValue(hasProperty("name", is("test.param"))));
    }

    @Test
    public void mergingContext_appliesContextToNewMsg_whenContextDoesNotExist() {

        final Message msg = Message
                .create("test.key", "test.msg")
                .build();

        final Message enhancedMsg = msg.withAdditionalContext(
                MessageContext.create()
                        .withRequestPath("request.path")
                        .build());

        assertThat(enhancedMsg, not(is(msg)));

        final MessageContext context = enhancedMsg.getContext().orElse(null);
        assertThat(context, is(notNullValue()));
        assertThat(context.getRequestPath(), optionalWithValue(is("request.path")));
        assertThat(context.getParameter(), emptyOptional());
    }

}
