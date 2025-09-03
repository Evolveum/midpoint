/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;

/**
 * Represents one of the following:
 *
 * - definition of a resource object type (e.g. "account/default"); B is {@link ResourceObjectTypeDefinitionType}
 * - definition of a resource object class (e.g. "inetOrgPerson"); B is {@link ResourceObjectTypeDefinitionType}
 */
public class ResourceObjectDefinitionConfigItem<B extends ResourceObjectTypeDefinitionType>
        extends AbstractResourceDataDefinitionConfigItem<B> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ResourceObjectDefinitionConfigItem(
            @NotNull ConfigurationItem<B> original) {
        super(original);
    }

    public @NotNull List<QName> getAuxiliaryObjectClassNames() throws ConfigurationException {
        ResourceObjectTypeDelineationType delineation = value().getDelineation();
        List<QName> newValues = delineation != null ? delineation.getAuxiliaryObjectClass() : List.of();
        List<QName> legacyValues = value().getAuxiliaryObjectClass();
        // We do not want to compare the lists (etc) - we simply disallow specifying at both places.
        configCheck(newValues.isEmpty() || legacyValues.isEmpty(),
                "Auxiliary object classes must not be specified in both new and legacy ways in %s", DESC);
        if (!newValues.isEmpty()) {
            return newValues;
        } else {
            return legacyValues;
        }
    }

    public List<ResourceAttributeDefinitionConfigItem> getAttributes() {
        return children(
                value().getAttribute(),
                ResourceAttributeDefinitionConfigItem.class,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
    }

    public List<ResourceObjectAssociationConfigItem> getLegacyAssociations() {
        return children(
                value().getAssociation(),
                ResourceObjectAssociationConfigItem.class,
                ResourceObjectTypeDefinitionType.F_ASSOCIATION);
    }

//    public @Nullable ResourceObjectAssociationConfigItem getAssociationDefinitionIfPresent(ItemName assocName)
//            throws ConfigurationException {
//        List<ResourceObjectAssociationConfigItem> matching = new ArrayList<>();
//        for (var assocDef : getAssociations()) {
//            if (QNameUtil.match(assocDef.getItemName(), assocName)) {
//                matching.add(assocDef);
//            }
//        }
//        return single(matching, "Duplicate definition of association '%s' in %s", assocName, DESC);
//    }
}
