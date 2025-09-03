/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * Represents object type definition. Currently, also complex attribute type definitions are transformed into this CI.
 */
public class ResourceObjectTypeDefinitionConfigItem
        extends ResourceObjectDefinitionConfigItem<ResourceObjectTypeDefinitionType>
        implements ResourceDataTypeDefinitionConfigItem<ResourceObjectTypeDefinitionType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ResourceObjectTypeDefinitionConfigItem(@NotNull ConfigurationItem<ResourceObjectTypeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getObjectClassName() throws ConfigurationException {
        QName newName = getNewObjectClassName();
        QName legacyName = getLegacyObjectClassName();

        configCheck(newName == null || legacyName == null || QNameUtil.match(newName, legacyName),
                "Contradicting legacy (%s) vs delineation-based (%s) object class names in %s",
                legacyName, newName, DESC);

        return nonNull(
                MiscUtil.getFirstNonNull(newName, legacyName),
                "object class name");
    }


    private QName getLegacyObjectClassName() {
        return value().getObjectClass();
    }

    /** Method to get the class name for diagnostics purposes. Should not fail on wrong configuration. */
    protected @Nullable QName getObjectClassNameAny() {
        QName newName = getNewObjectClassName();
        if (newName != null) {
            return newName;
        } else {
            return getLegacyObjectClassName();
        }
    }

    private @Nullable QName getNewObjectClassName() {
        var delineation = value().getDelineation();
        return delineation != null ? delineation.getObjectClass() : null;
    }

    public boolean isAbstract() {
        return Boolean.TRUE.equals(value().isAbstract());
    }

    public @NotNull ResourceObjectTypeIdentification getTypeIdentification() throws ConfigurationException {
        var kind = ResourceObjectTypeDefinitionTypeUtil.getKind(value());
        var intent = ResourceObjectTypeDefinitionTypeUtil.getIntent(value());
        configCheck(ShadowUtil.isKnown(kind), "Unknown kind in %s", DESC);
        configCheck(ShadowUtil.isKnown(intent), "Unknown intent in %s", DESC);
        return ResourceObjectTypeIdentification.of(kind, intent);
    }

    @Override
    public @NotNull ResourceObjectTypeDefinitionType getObjectTypeDefinitionBean() {
        return value();
    }

    @Override
    public @NotNull String localDescription() {
        var displayName = value().getDisplayName();
        return "object type %s/%s %sdefinition".formatted(
                value().getKind(), value().getIntent(), displayName != null ? "(" + displayName + ") " : "");
    }
}
