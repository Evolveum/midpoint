/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Specialized class that wraps a single-valued non-null primary or secondary identifier attribute.
 *
 * The intended use is in method parameters and internal data structures where it ensures the requirements on
 * the identifiers being passed on or stored.
 */
public abstract class ResourceObjectIdentifier<T> implements Serializable, ShortDumpable {

    /** The identifier attribute. It must have a definition and exactly one value. Immutable. */
    @NotNull final ResourceAttribute<T> attribute;

    private ResourceObjectIdentifier(@NotNull ResourceAttribute<T> attribute) {
        Preconditions.checkArgument(
                attribute.getRealValue() != null,
                "Expected exactly one non-null value in %s", attribute);
        Preconditions.checkArgument(
                attribute.getDefinition() != null,
                "Expected the definition for attribute %s", attribute);
        ResourceAttribute<T> clone = attribute.clone();
        clone.freeze();
        this.attribute = clone;
    }

    public @NotNull ResourceAttribute<T> getAttribute() {
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

    private static <T> @NotNull ResourceAttribute<T> toSingleValuedResourceAttribute(
            @NotNull ResourceObjectDefinition objDef, @NotNull PrismProperty<T> item) throws SchemaException {
        //noinspection unchecked
        var primaryIdentifier = ((ResourceAttributeDefinition<T>) objDef
                .findAttributeDefinitionRequired(item.getElementName()))
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

    public @NotNull ResourceAttributeDefinition<T> getDefinition() {
        return Objects.requireNonNull(
                attribute.getDefinition());
    }

    public List<PrismPropertyValue<T>> getNormalizedValues()
            throws SchemaException {
        MatchingRule<T> matchingRule = getDefinition().getMatchingRule();
        List<PrismPropertyValue<T>> normalizedAttributeValues = new ArrayList<>();
        for (PrismPropertyValue<T> origAttributeValue : attribute.getValues()) {
            PrismPropertyValue<T> normalizedAttributeValue = origAttributeValue.clone();
            normalizedAttributeValue.setValue(
                    matchingRule.normalize(
                            origAttributeValue.getValue()));
            normalizedAttributeValues.add(normalizedAttributeValue);
        }
        return normalizedAttributeValues;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this);
    }

    /** Identifier that is a primary one. */
    public static class Primary<T> extends ResourceObjectIdentifier<T> {

        private Primary(@NotNull ResourceAttribute<T> attribute) {
            super(attribute);
        }

        public static <T> @NotNull Primary<T> of(@NotNull ResourceAttribute<T> attribute) {
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
    }

    public static class Secondary<T> extends ResourceObjectIdentifier<T> {

        public Secondary(@NotNull ResourceAttribute<T> attribute) {
            super(attribute);
        }

        public static <T> @NotNull Secondary<T> of(@NotNull ResourceAttribute<T> attribute) {
            return new Secondary<>(attribute);
        }

        public static @NotNull List<? extends Secondary<?>> of (@NotNull Collection<? extends ResourceAttribute<?>> attributes) {
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
    }
}
