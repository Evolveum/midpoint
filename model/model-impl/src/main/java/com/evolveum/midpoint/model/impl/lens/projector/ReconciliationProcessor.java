/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingAndDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.util.MiscUtil.filter;

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
public class ReconciliationProcessor {

	@Autowired
	private ProvisioningService provisioningService;

	@Autowired
	PrismContext prismContext;

	@Autowired
	private MatchingRuleRegistry matchingRuleRegistry;

	private static final String PROCESS_RECONCILIATION = ReconciliationProcessor.class.getName() + ".processReconciliation";
	private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);

	<F extends ObjectType> void processReconciliation(LensContext<F> context,
													  LensProjectionContext projectionContext, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			// We can do this only for focal types.
			return;
		}
		processReconciliationFocus(context, projectionContext, task, result);
	}

	private <F extends ObjectType> void processReconciliationFocus(LensContext<F> context,
			LensProjectionContext projCtx, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {

		OperationResult subResult = result.createMinorSubresult(PROCESS_RECONCILIATION);

		try {
			// Reconcile even if it was not explicitly requested and if we have full shadow
			// reconciliation is cheap if the shadow is already fetched therefore just do it
			if (!projCtx.isDoReconciliation() && !projCtx.isFullShadow()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Skipping reconciliation of {}: no doReconciliation and no full shadow", projCtx.getHumanReadableName());
				}
				return;
			}

			SynchronizationPolicyDecision policyDecision = projCtx.getSynchronizationPolicyDecision();
			if (policyDecision != null
					&& (policyDecision == SynchronizationPolicyDecision.DELETE || policyDecision == SynchronizationPolicyDecision.UNLINK)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Skipping reconciliation of {}: decision={}", projCtx.getHumanReadableName(), policyDecision);
				}
				return;
			}

			if (projCtx.getObjectCurrent() == null) {
				LOGGER.warn("Can't do reconciliation. Account context doesn't contain current version of account.");
				return;
			}

			if (!projCtx.isFullShadow()) {
				// We need to load the object
				GetOperationOptions rootOps = GetOperationOptions.createDoNotDiscovery();
				rootOps.setPointInTimeType(PointInTimeType.FUTURE);
				PrismObject<ShadowType> objectOld = provisioningService.getObject(ShadowType.class,
						projCtx.getOid(), SelectorOptions.createCollection(rootOps),
						task, result);
				ShadowType oldShadow = objectOld.asObjectable();
				projCtx.determineFullShadowFlag(oldShadow.getFetchResult());
				projCtx.setLoadedObject(objectOld);

				projCtx.recompute();
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Starting reconciliation of {}", projCtx.getHumanReadableName());
			}

			reconcileAuxiliaryObjectClasses(projCtx);

            RefinedObjectClassDefinition rOcDef = projCtx.getCompositeObjectClassDefinition();

			Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> squeezedAttributes = projCtx
					.getSqueezedAttributes();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Attribute reconciliation processing {}", projCtx.getHumanReadableName());
			}
			reconcileProjectionAttributes(projCtx, squeezedAttributes, rOcDef);

            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> squeezedAssociations = projCtx.getSqueezedAssociations();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Association reconciliation processing {}", projCtx.getHumanReadableName());
			}
			reconcileProjectionAssociations(projCtx, squeezedAssociations, rOcDef, task, result);

            reconcileMissingAuxiliaryObjectClassAttributes(projCtx);

		} catch (RuntimeException | SchemaException e) {
			subResult.recordFatalError(e);
			throw e;
		} finally {
			subResult.computeStatus();
		}
	}

	private void reconcileAuxiliaryObjectClasses(LensProjectionContext projCtx) throws SchemaException {

		Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses = projCtx.getSqueezedAuxiliaryObjectClasses();
		if (squeezedAuxiliaryObjectClasses == null || squeezedAuxiliaryObjectClasses.isEmpty()) {
			return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Auxiliary object class reconciliation processing {}", projCtx.getHumanReadableName());
        }

		PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();
		PrismPropertyDefinition<QName> propDef = shadowNew.getDefinition().findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS);

		DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>> pvwoTriple = squeezedAuxiliaryObjectClasses.get(ShadowType.F_AUXILIARY_OBJECT_CLASS);

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

		ValueMatcher<QName> valueMatcher = ValueMatcher.createDefaultMatcher(DOMUtil.XSD_QNAME, matchingRuleRegistry);

		boolean auxObjectClassChanged = false;

		for (ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>> shouldBePvwo : shouldBePValues) {
			QName shouldBeRealValue = shouldBePvwo.getItemValue().getValue();
			if (!isInValues(valueMatcher, shouldBeRealValue, arePValues)) {
				auxObjectClassChanged = true;
				recordDelta(valueMatcher, projCtx, ItemPath.EMPTY_PATH, propDef, ModificationType.ADD, shouldBeRealValue,
						shouldBePvwo.getSource(), "it is given");
			}
		}

		if (!isTolerantAuxiliaryObjectClasses(projCtx)) {
			for (PrismPropertyValue<QName> isPValue : arePValues) {
				if (!isInPvwoValues(valueMatcher, isPValue.getValue(), shouldBePValues)) {
					auxObjectClassChanged = true;
					recordDelta(valueMatcher, projCtx, ItemPath.EMPTY_PATH, propDef, ModificationType.DELETE,
							isPValue.getValue(), null, "it is not given");
				}
			}
		}

		if (auxObjectClassChanged) {
			projCtx.recompute();
			projCtx.refreshAuxiliaryObjectClassDefinitions();
		}
	}

	private boolean isTolerantAuxiliaryObjectClasses(LensProjectionContext projCtx) throws SchemaException {
		ResourceBidirectionalMappingAndDefinitionType auxiliaryObjectClassMappings = projCtx.getStructuralObjectClassDefinition().getAuxiliaryObjectClassMappings();
		if (auxiliaryObjectClassMappings == null) {
			return false;
		}
		Boolean tolerant = auxiliaryObjectClassMappings.isTolerant();
		if (tolerant == null) {
			return false;
		}
		return tolerant;
	}

	/**
	 * If auxiliary object classes changed, there may still be some attributes that were defined by the aux objectclasses
	 * that were deleted. If these attributes are still around then delete them. Otherwise the delete of the aux object class
	 * may fail.
	 */
	private void reconcileMissingAuxiliaryObjectClassAttributes(LensProjectionContext projCtx) throws SchemaException {
		ObjectDelta<ShadowType> delta = projCtx.getDelta();
		if (delta == null) {
			return;
		}
		PropertyDelta<QName> auxOcDelta = delta.findPropertyDelta(ShadowType.F_AUXILIARY_OBJECT_CLASS);
		if (auxOcDelta == null || auxOcDelta.isEmpty()) {
			return;
		}
		Collection<QName> deletedAuxObjectClassNames = null;
		PrismObject<ShadowType> objectOld = projCtx.getObjectOld();
		if (auxOcDelta.isReplace()) {
			if (objectOld == null) {
				return;
			}
			PrismProperty<QName> auxOcPropOld = objectOld.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
			if (auxOcPropOld == null) {
				return;
			}
			Collection<QName> auxOcsOld = auxOcPropOld.getRealValues();
			Set<QName> auxOcsToReplace = PrismPropertyValue.getRealValuesOfCollection(auxOcDelta.getValuesToReplace());
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
			deletedAuxObjectClassNames = PrismPropertyValue.getRealValuesOfCollection(valuesToDelete);
		}
		LOGGER.trace("Deleted auxiliary object classes: {}", deletedAuxObjectClassNames);
		if (deletedAuxObjectClassNames == null || deletedAuxObjectClassNames.isEmpty()) {
			return;
		}

		List<QName> attributesToDelete = new ArrayList<>();
		String projHumanReadableName = projCtx.getHumanReadableName();
		RefinedResourceSchema refinedResourceSchema = projCtx.getRefinedResourceSchema();
		RefinedObjectClassDefinition structuralObjectClassDefinition = projCtx.getStructuralObjectClassDefinition();
		Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = projCtx.getAuxiliaryObjectClassDefinitions();
		for (QName deleteAuxOcName: deletedAuxObjectClassNames) {
			ObjectClassComplexTypeDefinition auxOcDef = refinedResourceSchema.findObjectClassDefinition(deleteAuxOcName);
			for (ResourceAttributeDefinition auxAttrDef: auxOcDef.getAttributeDefinitions()) {
				QName auxAttrName = auxAttrDef.getName();
				if (attributesToDelete.contains(auxAttrName)) {
					continue;
				}
				RefinedAttributeDefinition<Object> strucuralAttrDef = structuralObjectClassDefinition.findAttributeDefinition(auxAttrName);
				if (strucuralAttrDef == null) {
					boolean found = false;
					for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
						if (QNameUtil.contains(deletedAuxObjectClassNames, auxiliaryObjectClassDefinition.getTypeName())) {
							continue;
						}
						RefinedAttributeDefinition<Object> existingAuxAttrDef = auxiliaryObjectClassDefinition.findAttributeDefinition(auxAttrName);
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
		if (attributesToDelete.isEmpty()) {
			return;
		}

		for (QName attrNameToDelete: attributesToDelete) {
			ResourceAttribute<Object> attrToDelete = ShadowUtil.getAttribute(objectOld, attrNameToDelete);
			if (attrToDelete == null || attrToDelete.isEmpty()) {
				continue;
			}
			PropertyDelta<Object> attrDelta = attrToDelete.createDelta();
			attrDelta.addValuesToDelete(PrismValue.cloneCollection(attrToDelete.getValues()));
			projCtx.swallowToSecondaryDelta(attrDelta);
		}
	}

	private void reconcileProjectionAttributes(
            LensProjectionContext projCtx,
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> squeezedAttributes,
            RefinedObjectClassDefinition rOcDef) throws SchemaException {

		PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

		PrismContainer attributesContainer = shadowNew.findContainer(ShadowType.F_ATTRIBUTES);
		Collection<QName> attributeNames = squeezedAttributes != null ?
				MiscUtil.union(squeezedAttributes.keySet(), attributesContainer.getValue().getPropertyNames()) :
				attributesContainer.getValue().getPropertyNames();

		for (QName attrName : attributeNames) {
			reconcileProjectionAttribute(attrName, projCtx, squeezedAttributes, rOcDef, shadowNew, attributesContainer);
		}
	}

	private <T> void reconcileProjectionAttribute(QName attrName,
            LensProjectionContext projCtx,
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> squeezedAttributes,
            RefinedObjectClassDefinition rOcDef,
            PrismObject<ShadowType> shadowNew, PrismContainer attributesContainer) throws SchemaException {

		LOGGER.trace("Attribute reconciliation processing attribute {}", attrName);
		RefinedAttributeDefinition<T> attributeDefinition = projCtx.findAttributeDefinition(attrName);
		if (attributeDefinition == null) {
			String msg = "No definition for attribute " + attrName + " in "
					+ projCtx.getResourceShadowDiscriminator();
			throw new SchemaException(msg);
		}

		DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> pvwoTriple =
				squeezedAttributes != null ? (DeltaSetTriple) squeezedAttributes.get(attrName) : null;

		if (attributeDefinition.isIgnored(LayerType.MODEL)) {
			LOGGER.trace("Skipping reconciliation of attribute {} because it is ignored", attrName);
			return;
		}

		PropertyLimitations limitations = attributeDefinition.getLimitations(LayerType.MODEL);
		if (limitations != null) {
			PropertyAccessType access = limitations.getAccess();
			if (access != null) {
				if (projCtx.isAdd() && (access.isAdd() == null || !access.isAdd())) {
					LOGGER.trace("Skipping reconciliation of attribute {} because it is non-createable",
							attrName);
					return;
				}
				if (projCtx.isModify() && (access.isModify() == null || !access.isModify())) {
					LOGGER.trace("Skipping reconciliation of attribute {} because it is non-updateable",
							attrName);
					return;
				}
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
		for (ItemValueWithOrigin<? extends PrismPropertyValue<T>,PrismPropertyDefinition<T>> shouldBePValue : shouldBePValues) {
			if (shouldBePValue.getMapping() != null
					&& shouldBePValue.getMapping().getStrength() == MappingStrengthType.STRONG) {
				hasStrongShouldBePValue = true;
				break;
			}
		}

		PrismProperty<T> attribute = attributesContainer.findProperty(attrName);
		Collection<PrismPropertyValue<T>> arePValues;
		if (attribute != null) {
			arePValues = attribute.getValues();
		} else {
			arePValues = new HashSet<>();
		}

		// Too loud :-)
//			if (LOGGER.isTraceEnabled()) {
//				StringBuilder sb = new StringBuilder();
//				sb.append("Reconciliation\nATTR: ").append(PrettyPrinter.prettyPrint(attrName));
//				sb.append("\n  Should be:");
//				for (ItemValueWithOrigin<?,?> shouldBePValue : shouldBePValues) {
//					sb.append("\n    ");
//					sb.append(shouldBePValue.getItemValue());
//					PrismValueDeltaSetTripleProducer<?, ?> shouldBeMapping = shouldBePValue.getMapping();
//					if (shouldBeMapping.getStrength() == MappingStrengthType.STRONG) {
//						sb.append(" STRONG");
//					}
//					if (shouldBeMapping.getStrength() == MappingStrengthType.WEAK) {
//						sb.append(" WEAK");
//					}
//					if (!shouldBePValue.isValid()) {
//						sb.append(" INVALID");
//					}
//				}
//				sb.append("\n  Is:");
//				for (PrismPropertyValue<Object> isPVal : arePValues) {
//					sb.append("\n    ");
//					sb.append(isPVal);
//				}
//				LOGGER.trace("{}", sb.toString());
//			}

		ValueMatcher<T> valueMatcher = ValueMatcher.createMatcher(attributeDefinition, matchingRuleRegistry);

		T realValueToReplace = null;
		boolean hasRealValueToReplace = false;
		for (ItemValueWithOrigin<? extends PrismPropertyValue<T>,PrismPropertyDefinition<T>> shouldBePvwo : shouldBePValues) {
			PrismValueDeltaSetTripleProducer<?,?> shouldBeMapping = shouldBePvwo.getMapping();
			if (shouldBeMapping == null) {
				continue;
			}
			T shouldBeRealValue = shouldBePvwo.getItemValue().getValue();
			if (shouldBeMapping.getStrength() != MappingStrengthType.STRONG
					&& (!arePValues.isEmpty() || hasStrongShouldBePValue)) {
				// weak or normal value and the attribute already has a
				// value. Skip it.
				// we cannot override it as it might have been legally
				// changed directly on the projection resource object
				LOGGER.trace("Skipping reconciliation of value {} of the attribute {}: the mapping is not strong", shouldBeRealValue, attributeDefinition.getName().getLocalPart());
				continue;
			}
			if (!isInValues(valueMatcher, shouldBeRealValue, arePValues)) {
				if (attributeDefinition.isSingleValue()) {
					// It is quite possible that there are more shouldBePValues with equivalent real values but different 'context'.
					// We don't want to throw an exception if real values are in fact equivalent.
					// TODO generalize this a bit (e.g. also for multivalued items)
					if (hasRealValueToReplace) {
						if (matchValue(shouldBeRealValue, realValueToReplace, valueMatcher)) {
							LOGGER.trace("Value to replace for {} is already set, skipping it: {}", attrName, realValueToReplace);
							continue;
						} else {
							String message = "Attempt to set more than one value for single-valued attribute "
									+ attrName + " in " + projCtx.getResourceShadowDiscriminator();
							LOGGER.debug("{}: value to be added: {}, existing value to replace: {}", message, shouldBeMapping, realValueToReplace);
							throw new SchemaException(message);
						}
					}
					hasRealValueToReplace = true;
					realValueToReplace = shouldBeRealValue;
					recordDelta(valueMatcher, projCtx, SchemaConstants.PATH_ATTRIBUTES, attributeDefinition, ModificationType.REPLACE, shouldBeRealValue,
							shouldBePvwo.getSource(), "it is given by a mapping");
				} else {
					recordDelta(valueMatcher, projCtx, SchemaConstants.PATH_ATTRIBUTES, attributeDefinition, ModificationType.ADD, shouldBeRealValue,
							shouldBePvwo.getSource(), "it is given by a mapping");
				}
			}
		}

		decideIfTolerate(projCtx, attributeDefinition, arePValues, shouldBePValues, valueMatcher);

	}

	private <PV extends PrismValue, PD extends ItemDefinition> Collection<ItemValueWithOrigin<PV, PD>> selectValidValues(
			Collection<ItemValueWithOrigin<PV, PD>> values) {
		return filter(values, v -> v.isValid());
	}

	private <T> void addPropValuesFromDelta(
			Collection<ItemValueWithOrigin<PrismPropertyValue<T>, PrismPropertyDefinition<T>>> shouldBePValues,
			ObjectDelta<ShadowType> delta, QName attrName) {
		if (delta == null) {
			return;
		}
		List<PrismValue> values = delta.getNewValuesFor(new ItemPath(ShadowType.F_ATTRIBUTES, attrName));
		for (PrismValue value : values) {
			if (value instanceof PrismPropertyValue) {
				shouldBePValues.add(new ItemValueWithOrigin<>((PrismPropertyValue) value, null, null));
			} else if (value != null) {
				throw new IllegalStateException("Unexpected type of prism value. Expected PPV, got " + value);
			}
		}
	}

	private void addContainerValuesFromDelta(
			Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> shouldBeCValues,
			ObjectDelta<ShadowType> delta, QName assocName) {
		if (delta == null) {
			return;
		}
		List<PrismValue> values = delta.getNewValuesFor(new ItemPath(ShadowType.F_ASSOCIATION));
		for (PrismValue value : values) {
			if (value instanceof PrismContainerValue) {
				Containerable c = ((PrismContainerValue) value).asContainerable();
				if (c instanceof ShadowAssociationType) {
					ShadowAssociationType assocValue = (ShadowAssociationType) c;
					if (QNameUtil.match(assocValue.getName(), assocName)) {
						shouldBeCValues
								.add(new ItemValueWithOrigin<>((PrismContainerValue<ShadowAssociationType>) value, null, null));
					}
				} else {
					throw new IllegalStateException("Unexpected type of prism value. Expected PCV<ShadowAssociationType>, got " + value);
				}
			} else if (value != null) {
				throw new IllegalStateException("Unexpected type of prism value. Expected PCV<ShadowAssociationType>, got " + value);
			}
		}
	}

	private void reconcileProjectionAssociations(
			LensProjectionContext projCtx,
			Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>>> squeezedAssociations,
			RefinedObjectClassDefinition accountDefinition, Task task, OperationResult result)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException {

        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

        PrismContainer associationsContainer = shadowNew.findContainer(ShadowType.F_ASSOCIATION);

        Collection<QName> associationNames =
				squeezedAssociations != null ?
						MiscUtil.union(squeezedAssociations.keySet(), accountDefinition.getNamesOfAssociations()) :
						accountDefinition.getNamesOfAssociations();

        for (QName assocName : associationNames) {
            LOGGER.trace("Association reconciliation processing association {}", assocName);
            RefinedAssociationDefinition associationDefinition = accountDefinition.findAssociationDefinition(assocName);
            if (associationDefinition == null) {
                throw new SchemaException("No definition for association " + assocName + " in "
                        + projCtx.getResourceShadowDiscriminator());
            }

            DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> cvwoTriple =
					squeezedAssociations != null ? squeezedAssociations.get(assocName) : null;

            // note: actually isIgnored is not implemented yet
            if (associationDefinition.isIgnored()) {
                LOGGER.trace("Skipping reconciliation of association {} because it is ignored", assocName);
                continue;
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

            Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> shouldBeCValues;
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


			// values in shouldBeCValues are parent-less
            // to be able to make Containerable out of them, we provide them a (fake) parent
            // (and we clone them not to mess anything)

            PrismContainer<ShadowAssociationType> fakeParent = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ShadowAssociationType.class).instantiate();
            for (ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> cvwo : shouldBeCValues) {
                PrismContainerValue<ShadowAssociationType> cvalue = cvwo.getItemValue().clone();
                cvalue.setParent(fakeParent);
                cvwo.setItemValue(cvalue);
            }

            boolean hasStrongShouldBeCValue = false;
            for (ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> shouldBeCValue : shouldBeCValues) {
                if (shouldBeCValue.getMapping() != null
                        && shouldBeCValue.getMapping().getStrength() == MappingStrengthType.STRONG) {
                    hasStrongShouldBeCValue = true;
                    break;
                }
            }

            Collection<PrismContainerValue<ShadowAssociationType>> areCValues = new HashSet<>();
            if (associationsContainer != null) {
                for (Object o : associationsContainer.getValues()) {
                    PrismContainerValue<ShadowAssociationType> existingAssocValue = (PrismContainerValue<ShadowAssociationType>) o;
                    if (existingAssocValue.getValue().getName().equals(assocName)) {
                        areCValues.add(existingAssocValue);
                    }
                }
            } else {
                areCValues = new HashSet<>();
            }

            // todo comment this logging code out eventually
//			if (LOGGER.isTraceEnabled()) {
//				StringBuilder sb = new StringBuilder();
//				sb.append("Reconciliation\nASSOCIATION: ").append(PrettyPrinter.prettyPrint(assocName));
//				sb.append("\n  Should be:");
//				for (ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> shouldBeCValue : shouldBeCValues) {
//					sb.append("\n    ");
//					sb.append(shouldBeCValue.getItemValue());
//					PrismValueDeltaSetTripleProducer<?,?> shouldBeMapping = shouldBeCValue.getMapping();
//					if (shouldBeMapping != null && shouldBeMapping.getStrength() == MappingStrengthType.STRONG) {
//						sb.append(" STRONG");
//					}
//					if (shouldBeMapping != null && shouldBeMapping.getStrength() == MappingStrengthType.WEAK) {
//						sb.append(" WEAK");
//					}
//					if (!shouldBeCValue.isValid()) {
//						sb.append(" INVALID");
//					}
//				}
//				sb.append("\n  Is:");
//				for (PrismContainerValue<ShadowAssociationType> isCVal : areCValues) {
//					sb.append("\n    ");
//					sb.append(isCVal);
//				}
//				LOGGER.trace("{}", sb.toString());
//			}

            ValueMatcher associationValueMatcher = new ValueMatcher(null) {
                // todo is this correct? [med]
                @Override
                public boolean match(Object realA, Object realB) {
                    checkType(realA);
                    checkType(realB);

                    if (realA == null) {
                        return realB == null;
                    } else if (realB == null) {
                        return false;
                    } else {
                        ShadowAssociationType a = (ShadowAssociationType) realA;
                        ShadowAssociationType b = (ShadowAssociationType) realB;
                        checkName(a);
                        checkName(b);
                        if (!a.getName().equals(b.getName())) {
                            return false;
                        }
                        if (a.getShadowRef() != null && a.getShadowRef().getOid() != null && b.getShadowRef() != null && b.getShadowRef().getOid() != null) {
                            return a.getShadowRef().getOid().equals(b.getShadowRef().getOid());
                        }
                        LOGGER.warn("Comparing association values without shadowRefs: {} and {}", a, b);
                        return false;
                    }
                }

                private void checkName(ShadowAssociationType s) {
                    if (s.getName() == null) {
                        throw new IllegalStateException("No name for association " + s);
                    }
                }

                @Override
                public boolean matches(Object realValue, String regex) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean hasRealValue(PrismProperty property, PrismPropertyValue pValue) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isRealValueToAdd(PropertyDelta delta, PrismPropertyValue pValue) {
                    throw new UnsupportedOperationException();
                }

                private void checkType(Object o) {
                    if (o != null && !(o instanceof ShadowAssociationType)) {
                        throw new IllegalStateException("Object is not a ShadowAssociationType, it is " + o.getClass() + " instead");
                    }
                }
            };

            for (ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> shouldBeCvwo : shouldBeCValues) {
            	PrismValueDeltaSetTripleProducer<?,?> shouldBeMapping = shouldBeCvwo.getMapping();
                if (shouldBeMapping == null) {
                    continue;
                }
                if (shouldBeMapping.getStrength() != MappingStrengthType.STRONG
                        && (!areCValues.isEmpty() || hasStrongShouldBeCValue)) {
                    // weak or normal value and the attribute already has a
                    // value. Skip it.
                    // we cannot override it as it might have been legally
                    // changed directly on the projection resource object
                    continue;
                }
                ShadowAssociationType shouldBeRealValue = shouldBeCvwo.getItemValue().getValue();
                if (shouldBeCvwo.isValid() && !isInAssociationValues(associationValueMatcher, shouldBeRealValue, areCValues)) {
                    recordAssociationDelta(associationValueMatcher, projCtx, associationDefinition, ModificationType.ADD, shouldBeRealValue,
                            shouldBeCvwo.getSource(), "it is given by a mapping");
                }
            }

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Before decideIfTolerateAssociation:");
				LOGGER.trace("areCValues:\n{}", DebugUtil.debugDump(areCValues));
				LOGGER.trace("shouldBeCValues:\n{}", DebugUtil.debugDump(shouldBeCValues));
			}

			decideIfTolerateAssociation(projCtx, associationDefinition, areCValues, shouldBeCValues, associationValueMatcher,
					task, result);
        }
    }

	private <T> void decideIfTolerate(LensProjectionContext projCtx,
			RefinedAttributeDefinition<T> attributeDefinition,
			Collection<PrismPropertyValue<T>> arePValues,
			Collection<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> shouldBePValues,
			ValueMatcher<T> valueMatcher) throws SchemaException {

		for (PrismPropertyValue<T> isPValue : arePValues) {
			if (matchPattern(attributeDefinition.getTolerantValuePattern(), isPValue, valueMatcher)){
				LOGGER.trace("Reconciliation: KEEPING value {} of the attribute {}: match with tolerant value pattern." , isPValue, attributeDefinition.getName().getLocalPart());
				continue;
			}

			if (matchPattern(attributeDefinition.getIntolerantValuePattern(), isPValue, valueMatcher)){
				recordDeleteDelta(isPValue, attributeDefinition, valueMatcher, projCtx, "it has matched with intolerant pattern");
				continue;
			}

			if (!attributeDefinition.isTolerant()) {
				if (!isInPvwoValues(valueMatcher, isPValue.getValue(), shouldBePValues)) {
					recordDeleteDelta(isPValue, attributeDefinition, valueMatcher, projCtx, "it is not given by any mapping and the attribute is not tolerant");
				}
			}
		}

	}

	private void decideIfTolerateAssociation(LensProjectionContext accCtx,
			RefinedAssociationDefinition assocDef,
			Collection<PrismContainerValue<ShadowAssociationType>> areCValues,
			Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> shouldBeCValues,
			ValueMatcher valueMatcher, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
			ObjectNotFoundException, ExpressionEvaluationException {

		boolean evaluatePatterns = !assocDef.getTolerantValuePattern().isEmpty() || !assocDef.getIntolerantValuePattern().isEmpty();
		MatchingRule<Object> matchingRule = evaluatePatterns ? getMatchingRuleForTargetNamingIdentifier(assocDef) : null;

		// for each existing value we decide whether to keep it or delete it
		for (PrismContainerValue<ShadowAssociationType> isCValue : areCValues) {
			ResourceAttribute<String> targetNamingIdentifier = null;
			if (evaluatePatterns) {
				targetNamingIdentifier = getTargetNamingIdentifier(isCValue, task, result);
				if (targetNamingIdentifier == null) {
					LOGGER.warn("Couldn't check tolerant/intolerant patterns for {}, as there's no naming identifier for it", isCValue);
					evaluatePatterns = false;
				}
			}

			String assocNameLocal = assocDef.getName().getLocalPart();
			if (evaluatePatterns && matchesAssociationPattern(assocDef.getTolerantValuePattern(), targetNamingIdentifier, matchingRule)) {
				LOGGER.trace("Reconciliation: KEEPING value {} of association {}: identifier {} matches with tolerant value pattern.",
						isCValue, assocNameLocal, targetNamingIdentifier);
				continue;
			}

			if (isInCvwoAssociationValues(valueMatcher, isCValue.getValue(), shouldBeCValues)) {
				LOGGER.trace("Reconciliation: KEEPING value {} of association {}: it is in 'shouldBeCValues'", isCValue, assocNameLocal);
				continue;
			}

			if (evaluatePatterns && matchesAssociationPattern(assocDef.getIntolerantValuePattern(), targetNamingIdentifier, matchingRule)) {
				recordAssociationDelta(valueMatcher, accCtx, assocDef, ModificationType.DELETE,
						isCValue.getValue(), null, "identifier " + targetNamingIdentifier + " matches with intolerant pattern");
				continue;
			}

			if (!assocDef.isTolerant()) {
				recordAssociationDelta(valueMatcher, accCtx, assocDef, ModificationType.DELETE,
						isCValue.getValue(), null, "it is not given by any mapping and the association is not tolerant");
			} else {
				LOGGER.trace("Reconciliation: KEEPING value {} of association {}: the association is tolerant and the value"
						+ " was not caught by any intolerantValuePattern", isCValue, assocNameLocal);
			}
		}
    }

	@NotNull
	private MatchingRule<Object> getMatchingRuleForTargetNamingIdentifier(RefinedAssociationDefinition associationDefinition) throws SchemaException {
		RefinedAttributeDefinition<Object> targetNamingAttributeDef = associationDefinition.getAssociationTarget().getNamingAttribute();
		if (targetNamingAttributeDef != null) {
			QName matchingRuleName = targetNamingAttributeDef.getMatchingRuleQName();
			return matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
		} else {
			throw new IllegalStateException(
					"Couldn't evaluate tolerant/intolerant value patterns, because naming attribute is not known for "
							+ associationDefinition.getAssociationTarget());
		}
	}

	private ResourceAttribute<String> getTargetNamingIdentifier(
			PrismContainerValue<ShadowAssociationType> associationValue, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		return getIdentifiersForAssociationTarget(associationValue, task, result).getNamingAttribute();
	}

	@NotNull
	private ResourceAttributeContainer getIdentifiersForAssociationTarget(PrismContainerValue<ShadowAssociationType> isCValue,
			Task task, OperationResult result) throws CommunicationException,
			SchemaException, ConfigurationException,
			SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException {
		ResourceAttributeContainer identifiersContainer =
				ShadowUtil.getAttributesContainer(isCValue, ShadowAssociationType.F_IDENTIFIERS);
		if (identifiersContainer != null) {
			return identifiersContainer;
		}
		String oid = isCValue.asContainerable().getShadowRef() != null ? isCValue.asContainerable().getShadowRef().getOid() : null;
		if (oid == null) {
			// TODO maybe warn/error log would suffice?
			throw new IllegalStateException("Couldn't evaluate tolerant/intolerant values for association " + isCValue
					+ ", because there are no identifiers and no shadow reference present");
		}
		PrismObject<ShadowType> target;
		try {
			GetOperationOptions rootOpt = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
			rootOpt.setNoFetch(true);
			target = provisioningService.getObject(ShadowType.class, oid, SelectorOptions.createCollection(rootOpt), task, result);
		} catch (ObjectNotFoundException e) {
			// TODO maybe warn/error log would suffice (also for other exceptions?)
			throw new ObjectNotFoundException("Couldn't evaluate tolerant/intolerant values for association " + isCValue
					+ ", because the association target object does not exist: " + e.getMessage(), e);
		}
		identifiersContainer = ShadowUtil.getAttributesContainer(target);
		if (identifiersContainer == null) {
			// TODO maybe warn/error log would suffice?
			throw new IllegalStateException("Couldn't evaluate tolerant/intolerant values for association " + isCValue
					+ ", because there are no identifiers present, even in the repository object for association target");
		}
		return identifiersContainer;
	}

	private <T> void recordDelta(ValueMatcher<T> valueMatcher, LensProjectionContext projCtx, ItemPath parentPath,
			PrismPropertyDefinition<T> attrDef, ModificationType changeType, T value, ObjectType originObject, String reason)
			throws SchemaException {

		ItemDelta existingDelta = null;
		if (projCtx.getSecondaryDelta() != null) {
			existingDelta = projCtx.getSecondaryDelta().findItemDelta(
					new ItemPath(parentPath, attrDef.getName()));
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Reconciliation will {} value of attribute {}: {} because {}", changeType,
					PrettyPrinter.prettyPrint(attrDef.getName()), value, reason);
		}

		PropertyDelta<T> attrDelta = new PropertyDelta<>(parentPath, attrDef.getName(),
				attrDef, prismContext);
		PrismPropertyValue<T> pValue = new PrismPropertyValue<>(value, OriginType.RECONCILIATION,
				originObject);
		if (changeType == ModificationType.ADD) {
			attrDelta.addValueToAdd(pValue);
		} else if (changeType == ModificationType.DELETE) {
			if (!isToBeDeleted(existingDelta, valueMatcher, value)){
				attrDelta.addValueToDelete(pValue);
			}

		} else if (changeType == ModificationType.REPLACE) {
			attrDelta.setValueToReplace(pValue);
		} else {
			throw new IllegalArgumentException("Unknown change type " + changeType);
		}

		LensUtil.setDeltaOldValue(projCtx, attrDelta);

		projCtx.swallowToSecondaryDelta(attrDelta);
	}

	private <T> void recordDeleteDelta(PrismPropertyValue<T> isPValue, RefinedAttributeDefinition<T> attributeDefinition,
			ValueMatcher<T> valueMatcher, LensProjectionContext projCtx, String reason)
			throws SchemaException {
		recordDelta(valueMatcher, projCtx, SchemaConstants.PATH_ATTRIBUTES, attributeDefinition, ModificationType.DELETE,
				isPValue.getValue(), null, reason);
	}

	private void recordAssociationDelta(ValueMatcher valueMatcher, LensProjectionContext accCtx,
			RefinedAssociationDefinition assocDef, ModificationType changeType, ShadowAssociationType value,
			ObjectType originObject, String reason) throws SchemaException {

        ItemDelta existingDelta = null;
        if (accCtx.getSecondaryDelta() != null) {
            existingDelta = accCtx.getSecondaryDelta().findItemDelta(SchemaConstants.PATH_ASSOCIATION);
        }
        LOGGER.trace("Reconciliation will {} value of association {}: {} because {}", changeType, assocDef, value, reason);

        // todo initialize only once
        PrismContainerDefinition<ShadowAssociationType> associationDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class)
                .findContainerDefinition(ShadowType.F_ASSOCIATION);

        ContainerDelta assocDelta = new ContainerDelta(SchemaConstants.PATH_ASSOCIATION, associationDefinition, prismContext);

        PrismContainerValue cValue = value.asPrismContainerValue().clone();
        cValue.setOriginType(OriginType.RECONCILIATION);
        cValue.setOriginObject(originObject);

        if (changeType == ModificationType.ADD) {
            assocDelta.addValueToAdd(cValue);
        } else if (changeType == ModificationType.DELETE) {
            if (!isToBeDeleted(existingDelta, valueMatcher, value)){
                LOGGER.trace("Adding association value to delete {} ", cValue);
            	assocDelta.addValueToDelete(cValue);
            }
        } else if (changeType == ModificationType.REPLACE) {
            assocDelta.setValueToReplace(cValue);
        } else {
            throw new IllegalArgumentException("Unknown change type " + changeType);
        }
        LensUtil.setDeltaOldValue(accCtx, assocDelta);

        accCtx.swallowToSecondaryDelta(assocDelta);
    }


    private <T> boolean isToBeDeleted(ItemDelta existingDelta, ValueMatcher valueMatcher, T value) {
    	LOGGER.trace("Checking existence for DELETE of value {} in existing delta: {}", value, existingDelta);
		if (existingDelta == null) {
			return false;
		}

		if (existingDelta.getValuesToDelete() == null){
			return false;
		}


		for (Object isInDeltaValue : existingDelta.getValuesToDelete()) {
			if (isInDeltaValue instanceof PrismPropertyValue){
				PrismPropertyValue isInRealValue = (PrismPropertyValue) isInDeltaValue;
				if (matchValue(isInRealValue.getValue(), value, valueMatcher)) {
					LOGGER.trace("Skipping adding value {} to delta for DELETE because it's already there");
					return true;
				}
			} else if (isInDeltaValue instanceof PrismContainerValue) {
				PrismContainerValue isInRealValue = (PrismContainerValue) isInDeltaValue;
				if (matchValue(isInRealValue.asContainerable(), value, valueMatcher)){
					LOGGER.trace("Skipping adding value {} to delta for DELETE because it's already there");
					return true;
				}
			} //TODO: reference delta???


		}

		return false;

	}

	private <T> boolean isInValues(ValueMatcher<T> valueMatcher, T shouldBeValue,
			Collection<PrismPropertyValue<T>> arePValues) {
		if (arePValues == null || arePValues.isEmpty()) {
			return false;
		}
		for (PrismPropertyValue<T> isPValue : arePValues) {
			if (matchValue(isPValue.getValue(), shouldBeValue, valueMatcher)) {
				return true;
			}
		}
		return false;
	}

    // todo deduplicate; this was copied not to broke what works now [mederly]
    private boolean isInAssociationValues(ValueMatcher valueMatcher, ShadowAssociationType shouldBeValue,
                               Collection<PrismContainerValue<ShadowAssociationType>> arePValues) {
        if (arePValues == null || arePValues.isEmpty()) {
            return false;
        }
        for (PrismContainerValue<ShadowAssociationType> isPValue : arePValues) {
            if (matchValue(isPValue.getValue(), shouldBeValue, valueMatcher)) {
                return true;
            }
        }
        return false;
    }

	private <T> boolean isInPvwoValues(ValueMatcher<T> valueMatcher, T value,
			Collection<ItemValueWithOrigin<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> shouldBePvwos) {

		if (shouldBePvwos == null || shouldBePvwos.isEmpty()) {
			return false;
		}

		for (ItemValueWithOrigin<? extends PrismPropertyValue<T>,PrismPropertyDefinition<T>> shouldBePvwo : shouldBePvwos) {
			if (!shouldBePvwo.isValid()) {
        		continue;
        	}
			PrismPropertyValue<T> shouldBePPValue = shouldBePvwo.getItemValue();
			T shouldBeValue = shouldBePPValue.getValue();
			if (matchValue(value, shouldBeValue, valueMatcher)) {
				return true;
			}
		}
		return false;
	}

    private boolean isInCvwoAssociationValues(ValueMatcher valueMatcher, ShadowAssociationType value,
                                              Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> shouldBeCvwos) {

        if (shouldBeCvwos == null || shouldBeCvwos.isEmpty()) {
            return false;
        }

        for (ItemValueWithOrigin<? extends PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> shouldBeCvwo : shouldBeCvwos) {
        	if (!shouldBeCvwo.isValid()) {
        		continue;
        	}
            PrismContainerValue<ShadowAssociationType> shouldBePCValue = shouldBeCvwo.getItemValue();
            ShadowAssociationType shouldBeValue = shouldBePCValue.getValue();
            if (matchValue(value, shouldBeValue, valueMatcher)) {
                return true;
            }
        }
        return false;
    }


    private <T> boolean matchValue(T realA, T realB, ValueMatcher<T> valueMatcher) {
		try {
			return valueMatcher.match(realA, realB);
		} catch (SchemaException e) {
			LOGGER.warn("Value '{}' or '{}' is invalid: {}", realA, realB, e.getMessage(), e);
			return false;
		}
	}

    private <T> boolean matchPattern(List<String> patterns,
			PrismPropertyValue<T> isPValue, ValueMatcher<T> valueMatcher) {
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

    private boolean matchesAssociationPattern(@NotNull List<String> patterns, @NotNull ResourceAttribute<?> identifier,
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
}
