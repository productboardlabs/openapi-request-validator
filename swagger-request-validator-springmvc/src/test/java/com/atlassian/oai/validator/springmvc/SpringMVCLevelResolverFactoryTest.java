package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;

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
        Assert.assertThat(getLevelsFromLevelResolver(resolver),
                Matchers.equalTo(ImmutableMap.of("validation.request.path.missing", ValidationReport.Level.INFO)));
    }
}
