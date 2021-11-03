package com.atlassian.oai.validator.springmvc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import static com.google.common.io.ByteStreams.exhaust;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This benchmark measures the time for reading the cached content of a {@link ServletInputStream}.
 * <p>
 * Three various content sizes will be used for measurement.
 */
@State(Scope.Benchmark)
public class ResettableRequestServletWrapperBenchmark {
    private static final byte[] LARGE_BYTES_BODY = createByteArray(1_500_000);
    private static final byte[] MEDIUM_BYTES_BODY = Arrays.copyOfRange(LARGE_BYTES_BODY, 0, 250_000);
    private static final byte[] SMALL_BYTES_BODY = Arrays.copyOfRange(LARGE_BYTES_BODY, 0, 5_000);

    private ServletInputStream servletInputStreamSmall;
    private ServletInputStream servletInputStreamMedium;
    private ServletInputStream servletInputStreamLarge;

    public static void main(final String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
                .include(ResettableRequestServletWrapperBenchmark.class.getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupIterations(3)
                .measurementIterations(4)
                .build();
        new Runner(options).run();
    }

    private static byte[] createByteArray(final int length) {
        final byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }

    @Setup(Level.Trial)
    public void createServletInputStreamsForBenchmark() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamMock(SMALL_BYTES_BODY));
        servletInputStreamSmall = new ResettableRequestServletWrapper(servletRequest).getInputStream();
        exhaust(servletInputStreamSmall);

        when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamMock(MEDIUM_BYTES_BODY));
        servletInputStreamMedium = new ResettableRequestServletWrapper(servletRequest).getInputStream();
        exhaust(servletInputStreamMedium);

        when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamMock(LARGE_BYTES_BODY));
        servletInputStreamLarge = new ResettableRequestServletWrapper(servletRequest).getInputStream();
        exhaust(servletInputStreamLarge);
    }

    @Setup(Level.Invocation)
    public void resetServletInputStreamsForNextRead() throws IOException {
        servletInputStreamSmall.reset();
        servletInputStreamMedium.reset();
        servletInputStreamLarge.reset();
    }

    @Benchmark
    public void readCachedServletInputStream_small() throws IOException {
        final byte[] result = toByteArray(servletInputStreamSmall);
        assertThat(result[0], is(SMALL_BYTES_BODY[0]));
        assertThat(result[result.length - 1], is(SMALL_BYTES_BODY[SMALL_BYTES_BODY.length - 1]));
    }

    @Benchmark
    public void readCachedServletInputStream_medium() throws IOException {
        final byte[] result = toByteArray(servletInputStreamMedium);
        assertThat(result[0], is(MEDIUM_BYTES_BODY[0]));
        assertThat(result[result.length - 1], is(MEDIUM_BYTES_BODY[MEDIUM_BYTES_BODY.length - 1]));
    }

    @Benchmark
    public void readCachedServletInputStream_large() throws IOException {
        final byte[] result = toByteArray(servletInputStreamLarge);
        assertThat(result[0], is(LARGE_BYTES_BODY[0]));
        assertThat(result[result.length - 1], is(LARGE_BYTES_BODY[LARGE_BYTES_BODY.length - 1]));
    }
}
