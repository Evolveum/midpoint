/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.smart.api.conndev.ConnDevDocumentationTopic;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.polygon.conndev.devtools.doc.DocumentationCatalog;
import com.evolveum.polygon.conndev.devtools.doc.DocTopic;

import org.springframework.stereotype.Component;

/**
 * Access to the conndev connector documentation packaged into the documentation JARs on the
 * classpath. Every documentation JAR carries a manifest at
 * {@code META-INF/conndev-doc/docs.yaml} (see conndev's {@code ConndevDocFormat}) mapping stable
 * GUI topic keys to rendered fragment HTML.
 *
 * <p>Catalogs are discovered once through {@link DocumentationCatalog#discover(ClassLoader)}; the
 * classpath is static for the lifetime of the JVM, so the discovery result and the resolved
 * topics are cached.
 *
 * <p>Topics resolve strictly by protocol: for a key, the topics whose protocol equals the
 * requested one come first, then the protocol-less (generic) topics of the same key. When several
 * documentation JARs contribute topics for the same key, the protocol-specific bucket is taken
 * from the first JAR that has one and the generic bucket from the first JAR that has one
 * (classpath order).
 */
@Component
public class ConndevDocumentationService {

    private static final Trace LOGGER = TraceManager.getTrace(ConndevDocumentationService.class);

    private final ClassLoader classLoader;
    private final Map<String, List<ConnDevDocumentationTopic>> topicCache = new ConcurrentHashMap<>();
    private volatile List<DocumentationCatalog> catalogs;

    public ConndevDocumentationService() {
        this(ConndevDocumentationService.class.getClassLoader());
    }

    ConndevDocumentationService(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * @param key      the stable topic identifier used by the GUI screen
     * @param protocol the integration protocol to resolve for, or {@code null} for the generic
     *                 topics only
     * @return the matching topics, protocol-specific before generic; empty when no documentation
     *         JAR declares the key
     */
    public List<ConnDevDocumentationTopic> getTopics(String key, String protocol) {
        if (key == null || key.isBlank()) {
            return List.of();
        }
        String normalizedProtocol = (protocol == null || protocol.isBlank()) ? null : protocol;
        String cacheKey = key + "\u0000" + normalizedProtocol;
        return topicCache.computeIfAbsent(cacheKey, k -> resolveTopics(key, normalizedProtocol));
    }

    private List<ConnDevDocumentationTopic> resolveTopics(String key, String protocol) {
        var specific = new ArrayList<DocumentationCatalog>();
        var generic = new ArrayList<DocumentationCatalog>();
        var specificTopics = new ArrayList<DocTopic>();
        var genericTopics = new ArrayList<DocTopic>();
        for (var catalog : catalogs()) {
            var topics = catalog.topics(key, protocol);
            if (topics.isEmpty()) {
                continue;
            }
            if (specificTopics.isEmpty()) {
                for (var topic : topics) {
                    if (!topic.isGeneric()) {
                        specificTopics.add(topic);
                        specific.add(catalog);
                    }
                }
            }
            if (genericTopics.isEmpty()) {
                for (var topic : topics) {
                    if (topic.isGeneric()) {
                        genericTopics.add(topic);
                        generic.add(catalog);
                    }
                }
            }
            if (!specificTopics.isEmpty() && !genericTopics.isEmpty()) {
                break;
            }
        }

        var result = new ArrayList<ConnDevDocumentationTopic>(specificTopics.size() + genericTopics.size());
        addTopics(result, specific, specificTopics);
        addTopics(result, generic, genericTopics);
        return List.copyOf(result);
    }

    private void addTopics(List<ConnDevDocumentationTopic> result, List<DocumentationCatalog> catalogs, List<DocTopic> topics) {
        for (int i = 0; i < topics.size(); i++) {
            var topic = topics.get(i);
            String html;
            try {
                html = catalogs.get(i).readDocument(topic);
            } catch (IOException e) {
                LOGGER.warn("Couldn't read documentation topic '{}' ({}).", topic.key(), topic.resource(), e);
                continue;
            }
            result.add(new ConnDevDocumentationTopic(topic.key(), topic.protocol(), topic.title(), html));
        }
    }

    private List<DocumentationCatalog> catalogs() {
        var result = catalogs;
        if (result == null) {
            synchronized (this) {
                result = catalogs;
                if (result == null) {
                    try {
                        result = DocumentationCatalog.discover(classLoader);
                    } catch (IOException e) {
                        LOGGER.error("Couldn't discover conndev documentation catalogs.", e);
                        result = List.of();
                    }
                    LOGGER.debug("Discovered {} conndev documentation catalog(s).", result.size());
                    catalogs = result;
                }
            }
        }
        return result;
    }
}
