/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectIdentifiersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectIdentityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Identification of a resource object using its primary and/or secondary identifiers.
 *
 * For facilitating type safety in clients, we provide two specific subclasses: {@link Primary} and {@link NoPrimary}.
 *
 * @author semancik
 */
public class ResourceObjectIdentification implements Serializable, DebugDumpable, Cloneable {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

    /** Unmodifiable. */
    @NotNull private final Collection<? extends ResourceAttribute<?>> primaryIdentifiers;

    /** Unmodifiable. */
    @NotNull private final Collection<? extends ResourceAttribute<?>> secondaryIdentifiers;

    // TODO: identification strategy

    private ResourceObjectIdentification(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> primaryIdentifiers,
            @NotNull Collection<? extends ResourceAttribute<?>> secondaryIdentifiers) {
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.primaryIdentifiers = Collections.unmodifiableCollection(primaryIdentifiers);
        this.secondaryIdentifiers = Collections.unmodifiableCollection(secondaryIdentifiers);
    }

    public ResourceObjectIdentification.Primary primary(@NotNull Collection<ResourceAttribute<?>> primaryIdentifiers) {
        argCheck(!primaryIdentifiers.isEmpty(), "No primary identifiers to be added to %s", this);
        return new Primary(resourceObjectDefinition, primaryIdentifiers, secondaryIdentifiers);
    }

    public static ResourceObjectIdentification of(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> primaryIdentifiers,
            @NotNull Collection<? extends ResourceAttribute<?>> secondaryIdentifiers) {
        if (!primaryIdentifiers.isEmpty()) {
            return new Primary(resourceObjectDefinition, primaryIdentifiers, secondaryIdentifiers);
        } else {
            return new NoPrimary(resourceObjectDefinition, secondaryIdentifiers);
        }
    }

    private static ResourceObjectIdentification fromIdentifiersOrAttributes(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> allAttributes,
            boolean nonIdentifiersAllowed) throws SchemaException {
        Collection<ResourceAttribute<?>> primaryIdentifiers = new ArrayList<>();
        Collection<ResourceAttribute<?>> secondaryIdentifiers = new ArrayList<>();
        for (ResourceAttribute<?> attribute : allAttributes) {
            if (objectDefinition.isPrimaryIdentifier(attribute.getElementName())) {
                primaryIdentifiers.add(attribute);
            } else if (objectDefinition.isSecondaryIdentifier(attribute.getElementName())) {
                secondaryIdentifiers.add(attribute);
            } else if (!nonIdentifiersAllowed) {
                throw new SchemaException(
                        "Attribute %s is neither primary not secondary identifier in object class %s".
                                formatted(attribute, objectDefinition));
            } else {
                // just ignore
            }
        }
        return ResourceObjectIdentification.of(objectDefinition, primaryIdentifiers, secondaryIdentifiers);
    }

    public static ResourceObjectIdentification fromIdentifiers(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> allIdentifiers) throws SchemaException {
        return fromIdentifiersOrAttributes(objectDefinition, allIdentifiers, false);
    }

    public static ResourceObjectIdentification fromAttributes(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> attributes) {
        try {
            return fromIdentifiersOrAttributes(resourceObjectDefinition, attributes, true);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    public static ResourceObjectIdentification fromShadow(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ShadowType shadow) {
        return fromAttributes(resourceObjectDefinition, ShadowUtil.getAttributes(shadow));
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

    public @Nullable PrismPropertyValue<?> getSecondaryIdentifierValue(@NotNull QName name) throws SchemaException {
        var identifier = getSecondaryIdentifier(name);
        if (identifier == null) {
            return null;
        }
        return MiscUtil.extractSingleton(
                (Collection<? extends PrismPropertyValue<?>>) identifier.getValues(),
                () -> new SchemaException("Secondary identifier has more than one value: %s in %s"
                        .formatted(identifier.getValues(), this)));
    }

    private ResourceAttribute<?> getSecondaryIdentifier(@NotNull QName name) {
        for (ResourceAttribute<?> identifier : secondaryIdentifiers) {
            if (identifier.getElementName().equals(name)) {
                return identifier;
            }
        }
        return null;
    }

    /**
     * Returned collection should be never modified!
     */
    public Collection<? extends ResourceAttribute<?>> getAllIdentifiers() {
        return Collections.unmodifiableCollection(
                MiscUtil.concat(primaryIdentifiers, secondaryIdentifiers));
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
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
        return resourceObjectDefinition.equals(that.resourceObjectDefinition)
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
        bean.setObjectClass(resourceObjectDefinition.getTypeName());
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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ResourceObjectIdentification clone() {
        return of(
                resourceObjectDefinition,
                CloneUtil.cloneCollectionMembers(primaryIdentifiers),
                CloneUtil.cloneCollectionMembers(secondaryIdentifiers));
    }

    public Primary ensurePrimary() {
        if (this instanceof Primary primary) {
            return primary;
        } else {
            throw new IllegalStateException("Expected primary identification, but got " + this);
        }
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectDefinition", resourceObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifiers", primaryIdentifiers, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "secondaryIdentifiers", secondaryIdentifiers, indent + 1);
        return sb.toString();
    }

    /** Identification that contains a primary identifier. */
    public static class Primary extends ResourceObjectIdentification {

        private Primary(
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull Collection<? extends ResourceAttribute<?>> primaryIdentifiers,
                @NotNull Collection<? extends ResourceAttribute<?>> secondaryIdentifiers) {
            super(resourceObjectDefinition, primaryIdentifiers, secondaryIdentifiers);
            argCheck(!primaryIdentifiers.isEmpty(), "No primary identifiers in %s", this);
        }
    }

    /** Identification that does not contain a primary identifier. */
    public static class NoPrimary extends ResourceObjectIdentification {

        private NoPrimary(
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull Collection<? extends ResourceAttribute<?>> secondaryIdentifiers) {
            super(resourceObjectDefinition, List.of(), secondaryIdentifiers);
        }
    }
}
