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

import static java.lang.Byte.toUnsignedInt;

/**
 * A {@link javax.servlet.http.HttpServletRequestWrapper} those {@link ServletInputStream} is
 * cached and can be reset and read again as often as necessary.
 */
public class ResettableRequestServletWrapper extends HttpServletRequestWrapper {

    private CachingServletInputStream servletInputStream;
    private BufferedReader reader;
    private Long contentLengthLong;

    public ResettableRequestServletWrapper(final HttpServletRequest request) {
        super(request);
    }

    /**
     * In case the original input stream has been read, the {@link ServletInputStream} will
     * be rewind to its beginning so it can be read again.
     * <p>
     * Additionally the {@link #getReader()} will be removed if it was used. It will be
     * created again on its next usage. If the original stream has been exhausted the
     * content length will be set as well.
     * <p>
     * To reset the input stream it is not necessary to exhaust it beforehand.
     */
    public void resetInputStream() {
        if (servletInputStream != null) {
            this.servletInputStream.reset();
            this.reader = null;

            // if the stream has been read fully set the content length
            if (servletInputStream.isExhausted()) {
                this.contentLengthLong = servletInputStream.getContentLength();
            }
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (servletInputStream == null) {
            this.servletInputStream = new CachingServletInputStream(super.getInputStream());
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
        return contentLengthLong == null ? super.getContentLength() :
                contentLengthLong <= Integer.MAX_VALUE ? contentLengthLong.intValue() : -1;
    }

    @Override
    public long getContentLengthLong() {
        return contentLengthLong == null ? super.getContentLengthLong() : contentLengthLong;
    }

    /**
     * A {@link ServletInputStream} wrapping the original request and saving all read bytes.
     */
    static class CachingServletInputStream extends ServletInputStream {
        private static final int CHUNK_SIZE = 8192;

        private final ServletInputStream originalServletInputStream;
        private final List<byte[]> cachedContent = new ArrayList<>();
        private long count = 0;
        private long pos = 0;
        private boolean exhausted = false;

        private CachingServletInputStream(final ServletInputStream originalServletInputStream) {
            this.originalServletInputStream = originalServletInputStream;
        }

        private boolean isExhausted() {
            return exhausted;
        }

        private Long getContentLength() {
            // only if the stream has been read completely its length is known
            return exhausted ? count : null;
        }

        @Override
        public boolean isFinished() {
            // According to the spec this method "returns true when all the data from the stream has
            // been read else it returns false". That is the case if the original stream is exhausted
            // and there is no cached data left.
            return pos >= count && exhausted;
        }

        @Override
        public boolean isReady() {
            // According to the spec this method "returns true if data can be read without blocking
            // else returns false". That is the case if cached data is available or the original
            // stream has data available.
            return pos < count || originalServletInputStream.isReady();
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            originalServletInputStream.setReadListener(readListener);
        }

        @Override
        public synchronized int read() throws IOException {
            if (pos >= count && exhausted) {
                return -1;
            }

            // use the data from the cache if applicable
            if (pos < count) {
                return toUnsignedInt(getChunkForCurrentPos()[(int) (pos++ % CHUNK_SIZE)]);
            }

            // read the data from the original stream
            // On async environments it is assumed that "isReady()" has been called beforehand to ensure
            // data is available on the stream.
            final int value = originalServletInputStream.read();
            if (value == -1) {
                exhausted = true;
                return -1;
            }

            // cache the data
            final int index = (int) (count % CHUNK_SIZE);
            getLatestChunk(index)[index] = (byte) value;
            ++count;
            ++pos;

            return value;
        }

        @Override
        public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
            if (pos >= count && exhausted) {
                return -1;
            }
            if (len == 0) {
                return 0;
            }

            // use the data from the cache if applicable
            if (pos < count) {
                // To ensure a non-blocking state in async environments only return the available cached
                // data. It is not guaranteed that the original stream contains data, see: "isReady()"
                int result = 0;
                while (result < len && pos < count) {
                    final int index = (int) pos % CHUNK_SIZE;
                    final int length = (int) Math.min(CHUNK_SIZE - index, Math.min(len - result, count - pos));
                    System.arraycopy(getChunkForCurrentPos(), index, b, off + result, length);
                    pos += length;
                    result += length;
                }
                return result;
            }

            // read the data from the original stream
            // On async environments it is assumed that "isReady()" has been called beforehand to ensure
            // data is available on the stream.
            final int bytesRead = originalServletInputStream.read(b, off, len);
            if (bytesRead == -1) {
                exhausted = true;
                return -1;
            }

            // cache the data
            for (int i = 0; i < bytesRead;) {
                final int index = (int) pos % CHUNK_SIZE;
                final int length = Math.min(CHUNK_SIZE - index, bytesRead - i);
                System.arraycopy(b, off + i, getLatestChunk(index), index, length);
                count += length;
                pos += length;
                i += length;
            }

            return bytesRead;
        }

        @Override
        public synchronized void reset() {
            pos = 0;
        }

        private byte[] getChunkForCurrentPos() {
            return cachedContent.get((int) (pos / CHUNK_SIZE));
        }

        private byte[] getLatestChunk(final int remainder) {
            if (remainder == 0) {
                cachedContent.add(new byte[CHUNK_SIZE]);
            }
            return cachedContent.get(cachedContent.size() - 1);
        }
    }
}