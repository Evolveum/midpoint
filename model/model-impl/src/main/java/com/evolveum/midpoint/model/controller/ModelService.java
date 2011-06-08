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
package com.evolveum.midpoint.model.controller;

import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
// @Service
public class ModelService implements ModelPortType {

	private static final Trace LOGGER = TraceManager.getTrace(ModelService.class);
	@Autowired(required = true)
	private ModelController model;

	@Override
	public String addObject(ObjectType object, Holder<OperationResultType> result) throws FaultMessage {
		notNullArgument(object, "Object must not be null.");
		notNullResultHolder(result);

		try {
			return model.addObject(object, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL addObject() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public ObjectType getObject(String oid, PropertyReferenceListType resolve,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(resolve, "Property reference list  must not be null.");
		notNullResultHolder(result);

		try {
			return model.getObject(oid, resolve, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getObject() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public ObjectListType listObjects(String objectType, PagingType paging, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(objectType, "Object type must not be null or empty.");
		notNullArgument(paging, "Paging  must not be null.");
		notNullResultHolder(result);

		try {
			return model.listObjects(objectType, paging, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listObjects() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging, Holder<OperationResultType> result)
			throws FaultMessage {
		notNullArgument(query, "Query must not be null.");
		notNullArgument(paging, "Paging  must not be null.");
		notNullResultHolder(result);

		try {
			return model.searchObjects(query, paging, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL searchObjects() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public void modifyObject(ObjectModificationType change, Holder<OperationResultType> result)
			throws FaultMessage {
		notNullArgument(change, "Object modification must not be null.");
		notNullResultHolder(result);

		try {
			model.modifyObject(change, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL modifyObject() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public void deleteObject(String oid, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullResultHolder(result);

		try {
			model.deleteObject(oid, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL deleteObject() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(oid, "Oid must not be null or empty.");
		notNullArgument(properties, "Property reference list must not be null.");
		notNullResultHolder(result);

		try {
			return model.getPropertyAvailableValues(oid, properties,
					OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getPropertyAvailableValues() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(accountOid, "Account oid must not be null or empty.");
		notNullResultHolder(result);

		try {
			return model.listAccountShadowOwner(accountOid,
					OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listAccountShadowOwner() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType, Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notEmptyArgument(resourceObjectShadowType, "Resource object shadow type must not be null or empty.");
		notNullResultHolder(result);

		try {
			return model.listResourceObjectShadows(resourceOid, resourceObjectShadowType,
					OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listResourceObjectShadows() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public ObjectListType listResourceObjects(String resourceOid, String objectType, PagingType paging,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notEmptyArgument(objectType, "Object type must not be null or empty.");
		notNullArgument(paging, "Paging  must not be null.");
		notNullResultHolder(result);

		try {
			return model.listResourceObjects(resourceOid, objectType, paging,
					OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL listResourceObjects() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public ResourceTestResultType testResource(String resourceOid, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullResultHolder(result);

		try {
			return model.testResource(resourceOid, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL testResource() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public EmptyType launchImportFromResource(String resourceOid, String objectClass,
			Holder<OperationResultType> result) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notEmptyArgument(objectClass, "Object class must not be null or empty.");
		notNullResultHolder(result);

		try {
			return model.launchImportFromResource(resourceOid, objectClass,
					OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL launchImportFromResource() failed.", ex);
			throw createSystemFault(ex);
		}
	}

	@Override
	public TaskStatusType getImportStatus(String resourceOid, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullResultHolder(result);

		try {
			return model.getImportStatus(resourceOid, OperationResult.createOperationResult(result.value));
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getImportStatus() failed.", ex);
			throw createSystemFault(ex);
		}
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

	private FaultMessage createSystemFault(Exception ex) {
		FaultType faultType = new SystemFaultType();
		faultType.setMessage(ex.getMessage());

		return new FaultMessage(ex.getMessage(), faultType, ex);
	}
}
