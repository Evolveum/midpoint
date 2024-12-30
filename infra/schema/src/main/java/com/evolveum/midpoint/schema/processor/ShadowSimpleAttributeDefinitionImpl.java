/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.util.CloneUtil.toImmutable;

import java.util.Collection;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.delta.PropertyDeltaImpl;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * An attribute definition (obtained typically from the connector),
 * optionally refined by information from `schemaHandling` section of a resource definition.
 *
 * The implementation consists of a pair of {@link #nativeDefinition} and {@link #customizationBean},
 * plus some auxiliary information for faster access.
 *
 * This class intentionally does NOT inherit from {@link PrismPropertyDefinitionImpl}. Instead, a large part of the required
 * functionality is delegated to {@link #nativeDefinition} which provides analogous functionality.
 *
 * @see NativeShadowSimpleAttributeDefinition
 */
public class ShadowSimpleAttributeDefinitionImpl<T>
        extends ShadowAttributeDefinitionImpl<
        PrismPropertyValue<T>, ShadowSimpleAttributeDefinition<T>, T, ShadowSimpleAttribute<T>, NativeShadowSimpleAttributeDefinition<T>>
        implements ShadowSimpleAttributeDefinition<T> {

    private ShadowSimpleAttributeDefinitionImpl(
            @NotNull NativeShadowSimpleAttributeDefinition<T> nativeDefinition,
            @NotNull ResourceAttributeDefinitionType customizationBean,
            boolean ignored) throws ConfigurationException {
        super(nativeDefinition, customizationBean, ignored);
    }

    private ShadowSimpleAttributeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull NativeShadowSimpleAttributeDefinition<T> nativeDefinition,
            @NotNull ResourceItemDefinitionType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride) {
        super(layer, nativeDefinition, customizationBean, limitationsMap, accessOverride);
    }

    @Override
    ShadowSimpleAttribute<T> instantiateFromQualifiedName(QName name) {
        return new ShadowSimpleAttributeImpl<>(name, this);
    }

    /**
     * This is the main creation point.
     *
     * @throws ConfigurationException If there's a problem with the customization bean.
     */
    public static <T> ShadowSimpleAttributeDefinition<T> create(
            @NotNull NativeShadowSimpleAttributeDefinition<T> nativeDefinition,
            @Nullable ResourceAttributeDefinitionType customizationBean,
            boolean ignored)
            throws ConfigurationException {

        return new ShadowSimpleAttributeDefinitionImpl<>(
                toImmutable(nativeDefinition),
                toImmutable(customizationBean != null ?
                        customizationBean : new ResourceAttributeDefinitionType()),
                ignored);
    }

    /** This is the creation point from native form only. */
    public static <T> ShadowSimpleAttributeDefinition<T> create(
            @NotNull NativeShadowSimpleAttributeDefinition<T> nativeDefinition) throws ConfigurationException {
        return create(nativeDefinition, null, false);
    }

    public @NotNull ShadowSimpleAttributeDefinitionImpl<T> forLayer(@NotNull LayerType layer) {
        if (layer == currentLayer) {
            return this;
        } else {
            return new ShadowSimpleAttributeDefinitionImpl<>(
                    layer,
                    nativeDefinition,
                    customizationBean,
                    limitationsMap,
                    accessOverride.clone() // TODO do we want to preserve also the access override?
            );
        }
    }

    @NotNull
    @Override
    public ShadowSimpleAttributeDefinitionImpl<T> clone() {
        return new ShadowSimpleAttributeDefinitionImpl<>(
                currentLayer,
                nativeDefinition,
                customizationBean,
                limitationsMap,
                accessOverride.clone());
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return customizationBean.isSecondaryIdentifier();
    }

    @Override
    public QName getMatchingRuleQName() {
        return MiscUtil.orElseGet(
                customizationBean.getMatchingRule(),
                nativeDefinition::getMatchingRuleQName);
    }

    @Override
    public @NotNull MatchingRule<T> getMatchingRule() {
        return MatchingRuleRegistryImpl.instance()
                .getMatchingRuleSafe(getMatchingRuleQName(), getTypeName());
    }

    @Override
    public @NotNull QName getTypeName() {
        return nativeDefinition.getTypeName();
    }

    @Override
    public @NotNull Class<T> getTypeClass() {
        // TODO cache this somehow
        return PrismContext.get().getSchemaRegistry().determineClassForType(getTypeName());
    }

    @Override
    public PrismPropertyValue<T> createPrismValueFromRealValue(@NotNull Object realValue) throws SchemaException {
        return ShadowAttributeValueConvertor.createPrismPropertyValueFromRealValue(realValue, this);
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return nativeDefinition.getAllowedValues();
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getSuggestedValues() {
        return nativeDefinition.getSuggestedValues();
    }

    @Override
    public @Nullable T defaultValue() {
        return nativeDefinition.defaultValue();
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return Boolean.TRUE.equals(
                customizationBean.isDisplayNameAttribute());
    }

    @Override
    public ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return customizationBean.getCorrelator();
    }

    @Override
    public ShadowSimpleAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
        // No deep cloning, because the constituents are immutable.
        return clone();
    }

    @Override
    public void revive(PrismContext prismContext) {
        // TODO is this [still] needed?
        customizationBean.asPrismContainerValue().revive(prismContext);
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        sb.append(this); // TODO improve if needed
    }

    @Override
    public String getDebugDumpClassName() {
        return "SSimpleAttrDef";
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        return clazz.isAssignableFrom(this.getClass())
                && QNameUtil.match(elementQName, getItemName(), caseInsensitive);
    }

    @Override
    public <T2 extends ItemDefinition<?>> T2 findItemDefinition(@NotNull ItemPath path, @NotNull Class<T2> clazz) {
        //noinspection unchecked
        return LivePrismItemDefinition.matchesThisDefinition(path, clazz, this) ? (T2) this : null;
    }

    @Override
    public @NotNull PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return new PropertyDeltaImpl<>(path, this);
    }

    @Override
    public @NotNull PrismPropertyDefinition.PrismPropertyDefinitionMutator<T> mutator() {
        throw new UnsupportedOperationException("Refined attribute definition can not be mutated: " + this);
    }

    @Override
    protected void extendToString(StringBuilder sb) {
        var matchingRuleQName = getMatchingRuleQName();
        if (matchingRuleQName != null) {
            sb.append(",MR=").append(PrettyPrinter.prettyPrint(matchingRuleQName));
        }
    }

    @Override
    public @NotNull ShadowSimpleAttributeDefinitionImpl<T> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShadowSimpleAttributeDefinitionImpl<?>)) {
            return false;
        }
        return super.equals(o); // no own fields to compare
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String getHumanReadableDescription() {
        return toString(); // FIXME
    }

    @Override
    public boolean isSimulated() {
        return false; // currently, all attributes are "real"; only reference attributes can be simulated
    }
}
