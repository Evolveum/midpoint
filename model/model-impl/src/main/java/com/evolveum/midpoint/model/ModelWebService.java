/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import java.util.List;

import javax.jws.WebParam;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ModelPort;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Service
public class ModelWebService implements ModelPortType, ModelPort {

	private static final Trace LOGGER = TraceManager.getTrace(ModelWebService.class);
	@Autowired(required = true)
	private ModelController model;
	@Autowired(required = true)
	private TaskManager taskManager;

	@Override
	public void addObject(ObjectType object, Holder<String> oidHolder, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(object, "Object must not be null.");

		OperationResult operationResult = new OperationResult(ADD_OBJECT);
		Task task = createTaskInstance();
		try {
			String oid = model.addObject(object, task, operationResult);
			handleOperationResult(operationResult, result);
			oidHolder.value = oid;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL addObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void getObject(String objectTypeUri, String oid, PropertyReferenceListType resolve,
			Holder<ObjectType> objectHolder, Holder<OperationResultType> resultHolder) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(resolve, "Property reference list  must not be null.");

		OperationResult operationResult = new OperationResult(GET_OBJECT);
		try {
			ObjectType object = model.getObject(ObjectTypes.getObjectTypeFromUri(objectTypeUri)
					.getClassDefinition(), oid, resolve, operationResult);
			handleOperationResult(operationResult, resultHolder);
			objectHolder.value = object;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listObjects(String objectType, PagingType paging, Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(objectType, "Object type must not be null or empty.");

		OperationResult operationResult = new OperationResult(LIST_OBJECTS);
		try {
			ResultList<? extends ObjectType> list = model.listObjects(ObjectTypes.getObjectTypeFromUri(objectType)
					.getClassDefinition(), paging, operationResult);
			handleOperationResult(operationResult, result);

			ObjectListType listType = new ObjectListType();
			for (ObjectType o : list) {
				listType.getObject().add(o);
			}
			listType.setCount(list.getTotalResultCount());
			objectListHolder.value = listType;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listObjects() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void searchObjects(String objectTypeUri, QueryType query, PagingType paging,
			Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(query, "Query must not be null.");

		OperationResult operationResult = new OperationResult(SEARCH_OBJECTS);
		try {
			ResultList<? extends ObjectType> list = model.searchObjects(
					ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), query, paging,
					operationResult);
			handleOperationResult(operationResult, result);
			ObjectListType listType = new ObjectListType();
			for (ObjectType o : list) {
				listType.getObject().add(o);
			}
			listType.setCount(list.getTotalResultCount());
			objectListHolder.value = listType;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL searchObjects() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public OperationResultType modifyObject(String objectTypeUri, ObjectModificationType change) throws FaultMessage {
		notNullArgument(change, "Object modification must not be null.");

		OperationResult operationResult = new OperationResult(MODIFY_OBJECT);
		Task task = createTaskInstance();
		try {
			model.modifyObject(ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), change,
					task, operationResult);
			return handleOperationResult(operationResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL modifyObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public OperationResultType deleteObject(String objectTypeUri, String oid)
			throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notEmptyArgument(objectTypeUri, "objectType must not be null or empty.");

		OperationResult operationResult = new OperationResult(DELETE_OBJECT);
		try {
			model.deleteObject(ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), oid,
					operationResult);
			return handleOperationResult(operationResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL deleteObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void getPropertyAvailableValues(String oid, PropertyReferenceListType properties, 
			Holder<PropertyAvailableValuesListType> propertyAvailableValuesListHolder,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(properties, "Property reference list must not be null.");

		OperationResult operationResult = new OperationResult(GET_PROPERTY_AVAILABLE_VALUES);
		try {
			PropertyAvailableValuesListType list = model.getPropertyAvailableValues(oid, properties,
					operationResult);
			handleOperationResult(operationResult, result);
			propertyAvailableValuesListHolder.value = list;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getPropertyAvailableValues() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listAccountShadowOwner(String accountOid, Holder<UserType> userHolder, 
			Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(accountOid, "Account oid must not be null or empty.");

		OperationResult operationResult = new OperationResult(LIST_ACCOUNT_SHADOW_OWNER);
		try {
			UserType user = model.listAccountShadowOwner(accountOid, operationResult);
			handleOperationResult(operationResult, result);
			userHolder.value = user;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listAccountShadowOwner() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void listResourceObjectShadows(String resourceOid, String resourceObjectShadowType,
			Holder<ResourceObjectShadowListType> resourceObjectShadowListHolder,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notEmptyArgument(resourceObjectShadowType, "Resource object shadow type must not be null or empty.");

		OperationResult operationResult = new OperationResult(LIST_RESOURCE_OBJECT_SHADOWS);
		try {
			List<ResourceObjectShadowType> list = model.listResourceObjectShadows(
					resourceOid,
					(Class<ResourceObjectShadowType>) ObjectTypes.getObjectTypeFromUri(
							resourceObjectShadowType).getClassDefinition(), operationResult);
			handleOperationResult(operationResult, result);

			ResourceObjectShadowListType shadowList = new ResourceObjectShadowListType();
			shadowList.getObject().addAll(list);
			resourceObjectShadowListHolder.value = shadowList;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listResourceObjectShadows() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			Holder<ObjectListType> objectListTypeHolder,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullArgument(objectType, "Object type must not be null.");
		notNullArgument(paging, "Paging  must not be null.");

		OperationResult operationResult = new OperationResult(LIST_RESOURCE_OBJECTS);
		try {
			ResultList<? extends ResourceObjectShadowType> list = model.listResourceObjects(resourceOid, objectType, paging, operationResult);
			handleOperationResult(operationResult, result);
			ObjectListType listType = new ObjectListType();
			for (ObjectType o : list) {
				listType.getObject().add(o);
			}
			listType.setCount(list.getTotalResultCount());
			objectListTypeHolder.value = listType;
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listResourceObjects() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public OperationResultType testResource(String resourceOid) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");

		try {
			OperationResult testResult = model.testResource(resourceOid);
			return handleOperationResult(testResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL testResource() failed", ex);
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
		} else {
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

		Task task = taskManager.createTaskInstance(IMPORT_FROM_RESOURCE);
		OperationResult operationResult = task.getResult();

		try {
			model.importAccountsFromResource(resourceOid, objectClass, task, operationResult);
			operationResult.computeStatus();
			return handleTaskResult(task);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL importFromResource() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}
	
	private Task createTaskInstance() {
		// TODO: better task initialization
		return taskManager.createTaskInstance();
	}
	
	/**
	 * return appropriate form of taskType (and result) to
	 * return back to a web service caller.
	 * 
	 * @param task
	 * @param taskHolder
	 */
	private TaskType handleTaskResult(Task task) {
		return task.getTaskTypeObject();
	}
}
