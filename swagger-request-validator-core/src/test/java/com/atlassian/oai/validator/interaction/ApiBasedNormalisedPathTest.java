package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.interaction.ApiOperationResolver.ApiBasedNormalisedPath;
import org.junit.Test;

import java.util.Optional;

import static java.util.Optional.of;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApiBasedNormalisedPathTest {

    @Test
    public void no_params_orNormalization() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("/p1/p2/p3", null);

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
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/p3", null);

        assertThat(classUnderTest.numberOfParts(), is(3));
        assertThat(classUnderTest.normalised(), is("/p1/p2/p3"));
        assertThat(classUnderTest.original(), is("p1/p2/p3"));
    }

    @Test
    public void apiPrefix_isRemoved() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/p3", "p1");

        assertThat(classUnderTest.numberOfParts(), is(2));
        assertThat(classUnderTest.normalised(), is("/p2/p3"));
        assertThat(classUnderTest.original(), is("p1/p2/p3"));
    }

    @Test
    public void pathParamsIdentified_whenWholePathPart() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/{param1}/p3", null);

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
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/{param1}.json", null);

        assertThat(classUnderTest.hasParams(0), is(false));
        assertThat(classUnderTest.paramNames(0), is(empty()));

        assertThat(classUnderTest.hasParams(1), is(false));
        assertThat(classUnderTest.paramNames(1), is(empty()));

        assertThat(classUnderTest.hasParams(2), is(true));
        assertThat(classUnderTest.paramNames(2), contains("param1"));
    }

    @Test
    public void pathParamsIdentified_whenMultipleParamsInOnePart() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/{param1}-{param2}.json", null);

        assertThat(classUnderTest.hasParams(0), is(false));
        assertThat(classUnderTest.paramNames(0), is(empty()));

        assertThat(classUnderTest.hasParams(1), is(false));
        assertThat(classUnderTest.paramNames(1), is(empty()));

        assertThat(classUnderTest.hasParams(2), is(true));
        assertThat(classUnderTest.paramNames(2), contains("param1", "param2"));
    }

    @Test
    public void canExtractParamValues_fromParam_whenWholePathPart() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/{param1}", null);
        assertThat(classUnderTest.paramValues(2, "foop"), contains(of("foop")));
    }

    @Test
    public void canExtractParamValues_fromParam_whenPartPathPart_singleParam() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/{param1}.json", null);
        assertThat(classUnderTest.paramValues(2, "foop.json"), contains(of("foop")));
    }

    @Test
    public void canExtractParamValues_fromParam_whenPartPathPart_multipleParams() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/{param1}-{param2}:{param3}", null);
        assertThat(classUnderTest.paramValues(2, "foop-b:blarp"), contains(of("foop"), of("b"), of("blarp")));
    }

    @Test
    public void addsEmpty_whenMissingParamValues() {
        final ApiBasedNormalisedPath classUnderTest = new ApiBasedNormalisedPath("p1/p2/{param1}-{param2}", null);
        assertThat(classUnderTest.paramValues(2, "foop-"), contains(of("foop"), Optional.empty()));
    }
}