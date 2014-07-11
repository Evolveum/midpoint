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

package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
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
import com.evolveum.midpoint.schema.SelectorOptions;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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

	<F extends ObjectType> void processReconciliation(LensContext<F> context,
			LensProjectionContext projectionContext, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			// We can do this only for focal types.
			return;
		}
		processReconciliationFocus(context, projectionContext, result);
	}

	<F extends ObjectType> void processReconciliationFocus(LensContext<F> context,
                                                           LensProjectionContext accContext, OperationResult result) throws SchemaException,
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

			if (accContext.getObjectCurrent() == null) {
				LOGGER.warn("Can't do reconciliation. Account context doesn't contain current version of account.");
				return;
			}

			if (!accContext.isFullShadow()) {
				// We need to load the object
				PrismObject<ShadowType> objectOld = provisioningService.getObject(ShadowType.class,
						accContext.getOid(), SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery())
						, null, result);
				ShadowType oldShadow = objectOld.asObjectable();
				accContext.determineFullShadowFlag(oldShadow.getFetchResult());
				accContext.setLoadedObject(objectOld);

				accContext.recompute();
			}

            RefinedObjectClassDefinition accountDefinition = accContext.getRefinedAccountDefinition();

			Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>>>> squeezedAttributes = accContext
					.getSqueezedAttributes();
			if (squeezedAttributes != null && !squeezedAttributes.isEmpty()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Attribute reconciliation processing {}", accContext.getHumanReadableName());
                }
                reconcileProjectionAttributes(accContext, squeezedAttributes, accountDefinition);
            }

            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>>>> squeezedAssociations = accContext.getSqueezedAssociations();
            if (squeezedAssociations != null && !squeezedAssociations.isEmpty()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Association reconciliation processing {}", accContext.getHumanReadableName());
                }
                reconcileProjectionAssociations(accContext, squeezedAssociations, accountDefinition);
            }

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

	private void reconcileProjectionAttributes(
            LensProjectionContext projCtx,
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>>>> squeezedAttributes,
            RefinedObjectClassDefinition accountDefinition) throws SchemaException {

		PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

		PrismContainer attributesContainer = shadowNew.findContainer(ShadowType.F_ATTRIBUTES);
		Collection<QName> attributeNames = MiscUtil.union(squeezedAttributes.keySet(), attributesContainer
				.getValue().getPropertyNames());

		for (QName attrName : attributeNames) {
			// LOGGER.trace("Attribute reconciliation processing attribute {}",attrName);
			RefinedAttributeDefinition attributeDefinition = accountDefinition
					.getAttributeDefinition(attrName);
			if (attributeDefinition == null) {
				throw new SchemaException("No definition for attribute " + attrName + " in "
						+ projCtx.getResourceShadowDiscriminator());
			}

			DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>>> pvwoTriple = squeezedAttributes
					.get(attrName);

			if (attributeDefinition.isIgnored(LayerType.MODEL)) {
				LOGGER.trace("Skipping reconciliation of attribute {} because it is ignored", attrName);
				continue;
			}

			PropertyLimitations limitations = attributeDefinition.getLimitations(LayerType.MODEL);
			if (limitations != null) {
				PropertyAccessType access = limitations.getAccess();
				if (access != null) {
					if (projCtx.isAdd() && (access.isAdd() == null || !access.isAdd())) {
						LOGGER.trace("Skipping reconciliation of attribute {} because it is non-createable",
								attrName);
						continue;
					}
					if (projCtx.isModify() && (access.isModify() == null || !access.isModify())) {
						LOGGER.trace("Skipping reconciliation of attribute {} because it is non-updateable",
								attrName);
						continue;
					}
				}
			}

			Collection<ItemValueWithOrigin<PrismPropertyValue<?>>> shouldBePValues = null;
			if (pvwoTriple == null) {
				shouldBePValues = new ArrayList<ItemValueWithOrigin<PrismPropertyValue<?>>>();
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
//			if (LOGGER.isTraceEnabled()) {
//				StringBuilder sb = new StringBuilder();
//				sb.append("Reconciliation\nATTR: ").append(PrettyPrinter.prettyPrint(attrName));
//				sb.append("\n  Should be:");
//				for (ItemValueWithOrigin<? extends PrismPropertyValue<?>> shouldBePValue : shouldBePValues) {
//					sb.append("\n    ");
//					sb.append(shouldBePValue.getPropertyValue());
//					Mapping<?> shouldBeMapping = shouldBePValue.getMapping();
//					if (shouldBeMapping.getStrength() == MappingStrengthType.STRONG) {
//						sb.append(" STRONG");
//					}
//					if (shouldBeMapping.getStrength() == MappingStrengthType.WEAK) {
//						sb.append(" WEAK");
//					}
//				}
//				sb.append("\n  Is:");
//				for (PrismPropertyValue<Object> isPVal : arePValues) {
//					sb.append("\n    ");
//					sb.append(isPVal);
//				}
//				LOGGER.trace("{}", sb.toString());	
//			}
			 

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
											+ attrName + " in " + projCtx.getResourceShadowDiscriminator());
						}
						recordDelta(valueMatcher, projCtx, attributeDefinition, ModificationType.REPLACE, shouldBeRealValue,
								shouldBePvwo.getConstruction().getSource());
					} else {
						recordDelta(valueMatcher, projCtx, attributeDefinition, ModificationType.ADD, shouldBeRealValue,
								shouldBePvwo.getConstruction().getSource());
					}
					hasValue = true;
				}

			}
			
			decideIfTolerate(projCtx, attributeDefinition, arePValues, shouldBePValues, valueMatcher);
			
