package com.atlassian.oai.validator.report;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LevelResolverTest {

    private LevelResolver classUnderTest;

    @Before
    public void setup() {
        final Map<String, ValidationReport.Level> levels = new HashMap<>();
        levels.put("a.b.c.d", ValidationReport.Level.ERROR);
        levels.put("a.b.c", ValidationReport.Level.WARN);
        levels.put("aa.bb", ValidationReport.Level.INFO);

        classUnderTest = LevelResolver.create()
                .withLoader(null)
                .withLevels(levels)
                .withDefaultLevel(ValidationReport.Level.IGNORE)
                .build();
    }

    @Test
    public void getLevel_withExactMatch_shouldReturnLevelForKey() {
        assertThat(classUnderTest.getLevel("a.b.c"), is(ValidationReport.Level.WARN));
    }

    @Test
    public void getLevel_withChildKey_shouldReturnLevelForClosestParentKey() {
        assertThat(classUnderTest.getLevel("a.b.c.d.e"), is(ValidationReport.Level.ERROR));
    }

    @Test
    public void getLevel_withUnknownKey_shouldReturnDefaultLevel() {
        assertThat(classUnderTest.getLevel("a.b"), is(ValidationReport.Level.IGNORE));
    }

    @Test
    public void getLevel_withNoMappings_shouldReturnDefaultLevel() {
        final LevelResolver resolver = LevelResolver.create()
                .withLoader(null)
                .withDefaultLevel(ValidationReport.Level.IGNORE)
                .build();
        assertThat(resolver.getLevel("a.b.c"), is(ValidationReport.Level.IGNORE));
    }

    @Test
    public void getLevel_returnsError_ifNoOtherConfiguration() {
        final LevelResolver resolver = LevelResolver.create().withLoader(null).build();
        assertThat(resolver.getLevel("a.b.c"), is(ValidationReport.Level.ERROR));
    }

    @Test
    public void builder_appliesLoadedLevels_afterProgrammaticLevels() {
        final LevelResolver resolver = LevelResolver.create()
                // The swagger-validator.properties file will be loaded and override any programmatic levels
                .withLoader(LevelLoader.classpathLoader())
                .withLevel("validation.test.warn", ValidationReport.Level.IGNORE)
                .withDefaultLevel(ValidationReport.Level.IGNORE)
                .build();

        assertThat(resolver.getLevel("validation.test.warn"), is(ValidationReport.Level.WARN));
        assertThat(resolver.getLevel("foo"), is(ValidationReport.Level.ERROR));
    }

}
