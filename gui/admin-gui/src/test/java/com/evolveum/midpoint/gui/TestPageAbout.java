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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
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
    private static final Pattern CSP_NONCE_PATTERN = Pattern.compile("'nonce-([^']+)'");
    private static final String TEST_CSS_ID = "csp-nonce-test-css";

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
    public void test002ContentSecurityPolicyConfiguration() {
        renderPage(PageAbout.class);

        String csp = getContentSecurityPolicyHeader();

        String scriptSrc = getCspDirective(csp, "script-src");
        assertTrue(scriptSrc.contains("'self'"), "script-src must allow 'self': " + csp);
        String scriptNonce = getNonce(scriptSrc);
        assertFalse(scriptSrc.contains("'unsafe-inline'"), "script-src must not allow 'unsafe-inline': " + csp);
        assertFalse(scriptSrc.contains("'unsafe-eval'"), "script-src must not allow 'unsafe-eval': " + csp);

        String styleSrc = getCspDirective(csp, "style-src");
        assertTrue(styleSrc.contains("'self'"), "style-src must allow 'self': " + csp);
        assertTrue(styleSrc.contains("'unsafe-inline'"), "style-src must retain the compatibility fallback: " + csp);

        String styleSrcElem = getCspDirective(csp, "style-src-elem");
        assertTrue(styleSrcElem.contains("'self'"), "style-src-elem must allow 'self': " + csp);
        assertFalse(styleSrcElem.contains("'unsafe-inline'"), "style-src-elem must not allow 'unsafe-inline': " + csp);
        String styleNonce = getNonce(styleSrcElem);
        assertEquals(styleNonce, scriptNonce, "The script and style CSP nonces must match");

        String styleSrcAttr = getCspDirective(csp, "style-src-attr");
        assertTrue(styleSrcAttr.contains("'unsafe-inline'"), "style-src-attr must allow 'unsafe-inline': " + csp);
    }

    @Test
    public void test003InlineScriptNonces() {
        renderPage(PageAbout.class);

        String expectedNonce = getNonce(getCspDirective(getContentSecurityPolicyHeader(), "script-src"));

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
        inlineScripts.forEach(script -> assertEquals(
                script.getAttribute("nonce"), expectedNonce,
                "Every executable inline script must use the nonce from script-src"));
    }

    private String getContentSecurityPolicyHeader() {
        String csp = tester.getLastResponse().getHeader("Content-Security-Policy");
        assertNotNull(csp, "Expected a Content-Security-Policy response header");
        return csp;
    }

    private String getCspDirective(String csp, String directiveName) {
        Matcher matcher = Pattern.compile("(?:^|;)\\s*" + Pattern.quote(directiveName) + "\\s+([^;]+)").matcher(csp);
        assertTrue(matcher.find(), "Expected " + directiveName + " directive in: " + csp);
        return matcher.group(1).trim();
    }

    private String getNonce(String directive) {
        Matcher matcher = CSP_NONCE_PATTERN.matcher(directive);
        assertTrue(matcher.find(), "Expected a nonce source in CSP directive: " + directive);
        return matcher.group(1);
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
