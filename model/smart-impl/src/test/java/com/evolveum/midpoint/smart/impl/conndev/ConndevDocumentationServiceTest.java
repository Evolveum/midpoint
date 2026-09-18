/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.evolveum.polygon.conndev.devtools.doc.ConndevDocFormat;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ConndevDocumentationService} against both the real documentation JARs on the
 * test classpath and synthetic multi-JAR situations (colliding keys, protocol buckets).
 */
public class ConndevDocumentationServiceTest {

    private ConndevDocumentationService service;

    private Path jarAlpha;
    private Path jarBeta;
    private URLClassLoader classLoader;

    @BeforeClass
    public void setUp() {
        service = new ConndevDocumentationService();
    }

    @BeforeClass
    public void buildSyntheticJars() throws IOException {
        jarAlpha = writeDocJar("alpha", """
                apiVersion: conndev-doc/v1
                connectorId: alpha
                topics:
                  - key: search-all
                    title: Search All (scim)
                    resource: shared/search-all-scim.html
                    protocol: scim
                    order: 10
                  - key: search-all
                    title: Search All (generic)
                    resource: shared/search-all.html
                    order: 20
                  - key: search-all
                    title: Search All (rest)
                    resource: shared/search-all-rest.html
                    protocol: rest
                    order: 30
                  - key: key-two
                    title: Key Two (scim)
                    resource: shared/key-two-scim.html
                    protocol: scim
                    order: 10
                """,
                "shared/search-all-scim.html", "<p>ALPHA-SCIM</p>",
                "shared/search-all.html", "<p>ALPHA-GENERIC</p>",
                "shared/search-all-rest.html", "<p>ALPHA-REST</p>",
                "shared/key-two-scim.html", "<p>ALPHA-KEY2</p>");
        jarBeta = writeDocJar("beta", """
                apiVersion: conndev-doc/v1
                connectorId: beta
                topics:
                  - key: search-all
                    title: Search All (scim, colliding)
                    resource: shared/search-all.html
                    protocol: scim
                    order: 10
                  - key: key-two
                    title: Key Two (generic)
                    resource: shared/key-two.html
                    order: 20
                """,
                "shared/search-all.html", "<p>BETA-SCIM</p>",
                "shared/key-two.html", "<p>BETA-GENERIC</p>");
        classLoader = new URLClassLoader(
                new URL[] { jarAlpha.toUri().toURL(), jarBeta.toUri().toURL() }, null);
    }

    @AfterClass
    public void cleanUp() throws IOException {
        if (classLoader != null) {
            classLoader.close();
        }
        Files.deleteIfExists(jarAlpha);
        Files.deleteIfExists(jarBeta);
    }

    @Test
    public void unknownKeyReturnsEmpty() {
        Assert.assertTrue(service.getTopics("no-such-key", "scim").isEmpty());
        Assert.assertTrue(service.getTopics("no-such-key", null).isEmpty());
        Assert.assertTrue(service.getTopics(null, "scim").isEmpty());
        Assert.assertTrue(service.getTopics("  ", "scim").isEmpty());
    }

    @Test
    public void protocolSpecificWinsOverGenericAndFirstJarWins() {
        var topics = new ConndevDocumentationService(classLoader).getTopics("search-all", "scim");

        assertThat(topics.size()).isEqualTo(2);
        // Protocol-specific topic of the first JAR on the classpath wins over the colliding one
        assertThat(topics.get(0).protocol()).isEqualTo("scim");
        assertThat(topics.get(0).title()).isEqualTo("Search All (scim)");
        assertThat(topics.get(0).html()).isEqualTo("<p>ALPHA-SCIM</p>");
        // Generic topic of the same key follows
        assertThat(topics.get(1).protocol()).isNull();
        assertThat(topics.get(1).html()).isEqualTo("<p>ALPHA-GENERIC</p>");
    }

    @Test
    public void genericBucketFallsBackToAnotherJar() {
        var topics = new ConndevDocumentationService(classLoader).getTopics("key-two", "scim");

        assertThat(topics.size()).isEqualTo(2);
        assertThat(topics.get(0).protocol()).isEqualTo("scim");
        assertThat(topics.get(0).html()).isEqualTo("<p>ALPHA-KEY2</p>");
        assertThat(topics.get(1).protocol()).isNull();
        assertThat(topics.get(1).html()).isEqualTo("<p>BETA-GENERIC</p>");
    }

    @Test
    public void requestedProtocolTopicsComeBeforeGeneric() {
        var topics = new ConndevDocumentationService(classLoader).getTopics("search-all", "rest");

        assertThat(topics).hasSize(2);
        assertThat(topics.get(0).protocol()).isEqualTo( "rest");
        assertThat(topics.get(0).html()).isEqualTo( "<p>ALPHA-REST</p>");
        assertThat(topics.get(1).protocol()).isNull();
        assertThat(topics.get(1).html()).isEqualTo("<p>ALPHA-GENERIC</p>");
        for (var topic : topics) {
            assertThat(topic.protocol())
                    .withFailMessage("Foreign protocol must never be returned")
                    .isNotEqualTo("scim");
        }
    }

    private static Path writeDocJar(
            String connectorId, String manifest, String... resourceAndContentPairs) throws IOException {
        var jar = Files.createTempFile("conndev-docs-test-" + connectorId + "-", ".jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(ConndevDocFormat.MANIFEST_RESOURCE));
            output.write(manifest.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            for (int i = 0; i < resourceAndContentPairs.length; i += 2) {
                var resource = ConndevDocFormat.DOC_ROOT + resourceAndContentPairs[i];
                output.putNextEntry(new JarEntry(resource));
                output.write(resourceAndContentPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return jar;
    }
}
