/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.io.Serializable;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * @param objectClass Qualified object class name.
 * @param identifiers Originally, here we expected primary identifiers. But in fact, clients put here almost anything.
 * @author semancik
 */
public record ResourceObjectDiscriminator(
        @NotNull QName objectClass,
        @NotNull ResourceObjectIdentifiers identifiers)
        implements Serializable {

    public static @NotNull ResourceObjectDiscriminator of(@NotNull ResourceObjectIdentification<?> identification) {
        return new ResourceObjectDiscriminator(
                identification.getResourceObjectDefinition().getObjectClassName(),
                identification.getIdentifiers());
    }

    public ResourceObjectDiscriminator {
        Preconditions.checkArgument(
                QNameUtil.isQualified(objectClass), "Object class must be qualified: %s", objectClass);
    }

    @Override
    public String toString() {
        return "ResourceObjectDiscriminator(" + objectClass + ": " + identifiers + ")";
    }

}
