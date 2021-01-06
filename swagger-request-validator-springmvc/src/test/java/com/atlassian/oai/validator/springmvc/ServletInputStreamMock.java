package com.atlassian.oai.validator.springmvc;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;

class ServletInputStreamMock extends ServletInputStream {
    private final ByteArrayInputStream inputStream;

    public ServletInputStreamMock(final byte[] bytes) {
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
    public int read() {
        return this.inputStream.read();
    }
}
