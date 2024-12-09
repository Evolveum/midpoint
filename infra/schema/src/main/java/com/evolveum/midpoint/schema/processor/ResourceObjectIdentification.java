/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

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

    public static @NotNull ResourceObjectIdentification<?> of(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ShadowSimpleAttribute<?>> identifierAttributes) throws SchemaException {
        return of(resourceObjectDefinition,
                ResourceObjectIdentifiers.of(resourceObjectDefinition, identifierAttributes));
    }

    public static @NotNull ResourceObjectIdentification.WithPrimary of(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ResourceObjectIdentifiers.WithPrimary primaryIdentifiers) {
        return new WithPrimary(resourceObjectDefinition, primaryIdentifiers);
    }

    /** Creates new identification with a primary identifier. */
    public static WithPrimary withPrimary(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ShadowSimpleAttribute<?> primaryIdentifierAttribute,
            @NotNull Collection<? extends ShadowSimpleAttribute<?>> secondaryIdentifierAttributes) {
        return new WithPrimary(
                resourceObjectDefinition,
                ResourceObjectIdentifiers.withPrimary(
                        ResourceObjectIdentifier.Primary.of(primaryIdentifierAttribute),
                        ResourceObjectIdentifier.Secondary.of(secondaryIdentifierAttributes)));
    }

    /** Enriches current identification with a primary identifier. */
    public WithPrimary withPrimaryAdded(@NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier) {
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
            @NotNull Collection<? extends ShadowAttribute<?, ?, ?, ?>> allAttributes,
            boolean nonIdentifiersAllowed) throws SchemaException {
        Collection<ResourceObjectIdentifier.Primary<?>> primaryIdentifiers = new ArrayList<>();
        Collection<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers = new ArrayList<>();
        for (var attribute : allAttributes) {
            var isSimple = attribute instanceof ShadowSimpleAttribute<?>;
            if (isSimple && objectDefinition.isPrimaryIdentifier(attribute.getElementName())) {
                primaryIdentifiers.add(ResourceObjectIdentifier.Primary.of((ShadowSimpleAttribute<?>) attribute));
            } else if (isSimple && objectDefinition.isSecondaryIdentifier(attribute.getElementName())) {
                secondaryIdentifiers.add(ResourceObjectIdentifier.Secondary.of((ShadowSimpleAttribute<?>) attribute));
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
            @NotNull Collection<? extends ShadowSimpleAttribute<?>> allIdentifiers) throws SchemaException {
        return fromIdentifiersOrAttributes(objectDefinition, allIdentifiers, false);
    }

    public static @NotNull ResourceObjectIdentification<?> fromAttributes(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<? extends ShadowAttribute<?, ?, ?, ?>> attributes) {
        try {
            return fromIdentifiersOrAttributes(resourceObjectDefinition, attributes, true);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    /** Returns identification for a shadow. The shadow must have the primary identifier and correct definition. */
    public static @NotNull ResourceObjectIdentification.WithPrimary fromCompleteShadow(@NotNull ShadowType shadow) {
        return fromCompleteShadow(
                ShadowUtil.getResourceObjectDefinition(shadow),
                shadow);
    }

    /** Returns identification for a shadow. The shadow must have the primary identifier. */
    public static @NotNull ResourceObjectIdentification.WithPrimary fromCompleteShadow(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ShadowType shadow) {
        var identification = fromAttributes(resourceObjectDefinition, ShadowUtil.getSimpleAttributes(shadow));
        if (identification instanceof WithPrimary withPrimary) {
            return withPrimary;
        } else {
            throw new IllegalStateException("Shadow " + shadow + " does not have a primary identifier");
        }
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

    public abstract @Nullable ShadowSimpleAttribute<?> getPrimaryIdentifierAttribute();

    public @NotNull Collection<? extends ShadowSimpleAttribute<?>> getPrimaryIdentifiersAsAttributes() {
        return MiscUtil.asListExceptForNull(getPrimaryIdentifierAttribute());
    }

    public @NotNull Set<? extends ResourceObjectIdentifier<?>> getSecondaryIdentifiers() {
        return identifiers.getSecondaryIdentifiers();
    }

    public @NotNull Collection<? extends ShadowSimpleAttribute<?>> getSecondaryIdentifiersAsAttributes() {
        return ResourceObjectIdentifiers.asAttributes(identifiers.getSecondaryIdentifiers());
    }

    /** Returns all identifiers, both primary and secondary, as an unmodifiable collection. */
    public @NotNull Collection<? extends ShadowSimpleAttribute<?>> getAllIdentifiersAsAttributes() {
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

    private ResourceObjectIdentifiersType getIdentifiersAsBean(Collection<? extends ShadowSimpleAttribute<?>> identifiers)
            throws SchemaException {
        if (identifiers.isEmpty()) {
            return null;
        }
        ResourceObjectIdentifiersType identifiersBean = new ResourceObjectIdentifiersType();
        for (ShadowSimpleAttribute<?> identifier : identifiers) {
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
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectDefinition", String.valueOf(resourceObjectDefinition), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "identifiers", identifiers, indent + 1);
        return sb.toString();
    }

    public @NotNull QName getObjectClassName() {
        return resourceObjectDefinition.getObjectClassName();
    }

    /** Identification that contains a primary identifier. Some methods are redeclared as returning a non-null value. */
    public static class WithPrimary extends ResourceObjectIdentification<ResourceObjectIdentifiers.WithPrimary> {

        private WithPrimary(
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull ResourceObjectIdentifiers.WithPrimary identifiers) {
            super(resourceObjectDefinition, identifiers);
        }

        @Override
        public @NotNull ShadowSimpleAttribute<?> getPrimaryIdentifierAttribute() {
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
        public @Nullable ShadowSimpleAttribute<?> getPrimaryIdentifierAttribute() {
            return null;
        }

        @Override
        public SecondaryOnly clone() {
            return this;
        }
    }
}
