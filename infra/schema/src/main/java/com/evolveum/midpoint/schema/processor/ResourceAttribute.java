/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;

import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

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
public interface ResourceAttribute<T> extends PrismProperty<T> {

    /** Converts the {@link PrismProperty} into {@link ResourceAttribute}, if needed. */
    static <T> ResourceAttribute<T> of(@NotNull Item<?, ?> item) {
        if (item instanceof ResourceAttribute<?> resourceAttribute) {
            //noinspection unchecked
            return (ResourceAttribute<T>) resourceAttribute;
        } else if (item instanceof PrismProperty<?> property) {
            ResourceAttributeImpl<T> attr = new ResourceAttributeImpl<>(item.getElementName(), null);
            for (PrismPropertyValue<?> value : property.getValues()) {
                //noinspection unchecked
                attr.addValue((PrismPropertyValue<T>) value, true);
            }
            return attr;
        } else {
            throw new IllegalArgumentException("Not a property: " + item);
        }
    }

    // TODO remove
//    static Collection<ResourceAttribute<?>> collectionOf(ShadowAttributesType bean) {
//        if (bean != null) {
//            List<ResourceAttribute<?>> list = new ArrayList<>();
//            //noinspection unchecked
//            for (Item<?, ?> item : ((PrismContainerValue<ShadowAttributesType>) bean.asPrismContainerValue()).getItems()) {
//                list.add(ResourceAttribute.of(item));
//            }
//            return list;
//        } else {
//            return List.of();
//        }
//    }

    ResourceAttributeDefinition<T> getDefinition();

    default @NotNull ResourceAttributeDefinition<T> getDefinitionRequired() {
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

    /** Returns self to be usable in chained calls. */
    default @NotNull ResourceAttribute<T> applyDefinitionFrom(@NotNull ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        var attrDef = objectDefinition.findAttributeDefinitionRequired(getElementName());
        //noinspection unchecked
        applyDefinition((ResourceAttributeDefinition<T>) attrDef);
        return this;
    }

    @Override
    ResourceAttribute<T> clone();

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

    /** There must be no duplicates or nulls among the real values. {@link RawType} values are tried to be converted. */
    void addNormalizedValues(@NotNull Collection<?> realValues, @NotNull ResourceAttributeDefinition<T> newDefinition)
            throws SchemaException;

    default @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getElementName());
    }

    /**
     * Creates "eq" filter for the current value of this attribute. It must have a definition and exactly one value.
     * Matching rule is not specified. We assume this filter will be evaluated on the resource.
     */
    default @NotNull ObjectFilter plainEqFilter() {
        ResourceAttributeDefinition<T> def = getDefinitionRequired();
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
    default <N> @NotNull ObjectFilter normalizationAwareEqFilter() throws SchemaException {
        var normAwareDef = getDefinitionRequired().toNormalizationAware();
        var normAwareRealValue = MiscUtil.extractSingletonRequired(normAwareDef.adoptRealValues(getRealValues()));
        return PrismContext.get().queryFor(ShadowType.class)
                .item(getStandardPath(), normAwareDef)
                .eq(normAwareRealValue)
                .matching(normAwareDef.getMatchingRuleQName())
                .buildFilter();
    }

    /** Creates normalization-aware version of this attribute: one that is suitable to be used in the repository. */
    default <N> @NotNull PrismProperty<N> normalizationAwareVersion() throws SchemaException {
        return getDefinitionRequired()
                .<N>toNormalizationAware()
                .adoptRealValuesAndInstantiate(getRealValues());
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
        ResourceAttributeDefinition<Object> expectedDefinition;
        try {
            expectedDefinition = objectDefinition.findAttributeDefinitionRequired(getElementName());
        } catch (SchemaException e) {
            throw new IllegalStateException(e);
        }

        var actualDefinition = getDefinition();
        stateCheck(
                expectedDefinition.equals(actualDefinition),
                "Definition of %s is %s, expected %s",
                this, actualDefinition, expectedDefinition);
    }
}
