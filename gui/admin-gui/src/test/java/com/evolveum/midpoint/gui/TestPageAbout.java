/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.*;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.util.tester.TagTester;
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
    private static final Set<String> INVALID_GIT_VALUES = Set.of("unknown", "null");

    @Test
    public void test001AboutPageShowsGeneratedGitMetadata() throws Exception {
        Properties systemProperties = loadMidpointSystemProperties();

        String branch = getValidGitBranch(systemProperties);
        String build = getValidGitDescribe(systemProperties);

        renderPage(PageAbout.class);

        tester.assertLabel("branch", branch);
        tester.assertLabel("build", build);
    }

    @Test
    public void test002ContentSecurityPolicyNonceConfiguration() {
        renderPage(PageAbout.class);

        String csp = tester.getLastResponse().getHeader("Content-Security-Policy");
        assertNotNull(csp);
        String scriptSrc = csp.split("script-src ")[1].split(";")[0];
        assertTrue(scriptSrc.contains("'self'"), csp);
        Matcher nonceMatcher = Pattern.compile("'nonce-([^']+)'").matcher(scriptSrc);
        assertTrue(nonceMatcher.find(), csp);
        String nonce = nonceMatcher.group(1);
        assertFalse(scriptSrc.contains("'unsafe-inline'"), csp);
        assertFalse(csp.contains("'unsafe-eval'"), csp);
        assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"), csp);

        List<TagTester> inlineScripts = TagTester.createTags(
                        tester.getLastResponseAsString(),
                        tag -> "script".equalsIgnoreCase(tag.getName()),
                        false).stream()
                .filter(tag -> !tag.hasAttribute("src"))
                .filter(tag -> {
                    String type = tag.getAttribute("type");
                    return type == null
                            || type.isBlank()
                            || "text/javascript".equalsIgnoreCase(type)
                            || "application/javascript".equalsIgnoreCase(type)
                            || "module".equalsIgnoreCase(type);
                })
                .toList();
        assertFalse(inlineScripts.isEmpty(), "Expected at least one inline executable script");
        inlineScripts.forEach(script -> assertEquals(script.getAttribute("nonce"), nonce));
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
