/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision.DELETE;
import static com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision.UNLINK;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.filter;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Processor that reconciles the computed account and the real account. There
 * will be some deltas already computed from the other processors. This
 * processor will compare the "projected" state of the account after application
 * of the deltas to the actual (real) account with the result of the mappings.
 * The differences will be expressed as additional "reconciliation" deltas.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class)
public class ReconciliationProcessor implements ProjectorProcessor {

    @Autowired private ProvisioningService provisioningService;
    @Autowired PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private ClockworkMedic medic;
    @Autowired private ContextLoader contextLoader;

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);

    @ProcessorMethod
    <F extends FocusType> void processReconciliation(
            LensContext<F> context,
            LensProjectionContext projectionContext,
            String activityDescription,
            XMLGregorianCalendar ignoredNow,
            Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        processReconciliation(projectionContext, task, result);

        medic.traceContext(
                LOGGER, activityDescription, "projection reconciliation of " + projectionContext.getDescription(),
                false, context, false);
    }

    private void processReconciliation(LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        // Reconcile even if it was not explicitly requested and if we have full shadow
        // reconciliation is cheap if the shadow is already fetched therefore just do it
        boolean avoidCachedShadows = !projCtx.isCachedShadowsUseAllowed();
        if (!projCtx.isDoReconciliation() && !projCtx.isFullShadow() && avoidCachedShadows) {
            LOGGER.trace("Skipping reconciliation of {}: no doReconciliation and no full shadow (and cache use disallowed)",
                    projCtx.getHumanReadableName());
            return;
        }

        SynchronizationPolicyDecision policyDecision = projCtx.getSynchronizationPolicyDecision();
        if (policyDecision == DELETE || policyDecision == UNLINK) {
            LOGGER.trace("Skipping reconciliation of {}: decision={}", projCtx.getHumanReadableName(), policyDecision);
            return;
        }

        if (projCtx.getObjectCurrent() == null) {
            LOGGER.warn("Can't do reconciliation. Projection context doesn't contain current version of resource object.");
            return;
        }

        if (avoidCachedShadows) {
            // This is the pre-4.9 behavior, present here to improve compatible behavior
            contextLoader.loadFullShadowNoDiscovery(projCtx, "projection reconciliation", task, result);
            if (!projCtx.isFullShadow()) {
                LOGGER.trace("Full shadow is not available, skipping the reconciliation of {}", projCtx.getHumanReadableName());
                result.recordNotApplicable("Full shadow is not available");
                return;
            }
        }

        LOGGER.trace("Starting reconciliation of {}", projCtx.getHumanReadableName());

        reconcileAuxiliaryObjectClasses(projCtx, task, result);
        reconcileProjectionAttributes(projCtx, task, result);
        reconcileProjectionAssociations(projCtx, task, result);

        reconcileMissingAuxiliaryObjectClassAttributes(projCtx);

        projCtx.checkConsistenceIfNeeded();
    }

    private void reconcileAuxiliaryObjectClasses(LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        var squeezedAuxiliaryObjectClasses = projCtx.getSqueezedAuxiliaryObjectClasses();
        if (squeezedAuxiliaryObjectClasses == null || squeezedAuxiliaryObjectClasses.isEmpty()) {
            return;
        }

        if (!projCtx.isAuxiliaryObjectClassPropertyLoaded()) {
            if (!loadIfPossible(projCtx, "auxiliary object class", task, result)) {
                return;
            }
        }

        LOGGER.trace("Auxiliary object class reconciliation processing {}", projCtx.getHumanReadableName());

        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();
        PrismPropertyDefinition<QName> propDef = shadowNew.getDefinition().findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS);

        DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>> pvwoTriple =
                squeezedAuxiliaryObjectClasses.get(ShadowType.F_AUXILIARY_OBJECT_CLASS);

        Collection<ItemValueWithOrigin<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>>> shouldBePValues;
        if (pvwoTriple == null) {
            shouldBePValues = new ArrayList<>();
        } else {
            shouldBePValues = selectValidValues(pvwoTriple.getNonNegativeValues());
        }

        Collection<PrismPropertyValue<QName>> arePValues;
        PrismProperty<QName> propertyNew = shadowNew.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        if (propertyNew != null) {
            arePValues = propertyNew.getValues();
        } else {
            arePValues = new HashSet<>();
        }

        PropertyValueMatcher<QName> valueMatcher =
                PropertyValueMatcher.createDefaultMatcher(DOMUtil.XSD_QNAME, matchingRuleRegistry);

        boolean auxObjectClassChanged = false;

        for (var shouldBeValueWithOrigin : shouldBePValues) {
            PrismPropertyValue<QName> shouldBePValue = shouldBeValueWithOrigin.getItemValue();
            if (isNotInValues(valueMatcher, shouldBePValue, arePValues)) {
                auxObjectClassChanged = true;
                recordDelta(valueMatcher, projCtx, ItemPath.EMPTY_PATH, propDef, ModificationType.ADD, shouldBePValue,
                        shouldBeValueWithOrigin.getSource(), "it is given");
            }
        }

        if (!isTolerantAuxiliaryObjectClasses(projCtx)) {
            for (PrismPropertyValue<QName> isPValue : arePValues) {
                if (isNotInPvwoValues(valueMatcher, isPValue, shouldBePValues, true)) {
                    auxObjectClassChanged = true;
                    recordDelta(
                            valueMatcher, projCtx, ItemPath.EMPTY_PATH, propDef, ModificationType.DELETE,
                            isPValue, null, "it is not given");
                }
            }
        }

        if (auxObjectClassChanged) {
            projCtx.refreshAuxiliaryObjectClassDefinitions();
        }
    }

    private boolean isTolerantAuxiliaryObjectClasses(LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        var auxiliaryObjectClassMappingsBean = projCtx.getStructuralObjectDefinitionRequired().getAuxiliaryObjectClassMappings();
        if (auxiliaryObjectClassMappingsBean == null) {
            return false;
        }
        Boolean tolerant = auxiliaryObjectClassMappingsBean.isTolerant();
        return tolerant != null && tolerant;
    }

    /**
     * If auxiliary object classes changed, there may still be some attributes that were defined by the aux objectclasses
     * that were deleted. If these attributes are still around then delete them. Otherwise the delete of the aux object class
     * may fail.
     */
    private void reconcileMissingAuxiliaryObjectClassAttributes(LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        ObjectDelta<ShadowType> delta = projCtx.getCurrentDelta();
        if (delta == null) {
            return;
        }
        PropertyDelta<QName> auxOcDelta = delta.findPropertyDelta(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        if (auxOcDelta == null || auxOcDelta.isEmpty()) {
            return;
        }
        Collection<QName> deletedAuxObjectClassNames;
        PrismObject<ShadowType> objectCurrent = projCtx.getObjectCurrent();
        if (auxOcDelta.isReplace()) {
            if (objectCurrent == null) {
                return;
            }
            PrismProperty<QName> auxOcPropOld = objectCurrent.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            if (auxOcPropOld == null) {
                return;
            }
            Collection<QName> auxOcsOld = auxOcPropOld.getRealValues();
            Set<QName> auxOcsToReplace = PrismValueCollectionsUtil.getRealValuesOfCollection(auxOcDelta.getValuesToReplace());
            deletedAuxObjectClassNames = new ArrayList<>(auxOcsOld.size());
            for (QName auxOcOld: auxOcsOld) {
                if (!QNameUtil.contains(auxOcsToReplace, auxOcOld)) {
                    deletedAuxObjectClassNames.add(auxOcOld);
                }
            }
        } else {
            Collection<PrismPropertyValue<QName>> valuesToDelete = auxOcDelta.getValuesToDelete();
            if (valuesToDelete == null || valuesToDelete.isEmpty()) {
                return;
            }
            deletedAuxObjectClassNames = PrismValueCollectionsUtil.getRealValuesOfCollection(valuesToDelete);
        }
        LOGGER.trace("Deleted auxiliary object classes: {}", deletedAuxObjectClassNames);
        if (deletedAuxObjectClassNames.isEmpty()) {
            return;
        }

        List<QName> attributesToDelete = new ArrayList<>();
        String projHumanReadableName = projCtx.getHumanReadableName();
        ResourceSchema refinedResourceSchema = projCtx.getResourceSchema();
        ResourceObjectDefinition structuralObjectDefinition = projCtx.getStructuralObjectDefinitionRequired();
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = projCtx.getAuxiliaryObjectClassDefinitions();
        for (QName deleteAuxOcName: deletedAuxObjectClassNames) {
            ResourceObjectDefinition auxOcDef = refinedResourceSchema.findDefinitionForObjectClassRequired(deleteAuxOcName);
            for (ShadowSimpleAttributeDefinition<?> auxAttrDef: auxOcDef.getSimpleAttributeDefinitions()) {
                QName auxAttrName = auxAttrDef.getItemName();
                if (attributesToDelete.contains(auxAttrName)) {
                    continue;
                }
                var structuralAttrDef = structuralObjectDefinition.findAttributeDefinition(auxAttrName);
                if (structuralAttrDef == null) {
                    boolean found = false;
                    for (ResourceObjectDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
                        if (QNameUtil.contains(deletedAuxObjectClassNames, auxiliaryObjectClassDefinition.getTypeName())) {
                            continue;
                        }
                        var existingAuxAttrDef = auxiliaryObjectClassDefinition.findAttributeDefinition(auxAttrName);
                        if (existingAuxAttrDef != null) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        LOGGER.trace("Removing attribute {} because it is in the deleted object class {} and it is not defined by any current object class for {}",
                                auxAttrName, deleteAuxOcName, projHumanReadableName);
                        attributesToDelete.add(auxAttrName);
                    }
                }
            }
        }
        LOGGER.trace("Attributes to delete: {}", attributesToDelete);
        for (QName attrNameToDelete: attributesToDelete) {
            ShadowSimpleAttribute<Object> attrToDelete = ShadowUtil.getSimpleAttribute(objectCurrent, attrNameToDelete);
            if (attrToDelete == null || attrToDelete.isEmpty()) {
                continue;
            }
            PropertyDelta<Object> attrDelta = attrToDelete.createDelta();
            attrDelta.addValuesToDelete(PrismValueCollectionsUtil.cloneCollection(attrToDelete.getValues()));
            projCtx.swallowToSecondaryDelta(attrDelta);
        }
    }

    private void reconcileProjectionAttributes(LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        LOGGER.trace("Attribute reconciliation processing {}", projCtx.getHumanReadableName());

        boolean useCachedShadows = projCtx.isCachedShadowsUseAllowed();

        var squeezedAttributes = projCtx.getSqueezedAttributes();
        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

        var attributesContainer = shadowNew.findContainer(ShadowType.F_ATTRIBUTES);
        var attributeNames = MiscUtil.union(
                squeezedAttributes != null ? squeezedAttributes.keySet() : null,
                attributesContainer.getValue().getItemNames(),
                useCachedShadows ? projCtx.getCachedAttributesNames() : null);

        for (QName attrName : attributeNames) {
            reconcileProjectionAttribute(attrName, projCtx, squeezedAttributes, attributesContainer, task, result);
        }
    }

    private <T> void reconcileProjectionAttribute(
            QName attrName,
            LensProjectionContext projCtx,
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>, PrismPropertyDefinition<?>>>> squeezedAttributes,
            PrismContainer<?> attributesContainer,
            Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        LOGGER.trace("Attribute reconciliation processing attribute {}", attrName);

        var attrDef = projCtx.findAttributeDefinition(attrName);
        if (attrDef == null) {
            throw new SchemaException("No definition for attribute " + attrName + " in " + projCtx.getKey());
        }
        if (!(attrDef instanceof ShadowSimpleAttributeDefinition<?>)) {
            // we reconcile only simple attributes for now
            return;
        }
        //noinspection unchecked
        var simpleAttrDef = (ShadowSimpleAttributeDefinition<T>) attrDef;

        //noinspection unchecked,rawtypes
        DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> pvwoTriple =
                squeezedAttributes != null ? (DeltaSetTriple) squeezedAttributes.get(attrName) : null;

        if (attrDef.isIgnored(LayerType.MODEL)) {
            LOGGER.trace("Skipping reconciliation of attribute {} because it is ignored", attrName);
            return;
        }
        if (!attrDef.isVisible(task.getExecutionMode())) {
            LOGGER.trace("Skipping reconciliation of attribute {} because it is not visible in current execution mode", attrName);
            return;
        }
        PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
        if (limitations != null) {
            if (projCtx.isAdd() && !limitations.canAdd()) {
                LOGGER.trace("Skipping reconciliation of attribute {} because it is non-createable", attrName);
                return;
            }
            if (projCtx.isModify() && !limitations.canModify()) {
                LOGGER.trace("Skipping reconciliation of attribute {} because it is non-updateable", attrName);
                return;
            }
        }

        if (!projCtx.isAttributeLoaded(attrName)) {
            if (!loadIfPossible(projCtx, "attribute " + attrName, task, result)) {
                return;
            }
        }

        Collection<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> shouldBePValues;
        if (pvwoTriple == null) {
            shouldBePValues = new HashSet<>();
        } else {
            shouldBePValues = new HashSet<>(selectValidValues(pvwoTriple.getNonNegativeValues()));
        }

        // We consider values explicitly requested by user to be among "should be values".
        addPropValuesFromDelta(shouldBePValues, projCtx.getPrimaryDelta(), attrName);
        // But we DO NOT take values from sync delta (because they just reflect what's on the resource),
        // nor from secondary delta (because these got there from mappings).

        boolean hasStrongShouldBePValue = false;
        boolean hasOtherNonWeakValues = false;
        for (ItemValueWithOrigin<? extends PrismPropertyValue<T>,PrismPropertyDefinition<T>> shouldBePValue : shouldBePValues) {
            var mapping = shouldBePValue.getProducer();
            if (mapping != null) {
                if (mapping.isStrong()) {
                    hasStrongShouldBePValue = true;
                    hasOtherNonWeakValues = true;
                    break;
                }
                if (mapping.isNormal()) {
                    hasOtherNonWeakValues = true;
                }
            }
        }

        PrismProperty<T> attribute = attributesContainer.findProperty(ItemName.fromQName(attrName));
        Collection<PrismPropertyValue<T>> arePValues;
        if (attribute != null) {
            arePValues = attribute.getValues();
        } else {
            arePValues = new HashSet<>();
        }

        // Too loud :-)
//            if (LOGGER.isTraceEnabled()) {
//                StringBuilder sb = new StringBuilder();
//                sb.append("Reconciliation\nATTR: ").append(PrettyPrinter.prettyPrint(attrName));
//                sb.append("\n  Should be:");
//                for (ItemValueWithOrigin<?,?> shouldBePValue : shouldBePValues) {
//                    sb.append("\n    ");
//                    sb.append(shouldBePValue.getItemValue());
//                    PrismValueDeltaSetTripleProducer<?, ?> shouldBeMapping = shouldBePValue.getMapping();
//                    if (shouldBeMapping.getStrength() == MappingStrengthType.STRONG) {
//                        sb.append(" STRONG");
//                    }
//                    if (shouldBeMapping.getStrength() == MappingStrengthType.WEAK) {
//                        sb.append(" WEAK");
//                    }
//                    if (!shouldBePValue.isValid()) {
//                        sb.append(" INVALID");
//                    }
//                }
//                sb.append("\n  Is:");
//                for (PrismPropertyValue<Object> isPVal : arePValues) {
//                    sb.append("\n    ");
//                    sb.append(isPVal);
//                }
//                LOGGER.trace("{}", sb.toString());
//            }

        PropertyValueMatcher<T> valueMatcher = PropertyValueMatcher.createMatcher(simpleAttrDef, matchingRuleRegistry);

        PrismPropertyValue<T> prismValueToReplace = null;
        boolean hasRealValueToReplace = false;
        for (ItemValueWithOrigin<? extends PrismPropertyValue<T>,PrismPropertyDefinition<T>> shouldBePvwo : shouldBePValues) {
            PrismPropertyValue<T> shouldBePrismValue = shouldBePvwo.getItemValue();
            PrismValueDeltaSetTripleProducer<?,?> shouldBeMapping = shouldBePvwo.getProducer();
            if (shouldBeMapping == null) {
                LOGGER.trace("Skipping reconciliation of value {} of the attribute {}: no origin mapping",
                        shouldBePrismValue, attrDef.getItemName().getLocalPart());
                continue;
            }
            if (shouldBeMapping.getStrength() != MappingStrengthType.STRONG
                    && (!arePValues.isEmpty() || hasStrongShouldBePValue)) {
                // Weak or normal value and the attribute already has a value. Skip it.
                // We cannot override it as it might have been legally changed directly on the projection resource object
                LOGGER.trace("Skipping reconciliation of value {} of the attribute {}: the mapping is not strong",
                        shouldBePrismValue, attrDef.getItemName().getLocalPart());
                continue;
            }
            if (isNotInValues(valueMatcher, shouldBePrismValue, arePValues)) {
                if (attrDef.isSingleValue()) {
                    // It is quite possible that there are more shouldBePValues with equivalent real values but different 'context'.
                    // We don't want to throw an exception if real values are in fact equivalent.
                    // TODO generalize this a bit (e.g. also for multivalued items)
                    if (hasRealValueToReplace) {
                        if (matchPrismValue(shouldBePrismValue, prismValueToReplace, valueMatcher)) {
                            LOGGER.trace("Value to replace for {} is already set, skipping it: {}", attrName, prismValueToReplace);
                            continue;
                        } else {
                            String message = "Attempt to set more than one value for single-valued attribute "
                                    + attrName + " in " + projCtx.getKey();
                            LOGGER.debug("{}: value to be added: {}, existing value to replace: {}", message, shouldBeMapping, prismValueToReplace);
                            throw new SchemaException(message);
                        }
                    }
                    hasRealValueToReplace = true;
                    prismValueToReplace = shouldBePrismValue;
                    recordDelta(
                            valueMatcher, projCtx, ShadowType.F_ATTRIBUTES, simpleAttrDef,
                            ModificationType.REPLACE, shouldBePrismValue,
                            shouldBePvwo.getSource(), "it is given by a mapping");
                } else {
                    recordDelta(
                            valueMatcher, projCtx, ShadowType.F_ATTRIBUTES, simpleAttrDef,
                            ModificationType.ADD, shouldBePrismValue,
                            shouldBePvwo.getSource(), "it is given by a mapping");
                }
            } else {
                LOGGER.trace("Value is already present in {}, skipping it: {}", attrName, shouldBePrismValue);
            }
        }
        decideIfTolerate(projCtx, simpleAttrDef, arePValues, shouldBePValues, valueMatcher, hasOtherNonWeakValues);
    }

    private <PV extends PrismValue, PD extends ItemDefinition<?>> Collection<ItemValueWithOrigin<PV, PD>> selectValidValues(
            Collection<ItemValueWithOrigin<PV, PD>> values) {
        return filter(values, v -> v.isValid());
    }

    private <T> void addPropValuesFromDelta(
            Collection<ItemValueWithOrigin<PrismPropertyValue<T>, PrismPropertyDefinition<T>>> shouldBePValues,
            ObjectDelta<ShadowType> delta, QName attrName) {
        if (delta == null) {
            return;
        }
        List<PrismValue> values = delta.getNewValuesFor(ItemPath.create(ShadowType.F_ATTRIBUTES, attrName));
        for (PrismValue value : values) {
            if (value instanceof PrismPropertyValue<?>) {
                //noinspection unchecked
                shouldBePValues.add(
                        new ItemValueWithOrigin<>((PrismPropertyValue<T>) value, null, null));
            } else if (value != null) {
                throw new IllegalStateException("Unexpected type of prism value. Expected PPV, got " + value);
            }
        }
    }

    private void addContainerValuesFromDelta(
            Collection<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>> shouldBeCValues,
            ObjectDelta<ShadowType> delta, QName assocName) {
        if (delta == null) {
            return;
        }
        List<PrismValue> values = delta.getNewValuesFor(ShadowType.F_ASSOCIATIONS.append(assocName));
        for (PrismValue value : values) {
            if (value instanceof ShadowAssociationValue associationValue) {
                shouldBeCValues
                        .add(new ItemValueWithOrigin<>(associationValue, null, null));
            } else if (value != null) {
                throw new IllegalStateException("Unexpected type of prism value. Expected PCV<ShadowAssociationValueType>, got " + value);
            }
        }
    }

    private void reconcileProjectionAssociations(LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {

        LOGGER.trace("Association reconciliation processing {}", projCtx.getHumanReadableName());

        ResourceObjectDefinition accountDefinition = projCtx.getCompositeObjectDefinitionRequired();

        var squeezedAssociations = projCtx.getSqueezedAssociations();
        Collection<QName> associationNames =
                squeezedAssociations != null ?
                        MiscUtil.union(squeezedAssociations.keySet(), accountDefinition.getNamesOfAssociations()) :
                        accountDefinition.getNamesOfAssociations();

        for (QName assocName : associationNames) {
            LOGGER.trace("Association reconciliation processing association {}", assocName);
            var associationDefinition =
                    accountDefinition.findAssociationDefinitionRequired(assocName, lazy(() -> " in " + projCtx.getKey()));

            var cvwoTriple = squeezedAssociations != null ? squeezedAssociations.get(assocName) : null;

            // note: actually isIgnored is not implemented yet
            if (associationDefinition.isIgnored()) {
                LOGGER.trace("Skipping reconciliation of association {} because it is ignored", assocName);
                continue;
            }
            if (!associationDefinition.isVisible(task)) {
                LOGGER.trace("Skipping reconciliation of association {} because it is not visible in current execution mode",
                        assocName);
                return;
            }

            // TODO implement limitations
//            PropertyLimitations limitations = associationDefinition.getLimitations(LayerType.MODEL);
//            if (limitations != null) {
//                PropertyAccessType access = limitations.getAccess();
//                if (access != null) {
//                    if (projCtx.isAdd() && (access.isAdd() == null || !access.isAdd())) {
//                        LOGGER.trace("Skipping reconciliation of attribute {} because it is non-createable",
//                                attrName);
//                        continue;
//                    }
//                    if (projCtx.isModify() && (access.isModify() == null || !access.isModify())) {
//                        LOGGER.trace("Skipping reconciliation of attribute {} because it is non-updateable",
//                                attrName);
//                        continue;
//                    }
//                }
//            }

            if (!projCtx.isAssociationLoaded(assocName)) {
                if (!loadIfPossible(projCtx, "association " + assocName, task, result)) {
                    return;
                }
            }

            Collection<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>> shouldBeCValues;
            if (cvwoTriple == null) {
                shouldBeCValues = new HashSet<>();
            } else {
                shouldBeCValues = new HashSet<>(selectValidValues(cvwoTriple.getNonNegativeValues()));
            }
            // TODO what about equality checks? There will be probably duplicates there.

            // We consider values explicitly requested by user to be among "should be values".
            addContainerValuesFromDelta(shouldBeCValues, projCtx.getPrimaryDelta(), assocName);
            // But we DO NOT take values from sync delta (because they just reflect what's on the resource),
            // nor from secondary delta (because these got there from mappings).

            // Values in shouldBeCValues are parent-less; to be able to make Containerable out of them, we provide them a (fake)
            // parent, and we clone them not to mess anything.
            PrismContainer<ShadowAssociationValueType> fakeParent = prismContext.getSchemaRegistry()
                    .findContainerDefinitionByCompileTimeClass(ShadowAssociationValueType.class)
                    .instantiate();
            for (var cvwo : shouldBeCValues) {
                var value = cvwo.getItemValue().clone();
                value.setParent(fakeParent);
                cvwo.setItemValue(value);
            }

            boolean hasStrongShouldBeCValue = false;
            for (var shouldBeCValue : shouldBeCValues) {
                if (shouldBeCValue.isMappingStrong()) {
                    hasStrongShouldBeCValue = true;
                    break;
                }
            }

            var shadowNew = projCtx.getObjectNewRequired();
            var areCValues = new HashSet<>(ShadowUtil.getAdoptedAssociationValues(shadowNew, assocName));

            for (var shouldBeCvwo : shouldBeCValues) {
                PrismValueDeltaSetTripleProducer<?, ?> shouldBeMapping = shouldBeCvwo.getProducer();
                if (shouldBeMapping == null) {
                    continue;
                }
                var shouldBeCValue = shouldBeCvwo.getItemValue();
                if (shouldBeMapping.getStrength() != MappingStrengthType.STRONG
                        && (!areCValues.isEmpty() || hasStrongShouldBeCValue)) {
                    // Weak or normal value and the attribute already has a value. Skip it.
                    // We cannot override it as it might have been legally changed directly on the projection resource object.
                    LOGGER.trace("Skipping reconciliation of value {} of the association {}: the mapping is not strong",
                            shouldBeCValue, associationDefinition.getItemName().getLocalPart());
                    continue;
                }
                if (shouldBeCvwo.isValid() && isNotInAssociationsValue(shouldBeCValue, areCValues)) {
                    swallowAssociationDelta(
                            projCtx, associationDefinition, ModificationType.ADD, shouldBeCValue,
                            shouldBeCvwo.getSource(), "it is given by a mapping");
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  association {} before decideIfTolerateAssociation:", assocName.getLocalPart());
                LOGGER.trace("    areCValues:\n{}", DebugUtil.debugDump(areCValues));
                LOGGER.trace("    shouldBeCValues:\n{}", DebugUtil.debugDump(shouldBeCValues));
            }

            decideIfTolerateAssociation(
                    projCtx, associationDefinition, areCValues, shouldBeCValues, task, result);
        }
    }

    private <T> void decideIfTolerate(LensProjectionContext projCtx,
            ShadowSimpleAttributeDefinition<T> attributeDefinition,
            Collection<PrismPropertyValue<T>> arePValues,
            Collection<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> shouldBePValues,
            PropertyValueMatcher<T> valueMatcher, boolean hasOtherNonWeakValues) throws SchemaException {

        for (PrismPropertyValue<T> isPValue : arePValues) {
            if (matchPattern(attributeDefinition.getTolerantValuePatterns(), isPValue, valueMatcher)) {
                LOGGER.trace("Reconciliation: KEEPING value {} of the attribute {}: match with tolerant value pattern." , isPValue, attributeDefinition.getItemName().getLocalPart());
                continue;
            }

            if (matchPattern(attributeDefinition.getIntolerantValuePatterns(), isPValue, valueMatcher)) {
                recordDeleteDelta(isPValue, attributeDefinition, valueMatcher, projCtx, "it has matched with intolerant pattern");
                continue;
            }

            if (!attributeDefinition.isTolerant()) {
                if (isNotInPvwoValues(valueMatcher, isPValue, shouldBePValues, hasOtherNonWeakValues)) {
                    recordDeleteDelta(
                            isPValue, attributeDefinition, valueMatcher, projCtx,
                            "it is not given by any mapping and the attribute is not tolerant");
                }
            }
        }
    }

    private void decideIfTolerateAssociation(
            LensProjectionContext accCtx,
            ShadowAssociationDefinition assocDef,
            Collection<? extends ShadowAssociationValue> areCValues,
            Collection<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>> shouldBeCValues,
            Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, ExpressionEvaluationException {

        var evaluatePatterns = !assocDef.getTolerantValuePatterns().isEmpty() || !assocDef.getIntolerantValuePatterns().isEmpty();
        var matchingRule = evaluatePatterns ? getMatchingRuleForTargetNamingIdentifier(assocDef) : null;

        // for each existing value we decide whether to keep it or delete it
        for (var isCValue : areCValues) {
            ShadowSimpleAttribute<String> targetNamingIdentifier = null;
            if (evaluatePatterns) {
                targetNamingIdentifier = getTargetNamingIdentifier(isCValue, task, result);
                if (targetNamingIdentifier == null) {
                    LOGGER.warn("Couldn't check tolerant/intolerant patterns for {}, as there's no naming identifier for it", isCValue);
                    evaluatePatterns = false;
                }
            }

            var assocNameLocal = assocDef.getItemName().getLocalPart();
            if (evaluatePatterns && matchesAssociationPattern(assocDef.getTolerantValuePatterns(), targetNamingIdentifier, matchingRule)) {
                LOGGER.trace("Reconciliation: KEEPING value {} of association {}: identifier {} matches with tolerant value pattern.",
                        isCValue, assocNameLocal, targetNamingIdentifier);
                continue;
            }

            if (isInCvwoAssociationValues(isCValue, shouldBeCValues)) {
                LOGGER.trace("Reconciliation: KEEPING value {} of association {}: it is in 'shouldBeCValues'", isCValue, assocNameLocal);
                continue;
            }

            if (evaluatePatterns && matchesAssociationPattern(assocDef.getIntolerantValuePatterns(), targetNamingIdentifier, matchingRule)) {
                swallowAssociationDelta(accCtx, assocDef, ModificationType.DELETE,
                        isCValue, null, "identifier " + targetNamingIdentifier + " matches with intolerant pattern");
                continue;
            }

            // TODO maybe we should override also patterns evaluation here

            AbstractShadow shadowToGetToleranceFrom;
            if (assocDef.isComplex()) {
                shadowToGetToleranceFrom = isCValue.getAssociationDataObject();
            } else {
                // We are strict here: it's simpler + it's better to get exception instead of unstable behavior.
                // If the real life tells otherwise, we will change this.
                shadowToGetToleranceFrom = isCValue.getSingleObjectShadowRequired();
            }
            var associationTolerance = assocDef.isTolerant();
            var toleranceOverride =
                    ObjectOperationPolicyTypeUtil.getToleranceOverride(
                            shadowToGetToleranceFrom.getEffectiveOperationPolicyRequired());
            var effectivelyTolerant = Objects.requireNonNullElse(toleranceOverride, associationTolerance);

            if (!effectivelyTolerant) {
                swallowAssociationDelta(
                        accCtx, assocDef, ModificationType.DELETE, isCValue, null,
                        String.format(
                                "it is not given by any mapping and the value is not tolerated "
                                        + "(association tolerant: %s, value override: %s)",
                                associationTolerance, toleranceOverride));
            } else {
                LOGGER.trace("Reconciliation: KEEPING value {} of association {}: there was no reason to NOT tolerate it"
                        + " (association tolerant: {}, value override: {})",
                        isCValue, assocNameLocal, associationTolerance, toleranceOverride);
            }
        }
    }

    @NotNull
    private MatchingRule<Object> getMatchingRuleForTargetNamingIdentifier(ShadowAssociationDefinition associationDefinition)
            throws SchemaException {
        var targetObjectDefinition = associationDefinition.getRepresentativeTargetObjectDefinition();
        // TODO why naming attribute? Why not valueAttribute from the association definition?
        ShadowSimpleAttributeDefinition<?> targetNamingAttributeDef = targetObjectDefinition.getNamingAttribute();
        if (targetNamingAttributeDef != null) {
            QName matchingRuleName = targetNamingAttributeDef.getMatchingRuleQName();
            return matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
        } else {
            throw new IllegalStateException(
                    "Couldn't evaluate tolerant/intolerant value patterns, because naming attribute is not known for "
                            + targetObjectDefinition);
        }
    }

    private ShadowSimpleAttribute<String> getTargetNamingIdentifier(
            ShadowAssociationValue associationValue, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return getIdentifiersForAssociationTarget(associationValue, task, result).getNamingAttribute();
    }

    @NotNull
    private ShadowAttributesContainer getIdentifiersForAssociationTarget(
            ShadowAssociationValue isCValue,
            Task task, OperationResult result) throws CommunicationException,
            SchemaException, ConfigurationException,
            SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException {
        var objectRef = isCValue.getSingleObjectRefValueRequired();
        var attributesContainer = objectRef.getAttributesContainerIfPresent();
        if (attributesContainer != null) {
            return attributesContainer;
        }
        String oid = objectRef.getOid();
        if (oid == null) {
            // TODO maybe warn/error log would suffice?
            throw new IllegalStateException("Couldn't evaluate tolerant/intolerant values for association " + isCValue
                    + ", because there are no identifiers and no shadow reference present");
        }
        PrismObject<ShadowType> target;
        try {
            var options = SchemaService.get().getOperationOptionsBuilder()
                    .noFetch()
                    .futurePointInTime()
                    .readOnly()
                    .build();
            target = provisioningService.getObject(ShadowType.class, oid, options, task, result);
        } catch (ObjectNotFoundException e) {
            // TODO maybe warn/error log would suffice (also for other exceptions?)
            throw e.wrap("Couldn't evaluate tolerant/intolerant values for association " + isCValue
                    + ", because the association target object does not exist");
        }
        var identifiersInTarget = ShadowUtil.getAttributesContainer(target);
        if (identifiersInTarget != null) {
            return identifiersInTarget;
        }

        // TODO maybe warn/error log would suffice?
        throw new IllegalStateException("Couldn't evaluate tolerant/intolerant values for association " + isCValue
                + ", because there are no identifiers present, even in the repository object for association target");
    }

    private <T> void recordDelta(
            PropertyValueMatcher<T> valueMatcher, LensProjectionContext projCtx, ItemPath parentPath,
            PrismPropertyDefinition<T> attrDef, ModificationType changeType, PrismPropertyValue<T> value,
            ObjectType originObject, String reason)
            throws SchemaException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("  reconciliation will {} value of attribute {}: {} because {}", changeType,
                    PrettyPrinter.prettyPrint(attrDef.getItemName()), value, reason);
        }

        PropertyDelta<T> attrDelta = prismContext.deltaFactory().property().create(parentPath, attrDef.getItemName(), attrDef);
        PrismPropertyValue<T> valueClone = value.clone();
        valueClone.setOriginType(OriginType.RECONCILIATION);
        valueClone.setOriginObject(originObject);
        if (changeType == ModificationType.ADD) {
            attrDelta.addValueToAdd(valueClone);
        } else if (changeType == ModificationType.DELETE) {
            ItemDelta<PrismPropertyValue<T>, ?> currentItemDelta;
            ObjectDelta<ShadowType> currentDelta = projCtx.getCurrentDelta();
            if (currentDelta != null) {
                currentItemDelta = currentDelta.findItemDelta(ItemPath.create(parentPath, attrDef.getItemName()));
            } else {
                currentItemDelta = null;
            }
            if (isNotAlreadyBeingDeleted(currentItemDelta, valueMatcher, value)) {
                attrDelta.addValueToDelete(valueClone);
            }

        } else if (changeType == ModificationType.REPLACE) {
            attrDelta.setValueToReplace(valueClone);
        } else {
            throw new IllegalArgumentException("Unknown change type " + changeType);
        }

        LensUtil.setDeltaOldValue(projCtx, attrDelta);
        projCtx.swallowToSecondaryDelta(attrDelta);
    }

    private <T> void recordDeleteDelta(PrismPropertyValue<T> isPValue, ShadowSimpleAttributeDefinition<T> attributeDefinition,
            PropertyValueMatcher<T> valueMatcher, LensProjectionContext projCtx, String reason)
            throws SchemaException {
        recordDelta(
                valueMatcher, projCtx, ShadowType.F_ATTRIBUTES, attributeDefinition, ModificationType.DELETE,
                isPValue, null, reason);
    }

    private void swallowAssociationDelta(
            LensProjectionContext projCtx, ShadowAssociationDefinition assocDef, ModificationType changeType,
            ShadowAssociationValue value, ObjectType originObject, String reason) throws SchemaException {

        assert changeType == ModificationType.ADD || changeType == ModificationType.DELETE;

        LOGGER.trace("Reconciliation will {} value of association {}: {} because {}", changeType, assocDef, value, reason);

        ContainerDelta<ShadowAssociationValueType> assocDelta = assocDef.createEmptyDelta();

        ShadowAssociationValue valueClone = value.clone();
        valueClone.setOriginType(OriginType.RECONCILIATION);
        valueClone.setOriginObject(originObject);

        if (changeType == ModificationType.ADD) {
            assocDelta.addValueToAdd(valueClone);
        } else {
            ItemDelta<ShadowAssociationValue, ?> existingDelta;
            ObjectDelta<ShadowType> currentDelta = projCtx.getCurrentDelta();
            if (currentDelta != null) {
                existingDelta = currentDelta.findItemDelta(assocDef.getStandardPath());
            } else {
                existingDelta = null;
            }
            if (isNotAlreadyBeingDeleted(existingDelta, ShadowAssociationValue.semanticEqualsChecker(), value)) {
                LOGGER.trace("Adding association value to delete {} ", valueClone);
                assocDelta.addValueToDelete(valueClone);
            }
        }
        LensUtil.setDeltaOldValue(projCtx, assocDelta);

        projCtx.swallowToSecondaryDelta(assocDelta);
    }

    private <V extends PrismValue> boolean isNotAlreadyBeingDeleted(
            ItemDelta<V, ?> existingDelta, EqualsChecker<V> equalsChecker, PrismValue newValueToDelete) {
        LOGGER.trace("Checking existence for DELETE of value {} in existing delta: {}", newValueToDelete, existingDelta);
        if (existingDelta == null) {
            return true;
        }

        if (existingDelta.getValuesToDelete() == null) {
            return true;
        }

        for (PrismValue existingValueToDelete : existingDelta.getValuesToDelete()) {
            if (matchPrismValue(existingValueToDelete, newValueToDelete, equalsChecker)) {
                LOGGER.trace("Skipping adding value {} to delta for DELETE because it's already there", newValueToDelete);
                return false;
            }
        }
        return true;
    }

    private <T> boolean isNotInValues(
            PropertyValueMatcher<T> valueMatcher,
            PrismPropertyValue<T> shouldBePValue,
            Collection<PrismPropertyValue<T>> arePValues) {
        for (PrismPropertyValue<T> isPValue : emptyIfNull(arePValues)) {
            if (matchPrismValue(isPValue, shouldBePValue, valueMatcher)) {
                return false;
            }
        }
        return true;
    }

    // todo deduplicate; this was copied not to broke what works now [mederly]
    private boolean isNotInAssociationsValue(
            PrismContainerValue<ShadowAssociationValueType> shouldBeCValue,
            Collection<? extends ShadowAssociationValue> areCValues) {
        for (PrismContainerValue<ShadowAssociationValueType> isCValue : emptyIfNull(areCValues)) {
            if (matchPrismValue(isCValue, shouldBeCValue, ShadowAssociationValue.semanticEqualsChecker())) {
                return false;
            }
        }
        return true;
    }

    private <T> boolean isNotInPvwoValues(PropertyValueMatcher<T> valueMatcher, PrismPropertyValue<T> pValue,
            Collection<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> shouldBePvwos,
            boolean hasOtherNonWeakValues) {

        for (var shouldBePvwo : emptyIfNull(shouldBePvwos)) {
            if (!shouldBePvwo.isValid()) {
                continue;
            }
            if (hasOtherNonWeakValues && shouldBePvwo.isMappingWeak()) {
                continue;
            }
            PrismPropertyValue<T> shouldBePValue = shouldBePvwo.getItemValue();
            if (matchPrismValue(pValue, shouldBePValue, valueMatcher)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInCvwoAssociationValues(
            PrismContainerValue<ShadowAssociationValueType> value,
            Collection<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>> shouldBeCvwos) {

        for (var shouldBeCvwo : emptyIfNull(shouldBeCvwos)) {
            if (!shouldBeCvwo.isValid()) {
                continue;
            }
            PrismContainerValue<ShadowAssociationValueType> shouldBeCValue = shouldBeCvwo.getItemValue();
            if (matchPrismValue(value, shouldBeCValue, ShadowAssociationValue.semanticEqualsChecker())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) // Terrible solution, I know.
    private boolean matchPrismValue(PrismValue a, PrismValue b, EqualsChecker equalsChecker) {
        try {
            return equalsChecker.test(a, b);
        } catch (RuntimeException e) {
            LOGGER.warn("Value '{}' or '{}' is invalid: {}", a, b, e.getMessage(), e);
            return false;
        }
    }

    private <T> boolean matchPattern(
            List<String> patterns, PrismPropertyValue<T> isPValue, PropertyValueMatcher<T> valueMatcher) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            try {
                if (valueMatcher.matches(isPValue.getValue(), pattern)) {
                    return true;
                }
            } catch (SchemaException e) {
                LOGGER.warn("Value '{}' is invalid: {}", isPValue.getValue(), e.getMessage(), e);
                return false;
            }

        }
        return false;
    }

    private boolean matchesAssociationPattern(@NotNull List<String> patterns, @NotNull ShadowSimpleAttribute<?> identifier,
            @NotNull MatchingRule<Object> matchingRule) {
        for (String pattern : patterns) {
            for (PrismPropertyValue<?> identifierValue : identifier.getValues()) {
                try {
                    if (identifierValue != null && matchingRule.matchRegex(identifierValue.getRealValue(), pattern)) {
                        return true;
                    }
                } catch (SchemaException e) {
                    LOGGER.warn("Value '{}' is invalid: {}", identifierValue, e.getMessage(), e);
                    return false;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean loadIfPossible(LensProjectionContext projCtx, String desc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!projCtx.isDoReconciliation()) {
            LOGGER.trace(
                    "Skipping loading the shadow, as the reconciliation was not requested for {}",
                    projCtx.getHumanReadableName());
            return false;
        }
        contextLoader.loadFullShadowNoDiscovery(projCtx, "projection reconciliation", task, result);
        if (!projCtx.isFullShadow()) {
            LOGGER.trace(
                    "Full shadow could or should not be loaded, skipping the reconciliation of {} in {}",
                    desc, projCtx.getHumanReadableName());
            return false;
        }
        return true;
    }
}
