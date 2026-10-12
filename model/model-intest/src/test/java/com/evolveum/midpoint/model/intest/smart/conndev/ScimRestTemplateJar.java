/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart.conndev;

import java.io.File;

/**
 * Location of the SCIM/REST template connector bundle ({@code connector-scimrest-generic})
 * that the connector-development story tests use as their source connector. The JAR is a
 * test-scope dependency of this module; the {@code copy-scimrest-test-connector} execution of
 * the {@code maven-dependency-plugin} (see the module POM) copies it to
 * {@code target/dependency} before the integration tests run.
 */
final class ScimRestTemplateJar {

    /** Must match the {@code conndev.version} property in the root POM. */
    static final String VERSION = "0.2.0-RC2";

    private static final String FILE_NAME = "connector-scimrest-generic-" + VERSION + ".jar";

    private ScimRestTemplateJar() {
    }

    /**
     * The absolute path of the template JAR - the {@code conndev.import.template.jars} system
     * property when set, otherwise the copy under {@code target/dependency} (the test JVM
     * working directory is the module basedir).
     */
    static File get() {
        var override = System.getProperty("conndev.import.template.jars");
        var jar = override != null && !override.isBlank()
                ? new File(override).getAbsoluteFile()
                : new File("target/dependency", FILE_NAME).getAbsoluteFile();
        if (!jar.isFile()) {
            throw new IllegalStateException("SCIM/REST template connector bundle not found: " + jar
                    + ". Run the Maven build of model-intest up to the pre-integration-test phase"
                    + " (the copy-scimrest-test-connector execution of the maven-dependency-plugin"
                    + " places the JAR there) or point the conndev.import.template.jars system"
                    + " property at the JAR explicitly.");
        }
        return jar;
    }
}
