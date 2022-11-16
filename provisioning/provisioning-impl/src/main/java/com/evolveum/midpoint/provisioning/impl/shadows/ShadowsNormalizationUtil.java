/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Repository shadows contain normalized attribute values.
 *
 * This class provides the necessary support for value normalization when storing the data and querying it.
 */
public class ShadowsNormalizationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowsNormalizationUtil.class);

    /** Normalizes shadow attributes "in place". */
    public static void normalizeAttributes(
            ShadowType shadow, ResourceObjectDefinition objectClassDefinition) throws SchemaException {
        for (ResourceAttribute<?> attribute : ShadowUtil.getAttributes(shadow)) {
            normalizeAttribute(
                    attribute,
                    objectClassDefinition.findAttributeDefinitionRequired(attribute.getElementName()));
        }
    }

    static private <T> void normalizeAttribute(
            @NotNull ResourceAttribute<T> attribute,
            @NotNull ResourceAttributeDefinition<?> attrDef) throws SchemaException {
        MatchingRule<T> matchingRule = getMatchingRule(attrDef);
        for (PrismPropertyValue<T> value : attribute.getValues()) {
            value.setValue(
                    matchingRule.normalize(
                            value.getValue()));
        }
    }

    public static <T> @NotNull MatchingRule<T> getMatchingRule(@NotNull ResourceAttributeDefinition<?> attrDef)
            throws SchemaException {
        return SchemaService.get().matchingRuleRegistry()
                .getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
    }

    /** No side effects, just returning the normalized (real) value. */
    public static <T> T getNormalizedAttributeValue(PrismPropertyValue<T> value, ResourceAttributeDefinition<?> attrDef)
            throws SchemaException {
        return ShadowsNormalizationUtil.<T>getMatchingRule(attrDef)
                .normalize(value.getValue());
    }

    /** No side effects, just returning the normalized (real) values. */
    public static <T> Collection<T> getNormalizedAttributeValues(
            ResourceAttribute<T> attribute, ResourceAttributeDefinition<T> attrDef)
            throws SchemaException {
        MatchingRule<T> matchingRule = getMatchingRule(attrDef);
        Collection<T> normalizedValues = new ArrayList<>();
        for (PrismPropertyValue<T> propertyValue : attribute.getValues()) {
            normalizedValues.add(
                    matchingRule.normalize(
                            propertyValue.getValue()));
        }
        return normalizedValues;
    }

    /** Normalizes the values in attribute delta. Does not modify the input, returns a copy if needed. */
    public static ItemDelta<?, ?> normalizeAttributeDelta(ItemDelta<?, ?> delta, ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        if (!delta.getPath().startsWithName(ShadowType.F_ATTRIBUTES)) {
            return delta;
        }
        ItemDelta<?, ?> deltaCopy = delta.clone();
        ResourceAttributeDefinition<?> rAttrDef =
                objectDefinition.findAttributeDefinitionRequired(
                        deltaCopy.getElementName(), () -> " during attribute normalization");
        normalizeAttributeDelta(deltaCopy, rAttrDef);
        return deltaCopy;
    }

    private static void normalizeAttributeDelta(ItemDelta<?, ?> delta, ResourceAttributeDefinition<?> attrDef)
            throws SchemaException {
        MatchingRule<?> matchingRule = getMatchingRule(attrDef);
        normalizeValues(delta.getValuesToReplace(), matchingRule);
        normalizeValues(delta.getValuesToAdd(), matchingRule);
        normalizeValues(delta.getValuesToDelete(), matchingRule);
    }

    private static void normalizeValues(Collection<? extends PrismValue> values, MatchingRule<?> matchingRule)
            throws SchemaException {
        for (PrismValue value : emptyIfNull(values)) {
            if (value instanceof PrismPropertyValue<?>) {
                //noinspection unchecked
                PrismPropertyValue<Object> propertyValue = (PrismPropertyValue<Object>) value;
                //noinspection unchecked
                propertyValue.setValue(
                        ((MatchingRule<Object>) matchingRule).normalize(propertyValue.getValue()));
            }
        }
    }

    /**
     * Visits the query and normalizes values according to matching rules set.
     *
     * This is because the repository shadows have attribute values stored in the normalized form.
     * Hence, when querying, we must query the normalized forms as well.
     *
     * Does not modify input query, creates a clone instead.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static ObjectQuery normalizeQueryValues(ObjectQuery originalQuery, ResourceObjectDefinition objectDef) {
        if (originalQuery == null) {
            return null;
        }

        ObjectQuery processedQuery = originalQuery.clone();
        ObjectFilter filter = processedQuery.getFilter();
        if (filter == null) {
            return originalQuery;
        }

        Visitor visitor = f -> {
            try {
                // TODO what about other kinds of filters?
                if (f instanceof EqualFilter) {
                    normalizeEqFilterAttrValues((EqualFilter<?>) f, objectDef);
                }
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        };
        filter.accept(visitor);
        return processedQuery;
    }

    private static  <T> void normalizeEqFilterAttrValues(EqualFilter<T> eqFilter, ResourceObjectDefinition objectDef)
            throws SchemaException {
        if (!eqFilter.getParentPath().equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
            return;
        }

        QName attrName = eqFilter.getElementName();
        ResourceAttributeDefinition<?> attrDef = MiscUtil.requireNonNull(
                objectDef.findAttributeDefinition(attrName),
                () -> "Unknown attribute " + attrName + " in filter " + eqFilter);

        if (attrDef.getMatchingRuleQName() == null) {
            return;
        }

        MatchingRule<T> matchingRule = getMatchingRule(attrDef);

        List<PrismPropertyValue<T>> valuesList = eqFilter.getValues();
        if (valuesList != null) {
            List<PrismPropertyValue<T>> newValues = new ArrayList<>();
            for (PrismPropertyValue<T> oldValue : valuesList) {
                newValues.add(
                        getNormalizedPropertyValue(matchingRule, oldValue));
            }
            valuesList.clear();
            valuesList.addAll(newValues);
            LOGGER.trace("Replacing values for attribute {} in search filter with normalized values because there "
                    + "is a matching rule. Normalized values: {}", attrName, newValues);
        }
    }

    private static <T> @NotNull PrismPropertyValue<T> getNormalizedPropertyValue(
            MatchingRule<T> matchingRule, PrismPropertyValue<T> propertyValue)
            throws SchemaException {
        PrismPropertyValue<T> newValue = propertyValue.clone();
        newValue.setValue(
                matchingRule.normalize(
                        propertyValue.getValue()));
        return newValue;
    }

    public static <T> List<PrismPropertyValue<T>> getNormalizedValues(PrismProperty<T> attribute, ResourceObjectDefinition objDef)
            throws SchemaException {
        ResourceAttributeDefinition<?> attrDef = objDef.findAttributeDefinitionRequired(attribute.getElementName());
        MatchingRule<T> matchingRule = getMatchingRule(attrDef);
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
}
