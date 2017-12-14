package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import java.util.UUID;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UUIDFormatValidatorTest {

    private final UUIDFormatValidator classUnderTest = new UUIDFormatValidator(new MessageResolver());

    @Test
    public void supports_uuid_format() {
        assertThat(classUnderTest.supports("uuid"), is(true));
        assertThat(classUnderTest.supports("other"), is(false));
    }

    @Test
    public void passes_whenValidUuid() {
        assertPass(classUnderTest.validate(UUID.randomUUID().toString()));
    }

    @Test
    public void fails_whenInvalidUuid() {
        assertFail(classUnderTest.validate("notauuid"), "validation.request.parameter.string.uuid.invalid");
    }

    @Test
    public void fails_whenEmpty() {
        assertFail(classUnderTest.validate(""), "validation.request.parameter.string.uuid.invalid");
    }

}