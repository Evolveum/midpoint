/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;

import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.EQUIVALENT;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.SUPERPATH;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * @author semancik
 *
 */
@Component
public class OperationalDataManager {

	private static final Trace LOGGER = TraceManager.getTrace(OperationalDataManager.class);

	@Autowired(required = false)
	private ActivationComputer activationComputer;

	// for inserting workflow-related metadata to changed object
	@Autowired(required = false)
	private WorkflowManager workflowManager;

	@Autowired
	private PrismContext prismContext;

	public <F extends ObjectType> void setRequestMetadataInContext(LensContext<F> context, XMLGregorianCalendar now, Task task)
			throws SchemaException {
		MetadataType requestMetadata = collectRequestMetadata(context, now, task);
		context.setRequestMetadata(requestMetadata);
	}

	private <F extends ObjectType> MetadataType collectRequestMetadata(LensContext<F> context, XMLGregorianCalendar now, Task task) {
		MetadataType metaData = new MetadataType();
		metaData.setCreateChannel(LensUtil.getChannel(context, task));      // TODO is this really used?
		metaData.setRequestTimestamp(now);
		if (task.getOwner() != null) {
			metaData.setRequestorRef(createObjectRef(task.getOwner()));
		}
		// It is not necessary to store requestor comment here as it is preserved in context.options field.
		return metaData;
	}

	private <F extends ObjectType> void transplantRequestMetadata(LensContext<F> context, MetadataType metaData) {
		MetadataType requestMetadata = context.getRequestMetadata();
		if (requestMetadata != null) {
			metaData.setRequestTimestamp(requestMetadata.getRequestTimestamp());
			metaData.setRequestorRef(requestMetadata.getRequestorRef());
		}
		OperationBusinessContextType businessContext = context.getRequestBusinessContext();
		if (businessContext != null) {
			metaData.setRequestorComment(businessContext.getComment());
		}
	}

	public <T extends ObjectType, F extends ObjectType> void applyMetadataAdd(LensContext<F> context,
			PrismObject<T> objectToAdd, XMLGregorianCalendar now, Task task, OperationResult result)
					throws SchemaException {

		T objectType = objectToAdd.asObjectable();
		MetadataType metadataType = objectType.getMetadata();
		if (metadataType == null) {
			metadataType = new MetadataType();
			objectType.setMetadata(metadataType);
		}

		transplantRequestMetadata(context, metadataType);

		applyCreateMetadata(context, metadataType, now, task);

		if (workflowManager != null) {
			metadataType.getCreateApproverRef().addAll(workflowManager.getApprovedBy(task, result));
			metadataType.getCreateApprovalComment().addAll(workflowManager.getApproverComments(task, result));
		}

		if (objectToAdd.canRepresent(FocusType.class)) {
			applyAssignmentMetadataObject((LensContext<? extends FocusType>) context, objectToAdd, now, task, result);
		}
	}

	public <T extends ObjectType, F extends ObjectType> void applyMetadataModify(ObjectDelta<T> objectDelta,
			LensElementContext<T> objectContext, Class objectTypeClass,
			XMLGregorianCalendar now, Task task, LensContext<F> context,
			OperationResult result) throws SchemaException {

		ItemDelta.mergeAll(objectDelta.getModifications(),
				createModifyMetadataDeltas(context, new ItemPath(ObjectType.F_METADATA), objectTypeClass, now, task));

		List<PrismReferenceValue> approverReferenceValues = new ArrayList<>();
		List<String> approverComments = new ArrayList<>();
		if (workflowManager != null) {
			for (ObjectReferenceType approverRef : workflowManager.getApprovedBy(task, result)) {
				approverReferenceValues.add(new PrismReferenceValue(approverRef.getOid()));
			}
			approverComments.addAll(workflowManager.getApproverComments(task, result));
		}
		ItemDelta.mergeAll(objectDelta.getModifications(),
				DeltaBuilder.deltaFor(objectTypeClass, prismContext)
						.item(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).replace(approverReferenceValues)
						.item(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVAL_COMMENT).replaceRealValues(approverComments)
						.asItemDeltas());
		if (FocusType.class.isAssignableFrom(objectTypeClass)) {
			applyAssignmentMetadataDelta((LensContext) context,
					(ObjectDelta)objectDelta, now, task, result);
		}
	}

