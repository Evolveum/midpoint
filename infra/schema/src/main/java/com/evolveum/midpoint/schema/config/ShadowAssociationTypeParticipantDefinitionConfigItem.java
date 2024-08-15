/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeParticipantDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;

public interface ShadowAssociationTypeParticipantDefinitionConfigItem<PT extends ShadowAssociationTypeParticipantDefinitionType>
        extends ConfigurationItemable<PT> {

    default @NotNull Collection<? extends ResourceObjectTypeIdentification> getTypeIdentifiers() throws ConfigurationException {
        List<ResourceObjectTypeIdentification> identifications = new ArrayList<>();
        for (ResourceObjectTypeIdentificationType type : value().getObjectType()) {
            var kind = type.getKind();
            var intent = type.getIntent();
            configCheck(ShadowUtil.isKnown(kind), "None or unknown kind in %s", DESC);
            configCheck(ShadowUtil.isKnown(intent), "None or unknown intent in %s", DESC);
            identifications.add(
                    ResourceObjectTypeIdentification.of(kind, intent));
        }
        return identifications;
    }

    /**
     * This is the name under which we declare the association. It may be the same as the foundational
     * (native/simulated) reference attribute, or it can be a different one.
     */
    default @NotNull ItemName getAssociationNameRequired() throws ConfigurationException {
        var assocDefBean = nonNull(value().getAssociation(), "association definition");
        return singleNameRequired(assocDefBean.getRef(), "item/ref");
    }

    /** Returns the name of the reference attribute (native/simulated) this association type participation is based on. */
    default @NotNull ItemName getReferenceAttributeNameRequired() throws ConfigurationException {
        var association = value().getAssociation();
        if (association != null) {
            var refAttrName = association.getSourceAttributeRef();
            if (refAttrName != null) {
                return singleNameRequired(refAttrName, "sourceAttributeRef");
            }
        }
        return getAssociationNameRequired();
    }

    default boolean isBasedOnReferenceAttribute(@NotNull ItemName refAttrName) throws ConfigurationException {
        return refAttrName.matches(getReferenceAttributeNameRequired());
    }

    default @Nullable ShadowAssociationDefinitionConfigItem getAssociation() {
        return child(
                value().getAssociation(),
                ShadowAssociationDefinitionConfigItem.class,
                ShadowAssociationTypeParticipantDefinitionType.F_ASSOCIATION);
    }
}
