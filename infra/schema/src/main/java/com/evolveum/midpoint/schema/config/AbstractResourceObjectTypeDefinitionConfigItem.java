/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class AbstractResourceObjectTypeDefinitionConfigItem<B extends ResourceObjectTypeDefinitionType>
        extends AbstractResourceObjectDefinitionConfigItem<B> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public AbstractResourceObjectTypeDefinitionConfigItem(@NotNull ConfigurationItem<B> original) {
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
        ResourceObjectTypeDelineationType delineation = value().getDelineation();
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
    public @NotNull String localDescription() {
        var displayName = value().getDisplayName();
        return "object type %s/%s %sdefinition".formatted(
                value().getKind(), value().getIntent(), displayName != null ? "(" + displayName + ") " : "");
    }
}
