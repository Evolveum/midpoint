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

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Service
public class ModelService implements ModelPortType {

	private static final Trace LOGGER = TraceManager.getTrace(ModelService.class);
	@Autowired(required = true)
	private ModelController model;

	@Override
	public String addObject(ObjectType object, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(object, "Object must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Add Object");
		try {
			String oid = model.addObject(object, operationResult);
			handleOperationResult(operationResult, result);

			return oid;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL addObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public ObjectType getObject(String oid, PropertyReferenceListType resolve,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(resolve, "Property reference list  must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Get Object");
		try {
			ObjectType object = model.getObject(oid, resolve, operationResult);
			handleOperationResult(operationResult, result);

			return object;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public ObjectListType listObjects(String objectType, PagingType paging, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(objectType, "Object type must not be null or empty.");
		notNullArgument(paging, "Paging  must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service List Objects");
		try {
			ObjectListType list = model.listObjects(ObjectTypes.getObjectTypeFromUri(objectType)
					.getClassDefinition(), paging, operationResult);
			handleOperationResult(operationResult, result);

			return list;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listObjects() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging, Holder<OperationResultType> result)
			throws FaultMessage {
		notNullArgument(query, "Query must not be null.");
		notNullArgument(paging, "Paging  must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Search Objects");
		try {
			ObjectListType list = model.searchObjectsInRepository(query, paging, operationResult);
			handleOperationResult(operationResult, result);

			return list;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL searchObjects() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void modifyObject(ObjectModificationType change, Holder<OperationResultType> result)
			throws FaultMessage {
		notNullArgument(change, "Object modification must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Modify Object");
		try {
			model.modifyObject(change, operationResult);
			handleOperationResult(operationResult, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL modifyObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void deleteObject(String oid, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Delete Object");
		try {
			model.deleteObject(oid, operationResult);
			handleOperationResult(operationResult, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL deleteObject() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(properties, "Property reference list must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Get Property Available Values");
		try {
			PropertyAvailableValuesListType list = model.getPropertyAvailableValues(oid, properties,
					operationResult);
			handleOperationResult(operationResult, result);

			return list;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getPropertyAvailableValues() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(accountOid, "Account oid must not be null or empty.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service List Account Shadow Owner");
		try {
			UserType user = model.listAccountShadowOwner(accountOid, operationResult);
			handleOperationResult(operationResult, result);

			return user;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listAccountShadowOwner() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notEmptyArgument(resourceObjectShadowType, "Resource object shadow type must not be null or empty.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service List Resource Object Shadows");
		try {
			List<ResourceObjectShadowType> list = model.listResourceObjectShadows(resourceOid, ObjectTypes
					.getObjectTypeFromUri(resourceObjectShadowType).getClassDefinition(), operationResult);
			handleOperationResult(operationResult, result);

			ResourceObjectShadowListType shadowList = new ResourceObjectShadowListType();
			shadowList.getObject().addAll(list);

			return shadowList;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listResourceObjectShadows() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public ObjectListType listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullArgument(objectType, "Object type must not be null.");
		notNullArgument(paging, "Paging  must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service List Resource Objects");
		try {
			ObjectListType list = model.listResourceObjects(resourceOid, objectType, paging, operationResult);
			handleOperationResult(operationResult, result);

			return list;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listResourceObjects() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public void testResource(String resourceOid, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Test Resource");
		try {
			model.testResource(resourceOid, operationResult);
			handleOperationResult(operationResult, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL testResource() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public EmptyType launchImportFromResource(String resourceOid, QName objectClass,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullArgument(objectClass, "Object class must not be null.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Launch Import From Resource");
		try {
			model.launchImportFromResource(resourceOid, objectClass, operationResult);
			handleOperationResult(operationResult, result);

			return new EmptyType();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL launchImportFromResource() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	@Override
	public TaskStatusType getImportStatus(String resourceOid, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullResultHolder(result);

		OperationResult operationResult = new OperationResult("Model Service Get Import Status");
		try {
			TaskStatusType task = model.getImportStatus(resourceOid, operationResult);
			handleOperationResult(operationResult, result);

			return task;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getImportStatus() failed", ex);
			throw createSystemFault(ex, operationResult);
		}
	}

	private void handleOperationResult(OperationResult result, Holder<OperationResultType> holder) {
		result.recordSuccess();
		OperationResultType res = result.createOperationResultType();
		holder.value.getPartialResults().add(res);
	}

	private void notNullResultHolder(Holder<OperationResultType> holder) throws FaultMessage {
		notNullArgument(holder, "Holder must not be null.");
		notNullArgument(holder.value, "Result type must not be null.");
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
		result.recordFatalError(ex.getMessage(), ex);

		FaultType faultType;
		if (ex instanceof ObjectNotFoundException) {
			faultType = new ObjectNotFoundFaultType();
		} else if (ex instanceof IllegalArgumentException) {
			faultType = new IllegalArgumentFaultType();
		} else {
			faultType = new SystemFaultType();
		}
		faultType.setMessage(ex.getMessage());
		faultType.setOperationResult(result.createOperationResultType());

		return new FaultMessage(ex.getMessage(), faultType, ex);
	}
}
