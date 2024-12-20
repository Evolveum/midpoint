/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Primary and/or secondary identifiers of a resource object.
 * At least one identifier must be present.
 *
 * (In the future we may relax this restriction.)
 *
 * Basically a categorized set of {@link ResourceObjectIdentifier} instances.
 *
 * The difference to (older) {@link ResourceObjectIdentification} is that this class does not contain the object definition.
 *
 * @see ResourceObjectIdentifier
 * @see ResourceObjectIdentification
 */
public abstract class ResourceObjectIdentifiers implements Serializable, DebugDumpable, ShortDumpable, Cloneable {

    @Serial private static final long serialVersionUID = 1L;

    /** Unmodifiable. Not empty for {@link SecondaryOnly} instances. */
    @NotNull private final Set<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers;

    ResourceObjectIdentifiers(@NotNull Collection<? extends ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) {
        this.secondaryIdentifiers = Set.copyOf(secondaryIdentifiers);
    }

    /** Precondition: At least one identifier must be present. */
    public static @NotNull ResourceObjectIdentifiers of(
            @Nullable ResourceObjectIdentifier.Primary<?> primaryIdentifier,
            @NotNull Collection<? extends ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) {
        if (primaryIdentifier != null) {
            return withPrimary(primaryIdentifier, secondaryIdentifiers);
        } else {
            return new SecondaryOnly(secondaryIdentifiers);
        }
    }

    public static @NotNull WithPrimary withPrimary(
            @NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier,
            @NotNull Collection<? extends ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) {
        return new WithPrimary(primaryIdentifier, secondaryIdentifiers);
    }

    /** Precondition: At least one identifier must be present, at most one primary identifier must be present. */
    public static @NotNull ResourceObjectIdentifiers of(
            @NotNull Collection<? extends ResourceObjectIdentifier.Primary<?>> primaryIdentifiers,
            @NotNull Collection<? extends ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) throws SchemaException {
        return of(
                MiscUtil.extractSingleton(
                        primaryIdentifiers,
                        () -> new SchemaException("Multiple primary identifiers: " + primaryIdentifiers)),
                secondaryIdentifiers);
    }

    /**
     * Creates identifiers from a shadow. The shadow may or may not have a primary identifier.
     * The only requirements is that it has any identifier, and at most one primary one.
     */
    public static @NotNull ResourceObjectIdentifiers of(
            @NotNull ResourceObjectDefinition objDef, @NotNull ShadowType repoShadow)
            throws SchemaException {
        var optional = optionalOf(objDef, repoShadow);
        MiscUtil.argCheck(optional.isPresent(), "No identifiers in %s", repoShadow);
        return optional.get();
    }

    /** Creates identifiers from a shadow, if possible. The shadow must have schema applied. */
    public static @NotNull Optional<ResourceObjectIdentifiers> optionalOf(@NotNull ShadowType bean)
            throws SchemaException {
        return optionalOf(ShadowUtil.getResourceObjectDefinition(bean), bean);
    }

    /** Creates identifiers from a shadow, if possible. */
    public static @NotNull Optional<ResourceObjectIdentifiers> optionalOf(
            @NotNull ResourceObjectDefinition objDef, @NotNull ShadowType repoShadow)
            throws SchemaException {

        PrismContainer<?> attributesContainer =
                repoShadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            return Optional.empty();
        }

