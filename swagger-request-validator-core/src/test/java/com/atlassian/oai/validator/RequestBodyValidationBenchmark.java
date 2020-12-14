package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
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

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * This benchmark measures the time for request body validation.
 * <p>
 * Three requests with various body sizes will be used for measurement. The body will either be
 * set as String, byte[] or InputStream making it a total of nine running benchmarks.
 * For better comparison all body types will use the same request content.
 */
@State(Scope.Benchmark)
public class RequestBodyValidationBenchmark {
    private static final String LARGE_BASE64_STRING = createBase64String(1_500_000);
    private static final String SMALL_STRING_BODY = "{\"field\":\"" + LARGE_BASE64_STRING.substring(0, 5_000) + "\"}";
    private static final String MEDIUM_STRING_BODY = "{\"field\":\"" + LARGE_BASE64_STRING.substring(0, 250_000) + "\"}";
    private static final String LARGE_STRING_BODY = "{\"field\":\"" + LARGE_BASE64_STRING.substring(0, 1_500_000) + "\"}";
    private static final byte[] SMALL_BYTES_BODY = SMALL_STRING_BODY.getBytes(UTF_8);
    private static final byte[] MEDIUM_BYTES_BODY = MEDIUM_STRING_BODY.getBytes(UTF_8);
    private static final byte[] LARGE_BYTES_BODY = LARGE_STRING_BODY.getBytes(UTF_8);

    private OpenApiInteractionValidator openApiInteractionValidator;
    private Request requestWithSmallStringBody;
    private Request requestWithMediumStringBody;
    private Request requestWithLargeStringBody;
    private Request requestWithSmallBytesBody;
    private Request requestWithMediumBytesBody;
    private Request requestWithLargeBytesBody;
    private Request requestWithSmallStreamBody;
    private Request requestWithMediumStreamBody;
    private Request requestWithLargeStreamBody;

    public static void main(final String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
                .include(RequestBodyValidationBenchmark.class.getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupIterations(3)
                .measurementIterations(4)
                .build();
        new Runner(options).run();
    }

    private static String createBase64String(final int length) {
        final byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return Base64.getEncoder()
                .encodeToString(bytes);
    }

    private static SimpleRequest.Builder baseRequest() {
        return SimpleRequest.Builder.post("/benchmark")
                .withContentType("application/json");
    }

    private static Request stringBodyRequest(final String body) {
        return baseRequest()
                .withBody(body, UTF_8)
                .build();
    }

    private static Request bytesBodyRequest(final byte[] body) {
        return baseRequest()
                .withBody(body)
                .build();
    }

    private static Request streamBodyRequest(final byte[] body) {
        return baseRequest()
                .withBody(new ByteArrayInputStream(body))
                .build();
    }

    @Setup(Level.Trial)
    public void createRequestsForBenchmark() {
        openApiInteractionValidator = OpenApiInteractionValidator
                .createFor("/oai/v3/api-benchmark.yaml")
                .build();
    }

    @Setup(Level.Invocation)
    public void prepareRequestsForNextValidation() {
        requestWithSmallStringBody = stringBodyRequest(SMALL_STRING_BODY);
        requestWithMediumStringBody = stringBodyRequest(MEDIUM_STRING_BODY);
        requestWithLargeStringBody = stringBodyRequest(LARGE_STRING_BODY);
        requestWithSmallBytesBody = bytesBodyRequest(SMALL_BYTES_BODY);
        requestWithMediumBytesBody = bytesBodyRequest(MEDIUM_BYTES_BODY);
        requestWithLargeBytesBody = bytesBodyRequest(LARGE_BYTES_BODY);
        requestWithSmallStreamBody = streamBodyRequest(SMALL_BYTES_BODY);
        requestWithMediumStreamBody = streamBodyRequest(MEDIUM_BYTES_BODY);
        requestWithLargeStreamBody = streamBodyRequest(LARGE_BYTES_BODY);
    }

    @Benchmark
    public void requestValidation_withSmallStringBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithSmallStringBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withMediumStringBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithMediumStringBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withLargeStringBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithLargeStringBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withSmallBytesBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithSmallBytesBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withMediumBytesBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithMediumBytesBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withLargeBytesBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithLargeBytesBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withSmallStreamBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithSmallStreamBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withMediumStreamBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithMediumStreamBody);
        assertThat(result.hasErrors(), is(false));
    }

    @Benchmark
    public void requestValidation_withLargeStreamBody() {
        final ValidationReport result = openApiInteractionValidator.validateRequest(requestWithLargeStreamBody);
        assertThat(result.hasErrors(), is(false));
    }
}
