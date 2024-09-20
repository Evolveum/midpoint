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

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeObjectDefinitionType;

public class ShadowAssociationTypeObjectDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeObjectDefinitionType>
        implements ShadowAssociationTypeParticipantDefinitionConfigItem<ShadowAssociationTypeObjectDefinitionType> {

    public ShadowAssociationTypeObjectDefinitionConfigItem(
            @NotNull ConfigurationItem<ShadowAssociationTypeObjectDefinitionType> original) {
        super(original);
    }

    @Override
    public @NotNull String localDescription() {
        return "object definition";
    }

    /** Returns the object participant name for this association object. If not specified explicitly, tries to determine it. */
    public @NotNull QName getRefOrDefault(
            @NotNull ShadowReferenceAttributeDefinition refAttrDef,
            @Nullable ResourceObjectDefinition assocDataObjectDef)
            throws ConfigurationException {
        var explicit = value().getRef();
        if (explicit != null) {
            return explicit;
        }
        if (assocDataObjectDef != null) {
            var refAttrs = assocDataObjectDef.getReferenceAttributeDefinitions();
            if (refAttrs.size() == 1) {
                return refAttrs.iterator().next().getItemName();
            } else {
                throw configException(
                        "Couldn't determine default object name for complex association (%s) in %s",
                        assocDataObjectDef, DESC);
            }
        }
        return refAttrDef.getItemName();
    }
}
