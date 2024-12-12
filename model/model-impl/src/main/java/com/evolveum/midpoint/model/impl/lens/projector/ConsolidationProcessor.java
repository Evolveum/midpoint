/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.*;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedPlainResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.PlainResourceObjectConstruction;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Coverts delta set triples to secondary account deltas (before/after reconciliation).
 *
 * - {@link #consolidateValues(LensProjectionContext, Task, OperationResult)}
 * - {@link #consolidateValuesPostRecon(LensProjectionContext, Task, OperationResult)}
 *
 * Uses {@link IvwoConsolidator} to do the computation.
 *
 * @author Radovan Semancik
 * @author lazyman
 */
@Component
public class ConsolidationProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ConsolidationProcessor.class);

    private static final String OP_CONSOLIDATE_VALUES = ConsolidationProcessor.class.getName() + ".consolidateValues";
    private static final String OP_CONSOLIDATE_VALUES_POST_RECON = ConsolidationProcessor.class.getName() + ".consolidateValuesPostRecon";
    private static final String OP_CONSOLIDATE_ITEM = ConsolidationProcessor.class.getName() + ".consolidateItem";

    @Autowired private ContextLoader contextLoader;
    @Autowired private PrismContext prismContext;

    /** Converts delta set triples to a secondary account deltas. */
    @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
    void consolidateValues(LensProjectionContext projCtx, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
            //todo filter changes which were already in account sync delta

        OperationResult result = parentResult.subresult(OP_CONSOLIDATE_VALUES)
                .setMinor()
                .build();
        try {
            if (isDeletion(projCtx)) {
                return;
            }

            // This is ADD, KEEP, UNLINK or null. All are in fact the same as KEEP

            squeezeAll(projCtx);

            loadFullShadowIfNeeded(projCtx, task, result);
            doConsolidation(projCtx, true, task, result);

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** Converts delta set triples to a secondary account deltas after the reconciliation is done (for weak mappings). */
    void consolidateValuesPostRecon(LensProjectionContext projCtx, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        OperationResult result = parentResult.subresult(OP_CONSOLIDATE_VALUES_POST_RECON)
                .setMinor()
                .build();
        try {
            if (isDeletion(projCtx)) {
                return;
            }
            doConsolidation(projCtx, false, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void loadFullShadowIfNeeded(LensProjectionContext projCtx, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        // Do not automatically load the full projection now. Even if we have weak mapping.
        // That may be a waste of resources if the weak mapping results in no change anyway.
        // Let's be very very lazy about fetching the account from the resource.
        if (hasUnsatisfiedActiveWeakMapping(projCtx, false)
                || hasUnsatisfiedActiveWeakMapping(projCtx, true)
                || hasUnsatisfiedActiveStrongMapping(projCtx, false)
                || hasUnsatisfiedActiveStrongMapping(projCtx, true)) {
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

            // TODO consider removing this code - the shadow should be already loaded for strong/weak mappings.
            //  Unless special cases like it doesn't exist yet (add, simulation, broken) - but for that cases, the
            //  repeated loading is useless anyway.
            contextLoader.loadFullShadow(projCtx, "weak or strong mapping", task, result);
        }
    }

    private void doConsolidation(LensProjectionContext projCtx, boolean beforeReconciliation, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException {

        if (projCtx.isBroken()) {
            LOGGER.trace("Projection is broken, no consolidation will be done");
            return;
        }

        var selector = beforeReconciliation ?
                projCtx.isAdd() ? StrengthSelector.ALL : StrengthSelector.ALL_EXCEPT_WEAK :
                StrengthSelector.WEAK_ONLY;

        // We use delta (and not a plain list) because of delta merging functionality
        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFactory().object().create(ShadowType.class, ChangeType.MODIFY);

        ObjectDelta<ShadowType> existingDelta = projCtx.getSummaryDelta(); // TODO check this
        LOGGER.trace("Existing delta:\n{}", existingDelta);

        ResourceObjectDefinition objectDef;
        if (beforeReconciliation) {
            objectDef = consolidateAuxiliaryObjectClasses(projCtx, objectDelta, result);
            LOGGER.trace("Definition for {} consolidation:\n{}", projCtx.getKey(), objectDef.debugDumpLazily());
        } else {
            // We assume there are no weak mappings for auxiliary object classes.
            objectDef = projCtx.getCompositeObjectDefinitionRequired();
        }

        consolidateAttributes(projCtx, objectDef, objectDelta, existingDelta, selector, task, result);
        consolidateAssociations(projCtx, objectDef, objectDelta, existingDelta, selector, task, result);

        projCtx.swallowToSecondaryDelta(
                objectDelta.getModifications());

        projCtx.checkConsistenceIfNeeded();
    }

    private boolean isDeletion(LensProjectionContext projCtx) {
        if (ObjectDelta.isDelete(projCtx.getSyncDelta())) {
            LOGGER.trace("Object was deleted on resource, no consolidation is necessary");
            return true;
        }
        if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE) {
            LOGGER.trace("Policy decision is DELETE, no consolidation is necessary");
            return true;
        }
        return false;
    }

    private ResourceObjectDefinition consolidateAuxiliaryObjectClasses(
            LensProjectionContext projCtx,
            ObjectDelta<ShadowType> objectDelta,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {
        ProjectionContextKey key = projCtx.getKey();
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses = projCtx.getSqueezedAuxiliaryObjectClasses();

        // AUXILIARY OBJECT CLASSES
        PrismPropertyDefinition<QName> auxiliaryObjectClassPropertyDef = projCtx.getObjectDefinition().findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        PropertyDelta<QName> auxiliaryObjectClassAPrioriDelta = null;
        ResourceSchema resourceSchema = projCtx.getResourceSchema();
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
                ResourceObjectDefinition auxOcDef = resourceSchema.findDefinitionForObjectClass(auxObjectClassName);
                if (auxOcDef == null) {
                    LOGGER.error("Auxiliary object class definition {} for {} not found in the schema, but it should be there, dumping context:\n{}",
                            auxObjectClassName, key, projCtx.getLensContext().debugDump());
                    throw new IllegalStateException("Auxiliary object class definition " + auxObjectClassName + " for "+ key + " not found in the context, but it should be there");
                }
                auxOcDefs.add(auxOcDef);
            }

            //noinspection unchecked,rawtypes
            try (IvwoConsolidator<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>, ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>
                    consolidator = new IvwoConsolidatorBuilder()
                    .itemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS)
                    .ivwoTriple(ivwoTriple)
                    .itemDefinition(auxiliaryObjectClassPropertyDef)
                    .aprioriItemDelta(auxiliaryObjectClassAPrioriDelta)
                    .itemDeltaExists(!ItemDelta.isEmpty(auxiliaryObjectClassAPrioriDelta)) // TODO
                    .itemContainer(projCtx.getObjectNew())
                    .equalsChecker(null)
                    .addUnchangedValues(projCtx.isSynchronizationDecisionAdd())
                    .addUnchangedValuesExceptForNormalMappings(true) // todo
                    .existingItemKnown(projCtx.isSynchronizationDecisionAdd() || projCtx.isAuxiliaryObjectClassPropertyLoaded())
                    .isExclusiveStrong(false)
                    .contextDescription(key.toHumanReadableDescription())
                    .strengthSelector(StrengthSelector.ALL_EXCEPT_WEAK)
                    .result(result)
                    .build()) {

                // TODO what about setting existing item?

                PropertyDelta<QName> propDelta = (PropertyDelta<QName>) consolidator.consolidateToDeltaNoMetadata();

                LOGGER.trace("Auxiliary object class delta:\n{}", propDelta.debugDumpLazily());

                if (!propDelta.isEmpty()) {
                    objectDelta.addModification(propDelta);
                }
            }
        }

        return CompositeObjectDefinition.of(
                projCtx.getStructuralObjectDefinitionRequired(),
                auxOcDefs);
    }

    private void consolidateAttributes(
            LensProjectionContext projCtx,
            ResourceObjectDefinition rOcDef,
            ObjectDelta<ShadowType> objectDelta,
            ObjectDelta<ShadowType> existingDelta,
            StrengthSelector strengthSelector,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {
        var squeezedAttributes = projCtx.getSqueezedAttributes();
        // Iterate and process each attribute separately. Now that we have squeezed the data we can process each attribute just
        // with the data in ItemValueWithOrigin triples.
        for (var entry : squeezedAttributes.entrySet()) {
            QName attributeName = entry.getKey();
            var triple = entry.getValue();
            var propDelta = consolidateAttribute(
                    rOcDef, projCtx.getKey(), existingDelta, projCtx,
                    attributeName, triple, strengthSelector, task, result);
            if (propDelta != null) {
                objectDelta.addModification(propDelta);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V, D> consolidateAttribute(
            ResourceObjectDefinition rOcDef, ProjectionContextKey key, ObjectDelta<ShadowType> existingDelta,
            LensProjectionContext projCtx, QName itemName,
            DeltaSetTriple<ItemValueWithOrigin<?, ?>> triple,
            StrengthSelector strengthSelector, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        if (triple == null || triple.isEmpty()) {
            return null;
        }

        var attributeDef = triple.getAnyValue().getConstruction().findAttributeDefinition(itemName);

        ItemPath itemPath = ItemPath.create(ShadowType.F_ATTRIBUTES, itemName);

        if (attributeDef.isIgnored(LayerType.MODEL)) {
            LOGGER.trace("Skipping consolidation of attribute {} because it is ignored", itemName);
            return null;
        }
        if (!attributeDef.isVisible(task.getExecutionMode())) {
            LOGGER.trace("Skipping consolidation of attribute {} because it is not visible in current execution mode",
                    attributeDef);
            return null;
        }
        var equalsChecker = AttributeEqualsCheckerFactory.checkerFor(attributeDef);

        //noinspection unchecked,rawtypes
        return consolidateItem(
                rOcDef, key, existingDelta, projCtx, attributeDef.isExclusiveStrong(),
                itemPath, (D) attributeDef, (DeltaSetTriple) triple, equalsChecker, strengthSelector,
                "attribute "+itemName, result);
    }

    private void consolidateAssociations(
            LensProjectionContext projCtx,
            ResourceObjectDefinition objectDef,
            ObjectDelta<ShadowType> objectDelta,
            ObjectDelta<ShadowType> existingDelta,
            StrengthSelector strengthSelector,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {
        for (var entry : projCtx.getSqueezedAssociations().entrySet()) {
            QName associationName = entry.getKey();
            var triple = entry.getValue();
            var idemDelta =
                    consolidateAssociation(
                            objectDef, projCtx.getKey(), existingDelta, projCtx, associationName,
                            triple, strengthSelector, task, result);
            if (idemDelta != null) {
                objectDelta.addModification(idemDelta);
            }
        }
    }

    private ItemDelta<?, ?> consolidateAssociation(
            ResourceObjectDefinition objectDef,
            ProjectionContextKey key,
            ObjectDelta<ShadowType> existingDelta,
            LensProjectionContext projCtx,
            QName associationName,
            DeltaSetTriple<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>> triple,
            StrengthSelector strengthSelector,
            Task task,
            OperationResult result) throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        var associationDef = objectDef.findAssociationDefinitionRequired(associationName);

        if (!associationDef.isVisible(task)) {
            LOGGER.trace("Skipping consolidation of association {} because it is not visible in current execution mode",
                    associationDef);
            return null;
        }

        return consolidateItem(
                objectDef, key, existingDelta,
                projCtx, false,
                ShadowType.F_ASSOCIATIONS.append(associationName),
                associationDef,
                triple, ShadowAssociationValue.semanticEqualsChecker(), strengthSelector,
                "association " + associationName, result);
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V,D> consolidateItem(
            ResourceObjectDefinition objectDef,
            ProjectionContextKey key,
            ObjectDelta<ShadowType> existingDelta,
            LensProjectionContext projCtx,
            boolean isExclusiveStrong,
            ItemPath itemPath,
            D itemDefinition,
            DeltaSetTriple<ItemValueWithOrigin<V, D>> triple,
            EqualsChecker<V> equalsChecker,
            StrengthSelector strengthSelector,
            String itemDesc,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

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

            var addUnchangedValues = projCtx.isSynchronizationDecisionAdd();
            LOGGER.trace("CONSOLIDATE {}\n  ({}) addUnchangedValues={}, forceAddUnchangedValues={}",
                    itemDesc, key, addUnchangedValues, forceAddUnchangedValues);

            ItemName itemName = itemDefinition.getItemName();
            boolean existingItemKnown;
            // FIXME When determining whether we know the attribute value for accounts-to-be-created, the condition
            //  "isSynchronizationDecisionAdd" is not precise enough! It works for attributes that are not played with
            //  by the resource. But attributes that are created by the resource, like LDAP uid (determined from DN),
            //  are not known at this point. The current solution continues with the pre-4.9 tradition (where hasFullShadow
            //  was used), but it is not ideal. We should have a more precise way to determine whether the value is known.
            //
            // TODO What are we risking with the current solution?
            //  If we have some (add/delete) deltas for uid-like attributes, they may produce duplicate operations (for
            //  the "add" case) or they may be lost as phantom ones (for the "delete" case).
            if (itemDefinition instanceof ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition) {
                // The "isIdentifier" condition is a pre-4.9 legacy. At this place we consider identifiers to be always available
                // (even if the cached shadows use policy is "fresh"). This is as it was before 4.9.
                existingItemKnown =
                        projCtx.isSynchronizationDecisionAdd()
                                || projCtx.isAttributeLoaded(itemName, attributeDefinition)
                                || objectDef.isIdentifier(itemDefinition.getItemName());
            } else {
                assert itemDefinition instanceof ShadowAssociationDefinition;
                existingItemKnown =
                        projCtx.isSynchronizationDecisionAdd()
                                || projCtx.isAssociationLoaded(itemName);
            }

            ItemDelta<V, D> itemDelta;
            // Use the consolidator to do the computation. It does most of the work.
            try (IvwoConsolidator<V,D,ItemValueWithOrigin<V,D>> consolidator = new IvwoConsolidatorBuilder<V,D,ItemValueWithOrigin<V, D>>()
                    .itemPath(itemPath)
                    .ivwoTriple(triple)
                    .itemDefinition(itemDefinition)
                    .aprioriItemDelta(aprioriItemDelta)
                    .itemDeltaExists(!ItemDelta.isEmpty(aprioriItemDelta))
                    .itemContainer(projCtx.getObjectNew())
                    .equalsChecker(equalsChecker)
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
            } else if (equalsChecker instanceof PropertyValueMatcher<?> propertyValueMatcher
                    && itemDelta instanceof PropertyDelta<?> propertyDelta) {
                // Also consider a synchronization delta (if it is present). This may filter out some deltas.
                //noinspection unchecked,rawtypes
                itemDelta = (ItemDelta<V, D>) consolidatePropertyWithSync(
                        projCtx, (PropertyDelta) propertyDelta, (PropertyValueMatcher) propertyValueMatcher);
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

    private boolean hasUnsatisfiedActiveWeakMapping(LensProjectionContext projCtx, boolean associations)
            throws SchemaException, ConfigurationException {
        for (var entry : (associations ? projCtx.getSqueezedAssociations() : projCtx.getSqueezedAttributes()).entrySet()) {
            if (isAlreadyLoadedOrCanBeIgnored(entry, projCtx, associations)) {
                continue;
            }
            var hasWeak = false;
            var ivwoTriple = entry.getValue();
            for (var ivwo : ivwoTriple.getAllValues()) {
                var mapping = ivwo.getProducer();
                if (mapping.getStrength() == MappingStrengthType.WEAK) {
                    // We only care about mappings that change something. If the weak mapping is not
                    // changing anything then it will not be applied in this step anyway. Therefore
                    // there is no point in loading the real values just because there is such mapping.
                    // Note: we can be sure that we are NOT doing reconciliation. If we do reconciliation
                    // then we cannot get here in the first place (the projection is already loaded).
                    // TODO what about shadow caching? It's possible that we are doing reconciliation
                    //  and still be here... please check this
                    var outputTriple = mapping.getOutputTriple();
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
                    for (var ivwo: ivwoTriple.getMinusSet()) {
                        var outputTriple = ivwo.getProducer().getOutputTriple();
                        if (outputTriple != null && !outputTriple.isEmpty()) {
                            return true;
                        }
                    }
                }
                for (var ivwo: ivwoTriple.getNonNegativeValues()) {
                    var outputTriple = ivwo.getProducer().getOutputTriple();
                    if (outputTriple != null && outputTriple.hasMinusSet()) {
                        return true;
                    }
                }
                var projectionDelta = projCtx.getSummaryDelta(); // TODO check this
                if (projectionDelta != null) {
                    var aPrioriItemDelta = projectionDelta.findItemDelta(
                            ItemPath.create(associations ? ShadowType.F_ASSOCIATIONS : ShadowType.F_ATTRIBUTES, entry.getKey()));
                    if (aPrioriItemDelta != null && aPrioriItemDelta.isDelete()) {
                        return true;
                    }
                    if (aPrioriItemDelta != null && aPrioriItemDelta.isReplace() && aPrioriItemDelta.getValuesToReplace().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * We need to determine the attribute definition. The projection context may be unaware of the auxiliary object
     * definition where the attribute in question is defined.
     */
    private @Nullable ShadowAttributeDefinition<?, ?, ?, ?> getDefinitionFromIvwos(
            @NotNull Collection<? extends ItemValueWithOrigin<?, ?>> allIvwos) {
        for (var ivwo : allIvwos) {
            var itemDef = ivwo.getProducer().getTargetItemDefinition();
            if (itemDef instanceof ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition) {
                return attributeDefinition;
            }
        }
        return null;
    }

    private boolean hasUnsatisfiedActiveStrongMapping(LensProjectionContext projCtx, boolean associations)
            throws SchemaException, ConfigurationException {
        for (var entry : (associations ? projCtx.getSqueezedAssociations() : projCtx.getSqueezedAttributes()).entrySet()) {
            if (isAlreadyLoadedOrCanBeIgnored(entry, projCtx, associations)) {
                continue;
            }
            var ivwoTriple = entry.getValue();
            for (var ivwo: ivwoTriple.getAllValues()) {
                if (ivwo.isMappingStrong()) {
                    // Do not optimize for "nothing changed" case here. We want to make
                    // sure that the values of strong mappings are applied even if nothing
                    // has changed.
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAlreadyLoadedOrCanBeIgnored(
            Entry<QName, ? extends DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> entry,
            LensProjectionContext projCtx,
            boolean associations) throws SchemaException, ConfigurationException {
        var allIvwos = entry.getValue().getAllValues();
        if (allIvwos.isEmpty()) {
            return true;
        }
        if (associations) {
            return projCtx.isAssociationLoaded(entry.getKey());
        } else {
            return projCtx.isAttributeLoaded(entry.getKey(), getDefinitionFromIvwos(allIvwos));
        }
    }

    /**
     * This method checks {@link PropertyDelta} created during consolidation with account sync deltas.
     * If changes from property delta are in account sync deltas than they must be removed,
     * because they already had been applied (they came from sync, already happened).
     *
     * @param projCtx current account sync context
     * @param delta  new delta created during consolidation process
     * @return method return updated delta, or null if delta was empty after filtering (removing unnecessary values).
     */
    private <T> PropertyDelta<T> consolidatePropertyWithSync(
            LensProjectionContext projCtx,
            @NotNull PropertyDelta<T> delta, PropertyValueMatcher<T> valueMatcher) {
        ObjectDelta<ShadowType> syncDelta = projCtx.getSyncDelta();
        if (syncDelta == null) {
            return consolidateWithSyncAbsolute(projCtx, delta, valueMatcher);
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
     * @return method return updated delta, or null if delta was empty after filtering (removing unnecessary values).
     */
    private <T> PropertyDelta<T> consolidateWithSyncAbsolute(
            LensProjectionContext accCtx, PropertyDelta<T> delta,
            PropertyValueMatcher<T> valueMatcher) {
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
    private <T> void cleanupAbsoluteValues(
            Collection<PrismPropertyValue<T>> values, boolean adding, PrismProperty<T> property,
            PropertyValueMatcher<T> valueMatcher) {
        if (values == null) {
            return;
        }

        Iterator<PrismPropertyValue<T>> iterator = values.iterator();
        while (iterator.hasNext()) {
            PrismPropertyValue<T> value = iterator.next();
            if (adding && valueMatcher.hasRealValue(property, value)) {
                iterator.remove();
            }

            if (!adding && !valueMatcher.hasRealValue(property, value)) {
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
    private <T> void cleanupValues(
            Collection<PrismPropertyValue<T>> values, PropertyDelta<T> alreadyDoneDelta, PropertyValueMatcher<T> valueMatcher) {
        if (values != null) {
            values.removeIf(valueToAdd -> valueMatcher.isRealValueToAdd(alreadyDoneDelta, valueToAdd));
        }
    }

    /**
     * "Squeeze" all the relevant mappings (triple producers) into a data structure that we can process conveniently.
     * We want to have all the (meta)data relevant to a specific attribute in one data structure, not spread over
     * several resource object constructions.
     */
    private void squeezeAll(LensProjectionContext projCtx) throws SchemaException, ConfigurationException {

        projCtx.checkConsistenceIfNeeded();

        // TODO resolve this parameterization mess
        //noinspection unchecked,rawtypes
        projCtx.setSqueezedAttributes(
                (Map) squeeze(projCtx, construction -> construction.getAttributeTripleProducers()));

        projCtx.setSqueezedAssociations(
                squeeze(projCtx, construction -> construction.getAssociationTripleProducers()));

        projCtx.setSqueezedAuxiliaryObjectClasses(
                squeeze(projCtx, construction -> List.of(getTrivialAuxObjectClassTripleProducer(construction))));
    }

    private <F extends FocusType, V extends PrismValue, D extends ItemDefinition<?>> @NotNull PrismValueDeltaSetTripleProducer<V, D>
    getTrivialAuxObjectClassTripleProducer(EvaluatedResourceObjectConstructionImpl<F, ?> evaluatedConstruction) {
        //noinspection MethodDoesntCallSuperMethod
        return new PrismValueDeltaSetTripleProducer<>() {
            @Override
            public QName getTargetItemName() {
                return ShadowType.F_AUXILIARY_OBJECT_CLASS;
            }

            @Override
            public PrismValueDeltaSetTriple<V> getOutputTriple() {
                PrismValueDeltaSetTriple<PrismPropertyValue<QName>> triple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
                for (ResourceObjectDefinition auxObjectClassDefinition :
                        emptyIfNull(evaluatedConstruction.getConstruction().getAuxiliaryObjectClassDefinitions())) {
                    triple.addToZeroSet(prismContext.itemFactory().createPropertyValue(auxObjectClassDefinition.getTypeName()));
                }
                //noinspection unchecked
                return (PrismValueDeltaSetTriple<V>) triple;
            }

            @Override
            public @NotNull MappingStrengthType getStrength() {
                return MappingStrengthType.STRONG;
            }

            @Override
            public PrismValueDeltaSetTripleProducer<V, D> clone() {
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
            public boolean isEnabled() {
                return true;
            }

            @Override
            public @Nullable D getTargetItemDefinition() {
                return PrismContext.get().getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(ShadowType.class)
                        .findItemDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            }

            @Override
            public String toHumanReadableDescription() {
                return "auxiliary object class construction " + evaluatedConstruction;
            }

            @Override
            public String toString() {
                return "extractor(" + toHumanReadableDescription() + ")";
            }

            @Override
            public String debugDump(int indent) {
                return DebugUtil.debugDump(toString(), indent);
            }
        };
    }

    private <V extends PrismValue, D extends ItemDefinition<?>, F extends FocusType> Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeeze(
            LensProjectionContext projCtx, EvaluatedConstructionMappingExtractor<V,D,F> extractor)
            throws SchemaException, ConfigurationException {
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap = new HashMap<>();
        if (projCtx.getEvaluatedAssignedConstructionDeltaSetTriple() != null) {
            squeezeMappingsFromConstructionTriple(
                    squeezedMap, projCtx.getEvaluatedAssignedConstructionDeltaSetTriple(),
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

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionTriple(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            DeltaSetTriple<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionDeltaSetTriple, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        // Zero account constructions go normally, plus to plus, minus to minus
        squeezeMappingsFromEvaluatedAccountConstructionSetStraight(squeezedMap, evaluatedConstructionDeltaSetTriple.getZeroSet(), extractor, enforcement);
        // Plus accounts: zero and plus values go to plus
        squeezeMappingsFromAccountConstructionSetNonMinusToPlus(squeezedMap, evaluatedConstructionDeltaSetTriple.getPlusSet(), extractor);
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

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedAccountConstructionSetStraight(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        for (var evaluatedConstruction : emptyIfNull(evaluatedConstructionSet)) {
            squeezeMappingsFromEvaluatedConstructionStraight(squeezedMap, evaluatedConstruction, extractor, enforcement);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromAccountConstructionSetNonMinusToPlus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor) {
        for (var evaluatedConstruction : emptyIfNull(evaluatedConstructionSet)) {
            squeezeMappingsFromEvaluatedConstructionNonMinusToPlus(squeezedMap, evaluatedConstruction, extractor);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionSetAllToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            Collection<? extends EvaluatedResourceObjectConstructionImpl<AH, ?>> evaluatedConstructionSet, EvaluatedConstructionMappingExtractor<V,D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        for (var evaluatedConstruction : emptyIfNull(evaluatedConstructionSet)) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, evaluatedConstruction, extractor, enforcement);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionStraight(
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
            squeezeMappingsFromEvaluatedConstructionNonMinusToPlus(squeezedMap, eConstruction, extractor);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getMinusSet()) {
            squeezeMappingsFromEvaluatedConstructionAllToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        /////////////
    }

    @SuppressWarnings("SameParameterValue")
    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionNonMinusToPlus(
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
            squeezeMappingsFromEvaluatedConstructionNonMinusToPlus(squeezedMap, eConstruction, extractor);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getPlusSet()) {
            squeezeMappingsFromEvaluatedConstructionNonMinusToPlus(squeezedMap, eConstruction, extractor);
        }
        // Ignore minus set
    }

    @SuppressWarnings("SameParameterValue")
    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromConstructionNonMinusToMinus(
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
            squeezeMappingsFromEvaluatedConstructionNonMinusToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> eConstruction : evaluatedConstructionTriple.getPlusSet()) {
            squeezeMappingsFromEvaluatedConstructionNonMinusToMinus(squeezedMap, eConstruction, extractor, enforcement);
        }
        // Ignore minus set
    }

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionStraight(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        for (var mappingRaw : extractor.getMappings(evaluatedConstruction)) {
            //noinspection unchecked
            var mapping = (PrismValueDeltaSetTripleProducer<V, D>) mappingRaw;
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getTargetItemName();
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

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionNonMinusToPlus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D, AH> extractor) {
        for (var mappingRaw : extractor.getMappings(evaluatedConstruction)) {
            //noinspection unchecked
            PrismValueDeltaSetTripleProducer<V, D> mapping = (PrismValueDeltaSetTripleProducer<V, D>) mappingRaw;
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getTargetItemName();
            DeltaSetTriple<ItemValueWithOrigin<V,D>> squeezeTriple = getSqueezeMapTriple(squeezedMap, name);
            convertSqueezeSet(vcTriple.getZeroSet(), squeezeTriple.getPlusSet(), mapping, evaluatedConstruction);
            convertSqueezeSet(vcTriple.getPlusSet(), squeezeTriple.getPlusSet(), mapping, evaluatedConstruction);
            // Ignore minus set
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionNonMinusToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction, EvaluatedConstructionMappingExtractor<V,D,AH> extractor, AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        for (var mappingRaw: extractor.getMappings(evaluatedConstruction)) {
            //noinspection DuplicatedCode,unchecked
            PrismValueDeltaSetTripleProducer<V, D> mapping = (PrismValueDeltaSetTripleProducer<V, D>) mappingRaw;
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getTargetItemName();
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

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void squeezeMappingsFromEvaluatedConstructionAllToMinus(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V, D>>> squeezedMap,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction,
            EvaluatedConstructionMappingExtractor<V, D, AH> extractor,
            AssignmentPolicyEnforcementType enforcement) {
        if (enforcement == AssignmentPolicyEnforcementType.NONE) {
            return;
        }
        for (var mappingRaw : extractor.getMappings(evaluatedConstruction)) {
            //noinspection unchecked
            var mapping = (PrismValueDeltaSetTripleProducer<V, D>) mappingRaw;
            PrismValueDeltaSetTriple<V> vcTriple = mapping.getOutputTriple();
            if (vcTriple == null) {
                continue;
            }
            QName name = mapping.getTargetItemName();
            DeltaSetTriple<ItemValueWithOrigin<V, D>> squeezeTriple = getSqueezeMapTriple(squeezedMap, name);
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

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> void convertSqueezeSet(
            Collection<V> sourceSet,
            Collection<ItemValueWithOrigin<V, D>> targetSet,
            PrismValueDeltaSetTripleProducer<V, D> tripleProducer,
            EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction) {
        if (sourceSet != null) {
            for (V value : sourceSet) {
                targetSet.add(
                        new ItemValueWithOrigin<>(value, tripleProducer, evaluatedConstruction.getConstruction()));
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> DeltaSetTriple<ItemValueWithOrigin<V,D>> getSqueezeMapTriple(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<V,D>>> squeezedMap, QName itemName) {
        DeltaSetTriple<ItemValueWithOrigin<V,D>> triple = squeezedMap.get(itemName);
        if (triple == null) {
            triple = prismContext.deltaFactory().createDeltaSetTriple();
            squeezedMap.put(itemName, triple);
        }
        return triple;
    }
}
