/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Component that takes care of some shadow maintenance, such as applying definitions, applying pending
 * operations and so on.
 * 
 * Important: this component should be pretty much stand-alone low-level component. It should NOT have
 * any dependencies on ShadowCache, ShadowManager or any other provisioning components (except util components).
 * 
 * @author Radovan Semancik
 */
@Component
public class ShadowCaretaker {
	
	@Autowired private Clock clock;
	@Autowired private PrismContext prismContext;

	private static final Trace LOGGER = TraceManager.getTrace(ShadowCaretaker.class);
	
	public void applyAttributesDefinition(ProvisioningContext ctx, ObjectDelta<ShadowType> delta)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		if (delta.isAdd()) {
			applyAttributesDefinition(ctx, delta.getObjectToAdd());
		} else if (delta.isModify()) {
			for (ItemDelta<?, ?> itemDelta : delta.getModifications()) {
				if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
					applyAttributeDefinition(ctx, delta, itemDelta);
				} else if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getPath())) {
					if (itemDelta.isAdd()) {
						for (PrismValue value : itemDelta.getValuesToAdd()) {
							applyAttributeDefinition(ctx, value);
						}
					}
					if (itemDelta.isReplace()) {
						for (PrismValue value : itemDelta.getValuesToReplace()) {
							applyAttributeDefinition(ctx, value);
						}
					}
				}
			}
		}
	}

	// value should be a value of attributes container
	private void applyAttributeDefinition(ProvisioningContext ctx, PrismValue value)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		if (!(value instanceof PrismContainerValue)) {
			return; // should never occur
		}
		PrismContainerValue<ShadowAttributesType> pcv = (PrismContainerValue<ShadowAttributesType>) value;
		for (Item item : pcv.getItems()) {
			ItemDefinition itemDef = item.getDefinition();
			if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
				QName attributeName = item.getElementName();
				ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition()
						.findAttributeDefinition(attributeName);
				if (attributeDefinition == null) {
					throw new SchemaException("No definition for attribute " + attributeName);
				}
				if (itemDef != null) {
					// We are going to rewrite the definition anyway. Let's just
					// do some basic checks first
					if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
						throw new SchemaException("The value of type " + itemDef.getTypeName()
								+ " cannot be applied to attribute " + attributeName + " which is of type "
								+ attributeDefinition.getTypeName());
					}
				}
				item.applyDefinition(attributeDefinition);
			}
		}
	}

	private <V extends PrismValue, D extends ItemDefinition> void applyAttributeDefinition(
			ProvisioningContext ctx, ObjectDelta<ShadowType> delta, ItemDelta<V, D> itemDelta)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException, ExpressionEvaluationException {
		if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) { 
			// just to be sure
			return;
		}
		D itemDef = itemDelta.getDefinition();
		if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
			QName attributeName = itemDelta.getElementName();
			ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition()
					.findAttributeDefinition(attributeName);
			if (attributeDefinition == null) {
				throw new SchemaException(
						"No definition for attribute " + attributeName + " in object delta " + delta);
			}
			if (itemDef != null) {
				// We are going to rewrite the definition anyway. Let's just do
				// some basic checks first
				if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
					throw new SchemaException("The value of type " + itemDef.getTypeName()
							+ " cannot be applied to attribute " + attributeName + " which is of type "
							+ attributeDefinition.getTypeName());
				}
			}
			itemDelta.applyDefinition((D) attributeDefinition);
		}
	}

	public ProvisioningContext applyAttributesDefinition(ProvisioningContext ctx,
			PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException,
					ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		ProvisioningContext subctx = ctx.spawn(shadow);
		subctx.assertDefinition();
		RefinedObjectClassDefinition objectClassDefinition = subctx.getObjectClassDefinition();

		PrismContainer<ShadowAttributesType> attributesContainer = shadow
				.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer != null) {
			if (attributesContainer instanceof ResourceAttributeContainer) {
				if (attributesContainer.getDefinition() == null) {
					attributesContainer
							.applyDefinition(objectClassDefinition.toResourceAttributeContainerDefinition());
				}
			} else {
				try {
					// We need to convert <attributes> to
					// ResourceAttributeContainer
					ResourceAttributeContainer convertedContainer = ResourceAttributeContainer
							.convertFromContainer(attributesContainer, objectClassDefinition);
					shadow.getValue().replace(attributesContainer, convertedContainer);
				} catch (SchemaException e) {
					throw new SchemaException(e.getMessage() + " in " + shadow, e);
				}
			}
		}

		// We also need to replace the entire object definition to inject
		// correct object class definition here
		// If we don't do this then the patch (delta.applyTo) will not work
		// correctly because it will not be able to
		// create the attribute container if needed.

		PrismObjectDefinition<ShadowType> objectDefinition = shadow.getDefinition();
		PrismContainerDefinition<ShadowAttributesType> origAttrContainerDef = objectDefinition
				.findContainerDefinition(ShadowType.F_ATTRIBUTES);
		if (origAttrContainerDef == null
				|| !(origAttrContainerDef instanceof ResourceAttributeContainerDefinition)) {
			PrismObjectDefinition<ShadowType> clonedDefinition = objectDefinition.cloneWithReplacedDefinition(
					ShadowType.F_ATTRIBUTES, objectClassDefinition.toResourceAttributeContainerDefinition());
			shadow.setDefinition(clonedDefinition);
		}

		return subctx;
	}

	/**
	 * Reapplies definition to the shadow if needed. The definition needs to be
	 * reapplied e.g. if the shadow has auxiliary object classes, it if subclass
	 * of the object class that was originally requested, etc.
	 */
	public ProvisioningContext reapplyDefinitions(ProvisioningContext ctx,
			PrismObject<ShadowType> rawResourceShadow) throws SchemaException, ConfigurationException,
					ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		ShadowType rawResourceShadowType = rawResourceShadow.asObjectable();
		QName objectClassQName = rawResourceShadowType.getObjectClass();
		List<QName> auxiliaryObjectClassQNames = rawResourceShadowType.getAuxiliaryObjectClass();
		if (auxiliaryObjectClassQNames.isEmpty()
				&& objectClassQName.equals(ctx.getObjectClassDefinition().getTypeName())) {
			// shortcut, no need to reapply anything
			return ctx;
		}
		ProvisioningContext shadowCtx = ctx.spawn(rawResourceShadow);
		shadowCtx.assertDefinition();
		RefinedObjectClassDefinition shadowDef = shadowCtx.getObjectClassDefinition();
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(rawResourceShadow);
		attributesContainer.applyDefinition(shadowDef.toResourceAttributeContainerDefinition());
		return shadowCtx;
	}
	
	public PrismObject<ShadowType> applyPendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> shadow, XMLGregorianCalendar now)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		if (shadow == null) {
			return null;
		}
		return applyPendingOperations(ctx, shadow, shadow.asObjectable().getPendingOperation(), false, now);
	}
	
	public PrismObject<ShadowType> applyPendingOperations(ProvisioningContext ctx,
			PrismObject<ShadowType> shadow, 
			List<PendingOperationType> pendingOperations,
			boolean skipExecutionPendingOperations,
			XMLGregorianCalendar now)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		if (shadow == null) {
			return null;
		}
		PrismObject<ShadowType> resultShadow = shadow;
		ShadowType resultShadowType = resultShadow.asObjectable();
		if (pendingOperations.isEmpty()) {
			return shadow;
		}
		List<PendingOperationType> sortedOperations = sortPendingOperations(pendingOperations);
		Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
		boolean resourceReadIsCachingOnly = ProvisioningUtil.resourceReadIsCachingOnly(ctx.getResource());
		for (PendingOperationType pendingOperation: sortedOperations) {
			OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
			PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
			if (resultStatus == OperationResultStatusType.FATAL_ERROR || resultStatus == OperationResultStatusType.NOT_APPLICABLE) {
				continue;
			}
			if (skipExecutionPendingOperations && executionStatus == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
				continue;
			}
			if (ProvisioningUtil.isOverGrace(now, gracePeriod, pendingOperation)) {
				continue;
			}
			if (resourceReadIsCachingOnly) {
				// We are getting the data from our own cache. So we know that all completed operations are already applied in the cache.
				// Re-applying them will mean additional risk of corrupting the data.
				if (resultStatus != null && resultStatus != OperationResultStatusType.IN_PROGRESS && resultStatus != OperationResultStatusType.UNKNOWN) {
					continue;
				}
			} else {
				// We want to apply all the deltas, even those that are already completed. They might not be reflected on the resource yet.
				// E.g. they may be not be present in the CSV export until the next export cycle is scheduled
			}
			ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
			ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
			if (pendingDelta.isAdd()) {
				if (Boolean.FALSE.equals(resultShadowType.isExists())) {
					ShadowType shadowType = shadow.asObjectable();
					resultShadow = pendingDelta.getObjectToAdd().clone();
					resultShadow.setOid(shadow.getOid());
					resultShadowType = resultShadow.asObjectable();
					resultShadowType.setExists(true);
					resultShadowType.setName(shadowType.getName());
					List<PendingOperationType> newPendingOperations = resultShadowType.getPendingOperation();
					for (PendingOperationType pendingOperation2: shadowType.getPendingOperation()) {
						newPendingOperations.add(pendingOperation2.clone());
					}
					applyAttributesDefinition(ctx, resultShadow);
				}
			}
			if (pendingDelta.isModify()) {
				pendingDelta.applyTo(resultShadow);
			}
			if (pendingDelta.isDelete()) {
				resultShadowType.setDead(true);
				resultShadowType.setExists(false);
			}
		}
		// TODO: check schema, remove non-readable attributes, activation, password, etc.
//		CredentialsType creds = resultShadowType.getCredentials();
//		if (creds != null) {
//			PasswordType passwd = creds.getPassword();
//			if (passwd != null) {
//				passwd.setValue(null);
//			}
//		}
		return resultShadow;
	}
	
	public List<PendingOperationType> sortPendingOperations(List<PendingOperationType> pendingOperations) {
		// Copy to mutable list that is not bound to the prism
		List<PendingOperationType> sortedList = new ArrayList<>(pendingOperations.size());
		sortedList.addAll(pendingOperations);
		sortedList.sort((o1, o2) -> XmlTypeConverter.compare(o1.getRequestTimestamp(), o2.getRequestTimestamp()));
		return sortedList;
	}

}