	private <F extends FocusType, T extends ObjectType> void applyAssignmentMetadataObject(LensContext<F> context,
			PrismObject<T> objectToAdd,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

		PrismContainer<AssignmentType> assignmentContainer = objectToAdd.findContainer(FocusType.F_ASSIGNMENT);
		if (assignmentContainer != null) {
			for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentContainer.getValues()) {
				applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "ADD", now, task, result);
			}
		}
	}

	private <F extends FocusType> void applyAssignmentMetadataDelta(LensContext<F> context, ObjectDelta<F> objectDelta,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

		if (objectDelta == null || objectDelta.isDelete()) {
			return;
		}

		if (objectDelta.isAdd()) {
			applyAssignmentMetadataObject(context, objectDelta.getObjectToAdd(), now, task, result);
		} else {
			// see also ApprovalMetadataHelper.addAssignmentApprovalMetadataOnObjectModify
			Set<Long> processedIds = new HashSet<>();
			List<ItemDelta<?,?>> assignmentMetadataDeltas = new ArrayList<>();
			for (ItemDelta<?,?> itemDelta: objectDelta.getModifications()) {
				ItemPath deltaPath = itemDelta.getPath();
				CompareResult comparison = deltaPath.compareComplex(SchemaConstants.PATH_ASSIGNMENT);
				if (comparison == EQUIVALENT) {
					// whole assignment is being added/replaced (or deleted but we are not interested in that)
					ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>)itemDelta;
					if (assignmentDelta.getValuesToAdd() != null) {
						for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToAdd()) {
							applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "MOD/add", now, task, result);
						}
					}
					if (assignmentDelta.getValuesToReplace() != null) {
						for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToReplace()) {
							applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "MOD/replace", now, task, result);
						}
					}
				} else if (comparison == SUPERPATH) {
					ItemPathSegment secondSegment = deltaPath.rest().first();
					if (!(secondSegment instanceof IdItemPathSegment)) {
						throw new IllegalStateException("Assignment modification contains no assignment ID. Offending path = " + deltaPath);
					}
					Long id = ((IdItemPathSegment) secondSegment).getId();
					if (id == null) {
						throw new IllegalStateException("Assignment modification contains no assignment ID. Offending path = " + deltaPath);
					}
					if (processedIds.add(id)) {
						assignmentMetadataDeltas.addAll(createModifyMetadataDeltas(context,
								new ItemPath(FocusType.F_ASSIGNMENT, id, AssignmentType.F_METADATA), context.getFocusClass(), now, task));
					}
				}
			}
			ItemDelta.mergeAll(objectDelta.getModifications(), assignmentMetadataDeltas);
		}
	}

	private <F extends FocusType> void applyAssignmentValueMetadataAdd(LensContext<F> context,
			PrismContainerValue<AssignmentType> assignmentContainerValue, String desc,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

		AssignmentType assignmentType = assignmentContainerValue.asContainerable();
		MetadataType metadataType = assignmentType.getMetadata();
		if (metadataType == null) {
			metadataType = new MetadataType();
			assignmentType.setMetadata(metadataType);
		}

		transplantRequestMetadata(context, metadataType);

		// This applies the effective status only to assignments that are completely new (whole container is added/replaced)
		// The effectiveStatus of existing assignments is processed in FocusProcessor.processAssignmentActivation()
		// We cannot process that here. Because this code is not even triggered when there is no delta. So recompute will not work.
		ActivationType activationType = assignmentType.getActivation();
		ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(assignmentType.getLifecycleState(), activationType);
		if (activationType == null) {
			activationType = new ActivationType();
			assignmentType.setActivation(activationType);
		}
		activationType.setEffectiveStatus(effectiveStatus);

		applyCreateMetadata(context, metadataType, now, task);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Adding operational data {} to assignment cval ({}):\nMETADATA:\n{}\nACTIVATION:\n{}",
				 metadataType, desc, assignmentContainerValue.debugDump(1), activationType.asPrismContainerValue().debugDump(1));
		}
	}

	public <F extends ObjectType> MetadataType createCreateMetadata(LensContext<F> context, XMLGregorianCalendar now, Task task) {
		MetadataType metaData = new MetadataType();
		applyCreateMetadata(context, metaData, now, task);
		return metaData;
	}

	private <F extends ObjectType> void applyCreateMetadata(LensContext<F> context, MetadataType metaData, XMLGregorianCalendar now, Task task) {
		String channel = LensUtil.getChannel(context, task);
		metaData.setCreateChannel(channel);
		metaData.setCreateTimestamp(now);
		if (task.getOwner() != null) {
			metaData.setCreatorRef(createObjectRef(task.getOwner()));
		}
		metaData.setCreateTaskRef(task.getOid() != null ? createObjectRef(task.getTaskPrismObject()) : null);
	}

	public <F extends ObjectType, T extends ObjectType> Collection<ItemDelta<?,?>> createModifyMetadataDeltas(LensContext<F> context,
			ItemPath metadataPath, Class<T> objectType, XMLGregorianCalendar now, Task task) throws SchemaException {
		return DeltaBuilder.deltaFor(objectType, prismContext)
				.item(metadataPath.subPath(MetadataType.F_MODIFY_CHANNEL)).replace(LensUtil.getChannel(context, task))
				.item(metadataPath.subPath(MetadataType.F_MODIFY_TIMESTAMP)).replace(now)
				.item(metadataPath.subPath(MetadataType.F_MODIFIER_REF)).replace(createObjectRef(task.getOwner()))
				.item(metadataPath.subPath(MetadataType.F_MODIFY_TASK_REF)).replaceRealValues(
						task.getOid() != null ? singleton(createObjectRef(task.getTaskPrismObject())) : emptySet())
				.asItemDeltas();
	}

}
