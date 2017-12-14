package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EmailFormatValidatorTest {

    private static final String EXPECTED_KEY = "validation.request.parameter.string.email.invalid";

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
        assertFail(classUnderTest.validate("not@anemail@"), EXPECTED_KEY);
        assertFail(classUnderTest.validate("notanemail"), EXPECTED_KEY);
        assertFail(classUnderTest.validate("not@@anemail"), EXPECTED_KEY);
    }

    @Test
    public void fails_whenEmpty() {
        assertFail(classUnderTest.validate(""), EXPECTED_KEY);
    }

}