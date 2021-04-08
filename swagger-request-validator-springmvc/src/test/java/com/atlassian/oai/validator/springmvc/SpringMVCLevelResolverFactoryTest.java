package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class SpringMVCLevelResolverFactoryTest {

    private static Map<String, ValidationReport.Level> getLevelsFromLevelResolver(final LevelResolver resolver) {
        final Field levelsField = ReflectionUtils.findField(LevelResolver.class, "levels");
        ReflectionUtils.makeAccessible(levelsField);
        return (Map<String, ValidationReport.Level>) ReflectionUtils.getField(levelsField, resolver);
    }

    @Test
    public void create_aDefaultLevelResolver() {
        // when:
        final LevelResolver resolver = SpringMVCLevelResolverFactory.create();

        // then:
        assertThat(getLevelsFromLevelResolver(resolver),
                Matchers.equalTo(ImmutableMap.of("validation.request.path.missing", ValidationReport.Level.INFO)));
    }
}
