package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.v3.oas.models.media.IntegerSchema;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;

import static com.atlassian.oai.validator.util.ParameterGenerator.arrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.enumeratedArrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.intArrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringArrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFailWithoutContext;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.FORM;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.PIPEDELIMITED;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SIMPLE;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SPACEDELIMITED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayParameterValidatorTest {

    private final ArrayParameterValidator classUnderTest = new ArrayParameterValidator(null, new MessageResolver());

    @Test
    public void validate_withValidCsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3", intArrayParam(true, SIMPLE)));
    }

    @Test
    public void validate_withValidCsvFormatAndNoCollectionFormat_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3", intArrayParam(true, null)));
    }

    @Test
    public void validate_withValidPipesFormat_shouldPass() {
        assertPass(classUnderTest.validate("1|2|3", intArrayParam(true, PIPEDELIMITED)));
    }

    @Test
    public void validate_withValidSsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1 2 3", intArrayParam(true, SPACEDELIMITED)));
    }

    @Test
    public void validate_withTrailingSeparator_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3,", intArrayParam(true, SIMPLE)));
    }

    @Test
    public void validate_withSingleValue_shouldPass() {
        assertPass(classUnderTest.validate("bob", stringArrayParam(true, SIMPLE)));
    }

    @Test
    public void validate_withInvalidParameter_shouldFail() {
        assertFail(classUnderTest.validate("1,2.1,3", intArrayParam(true, SIMPLE)),
                "validation.schema.type");
    }

    @Test
    public void validate_withValue_shouldPass_whenUnsupportedParameter() {
        assertPass(classUnderTest.validate("value", stringParam()));
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", intArrayParam(true, SIMPLE)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate((String) null, intArrayParam(true, SIMPLE)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", intArrayParam(false, SIMPLE)));
    }

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate((String) null, intArrayParam(false, SIMPLE)));
    }

    @Test
    public void validate_withCollection_shouldFail_whenNotMultiFormat() {
        assertFail(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, SIMPLE)),
                "validation.request.parameter.collection.invalidFormat");
    }

    @Test
    @Ignore("Need to fix 'explode' style validation")
    public void validate_withCollection_shouldPass_whenMultiFormat() {
        // TODO Need to fix 'explode' style validation
        assertPass(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, FORM)));
    }

    @Test
    @Ignore("Need to fix 'explode' style validation")
    public void validate_withInvalidCollectionParameter_shouldFail() {
        // TODO Need to fix 'explode' style validation
        assertFailWithoutContext(classUnderTest.validate(asList("1", "2.1", "3"), intArrayParam(true, FORM)),
                "validation.schema.type");
    }

    @Test
    public void validate_withCollection_shouldPass_whenParameterMissing() {
        assertPass(classUnderTest.validate(asList("value"), null));
    }

    @Test
    public void validate_withEmptyCollection_shouldFail_whenRequired() {
        // TODO Need to fix 'explode' style validation
        assertFail(classUnderTest.validate(emptyList(), intArrayParam(true, FORM)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyCollection_shouldPass_whenNotRequired() {
        // TODO Need to fix 'explode' style validation
        assertPass(classUnderTest.validate(emptyList(), intArrayParam(false, FORM)));
    }

    @Test
    public void validate_withNull_shouldPass_whenNotRequired() {
        // TODO Need to fix 'explode' style validation
        assertPass(classUnderTest.validate((Collection) null, intArrayParam(false, FORM)));
    }

    @Test
    public void validate_withTooFewValues_shouldFail_whenMinItemsSpecified() {
        assertFail(classUnderTest.validate("1,2", arrayParam(true, SIMPLE, 3, 5, null, new IntegerSchema())),
                "validation.request.parameter.collection.tooFewItems");
    }

    @Test
    public void validate_withTooManyValues_shouldFail_whenMaxItemsSpecified() {
        assertFail(classUnderTest.validate("1,2,3,4,5,6", arrayParam(true, SIMPLE, 3, 5, null, new IntegerSchema())),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void validate_withNonUniqueValues_shouldFail_whenUniqueSpecified() {
        assertFail(classUnderTest.validate("1,2,1", arrayParam(true, SIMPLE, null, null, true, new IntegerSchema())),
                "validation.request.parameter.collection.duplicateItems");
    }

    @Test
    public void validate_withNonUniqueValues_shouldPass_whenUniqueNotSpecified() {
        assertPass(classUnderTest.validate("1,2,1", arrayParam(true, SIMPLE, null, null, false, new IntegerSchema())));
    }

    @Test
    public void validate_withEnumValues_whouldPass_whenAllValuesMatchEnum() {
        assertPass(classUnderTest.validate("1,2,1", enumeratedArrayParam(true, SIMPLE, "1", "2", "3")));
    }

    @Test
    public void validate_withEnumValues_whouldFail_whenValueDoesntMatchEnum() {
        assertFail(classUnderTest.validate("1,2,1,4", enumeratedArrayParam(true, SIMPLE, "1", "2", "bob")),
                "validation.request.parameter.enum.invalid");
    }
}