//				if (!attributeDefinition.isTolerant()) {
//				for (PrismPropertyValue<Object> isPValue : arePValues) {
//					if (!isInPvwoValues(valueMatcher, isPValue.getValue(), shouldBePValues)) {
//						recordDelta(valueMatcher, accCtx, attributeDefinition, ModificationType.DELETE,
//								isPValue.getValue(), null);
//					}
//				}
//			}
		}
	}

    private void reconcileProjectionAssociations(
            LensProjectionContext projCtx,
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>>>> squeezedAssociations,
            RefinedObjectClassDefinition accountDefinition) throws SchemaException {

        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

        PrismContainer associationsContainer = shadowNew.findContainer(ShadowType.F_ASSOCIATION);

        Collection<QName> associationNames = MiscUtil.union(squeezedAssociations.keySet(), accountDefinition.getNamesOfAssociations());

        for (QName assocName : associationNames) {
            LOGGER.trace("Association reconciliation processing association {}", assocName);
            RefinedAssociationDefinition associationDefinition = accountDefinition.findAssociation(assocName);
            if (associationDefinition == null) {
                throw new SchemaException("No definition for association " + assocName + " in "
                        + projCtx.getResourceShadowDiscriminator());
            }

            DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>>> cvwoTriple = squeezedAssociations.get(assocName);

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

            Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>>> shouldBeCValues;
            if (cvwoTriple == null) {
                shouldBeCValues = new ArrayList<>();
            } else {
                shouldBeCValues = cvwoTriple.getNonNegativeValues();
            }

            // values in shouldBeCValues are parent-less
            // to be able to make Containerable out of them, we provide them a (fake) parent
            // (and we clone them not to mess anything)

            PrismContainer<ShadowAssociationType> fakeParent = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ShadowAssociationType.class).instantiate();
            for (ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>> cvwo : shouldBeCValues) {
                PrismContainerValue<ShadowAssociationType> cvalue = cvwo.getItemValue().clone();
                cvalue.setParent(fakeParent);
                cvwo.setItemValue(cvalue);
            }

            boolean hasStrongShouldBeCValue = false;
            for (ItemValueWithOrigin<? extends PrismContainerValue<ShadowAssociationType>> shouldBeCValue : shouldBeCValues) {
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
			if (LOGGER.isTraceEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Reconciliation\nASSOCIATION: ").append(PrettyPrinter.prettyPrint(assocName));
				sb.append("\n  Should be:");
				for (ItemValueWithOrigin<? extends PrismContainerValue<ShadowAssociationType>> shouldBeCValue : shouldBeCValues) {
					sb.append("\n    ");
					sb.append(shouldBeCValue.getItemValue());
					Mapping<?> shouldBeMapping = shouldBeCValue.getMapping();
					if (shouldBeMapping.getStrength() == MappingStrengthType.STRONG) {
						sb.append(" STRONG");
					}
					if (shouldBeMapping.getStrength() == MappingStrengthType.WEAK) {
						sb.append(" WEAK");
					}
				}
				sb.append("\n  Is:");
				for (PrismContainerValue<ShadowAssociationType> isCVal : areCValues) {
					sb.append("\n    ");
					sb.append(isCVal);
				}
				LOGGER.trace("{}", sb.toString());
			}

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

            for (ItemValueWithOrigin<? extends PrismContainerValue<ShadowAssociationType>> shouldBeCvwo : shouldBeCValues) {
                Mapping<?> shouldBeMapping = shouldBeCvwo.getMapping();
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
                if (!isInAssociationValues(associationValueMatcher, shouldBeRealValue, areCValues)) {
                    recordAssociationDelta(associationValueMatcher, projCtx, associationDefinition, ModificationType.ADD, shouldBeRealValue,
                            shouldBeCvwo.getConstruction().getSource());
                }
            }

            decideIfTolerateAssociation(projCtx, associationDefinition, areCValues, shouldBeCValues, associationValueMatcher);
        }
    }

    private void decideIfTolerate(LensProjectionContext accCtx,
			RefinedAttributeDefinition attributeDefinition,
			Collection<PrismPropertyValue<Object>> arePValues,
			Collection<ItemValueWithOrigin<PrismPropertyValue<?>>> shouldBePValues,
			ValueMatcher valueMatcher) throws SchemaException {
		
		for (PrismPropertyValue<Object> isPValue : arePValues){
			if (matchPattern(attributeDefinition.getTolerantValuePattern(), isPValue, valueMatcher)){
				LOGGER.trace("Value {} of the attribute {} match with toletant value pattern. Value will be NOT DELETED." , new Object[]{isPValue, attributeDefinition});
				continue;
			}
		
			if (matchPattern(attributeDefinition.getIntolerantValuePattern(), isPValue, valueMatcher)){
				LOGGER.trace("Value {} of the attribute {} match with intoletant value pattern. Value will be DELETED." , new Object[]{isPValue, attributeDefinition});
				recordDelta(valueMatcher, accCtx, attributeDefinition, ModificationType.DELETE,
						isPValue.getValue(), null);
				continue;
			}		
				
			
			if (!attributeDefinition.isTolerant()) {
				if (!isInPvwoValues(valueMatcher, isPValue.getValue(), shouldBePValues)) {
						recordDelta(valueMatcher, accCtx, attributeDefinition, ModificationType.DELETE,
								isPValue.getValue(), null);
				}
			}
		}
		
	}

    private void decideIfTolerateAssociation(LensProjectionContext accCtx,
                                  RefinedAssociationDefinition associationDefinition,
                                  Collection<PrismContainerValue<ShadowAssociationType>> areCValues,
                                  Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>>> shouldBeCValues,
                                  ValueMatcher valueMatcher) throws SchemaException {

        for (PrismContainerValue<ShadowAssociationType> isCValue : areCValues){
            if (!associationDefinition.isTolerant()) {
                if (!isInCvwoAssociationValues(valueMatcher, isCValue.getValue(), shouldBeCValues)) {
                    recordAssociationDelta(valueMatcher, accCtx, associationDefinition, ModificationType.DELETE,
                            isCValue.getValue(), null);
                }
            }
        }
    }

    private boolean matchPattern(List<String> patterns,
			PrismPropertyValue<Object> isPValue, ValueMatcher valueMatcher) {
		if (patterns == null || patterns.isEmpty()) {
			return false;
		}
		for (String toleratePattern : patterns) {
			if (valueMatcher.matches(isPValue.getValue(), toleratePattern)) {
				return true;
			}

		}
		return false;
	}
	

	private <T> void recordDelta(ValueMatcher valueMatcher, LensProjectionContext accCtx,
			ResourceAttributeDefinition attrDef, ModificationType changeType, T value, ObjectType originObject)
			throws SchemaException {

		ItemDelta existingDelta = null;
		if (accCtx.getSecondaryDelta() != null) {
			existingDelta = accCtx.getSecondaryDelta().findItemDelta(
					new ItemPath(SchemaConstants.PATH_ATTRIBUTES, attrDef.getName()));
		}
		LOGGER.trace("Reconciliation will {} value of attribute {}: {}", new Object[] { changeType, attrDef,
				value });

		PropertyDelta<T> attrDelta = new PropertyDelta<T>(SchemaConstants.PATH_ATTRIBUTES, attrDef.getName(),
				attrDef, prismContext);
		PrismPropertyValue<T> pValue = new PrismPropertyValue<T>(value, OriginType.RECONCILIATION,
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

		accCtx.swallowToSecondaryDelta(attrDelta);
	}

    private void recordAssociationDelta(ValueMatcher valueMatcher, LensProjectionContext accCtx,
                                 RefinedAssociationDefinition assocDef, ModificationType changeType, ShadowAssociationType value, ObjectType originObject)
            throws SchemaException {

        ItemDelta existingDelta = null;
        if (accCtx.getSecondaryDelta() != null) {
            existingDelta = accCtx.getSecondaryDelta().findItemDelta(SchemaConstants.PATH_ASSOCIATION);
        }
        LOGGER.trace("Reconciliation will {} value of association {}: {}", new Object[] { changeType, assocDef, value });

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
                assocDelta.addValueToDelete(cValue);
            }
        } else if (changeType == ModificationType.REPLACE) {
            assocDelta.setValueToReplace(cValue);
        } else {
            throw new IllegalArgumentException("Unknown change type " + changeType);
        }

        accCtx.swallowToSecondaryDelta(assocDelta);
    }


    private <T> boolean isToBeDeleted(ItemDelta existingDelta, ValueMatcher valueMatcher, T value) {
		if (existingDelta == null) {
			return false;
		}
		
		if (existingDelta.getValuesToDelete() == null){
			return false;
		}
		
		for (Object isInDeltaValue : existingDelta.getValuesToDelete()) {
			if (isInDeltaValue instanceof PrismPropertyValue){
				PrismPropertyValue isInRealValue = (PrismPropertyValue) isInDeltaValue;
				if (valueMatcher.match(isInRealValue.getValue(), value)) {
					return true;
				}
			}
		}
		
		return false;
		
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

    // todo deduplicate; this was copied not to broke what works now [mederly]
    private boolean isInAssociationValues(ValueMatcher valueMatcher, ShadowAssociationType shouldBeValue,
                               Collection<PrismContainerValue<ShadowAssociationType>> arePValues) {
        if (arePValues == null || arePValues.isEmpty()) {
            return false;
        }
        for (PrismContainerValue<ShadowAssociationType> isPValue : arePValues) {
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
			Collection<ItemValueWithOrigin<PrismPropertyValue<?>>> shouldBePvwos) {

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

    private boolean isInCvwoAssociationValues(ValueMatcher valueMatcher, ShadowAssociationType value,
                                              Collection<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>>> shouldBeCvwos) {

        if (shouldBeCvwos == null || shouldBeCvwos.isEmpty()) {
            return false;
        }

        for (ItemValueWithOrigin<? extends PrismContainerValue<ShadowAssociationType>> shouldBeCvwo : shouldBeCvwos) {
            PrismContainerValue<ShadowAssociationType> shouldBePCValue = shouldBeCvwo.getItemValue();
            ShadowAssociationType shouldBeValue = shouldBePCValue.getValue();
            if (valueMatcher.match(value, shouldBeValue)) {
                return true;
            }
        }
        return false;
    }

}
