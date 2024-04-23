/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;

/**
 * An object with custom structural object class.
 */
public class DummyGenericObject extends DummyObject {

    @NotNull private final String objectClassName;

    public DummyGenericObject(@NotNull String objectClassName) {
        this.objectClassName = objectClassName;
    }

    public DummyGenericObject(@NotNull String objectClassName, String name) {
        super(name);
        this.objectClassName = objectClassName;
    }

    @Override
    public @NotNull String getObjectClassName() {
        return objectClassName;
    }

    @Override
    public String getShortTypeName() {
        return objectClassName;
    }
}
