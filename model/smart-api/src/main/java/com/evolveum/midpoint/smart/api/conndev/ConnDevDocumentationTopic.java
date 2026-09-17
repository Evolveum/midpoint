/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import java.io.Serializable;

/**
 * One topic of the conndev connector documentation packaged into a documentation JAR
 * ({@code META-INF/conndev-doc/docs.yaml} manifest + fragment HTML).
 *
 * <p>Topics are addressed by their stable {@code key} (the GUI screen identifier). The optional
 * {@code protocol} tag marks a topic as protocol-specific; a topic without a protocol is the
 * generic variant. A query for a key resolves the protocol-specific topic first and falls back to
 * the generic topic of the same key — a key under a different protocol is never returned.
 *
 * @param key      the stable topic identifier used by the GUI
 * @param protocol the protocol the topic is specific to, or {@code null} for generic topics
 * @param title    the display name of the topic
 * @param html     the rendered documentation content (fragment HTML)
 */
public record ConnDevDocumentationTopic(String key, String protocol, String title, String html)
        implements Serializable {
}
