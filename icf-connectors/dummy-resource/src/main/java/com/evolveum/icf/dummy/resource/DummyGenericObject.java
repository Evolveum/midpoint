/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    private final String objectClassDescription;

    public DummyGenericObject(@NotNull String objectClassName) {
        this.objectClassName = objectClassName;
        this.objectClassDescription = null;
    }

    public DummyGenericObject(@NotNull String objectClassName, @NotNull String name) {
        super(name);
        this.objectClassName = objectClassName;
        this.objectClassDescription = null;
    }

    public DummyGenericObject(@NotNull String objectClassName, @Nullable String name, @NotNull DummyResource resource) {

        this(objectClassName, name, resource, null);
    }

    public DummyGenericObject(@NotNull String objectClassName, @Nullable String name, @NotNull DummyResource resource,
            String objectClassDescription) {
        super(generateNameIfNeeded(objectClassName, name, resource),
                resource);
        this.objectClassName = objectClassName;
        this.objectClassDescription = objectClassDescription;
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
    public String getObjectClassDescription() {
        return objectClassDescription;
    }

    @Override
    public String getShortTypeName() {
        return objectClassName;
    }
}
