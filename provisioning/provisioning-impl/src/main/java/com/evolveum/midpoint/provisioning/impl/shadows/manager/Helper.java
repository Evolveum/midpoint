/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Generally useful methods for the shadow manager.
 *
 * TODO provide more meaningful name and/or split into more cohesive components
 */
@Component
class Helper {

    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    <T> T getNormalizedAttributeValue(PrismPropertyValue<T> pval, RefinedAttributeDefinition<?> rAttrDef) throws SchemaException {
        return matchingRuleRegistry
                .<T>getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName())
                .normalize(pval.getValue());
    }

    <T> Collection<T> getNormalizedAttributeValues(ResourceAttribute<T> attribute, RefinedAttributeDefinition<T> rAttrDef) throws SchemaException {
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
        Collection<T> normalizedValues = new ArrayList<>();
        for (PrismPropertyValue<T> pval : attribute.getValues()) {
            T normalizedRealValue = matchingRule.normalize(pval.getValue());
            normalizedValues.add(normalizedRealValue);
        }
        return normalizedValues;
    }

    <T> T getNormalizedAttributeValue(RefinedAttributeDefinition<T> rAttrDef, T value) throws SchemaException {
        return matchingRuleRegistry
                .<T>getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName())
                .normalize(value);
    }

    public String determinePrimaryIdentifierValue(ProvisioningContext ctx, PrismObject<ShadowType> shadow) throws SchemaException {
        if (ShadowUtil.isDead(shadow)) {
            return null;
        }
        ResourceAttribute<String> primaryIdentifier = getPrimaryIdentifier(shadow);
        if (primaryIdentifier == null) {
            return null;
        }
        RefinedAttributeDefinition<String> rDef;
        try {
            rDef = ctx.getObjectClassDefinition().findAttributeDefinition(primaryIdentifier.getElementName());
        } catch (ConfigurationException | ObjectNotFoundException | CommunicationException
                | ExpressionEvaluationException e) {
            // Should not happen at this stage. And we do not want to pollute throws clauses all the way up.
            throw new SystemException(e.getMessage(), e);
        }
        Collection<String> normalizedPrimaryIdentifierValues = getNormalizedAttributeValues(primaryIdentifier, rDef);
        if (normalizedPrimaryIdentifierValues.isEmpty()) {
            throw new SchemaException("No primary identifier values in " + shadow);
        }
        if (normalizedPrimaryIdentifierValues.size() > 1) {
            throw new SchemaException("Too many primary identifier values in " + shadow + ", this is not supported yet");
        }
        return normalizedPrimaryIdentifierValues.iterator().next();
    }

    ResourceAttribute<String> getPrimaryIdentifier(PrismObject<ShadowType> shadow) throws SchemaException {
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

    public void setKindIfNecessary(ShadowType repoShadowType, RefinedObjectClassDefinition objectClassDefinition) {
        if (repoShadowType.getKind() == null && objectClassDefinition != null) {
            repoShadowType.setKind(objectClassDefinition.getKind());
        }
    }

    public void setIntentIfNecessary(ShadowType repoShadowType, RefinedObjectClassDefinition objectClassDefinition) {
        if (repoShadowType.getIntent() == null && objectClassDefinition.getIntent() != null) {
            repoShadowType.setIntent(objectClassDefinition.getIntent());
        }
    }

    public void normalizeAttributes(PrismObject<ShadowType> shadow, RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
        for (ResourceAttribute<?> attribute : ShadowUtil.getAttributes(shadow)) {
            RefinedAttributeDefinition rAttrDef = objectClassDefinition.findAttributeDefinition(attribute.getElementName());
            normalizeAttribute(attribute, rAttrDef);
        }
    }

    <T> void normalizeAttribute(ResourceAttribute<T> attribute, RefinedAttributeDefinition rAttrDef) throws SchemaException {
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
        for (PrismPropertyValue<T> pval : attribute.getValues()) {
            T normalizedRealValue = matchingRule.normalize(pval.getValue());
            pval.setValue(normalizedRealValue);
        }
    }

    public <T> void normalizeDeltas(Collection<? extends ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>>> deltas,
            RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
        for (ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> delta : deltas) {
            normalizeDelta(delta, objectClassDefinition);
        }
    }

    public <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> delta,
            RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
        if (!delta.getPath().startsWithName(ShadowType.F_ATTRIBUTES)) {
            return;
        }
        RefinedAttributeDefinition rAttrDef = objectClassDefinition.findAttributeDefinition(delta.getElementName());
        if (rAttrDef == null) {
            throw new SchemaException("Failed to normalize attribute: " + delta.getElementName() + ". Definition for this attribute doesn't exist.");
        }
        normalizeDelta(delta, rAttrDef);
    }

    <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> delta, RefinedAttributeDefinition rAttrDef) throws SchemaException {
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

    public <T> boolean compareAttribute(RefinedObjectClassDefinition refinedObjectClassDefinition,
            ResourceAttribute<T> attributeA, T... valuesB) throws SchemaException {
        RefinedAttributeDefinition refinedAttributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(attributeA.getElementName());
        Collection<T> valuesA = getNormalizedAttributeValues(attributeA, refinedAttributeDefinition);
        return MiscUtil.unorderedCollectionEquals(valuesA, Arrays.asList(valuesB));
    }

    public <T> boolean compareAttribute(RefinedObjectClassDefinition refinedObjectClassDefinition,
            ResourceAttribute<T> attributeA, ResourceAttribute<T> attributeB) throws SchemaException {
        RefinedAttributeDefinition refinedAttributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(attributeA.getElementName());
        Collection<T> valuesA = getNormalizedAttributeValues(attributeA, refinedAttributeDefinition);

        refinedAttributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(attributeA.getElementName());
        Collection<T> valuesB = getNormalizedAttributeValues(attributeB, refinedAttributeDefinition);
        return MiscUtil.unorderedCollectionEquals(valuesA, valuesB);
    }

    boolean isRepositoryOnlyModification(Collection<? extends ItemDelta> modifications) {
        for (ItemDelta itemDelta : modifications) {
            if (isResourceModification(itemDelta)) {
                return false;
            }
        }
        return true;
    }

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