        Collection<ResourceObjectIdentifier.Primary<?>> primaryIdentifiers = new ArrayList<>();
        Collection<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers = new ArrayList<>();
        for (Item<?, ?> item: attributesContainer.getValue().getItems()) {
            ItemName itemName = item.getElementName();
            if (objDef.isPrimaryIdentifier(itemName)) {
                primaryIdentifiers.add(
                        ResourceObjectIdentifier.Primary.of(objDef, (PrismProperty<?>) item));
            } else if (objDef.isSecondaryIdentifier(itemName)) {
                secondaryIdentifiers.add(
                        ResourceObjectIdentifier.Secondary.of(objDef, (PrismProperty<?>) item));
            }
        }
        if (primaryIdentifiers.isEmpty() && secondaryIdentifiers.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(
                    ResourceObjectIdentifiers.of(primaryIdentifiers, secondaryIdentifiers));
        }
    }

    /** Creates identifiers from a collection of identifying attributes. */
    public static @NotNull ResourceObjectIdentifiers of(
            @NotNull ResourceObjectDefinition objDef, @NotNull Collection<? extends ShadowSimpleAttribute<?>> attributes)
            throws SchemaException {
        Collection<ResourceObjectIdentifier.Primary<?>> primaryIdentifiers = new ArrayList<>();
        Collection<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers = new ArrayList<>();
        for (var attribute : attributes) {
            ItemName itemName = attribute.getElementName();
            if (objDef.isPrimaryIdentifier(itemName)) {
                primaryIdentifiers.add(
                        ResourceObjectIdentifier.Primary.of(objDef, (PrismProperty<?>) attribute));
            } else if (objDef.isSecondaryIdentifier(itemName)) {
                secondaryIdentifiers.add(
                        ResourceObjectIdentifier.Secondary.of(objDef, (PrismProperty<?>) attribute));
            } else {
                throw new IllegalArgumentException(
                        "Attribute %s is not an identifier in %s".formatted(attribute, objDef));
            }
        }

        return of(primaryIdentifiers, secondaryIdentifiers);
    }

    public static @NotNull Collection<? extends ShadowSimpleAttribute<?>> asAttributes(
            @NotNull Collection<? extends ResourceObjectIdentifier<?>> identifiers) {
        return identifiers.stream()
                .map(i -> i.attribute)
                .toList();
    }

    public abstract @Nullable ResourceObjectIdentifier.Primary<?> getPrimaryIdentifier();

    public @NotNull ResourceObjectIdentifier.Primary<?> getPrimaryIdentifierRequired() {
        return MiscUtil.requireNonNull(
                getPrimaryIdentifier(),
                () -> new IllegalStateException("No primary identifier in " + this));
    }

    /** Not empty for {@link SecondaryOnly} instances. */
    public @NotNull Set<ResourceObjectIdentifier.Secondary<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiers;
    }

    WithPrimary withPrimary(@NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier) {
        return new WithPrimary(primaryIdentifier, secondaryIdentifiers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectIdentifiers that = (ResourceObjectIdentifiers) o;
        return Objects.equals(secondaryIdentifiers, that.secondaryIdentifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secondaryIdentifiers);
    }

    @Override
    public abstract ResourceObjectIdentifiers clone();

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "primary identifier", String.valueOf(getPrimaryIdentifier()), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "secondary identifiers", secondaryIdentifiers, indent + 1);
        return sb.toString();
    }

    /** The primary identifier (if present) goes first. */
    public @NotNull List<ResourceObjectIdentifier<?>> getAllIdentifiers() {
        return Stream.concat(
                        Stream.ofNullable(getPrimaryIdentifier()),
                        secondaryIdentifiers.stream())
                .toList();
    }

    @Override
    public String toString() {
        return "Resource object identifiers: " + getAllIdentifiers();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(
                getAllIdentifiers().stream()
                        .map(ResourceObjectIdentifier::shortDump)
                        .collect(Collectors.joining(", ")));
    }

    /** Identifiers that contain a primary identifier. */
    public static class WithPrimary extends ResourceObjectIdentifiers {

        @NotNull private final ResourceObjectIdentifier.Primary<?> primaryIdentifier;

        private WithPrimary(
                @NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier,
                @NotNull Collection<? extends ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) {
            super(secondaryIdentifiers);
            this.primaryIdentifier = primaryIdentifier;
        }

        @Override
        public @NotNull ResourceObjectIdentifier.Primary<?> getPrimaryIdentifier() {
            return primaryIdentifier;
        }

        @Override
        public WithPrimary clone() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            WithPrimary that = (WithPrimary) o;
            return Objects.equals(primaryIdentifier, that.primaryIdentifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), primaryIdentifier);
        }
    }

    /** Secondary-only identifiers. At least one must be present! */
    public static class SecondaryOnly extends ResourceObjectIdentifiers {

        private SecondaryOnly(@NotNull Collection<? extends ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers) {
            super(secondaryIdentifiers);
            Preconditions.checkArgument(!secondaryIdentifiers.isEmpty(), "No identifiers");
        }

        @Override
        public @Nullable ResourceObjectIdentifier.Primary<?> getPrimaryIdentifier() {
            return null;
        }

        @Override
        public SecondaryOnly clone() {
            return this;
        }
    }
}
