package com.atlassian.oai.validator.springmvc;

import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link javax.servlet.http.HttpServletRequestWrapper} those {@link ServletInputStream} can
 * be cached, reset and read again as often as necessary.
 * <p>
 * Asynchronous requests are not supported.
 */
class ResettableRequestServletWrapper extends ContentCachingRequestWrapper {

    private ServletInputStream servletInputStream;

    ResettableRequestServletWrapper(final HttpServletRequest request) {
        super(request);
        this.servletInputStream = null;
    }

    /**
     * Will rewind the {@link ServletInputStream} to its beginning so it can be
     * read again.
     * <p>
     * Before resetting the stream the first time it has to be read entirely. The
     * reset will only provide a view from mark 0 to the already read bytes from
     * that stream.
     */
    void resetInputStream() throws IOException {
        final InputStream inputStream = getInputStream();

        if (inputStream instanceof CachedServletInputStream) {
            // reset the cached stream
            ((CachedServletInputStream) inputStream).reset();
        } else {
            // replace the original ServletInputStream with a CachedServletInputStream
            // using the already read bytes from the original
            final byte[] bytes = getContentAsByteArray();
            this.servletInputStream = new CachedServletInputStream(bytes);
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (servletInputStream == null) {
            this.servletInputStream = super.getInputStream();
        }
        return servletInputStream;
    }

    /**
     * A {@link ServletInputStream} backed by a non-blocking {@link java.io.InputStream}
     * which can be reset freely.
     */
    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(final byte[] bytes) {
            this.inputStream = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            // read listeners will only be set on async requests
            throw new IllegalStateException("The current request is not async.");
        }

        @Override
        public void reset() {
            inputStream.reset();
        }
    }
}