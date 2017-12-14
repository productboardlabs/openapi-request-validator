package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IPv6FormatValidatorTest {

    private static final String EXPECTED_KEY = "validation.request.parameter.string.ipv6.invalid";
    
    private final IPv6FormatValidator classUnderTest = new IPv6FormatValidator(new MessageResolver());

    @Test
    public void supports_ipv6_format() {
        assertThat(classUnderTest.supports("ipv6"), is(true));
        assertThat(classUnderTest.supports("other"), is(false));
    }

    @Test
    public void passes_whenValid() {
        assertPass(classUnderTest.validate("2001:0db8:0000:0000:0000:ff00:0042:8329"));
        assertPass(classUnderTest.validate("2001:db8:0:0:0:ff00:42:8329"));
        assertPass(classUnderTest.validate("2001:db8::ff00:42:8329"));
        assertPass(classUnderTest.validate("::1"));
    }

    @Test
    public void fails_whenInvalid() {
        assertFail(classUnderTest.validate(":1"), EXPECTED_KEY);
        assertFail(classUnderTest.validate("2001:db8:0:0:0:gf00:42:8329"), EXPECTED_KEY);
        assertFail(classUnderTest.validate("192.168.0.1"), EXPECTED_KEY);
        assertFail(classUnderTest.validate("floop"), EXPECTED_KEY);
    }

    @Test
    public void fails_whenEmpty() {
        assertFail(classUnderTest.validate(""), EXPECTED_KEY);
    }

}