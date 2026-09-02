/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.*;

import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.configuration.PageAbout;

/**
 * Tests that the About page displays valid Git metadata generated during the build.
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageAbout extends AbstractInitializedGuiIntegrationTest {

    private static final String SYSTEM_PROPERTIES_RESOURCE = "/midpoint-system.properties";
    private static final String BUILD_PROPERTY = "midpoint.system.build";
    private static final String BRANCH_PROPERTY = "midpoint.system.branch";
    private static final String UNRESOLVED_DESCRIBE_PLACEHOLDER = "${git.describe}";
    private static final String UNRESOLVED_BRANCH_PLACEHOLDER = "${git.branch}";
    private static final String GIT_DESCRIBE_PATTERN = "v\\d+\\.\\d+.*";
    private static final String RAW_GIT_HASH = "[0-9a-fA-F]{7,64}";
    private static final Set<String> INVALID_GIT_VALUES = Set.of(
            "unknown", "null");

    @Test
    public void test001AboutPageShowsGeneratedGitMetadata() throws Exception {
        Properties systemProperties = loadMidpointSystemProperties();

        String branch = getValidGitBranch(systemProperties);
        String build = getValidGitDescribe(systemProperties);

        renderPage(PageAbout.class);

        tester.assertLabel("branch", branch);
        tester.assertLabel("build", build);
    }

    private Properties loadMidpointSystemProperties() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(SYSTEM_PROPERTIES_RESOURCE)) {
            assertNotNull(stream, SYSTEM_PROPERTIES_RESOURCE + " must be available on the classpath");

            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        }
    }

    private String getValidGitBranch(Properties properties) {
        String branch = getRequiredGitProperty(properties, BRANCH_PROPERTY, UNRESOLVED_BRANCH_PLACEHOLDER);
        assertFalse(branch.matches(RAW_GIT_HASH),
                BRANCH_PROPERTY + " must contain a branch name, not a raw Git hash: " + branch);

        return branch;
    }

    private String getValidGitDescribe(Properties properties) {
        String build = getRequiredGitProperty(properties, BUILD_PROPERTY, UNRESOLVED_DESCRIBE_PLACEHOLDER);
        assertTrue(build.matches(GIT_DESCRIBE_PATTERN),
                BUILD_PROPERTY + " must contain a valid git describe value: " + build);

        return build;
    }

    private String getRequiredGitProperty(Properties properties, String key, String unresolvedPlaceholder) {
        String value = properties.getProperty(key);
        assertNotNull(value, key + " must be present");

        String trimmed = value.trim();
        assertFalse(trimmed.isEmpty(), key + " must not be empty");
        assertFalse(trimmed.contains(unresolvedPlaceholder),
                key + " contains unresolved placeholder: " + trimmed);
        assertFalse(
                INVALID_GIT_VALUES.stream()
                        .anyMatch(invalid -> invalid.equalsIgnoreCase(trimmed)),
                key + " contains invalid value: " + trimmed);

        return trimmed;
    }
}
