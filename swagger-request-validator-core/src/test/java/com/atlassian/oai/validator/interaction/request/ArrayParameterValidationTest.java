package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.v3.oas.models.media.IntegerSchema;
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

public class ArrayParameterValidationTest {

    private final ParameterValidator classUnderTest = new ParameterValidator(null, new MessageResolver());

    @Test
    public void validate_withValidCsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3", intArrayParam(SIMPLE)));
    }

    @Test
    public void validate_withValidCsvFormatAndStyleSpecified_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3", intArrayParam(null)));
    }

    @Test
    public void validate_withValidPipesFormat_shouldPass() {
        assertPass(classUnderTest.validate("1|2|3", intArrayParam(PIPEDELIMITED)));
    }

    @Test
    public void validate_withValidSsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1 2 3", intArrayParam(SPACEDELIMITED)));
    }

    @Test
    public void validate_withTrailingSeparator_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3,", intArrayParam(SIMPLE)));
    }

    @Test
    public void validate_withSingleValue_shouldPass() {
        assertPass(classUnderTest.validate("bob", stringArrayParam(SIMPLE)));
    }

    @Test
    public void validate_withInvalidParameter_shouldFail() {
        assertFail(classUnderTest.validate("1,2.1,3", intArrayParam(SIMPLE)),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withValue_shouldPass_whenUnsupportedParameter() {
        assertPass(classUnderTest.validate("value", stringParam()));
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", intArrayParam(true, SIMPLE, false)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate((String) null, intArrayParam(true, SIMPLE, false)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", intArrayParam(SIMPLE)));
    }

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate((String) null, intArrayParam(SIMPLE)));
    }

    @Test
    public void validate_withCollection_shouldFail_whenNotMultiFormat() {
        assertFail(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(SIMPLE)),
                "validation.request.parameter.collection.invalidFormat");
    }

    @Test
    public void validate_withCollection_shouldPass_whenMultiValuedFormat() {
        assertPass(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, FORM, true)));
    }

    @Test
    public void validate_withInvalidCollectionParameter_shouldFail() {
        assertFailWithoutContext(classUnderTest.validate(asList("1", "2.1", "3"), intArrayParam(true, FORM, true)),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withEmptyCollection_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(emptyList(), intArrayParam(true, FORM, true)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyCollection_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(emptyList(), intArrayParam(false, FORM, true)));
    }

    @Test
    public void validate_withNull_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate((Collection) null, intArrayParam(false, FORM, true)));
    }

    @Test
    public void validate_withTooFewValues_shouldFail_whenMinItemsSpecified() {
        assertFail(classUnderTest.validate("1,2",
                arrayParam(true, SIMPLE, false, 3, 5, null, new IntegerSchema())),
                "validation.request.parameter.collection.tooFewItems");
    }

    @Test
    public void validate_withTooManyValues_shouldFail_whenMaxItemsSpecified() {
        assertFail(classUnderTest.validate("1,2,3,4,5,6",
                arrayParam(true, SIMPLE, false, 3, 5, null, new IntegerSchema())),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void validate_withNonUniqueValues_shouldFail_whenUniqueSpecified() {
        assertFail(classUnderTest.validate("1,2,1",
                arrayParam(true, SIMPLE, false, null, null, true, new IntegerSchema())),
                "validation.request.parameter.collection.duplicateItems");
    }

    @Test
    public void validate_withNonUniqueValues_shouldPass_whenUniqueNotSpecified() {
        assertPass(classUnderTest.validate("1,2,1",
                arrayParam(true, SIMPLE, false, null, null, false, new IntegerSchema())));
    }

    @Test
    public void validate_withEnumValues_whouldPass_whenAllValuesMatchEnum() {
        assertPass(classUnderTest.validate("1,2,1", enumeratedArrayParam(true, SIMPLE, "1", "2", "3")));
    }

    @Test
    public void validate_withEnumValues_whouldFail_whenValueDoesntMatchEnum() {
        assertFail(classUnderTest.validate("1,2,1,4", enumeratedArrayParam(true, SIMPLE, "1", "2", "bob")),
                "validation.request.parameter.schema.enum");
    }
}
