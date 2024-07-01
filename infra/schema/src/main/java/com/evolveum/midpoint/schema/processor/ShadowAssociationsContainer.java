/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.AbstractShadow;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

/**
 * Container holding {@link ShadowAssociation} objects. It must have the correct definitions applied.
 *
 * @see ShadowAttributesContainer
 */
public interface ShadowAssociationsContainer extends ShadowItemsContainer, PrismContainer<ShadowAssociationsType> {

    static ShadowAssociationsContainer convertFromPrismContainer(
            @NotNull PrismContainer<?> origPrismContainer, @NotNull ResourceObjectDefinition resourceObjectDefinition)
            throws SchemaException {
        var associationsContainer =
                resourceObjectDefinition
                        .toShadowAssociationsContainerDefinition()
                        .instantiate(origPrismContainer.getElementName());
        for (Item<?, ?> item : origPrismContainer.getValue().getItems()) {
            var associationName = item.getElementName();
            var associationDef = resourceObjectDefinition.findAssociationDefinitionRequired(associationName, ""); // TODO
            associationsContainer.add(
                    ShadowAssociation.convertFromPrismItem(item.clone(), associationDef));
        }
        return associationsContainer;
    }

    @Override
    ShadowAssociationsContainerDefinition getDefinition();

    default @NotNull ShadowAssociationsContainerDefinition getDefinitionRequired() {
        return MiscUtil.stateNonNull(
                getDefinition(),
                () -> "No definition in " + this);
    }

    /**
     * Returns the resource object associations. Their order is insignificant.
     * The returned set is immutable!
     */
    @NotNull Collection<ShadowAssociation> getAssociations();

    default @NotNull Collection<ItemName> getAssociationNames() {
        return getAssociations().stream()
                .map(a -> a.getElementName())
                .collect(Collectors.toSet());
    }

    @Override
    void add(Item<?, ?> item) throws SchemaException;

    void add(ShadowAssociation association) throws SchemaException;

    /**
     * Finds a specific attribute in the resource object by name.
     *
     * Returns null if nothing is found.
     *
     * @param assocName attribute name to find.
     * @return found attribute or null
     */
    ShadowAssociation findAssociation(QName assocName);

    ShadowAssociation findOrCreateAssociation(QName assocName) throws SchemaException;

    default ShadowAssociationsContainer add(QName attributeName, ShadowAssociationValue value) throws SchemaException {
        findOrCreateAssociation(attributeName)
                .add(value);
        return this;
    }

    default ShadowAssociationsContainer add(QName attributeName, AbstractShadow associationShadow) throws SchemaException {
        findOrCreateAssociation(attributeName)
                .createNewValue()
                .fillFromAssociationObject(associationShadow);
        return this;
    }

    @Override
    ShadowAssociationsContainer clone();

    default @NotNull Collection<? extends ShadowAssociationValue> getAssociationValues(QName assocName) {
        var association = findAssociation(assocName);
        return association != null ? association.getAssociationValues() : List.of();
    }
}
