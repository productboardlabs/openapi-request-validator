package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IPv4FormatValidatorTest {

    private final IPv4FormatValidator classUnderTest = new IPv4FormatValidator(new MessageResolver());

    @Test
    public void supports_ipv4_format() {
        assertThat(classUnderTest.supports("ipv4"), is(true));
        assertThat(classUnderTest.supports("other"), is(false));
    }

    @Test
    public void passes_whenValid() {
        assertPass(classUnderTest.validate("127.0.0.1"));
        assertPass(classUnderTest.validate("0.0.0.0"));
        assertPass(classUnderTest.validate("192.168.0.1"));
    }

    @Test
    public void fails_whenInvalid() {
        assertFail(classUnderTest.validate("123.456.789"), "validation.request.parameter.string.ipv4.invalid");
        assertFail(classUnderTest.validate("2001:0db8:0000:0000:0000:ff00:0042:8329"), "validation.request.parameter.string.ipv4.invalid");
        assertFail(classUnderTest.validate("floop"), "validation.request.parameter.string.ipv4.invalid");
    }

    @Test
    public void fails_whenEmpty() {
        assertFail(classUnderTest.validate(""), "validation.request.parameter.string.ipv4.invalid");
    }

}