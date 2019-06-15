package com.atlassian.oai.validator.springmvc;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link javax.servlet.http.HttpServletRequestWrapper} those {@link ServletInputStream} is
 * cached and can be reset and read again as often as necessary.
 */
public class ResettableRequestServletWrapper extends HttpServletRequestWrapper {

    private ServletInputStream servletInputStream;
    private BufferedReader reader;
    private Integer contentLength;

    ResettableRequestServletWrapper(final HttpServletRequest request) {
        super(request);
    }

    /**
     * Will rewind the {@link ServletInputStream} to its beginning so it can be
     * read again.
     * <p>
     * Additionally the {@link #getReader()} will be removed if it was used. It will be
     * created again on its next usage.
     * <p>
     * Before resetting the stream the first time it has to be read entirely. The
     * reset will only provide a view from position 0 to the already read bytes from
     * that stream.
     * If the stream wasn't read at all the content will be empty.
     */
    public void resetInputStream() throws IOException {
        if (servletInputStream == null) {
            this.servletInputStream = CachedServletInputStream.empty();
            this.contentLength = -1;
        } else if (servletInputStream instanceof WrappedOriginalServletInputStream) {
            final WrappedOriginalServletInputStream wrapped = (WrappedOriginalServletInputStream) servletInputStream;
            this.servletInputStream = new CachedServletInputStream(wrapped.cachedContent, wrapped.chunkSize, wrapped.count);
            this.contentLength = wrapped.count;
        } else if (servletInputStream instanceof CachedServletInputStream) {
            // just reset the already cached stream
            this.servletInputStream.reset();
        }
        this.reader = null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (servletInputStream == null) {
            this.servletInputStream = new WrappedOriginalServletInputStream(super.getInputStream());
        }
        return this.servletInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.reader == null) {
            final String encoding = super.getCharacterEncoding();
            final InputStreamReader streamReader = encoding == null ?
                    new InputStreamReader(this.getInputStream()) : new InputStreamReader(this.getInputStream(), encoding);
            this.reader = new BufferedReader(streamReader);
        }
        return this.reader;
    }

    @Override
    public int getContentLength() {
        return contentLength != null ? contentLength : super.getContentLength();
    }

    /**
     * A {@link ServletInputStream} wrapping the original request and saving all read bytes.
     */
    private static class WrappedOriginalServletInputStream extends ServletInputStream {

        private final ServletInputStream originalServletInputStream;
        private final List<byte[]> cachedContent;
        private final int chunkSize;
        private int count = 0;

        private WrappedOriginalServletInputStream(final ServletInputStream originalServletInputStream) {
            this.originalServletInputStream = originalServletInputStream;
            this.cachedContent = new ArrayList<>();
            this.chunkSize = 8192;
        }

        @Override
        public boolean isFinished() {
            return originalServletInputStream.isFinished();
        }

        @Override
        public boolean isReady() {
            return originalServletInputStream.isReady();
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            originalServletInputStream.setReadListener(readListener);
        }

        @Override
        public synchronized int read() throws IOException {
            final int value = originalServletInputStream.read();
            if (value != -1) {
                ensureCapacity();
                cachedContent.get(cachedContent.size() - 1)[count++ % chunkSize] = (byte) value;
            }
            return value;
        }

        @Override
        public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
            final int result = originalServletInputStream.read(b, off, len);
            if (result > 0) {
                int leftLen = result;
                int srcPos = off;
                while (leftLen > 0) {
                    ensureCapacity();
                    final int destPos = count % chunkSize;
                    final int length = Math.min(leftLen, chunkSize - destPos);
                    System.arraycopy(b, srcPos, cachedContent.get(cachedContent.size() - 1), destPos, length);
                    leftLen -= length;
                    srcPos += length;
                    this.count += length;
                }
            }
            return result;
        }

        private void ensureCapacity() {
            if (count % chunkSize == 0) {
                cachedContent.add(new byte[chunkSize]);
            }
        }
    }

    /**
     * A {@link ServletInputStream} backed by a non-blocking {@link java.io.InputStream}
     * which can be reset freely.
     */
    private static class CachedServletInputStream extends ServletInputStream {

        private final List<byte[]> cachedContent;
        private final int chunkSize;
        private final int count;
        private int pos = 0;

        private CachedServletInputStream(final List<byte[]> cachedContent, final int chunkSize, final int count) {
            this.cachedContent = cachedContent;
            this.chunkSize = chunkSize;
            this.count = count;
        }

        private static CachedServletInputStream empty() {
            return new CachedServletInputStream(null, -1, -1);
        }

        @Override
        public synchronized int read() {
            // mostly copied from ByteArrayInputStream#read()
            return this.pos < this.count ? cachedContent.get(pos / chunkSize)[pos++ % chunkSize] & 255 : -1;
        }

        @Override
        public synchronized int read(final byte[] b, final int off, int len) {
            // mostly copied from ByteArrayInputStream#read(byte[], int, int)
            if (this.pos >= this.count) {
                return -1;
            } else {
                final int avail = this.count - this.pos;
                if (len > avail) {
                    len = avail;
                }

                if (len <= 0) {
                    return 0;
                } else {
                    int leftLen = len;
                    int destPos = off;
                    while (leftLen > 0) {
                        final int srcPos = pos % chunkSize;
                        final int length = Math.min(leftLen, chunkSize - srcPos);
                        System.arraycopy(cachedContent.get(pos / chunkSize), srcPos, b, destPos, length);
                        leftLen -= length;
                        destPos += length;
                        this.pos += length;
                    }
                    return len;
                }
            }
        }

        @Override
        public boolean isFinished() {
            return pos >= count;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            throw new IllegalStateException("Can't set ReadListener on CachedServletInputStream.");
        }

        @Override
        public void reset() {
            pos = 0;
        }
    }
}