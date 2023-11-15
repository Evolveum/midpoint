/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectIdentifiersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectIdentityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Identification of a resource object using its primary and/or secondary identifiers.
 *
 * For facilitating type safety (mainly) across APIs, we provide two specific subclasses:
 * {@link WithPrimary} and {@link SecondaryOnly}.
 *
 * NOTE: Making this class parameterized by the type of identifiers (primary-containing or secondary-only)
 * may or may not be a good idea. It makes the implementation nicer, but the clients are bothered with
 * additional `<?>` in the code. To be reconsidered.
 *
 * @see ResourceObjectIdentifier
 *
 * @author semancik
 */
public abstract class ResourceObjectIdentification<I extends ResourceObjectIdentifiers>
        implements Serializable, DebugDumpable, Cloneable {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

    /** Primary and/or secondary identifiers */
    @NotNull final I identifiers;

    // TODO: identification strategy

    private ResourceObjectIdentification(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull I identifiers) {
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.identifiers = identifiers;
    }

    public static @NotNull ResourceObjectIdentification<?> of(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ResourceObjectIdentifiers identifiers) {
        if (identifiers instanceof ResourceObjectIdentifiers.WithPrimary withPrimary) {
            return new WithPrimary(resourceObjectDefinition, withPrimary);
        } else if (identifiers instanceof ResourceObjectIdentifiers.SecondaryOnly secondaryOnly) {
            return new SecondaryOnly(resourceObjectDefinition, secondaryOnly);
        } else {
            throw new AssertionError(identifiers);
        }
    }

    public static @NotNull ResourceObjectIdentification.WithPrimary of(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ResourceObjectIdentifiers.WithPrimary primaryIdentifiers) {
        return new WithPrimary(resourceObjectDefinition, primaryIdentifiers);
    }

    /** Creates new identification with a primary identifier. */
    public static WithPrimary withPrimary(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ResourceAttribute<?> primaryIdentifierAttribute,
            @NotNull Collection<? extends ResourceAttribute<?>> secondaryIdentifierAttributes) {
        return new WithPrimary(
                resourceObjectDefinition,
                ResourceObjectIdentifiers.withPrimary(
                        ResourceObjectIdentifier.Primary.of(primaryIdentifierAttribute),
                        ResourceObjectIdentifier.Secondary.of(secondaryIdentifierAttributes)));
    }

    /** Gets identifiers from the association value, applying definitions if needed. */
    private static @NotNull Collection<ResourceAttribute<?>> getIdentifiersAttributes(
            PrismContainerValue<ShadowAssociationType> associationCVal, ResourceObjectDefinition entitlementDef)
            throws SchemaException {
        PrismContainer<?> container =
                MiscUtil.requireNonNull(
                        associationCVal.findContainer(ShadowAssociationType.F_IDENTIFIERS),
                        () -> "No identifiers in association value: " + associationCVal);
        if (container instanceof ResourceAttributeContainer resourceAttributeContainer) {
            return resourceAttributeContainer.getAttributes();
        }
        Collection<ResourceAttribute<?>> identifierAttributes = new ArrayList<>();
        for (Item<?, ?> rawIdentifierItem : container.getValue().getItems()) {
            //noinspection unchecked
            ResourceAttribute<Object> attribute =
                    ((ResourceAttributeDefinition<Object>)
                            entitlementDef.findAttributeDefinitionRequired(rawIdentifierItem.getElementName()))
                            .instantiate();
            for (Object val : rawIdentifierItem.getRealValues()) {
                attribute.addRealValue(val);
            }
            identifierAttributes.add(attribute);
        }
        return identifierAttributes;
    }

    /** Enriches current identification with a primary identifier. */
    public WithPrimary withPrimary(@NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier) {
        return new WithPrimary(resourceObjectDefinition, identifiers.withPrimary(primaryIdentifier));
    }

    /** See {@link ResourceObjectIdentifiers#of(Collection, Collection)} for preconditions. */
    public static @NotNull ResourceObjectIdentification<?> of(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<ResourceObjectIdentifier.Primary<?>> primaryIdentifiers,
            @NotNull Collection<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) throws SchemaException {
        return ResourceObjectIdentification.of(
                resourceObjectDefinition,
                ResourceObjectIdentifiers.of(primaryIdentifiers, secondaryIdentifiers));
    }

    private static @NotNull ResourceObjectIdentification<?> fromIdentifiersOrAttributes(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> allAttributes,
            boolean nonIdentifiersAllowed) throws SchemaException {
        Collection<ResourceObjectIdentifier.Primary<?>> primaryIdentifiers = new ArrayList<>();
        Collection<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers = new ArrayList<>();
        for (ResourceAttribute<?> attribute : allAttributes) {
            if (objectDefinition.isPrimaryIdentifier(attribute.getElementName())) {
                primaryIdentifiers.add(ResourceObjectIdentifier.Primary.of(attribute));
            } else if (objectDefinition.isSecondaryIdentifier(attribute.getElementName())) {
                secondaryIdentifiers.add(ResourceObjectIdentifier.Secondary.of(attribute));
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

    public static @NotNull ResourceObjectIdentification<?> fromIdentifiers(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> allIdentifiers) throws SchemaException {
        return fromIdentifiersOrAttributes(objectDefinition, allIdentifiers, false);
    }

    public static @NotNull ResourceObjectIdentification<?> fromAssociationValue(
            @NotNull ResourceObjectDefinition targetObjDef,
            @NotNull PrismContainerValue<ShadowAssociationType> associationValue)
            throws SchemaException {
        return fromIdentifiers(
                targetObjDef,
                getIdentifiersAttributes(associationValue, targetObjDef));
    }

    public static @NotNull ResourceObjectIdentification<?> fromAttributes(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ResourceAttribute<?>> attributes) {
        try {
            return fromIdentifiersOrAttributes(resourceObjectDefinition, attributes, true);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    /** Returns identification for a shadow. The shadow must have the primary identifier. */
    public static @NotNull ResourceObjectIdentification.WithPrimary fromCompleteShadow(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ShadowType shadow) {
        var identification = fromAttributes(resourceObjectDefinition, ShadowUtil.getAttributes(shadow));
        if (identification instanceof WithPrimary withPrimary) {
            return withPrimary;
        } else {
            throw new IllegalStateException("Shadow " + shadow + " does not have a primary identifier");
        }
    }

    /** Returns identification for a shadow. The shadow must have at least one identifier, not necessarily the primary one. */
    public static @NotNull ResourceObjectIdentification<?> fromIncompleteShadow(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ShadowType shadow) {
        return fromAttributes(resourceObjectDefinition, ShadowUtil.getAttributes(shadow));
    }

    public @NotNull I getIdentifiers() {
        return identifiers;
    }

    public @Nullable ResourceObjectIdentifier<?> getPrimaryIdentifier() {
        return identifiers.getPrimaryIdentifier();
    }

    public @NotNull Set<? extends ResourceObjectIdentifier<?>> getPrimaryIdentifiers() {
        var primaryIdentifier = getPrimaryIdentifier();
        return primaryIdentifier != null ? Set.of(primaryIdentifier) : Set.of();
    }

    public abstract @Nullable ResourceAttribute<?> getPrimaryIdentifierAttribute();

    public @NotNull Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiersAsAttributes() {
        return MiscUtil.asListExceptForNull(getPrimaryIdentifierAttribute());
    }

    public @NotNull Set<? extends ResourceObjectIdentifier<?>> getSecondaryIdentifiers() {
        return identifiers.getSecondaryIdentifiers();
    }

    public @NotNull Collection<? extends ResourceAttribute<?>> getSecondaryIdentifiersAsAttributes() {
        return ResourceObjectIdentifiers.asAttributes(identifiers.getSecondaryIdentifiers());
    }

    /** Returns all identifiers, both primary and secondary, as an unmodifiable collection. */
    public @NotNull Collection<? extends ResourceAttribute<?>> getAllIdentifiersAsAttributes() {
        return ResourceObjectIdentifiers.asAttributes(
                Sets.union(getPrimaryIdentifiers(), getSecondaryIdentifiers()));
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public boolean hasPrimaryIdentifier() {
        return identifiers instanceof ResourceObjectIdentifiers.WithPrimary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectIdentification<?> that = (ResourceObjectIdentification<?>) o;
        return resourceObjectDefinition.equals(that.resourceObjectDefinition)
                && identifiers.equals(that.identifiers);
    }

    /**
     * TODO Or should we compare only relevant parts of the definition? And compare identifiers in unordered way?
     */
    @Override
    public int hashCode() {
        return Objects.hash(resourceObjectDefinition, identifiers);
    }

    @Override
    public String toString() {
        return "ResourceObjectIdentification("
                + PrettyPrinter.prettyPrint(resourceObjectDefinition.getTypeName())
                + ": " + identifiers + ")";
    }

    @NotNull
    public ResourceObjectIdentityType asBean() throws SchemaException {
        ResourceObjectIdentityType bean = new ResourceObjectIdentityType();
        bean.setObjectClass(resourceObjectDefinition.getTypeName());
        bean.setPrimaryIdentifiers(getIdentifiersAsBean(getPrimaryIdentifiersAsAttributes()));
        bean.setSecondaryIdentifiers(getIdentifiersAsBean(getSecondaryIdentifiersAsAttributes()));
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

    public WithPrimary ensurePrimary() {
        if (this instanceof WithPrimary primary) {
            return primary;
        } else {
            throw new IllegalStateException("Expected primary identification, but got " + this);
        }
    }

    @Override
    public abstract ResourceObjectIdentification<I> clone();

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectDefinition", resourceObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "identifiers", identifiers, indent + 1);
        return sb.toString();
    }

    /** Identification that contains a primary identifier. Some methods are redeclared as returning a non-null value. */
    public static class WithPrimary extends ResourceObjectIdentification<ResourceObjectIdentifiers.WithPrimary> {

        private WithPrimary(
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull ResourceObjectIdentifiers.WithPrimary identifiers) {
            super(resourceObjectDefinition, identifiers);
        }

        @Override
        public @NotNull ResourceAttribute<?> getPrimaryIdentifierAttribute() {
            return identifiers.getPrimaryIdentifier().getAttribute();
        }

        @Override
        public @NotNull ResourceObjectIdentifier<?> getPrimaryIdentifier() {
            return Objects.requireNonNull(super.getPrimaryIdentifier());
        }

        @Override
        public WithPrimary clone() {
            return this;
        }
    }

    /** Identification that does not contain a primary identifier. */
    public static class SecondaryOnly extends ResourceObjectIdentification<ResourceObjectIdentifiers.SecondaryOnly> {

        private SecondaryOnly(
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull ResourceObjectIdentifiers.SecondaryOnly identifiers) {
            super(resourceObjectDefinition, identifiers);
        }

        @Override
        public @Nullable ResourceAttribute<?> getPrimaryIdentifierAttribute() {
            return null;
        }

        @Override
        public SecondaryOnly clone() {
            return this;
        }
    }
}
