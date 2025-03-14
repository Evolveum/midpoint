/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * TODO update this doc
 *
 * Resource Object Attribute is a Property of Resource Object. All that applies
 * to property applies also to attribute, e.g. only a whole attributes can be
 * changed, they may be simple or complex types, they should be representable in
 * XML, etc. In addition, attribute definition may have some annotations that
 * suggest its purpose and use on the Resource.
 *
 * Resource Object Attribute understands resource-specific annotations such as
 * native attribute name.
 *
 * Resource Object Attribute is mutable.
 *
 * @author Radovan Semancik
 */
public interface ShadowSimpleAttribute<T>
        extends
        PrismProperty<T>,
        ShadowAttribute<
                PrismPropertyValue<T>,
                ShadowSimpleAttributeDefinition<T>,
                T,
                ShadowSimpleAttribute<T>
                > {

    /** Converts the {@link PrismProperty} into {@link ShadowSimpleAttribute}, if needed. */
    static <T> ShadowSimpleAttribute<T> of(@NotNull Item<?, ?> item) {
        if (item instanceof ShadowSimpleAttribute<?> simpleAttribute) {
            //noinspection unchecked
            return (ShadowSimpleAttribute<T>) simpleAttribute;
        } else if (item instanceof PrismProperty<?> property) {
            ShadowSimpleAttributeImpl<T> attr = new ShadowSimpleAttributeImpl<>(item.getElementName(), null);
            for (PrismPropertyValue<?> value : property.getValues()) {
                //noinspection unchecked
                attr.addValue((PrismPropertyValue<T>) value, true);
            }
            return attr;
        } else {
            throw new IllegalArgumentException("Not a property: " + item);
        }
    }

    ShadowSimpleAttributeDefinition<T> getDefinition();

    default @NotNull ShadowSimpleAttributeDefinition<T> getDefinitionRequired() {
        return MiscUtil.stateNonNull(
                getDefinition(),
                "No definition in %s", this);
    }

    /**
     * Returns native attribute name.
     *
     * Native name of the attribute is a name as it is used on the resource or
     * as seen by the connector. It is used for diagnostics purposes and may be
     * used by the connector itself. As the attribute names in XSD have to
     * comply with XML element name limitations, this may be the only way how to
     * determine original attribute name.
     *
     * Returns null if native attribute name is not set or unknown.
     *
     * The name should be the same as the one used by the resource, if the
     * resource supports naming of attributes. E.g. in case of LDAP this
     * annotation should contain "cn", "givenName", etc. If the resource is not
     * that flexible, the native attribute names may be hardcoded (e.g.
     * "username", "homeDirectory") or may not be present at all.
     *
     * @return native attribute name
     */
    default String getNativeAttributeName() {
        var definition = getDefinition();
        return definition != null ? definition.getNativeAttributeName() : null;
    }

    default void applyDefinitionFrom(@NotNull ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        var attrDef = objectDefinition.findSimpleAttributeDefinitionRequired(getElementName());
        if (attrDef != getDefinition()) { // Maybe equals would be better?
            //noinspection unchecked
            applyDefinition((ShadowSimpleAttributeDefinition<T>) attrDef);
        }
    }

    @Override
    ShadowSimpleAttribute<T> clone();

    /** Returns the original real values. Assumes the definition is present. */
    @NotNull Collection<?> getOrigValues();

    /** Returns the normalized real values. Assumes the definition is present. */
    @NotNull Collection<?> getNormValues() throws SchemaException;

    /**
     * Returns true if the attribute is a PolyString-typed one - either native, or "simulated" (with a normalizer).
     * Assumes the definition is present.
     */
    default boolean isPolyString() {
        return QNameUtil.match(
                getDefinitionRequired().getTypeName(),
                PolyStringType.COMPLEX_TYPE);
    }

    default @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getElementName());
    }

    /**
     * Creates "eq" filter for the current value of this attribute. It must have a definition and exactly one value.
     * Matching rule is not specified. We assume this filter will be evaluated on the resource.
     */
    default @NotNull ObjectFilter plainEqFilter() {
        ShadowSimpleAttributeDefinition<T> def = getDefinitionRequired();
        var realValue = MiscUtil.argNonNull(getRealValue(), "no value of %s", this);
        return PrismContext.get().queryFor(ShadowType.class)
                .item(getStandardPath(), def)
                .eq(realValue)
                .buildFilter();
    }

    /**
     * Creates normalization-aware "eq" filter (i.e., suitable for the execution against the repository) for the current
     * value of this attribute. It must have a definition and exactly one value.
     */
    default @NotNull ObjectFilter normalizationAwareEqFilter() throws SchemaException {
        var normAwareDef = getDefinitionRequired().toNormalizationAware();
        var normAwareRealValue = MiscUtil.extractSingletonRequired(normAwareDef.adoptRealValues(getRealValues()));
        return PrismContext.get().queryFor(ShadowType.class)
                .item(getStandardPath(), normAwareDef)
                .eq(normAwareRealValue)
                .matching(normAwareDef.getMatchingRuleQName())
                .buildFilter();
    }

    /** Creates a delta that would enforce (via REPLACE operation) the values of this attribute. */
    default @NotNull PropertyDelta<T> createReplaceDelta() {
        var delta = getDefinitionRequired().createEmptyDelta();
        delta.setValuesToReplace(
                CloneUtil.cloneCollectionMembers(getValues()));
        return delta;
    }

    /** TODO decide on this. */
    default void checkDefinitionConsistence(@NotNull ResourceObjectDefinition objectDefinition) {
        ShadowSimpleAttributeDefinition<Object> expectedDefinition;
        try {
            expectedDefinition = objectDefinition.findSimpleAttributeDefinitionRequired(getElementName());
        } catch (SchemaException e) {
            throw new IllegalStateException(e);
        }

        var actualDefinition = getDefinition();
        stateCheck(
                expectedDefinition.equals(actualDefinition),
                "Definition of %s is %s, expected %s",
                this, actualDefinition, expectedDefinition);
    }

    default List<PrismPropertyValue<T>> getAttributeValues() {
        return getValues();
    }

    default boolean hasNoValues() {
        return PrismProperty.super.hasNoValues();
    }

    @Override
    PropertyDelta<T> createDelta();

    @Override
    PropertyDelta<T> createDelta(ItemPath path);

    @Override
    ShadowSimpleAttribute<T> createImmutableClone();

    @Override
    @NotNull ShadowSimpleAttribute<T> cloneComplex(@NotNull CloneStrategy strategy);
}
