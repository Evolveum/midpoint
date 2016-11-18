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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
@Component
public class MetadataManager {
	
	private static final Trace LOGGER = TraceManager.getTrace(MetadataManager.class);
	
	// for inserting workflow-related metadata to changed object
	@Autowired(required = false)
	private WorkflowManager workflowManager;

	@Autowired(required = true)
	private PrismContext prismContext;
	
	public <F extends FocusType> void applyRequestFocusMetadata(LensContext<F> context,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		
		if (focusContext.isDelete()) {
			return;
		}
		
		// Not entirely correct. We should not modify the primary delta.
		// But this is an efficient way how to implement this quickly.
		// TODO: fix later, maybe with special metadata delta in the context 
		// MID-3530
		
		ObjectDelta<F> primaryDelta = focusContext.getPrimaryDelta();
		if (primaryDelta == null || primaryDelta.isEmpty()) {
			return;
		}
		
		if (focusContext.isAdd()) {
			PrismObject<F> objectToAdd = primaryDelta.getObjectToAdd();
			
			applyMetadataAdd(context, objectToAdd, AuthorizationPhaseType.REQUEST, now, task, result);
			
		} else {
			
			applyMetadataModify(primaryDelta, focusContext, focusContext.getObjectTypeClass(), 
					AuthorizationPhaseType.REQUEST, now, task, context, result);
			
		}
		
		
	}
	
	public <T extends ObjectType, F extends ObjectType> void applyMetadataAdd(LensContext<F> context,
			PrismObject<T> object, AuthorizationPhaseType phase,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		
		T objectType = object.asObjectable();
		MetadataType metadataType = objectType.getMetadata();
		if (metadataType == null) {
			metadataType = new MetadataType();
			objectType.setMetadata(metadataType);
		}
		
		if (phase == AuthorizationPhaseType.REQUEST) {
			
			if (metadataType.getRequestTimestamp() == null) {
				applyRequestMetadata(context, metadataType, now, task);
			}
			
		} else {

			applyCreateMetadata(context, metadataType, now, task);
		
			if (workflowManager != null) {
				metadataType.getCreateApproverRef().addAll(workflowManager.getApprovedBy(task, result));
			}
			
		}
		
		if (object.canRepresent(FocusType.class)) {
			applyAssignmentMetadata((LensContext<? extends FocusType>) context, phase, now, task, result);
		}
		
	}
	
	public <T extends ObjectType, F extends ObjectType> void applyMetadataModify(ObjectDelta<T> objectDelta,
			LensElementContext<T> objectContext, Class objectTypeClass, AuthorizationPhaseType phase, 
			XMLGregorianCalendar now, Task task, LensContext<F> context,
			OperationResult result) throws SchemaException {
		String channel = LensUtil.getChannel(context, task);

		PrismObjectDefinition<T> def = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(objectTypeClass);

		ItemDelta.mergeAll(objectDelta.getModifications(), createModifyMetadataDeltas(context,
				new ItemPath(ObjectType.F_METADATA), def, now, task));

		List<PrismReferenceValue> approverReferenceValues = new ArrayList<PrismReferenceValue>();

		if (workflowManager != null) {
			for (ObjectReferenceType approverRef : workflowManager.getApprovedBy(task, result)) {
				approverReferenceValues.add(new PrismReferenceValue(approverRef.getOid()));
			}
		}
		if (!approverReferenceValues.isEmpty()) {
			ReferenceDelta refDelta = ReferenceDelta.createModificationReplace(
					(new ItemPath(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)), def,
					approverReferenceValues);
			((Collection) objectDelta.getModifications()).add(refDelta);
		} else {

			// a bit of hack - we want to replace all existing values with empty
			// set of values;
			// however, it is not possible to do this using REPLACE, so we have
			// to explicitly remove all existing values

			if (objectContext != null && objectContext.getObjectOld() != null) {
				// a null value of objectOld means that we execute MODIFY delta
				// that is a part of primary ADD operation (in a wave greater
				// than 0)
				// i.e. there are NO modifyApprovers set (theoretically they
				// could be set in previous waves, but because in these waves
				// the data
				// are taken from the same source as in this step - so there are
				// none modify approvers).

				if (objectContext.getObjectOld().asObjectable().getMetadata() != null) {
					List<ObjectReferenceType> existingModifyApproverRefs = objectContext.getObjectOld()
							.asObjectable().getMetadata().getModifyApproverRef();
					LOGGER.trace("Original values of MODIFY_APPROVER_REF: {}", existingModifyApproverRefs);

					if (!existingModifyApproverRefs.isEmpty()) {
						List<PrismReferenceValue> valuesToDelete = new ArrayList<PrismReferenceValue>();
						for (ObjectReferenceType approverRef : objectContext.getObjectOld().asObjectable()
								.getMetadata().getModifyApproverRef()) {
							valuesToDelete.add(approverRef.asReferenceValue().clone());
						}
						ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(
								(new ItemPath(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)),
								def, valuesToDelete);
						((Collection) objectDelta.getModifications()).add(refDelta);
					}
				}
			}
		}

		if (FocusType.class.isAssignableFrom(objectTypeClass)) {
			applyAssignmentMetadataDelta((LensContext) context, 
					(ObjectDelta)objectDelta, phase, now, task, result);
		}
	}
	
