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
package com.evolveum.midpoint.model;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.api.ModelPort;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceEventDescription;
import com.evolveum.midpoint.provisioning.api.ResourceEventListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PagingConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectAlreadyExistsFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * 
 * @author lazyman
 * 
 */
@Service
public class ModelWebService implements ModelPortType, ModelPort {

	private static final Trace LOGGER = TraceManager.getTrace(ModelWebService.class);
	
	@Autowired(required = true)
	private ModelCrudService model;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Autowired(required = true)
	private AuditService auditService;
	
	@Autowired(required = true)
	private ChangeNotificationDispatcher dispatcher;
	
	@Autowired(required = true)
	private PrismContext prismContext;

	@Override
	public void addObject(ObjectType objectType, Holder<String> oidHolder, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(objectType, "Object must not be null.");

		Task task = createTaskInstance(ADD_OBJECT);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			PrismObject object = objectType.asPrismObject();
			prismContext.adopt(objectType);
			String oid = model.addObject(object, null, task, operationResult);
			handleOperationResult(operationResult, result);
			oidHolder.value = oid;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL addObject() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void getObject(String objectTypeUri, String oid, OperationOptionsType options,
			Holder<ObjectType> objectHolder, Holder<OperationResultType> resultHolder) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(options, "options  must not be null.");

		Task task = createTaskInstance(GET_OBJECT);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {			
			PrismObject<? extends ObjectType> object = model.getObject(
					ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), oid,
					MiscSchemaUtil.optionsTypeToOptions(options), task, operationResult);
			handleOperationResult(operationResult, resultHolder);
			objectHolder.value = object.asObjectable();
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getObject() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listObjects(String objectType, PagingType paging, OperationOptionsType options,
                    Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(objectType, "Object type must not be null or empty.");

		Task task = createTaskInstance(LIST_OBJECTS);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			ObjectQuery query = ObjectQuery.createObjectQuery(PagingConvertor.createObjectPaging(paging));
			List<PrismObject<? extends ObjectType>> list = (List) model.searchObjects(ObjectTypes.getObjectTypeFromUri(objectType)
					.getClassDefinition(), query, MiscSchemaUtil.optionsTypeToOptions(options), task, operationResult);
			handleOperationResult(operationResult, result);

			ObjectListType listType = new ObjectListType();
			for (PrismObject<? extends ObjectType> o : list) {
				listType.getObject().add(o.asObjectable());
			}
			objectListHolder.value = listType;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listObjects() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void searchObjects(String objectTypeUri, QueryType query, OperationOptionsType options,
                  Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(query, "Query must not be null.");

		Task task = createTaskInstance(SEARCH_OBJECTS);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			ObjectQuery q = QueryConvertor.createObjectQuery(ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), query, prismContext);
			List<PrismObject<? extends ObjectType>> list = (List)model.searchObjects(
					ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), q,
                    MiscSchemaUtil.optionsTypeToOptions(options), task, operationResult);
			handleOperationResult(operationResult, result);
			ObjectListType listType = new ObjectListType();
			for (PrismObject<? extends ObjectType> o : list) {
				listType.getObject().add(o.asObjectable());
			}
			objectListHolder.value = listType;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL searchObjects() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public OperationResultType modifyObject(String objectTypeUri, ObjectModificationType change) throws FaultMessage {
		notNullArgument(change, "Object modification must not be null.");

		Task task = createTaskInstance(MODIFY_OBJECT);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			Class<? extends ObjectType> type = ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition();
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(change, type, prismContext);
			model.modifyObject(type, change.getOid(),
					modifications, null, task, operationResult);
			return handleOperationResult(operationResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL modifyObject() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public OperationResultType deleteObject(String objectTypeUri, String oid)
			throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notEmptyArgument(objectTypeUri, "objectType must not be null or empty.");

		Task task = createTaskInstance(DELETE_OBJECT);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			model.deleteObject(ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), oid,
					null, task, operationResult);
			return handleOperationResult(operationResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL deleteObject() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listAccountShadowOwner(String accountOid, Holder<UserType> userHolder, 
			Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(accountOid, "Account oid must not be null or empty.");

		Task task = createTaskInstance(LIST_ACCOUNT_SHADOW_OWNER);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			PrismObject<UserType> user = model.findShadowOwner(accountOid, task, operationResult);
			handleOperationResult(operationResult, result);
			if (user != null) {
				userHolder.value = user.asObjectable();
			}
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listAccountShadowOwner() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listResourceObjectShadows(String resourceOid, String resourceObjectShadowType,
			Holder<ResourceObjectShadowListType> resourceObjectShadowListHolder,
			Holder<OperationResultType> result) throws FaultMessage {
		throw new UnsupportedOperationException("Not supported. DEPRECATED. Use searchObjects instead.");
	}

	@Override
	public OperationResultType testResource(String resourceOid) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");

		Task task = createTaskInstance(TEST_RESOURCE);
		auditLogin(task);
		try {
			OperationResult testResult = model.testResource(resourceOid, task);
			return handleOperationResult(testResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL testResource() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, null);
		}
	}

	private void handleOperationResult(OperationResult result, Holder<OperationResultType> holder) {
		result.recordSuccess();
		OperationResultType resultType = result.createOperationResultType();
		if (holder.value == null) {
			holder.value = resultType;
		} else {
			holder.value.getPartialResults().add(resultType);
		}
	}

	private OperationResultType handleOperationResult(OperationResult result) {
		result.recordSuccess();
		return result.createOperationResultType();
	}
	
	private void notNullResultHolder(Holder<OperationResultType> holder) throws FaultMessage {
		notNullArgument(holder, "Holder must not be null.");
		notNullArgument(holder.value, "Result type must not be null.");
	}

	private <T> void notNullHolder(Holder<T> holder) throws FaultMessage {
		notNullArgument(holder, "Holder must not be null.");
		notNullArgument(holder.value, holder.getClass().getSimpleName() + " must not be null (in Holder).");
	}

	private void notEmptyArgument(String object, String message) throws FaultMessage {
		if (StringUtils.isEmpty(object)) {
			throw createIllegalArgumentFault(message);
		}
	}

	private void notNullArgument(Object object, String message) throws FaultMessage {
		if (object == null) {
			throw createIllegalArgumentFault(message);
		}
	}

	private FaultMessage createIllegalArgumentFault(String message) {
		FaultType faultType = new IllegalArgumentFaultType();
		return new FaultMessage(message, faultType);
	}

	private FaultMessage createSystemFault(Exception ex, OperationResult result) {
		if (result != null) {
			result.recordFatalError(ex.getMessage(), ex);
		}

		FaultType faultType;
		if (ex instanceof ObjectNotFoundException) {
			faultType = new ObjectNotFoundFaultType();
		} else if (ex instanceof IllegalArgumentException) {
			faultType = new IllegalArgumentFaultType();
		} else if (ex instanceof ObjectAlreadyExistsException){
			faultType = new ObjectAlreadyExistsFaultType();
		} else{
			faultType = new SystemFaultType();
		}
		faultType.setMessage(ex.getMessage());
		if (result != null) {
			faultType.setOperationResult(result.createOperationResultType());
		}

		return new FaultMessage(ex.getMessage(), faultType, ex);
	}

	@Override
	public TaskType importFromResource(String resourceOid, QName objectClass)
			throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullArgument(objectClass, "Object class must not be null.");

		Task task = createTaskInstance(IMPORT_FROM_RESOURCE);
		auditLogin(task);
		OperationResult operationResult = task.getResult();

		try {
			model.importFromResource(resourceOid, objectClass, task, operationResult);
			operationResult.computeStatus();
			return handleTaskResult(task);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL importFromResource() failed", ex);
			auditLogout(task);
			throw createSystemFault(ex, operationResult);
		}
	}
	
	@Override
	public TaskType notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription)
			throws FaultMessage {
		// TODO Auto-generated method stub
		notNullArgument(changeDescription, "Change description must not be null");
		LOGGER.info("notify change started");
		
		Task task = createTaskInstance(NOTIFY_CHANGE);
		OperationResult parentResult = task.getResult();
		
		String oldShadowOid = changeDescription.getOldShadowOid();
		
		ResourceEventDescription eventDescription = new ResourceEventDescription();
		
		try {
			PrismObject<ShadowType> oldShadow = null;
			LOGGER.info("resolving old object");
			if (!StringUtils.isEmpty(oldShadowOid)){
				oldShadow = model.getObject(ShadowType.class, oldShadowOid, SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, parentResult);
				eventDescription.setOldShadow(oldShadow);
				LOGGER.info("old object resolved to: {}", oldShadow.dump());
			} else{
				LOGGER.info("Old shadow null");
			}
			
				PrismObject<ShadowType> currentShadow = null;
				ShadowType currentShadowType = changeDescription.getCurrentShadow();
				LOGGER.info("resolving current shadow");
				if (currentShadowType != null){
					prismContext.adopt(currentShadowType);
					currentShadow = currentShadowType.asPrismObject();
					LOGGER.info("current shadow resolved to {}", currentShadow.dump());
				}
				
				eventDescription.setCurrentShadow(currentShadow);
				
				ObjectDeltaType deltaType = changeDescription.getObjectDelta();
				ObjectDelta delta = null;
				LOGGER.info("resolving resource");
				PrismObject<ShadowType> shadowToAdd = null;
				if (deltaType != null){
				
					delta = ObjectDelta.createEmptyDelta(ShadowType.class, deltaType.getOid(), prismContext, ChangeType.toChangeType(deltaType.getChangeType()));
					
					if (delta.getChangeType() == ChangeType.ADD) {
						LOGGER.info("determined ADD change ");
						if (deltaType.getObjectToAdd() == null){
							LOGGER.info("No object to add specified. Check your delta. Add delta must contain object to add");
							createIllegalArgumentFault("No object to add specified. Check your delta. Add delta must contain object to add");
							return handleTaskResult(task);
						}
						Object objToAdd = deltaType.getObjectToAdd().getAny();
						if (!(objToAdd instanceof ShadowType)){
							LOGGER.info("Wrong object specified in change description. Expected on the the shadow type, but got " + objToAdd.getClass().getSimpleName());
							createIllegalArgumentFault("Wrong object specified in change description. Expected on the the shadow type, but got " + objToAdd.getClass().getSimpleName());
							return handleTaskResult(task);
						}
						prismContext.adopt((ShadowType)objToAdd);
						
						shadowToAdd = ((ShadowType) objToAdd).asPrismObject();
						LOGGER.info("object to add: {}", shadowToAdd.dump());
						delta.setObjectToAdd(shadowToAdd);
					} else {
						Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(deltaType.getModification(), prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class));
						delta.getModifications().addAll(modifications);
					} 
				}
				eventDescription.setDelta(delta);
//				LOGGER.info("resolving shadow for resource");
//				PrismObject<ShadowType> shadowToResolveResource = null;
//				if (oldShadow != null){
//					shadowToResolveResource = oldShadow;
//				} else if (currentShadow != null){
//					shadowToResolveResource = currentShadow;
//				} else if (shadowToAdd != null){
//					shadowToResolveResource = shadowToAdd;
//				} else {
//					createIllegalArgumentFault("Could not determine resource from the change description. Description does not contain neither shadow, or delta.");
//				}
//			
//				PrismObject<ResourceType> resource = null;
//				
//				LOGGER.info("shadow to resolve resource: {}", shadowToResolveResource.dump());
//				
//				ResourceType resourceType = shadowToResolveResource.asObjectable().getResource();
//					if (resourceType != null){
//						resource = resourceType.asPrismObject();
//					} else {
//						resource = model.getObject(ResourceType.class, ShadowUtil.getResourceOid(shadowToResolveResource), null, task, parentResult);
//					}
//					LOGGER.info("Resource resolved: {}", resource != null ? resource.dump() : "noResource");
//					notificationDescription.setResource(resource);
					
				eventDescription.setSourceChannel(changeDescription.getChannel());
			
					dispatcher.notifyEvent(eventDescription, task, parentResult);
					parentResult.computeStatus();
					task.setResult(parentResult);
			} catch (ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			} catch (SchemaException ex) {
				 LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			} catch (CommunicationException ex) {
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			} catch (ConfigurationException ex) {
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			} catch (SecurityViolationException ex) {
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			} catch (RuntimeException ex){
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			} catch (ObjectAlreadyExistsException ex){
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throw createSystemFault(ex, parentResult);
			}
		
		
		LOGGER.info("notify change ended.");
		LOGGER.info("result of notify change: {}", parentResult.dump());
		return handleTaskResult(task);
	}


