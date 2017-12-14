package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EmailFormatValidatorTest {

    private final EmailFormatValidator classUnderTest = new EmailFormatValidator(new MessageResolver());

    @Test
    public void supports_email_format() {
        assertThat(classUnderTest.supports("email"), is(true));
        assertThat(classUnderTest.supports("other"), is(false));
    }

    @Test
    public void passes_whenValidEmail() {
        assertPass(classUnderTest.validate("some.body@some-where.co.uk"));
    }

    @Test
    public void fails_whenInvalidEmail() {
        assertFail(classUnderTest.validate("not@anemail@"), "validation.request.parameter.string.email.invalid");
    }

    @Test
    public void fails_whenEmpty() {
        assertFail(classUnderTest.validate(""), "validation.request.parameter.string.email.invalid");
    }

}