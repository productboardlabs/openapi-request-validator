package com.atlassian.oai.validator.schema;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.Before;
import org.junit.Test;

public class SchemaValidatorV31Test {

    private final String api = "/oai/v31/api-with-path-ref.yaml";

    private final SchemaValidator classUnderTest = validator(api);

    private OpenAPI parserUnderTest;

    @Before
    public void setup(){
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parserUnderTest =  new OpenAPIParser().readLocation(api, null, parseOptions).getOpenAPI();
    }

    @Test
    public void validate_pathParameter_withRef_shouldPass() {
        final String value = "admin";
        final Schema schema = parserUnderTest.getPaths().get("/reports/{reportType}").getGet().getParameters().get(0).getSchema();
        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    private SchemaValidator validator(final String api) {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        return new SchemaValidator(new OpenAPIParser().readLocation(api, null, parseOptions).getOpenAPI(), new MessageResolver());
    }
}
