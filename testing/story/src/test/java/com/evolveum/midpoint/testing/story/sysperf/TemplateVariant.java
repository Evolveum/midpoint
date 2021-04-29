/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

enum TemplateVariant {

    NONE("none"),
    T0010("t0010"),
    T0100("t0100");

    private static final String PROP_TEMPLATE = "template";

    private final String name;

    TemplateVariant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TemplateVariant setup() {
        String configuredName = System.getProperty(PROP_TEMPLATE, NONE.name);
        TemplateVariant variant = fromName(configuredName);
        System.out.println("Template variant: " + variant);
        return variant;
    }

    private static TemplateVariant fromName(String name) {
        for (TemplateVariant value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown template variant: " + name);
    }
}
