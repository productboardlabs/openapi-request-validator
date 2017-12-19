package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MessageResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class URIFormatValidatorTest {

    private final URIFormatValidator classUnderTest = new URIFormatValidator(new MessageResolver());
    private static final String EXPECTED_KEY = "validation.request.parameter.string.uri.invalid";

    @Test
    public void supports_ipv4_format() {
        assertThat(classUnderTest.supports("uri"), is(true));
        assertThat(classUnderTest.supports("other"), is(false));
    }

    @Test
    public void passes_whenValid() {
        assertPass(classUnderTest.validate("http://192.168.0.1:8080"));
        assertPass(classUnderTest.validate("www.example.com"));
        assertPass(classUnderTest.validate("file:///usr/etc"));
    }

    @Test
    public void fails_whenInvalid() {
        // Turns out the URI spec is very encompassing - almost anything with valid characters is a valid URI.
        assertFail(classUnderTest.validate("<>$$"), EXPECTED_KEY);
        assertFail(classUnderTest.validate("h%%"), EXPECTED_KEY);
    }

    @Test
    public void fails_whenEmpty() {
        assertFail(classUnderTest.validate(""), EXPECTED_KEY);
    }

}