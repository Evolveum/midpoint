/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provides information about definitions of associations.
 */
public interface AssociationDefinitionStore {

    /**
     * Returns definitions of all associations as an unmodifiable collection.
     *
     * Note: these items are _not_ included in getDefinitions.
     * (BTW, ResourceAssociationDefinition is not a subtype of ItemDefinition, not even of Definition.)
     */
    @NotNull Collection<ResourceAssociationDefinition> getAssociationDefinitions();

    default Collection<ResourceAssociationDefinition> getAssociationDefinitions(ShadowKindType kind) {
        return getAssociationDefinitions().stream()
                .filter(association -> association.getKind() == kind)
                .toList();
    }

    default ResourceAssociationDefinition findAssociationDefinition(QName name) {
        return getAssociationDefinitions().stream()
                .filter(a -> QNameUtil.match(a.getName(), name))
                .findFirst().orElse(null);
    }

    default ResourceAssociationDefinition findAssociationDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        ResourceAssociationDefinition def = findAssociationDefinition(name);
        if (def == null) {
            throw new SchemaException("No definition of association named '" + name + "' in " + this + contextSupplier.get());
        }
        return def;
    }

    default @NotNull Collection<QName> getNamesOfAssociations() {
        return getAssociationDefinitions().stream()
                .map(ResourceAssociationDefinition::getName)
                .collect(Collectors.toCollection(HashSet::new));
    }

    default @NotNull Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions() {
        return getAssociationDefinitions().stream()
                .filter(assocDef -> assocDef.getOutboundMappingType() != null)
                .map(ResourceAssociationDefinition::getName)
                .collect(Collectors.toCollection(HashSet::new));
    }

    default @NotNull Collection<? extends QName> getNamesOfAssociationsWithInboundExpressions() {
        return getAssociationDefinitions().stream()
                .filter(assocDef -> CollectionUtils.isNotEmpty(assocDef.getInboundMappingBeans()))
                .map(ResourceAssociationDefinition::getName)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Returns all attributes that are used as targets for object-to-subject associations, i.e., attributes whose values
     * are referenced from the entitlements. For example, the group (an entitlement) may list its members by their `uid`
     * attribute. This method returns `uid` in such a case.
     *
     * The goal is to making sure such attributes are always cached, regardless of whether they are formally defined
     * as identifiers.
     */
    default @NotNull Collection<? extends QName> getAssociationValueAttributes() {
        return getAssociationDefinitions().stream()
                .filter(assocDef -> assocDef.isObjectToSubject())
                .map(associationDef -> associationDef.getDefinitionBean().getValueAttribute())
                .filter(Objects::nonNull) // just for sure
                .toList();
    }
}
