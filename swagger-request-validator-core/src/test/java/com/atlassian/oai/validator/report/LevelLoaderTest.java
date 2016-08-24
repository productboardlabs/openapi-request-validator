package com.atlassian.oai.validator.report;

import org.junit.After;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LevelLoaderTest {

    private static final String ERROR_KEY = "validation.test.error";
    private static final String WARN_KEY = "validation.test.warn";
    private static final String INFO_KEY = "validation.test.info";
    private static final String IGNORE_KEY = "validation.test.ignore";
    private static final String DEFAULT_LEVEL_KEY = "defaultLevel";
    private static final String SYSPROP_PREFIX = "swagger.";

    @After
    public void tearDown() {
        System.clearProperty(SYSPROP_PREFIX + ERROR_KEY);
        System.clearProperty(SYSPROP_PREFIX + WARN_KEY);
        System.clearProperty(SYSPROP_PREFIX + INFO_KEY);
        System.clearProperty(SYSPROP_PREFIX + IGNORE_KEY);
        System.clearProperty(SYSPROP_PREFIX + DEFAULT_LEVEL_KEY);
    }

    @Test
    public void classpathLoader_loadsLevels_whenFileExists() {
        final LevelLoader loader = LevelLoader.classpathLoader();
        assertLevelsLoadedCorrectly(loader);
        assertDefaultLevelLoadedCorrectly(loader);
    }

    @Test
    public void systemPropertiesLoader_loadsLevels_whenPropertiesSet() {
        System.setProperty(SYSPROP_PREFIX + ERROR_KEY, "ERROR");
        System.setProperty(SYSPROP_PREFIX + WARN_KEY, "WARN");
        System.setProperty(SYSPROP_PREFIX + INFO_KEY, "INFO");
        System.setProperty(SYSPROP_PREFIX + IGNORE_KEY, "IGNORE");
        System.setProperty(SYSPROP_PREFIX + DEFAULT_LEVEL_KEY, "ERROR");

        final LevelLoader loader = LevelLoader.systemPropertyLoader();
        assertLevelsLoadedCorrectly(loader);
        assertDefaultLevelLoadedCorrectly(loader);
    }

    @Test
    public void systemPropertiesLoader_doesNotLoadLevels_whenNoPropertiesSet() {
        final LevelLoader loader = LevelLoader.systemPropertyLoader();
        assertThat(loader.loadLevels().size(), is(0));
        assertThat(loader.defaultLevel(), is(Optional.empty()));
    }

    @Test
    public void propertiesFileLoader_loadsLevels_whenFileExists() throws Exception {
        final Path tempFile = Files.createTempFile("swagger-validator-test", ".properties");
        Files.copy(getClass().getResourceAsStream("/swagger-validator.properties"), tempFile, REPLACE_EXISTING);
        tempFile.toFile().deleteOnExit();

        final LevelLoader loader = new LevelLoader.PropertiesLoader(tempFile.toFile(), null);
        assertLevelsLoadedCorrectly(loader);
        assertDefaultLevelLoadedCorrectly(loader);
    }

    @Test
    public void propertiesFileLoader_doesNotLoadLevels_whenFileNotFound() {
        final LevelLoader loader =
                new LevelLoader.PropertiesLoader(System.getProperty("user.dir") + "/nonexistentfile.properties", null);
        assertThat(loader.loadLevels().size(), is(0));
        assertThat(loader.defaultLevel(), is(Optional.empty()));
    }

    private void assertLevelsLoadedCorrectly(final LevelLoader loader) {
        final Map<String, ValidationReport.Level> levels = loader.loadLevels();
        assertThat(levels.get(ERROR_KEY), is(ValidationReport.Level.ERROR));
        assertThat(levels.get(WARN_KEY), is(ValidationReport.Level.WARN));
        assertThat(levels.get(INFO_KEY), is(ValidationReport.Level.INFO));
        assertThat(levels.get(IGNORE_KEY), is(ValidationReport.Level.IGNORE));
        assertThat(levels.size(), is(4));
    }

    private void assertDefaultLevelLoadedCorrectly(final LevelLoader loader) {
        assertThat(loader.defaultLevel(), is(Optional.of(ValidationReport.Level.ERROR)));
    }
}
