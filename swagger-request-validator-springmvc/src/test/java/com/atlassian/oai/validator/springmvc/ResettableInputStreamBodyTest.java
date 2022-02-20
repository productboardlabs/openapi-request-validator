package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.model.Body;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

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
}
