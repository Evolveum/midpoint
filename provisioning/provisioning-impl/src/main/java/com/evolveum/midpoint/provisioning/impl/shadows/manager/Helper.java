/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Generally useful methods for the shadow manager.
 *
 * TODO provide more meaningful name and/or split into more cohesive components
 */
@Component
class Helper {

    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private ShadowCaretaker shadowCaretaker;

    <T> T getNormalizedAttributeValue(PrismPropertyValue<T> pval, ResourceAttributeDefinition<?> rAttrDef) throws SchemaException {
        return matchingRuleRegistry
                .<T>getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName())
                .normalize(pval.getValue());
    }

    private <T> Collection<T> getNormalizedAttributeValues(
            ResourceAttribute<T> attribute,
            ResourceAttributeDefinition<T> attrDef)
            throws SchemaException {
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
        Collection<T> normalizedValues = new ArrayList<>();
        for (PrismPropertyValue<T> pval : attribute.getValues()) {
            T normalizedRealValue = matchingRule.normalize(pval.getValue());
            normalizedValues.add(normalizedRealValue);
        }
        return normalizedValues;
    }

    <T> T getNormalizedAttributeValue(ResourceAttributeDefinition<T> rAttrDef, T value) throws SchemaException {
        return matchingRuleRegistry
                .<T>getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName())
                .normalize(value);
    }

    <T> T determinePrimaryIdentifierValue(ProvisioningContext ctx, ShadowType shadow) throws SchemaException {
        if (ShadowUtil.isDead(shadow)) {
            return null;
        }
        ShadowLifecycleStateType state = shadowCaretaker.determineShadowState(ctx, shadow);
        if (state == REAPING || state == CORPSE || state == TOMBSTONE) {
            return null;
        }

        //noinspection unchecked
        ResourceAttribute<T> primaryIdentifier =
                (ResourceAttribute<T>) getPrimaryIdentifier(shadow);
        if (primaryIdentifier == null) {
            return null;
        }
        //noinspection unchecked
        ResourceAttributeDefinition<T> rDef =
                (ResourceAttributeDefinition<T>)
                        ctx.getObjectDefinitionRequired()
                                .findAttributeDefinitionRequired(primaryIdentifier.getElementName());

        Collection<T> normalizedPrimaryIdentifierValues = getNormalizedAttributeValues(primaryIdentifier, rDef);
        if (normalizedPrimaryIdentifierValues.isEmpty()) {
            throw new SchemaException("No primary identifier values in " + shadow);
        }
        if (normalizedPrimaryIdentifierValues.size() > 1) {
            throw new SchemaException("Too many primary identifier values in " + shadow + ", this is not supported yet");
        }
        return normalizedPrimaryIdentifierValues.iterator().next();
    }

    ResourceAttribute<String> getPrimaryIdentifier(ShadowType shadow) throws SchemaException {
        Collection<? extends ResourceAttribute<?>> primaryIdentifiers = emptyIfNull(ShadowUtil.getPrimaryIdentifiers(shadow));
        // Let's make this simple. We support single-attribute, single-value, string-only primary identifiers anyway
        if (primaryIdentifiers.isEmpty()) {
            // No primary identifiers. This can happen in sme cases, e.g. for proposed shadows.
            // Therefore we should be tolerating this.
            return null;
        }
        if (primaryIdentifiers.size() > 1) {
            throw new SchemaException("Too many primary identifiers in " + shadow + ", this is not supported yet");
        }
        //noinspection unchecked
        return (ResourceAttribute<String>) primaryIdentifiers.iterator().next();
    }

    void normalizeAttributes(
            ShadowType shadow, ResourceObjectDefinition objectClassDefinition) throws SchemaException {
        for (ResourceAttribute<?> attribute : ShadowUtil.getAttributes(shadow)) {
            normalizeAttribute(attribute, objectClassDefinition.findAttributeDefinitionRequired(attribute.getElementName()));
        }
    }

    <T> void normalizeAttribute(
            @NotNull ResourceAttribute<T> attribute,
            @NotNull ResourceAttributeDefinition<?> attrDef) throws SchemaException {
        MatchingRule<T> matchingRule =
                matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
        for (PrismPropertyValue<T> pval : attribute.getValues()) {
            T normalizedRealValue = matchingRule.normalize(pval.getValue());
            pval.setValue(normalizedRealValue);
        }
    }

    <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> delta,
            ResourceObjectDefinition objectDefinition) throws SchemaException {
        if (!delta.getPath().startsWithName(ShadowType.F_ATTRIBUTES)) {
            return;
        }
        ResourceAttributeDefinition<?> rAttrDef = objectDefinition.findAttributeDefinition(delta.getElementName());
        if (rAttrDef == null) {
            throw new SchemaException("Failed to normalize attribute: " + delta.getElementName() + ". Definition for this attribute doesn't exist.");
        }
        normalizeDelta(delta, rAttrDef);
    }

    <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> delta, ResourceAttributeDefinition<?> rAttrDef) throws SchemaException {
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
        if (delta.getValuesToReplace() != null) {
            normalizeValues(delta.getValuesToReplace(), matchingRule);
        }
        if (delta.getValuesToAdd() != null) {
            normalizeValues(delta.getValuesToAdd(), matchingRule);
        }

        if (delta.getValuesToDelete() != null) {
            normalizeValues(delta.getValuesToDelete(), matchingRule);
        }
    }

    private <T> void normalizeValues(Collection<PrismPropertyValue<T>> values, MatchingRule<T> matchingRule) throws SchemaException {
        for (PrismPropertyValue<T> pval : values) {
            T normalizedRealValue = matchingRule.normalize(pval.getValue());
            pval.setValue(normalizedRealValue);
        }
    }

    boolean containsNoResourceModification(Collection<? extends ItemDelta<?, ?>> modifications) {
        return modifications.stream()
                .noneMatch(this::isResourceModification);
    }

    /** TODO reconcile with {@link ProvisioningUtil#isResourceModification(ItemDelta)}. */
    private boolean isResourceModification(ItemDelta<?, ?> itemDelta) {
        ItemPath path = itemDelta.getPath();
        ItemPath parentPath = itemDelta.getParentPath();
        return ShadowType.F_ATTRIBUTES.equivalent(parentPath)
                || ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(path)
                || ShadowType.F_ASSOCIATION.equivalent(parentPath)
                || ShadowType.F_ASSOCIATION.equivalent(path)
                || ShadowType.F_ACTIVATION.equivalent(parentPath)
                || ShadowType.F_ACTIVATION.equivalent(path) // should not occur, but for completeness...
                || SchemaConstants.PATH_CREDENTIALS_PASSWORD.equivalent(parentPath);
    }
}
