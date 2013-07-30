/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.model.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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

	@Autowired(required = true)
	private ProvisioningService provisioningService;

	@Autowired(required = true)
	PrismContext prismContext;

	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;

	public static final String PROCESS_RECONCILIATION = ReconciliationProcessor.class.getName()
			+ ".processReconciliation";
	private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);

	<F extends ObjectType, P extends ObjectType> void processReconciliation(LensContext<F, P> context,
			LensProjectionContext<P> projectionContext, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		if (focusContext.getObjectTypeClass() != UserType.class) {
			// We can do this only for user.
			return;
		}
		processReconciliationUser((LensContext<UserType, ShadowType>) context,
				(LensProjectionContext<ShadowType>) projectionContext, result);
	}

	void processReconciliationUser(LensContext<UserType, ShadowType> context,
			LensProjectionContext<ShadowType> accContext, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		OperationResult subResult = result.createSubresult(PROCESS_RECONCILIATION);

		try {
			// Reconcile even if it was not explicitly requested and if we have
			// full shadow
			// reconciliation is cheap if the shadow is already fetched
			// therefore just do it
			if (!accContext.isDoReconciliation() && !accContext.isFullShadow()) {
				return;
			}

			SynchronizationPolicyDecision policyDecision = accContext.getSynchronizationPolicyDecision();
			if (policyDecision != null
					&& (policyDecision == SynchronizationPolicyDecision.DELETE || policyDecision == SynchronizationPolicyDecision.UNLINK)) {
				return;
			}

			if (accContext.getObjectOld() == null) {
				LOGGER.warn("Can't do reconciliation. Account context doesn't contain old version of account.");
				return;
			}

			if (!accContext.isFullShadow()) {
				// We need to load the object
				PrismObject<ShadowType> objectOld = provisioningService.getObject(ShadowType.class,
						accContext.getOid(), GetOperationOptions.createDoNotDiscovery(), result);
				ShadowType oldShadow = objectOld.asObjectable();
				accContext.determineFullShadowFlag(oldShadow.getFetchResult());
				accContext.setObjectOld(objectOld);

				accContext.recompute();
			}

			Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> squeezedAttributes = accContext
					.getSqueezedAttributes();
			if (squeezedAttributes == null || squeezedAttributes.isEmpty()) {
				return;
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Attribute reconciliation processing {}", accContext.getHumanReadableName());
			}

			RefinedObjectClassDefinition accountDefinition = accContext.getRefinedAccountDefinition();
			reconcileAccount(accContext, squeezedAttributes, accountDefinition);
		} catch (RuntimeException e) {
			subResult.recordFatalError(e);
			throw e;
		} catch (SchemaException e) {
			subResult.recordFatalError(e);
			throw e;
		} finally {
			subResult.computeStatus();
		}
	}

	private void reconcileAccount(
			LensProjectionContext<ShadowType> accCtx,
			Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> squeezedAttributes,
			RefinedObjectClassDefinition accountDefinition) throws SchemaException {

		PrismObject<ShadowType> account = accCtx.getObjectNew();

		PrismContainer attributesContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		Collection<QName> attributeNames = MiscUtil.union(squeezedAttributes.keySet(), attributesContainer
				.getValue().getPropertyNames());

		for (QName attrName : attributeNames) {
			// LOGGER.trace("Attribute reconciliation processing attribute {}",attrName);
			RefinedAttributeDefinition attributeDefinition = accountDefinition
					.getAttributeDefinition(attrName);
			if (attributeDefinition == null) {
				throw new SchemaException("No definition for attribute " + attrName + " in "
						+ accCtx.getResourceShadowDiscriminator());
			}

			DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> pvwoTriple = squeezedAttributes
					.get(attrName);

			if (attributeDefinition.isIgnored(LayerType.MODEL)) {
				LOGGER.trace("Skipping reconciliation of attribute {} because it is ignored", attrName);
				continue;
			}

			PropertyLimitations limitations = attributeDefinition.getLimitations(LayerType.MODEL);
			if (limitations != null) {
				PropertyAccessType access = limitations.getAccess();
				if (access != null) {
					if (accCtx.isAdd() && (access.isCreate() == null || !access.isCreate())) {
						LOGGER.trace("Skipping reconciliation of attribute {} because it is non-createable",
								attrName);
						continue;
					}
					if (accCtx.isModify() && (access.isUpdate() == null || !access.isUpdate())) {
						LOGGER.trace("Skipping reconciliation of attribute {} because it is non-updateable",
								attrName);
						continue;
					}
				}
			}

			Collection<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> shouldBePValues = null;
			if (pvwoTriple == null) {
				shouldBePValues = new ArrayList<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>();
			} else {
				shouldBePValues = pvwoTriple.getNonNegativeValues();
			}

			boolean hasStrongShouldBePValue = false;
			for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePValue : shouldBePValues) {
				if (shouldBePValue.getMapping() != null
						&& shouldBePValue.getMapping().getStrength() == MappingStrengthType.STRONG) {
					hasStrongShouldBePValue = true;
					break;
				}
			}

			PrismProperty<?> attribute = attributesContainer.findProperty(attrName);
			Collection<PrismPropertyValue<Object>> arePValues = null;
			if (attribute != null) {
				arePValues = attribute.getValues(Object.class);
			} else {
				arePValues = new HashSet<PrismPropertyValue<Object>>();
			}

			// Too loud :-)
			// LOGGER.trace("SHOULD BE:\n{}\nIS:\n{}",shouldBePValues,arePValues);

			ValueMatcher<?> valueMatcher = ValueMatcher.createMatcher(attributeDefinition,
					matchingRuleRegistry);

			boolean hasValue = false;
			for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePvwo : shouldBePValues) {
				Mapping<?> shouldBeMapping = shouldBePvwo.getMapping();
				if (shouldBeMapping == null) {
					continue;
				}
				if (shouldBeMapping.getStrength() != MappingStrengthType.STRONG
						&& (!arePValues.isEmpty() || hasStrongShouldBePValue)) {
					// weak or normal value and the attribute already has a
					// value. Skip it.
					// we cannot override it as it might have been legally
					// changed directly on the projection resource object
					continue;
				}
				Object shouldBeRealValue = shouldBePvwo.getPropertyValue().getValue();
				if (!isInValues(valueMatcher, shouldBeRealValue, arePValues)) {
					if (attributeDefinition.isSingleValue()) {
						if (hasValue) {
							throw new SchemaException(
									"Attempt to set more than one value for single-valued attribute "
											+ attrName + " in " + accCtx.getResourceShadowDiscriminator());
						}
						recordDelta(valueMatcher, accCtx, attributeDefinition, ModificationType.REPLACE, shouldBeRealValue,
								shouldBePvwo.getAccountConstruction().getSource());
					} else {
						recordDelta(valueMatcher, accCtx, attributeDefinition, ModificationType.ADD, shouldBeRealValue,
								shouldBePvwo.getAccountConstruction().getSource());
					}
					hasValue = true;
				}

			}

			if (!attributeDefinition.isTolerant()) {
				for (PrismPropertyValue<Object> isPValue : arePValues) {
					if (!isInPvwoValues(valueMatcher, isPValue.getValue(), shouldBePValues)) {
						recordDelta(valueMatcher, accCtx, attributeDefinition, ModificationType.DELETE,
								isPValue.getValue(), null);
					}
				}
			}
		}
	}

	private <T> void recordDelta(ValueMatcher valueMatcher, LensProjectionContext<ShadowType> accCtx,
			ResourceAttributeDefinition attrDef, ModificationType changeType, T value, ObjectType originObject)
			throws SchemaException {

		// value matcher na realnu hodnotu
		ItemDelta delta = accCtx.getSecondaryDelta().findItemDelta(new ItemPath(SchemaConstants.PATH_ATTRIBUTES, attrDef.getName()));
		
		LOGGER.trace("Reconciliation will {} value of attribute {}: {}", new Object[] { changeType, attrDef,
				value });

		PropertyDelta<T> attrDelta = new PropertyDelta<T>(SchemaConstants.PATH_ATTRIBUTES, attrDef.getName(),
				attrDef);
		PrismPropertyValue<T> pValue = new PrismPropertyValue<T>(value, OriginType.RECONCILIATION,
				originObject);
		if (changeType == ModificationType.ADD) {
			attrDelta.addValueToAdd(pValue);
		} else if (changeType == ModificationType.DELETE) {
			for (Object isInDeltaValue : delta.getValuesToDelete()){
				if (valueMatcher.match(isInDeltaValue, pValue)){
					break;
				}
			}
			attrDelta.addValueToDelete(pValue);
		} else if (changeType == ModificationType.REPLACE) {
			attrDelta.setValueToReplace(pValue);
		} else {
			throw new IllegalArgumentException("Unknown change type " + changeType);
		}

		accCtx.addToSecondaryDelta(attrDelta);
	}

	private boolean isInValues(ValueMatcher valueMatcher, Object shouldBeValue,
			Collection<PrismPropertyValue<Object>> arePValues) {
		if (arePValues == null || arePValues.isEmpty()) {
			return false;
		}
		for (PrismPropertyValue<Object> isPValue : arePValues) {
			if (valueMatcher.match(isPValue.getValue(), shouldBeValue)) {
				return true;
			}
		}
		return false;
	}

	private boolean isInPvwoValues(Object value,
			Collection<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> shouldBePvwos) {
		for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePvwo : shouldBePvwos) {
			PrismPropertyValue<?> shouldBePPValue = shouldBePvwo.getPropertyValue();
			Object shouldBeValue = shouldBePPValue.getValue();
			if (shouldBeValue.equals(value)) {
				return true;
			}
		}
		return false;
	}

	private boolean isInPvwoValues(ValueMatcher valueMatcher, Object value,
			Collection<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> shouldBePvwos) {

		if (shouldBePvwos == null || shouldBePvwos.isEmpty()) {
			return false;
		}

		for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePvwo : shouldBePvwos) {
			PrismPropertyValue<?> shouldBePPValue = shouldBePvwo.getPropertyValue();
			Object shouldBeValue = shouldBePPValue.getValue();
			if (valueMatcher.match(value, shouldBeValue)) {
				return true;
			}
			// if (shouldBeValue.equals(value)) {
			// return true;
			// }
		}
		return false;
	}

}
