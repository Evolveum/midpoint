/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.icf.dummy.resource;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object with custom structural object class.
 */
public class DummyGenericObject extends DummyObject {

    @NotNull private final String objectClassName;

    public DummyGenericObject(@NotNull String objectClassName) {
        this.objectClassName = objectClassName;
    }

    public DummyGenericObject(@NotNull String objectClassName, @NotNull String name) {
        super(name);
        this.objectClassName = objectClassName;
    }

    public DummyGenericObject(@NotNull String objectClassName, @Nullable String name, @NotNull DummyResource resource) {
        super(generateNameIfNeeded(objectClassName, name, resource),
                resource);
        this.objectClassName = objectClassName;
    }

    private static String generateNameIfNeeded(String objectClassName, String name, DummyResource resource) {
        if (name != null) {
            return name;
        }
        var objectClassDef = resource.getStructuralObjectClass(objectClassName);
        if (objectClassDef.isEmbeddedObject()) {
            return RandomStringUtils.randomAlphabetic(20);
        } else {
            throw new IllegalArgumentException("No name provided for a regular (non-association) object: " + objectClassName);
        }
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
