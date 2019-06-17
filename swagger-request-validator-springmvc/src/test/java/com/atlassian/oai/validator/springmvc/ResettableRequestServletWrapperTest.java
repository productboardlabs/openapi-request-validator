package com.atlassian.oai.validator.springmvc;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ResettableRequestServletWrapperTest {

    @Test
    public void getInputStream_resetInputStream_emptyBody_initialReadWithInputStream() throws IOException {
        testingReadingAndResettingInputStream(0, requestServletWrapper -> IOUtils.toByteArray(requestServletWrapper.getInputStream()));
    }

    @Test
    public void getInputStream_resetInputStream_emptyBody_initialReadWithReader() throws IOException {
        testingReadingAndResettingInputStream(0, requestServletWrapper -> IOUtils.toByteArray(requestServletWrapper.getReader()));
    }

    @Test
    public void getInputStream_resetInputStream_shortBody_initialReadWithInputStream() throws IOException {
        testingReadingAndResettingInputStream(12, requestServletWrapper -> IOUtils.toByteArray(requestServletWrapper.getInputStream()));
    }

    @Test
    public void getInputStream_resetInputStream_shortBody_initialReadWithReader() throws IOException {
        testingReadingAndResettingInputStream(12, requestServletWrapper -> IOUtils.toByteArray(requestServletWrapper.getReader()));
    }

    @Test
    public void getInputStream_resetInputStream_longBody_initialReadWithInputStream() throws IOException {
        testingReadingAndResettingInputStream(10001, requestServletWrapper -> IOUtils.toByteArray(requestServletWrapper.getInputStream()));
    }

    @Test
    public void getInputStream_resetInputStream_longBody_initialReadWithReader() throws IOException {
        testingReadingAndResettingInputStream(10001, requestServletWrapper -> IOUtils.toByteArray(requestServletWrapper.getReader()));
    }

    @Test
    public void getInputStream_resetInputStream_initialReadWithPlainReading() throws IOException {
        testingReadingAndResettingInputStream(124, requestServletWrapper -> plainReadingStream(requestServletWrapper.getInputStream()));
    }

    @Test
    public void getInputStream_resetInputStream_noBody() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test: reset the input stream before even reading it
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithStream = IOUtils.toByteArray(classUnderTest.getInputStream());
        assertThat(readAfterResetWithStream.length, is(0));

        // Test: reset the input stream again
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithReader = IOUtils.toByteArray(classUnderTest.getReader());
        assertThat(readAfterResetWithReader.length, is(0));
    }

    @Test
    public void isFinished_isReady_setReadListener_onOriginalServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper wrappedRequest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = mock(ServletInputStream.class);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ServletInputStream classUnderTest = wrappedRequest.getInputStream();

        // Test:
        when(servletInputStream.isFinished()).thenReturn(true);
        assertThat(classUnderTest.isFinished(), is(true));
        verify(servletInputStream).isFinished();

        // Test:
        when(servletInputStream.isReady()).thenReturn(false);
        assertThat(classUnderTest.isReady(), is(false));
        verify(servletInputStream).isReady();

        // Test:
        final ReadListener readListener = mock(ReadListener.class);
        classUnderTest.setReadListener(readListener);
        verify(servletInputStream).setReadListener(readListener);
    }

    @Test
    public void isFinished_isReady_setReadListener_onCachedServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper wrappedRequest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = new TestServletInputStream(new byte[]{0x00});
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        IOUtils.toString(wrappedRequest.getInputStream());
        wrappedRequest.resetInputStream();
        final ServletInputStream classUnderTest = wrappedRequest.getInputStream();

        // Test:
        assertThat(classUnderTest.isFinished(), is(false));
        assertThat(classUnderTest.isReady(), is(true)); // always true

        classUnderTest.read();
        assertThat(classUnderTest.isFinished(), is(true));
        assertThat(classUnderTest.isReady(), is(true));

        try {
            classUnderTest.setReadListener(mock(ReadListener.class));
            fail("Read listeners can't be set on the cached servlet input stream.");
        } catch (final IllegalStateException expected) {
            // this exception was expected
            assertThat(expected, notNullValue());
        }
    }

    @Test
    public void getContentLength_notSetOnOriginalServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        final ServletInputStream servletInputStream = new TestServletInputStream(new byte[]{0x00, 0x01, 0x02});
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test: 'not set before reading stream'
        when(servletRequest.getContentLength()).thenReturn(-1);
        assertThat(classUnderTest.getContentLength(), is(-1));
        verify(servletRequest).getContentLength();

        // Test: 'set after exhausting and resetting stream'
        ByteStreams.exhaust(classUnderTest.getInputStream());
        classUnderTest.resetInputStream();
        assertThat(classUnderTest.getContentLength(), is(3));
    }

    @Test
    public void getContentLength_setOnOriginalServletInputStream() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        final ServletInputStream servletInputStream = new TestServletInputStream(new byte[]{0x00, 0x01, 0x02});
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test: 'wrongly set on original request'
        when(servletRequest.getContentLength()).thenReturn(2);
        assertThat(classUnderTest.getContentLength(), is(2));
        verify(servletRequest).getContentLength();

        // Test: 'corrected after exhausting and resetting stream'
        ByteStreams.exhaust(classUnderTest.getInputStream());
        classUnderTest.resetInputStream();
        assertThat(classUnderTest.getContentLength(), is(3));
    }

    @Test
    public void getContentLength_withoutReadingBody() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        // Test:
        classUnderTest.resetInputStream();
        assertThat(classUnderTest.getContentLength(), is(-1));
        verifyZeroInteractions(servletRequest);
    }

    private void testingReadingAndResettingInputStream(final int contentLength, final ContentReader initialContentReader) throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        final String content = RandomStringUtils.random(contentLength);
        final byte[] bytes = content.getBytes();

        final ServletInputStream servletInputStream = new TestServletInputStream(bytes);
        when(servletRequest.getInputStream()).thenReturn(servletInputStream);

        // Test: initial reading the stream - this will fill the cache
        final byte[] initialRead = initialContentReader.read(classUnderTest);
        assertArrayEquals(bytes, initialRead);

        // Test: reset the input stream and reread it again
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithStream = IOUtils.toByteArray(classUnderTest.getInputStream());
        assertArrayEquals(bytes, readAfterResetWithStream);

        // Test: reset the input stream and reread it again from the buffered reader
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithReader = IOUtils.toByteArray(classUnderTest.getReader());
        assertArrayEquals(bytes, readAfterResetWithReader);

        // Test: reset the input stream and reread it again from the buffered reader with set charset
        classUnderTest.resetInputStream();

        when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");
        final byte[] readAfterResetWithReaderWithCharset = IOUtils.toByteArray(classUnderTest.getReader());
        assertArrayEquals(bytes, readAfterResetWithReaderWithCharset);

        // Test: reset the input stream and reread it again with plain reading the stream
        classUnderTest.resetInputStream();

        final byte[] readAfterResetWithPlainReading = plainReadingStream(classUnderTest.getInputStream());
        assertArrayEquals(bytes, readAfterResetWithPlainReading);
    }

    private byte[] plainReadingStream(final InputStream inputStream) throws IOException {
        final List<Byte> list = new ArrayList<>();
        while (true) {
            final int value = inputStream.read();
            if (value == -1) {
                break;
            }
            list.add((byte) value);
        }
        final byte[] result = new byte[list.size()];
        for (int i = list.size() - 1; i >= 0; --i) {
            result[i] = list.get(i);
        }
        return result;
    }

    @FunctionalInterface
    private interface ContentReader {
        byte[] read(final ResettableRequestServletWrapper requestServletWrapper) throws IOException;
    }

    private static class TestServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public TestServletInputStream(final byte[] bytes) {
            this.inputStream = new ByteArrayInputStream(bytes);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
        }

        @Override
        public int read() throws IOException {
            return this.inputStream.read();
        }
    }
}
