/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * - `objectClass`: Qualified object class name.
 * - `identifiers`: Identifiers of the object that contain the primary identifier.
 *
 * @author semancik
 */
public record ResourceObjectDiscriminator(
        @NotNull QName objectClass,
        @NotNull ResourceObjectIdentifiers.WithPrimary identifiers)
        implements Serializable {

    public static @NotNull ResourceObjectDiscriminator of(@NotNull ResourceObjectIdentification.WithPrimary identification) {
        return new ResourceObjectDiscriminator(
                identification.getResourceObjectDefinition().getObjectClassName(),
                identification.getIdentifiers());
    }

    public ResourceObjectDiscriminator {
        Preconditions.checkArgument(
                QNameUtil.isQualified(objectClass), "Object class must be qualified: %s", objectClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectDiscriminator that = (ResourceObjectDiscriminator) o;
        return Objects.equals(objectClass, that.objectClass)
                && Objects.equals(getPrimaryIdentifierRealValue(), that.getPrimaryIdentifierRealValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectClass, getPrimaryIdentifierRealValue());
    }

    private @NotNull Object getPrimaryIdentifierRealValue() {
        return identifiers.getPrimaryIdentifier().getOrigValue();
    }

    @Override
    public String toString() {
        return "ResourceObjectDiscriminator(" + objectClass + ": " + identifiers + ")";
    }

}
