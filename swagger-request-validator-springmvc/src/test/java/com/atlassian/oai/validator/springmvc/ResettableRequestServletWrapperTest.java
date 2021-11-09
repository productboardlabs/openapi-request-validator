package com.atlassian.oai.validator.springmvc;

import com.google.common.primitives.Bytes;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.io.ByteStreams.exhaust;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ResettableRequestServletWrapperTest {

    @Test
    public void resetInputStream_emptyBody() throws IOException {
        // read with input stream
        testingFullReadingAndResettingInputStream(0, requestServletWrapper -> toByteArray(requestServletWrapper.getInputStream()));
        // read with reader
        testingFullReadingAndResettingInputStream(0, requestServletWrapper -> toByteArray(requestServletWrapper.getReader(), UTF_8));
        // read with custom reading stream
        testingFullReadingAndResettingInputStream(0, requestServletWrapper -> customReadingStream(requestServletWrapper.getInputStream()));
    }

    @Test
    public void resetInputStream_shortBody() throws IOException {
        // read with input stream
        testingFullReadingAndResettingInputStream(12, requestServletWrapper -> toByteArray(requestServletWrapper.getInputStream()));
        // read with reader
        testingFullReadingAndResettingInputStream(24, requestServletWrapper -> toByteArray(requestServletWrapper.getReader(), UTF_8));
        // read with custom reading stream
        testingFullReadingAndResettingInputStream(48, requestServletWrapper -> customReadingStream(requestServletWrapper.getInputStream()));
        // partial read stream
        testingPartialReadingAndResettingInputStream(96, 4);
    }

    @Test
    public void resetInputStream_longBody() throws IOException {
        // read with input stream
        testingFullReadingAndResettingInputStream(10001, requestServletWrapper -> toByteArray(requestServletWrapper.getInputStream()));
        // read with reader
        testingFullReadingAndResettingInputStream(13004, requestServletWrapper -> toByteArray(requestServletWrapper.getReader(), UTF_8));
        // read with custom reading stream
        testingFullReadingAndResettingInputStream(17008, requestServletWrapper -> customReadingStream(requestServletWrapper.getInputStream()));
        // partial read stream - small peek into the original stream
        testingPartialReadingAndResettingInputStream(22013, 2);
        // partial read stream - big peek into the original stream
        testingPartialReadingAndResettingInputStream(28019, 25897);
    }

    @Test // fix for: https://bitbucket.org/atlassian/swagger-request-validator/issues/367
    public void resetInputStream_binaryBody_read() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        final byte[] bytes = new byte[]{-6, -5, -4, -3, -2};

        final ServletInputStream servletInputStream = new ServletInputStreamMock(bytes);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        // Test: initial read the complete stream
        assertThat(classUnderTest.getInputStream().read(), is(250));
        assertThat(classUnderTest.getInputStream().read(), is(251));
        assertThat(classUnderTest.getInputStream().read(), is(252));
        assertThat(classUnderTest.getInputStream().read(), is(253));
        assertThat(classUnderTest.getInputStream().read(), is(254));
        assertThat(classUnderTest.getInputStream().read(), is(-1));

        // Test: re-read the complete stream
        classUnderTest.resetInputStream();

        assertThat(classUnderTest.getInputStream().read(), is(250));
        assertThat(classUnderTest.getInputStream().read(), is(251));
        assertThat(classUnderTest.getInputStream().read(), is(252));
        assertThat(classUnderTest.getInputStream().read(), is(253));
        assertThat(classUnderTest.getInputStream().read(), is(254));
        assertThat(classUnderTest.getInputStream().read(), is(-1));
    }

    @Test
    public void resetInputStream_before_getInputStream_hasNoEffect() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = new ServletInputStreamMock("test".getBytes(UTF_8));
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        // when:
        classUnderTest.resetInputStream();

        // then:
        assertThat(classUnderTest.getInputStream(), notNullValue());
        verify(servletRequest).getInputStream();
    }

    @Test
    public void isFinished_variousStates() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper wrappedRequest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = new ServletInputStreamMock("bytes".getBytes(UTF_8));
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ServletInputStream classUnderTest = wrappedRequest.getInputStream();

        // Test: no bytes have been read
        assertThat(classUnderTest.isFinished(), is(false));

        // Test: read some bytes
        classUnderTest.read(new byte[3]);

        assertThat(classUnderTest.isFinished(), is(false));

        // Test: exhaust the stream
        exhaust(classUnderTest);

        assertThat(classUnderTest.isFinished(), is(true));

        // Test: after resetting the stream is unfinished again
        classUnderTest.reset();

        assertThat(classUnderTest.isFinished(), is(false));

        // Test: after exhausting the stream again, is is back in finished state
        exhaust(classUnderTest);

        assertThat(classUnderTest.isFinished(), is(true));
    }

    @Test
    public void isReady_variousStates() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper wrappedRequest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = mock(ServletInputStream.class);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ServletInputStream classUnderTest = wrappedRequest.getInputStream();

        // Test: delegates to the original stream if no bytes have been read
        assertThat(classUnderTest.isReady(), is(false));

        verify(servletInputStream).isReady();

        // Test: delegates to the original stream if there is no cached data
        Mockito.reset(servletInputStream);
        when(servletInputStream.read()).thenReturn(1);
        when(servletInputStream.isReady()).thenReturn(true);

        classUnderTest.read();

        assertThat(classUnderTest.isReady(), is(true));
        verify(servletInputStream).isReady();

        // Test: after a reset there is cached data and the stream is ready
        Mockito.reset(servletInputStream);
        classUnderTest.reset();

        assertThat(classUnderTest.isReady(), is(true));
        verifyNoInteractions(servletInputStream);
    }

    @Test
    public void setReadListener_isDelegatedToTheOriginalServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper wrappedRequest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = mock(ServletInputStream.class);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ServletInputStream classUnderTest = wrappedRequest.getInputStream();
        final ReadListener readListener = mock(ReadListener.class);

        // when:
        classUnderTest.setReadListener(readListener);

        // then:
        verify(servletInputStream).setReadListener(readListener);
    }

    @Test
    public void getContentLength_notSetOnOriginalServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        final ServletInputStream servletInputStream = new ServletInputStreamMock(new byte[]{0x00, 0x01, 0x02});
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test: 'not set before reading stream'
        when(servletRequest.getContentLength()).thenReturn(-1);
        when(servletRequest.getContentLengthLong()).thenReturn(-1L);
        assertThat(classUnderTest.getContentLength(), is(-1));
        assertThat(classUnderTest.getContentLengthLong(), is(-1L));
        verify(servletRequest).getContentLength();
        verify(servletRequest).getContentLengthLong();

        // Test: 'set after exhausting and resetting stream'
        exhaust(classUnderTest.getInputStream());
        classUnderTest.resetInputStream();
        assertThat(classUnderTest.getContentLength(), is(3));
        assertThat(classUnderTest.getContentLengthLong(), is(3L));
    }

    @Test
    public void getContentLength_setOnOriginalServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        final ServletInputStream servletInputStream = new ServletInputStreamMock(new byte[]{0x00, 0x01, 0x02});
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test: 'wrongly set on original request'
        when(servletRequest.getContentLength()).thenReturn(2);
        when(servletRequest.getContentLengthLong()).thenReturn(2L);
        assertThat(classUnderTest.getContentLength(), is(2));
        assertThat(classUnderTest.getContentLengthLong(), is(2L));
        verify(servletRequest).getContentLength();
        verify(servletRequest).getContentLengthLong();

        // Test: 'corrected after exhausting and resetting stream'
        exhaust(classUnderTest.getInputStream());
        classUnderTest.resetInputStream();
        assertThat(classUnderTest.getContentLength(), is(3));
        assertThat(classUnderTest.getContentLengthLong(), is(3L));
    }

    @Test
    public void getContentLength_after_resetInputStream_withoutReadingBody() {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test: 'still calls the original request'
        classUnderTest.resetInputStream();
        when(servletRequest.getContentLength()).thenReturn(-1);
        when(servletRequest.getContentLengthLong()).thenReturn(-1L);
        assertThat(classUnderTest.getContentLength(), is(-1));
        assertThat(classUnderTest.getContentLengthLong(), is(-1L));
        verify(servletRequest).getContentLength();
        verify(servletRequest).getContentLengthLong();
    }

    private void testingFullReadingAndResettingInputStream(final int contentLength, final ContentReader initialContentReader) throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        final String content = RandomStringUtils.random(contentLength);
        final byte[] bytes = content.getBytes();

        final ServletInputStream servletInputStream = new ServletInputStreamMock(bytes);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        // Test: initial reading the stream - this will fill the cache
        final byte[] initialRead = initialContentReader.read(classUnderTest);
        assertThat(bytes, equalTo(initialRead));

        // Test: reading more content from the exhausted stream does no harm
        assertThat(servletInputStream.read(), is(-1));
        assertThat(servletInputStream.read(new byte[10]), is(-1));
        assertThat(servletInputStream.read(new byte[10], 2, 4), is(-1));

        // Test: reset the input stream and reread it again
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithStream = toByteArray(classUnderTest.getInputStream());
        assertThat(bytes, equalTo(readAfterResetWithStream));

        // Test: reset the input stream and reread it again from the buffered reader
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithReader = toByteArray(classUnderTest.getReader(), UTF_8);
        assertThat(bytes, equalTo(readAfterResetWithReader));

        // Test: reset the input stream and reread it again from the buffered reader with set charset
        classUnderTest.resetInputStream();

        when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");
        final byte[] readAfterResetWithReaderWithCharset = toByteArray(classUnderTest.getReader(), UTF_8);
        assertThat(bytes, equalTo(readAfterResetWithReaderWithCharset));

        // Test: reset the input stream and reread it again with plain reading the stream
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithPlainReading = customReadingStream(classUnderTest.getInputStream());
        assertThat(bytes, equalTo(readAfterResetWithPlainReading));
    }

    private void testingPartialReadingAndResettingInputStream(final int contentLength, final int partialReadLength) throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        final String content = RandomStringUtils.random(contentLength);
        final byte[] bytes = content.getBytes();

        final ServletInputStream servletInputStream = new ServletInputStreamMock(bytes);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        // Test: initial reading the stream - this will partially fill the cache
        final byte[] initialRead = toByteArray(classUnderTest.getInputStream(), partialReadLength);
        assertThat(Arrays.copyOf(bytes, partialReadLength), equalTo(initialRead));

        // Test: reset the input stream and reread the first byte
        classUnderTest.resetInputStream();

        final byte firstByte = (byte) classUnderTest.getInputStream().read();
        assertThat(firstByte, equalTo(bytes[0]));

        // Test: further reading stream - only the available cached data is returned, the original stream is not read further to prevent blocking
        final byte[] moreBytes = new byte[partialReadLength];
        final int readBytes1 = classUnderTest.getInputStream().read(moreBytes);
        assertThat(readBytes1, equalTo(partialReadLength - 1));
        assertThat(moreBytes[partialReadLength - 1], equalTo((byte) 0));
        assertThat(copyOfRange(bytes, 1, partialReadLength), equalTo(copyOfRange(moreBytes, 0, partialReadLength - 1)));

        // Test: further reading stream - now the original stream is read further
        final byte[] evenMoreBytes = new byte[10];
        final int readBytes2 = classUnderTest.getInputStream().read(evenMoreBytes);
        assertThat(readBytes2, equalTo(10));
        assertThat(copyOfRange(bytes, partialReadLength, partialReadLength + 10), equalTo(evenMoreBytes));

        // Test: reset the input stream and reread it fully
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithStream = toByteArray(classUnderTest.getInputStream());
        assertThat(bytes, equalTo(readAfterResetWithStream));

        // Test: reset the input stream and reread it again from the buffered reader with set charset
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithReaderWithCharset = toByteArray(classUnderTest.getReader(), UTF_8);
        assertThat(bytes, equalTo(readAfterResetWithReaderWithCharset));

        // Test: reset the input stream and reread it again with plain reading the stream
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithPlainReading = customReadingStream(classUnderTest.getInputStream());
        assertThat(bytes, equalTo(readAfterResetWithPlainReading));
    }

    private byte[] customReadingStream(final InputStream inputStream) throws IOException {
        final List<Byte> list = new ArrayList<>();
        while (true) {
            final int value = inputStream.read();
            if (value == -1) {
                break;
            }
            list.add((byte) value);
        }
        return Bytes.toArray(list);
    }

    @FunctionalInterface
    private interface ContentReader {
        byte[] read(final ResettableRequestServletWrapper requestServletWrapper) throws IOException;
    }
}
