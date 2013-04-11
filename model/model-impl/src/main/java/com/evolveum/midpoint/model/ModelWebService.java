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

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.api.ModelPort;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.PagingConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
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
	private PrismContext prismContext;

	@Override
	public void addObject(ObjectType objectType, Holder<String> oidHolder, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(objectType, "Object must not be null.");

		Task task = createTaskInstance(ADD_OBJECT);
		OperationResult operationResult = task.getResult();
		try {
			PrismObject object = objectType.asPrismObject();
			prismContext.adopt(objectType);
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
	public void getObject(String objectTypeUri, String oid, OperationOptionsType options,
			Holder<ObjectType> objectHolder, Holder<OperationResultType> resultHolder) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(options, "options  must not be null.");

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(GET_OBJECT);
			operationResult = task.getResult();
			
			PrismObject<? extends ObjectType> object = model.getObject(
					ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), oid, 
					MiscSchemaUtil.optionsTypeToOptions(options), task, operationResult);
			handleOperationResult(operationResult, resultHolder);
			objectHolder.value = object.asObjectable();
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listObjects(String objectType, PagingType paging, OperationOptionsType options,
                    Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(objectType, "Object type must not be null or empty.");

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(LIST_OBJECTS);
			operationResult = task.getResult();
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
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void searchObjects(String objectTypeUri, QueryType query, OperationOptionsType options,
                  Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(query, "Query must not be null.");

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(SEARCH_OBJECTS);
			operationResult = task.getResult();
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
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public OperationResultType modifyObject(String objectTypeUri, ObjectModificationType change) throws FaultMessage {
		notNullArgument(change, "Object modification must not be null.");

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(MODIFY_OBJECT);
			operationResult = task.getResult();
			Class<? extends ObjectType> type = ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition();
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(change, type, prismContext);
			model.modifyObject(type, change.getOid(),
					modifications , task, operationResult);
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

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(DELETE_OBJECT);
			operationResult = task.getResult();
			model.deleteObject(ObjectTypes.getObjectTypeFromUri(objectTypeUri).getClassDefinition(), oid,
					task, operationResult);
			return handleOperationResult(operationResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL deleteObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void listAccountShadowOwner(String accountOid, Holder<UserType> userHolder, 
			Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(accountOid, "Account oid must not be null or empty.");

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(LIST_ACCOUNT_SHADOW_OWNER);
			operationResult = task.getResult();
			PrismObject<UserType> user = model.findShadowOwner(accountOid, task, operationResult);
			handleOperationResult(operationResult, result);
			if (user != null) {
				userHolder.value = user.asObjectable();
			}
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listAccountShadowOwner() failed", ex);
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
	public void listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			Holder<ObjectListType> objectListTypeHolder,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullArgument(objectType, "Object type must not be null.");
		notNullArgument(paging, "Paging  must not be null.");

		OperationResult operationResult = null;
		try {
			Task task = createTaskInstance(LIST_RESOURCE_OBJECTS);
			operationResult = task.getResult();
			List<PrismObject<? extends ShadowType>> list = model.listResourceObjects(
					resourceOid, objectType, PagingConvertor.createObjectPaging(paging), task, operationResult);
			handleOperationResult(operationResult, result);
			ObjectListType listType = new ObjectListType();
			for (PrismObject<? extends ShadowType> o : list) {
				listType.getObject().add(o.asObjectable());
			}
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
			Task task = createTaskInstance(TEST_RESOURCE);
			OperationResult testResult = model.testResource(resourceOid, task);
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
		OperationResult operationResult = task.getResult();

		try {
			model.importFromResource(resourceOid, objectClass, task, operationResult);
			operationResult.computeStatus();
			return handleTaskResult(task);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL importFromResource() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
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
}
