/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

enum ExtensionSchemaVariant {

    BASIC("basic"),
    BIG("big"),
    BIG_GLOBAL("big-global"),
    INDEXED("indexed");

    private static final String PROP_EXTENSION_SCHEMA = "schema";

    private final String name;

    ExtensionSchemaVariant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getExtensionSchemaDir() {
        return "./src/test/resources/system-perf/schema-" + name;
    }

    public static ExtensionSchemaVariant setup() {
        String configuredSchemaVariant = System.getProperty(PROP_EXTENSION_SCHEMA, BASIC.name);
        ExtensionSchemaVariant schemaVariant = fromName(configuredSchemaVariant);
        System.out.println("Extension schema variant: " + schemaVariant);
        String extensionSchemaDir = schemaVariant.getExtensionSchemaDir();
        System.out.println("Extension schema directory: " + extensionSchemaDir);
        System.setProperty("midpoint.global.extensionDir", extensionSchemaDir);
        return schemaVariant;
    }

    private static ExtensionSchemaVariant fromName(String name) {
        for (ExtensionSchemaVariant value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown extension schema variant: " + name);
    }
}
