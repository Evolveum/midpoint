/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Used for various maps related to entitlements processing.
 *
 * - `objectClass`: Qualified object class name.
 * - `identifiers`: Identifiers of the object that contain the primary identifier.
 *
 * @author semancik
 */
record ResourceObjectDiscriminator(
        @NotNull QName objectClass,
        @NotNull ResourceObjectIdentifiers.WithPrimary identifiers)
        implements Serializable {

    static @NotNull ResourceObjectDiscriminator of(@NotNull ResourceObjectIdentification.WithPrimary identification) {
        return new ResourceObjectDiscriminator(
                identification.getResourceObjectDefinition().getObjectClassName(),
                identification.getIdentifiers());
    }

    /** Returns the discriminator for a shadow. The shadow must have the primary identifier and correct definition. */
    static @NotNull ResourceObjectDiscriminator of(@NotNull ShadowType completeShadow) {
        return of(
                ResourceObjectIdentification.fromCompleteShadow(completeShadow));
    }

    ResourceObjectDiscriminator {
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