    private void setTaskOwner(Task task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new SystemException("Failed to get authentication object");
        }
        UserType userType = (UserType) ((MidPointPrincipal)(SecurityContextHolder.getContext().getAuthentication().getPrincipal())).getUser();
        if (userType == null) {
            throw new SystemException("Failed to get user from authentication object");
        }
        task.setOwner(userType.asPrismObject());
    }

    private Task createTaskInstance(String operationName) {
		// TODO: better task initialization
		Task task = taskManager.createTaskInstance(operationName);
		setTaskOwner(task);
		task.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
		return task;
	}
	
	/**
	 * return appropriate form of taskType (and result) to
	 * return back to a web service caller.
	 * 
	 * @param task
	 */
	private TaskType handleTaskResult(Task task) {
		return task.getTaskPrismObject().asObjectable();
	}
	
	private void auditLogin(Task task) {
        AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        PrismObject<UserType> owner = task.getOwner();
        if (owner != null) {
	        record.setInitiator(owner);
	        PolyStringType name = owner.asObjectable().getName();
	        if (name != null) {
	        	record.setParameter(name.getOrig());
	        }
        }

        record.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setSessionIdentifier(task.getTaskIdentifier());
        
        record.setOutcome(OperationResultStatus.SUCCESS);

        auditService.audit(record, task);
	}
	
	private void auditLogout(Task task) {
		AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
		PrismObject<UserType> owner = task.getOwner();
        if (owner != null) {
	        record.setInitiator(owner);
	        PolyStringType name = owner.asObjectable().getName();
	        if (name != null) {
	        	record.setParameter(name.getOrig());
	        }
        }

        record.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setSessionIdentifier(task.getTaskIdentifier());
        
        record.setOutcome(OperationResultStatus.SUCCESS);

        auditService.audit(record, task);
	}
}