	public <F extends FocusType> void applyAssignmentMetadata(LensContext<F> context,
			AuthorizationPhaseType phase,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		
		// This is not entirely correct. But we cannot really create secondary deltas for
		// assignment metadata here. The assignment containers do not have IDs yet.
		// So we cannot create a stand-alone deltas for them. So just add metadata
		// to the primary delta. Strictly speaking we should not modify the primary
		// delta. But this is the least evil here.
		// MID-3530
		
		LensFocusContext<F> focusContext = context.getFocusContext();

		applyAssignmentMetadataDelta(context, focusContext.getPrimaryDelta(), phase, now, task, result);
		applyAssignmentMetadataDelta(context, focusContext.getSecondaryDelta(), phase, now, task, result);
		
	}
	
	private <F extends FocusType> void applyAssignmentMetadataDelta(LensContext<F> context, ObjectDelta<F> objectDelta,
			AuthorizationPhaseType phase,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

		if (objectDelta == null || objectDelta.isDelete()) {
			return;
		}
		
		if (objectDelta.isAdd()) {
			
			PrismObject<F> objectToAdd = objectDelta.getObjectToAdd();
			PrismContainer<AssignmentType> assignmentContainer = objectToAdd.findContainer(FocusType.F_ASSIGNMENT);
			if (assignmentContainer != null) {
				for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentContainer.getValues()) {
					applyAssignmentValueMetadataAdd(context, assignmentContainerValue, phase, "ADD", now, task, result);
				}
			}
			
		} else {
			
			for (ItemDelta<?,?> itemDelta: objectDelta.getModifications()) {
				if (itemDelta.getPath().equivalent(SchemaConstants.PATH_ASSIGNMENT)) {
					ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>)itemDelta;
					if (assignmentDelta.getValuesToAdd() != null) {
						for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToAdd()) {
							applyAssignmentValueMetadataAdd(context, assignmentContainerValue, phase, "MOD/add", now, task, result);
						}
					}
					if (assignmentDelta.getValuesToReplace() != null) {
						for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToReplace()) {
							applyAssignmentValueMetadataAdd(context, assignmentContainerValue, phase, "MOD/replace", now, task, result);
						}
					}
				}
				// TODO: assignment modification
			}
			
		}
		
	}
	
	private <F extends FocusType> void applyAssignmentValueMetadataAdd(LensContext<F> context,
			PrismContainerValue<AssignmentType> assignmentContainerValue, AuthorizationPhaseType phase, String desc,
			XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		
		AssignmentType assignmentType = assignmentContainerValue.asContainerable();
		MetadataType metadataType = assignmentType.getMetadata();
		if (metadataType == null) {
			metadataType = new MetadataType();
			assignmentType.setMetadata(metadataType);
		}
		
		if (phase == AuthorizationPhaseType.REQUEST) {
		
			if (metadataType.getRequestTimestamp() == null) {
				applyRequestMetadata(context, metadataType, now, task);
			}
			
		} else {

			applyCreateMetadata(context, metadataType, now, task);
						
		}
		
		LOGGER.trace("Adding {} METADATA {} to assignment cval ({}):\n{}", 
				phase, metadataType, desc, assignmentContainerValue.debugDump(1));
	}

	public <F extends ObjectType> void applyRequestMetadata(LensContext<F> context, MetadataType metaData, XMLGregorianCalendar now, Task task) {
		String channel = LensUtil.getChannel(context, task);
		metaData.setCreateChannel(channel);
		metaData.setRequestTimestamp(now);
		if (task.getOwner() != null) {
			metaData.setRequestorRef(ObjectTypeUtil.createObjectRef(task.getOwner()));
		}
	}
	
	public <F extends ObjectType> MetadataType createCreateMetadata(LensContext<F> context, XMLGregorianCalendar now, Task task) {
		MetadataType metaData = new MetadataType();
		applyCreateMetadata(context, metaData, now, task);
		return metaData;
	}
	
	public <F extends ObjectType> void applyCreateMetadata(LensContext<F> context, MetadataType metaData, XMLGregorianCalendar now, Task task) {
		String channel = LensUtil.getChannel(context, task);
		metaData.setCreateChannel(channel);
		metaData.setCreateTimestamp(now);
		if (task.getOwner() != null) {
			metaData.setCreatorRef(ObjectTypeUtil.createObjectRef(task.getOwner()));
		}
	}
	
	public <F extends ObjectType, T extends ObjectType> Collection<? extends ItemDelta<?,?>> createModifyMetadataDeltas(LensContext<F> context, 
			ItemPath metadataPath, PrismObjectDefinition<T> def, XMLGregorianCalendar now, Task task) {
		Collection<? extends ItemDelta<?,?>> deltas = new ArrayList<>();
		String channel = LensUtil.getChannel(context, task);
		if (channel != null) {
            PropertyDelta<String> delta = PropertyDelta.createModificationReplaceProperty(metadataPath.subPath(MetadataType.F_MODIFY_CHANNEL), def, channel);
            ((Collection)deltas).add(delta);
        }
		PropertyDelta<XMLGregorianCalendar> delta = PropertyDelta.createModificationReplaceProperty(metadataPath.subPath(MetadataType.F_MODIFY_TIMESTAMP), def, now);
		((Collection)deltas).add(delta);
		if (task.getOwner() != null) {
            ReferenceDelta refDelta = ReferenceDelta.createModificationReplace(
            		metadataPath.subPath(MetadataType.F_MODIFIER_REF), def, task.getOwner().getOid());
            ((Collection)deltas).add(refDelta);
		}
		return deltas;
	}

}
