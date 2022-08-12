/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedPlainResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.PlainResourceObjectConstruction;
import com.evolveum.midpoint.model.impl.lens.construction.ResourceObjectConstruction;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * TODO
 *
 * @author Radovan Semancik
 * @author lazyman
 */
@Component
public class ConsolidationProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ConsolidationProcessor.class);

    private static final String OP_CONSOLIDATE_VALUES = ConsolidationProcessor.class.getName() + ".consolidateValues";
    private static final String OP_CONSOLIDATE_ITEM = ConsolidationProcessor.class.getName() + ".consolidateItem";

    private PrismContainerDefinition<ShadowAssociationType> associationDefinition;

    @Autowired private ContextLoader contextLoader;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private PrismContext prismContext;

    /**
     * Converts delta set triples to a secondary account deltas.
     */
    <F extends FocusType>
    void consolidateValues(LensContext<F> context, LensProjectionContext projCtx, Task task,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
            //todo filter changes which were already in account sync delta

        OperationResult result = parentResult.subresult(OP_CONSOLIDATE_VALUES)
                .setMinor()
                .build();
        try {
            //account was deleted, no changes are needed.
            if (wasProjectionDeleted(projCtx)) {
                return;
            }

            SynchronizationPolicyDecision policyDecision = projCtx.getSynchronizationPolicyDecision();

            context.checkConsistenceIfNeeded();
            if (policyDecision == SynchronizationPolicyDecision.DELETE) {
                // Nothing to do
            } else {
                // This is ADD, KEEP, UNLINK or null. All are in fact the same as KEEP
                consolidateValuesModifyProjection(context, projCtx, task, result);
                context.checkConsistenceIfNeeded();
            }
            context.recompute();
            context.checkConsistenceIfNeeded();
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean wasProjectionDeleted(LensProjectionContext accContext) {
        return ObjectDelta.isDelete(accContext.getSyncDelta());
    }

    private <F extends FocusType> ObjectDelta<ShadowType> consolidateValuesToModifyDelta(LensContext<F> context,
            LensProjectionContext projCtx, boolean addUnchangedValues, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException {

        squeezeAll(context, projCtx);

        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.MODIFY);
        objectDelta.setOid(projCtx.getOid());

        ObjectDelta<ShadowType> existingDelta = projCtx.getSummaryDelta(); // TODO check this
        LOGGER.trace("Existing delta:\n{}", existingDelta);

        // Do not automatically load the full projection now. Even if we have weak mapping.
        // That may be a waste of resources if the weak mapping results in no change anyway.
        // Let's be very very lazy about fetching the account from the resource.
        if (!projCtx.hasFullShadow() &&
                (hasActiveWeakMapping(projCtx.getSqueezedAttributes(), projCtx) || hasActiveWeakMapping(projCtx.getSqueezedAssociations(), projCtx)
                        || (hasActiveStrongMapping(projCtx.getSqueezedAttributes(), projCtx) || hasActiveStrongMapping(projCtx.getSqueezedAssociations(), projCtx)))) {
            // Full account was not yet loaded. This will cause problems as the weak mapping may be applied even though
            // it should not be applied and also same changes may be discarded because of unavailability of all
            // account's attributes. Therefore load the account now, but with doNotDiscovery options.

            // We also need to get account if there are strong mappings. Strong mappings
            // should always be applied. So reading the account now will indirectly
            // trigger reconciliation which makes sure that the strong mappings are
            // applied.

            // By getting accounts from provisioning, there might be a problem with
            // resource availability. We need to know, if the account was read full
            // or we have only the shadow from the repository. If we have only
            // shadow, the weak mappings may applied even if they should not be.
            contextLoader.loadFullShadow(projCtx, "weak or strong mapping", task, result);
            if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
                return null;
            }
        }

        ResourceObjectDefinition rOcDef = consolidateAuxiliaryObjectClasses(context, projCtx, addUnchangedValues, objectDelta, existingDelta, result);

        LOGGER.trace("Definition for {} consolidation:\n{}", projCtx.getKey(), rOcDef.debugDumpLazily());

        StrengthSelector strengthSelector = projCtx.isAdd() ? StrengthSelector.ALL : StrengthSelector.ALL_EXCEPT_WEAK;
        consolidateAttributes(projCtx, addUnchangedValues, rOcDef, objectDelta, existingDelta, strengthSelector, result);
        consolidateAssociations(context, projCtx, addUnchangedValues, rOcDef, objectDelta, existingDelta, strengthSelector, result);

        LOGGER.trace("consolidateValuesToModifyDelta result:\n{}", objectDelta.debugDumpLazily());

        return objectDelta;
    }

    private <F extends FocusType> ResourceObjectDefinition consolidateAuxiliaryObjectClasses(
            LensContext<F> context,
            LensProjectionContext projCtx,
            boolean addUnchangedValues,
            ObjectDelta<ShadowType> objectDelta,
            ObjectDelta<ShadowType> existingDelta,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {
        ProjectionContextKey key = projCtx.getKey();
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses = projCtx.getSqueezedAuxiliaryObjectClasses();

        // AUXILIARY OBJECT CLASSES
        PrismPropertyDefinition<QName> auxiliaryObjectClassPropertyDef = projCtx.getObjectDefinition().findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        PropertyDelta<QName> auxiliaryObjectClassAPrioriDelta = null;
        ResourceSchema refinedSchema = projCtx.getResourceSchema();
        List<QName> auxOcNames = new ArrayList<>();
        List<ResourceObjectDefinition> auxOcDefs = new ArrayList<>();
        ObjectDelta<ShadowType> projDelta = projCtx.getSummaryDelta(); // TODO check this
        if (projDelta != null) {
            auxiliaryObjectClassAPrioriDelta = projDelta.findPropertyDelta(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        }
        for (Entry<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> entry : squeezedAuxiliaryObjectClasses.entrySet()) {
            DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>> ivwoTriple = entry.getValue();

            LOGGER.trace("CONSOLIDATE auxiliary object classes ({}) from triple:\n{}", key, ivwoTriple.debugDumpLazily());

            for (ItemValueWithOrigin<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>> ivwo: ivwoTriple.getAllValues()) {
                QName auxObjectClassName = ivwo.getItemValue().getValue();
                if (auxOcNames.contains(auxObjectClassName)) {
                    continue;
                }
                auxOcNames.add(auxObjectClassName);
                ResourceObjectDefinition auxOcDef = refinedSchema.findObjectClassDefinition(auxObjectClassName);
                if (auxOcDef == null) {
                    LOGGER.error("Auxiliary object class definition {} for {} not found in the schema, but it should be there, dumping context:\n{}",
                            auxObjectClassName, key, context.debugDump());
                    throw new IllegalStateException("Auxiliary object class definition " + auxObjectClassName + " for "+ key + " not found in the context, but it should be there");
                }
                auxOcDefs.add(auxOcDef);
            }

            //noinspection unchecked
            try (IvwoConsolidator<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>, ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>
                    consolidator = new IvwoConsolidatorBuilder()
                    .itemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS)
                    .ivwoTriple(ivwoTriple)
                    .itemDefinition(auxiliaryObjectClassPropertyDef)
                    .aprioriItemDelta(auxiliaryObjectClassAPrioriDelta)
                    .itemDeltaExists(!ItemDelta.isEmpty(auxiliaryObjectClassAPrioriDelta)) // TODO
                    .itemContainer(projCtx.getObjectNew())
                    .valueMatcher(null)
                    .comparator(null)
                    .addUnchangedValues(addUnchangedValues)
                    .addUnchangedValuesExceptForNormalMappings(true) // todo
                    .existingItemKnown(projCtx.hasFullShadow())
                    .isExclusiveStrong(false)
                    .contextDescription(key.toHumanReadableDescription())
                    .strengthSelector(StrengthSelector.ALL_EXCEPT_WEAK)
                    .result(result)
                    .build()) {

                // TODO what about setting existing item?

                //noinspection unchecked
                PropertyDelta<QName> propDelta = (PropertyDelta) consolidator.consolidateToDeltaNoMetadata();

                LOGGER.trace("Auxiliary object class delta:\n{}", propDelta.debugDumpLazily());

                if (!propDelta.isEmpty()) {
                    objectDelta.addModification(propDelta);
                }
            }
        }

        ResourceObjectDefinition structuralDefinition = projCtx.getStructuralObjectDefinition();
        if (structuralDefinition == null) {
            LOGGER.error("Structural object class definition for {} not found in the context, but it should be there, dumping context:\n{}", key, context.debugDump());
            throw new IllegalStateException("Structural object class definition for " + key + " not found in the context, but it should be there");
        }

        return new CompositeObjectDefinitionImpl(structuralDefinition, auxOcDefs);
    }

    private void consolidateAttributes(LensProjectionContext projCtx,
            boolean addUnchangedValues, ResourceObjectDefinition rOcDef, ObjectDelta<ShadowType> objectDelta,
            ObjectDelta<ShadowType> existingDelta, StrengthSelector strengthSelector, OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, PolicyViolationException {
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>, PrismPropertyDefinition<?>>>> squeezedAttributes = projCtx.getSqueezedAttributes();
        // Iterate and process each attribute separately. Now that we have squeezed the data we can process each attribute just
        // with the data in ItemValueWithOrigin triples.
        for (Map.Entry<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> entry : squeezedAttributes.entrySet()) {
            QName attributeName = entry.getKey();
            DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>> triple = entry.getValue();
            PropertyDelta<?> propDelta = consolidateAttribute(rOcDef, projCtx.getKey(), existingDelta, projCtx,
                    addUnchangedValues, attributeName, (DeltaSetTriple)triple, strengthSelector, result);
            if (propDelta != null) {
                objectDelta.addModification(propDelta);
            }
        }
    }

    private <T> PropertyDelta<T> consolidateAttribute(ResourceObjectDefinition rOcDef,
            ProjectionContextKey key, ObjectDelta<ShadowType> existingDelta, LensProjectionContext projCtx,
            boolean addUnchangedValues, QName itemName,
            DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<T>, PrismPropertyDefinition<T>>> triple,
            StrengthSelector strengthSelector, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException {

        if (triple == null || triple.isEmpty()) {
            return null;
        }

        //noinspection unchecked
        ResourceAttributeDefinition<T> attributeDefinition =
                triple.getAnyValue().getConstruction().findAttributeDefinition(itemName);

        ItemPath itemPath = ItemPath.create(ShadowType.F_ATTRIBUTES, itemName);

        if (attributeDefinition.isIgnored(LayerType.MODEL)) {
            LOGGER.trace("Skipping processing mappings for attribute {} because it is ignored", itemName);
            return null;
        }

        ValueMatcher<T> valueMatcher = ValueMatcher.createMatcher(attributeDefinition, matchingRuleRegistry);

        return (PropertyDelta<T>) consolidateItem(rOcDef, key, existingDelta, projCtx, addUnchangedValues,
                attributeDefinition.isExclusiveStrong(), itemPath, attributeDefinition, triple, valueMatcher, null, strengthSelector, "attribute "+itemName, result);
    }

    private <F extends FocusType> void consolidateAssociations(LensContext<F> context, LensProjectionContext projCtx,
            boolean addUnchangedValues, ResourceObjectDefinition rOcDef, ObjectDelta<ShadowType> objectDelta,
            ObjectDelta<ShadowType> existingDelta, StrengthSelector strengthSelector, OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, PolicyViolationException {
        for (Entry<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> entry : projCtx.getSqueezedAssociations().entrySet()) {
            QName associationName = entry.getKey();
            DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> triple = entry.getValue();
            ContainerDelta<ShadowAssociationType> containerDelta = consolidateAssociation(rOcDef, projCtx.getKey(),
                    existingDelta, projCtx, addUnchangedValues, associationName, triple, strengthSelector, result);
            if (containerDelta != null) {
                objectDelta.addModification(containerDelta);
            }
        }
    }

    private ContainerDelta<ShadowAssociationType> consolidateAssociation(
            ResourceObjectDefinition rOcDef, ProjectionContextKey key, ObjectDelta<ShadowType> existingDelta,
            LensProjectionContext projCtx, boolean addUnchangedValues, QName associationName,
            DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> triple,
            StrengthSelector strengthSelector, OperationResult result) throws SchemaException, ExpressionEvaluationException, PolicyViolationException {

        PrismContainerDefinition<ShadowAssociationType> assocContainerDef = getAssociationDefinition();
        ResourceAssociationDefinition associationDef = rOcDef.findAssociationDefinition(associationName);

        Comparator<PrismContainerValue<ShadowAssociationType>> comparator = (o1, o2) -> {
            if (o1 == null && o2 == null) {
                LOGGER.trace("Comparing {} and {}: 0 (A)", o1, o2);
                return 0;
            }

            if (o1 == null || o2 == null) {
                LOGGER.trace("Comparing {} and {}: 2 (B)", o1, o2);
                return 1;
            }

            PrismReference ref1 = o1.findReference(ShadowAssociationType.F_SHADOW_REF);
            PrismReference ref2 = o2.findReference(ShadowAssociationType.F_SHADOW_REF);

            // We do not want to compare references in details. Comparing OIDs suffices.
            // Otherwise we get into problems, as one of the references might be e.g. without type,
            // causing unpredictable behavior (MID-2368)

            // TODO what if OIDs are both null, and associations differ in identifier values?
            String oid1 = ref1 != null ? ref1.getOid() : null;
            String oid2 = ref2 != null ? ref2.getOid() : null;
            if (ObjectUtils.equals(oid1, oid2)) {
                LOGGER.trace("Comparing {} and {}: 0 (C)", o1, o2);
                return 0;
            }

            LOGGER.trace("Comparing {} and {}: 1 (D)", o1, o2);
            return 1;
        };

        ContainerDelta<ShadowAssociationType> delta = (ContainerDelta<ShadowAssociationType>) consolidateItem(rOcDef, key, existingDelta,
                projCtx, addUnchangedValues, associationDef.isExclusiveStrong(), ShadowType.F_ASSOCIATION,
                assocContainerDef, triple, null, comparator, strengthSelector, "association "+associationName, result);

        if (delta != null) {
            setAssociationName(delta.getValuesToAdd(), associationName);
            setAssociationName(delta.getValuesToDelete(), associationName);
            setAssociationName(delta.getValuesToReplace(), associationName);
        }

        return delta;
    }

    private void setAssociationName(Collection<PrismContainerValue<ShadowAssociationType>> values, QName itemName) {
        if (values == null) {
            return;
        }
        for (PrismContainerValue<ShadowAssociationType> val: values) {
            val.asContainerable().setName(itemName);
        }
    }

    private PrismContainerDefinition<ShadowAssociationType> getAssociationDefinition() {
        if (associationDefinition == null) {
            associationDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class)
                    .findContainerDefinition(ShadowType.F_ASSOCIATION);
        }
        return associationDefinition;
    }

    private <V extends PrismValue,D extends ItemDefinition> ItemDelta<V,D> consolidateItem(
            ResourceObjectDefinition rOcDef,
            ProjectionContextKey key,
            ObjectDelta<ShadowType> existingDelta,
            LensProjectionContext projCtx,
            boolean addUnchangedValues,
            boolean isExclusiveStrong,
            ItemPath itemPath,
            D itemDefinition,
            DeltaSetTriple<ItemValueWithOrigin<V, D>> triple,
            ValueMatcher<?> valueMatcher,
            Comparator<V> comparator,
            StrengthSelector strengthSelector,
            String itemDesc,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException {

        OperationResult result = parentResult.subresult(OP_CONSOLIDATE_ITEM)
                .setMinor()
                .addArbitraryObjectAsParam("itemPath", itemPath)
                .build();
        try {
            ItemDelta<V,D> aprioriItemDelta = existingDelta != null ? existingDelta.findItemDelta(itemPath) : null;
            boolean aprioriDeltaIsReplace = aprioriItemDelta != null && aprioriItemDelta.isReplace();

            // We need to add all values if there is replace delta. Otherwise the zero-set values will be lost
            //noinspection UnnecessaryLocalVariable
            boolean forceAddUnchangedValues = aprioriDeltaIsReplace;

            LOGGER.trace("CONSOLIDATE {}\n  ({}) completeShadow={}, addUnchangedValues={}, forceAddUnchangedValues={}",
                    itemDesc, key, projCtx.hasFullShadow(), addUnchangedValues, forceAddUnchangedValues);

            boolean existingItemKnown = projCtx.hasFullShadow() || rOcDef.isIdentifier(itemDefinition.getItemName());
            ItemDelta<V, D> itemDelta;
            // Use the consolidator to do the computation. It does most of the work.
            try (IvwoConsolidator<V,D,ItemValueWithOrigin<V,D>> consolidator = new IvwoConsolidatorBuilder<V,D,ItemValueWithOrigin<V, D>>()
                    .itemPath(itemPath)
                    .ivwoTriple(triple)
                    .itemDefinition(itemDefinition)
                    .aprioriItemDelta(aprioriItemDelta)
                    .itemDeltaExists(!ItemDelta.isEmpty(aprioriItemDelta))
                    .itemContainer(projCtx.getObjectNew())
                    .valueMatcher(valueMatcher)
                    .comparator(comparator)
                    .addUnchangedValues(addUnchangedValues || forceAddUnchangedValues)
                    .addUnchangedValuesExceptForNormalMappings(true) // todo
                    .existingItemKnown(existingItemKnown)
                    .isExclusiveStrong(isExclusiveStrong)
                    .contextDescription(key.toHumanReadableDescription())
                    .strengthSelector(existingItemKnown ? strengthSelector : strengthSelector.notWeak())
                    .result(result)
                    .build()) {

                // TODO what about setting existing item?

                itemDelta = consolidator.consolidateToDeltaNoMetadata();
            }

            LOGGER.trace("Consolidated delta (before sync filter) for {}:\n{}",key, itemDelta.debugDumpLazily());

            if (aprioriDeltaIsReplace) {
                // We cannot filter out any values if there is an replace delta. The replace delta cleans all previous
                // state and all the values needs to be passed on
                LOGGER.trace("Skipping consolidation with sync delta as there was a replace delta on top of that already");
            } else {
                // Also consider a synchronization delta (if it is present). This may filter out some deltas.
                itemDelta = consolidateItemWithSync(projCtx, itemDelta, valueMatcher);
                LOGGER.trace("Consolidated delta (after sync filter) for {}:\n{}", key, DebugUtil.debugDumpLazily(itemDelta));
            }

            if (!ItemDelta.isEmpty(itemDelta)) {
                if (!aprioriDeltaIsReplace) {
                    // We cannot simplify if there is already a replace delta. This might result in
                    // two replace deltas and therefore some information may be lost
                    itemDelta.simplify();
                }

                // Validate the delta. i.e. make sure it conforms to schema (that it does not have more values than allowed, etc.)
                if (aprioriItemDelta != null) {
                    // Let's make sure that both the previous delta and this delta makes sense
                    ItemDelta<V,D> mergedDelta = aprioriItemDelta.clone();
                    mergedDelta.merge(itemDelta);
                    mergedDelta.validate();
                } else {
                    itemDelta.validate();
                }

                return itemDelta;
            } else {
                return null;
            }

        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void fillInAssociationNames(Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> squeezedAssociations) throws SchemaException {
        PrismPropertyDefinition<QName> nameDefinition = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(ShadowAssociationType.class)
                .findPropertyDefinition(ShadowAssociationType.F_NAME);
        for (Entry<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> entry : squeezedAssociations.entrySet()) {
            DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> deltaSetTriple = entry.getValue();
            for (ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> ivwo : deltaSetTriple.getAllValues()) {
                PrismContainerValue<ShadowAssociationType> value = ivwo.getItemValue();
                if (value != null && value.findProperty(ShadowAssociationType.F_NAME) == null) {  // just for safety
                    PrismProperty<QName> nameProperty = value.createProperty(nameDefinition);
                    nameProperty.setRealValue(entry.getKey());
                }
            }
        }
        LOGGER.trace("Names for squeezed associations filled-in.");
    }

    private <V extends PrismValue,D extends ItemDefinition> boolean hasActiveWeakMapping(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedAttributes, LensProjectionContext accCtx) throws SchemaException {
        for (Map.Entry<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> entry : squeezedAttributes.entrySet()) {
            DeltaSetTriple<ItemValueWithOrigin<V,D>> ivwoTriple = entry.getValue();
            boolean hasWeak = false;
            for (ItemValueWithOrigin<V,D> ivwo: ivwoTriple.getAllValues()) {
                PrismValueDeltaSetTripleProducer<V,D> mapping = ivwo.getMapping();
                if (mapping.getStrength() == MappingStrengthType.WEAK) {
                    // We only care about mappings that change something. If the weak mapping is not
                    // changing anything then it will not be applied in this step anyway. Therefore
                    // there is no point in loading the real values just because there is such mapping.
                    // Note: we can be sure that we are NOT doing reconciliation. If we do reconciliation
                    // then we cannot get here in the first place (the projection is already loaded).
                    PrismValueDeltaSetTriple<?> outputTriple = mapping.getOutputTriple();
                    if (outputTriple != null && !outputTriple.isEmpty() && !outputTriple.isZeroOnly()) {
                        return true;
                    }
                    hasWeak = true;
                }
            }
            if (hasWeak) {
                // If we have a weak mapping for this attribute and there is also any
                // other mapping with a minus set then we need to get the real current value.
                // The minus value may cause that the result of consolidation is empty value.
                // In that case we should apply the weak mapping. But we will not know this
                // unless we fetch the real values.
                if (ivwoTriple.hasMinusSet()) {
                    for (ItemValueWithOrigin<V,D> ivwo: ivwoTriple.getMinusSet()) {
                        PrismValueDeltaSetTripleProducer<V, D> mapping = ivwo.getMapping();
                        PrismValueDeltaSetTriple<?> outputTriple = mapping.getOutputTriple();
                        if (outputTriple != null && !outputTriple.isEmpty()) {
                            return true;
                        }
                    }
                }
                for (ItemValueWithOrigin<V,D> ivwo: ivwoTriple.getNonNegativeValues()) {
                    PrismValueDeltaSetTripleProducer<V, D> mapping = ivwo.getMapping();
                    PrismValueDeltaSetTriple<?> outputTriple = mapping.getOutputTriple();
                    if (outputTriple != null && outputTriple.hasMinusSet()) {
                        return true;
                    }
                }
                ObjectDelta<ShadowType> projectionDelta = accCtx.getSummaryDelta(); // TODO check this
                if (projectionDelta != null) {
                    PropertyDelta<?> aPrioriAttributeDelta = projectionDelta.findPropertyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, entry.getKey()));
                    if (aPrioriAttributeDelta != null && aPrioriAttributeDelta.isDelete()) {
                        return true;
                    }
                    if (aPrioriAttributeDelta != null && aPrioriAttributeDelta.isReplace() && aPrioriAttributeDelta.getValuesToReplace().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private <V extends PrismValue,D extends ItemDefinition> boolean hasActiveStrongMapping(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedAttributes, LensProjectionContext accCtx) throws SchemaException {
        for (Map.Entry<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> entry : squeezedAttributes.entrySet()) {
            DeltaSetTriple<ItemValueWithOrigin<V,D>> ivwoTriple = entry.getValue();
            for (ItemValueWithOrigin<V,D> ivwo: ivwoTriple.getAllValues()) {
                PrismValueDeltaSetTripleProducer<V,D> mapping = ivwo.getMapping();
                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    // Do not optimize for "nothing changed" case here. We want to make
                    // sure that the values of strong mappings are applied even if nothing
                    // has changed.
                    return true;
                }
            }
        }
        return false;
    }

    private <F extends FocusType> void consolidateValuesModifyProjection(LensContext<F> context,
            LensProjectionContext accCtx, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {

        boolean addUnchangedValues = accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD;

        ObjectDelta<ShadowType> modifyDelta = consolidateValuesToModifyDelta(context, accCtx, addUnchangedValues, task, result);
        if (!ObjectDelta.isEmpty(modifyDelta)) {
            accCtx.swallowToSecondaryDelta(modifyDelta.getModifications());
        }
    }

    private <V extends PrismValue,D extends ItemDefinition> ItemDelta<V,D> consolidateItemWithSync(LensProjectionContext accCtx,
            @NotNull ItemDelta<V,D> delta, ValueMatcher<?> valueMatcher) {
        if (delta instanceof PropertyDelta<?>) {
            //noinspection unchecked
            return (ItemDelta<V,D>) consolidatePropertyWithSync(accCtx, (PropertyDelta) delta, valueMatcher);
        } else {
            return delta;
        }
    }

    /**
     * This method checks {@link com.evolveum.midpoint.prism.delta.PropertyDelta} created during consolidation with
     * account sync deltas. If changes from property delta are in account sync deltas than they must be removed,
     * because they already had been applied (they came from sync, already happened).
     *
     * @param accCtx current account sync context
     * @param delta  new delta created during consolidation process
     * @return method return updated delta, or null if delta was empty after filtering (removing unnecessary values).
     */
    private <T> PropertyDelta<T> consolidatePropertyWithSync(LensProjectionContext accCtx,
            @NotNull PropertyDelta<T> delta, ValueMatcher<T> valueMatcher) {
        ObjectDelta<ShadowType> syncDelta = accCtx.getSyncDelta();
        if (syncDelta == null) {
            return consolidateWithSyncAbsolute(accCtx, delta, valueMatcher);
        }

        PropertyDelta<T> alreadyDoneDelta = syncDelta.findPropertyDelta(delta.getPath());
        if (alreadyDoneDelta == null) {
            return delta;
        }

        cleanupValues(delta.getValuesToAdd(), alreadyDoneDelta, valueMatcher);
        cleanupValues(delta.getValuesToDelete(), alreadyDoneDelta, valueMatcher);

        if (delta.getValues(Object.class).isEmpty()) {
            return null;
        }

        return delta;
    }

    /**
     * This method consolidate property delta against account absolute state which came from sync (not as delta)
     *
     * @param accCtx
     * @param delta
     * @return method return updated delta, or null if delta was empty after filtering (removing unnecessary values).
     */
    private <T> PropertyDelta<T> consolidateWithSyncAbsolute(LensProjectionContext accCtx, PropertyDelta<T> delta,
            ValueMatcher<T> valueMatcher) {
        if (delta == null || accCtx.getObjectCurrent() == null) {
            return delta;
        }

        PrismObject<ShadowType> absoluteAccountState = accCtx.getObjectCurrent();
        PrismProperty<T> absoluteProperty = absoluteAccountState.findProperty(delta.getPath());
        if (absoluteProperty == null) {
            return delta;
        }

        cleanupAbsoluteValues(delta.getValuesToAdd(), true, absoluteProperty, valueMatcher);
        cleanupAbsoluteValues(delta.getValuesToDelete(), false, absoluteProperty, valueMatcher);

        if (delta.getValues(Object.class).isEmpty()) {
            return null;
        }

        return delta;
    }

    /**
     * Method removes values from property delta values list (first parameter).
     *
     * @param values   collection with {@link PrismPropertyValue} objects to add or delete (from {@link PropertyDelta}
     * @param adding   if true we removing {@link PrismPropertyValue} from {@link Collection} values parameter if they
     *                 already are in {@link PrismProperty} parameter. Otherwise we're removing {@link PrismPropertyValue}
     *                 from {@link Collection} values parameter if they already are not in {@link PrismProperty} parameter.
     * @param property property with absolute state
     */
    private <T> void cleanupAbsoluteValues(Collection<PrismPropertyValue<T>> values, boolean adding, PrismProperty<T> property,
            ValueMatcher<T> valueMatcher) {
        if (values == null) {
            return;
        }

        Iterator<PrismPropertyValue<T>> iterator = values.iterator();
        while (iterator.hasNext()) {
            PrismPropertyValue<T> value = iterator.next();
            if (adding && valueMatcher.hasRealValue(property,value)) {
                iterator.remove();
            }

            if (!adding && !valueMatcher.hasRealValue(property,value)) {
                iterator.remove();
            }
        }
    }

    /**
     * Simple util method which checks property values against already done delta.
     *
     * @param values           collection which has to be filtered
     * @param alreadyDoneDelta already applied delta from sync
     */
    private <T> void cleanupValues(Collection<PrismPropertyValue<T>> values, PropertyDelta<T> alreadyDoneDelta,
            ValueMatcher<T> valueMatcher) {
        if (values == null) {
            return;
        }

        Iterator<PrismPropertyValue<T>> iterator = values.iterator();
        while (iterator.hasNext()) {
            PrismPropertyValue<T> valueToAdd = iterator.next();
            if (valueMatcher.isRealValueToAdd(alreadyDoneDelta, valueToAdd)) {
                iterator.remove();
            }
        }
    }

    private <F extends FocusType> void squeezeAll(LensContext<F> context, LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        // "Squeeze" all the relevant mappings into a data structure that we can process conveniently. We want to have all the
        // (meta)data about relevant for a specific attribute in one data structure, not spread over several account constructions.
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> squeezedAttributes =
                squeeze(projCtx, construction -> (Collection)construction.getAttributeMappings());
        projCtx.setSqueezedAttributes(squeezedAttributes);

        Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> squeezedAssociations =
                squeeze(projCtx, construction -> construction.getAssociationMappings());
        projCtx.setSqueezedAssociations(squeezedAssociations);

        // Association values in squeezed associations do not contain association name attribute.
        // It is hacked-in later for use in this class, but not for other uses (e.g. in ReconciliationProcessor).
        // So, we do it here - once and for all.
        if (!squeezedAssociations.isEmpty()) {
            fillInAssociationNames(squeezedAssociations);
        }

        EvaluatedConstructionMappingExtractor<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>,F> auxiliaryObjectClassExtractor =
            evaluatedConstruction -> {
                PrismValueDeltaSetTripleProducer<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>> prod = new PrismValueDeltaSetTripleProducer<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>>() {
                    @Override
                    public QName getMappingQName() {
                        return ShadowType.F_AUXILIARY_OBJECT_CLASS;
                    }
                    @Override
                    public PrismValueDeltaSetTriple<PrismPropertyValue<QName>> getOutputTriple() {
                        PrismValueDeltaSetTriple<PrismPropertyValue<QName>> triple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
                        if (evaluatedConstruction.getConstruction().getAuxiliaryObjectClassDefinitions() != null) {
                            for (ResourceObjectDefinition auxiliaryObjectClassDefinition: evaluatedConstruction.getConstruction().getAuxiliaryObjectClassDefinitions()) {
                                triple.addToZeroSet(prismContext.itemFactory().createPropertyValue(auxiliaryObjectClassDefinition.getTypeName()));
                            }
                        }
                        return triple;
                    }
                    @Override
                    public MappingStrengthType getStrength() {
                        return MappingStrengthType.STRONG;
                    }
                    @Override
                    public PrismValueDeltaSetTripleProducer<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>> clone() {
                        return this;
                    }
                    @Override
                    public boolean isExclusive() {
                        return false;
                    }
                    @Override
                    public boolean isAuthoritative() {
                        return true;
                    }
                    @Override
                    public boolean isSourceless() {
                        return false;
                    }
                    @Override
                    public String getIdentifier() {
                        return null;
                    }
                    @Override
                    public boolean isPushChanges() {
                        return false;
                    }
                    @Override
                    public String toHumanReadableDescription() {
                        return "auxiliary object class construction " + evaluatedConstruction;
                    }
                    @Override
                    public String toString() {
                        return "extractor(" + toHumanReadableDescription() +")";
                    }
                };
                Collection<PrismValueDeltaSetTripleProducer<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>>> col = new ArrayList<>(1);
                col.add(prod);
                return col;
            };

        Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses =
                squeeze(projCtx, auxiliaryObjectClassExtractor);
        projCtx.setSqueezedAuxiliaryObjectClasses(squeezedAuxiliaryObjectClasses);
    }

    private <V extends PrismValue, D extends ItemDefinition, F extends FocusType> Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeeze(
            LensProjectionContext projCtx, EvaluatedConstructionMappingExtractor<V,D,F> extractor)
            throws SchemaException, ConfigurationException {
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap = new HashMap<>();
        if (projCtx.getEvaluatedAssignedConstructionDeltaSetTriple() != null) {
            squeezeMappingsFromConstructionTriple(squeezedMap, projCtx.getEvaluatedAssignedConstructionDeltaSetTriple(),
                    extractor, projCtx.getAssignmentPolicyEnforcementMode());
        }
        if (projCtx.getEvaluatedPlainConstruction() != null) {
            // The plus-minus-zero status of outbound account construction is determined by the type of account delta
            if (projCtx.isAdd()) {
                squeezeMappingsFromConstructionNonMinusToPlus(squeezedMap, projCtx.getEvaluatedPlainConstruction(), extractor, AssignmentPolicyEnforcementType.RELATIVE);
            } else if (projCtx.isDelete()) {
                squeezeMappingsFromConstructionNonMinusToMinus(squeezedMap, projCtx.getEvaluatedPlainConstruction(), extractor, AssignmentPolicyEnforcementType.RELATIVE);
            } else {
                squeezeMappingsFromConstructionStraight(squeezedMap, projCtx.getEvaluatedPlainConstruction(), extractor, AssignmentPolicyEnforcementType.RELATIVE);
            }
        }
        return squeezedMap;
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionTriple(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            DeltaSetTriple<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionDeltaSetTriple, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        // Zero account constructions go normally, plus to plus, minus to minus
        squeezeMappingsFromEvaluatedAccountConstructionSetStraight(squeezedMap, evaluatedConstructionDeltaSetTriple.getZeroSet(), extractor, enforcement);
        // Plus accounts: zero and plus values go to plus
        squeezeMappingsFromAccountConstructionSetNonminusToPlus(squeezedMap, evaluatedConstructionDeltaSetTriple.getPlusSet(), extractor, enforcement);
        // Minus accounts: all values go to minus
        squeezeMappingsFromConstructionSetAllToMinus(squeezedMap, evaluatedConstructionDeltaSetTriple.getMinusSet(), extractor, enforcement);

        // Why all values in the last case: imagine that mapping M evaluated to "minus: A" on delta D.
        // The mapping itself is in minus set, so it disappears when delta D is applied. Therefore, value of A
        // was originally produced by the mapping M, and the mapping was originally active. So originally there was value of A
        // present, and we have to remove it. See MID-3325 / TestNullAttribute story.
        //
        // The same argument is valid for zero set of mapping output.
        //
        // Finally, the plus set of mapping output goes to resulting minus just for historical reasons... it was implemented
        // in this way for a long time. It seems to be unnecessary but also harmless. So let's keep it there, for now.
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedAccountConstructionSetStraight(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (evaluatedConstructionSet == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction: evaluatedConstructionSet) {
            squeezeMappingsFromEvaluatedConstructionStraight(squeezedMap, evaluatedConstruction, extractor, enforcement);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromAccountConstructionSetNonminusToPlus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (evaluatedConstructionSet == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction: evaluatedConstructionSet) {
            squeezeMappingsFromEvaluatedConstructionNonminusToPlus(squeezedMap, evaluatedConstruction, extractor, enforcement);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionSetNonminusToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (evaluatedConstructionSet == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction: evaluatedConstructionSet) {
            squeezeMappingsFromEvaluatedConstructionNonminusToMinus(squeezedMap, evaluatedConstruction, extractor, enforcement);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionSetAllToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (evaluatedConstructionSet == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction: evaluatedConstructionSet) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, evaluatedConstruction, extractor, enforcement);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionStraight(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            PlainResourceObjectConstruction<AH> construction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        DeltaSetTriple<EvaluatedPlainResourceObjectConstructionImpl<AH>> evaluatedConstructionTriple = construction.getEvaluatedConstructionTriple();
        if (evaluatedConstructionTriple == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getZeroSet()) {
            squeezeMappingsFromEvaluatedConstructionStraight(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getPlusSet()) {
            squeezeMappingsFromEvaluatedConstructionNonminusToPlus(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getMinusSet()) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        /////////////
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionNonMinusToPlus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            PlainResourceObjectConstruction<AH> construction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }

        DeltaSetTriple<EvaluatedPlainResourceObjectConstructionImpl<AH>> evaluatedConstructionTriple = construction.getEvaluatedConstructionTriple();
        if (evaluatedConstructionTriple == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getZeroSet()) {
            squeezeMappingsFromEvaluatedConstructionNonminusToPlus(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getPlusSet()) {
            squeezeMappingsFromEvaluatedConstructionNonminusToPlus(squeezedMap, eConstruction, extractor, enforcement);
        }
        // Ignore minus set
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionNonMinusToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            PlainResourceObjectConstruction<AH> construction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }

        DeltaSetTriple<EvaluatedPlainResourceObjectConstructionImpl<AH>> evaluatedConstructionTriple = construction.getEvaluatedConstructionTriple();
        if (evaluatedConstructionTriple == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getZeroSet()) {
            squeezeMappingsFromEvaluatedConstructionNonminusToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getPlusSet()) {
            squeezeMappingsFromEvaluatedConstructionNonminusToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        // Ignore minus set
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionAllToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            ResourceObjectConstruction<AH, ? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> construction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        DeltaSetTriple<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionTriple = construction.getEvaluatedConstructionTriple();
        if (evaluatedConstructionTriple == null) {
            return;
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getZeroSet()) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getPlusSet()) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getMinusSet()) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionStraight(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        for (PrismValueDeltaSetTripleProducer<V, D> mapping: extractor.getMappings(evaluatedConstruction)) {
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getMappingQName();
            DeltaSetTriple<ItemValueWithOrigin<V,D>> squeezeTriple = getSqueezeMapTriple(squeezedMap, name);
            convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
            convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getPlusSet(), mapping, evaluatedConstruction);
            if (enforcement == AssignmentPolicyEnforcementType.POSITIVE) {
                convertSqueezeSet(vcTriple.getMinusSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
            } else {
                convertSqueezeSet(vcTriple.getMinusSet(), squeezeTriple.getMinusSet(), mapping, evaluatedConstruction);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionNonminusToPlus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        for (PrismValueDeltaSetTripleProducer<V, D> mapping: extractor.getMappings(evaluatedConstruction)) {
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getMappingQName();
            DeltaSetTriple<ItemValueWithOrigin<V,D>> squeezeTriple = getSqueezeMapTriple(squeezedMap, name);
            convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getPlusSet(), mapping, evaluatedConstruction);
            convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getPlusSet(), mapping, evaluatedConstruction);
            // Ignore minus set
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionNonminusToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D,AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        for (PrismValueDeltaSetTripleProducer<V, D> mapping: extractor.getMappings(evaluatedConstruction)) {
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getMappingQName();
            DeltaSetTriple<ItemValueWithOrigin<V,D>> squeezeTriple
                    = getSqueezeMapTriple(squeezedMap, name);
            if (enforcement == AssignmentPolicyEnforcementType.POSITIVE) {
                convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
                convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
            } else {
                convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getMinusSet(), mapping, evaluatedConstruction);
                convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getMinusSet(), mapping, evaluatedConstruction);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionAllToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        for (PrismValueDeltaSetTripleProducer<V, D> mapping: extractor.getMappings(evaluatedConstruction)) {
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getMappingQName();
            DeltaSetTriple<ItemValueWithOrigin<V,D>> squeezeTriple = getSqueezeMapTriple(squeezedMap, name);
            if (enforcement == AssignmentPolicyEnforcementType.POSITIVE) {
                convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
                convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
                convertSqueezeSet(vcTriple.getMinusSet(), squeezeTriple.getZeroSet(), mapping, evaluatedConstruction);
            } else {
                convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getMinusSet(), mapping, evaluatedConstruction);
                convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getMinusSet(), mapping, evaluatedConstruction);
                convertSqueezeSet(vcTriple.getMinusSet(), squeezeTriple.getMinusSet(), mapping, evaluatedConstruction);
            }
        }
    }


    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> void convertSqueezeSet(Collection<V> fromSet,
            Collection<ItemValueWithOrigin<V,D>> toSet,
            PrismValueDeltaSetTripleProducer<V, D> mapping, EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction) {
        if (fromSet != null) {
            for (V from: fromSet) {
                ItemValueWithOrigin<V,D> pvwo = new ItemValueWithOrigin<>(from, mapping, evaluatedConstruction.getConstruction());
                toSet.add(pvwo);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> DeltaSetTriple<ItemValueWithOrigin<V,D>> getSqueezeMapTriple(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap, QName itemName) {
        DeltaSetTriple<ItemValueWithOrigin<V,D>> triple = squeezedMap.get(itemName);
        if (triple == null) {
            triple = prismContext.deltaFactory().createDeltaSetTriple();
            squeezedMap.put(itemName, triple);
        }
        return triple;
    }

    /**
     * TODO
     */
    <F extends FocusType> void consolidateValuesPostRecon(
            LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, ConfigurationException {

        //account was deleted, no changes are needed.
        if (wasProjectionDeleted(projCtx)) {
            return;
        }

        SynchronizationPolicyDecision policyDecision = projCtx.getSynchronizationPolicyDecision();

        if (policyDecision == SynchronizationPolicyDecision.DELETE) {
            return;
        }

        if (!projCtx.hasFullShadow()) {
            return;
        }

        boolean addUnchangedValues = projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD;

        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.MODIFY);
        objectDelta.setOid(projCtx.getOid());

        ObjectDelta<ShadowType> existingDelta = projCtx.getSummaryDelta(); // TODO check this

        ResourceObjectDefinition rOcDef = projCtx.getCompositeObjectDefinition();

        LOGGER.trace("Definition for {} post-recon consolidation:\n{}", projCtx.getKey(), rOcDef.debugDumpLazily());

        consolidateAttributes(projCtx, addUnchangedValues, rOcDef, objectDelta, existingDelta, StrengthSelector.WEAK_ONLY, result);
        consolidateAssociations(context, projCtx, addUnchangedValues, rOcDef, objectDelta, existingDelta, StrengthSelector.WEAK_ONLY, result);

        if (objectDelta.isEmpty()) {
            return;
        }
        projCtx.swallowToSecondaryDelta(objectDelta.getModifications());
    }
}
