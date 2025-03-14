/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.ClassUtils;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ShadowSimpleAttributeImpl<T> extends PrismPropertyImpl<T> implements ShadowSimpleAttribute<T> {

    @Serial private static final long serialVersionUID = -6149194956029296486L;

    ShadowSimpleAttributeImpl(QName name, ShadowSimpleAttributeDefinition<T> definition) {
        super(name, definition);
    }

    @Override
    public ShadowSimpleAttributeDefinition<T> getDefinition() {
        return (ShadowSimpleAttributeDefinition<T>) super.getDefinition();
    }

    @Override
    public ShadowSimpleAttributeImpl<T> createImmutableClone() {
        return (ShadowSimpleAttributeImpl<T>) super.createImmutableClone();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ShadowSimpleAttribute<T> clone() {
        return cloneComplex(CloneStrategy.LITERAL_MUTABLE);
    }

    @Override
    public @NotNull ShadowSimpleAttribute<T> cloneComplex(@NotNull CloneStrategy strategy) {
        if (isImmutable() && !strategy.mutableCopy()) {
            return this; // FIXME here should come a flyweight
        }

        ShadowSimpleAttributeImpl<T> clone = new ShadowSimpleAttributeImpl<>(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ShadowSimpleAttributeImpl<T> clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "RA";
    }

    @Override
    protected void checkDefinition(@NotNull PrismPropertyDefinition<T> def) {
        super.checkDefinition(def);
        Preconditions.checkArgument(
                def instanceof ShadowSimpleAttributeDefinition,
                "Definition should be %s not %s" ,
                ShadowSimpleAttributeDefinition.class.getSimpleName(), definition.getClass().getName());
    }

    @Override
    protected void applyDefinitionToValues(@NotNull PrismPropertyDefinition<T> newDefinition, boolean force)
            throws SchemaException {
        // We always do the forced reapplication of definition here. It is the most simple approach. TODO reconsider if needed.
        List<T> oldRealValues = List.copyOf(getRealValues());
        clear();

        addConvertedValues(oldRealValues, (ShadowSimpleAttributeDefinition<T>) newDefinition);
    }

    private void addConvertedValues(@NotNull Collection<?> realValues, @NotNull ShadowSimpleAttributeDefinition<T> newDefinition)
            throws SchemaException {
        Normalizer<T> normalizer = newDefinition.getNormalizer();
        Normalizer<String> polyStringNormalizer = newDefinition.getStringNormalizerIfApplicable();
        //noinspection unchecked
        Class<T> newJavaType = (Class<T>) ClassUtils.resolvePrimitiveIfNecessary(newDefinition.getTypeClass());
        for (Object realValue : realValues) {
            // MID-5833 Assuming no duplicate values. The conversion should be a bijection. Hence, no dup check is needed.
            addIgnoringEquivalents(
                    convertAndNormalize(realValue, newJavaType, normalizer, polyStringNormalizer));
        }
    }

    /** Returns detached value. */
    private <T2> PrismPropertyValue<T2> convertAndNormalize(
            @NotNull Object oldRealValue,
            @NotNull Class<T2> newJavaType,
            @NotNull Normalizer<T2> normalizer,
            Normalizer<String> polyStringNormalizer) throws SchemaException {

        Preconditions.checkNotNull(oldRealValue,
                "null value cannot be added to %s", this);

        return ShadowAttributeValueConvertor.convertAndNormalize(oldRealValue, newJavaType, normalizer, polyStringNormalizer);
    }

    @Override
    public @NotNull Collection<T> getOrigValues() {
        return getRealValues();
    }

    @Override
    public @NotNull Collection<T> getNormValues() throws SchemaException {
        var normalizer = getDefinition().getNormalizer();

        Collection<T> origValues = getRealValues();
        if (normalizer.isIdentity()) {
            return origValues;
        } else {
            Collection<T> normValues = new ArrayList<>();
            for (T origValue : origValues) {
                normValues.add(normalizer.normalize(origValue));
            }
            return normValues;
        }
    }

    @Override
    public void checkConsistenceInternal(
            Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);

        ShadowSimpleAttributeDefinition<T> definition = getDefinition();
        if (!scope.isThorough() || definition == null) {
            return;
        }

        // Resource attribute values have no special type, otherwise we would just extend their "checkConsistenceInternal"
        for (PrismPropertyValue<T> value : values) {
            T realValue = stateNonNull(value.getRealValue(), "Null real value in %s", this);
            Class<?> expectedType = primitiveToWrapper(definition.getTypeClass());
            Class<?> actualType = primitiveToWrapper(realValue.getClass());
            stateCheck(expectedType.isAssignableFrom(actualType),
                    "The value '%s' does not conform to the definition %s in %s: expected type: %s, actual type: %s",
                    value, definition, this, expectedType, actualType);
        }
    }

    @Override
    public void addValueSkipUniquenessCheck(PrismPropertyValue<T> value) {
        // This also recomputes the value, so e.g. computes the "norm" for polystrings.
        // It may be good or not; nevertheless, it makes TestOpenDjDumber.test478b to pass.
        addValue(value, false);
    }
}
