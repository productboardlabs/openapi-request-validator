package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import java.util.UUID;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFailWithoutContext;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UUIDFormatValidatorTest {

    private static final String EXPECTED_KEY = "validation.request.parameter.string.uuid.invalid";

    private final UUIDFormatValidator classUnderTest = new UUIDFormatValidator(new MessageResolver());

    @Test
    public void supports_uuid_format() {
        assertThat(classUnderTest.supports("uuid"), is(true));
        assertThat(classUnderTest.supports("other"), is(false));
    }

    @Test
    public void passes_whenValid() {
        assertPass(classUnderTest.validate(UUID.randomUUID().toString()));
    }

    @Test
    public void fails_whenInvalid() {
        assertFailWithoutContext(classUnderTest.validate("notauuid"), EXPECTED_KEY);
    }

    @Test
    public void fails_whenEmpty() {
        assertFailWithoutContext(classUnderTest.validate(""), EXPECTED_KEY);
    }

}