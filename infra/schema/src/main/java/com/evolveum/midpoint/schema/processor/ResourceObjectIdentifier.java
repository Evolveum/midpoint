/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * Specialized class that wraps a single-valued non-null primary or secondary identifier attribute.
 *
 * The intended use is in method parameters and internal data structures where it ensures the requirements on
 * the identifiers being passed on or stored.
 */
public abstract class ResourceObjectIdentifier<T> implements Serializable, ShortDumpable {

    /** The identifier attribute. It must have a definition and exactly one value. Immutable. */
    @NotNull final ShadowSimpleAttribute<T> attribute;

    private ResourceObjectIdentifier(@NotNull ShadowSimpleAttribute<T> attribute) {
        Preconditions.checkArgument(
                attribute.getRealValue() != null,
                "Expected exactly one non-null value in %s", attribute);
        Preconditions.checkArgument(
                attribute.getDefinition() != null,
                "Expected the definition for attribute %s", attribute);
        ShadowSimpleAttribute<T> clone = attribute.clone();
        clone.freeze();
        this.attribute = clone;
    }

    public static @NotNull ResourceObjectIdentifier.Primary<?> primaryFromIdentifiers(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<ShadowSimpleAttribute<?>> identifiers,
            Object errorCtx)
            throws SchemaException {
        var primaryIdentifierAttributes = identifiers.stream()
                .filter(attr -> objectDefinition.isPrimaryIdentifier(attr.getElementName()))
                .toList();
        ShadowSimpleAttribute<?> primaryIdentifierAttribute = MiscUtil.extractSingletonRequired(
                primaryIdentifierAttributes,
                () -> new SchemaException("Multiple primary identifiers among " + identifiers + " in " + errorCtx),
                () -> new SchemaException("No primary identifier in " + errorCtx));
        return ResourceObjectIdentifier.Primary.of(primaryIdentifierAttribute);
    }

    public @NotNull ShadowSimpleAttribute<T> getAttribute() {
        return attribute;
    }

    public @NotNull T getRealValue() {
        return Objects.requireNonNull(attribute.getRealValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectIdentifier<?> that = (ResourceObjectIdentifier<?>) o;
        return Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute);
    }

    public @NotNull ItemName getName() {
        return Objects.requireNonNull(
                attribute.getElementName());
    }

    private static <T> @NotNull ShadowSimpleAttribute<T> toSingleValuedResourceAttribute(
            @NotNull ResourceObjectDefinition objDef, @NotNull PrismProperty<T> item) throws SchemaException {
        //noinspection unchecked
        var primaryIdentifier = ((ShadowSimpleAttributeDefinition<T>) objDef
                .findSimpleAttributeDefinitionRequired(item.getElementName()))
                .instantiate();
        primaryIdentifier.setRealValue(
                MiscUtil.requireNonNull(
                        item.getRealValue(), () -> "No real value in " + item));
        return primaryIdentifier;
    }

    public @NotNull PrismPropertyValue<T> getValue() {
        return Objects.requireNonNull(
                attribute.getValue());
    }

    public ItemPath getSearchPath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getName());
    }

    public @NotNull ShadowSimpleAttributeDefinition<T> getDefinition() {
        return Objects.requireNonNull(
                attribute.getDefinition());
    }

    public @NotNull Object getOrigValue() {
        return MiscUtil.extractSingletonRequired(attribute.getOrigValues());
    }

    /**
     * This may be quite courageous. But (especially) ConnId requires String values of identifiers. So, this is the place
     * to change the way non-String values are to be handled.
     */
    public @NotNull String getStringOrigValue() {
        return getOrigValue().toString();
    }

    public @NotNull Object getNormValue() {
        try {
            return MiscUtil.extractSingletonRequired(attribute.getNormValues());
        } catch (SchemaException e) {
            throw new SystemException(e); // Should have been checked earlier
        }
    }

    public QName getMatchingRuleName() {
        return attribute.getDefinitionRequired().getMatchingRuleQName();
    }

    /** See {@link ShadowSimpleAttribute#normalizationAwareEqFilter()}. */
    public @NotNull ObjectFilter normalizationAwareEqFilter() throws SchemaException {
        return attribute.normalizationAwareEqFilter();
    }

    public @NotNull ObjectFilter plainEqFilter() {
        return attribute.plainEqFilter();
    }

    /** Identifier that is a primary one. */
    public static class Primary<T> extends ResourceObjectIdentifier<T> {

        private Primary(@NotNull ShadowSimpleAttribute<T> attribute) {
            super(attribute);
        }

        public static <T> @NotNull Primary<T> of(@NotNull ShadowSimpleAttribute<T> attribute) {
            return new Primary<>(attribute);
        }

        /** Item must correspond to a primary identifier and have exactly one real value. */
        public static <T> Primary<T> of(@NotNull ResourceObjectDefinition objDef, @NotNull PrismProperty<T> item)
                throws SchemaException {
            ItemName attrName = item.getElementName();
            Preconditions.checkArgument(
                    objDef.isPrimaryIdentifier(attrName),
                    "'%s' is not a primary identifier in %s", attrName, objDef);
            return new Primary<>(
                    toSingleValuedResourceAttribute(objDef, item));
        }

        @Override
        public String toString() {
            return "Primary identifier: " + attribute;
        }

        @Override
        public void shortDump(StringBuilder sb) {
            sb.append(attribute.shortDump());
            sb.append(" (primary identifier)");
        }
    }

    public static class Secondary<T> extends ResourceObjectIdentifier<T> {

        public Secondary(@NotNull ShadowSimpleAttribute<T> attribute) {
            super(attribute);
        }

        public static <T> @NotNull Secondary<T> of(@NotNull ShadowSimpleAttribute<T> attribute) {
            return new Secondary<>(attribute);
        }

        public static @NotNull List<? extends Secondary<?>> of (@NotNull Collection<? extends ShadowSimpleAttribute<?>> attributes) {
            return attributes.stream()
                    .map(attr -> Secondary.of(attr))
                    .toList();
        }

        /** Item must correspond to a secondary identifier and have exactly one real value. */
        public static <T> Secondary<T> of(@NotNull ResourceObjectDefinition objDef, @NotNull PrismProperty<T> item)
                throws SchemaException {
            ItemName attrName = item.getElementName();
            Preconditions.checkArgument(
                    objDef.isSecondaryIdentifier(attrName),
                    "'%s' is not a secondary identifier in %s", attrName, objDef);
            return new Secondary<>(
                    toSingleValuedResourceAttribute(objDef, item));
        }

        @Override
        public String toString() {
            return "Secondary identifier: " + attribute;
        }

        @Override
        public void shortDump(StringBuilder sb) {
            sb.append(attribute.shortDump());
            sb.append(" (secondary identifier)");
        }
    }
}
