package com.atlassian.oai.validator.util;

import com.google.common.collect.Multimap;
import org.junit.Test;

import java.util.Optional;

import static com.atlassian.oai.validator.util.HttpParsingUtils.extractMultipartBoundary;
import static com.atlassian.oai.validator.util.HttpParsingUtils.isMultipartContentTypeAcceptedByConsumer;
import static com.atlassian.oai.validator.util.HttpParsingUtils.parseMultipartFormDataBody;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadRawRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpParsingUtilsTest {

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_accepted_whenSame() {
        assertTrue(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------735323031399963166993862150",
                "multipart/form-data; boundary=---------------------------735323031399963166993862150"
        ));
    }

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_rejected_whenDifferent() {
        assertFalse(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------735323031399963166993862150",
                "multipart/form-data; boundary=---------------------------foobar"
        ));

        assertFalse(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------foobar",
                "multipart/form-data; boundary=---------------------------735323031399963166993862150"
        ));
    }

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_rejected_whenSameButNotMultipart() {
        assertFalse(isMultipartContentTypeAcceptedByConsumer(
                "text/plain",
                "text/plain"
        ));
    }

    @Test
    public void isMultipartContentTypeAcceptedByConsumer_accepted_whenWithBoundary() {
        assertTrue(isMultipartContentTypeAcceptedByConsumer(
                "multipart/form-data; boundary=---------------------------foobar",
                "multipart/form-data"
        ));
    }

    @Test
    public void extractMultipartBoundary_extractsBoundary() {
        assertEquals(extractMultipartBoundary("multipart/form-data; boundary=foobar").get(), "foobar");
    }

    @Test
    public void extractMultipartBoundary_returnsEmpty_ifNoBoundary() {
        assertEquals(extractMultipartBoundary("multipart/form-data"), Optional.empty());
    }

    @Test
    public void parseMultipartFormDataBody_successfullyParsesData() {
        final Multimap params = parseMultipartFormDataBody(
                "multipart/form-data; boundary=---------------------------012345678901234567890123456",
                loadRawRequest("multipart-request").replace("\r\n", "\n").replace("\n", "\r\n")
        );
        assertEquals(params.size(), 4);
        assertEquals(params.get("foo").iterator().next(), "foo value");
        assertEquals(params.get("bar").iterator().next(), "bar value");
        assertEquals(params.get("baz").iterator().next(), "baz value");
        assertEquals(params.get("html").iterator().next(), "<!DOCTYPE html><title></title>");
    }
}
