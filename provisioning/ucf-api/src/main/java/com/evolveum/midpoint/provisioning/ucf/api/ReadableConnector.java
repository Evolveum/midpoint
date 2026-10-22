/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.IOException;

/**
 * A read-only view of a local connector bundle - a bundle directory, or a JAR file when the
 * connector was installed as a JAR bundle.
 */
public interface ReadableConnector {

    /**
     * Whether a file with the given path (relative to the bundle root) exists in the bundle.
     * Invalid paths (e.g. escaping the bundle) are reported as not existing.
     */
    boolean fileExists(String filename);

    /**
     * The content of the file with the given path (relative to the bundle root).
     *
     * @throws IOException when the file does not exist in the bundle or cannot be read
     */
    String readFile(String filename) throws IOException;
}
