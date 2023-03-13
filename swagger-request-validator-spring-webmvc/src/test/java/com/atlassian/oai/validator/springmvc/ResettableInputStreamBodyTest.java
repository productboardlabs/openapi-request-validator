package com.atlassian.oai.validator.springmvc;

import java.io.IOException;
import java.lang.reflect.Constructor;

import jakarta.servlet.ServletInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.atlassian.oai.validator.model.Body;
import com.fasterxml.jackson.databind.JsonNode;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResettableInputStreamBodyTest {
    final ResettableRequestServletWrapper.CachingServletInputStream cachingServletInputStream =
            mock(ResettableRequestServletWrapper.CachingServletInputStream.class);
    final Body classUnderTest = new ResettableInputStreamBody(cachingServletInputStream);

    private void mockEmptyJsonStringOnInputStream() throws IOException {
        final ArgumentCaptor<byte[]> bytesArgument = ArgumentCaptor.forClass(byte[].class);
        when(cachingServletInputStream.read(bytesArgument.capture(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    // set an empty JSON string into the bytes that are read
                    bytesArgument.getValue()[0] = '{';
                    bytesArgument.getValue()[1] = '}';
                    return 2;
                })
                .thenReturn(-1);
    }

    @Test
    public void hasBody_no_theBodyIsEmpty() throws IOException {
        // given:
        when(cachingServletInputStream.read()).thenReturn(-1);

        // when:
        final boolean result = classUnderTest.hasBody();

        // then:
        verify(cachingServletInputStream).reset();
        assertThat(result).isFalse();
    }

    @Test
    public void hasBody_no_errorOnReadingFirstByte() throws IOException {
        // given:
        when(cachingServletInputStream.read()).thenThrow(new IOException("Empty body."));

        // when:
        final boolean result = classUnderTest.hasBody();

        // then:
        verify(cachingServletInputStream, never()).reset();
        assertThat(result).isFalse();
    }

    @Test
    public void hasBody_yes() throws IOException {
        // given:
        when(cachingServletInputStream.read()).thenReturn(12);

        // when:
        final boolean result = classUnderTest.hasBody();

        // then:
        verify(cachingServletInputStream).reset();
        assertThat(result).isTrue();
    }

    @Test
    public void toJsonNode() throws IOException {
        // given:
        mockEmptyJsonStringOnInputStream();

        // when:
        final JsonNode result = classUnderTest.toJsonNode();

        // then:
        verify(cachingServletInputStream).reset();
        assertThat(result).isEmpty();
    }

    @Test
    public void toString_encoding() throws IOException {
        // given:
        mockEmptyJsonStringOnInputStream();

        // when:
        final String result = classUnderTest.toString(UTF_8);

        // then:
        verify(cachingServletInputStream).reset();
        assertThat(result).isEqualTo("{}");
    }

    @ParameterizedTest
    @CsvSource({"'',false,false", "'{\"key\": \"value\"}',true,true", "'body',true,false"})
    public void testingWithRealCachingServletInputStream(final String body, final boolean hasBody, final boolean bodyIsJson) throws Exception {
        // given:
        final Constructor<ResettableRequestServletWrapper.CachingServletInputStream> constructor =
                ResettableRequestServletWrapper.CachingServletInputStream.class.getDeclaredConstructor(ServletInputStream.class);
        constructor.setAccessible(true);
        final ServletInputStream servletInputStream = new ServletInputStreamMock(body.getBytes(UTF_8));
        final ResettableRequestServletWrapper.CachingServletInputStream realCachingServletInputStream = constructor.newInstance(servletInputStream);

        // and:
        final Body testedBody = new ResettableInputStreamBody(realCachingServletInputStream);

        // expect:
        assertThat(testedBody.hasBody()).isEqualTo(hasBody);

        // and: 'the body can be read multiple times'
        assertThat(testedBody.toString(UTF_8)).isEqualTo(body);
        assertThat(testedBody.toString(UTF_8)).isEqualTo(body);
        assertThat(testedBody.toString(UTF_8)).isEqualTo(body);

        // and: 'JSON bodies can be read multiple times, too'
        if (bodyIsJson) {
            assertThat(testedBody.toJsonNode()).isNotNull();
            assertThat(testedBody.toJsonNode()).isNotNull();
            assertThat(testedBody.toJsonNode()).isNotNull();
        }
    }
}
