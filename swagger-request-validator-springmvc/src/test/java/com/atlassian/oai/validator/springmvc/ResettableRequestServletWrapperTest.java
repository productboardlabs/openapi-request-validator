package com.atlassian.oai.validator.springmvc;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class ResettableRequestServletWrapperTest {

    @Test
    public void getInputStream_resetInputStream_compositeTest() throws IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getContentLength()).thenReturn(10);

        final ResettableRequestServletWrapper classUnderTest = new ResettableRequestServletWrapper(servletRequest);

        final ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);
        Mockito.when(servletRequest.getInputStream()).thenReturn(servletInputStream);
        Mockito.when(servletInputStream.read()).thenReturn(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, -1);

        // read the stream until the end to fill the cache
        final ServletInputStream originalInputStream = classUnderTest.getInputStream();
        while (originalInputStream.read() != -1) {
            // just read until the end
        }

        // when: reset the input stream
        classUnderTest.resetInputStream();

        // then: it is now an cached input stream having its properties
        final ServletInputStream cachedInputStream = classUnderTest.getInputStream();
        Assert.assertThat(cachedInputStream.getClass().getSimpleName(), equalTo("CachedServletInputStream"));
        Assert.assertThat(cachedInputStream.isFinished(), equalTo(false));
        Assert.assertThat(cachedInputStream.isReady(), equalTo(true));
        try {
            final ReadListener readListener = Mockito.mock(ReadListener.class);
            cachedInputStream.setReadListener(readListener);
            Assert.fail("Setting a read listener is not supported.");
        } catch (final IllegalStateException expected) {
            // checkstyle wants something in here
            Assert.assertThat(expected, is(expected));
        }

        // the read bytes matching the previously read bytes
        for (int i = 1; i <= 12; ++i) {
            Assert.assertThat(cachedInputStream.read(), equalTo(i));
        }

        // all subsequent reads are -1 marking the end of the stream
        Assert.assertThat(cachedInputStream.read(), equalTo(-1));
        Assert.assertThat(cachedInputStream.read(), equalTo(-1));
        Assert.assertThat(cachedInputStream.read(), equalTo(-1));

        // the stream is now finished
        Assert.assertThat(cachedInputStream.isFinished(), equalTo(true));

        // when: reset the stream again
        classUnderTest.resetInputStream();

        // then: the stream is the same as before and now reset
        Assert.assertThat(classUnderTest.getInputStream(), is(cachedInputStream));
        Assert.assertThat(cachedInputStream.isFinished(), equalTo(false));
        Assert.assertThat(cachedInputStream.read(), equalTo(1));
    }
}
