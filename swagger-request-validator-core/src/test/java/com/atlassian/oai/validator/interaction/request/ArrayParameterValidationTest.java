package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.atlassian.oai.validator.util.ParameterGenerator.arrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.enumeratedArrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.intArrayParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringArrayParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.FORM;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.PIPEDELIMITED;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SIMPLE;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SPACEDELIMITED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayParameterValidationTest {

    @Nested
    class ArrayAsStringTests {
        private final ParameterValidator classUnderTest = new ParameterValidator(
                new SchemaValidator(new OpenAPI(), new MessageResolver()), new MessageResolver());

        static Stream<StringTestCase> params() {
            // CHECKSTYLE:OFF indentation
            return Stream.of(
                    new StringTestCase("valid CSV format will pass",
                            "1,2,3", intArrayParam(SIMPLE),
                            assertPass()),
                    new StringTestCase("valid CSV format and no style specified will pass",
                            "1,2,3", intArrayParam(null),
                            assertPass()),
                    new StringTestCase("valid pipe format will pass",
                            "1|2|3", intArrayParam(PIPEDELIMITED),
                            assertPass()),
                    new StringTestCase("valid SSV format will pass",
                            "1 2 3", intArrayParam(SPACEDELIMITED),
                            assertPass()),
                    new StringTestCase("trailing separator will pass",
                            "1,2,3,", intArrayParam(SIMPLE),
                            assertPass()),
                    new StringTestCase("valid single value will pass",
                            "bob", stringArrayParam(SIMPLE),
                            assertPass()),
                    new StringTestCase("invalid array value will fail",
                            "1,2.1,3", intArrayParam(SIMPLE),
                            assertFail("validation.request.parameter.schema.type")),
                    new StringTestCase("empty array will pass when required",
                            "", intArrayParam(true, SIMPLE, false),
                            assertPass()),
                    new StringTestCase("empty array will fail when min length > 0",
                            "", intArrayParamWithMinItems(1),
                            assertFail("validation.request.parameter.collection.tooFewItems")),
                    new StringTestCase("null array will fail when required",
                            null, intArrayParam(true, SIMPLE, false),
                            assertFail("validation.request.parameter.missing")),
                    new StringTestCase("empty array will pass when not required",
                            "", intArrayParam(false, SIMPLE, false),
                            assertPass()),
                    new StringTestCase("null array will pass when not required",
                            null, intArrayParam(false, SIMPLE, false),
                            assertPass()),
                    new StringTestCase("array with too few values will fail",
                            "1,2,", intArrayParamWithMinItems(3),
                            assertFail("validation.request.parameter.collection.tooFewItems")),
                    new StringTestCase("array with too many values will fail",
                            "1,2,3,4,", intArrayParamWithMaxItems(3),
                            assertFail("validation.request.parameter.collection.tooManyItems")),
                    new StringTestCase("array with object schema with too many values will fail",
                            "{\"index\": 1},{\"index\": 2},{\"index\": 3}", objectArrayParamWithMaxItems(2),
                            assertFail("validation.request.parameter.collection.tooManyItems")),
                    new StringTestCase("array with non-unique items will fail when unique specified",
                            "1,2,1", intArrayParamWithUniqueItems(true),
                            assertFail("validation.request.parameter.collection.duplicateItems")),
                    new StringTestCase("array with non-unique items will pass when unique not specified",
                            "1,2,1", intArrayParamWithUniqueItems(false), assertPass()),
                    new StringTestCase("array with object schema with non-unique items will fail when unique specified",
                            "{\"index\": 1},{\"index\": 2},{\"index\": 1}", objectArrayParamWithUniqueItems(true),
                            assertFail("validation.request.parameter.collection.duplicateItems")),
                    new StringTestCase("enum array will pass when all values are valid",
                            "a,b,b,c", enumArrayParamWithAllowedItems("a", "b", "c"),
                            assertPass()),
                    new StringTestCase("enum array will fail when values are invalid", "a,b,bob,c", enumArrayParamWithAllowedItems("a", "b", "c"),
                            assertFail("validation.request.parameter.schema.enum"))
            );
            // CHECKSTYLE:ON indentation
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("params")
        void test(final StringTestCase testCase) {
            testCase.assertion().accept(classUnderTest.validate(testCase.value(), testCase.param()));
        }
    }

    @Nested
    class ArrayAsCollectionTests {
        private final ParameterValidator classUnderTest = new ParameterValidator(
                new SchemaValidator(new OpenAPI(), new MessageResolver()), new MessageResolver());

        static Stream<CollectionTestCase> params() {
            // CHECKSTYLE:OFF indentation
            return Stream.of(
                    new CollectionTestCase("should fail when not multi-value format",
                            asList("1", "2", "3"), intArrayParam(SIMPLE),
                            assertFail("validation.request.parameter.collection.invalidFormat")),
                    new CollectionTestCase("should pass when multi-value format",
                            asList("1", "2", "3"), intArrayParam(true, FORM, true),
                            assertPass()),
                    new CollectionTestCase("array with invalid value should fail",
                            asList("1", "2.1", "3"), intArrayParam(true, FORM, true),
                            assertFail("validation.request.parameter.schema.type")),
                    new CollectionTestCase("null array should fail when required",
                            null, intArrayParam(true, FORM, true),
                            assertFail("validation.request.parameter.missing")),
                    new CollectionTestCase("null array should pass when not required",
                            null, intArrayParam(false, FORM, true),
                            assertPass()),
                    new CollectionTestCase("empty array should pass when required and min items not specified",
                            emptyList(), intArrayParam(true, FORM, true),
                            assertPass()),
                    new CollectionTestCase("empty array should fail when min items > 0",
                            emptyList(), arrayParam(true, FORM, true, 1, null, null, new IntegerSchema()),
                            assertFail("validation.request.parameter.collection.tooFewItems"))
            );
            // CHECKSTYLE:ON indentation
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("params")
        void test(final CollectionTestCase testCase) {
            testCase.assertion().accept(classUnderTest.validate(testCase.value(), testCase.param()));
        }
    }

    record StringTestCase(String name, String value, Parameter param, Consumer<ValidationReport> assertion) {
        @Override
        public String toString() {
            return name;
        }
    }

    record CollectionTestCase(String name, Collection<String> value, Parameter param, Consumer<ValidationReport> assertion) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static Parameter intArrayParamWithMinItems(final int min) {
        return arrayParam(true, SIMPLE, false, min, null, null, new IntegerSchema());
    }

    private static Parameter intArrayParamWithMaxItems(final int max) {
        return arrayParam(true, SIMPLE, false, null, max, null, new IntegerSchema());
    }

    private static Parameter intArrayParamWithUniqueItems(final boolean unique) {
        return arrayParam(true, SIMPLE, false, null, null, unique, new IntegerSchema());
    }

    private static Parameter objectArrayParamWithMaxItems(final int max) {
        final Schema items = new ObjectSchema().addProperties("index", new IntegerSchema());
        return arrayParam(true, SIMPLE, false, null, max, null, items);
    }

    private static Parameter objectArrayParamWithUniqueItems(final boolean unique) {
        final Schema items = new ObjectSchema().addProperties("index", new IntegerSchema());
        return arrayParam(true, SIMPLE, false, null, null, unique, items);
    }

    private static Parameter enumArrayParamWithAllowedItems(final String... allowedItems) {
        return enumeratedArrayParam(true, SIMPLE, allowedItems);
    }

}
