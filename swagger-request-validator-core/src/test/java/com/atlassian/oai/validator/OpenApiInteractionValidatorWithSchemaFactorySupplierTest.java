package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.schema.SwaggerV20Library;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.AbstractSyntaxChecker;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ListReportProvider;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.keyword.digest.helpers.IdentityDigester;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.keyword.validator.KeywordValidator;
import com.github.fge.jsonschema.keyword.validator.KeywordValidatorFactory;
import com.github.fge.jsonschema.library.Keyword;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.loadJsonRequest;
import static com.github.fge.msgsimple.load.MessageBundles.getBundle;

public class OpenApiInteractionValidatorWithSchemaFactorySupplierTest {
    private static final String FOO_KEYWORD = "x-isFoo";

    static class FooSyntaxChecker extends AbstractSyntaxChecker {
        public FooSyntaxChecker() {
            super(FOO_KEYWORD, NodeType.BOOLEAN);
        }

        @Override
        protected void checkValue(@Nonnull final Collection<JsonPointer> collection,
                                  @Nonnull final MessageBundle messageBundle,
                                  @Nonnull final ProcessingReport processingReport,
                                  @Nonnull final SchemaTree schemaTree) throws ProcessingException {
            final JsonNode keywordField = schemaTree.getNode().get(FOO_KEYWORD);

            if (!keywordField.isBoolean()) {
                processingReport.fatal(buildMessage(FOO_KEYWORD + " must be a keyword"));
                return;
            }

            if (!keywordField.booleanValue()) {
                return;
            }

            // The "foo" field must be required.
            final JsonNode requiredNode = schemaTree.getNode().get("required");
            final String errorMessage = "The \"required\" field must be present and contain \"foo\" for isFoo schema objects.";

            if (requiredNode == null
                    || !requiredNode.isArray()
                    || StreamSupport
                    .stream(requiredNode.spliterator(), true)
                    .noneMatch(j -> j.isTextual() && j.textValue().equals("foo"))) {
                processingReport.fatal(buildMessage(errorMessage));
            }
        }

        private static ProcessingMessage buildMessage(final String message) {
            return new ProcessingMessage().setMessage(message).put("com.foo.schema_invalid", true);
        }
    }

    static class FooKeywordValidator extends AbstractKeywordValidator {
        private final Boolean isFoo;

        public FooKeywordValidator(@Nonnull final JsonNode digest) {
            super(FOO_KEYWORD);
            this.isFoo = digest.get(FOO_KEYWORD).asBoolean();
        }

        @Override
        public String toString() {
            return FOO_KEYWORD + ": " + this.isFoo;
        }

        @Override
        public void validate(@Nonnull final Processor<FullData, FullData> processor,
                             @Nonnull final ProcessingReport processingReport,
                             @Nonnull final MessageBundle messageBundle,
                             @Nonnull final FullData fullData) throws ProcessingException {
            if (null == this.isFoo) {
                return;
            }

            final List<String> validFooValues = Arrays.asList("foo", "bar", "baz");
            final JsonNode fooObj = fullData.getInstance().getNode();

            if (!validFooValues.contains(fooObj.get("foo").textValue())) {
                processingReport.error(
                        new ProcessingMessage()
                            .put("domain", "validation")
                            .put("keyword", JsonNodeFactory.instance.textNode(FOO_KEYWORD))
                            .put("invalid_foo", true));
            }
        }
    }

    static class FooKeywordValidatorFactory implements KeywordValidatorFactory {
        @Override
        public KeywordValidator getKeywordValidator(@Nonnull final JsonNode jsonNode) {
            return new FooKeywordValidator(jsonNode);
        }
    }

    private final Supplier<JsonSchemaFactory> schemaFactorySupplier = () -> {
        final Keyword fooKeyword =
                Keyword.newBuilder(FOO_KEYWORD)
                        .withSyntaxChecker(new FooSyntaxChecker())
                        .withDigester(new IdentityDigester(FOO_KEYWORD, NodeType.OBJECT))
                        .withValidatorFactory(new FooKeywordValidatorFactory())
                        .freeze();

        final String schemaUri = "https://foo.com/schema0";
        final Library library = SwaggerV20Library.get()
                .thaw()
                .addKeyword(fooKeyword)
                .freeze();

        return JsonSchemaFactory
                .newBuilder()
                .setValidationConfiguration(
                        ValidationConfiguration
                                .byDefault()
                                .thaw()
                                .setDefaultLibrary(schemaUri, library)
                                .setSyntaxMessages(getBundle(SwaggerV20Library.SyntaxBundle.class))
                                .setValidationMessages(getBundle(SwaggerV20Library.ValidationBundle.class))
                                .freeze())
                .setReportProvider(
                        // Only emit ERROR and above from the JSON schema validation
                        new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
                .freeze();
    };

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                    .createForSpecificationUrl("/oai/v3/api-foo.yaml")
                    .withSchemaFactorySupplier(schemaFactorySupplier)
                    .build();

    @Test
    public void validate_withValidRequest_shouldSucceed() {
        final Request request = SimpleRequest.Builder
                .post("/foo")
                .withContentType("application/json")
                .withBody(loadJsonRequest("newfoo-valid"))
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withInvalidRequest_shouldFail() {
        final Request request = SimpleRequest.Builder
                .post("/foo")
                .withContentType("application/json")
                .withBody(loadJsonRequest("newfoo-invalid-unrecognizedvalue"))
                .build();

        assertFail(classUnderTest.validateRequest(request));
    }
}
