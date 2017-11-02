package com.atlassian.oai.validator.model;

import org.junit.Test;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApiPathImplTest {

    @Test
    public void no_params_orNormalization() {
        final ApiPathImpl classUnderTest = new ApiPathImpl("/p1/p2/p3", null);

        assertThat(classUnderTest.numberOfParts(), is(3));
        assertThat(classUnderTest.normalised(), is("/p1/p2/p3"));
        assertThat(classUnderTest.original(), is("/p1/p2/p3"));

        assertThat(classUnderTest.hasParams(0), is(false));
        assertThat(classUnderTest.paramNames(0), is(empty()));

        assertThat(classUnderTest.hasParams(1), is(false));
        assertThat(classUnderTest.paramNames(1), is(empty()));

        assertThat(classUnderTest.hasParams(2), is(false));
        assertThat(classUnderTest.paramNames(2), is(empty()));
    }

    @Test
    public void normalization_addsLeadingSlash() {
        final ApiPathImpl classUnderTest = new ApiPathImpl("p1/p2/p3", null);

        assertThat(classUnderTest.numberOfParts(), is(3));
        assertThat(classUnderTest.normalised(), is("/p1/p2/p3"));
        assertThat(classUnderTest.original(), is("p1/p2/p3"));
    }

    @Test
    public void apiPrefix_isRemoved() {
        final ApiPathImpl classUnderTest = new ApiPathImpl("p1/p2/p3", "p1");

        assertThat(classUnderTest.numberOfParts(), is(2));
        assertThat(classUnderTest.normalised(), is("/p2/p3"));
        assertThat(classUnderTest.original(), is("p1/p2/p3"));
    }

    @Test
    public void pathParamsIdentified_whenWholePathPart() {
        final ApiPathImpl classUnderTest = new ApiPathImpl("p1/{param1}/p3", null);

        assertThat(classUnderTest.numberOfParts(), is(3));
        assertThat(classUnderTest.normalised(), is("/p1/{param1}/p3"));
        assertThat(classUnderTest.original(), is("p1/{param1}/p3"));

        assertThat(classUnderTest.hasParams(0), is(false));
        assertThat(classUnderTest.paramNames(0), is(empty()));

        assertThat(classUnderTest.hasParams(1), is(true));
        assertThat(classUnderTest.paramNames(1), contains("param1"));

        assertThat(classUnderTest.hasParams(2), is(false));
        assertThat(classUnderTest.paramNames(2), is(empty()));
    }

    @Test
    public void pathParamsIdentified_whenPartPathPart() {
        final ApiPathImpl classUnderTest = new ApiPathImpl("p1/p2/{param1}.json", null);

        assertThat(classUnderTest.hasParams(0), is(false));
        assertThat(classUnderTest.paramNames(0), is(empty()));

        assertThat(classUnderTest.hasParams(1), is(false));
        assertThat(classUnderTest.paramNames(1), is(empty()));

        assertThat(classUnderTest.hasParams(2), is(true));
        assertThat(classUnderTest.paramNames(2), contains("param1"));
    }

    @Test
    public void pathParamsIdentified_whenMultipleParamsInOnePart() {
        final ApiPathImpl classUnderTest = new ApiPathImpl("p1/p2/{param1}-{param2}.json", null);

        assertThat(classUnderTest.hasParams(0), is(false));
        assertThat(classUnderTest.paramNames(0), is(empty()));

        assertThat(classUnderTest.hasParams(1), is(false));
        assertThat(classUnderTest.paramNames(1), is(empty()));

        assertThat(classUnderTest.hasParams(2), is(true));
        assertThat(classUnderTest.paramNames(2), contains("param1", "param2"));
    }

    @Test
    public void paramValues_canExtractParamValues_fromParam_whenWholePathPart() {
        final String[] expected = {"foop"};
        testParamValueExtraction("{param1}", "foop", expected);
    }

    @Test
    public void paramValues_canExtractParamValues_fromParam_whenPartPathPart_singleParam() {
        final String[] expected = {"foop"};
        testParamValueExtraction("{param1}.json", "foop.json", expected);
    }

    @Test
    public void paramValues_canExtractParamValues_fromParam_whenPartPathPart_multipleParams() {
        final String[] expected = {"foop", "b", "blarp"};
        testParamValueExtraction("{param1}-{param2}:{param3}", "foop-b:blarp", expected);
    }

    @Test
    public void paramValues_addsEmpty_whenMissingParamValues() {
        final String[] expected = {"foop", null};
        testParamValueExtraction("{param1}-{param2}", "foop-", expected);
    }

    @Test
    public void paramValues_handlesBadTemplate() {
        final String[] expected = {"foop"};
        testParamValueExtraction("{param1}-{param2", "foop-blarp", expected);
    }

    private void testParamValueExtraction(final String expression, final String path, final String... expected) {
        assertThat(new ApiPathImpl(expression, null).paramValues(0, path).values(),
                containsInAnyOrder(stream(expected).map(e -> is(ofNullable(e))).collect(toList())));
    }
}