/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectIdentifiersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectIdentityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class ResourceObjectIdentification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResourceObjectDefinition resourceObjectDefinition;
    @NotNull private final Collection<? extends ResourceAttribute<?>> primaryIdentifiers;
    @NotNull private final Collection<? extends ResourceAttribute<?>> secondaryIdentifiers;
    // TODO: identification strategy

    public ResourceObjectIdentification(
            ResourceObjectDefinition resourceObjectDefinition,
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers,
            Collection<? extends ResourceAttribute<?>> secondaryIdentifiers) {
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.primaryIdentifiers = MiscUtil.emptyIfNull(primaryIdentifiers);
        this.secondaryIdentifiers = MiscUtil.emptyIfNull(secondaryIdentifiers);
    }

    public @NotNull Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers() {
        return primaryIdentifiers;
    }

    public <T> ResourceAttribute<T> getPrimaryIdentifier() throws SchemaException {
        //noinspection unchecked
        return (ResourceAttribute<T>)
                MiscUtil.extractSingleton(
                        primaryIdentifiers,
                        () -> new SchemaException("More than one primary identifier in " + this));
    }

    public @NotNull Collection<? extends ResourceAttribute<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiers;
    }

    public <T> ResourceAttribute<T> getSecondaryIdentifier() throws SchemaException {
        //noinspection unchecked
        return (ResourceAttribute<T>)
                MiscUtil.extractSingleton(
                        secondaryIdentifiers,
                        () -> new SchemaException("More than one secondary identifier in " + this));
    }

    /**
     * Returned collection should be never modified!
     */
    public Collection<? extends ResourceAttribute<?>> getAllIdentifiers() {
        return MiscUtil.concat(primaryIdentifiers, secondaryIdentifiers);
    }

    public ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public static ResourceObjectIdentification create(ResourceObjectDefinition objectDefinition,
            Collection<? extends ResourceAttribute<?>> allIdentifiers) throws SchemaException {
        if (allIdentifiers == null) {
            throw new IllegalArgumentException("Cannot create ResourceObjectIdentification with null identifiers");
        }
        Collection<? extends ResourceAttribute<?>> primaryIdentifiers = null;
        Collection<? extends ResourceAttribute<?>> secondaryIdentifiers = null;
        for (ResourceAttribute<?> identifier: allIdentifiers) {
            if (objectDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                if (primaryIdentifiers == null) {
                    primaryIdentifiers = new ArrayList<>();
                }
                //noinspection unchecked,rawtypes
                ((Collection)primaryIdentifiers).add(identifier);
            } else if (objectDefinition.isSecondaryIdentifier(identifier.getElementName())) {
                if (secondaryIdentifiers == null) {
                    secondaryIdentifiers = new ArrayList<>();
                }
                //noinspection unchecked,rawtypes
                ((Collection)secondaryIdentifiers).add(identifier);
            } else {
                throw new SchemaException("Attribute "+identifier+" is neither primary not secondary identifier in object class "+objectDefinition);
            }
        }
        return new ResourceObjectIdentification(objectDefinition, primaryIdentifiers, secondaryIdentifiers);
    }

    public static ResourceObjectIdentification createFromAttributes(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> attributes) {
        Collection<? extends ResourceAttribute<?>> primaryIdentifiers = null;
        Collection<? extends ResourceAttribute<?>> secondaryIdentifiers = null;
        for (ResourceAttribute<?> identifier : attributes) {
            if (resourceObjectDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                if (primaryIdentifiers == null) {
                    primaryIdentifiers = new ArrayList<>();
                }
                //noinspection unchecked,rawtypes
                ((Collection)primaryIdentifiers).add(identifier);
            } else if (resourceObjectDefinition.isSecondaryIdentifier(identifier.getElementName())) {
                if (secondaryIdentifiers == null) {
                    secondaryIdentifiers = new ArrayList<>();
                }
                //noinspection unchecked,rawtypes
                ((Collection)secondaryIdentifiers).add(identifier);
            }
        }
        return new ResourceObjectIdentification(resourceObjectDefinition, primaryIdentifiers, secondaryIdentifiers);
    }

    public static ResourceObjectIdentification createFromShadow(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ShadowType shadow) {
        return createFromAttributes(resourceObjectDefinition, ShadowUtil.getAttributes(shadow));
    }

    public void validatePrimaryIdentifiers() {
        stateCheck(hasPrimaryIdentifiers(), "No primary identifiers in %s", this);
    }

    public boolean hasPrimaryIdentifiers() {
        return !primaryIdentifiers.isEmpty();
    }

    /**
     * TODO Or should we compare only relevant parts of the definition? And compare identifiers in unordered way?
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectIdentification that = (ResourceObjectIdentification) o;
        return Objects.equals(resourceObjectDefinition, that.resourceObjectDefinition)
                && primaryIdentifiers.equals(that.primaryIdentifiers);
    }

    /**
     * TODO Or should we compare only relevant parts of the definition? And compare identifiers in unordered way?
     */
    @Override
    public int hashCode() {
        return Objects.hash(resourceObjectDefinition, primaryIdentifiers);
    }

    @Override
    public String toString() {
        return "ResourceObjectIdentification(" + PrettyPrinter.prettyPrint(resourceObjectDefinition.getTypeName())
                + ": primary=" + primaryIdentifiers + ", secondary=" + secondaryIdentifiers + ")";
    }

    @NotNull
    public ResourceObjectIdentityType asBean() throws SchemaException {
        ResourceObjectIdentityType bean = new ResourceObjectIdentityType();
        if (resourceObjectDefinition != null) {
            bean.setObjectClass(resourceObjectDefinition.getTypeName());
        }
        bean.setPrimaryIdentifiers(getIdentifiersAsBean(primaryIdentifiers));
        bean.setSecondaryIdentifiers(getIdentifiersAsBean(secondaryIdentifiers));
        return bean;
    }

    private ResourceObjectIdentifiersType getIdentifiersAsBean(Collection<? extends ResourceAttribute<?>> identifiers)
            throws SchemaException {
        if (identifiers.isEmpty()) {
            return null;
        }
        ResourceObjectIdentifiersType identifiersBean = new ResourceObjectIdentifiersType();
        for (ResourceAttribute<?> identifier : identifiers) {
            //noinspection unchecked
            identifiersBean.asPrismContainerValue().add(identifier.clone());
        }
        return identifiersBean;
    }
}
