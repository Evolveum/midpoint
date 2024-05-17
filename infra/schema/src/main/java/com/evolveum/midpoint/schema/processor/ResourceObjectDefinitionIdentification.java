/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/** Uniquely identifies resource object type or class on a resource. */
public class ResourceObjectDefinitionIdentification implements Serializable {

    @NotNull private final String localObjectClassName;

    @Nullable private final ResourceObjectTypeIdentification objectType;

    private ResourceObjectDefinitionIdentification(
            @NotNull String localObjectClassName, @Nullable ResourceObjectTypeIdentification objectType) {
        this.localObjectClassName = localObjectClassName;
        this.objectType = objectType;
    }

    public static ResourceObjectDefinitionIdentification create(
            @NotNull String localObjectClassName, @Nullable ResourceObjectTypeIdentification objectType) {
        return new ResourceObjectDefinitionIdentification(localObjectClassName, objectType);
    }

    public static ResourceObjectDefinitionIdentification forClass(@NotNull String localObjectClassName) {
        return new ResourceObjectDefinitionIdentification(localObjectClassName, null);
    }

    public static ResourceObjectDefinitionIdentification forClass(@NotNull QName objectClassName) {
        return forClass(
                QNameUtil.getLocalPartCheckingNamespace(objectClassName, NS_RI));
    }

    public @NotNull String getLocalObjectClassName() {
        return localObjectClassName;
    }

    public @Nullable ResourceObjectTypeIdentification getObjectType() {
        return objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectDefinitionIdentification that = (ResourceObjectDefinitionIdentification) o;
        return Objects.equals(localObjectClassName, that.localObjectClassName)
                && Objects.equals(objectType, that.objectType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localObjectClassName, objectType);
    }

    @Override
    public String toString() {
        if (objectType != null) {
            return objectType + " (" + localObjectClassName + ")";
        } else {
            return localObjectClassName;
        }
    }
}
