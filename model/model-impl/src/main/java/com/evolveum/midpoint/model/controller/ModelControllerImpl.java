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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.importer.ObjectImporter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.ProvisioningTypes;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.xpath.XPathSegment;
import com.evolveum.midpoint.schema.xpath.XPathHolder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
@Component("modelController")
@Scope
public class ModelControllerImpl implements ModelController {

	public static final String CLASS_NAME = ModelControllerImpl.class.getName() + ".";
	public static final String GET_SYSTEM_CONFIGURATION = CLASS_NAME + "getSystemConfiguration";
	public static final String RESOLVE_USER_ATTRIBUTES = CLASS_NAME + "resolveUserAttributes";
	public static final String RESOLVE_ACCOUNT_ATTRIBUTES = CLASS_NAME + "resolveAccountAttributes";
	public static final String CREATE_ACCOUNT = CLASS_NAME + "createAccount";
	public static final String UPDATE_ACCOUNT = CLASS_NAME + "updateAccount";
	public static final String PROCESS_USER_TEMPLATE = CLASS_NAME + "processUserTemplate";
	private static final Trace LOGGER = TraceManager.getTrace(ModelControllerImpl.class);
	@Autowired(required = true)
	private transient ProvisioningService provisioning;
	@Autowired(required = true)
	private transient RepositoryService repository;
	@Autowired(required = true)
	private transient SchemaHandler schemaHandler;

	@Autowired(required = true)
	private transient TaskManager taskManager;

	@Autowired(required = true)
	private transient ImportAccountsFromResourceTaskHandler importAccountsFromResourceTaskHandler;

	@Override
	public String addObject(ObjectType object, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		Validate.notEmpty(object.getName(), "Object name must not be null or empty.");
		LOGGER.debug("Adding object {} with oid {} and name {}.", new Object[] {
				object.getClass().getSimpleName(), object.getOid(), object.getName() });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(object));
		}

		OperationResult subResult = result.createSubresult(ADD_OBJECT);
		addResultParams(subResult, new String[] { "object" }, object);
		String oid = null;
		try {
			if (object instanceof TaskType) {
				oid = addTask((TaskType) object, subResult);
			} else if (ProvisioningTypes.isManagedByProvisioning(object)) {
				oid = addProvisioningObject(object, subResult);
			} else {
				oid = addRepositoryObject(object, subResult);
			}
			subResult.recordSuccess();
		} catch (ObjectAlreadyExistsException ex) {
			subResult.recordFatalError("Object with name '" + object.getName() + "' already exists.", ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			subResult.recordFatalError(ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object", ex, object.getName());
			subResult.recordFatalError(ex);
			if (ex instanceof SystemException) {
				throw (SystemException) ex;
			}
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			subResult.computeStatus("Error occured during add object '" + object.getName() + "'.",
					"Warning occured during add object '" + object.getName() + "'.");
			LOGGER.debug(subResult.dump());
		}

		return oid;
	}

	@Override
	public String addUser(UserType user, UserTemplateType userTemplate, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(user));
			LOGGER.trace(JAXBUtil.silentMarshalWrap(userTemplate));
		}

		if (userTemplate == null) {
			SystemConfigurationType systemConfiguration = getSystemConfiguration(result);
			userTemplate = systemConfiguration.getDefaultUserTemplate();
		}

		if (userTemplate != null) {
			LOGGER.debug("Adding user {}, oid {} using template {}, oid {}.", new Object[] { user.getName(),
					user.getOid(), userTemplate.getName(), userTemplate.getOid() });
		} else {
			LOGGER.debug("Adding user {}, oid {} using no template.",
					new Object[] { user.getName(), user.getOid() });
		}

		OperationResult subResult = result.createSubresult(ADD_USER);
		addResultParams(subResult, new String[] { "user", "userTemplate" }, user, userTemplate);

		String oid = null;
		try {
			processAddAccountFromUser(user, subResult);
			processUserTemplateForUser(user, userTemplate, subResult);
			oid = repository.addObject(user, subResult);
			subResult.recordSuccess();
		} catch (ObjectAlreadyExistsException ex) {
			subResult.recordFatalError("Couldn't add user '" + user.getName() + "', oid '" + user.getOid()
					+ "' because user already exists.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add user {}, oid {} using template {}, oid {}", ex,
					user.getName(), user.getOid(), userTemplate.getName(), userTemplate.getOid());
			subResult.recordFatalError("Couldn't add user " + user.getName() + ", oid '" + user.getOid()
					+ "' using template " + userTemplate.getName() + ", oid '" + userTemplate.getOid() + "'",
					ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}

		return oid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, PropertyReferenceListType resolve,
			OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		Validate.notNull(clazz, "Class must not be null.");
		LOGGER.debug("Getting object with oid {}.", new Object[] { oid });

		OperationResult subResult = result.createSubresult(GET_OBJECT);
		addResultParams(subResult, new String[] { "oid", "resolve", "class" }, oid, resolve, clazz);
		T object = null;
		try {
			// If class parameter is ObjectType we don't know if real object
			// is handled by provisioning or directly by repo, so we try to
			// get object from repository and then update class parameter to
			// real class. If needed we call provisioning to get object
			ObjectNotFoundException objectNotFound = null;
			if (ObjectType.class.equals(clazz) || !ProvisioningTypes.isClassManagedByProvisioning(clazz)) {
				try {
					object = getObjectFromRepository(oid, resolve, subResult, clazz);
				} catch (ObjectNotFoundException ex) {
					objectNotFound = ex;
				}
			}
			clazz = object == null ? clazz : (Class<T>) object.getClass();

			if (ProvisioningTypes.isClassManagedByProvisioning(clazz)) {
				object = getObjectFromProvisioning(oid, resolve, subResult, clazz);
			} else if (objectNotFound != null) {
				// throw previously caught exception, we don't need to call
				// repository again
				throw objectNotFound;
			}
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			subResult.recordFatalError("Object with oid '" + oid + "' not found.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object {}", ex, oid);
			subResult.recordFatalError("Couldn't get object with oid '" + oid + "'.", ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			subResult.computeStatus("Couldn't get object with oid '" + oid + "'.");
			LOGGER.debug(subResult.dump());
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(object));
		}

		return object;
	}

	@Override
	public <T extends ObjectType> List<T> listObjects(Class<T> objectType, PagingType paging,
			OperationResult result) {
		Validate.notNull(objectType, "Object type must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		if (paging == null) {
			LOGGER.debug("Listing objects of type {} (no paging).", objectType);
		} else {
			LOGGER.debug(
					"Listing objects of type {} from {} to {} ordered {} by {}.",
					new Object[] { objectType, paging.getOffset(), paging.getMaxSize(),
							paging.getOrderDirection(), paging.getOrderBy() });
		}

		OperationResult subResult = result.createSubresult(LIST_OBJECTS);
		addResultParams(subResult, new String[] { "objectType", "paging" }, objectType, paging);
		List<T> list = null;
		try {
			if (ProvisioningTypes.isObjectTypeManagedByProvisioning(objectType)) {
				LOGGER.debug("Listing objects from provisioning.");
				list = provisioning.listObjects(objectType, paging, subResult);
			} else {
				LOGGER.debug("Listing objects from repository.");
				list = repository.listObjects(objectType, paging, subResult);
			}
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
			subResult.recordFatalError("Couldn't list objects.", ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}

		if (list == null) {
			list = new ArrayList<T>();
		}

		return list;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelService#searchObjects(com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType, com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType, com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException {
		throw new NotImplementedException();
	}
	
	@Override
	public <T extends ObjectType> List<T>  searchObjects(Class<T> type, QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException {
		throw new NotImplementedException();
	}

	private <T extends ObjectType> List<T> searchObjects(Class<T> type, QueryType query, PagingType paging, OperationResult result,
			boolean searchInProvisioning) {
		Validate.notNull(query, "Query must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		if (paging == null) {
			LOGGER.debug("Searching objects with null paging (query in TRACE).");
		} else {
			LOGGER.debug("Searching objects from {} to {} ordered {} by {} (query in TRACE).",
					new Object[] { paging.getOffset(), paging.getMaxSize(), paging.getOrderDirection(),
							paging.getOrderBy() });
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(query));
		}

		String operationName = searchInProvisioning ? SEARCH_OBJECTS_IN_PROVISIONING
				: SEARCH_OBJECTS_IN_REPOSITORY;
		OperationResult subResult = result.createSubresult(operationName);
		addResultParams(subResult, new String[] { "query", "paging", "searchInProvisioning" }, query, paging,
				searchInProvisioning);
		List<T> list = null;
		try {
			if (searchInProvisioning) {
				list = provisioning.searchObjects(type, query, paging, subResult);
			} else {
				list = repository.searchObjects(type, query, paging, subResult);
			}
			subResult.recordSuccess();
		} catch (Exception ex) {
			String message;
			if (!searchInProvisioning) {
				message = "Couldn't search objects in repository";
			} else {
				message = "Couldn't search objects in provisioning";
			}
			LoggingUtils.logException(LOGGER, message, ex);
			subResult.recordFatalError(message, ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}

		if (list == null) {
			list = new ArrayList<T>();
		}

		return list;
	}

	@Override
	public <T extends ObjectType> List<T> searchObjectsInProvisioning(Class<T> type, QueryType query, PagingType paging,
			OperationResult result) {
		return searchObjects(type, query, paging, result, true);
	}

	@Override
	public <T extends ObjectType> List<T> searchObjectsInRepository(Class<T> type, QueryType query, PagingType paging, OperationResult result) {
		return searchObjects(type, query, paging, result, false);
	}

	@Override
	public void modifyObject(ObjectModificationType change, OperationResult result)
			throws ObjectNotFoundException {
		modifyObjectWithExclusion(change, null, result);
	}

	@Override
	public void modifyObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result) throws ObjectNotFoundException {
		Validate.notNull(change, "Object modification must not be null.");
		Validate.notEmpty(change.getOid(), "Change oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Modifying object with oid {} with exclusion account oid {} (change in TRACE).",
				new Object[] { change.getOid(), accountOid });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(change));
		}

		if (change.getPropertyModification().isEmpty()) {
			return;
		}

		OperationResult subResult = result.createSubresult(MODIFY_OBJECT_WITH_EXCLUSION);
		addResultParams(subResult, new String[] { "change", "accountOid" }, change, accountOid);

		try {
			ObjectType object = getObjectFromRepository(change.getOid(), new PropertyReferenceListType(),
					subResult, ObjectType.class);
			if (object instanceof TaskType) {
				modifyTaskWithExclusion(change, accountOid, subResult, object);
			} else if (ProvisioningTypes.isManagedByProvisioning(object)) {
				modifyProvisioningObjectWithExclusion(change, accountOid, subResult, object);
			} else {
				modifyRepositoryObjectWithExclusion(change, accountOid, subResult, object);
			}
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			subResult.recordFatalError("Coudln't update object with oid '" + change.getOid()
					+ "', object was not found.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update object with oid {}", ex, change.getOid());
			String message = "Couldn't update object with oid '" + change.getOid() + "'.";
			subResult.recordFatalError(message, ex);
			if (ex instanceof SystemException) {
				throw (SystemException) ex;
			}
			throw new SystemException(message, ex);
		} finally {
			subResult.computeStatus("Couldn't update object with oid '" + change.getOid() + "'.");
			LOGGER.debug(subResult.dump());
		}

		LOGGER.debug(subResult.dump());
	}

	@Override
	public boolean deleteObject(String oid, OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Deleting object with oid {}.", new Object[] { oid });

		OperationResult subResult = result.createSubresult(DELETE_OBJECT);
		addResultParams(subResult, new String[] { "oid" }, oid);

		boolean deleted = false;
		try {
			ObjectType object = getObjectFromRepository(oid, new PropertyReferenceListType(), subResult,
					ObjectType.class);

			if (object instanceof TaskType) {
				taskManager.deleteTask(oid, subResult);
			} else if (ProvisioningTypes.isManagedByProvisioning(object)) {
				ScriptsType scripts = getScripts(object, subResult);
				provisioning.deleteObject(oid, scripts, subResult);
			} else {
				if (object instanceof UserType) {
					deleteUserAccounts((UserType) object, subResult);
				}

				repository.deleteObject(oid, subResult);
			}
			deleted = true;
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			subResult.recordFatalError("Couldn't find object with oid '" + oid + "'.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			subResult.recordFatalError("Couldn't delete object with oid '" + oid + "'.", ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}

		return deleted;
	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(properties, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Getting property available values for object with oid {} (properties in TRACE).",
				new Object[] { oid });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(DebugUtil.prettyPrint(properties));
		}

		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Listing account shadow owner for account with oid {}.", new Object[] { accountOid });

		OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW_OWNER);
		addResultParams(subResult, new String[] { "accountOid" }, accountOid);

		UserType user = null;
		try {
			user = repository.listAccountShadowOwner(accountOid, subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Account with oid {} doesn't exists", ex, accountOid);
			subResult.recordFatalError("Account with oid '" + accountOid + "' doesn't exists", ex);

			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, accountOid);
			subResult.recordFatalError("Couldn't list account shadow owner for account with oid '"
					+ accountOid + "'.", ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}

		return user;
	}

	@Override
	public <T extends ResourceObjectShadowType> List<T> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Listing resource object shadows \"{}\" for resource with oid {}.", new Object[] {
				resourceObjectShadowType, resourceOid });

		OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
		addResultParams(subResult, new String[] { "resourceOid", "resourceObjectShadowType" }, resourceOid,
				resourceObjectShadowType);

		List<T> list = null;
		try {
			list = repository.listResourceObjectShadows(resourceOid, resourceObjectShadowType, subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			subResult.recordFatalError("Resource with oid '" + resourceOid + "' was not found.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource object shadows type "
					+ "{} from repository for resource, oid {}", ex, resourceObjectShadowType, resourceOid);
			subResult.recordFatalError(
					"Couldn't list resource object shadows type '" + resourceObjectShadowType
							+ "' from repository for resource, oid '" + resourceOid + "'.", ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}

		if (list == null) {
			list = new ArrayList<T>();
		}

		return list;
	}

	@Override
	public ObjectListType listResourceObjects(String resourceOid, QName objectClass, PagingType paging,
			OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object type must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		LOGGER.debug(
				"Listing resource objects {} from resource, oid {}, from {} to {} ordered {} by {}.",
				new Object[] { objectClass, resourceOid, paging.getOffset(), paging.getMaxSize(),
						paging.getOrderDirection(), paging.getOrderDirection() });

		OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECTS);
		addResultParams(subResult, new String[] { "resourceOid", "objectType", "paging" }, resourceOid,
				objectClass, paging);

		ObjectListType list = null;
		list = provisioning.listResourceObjects(resourceOid, objectClass, paging, subResult);
		subResult.recordSuccess();

		if (list == null) {
			list = new ObjectListType();
			list.setCount(0);
		}

		return list;
	}

	// This returns OperationResult instead of taking it as in/out argument.
	// This is different
	// from the other methods. The testResource method is not using
	// OperationResult to track its own
	// execution but rather to track the execution of resource tests (that in
	// fact happen in provisioning).
	@Override
	public OperationResult testResource(String resourceOid) throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		LOGGER.debug("Testing resource with oid {}.", new Object[] { resourceOid });

		OperationResult testResult = provisioning.testResource(resourceOid);

		if (testResult != null) {
			LOGGER.debug(testResult.dump());
		} else {
			LOGGER.debug("Operation sub result was null (Error occured).");
		}
		return testResult;
	}

	// Note: The result is in the task. No need to pass it explicitly
	@Override
	public void importAccountsFromResource(String resourceOid, QName objectClass, Task task)
			throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(task, "Task must not be null.");
		LOGGER.debug("Launching import from resource with oid {} for object class {}.", new Object[] {
				resourceOid, objectClass });

		OperationResult result = task.getResult().createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE);
		addResultParams(result, new String[] { "resourceOid", "objectClass", "task" }, resourceOid,
				objectClass, task);
		// TODO: add params and context to the result

		// Fetch resource definition from the repo/provisioning
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ResourceType resource = getObject(ResourceType.class, resourceOid, resolve, result);

		importAccountsFromResourceTaskHandler.launch(resource, objectClass, task, result);

		// The launch should switch task to asynchronous. It is in/out, so no
		// other action is needed
	}

	@Override
	public void importObjectsFromFile(File input, Task task, OperationResult parentResult) {
		// OperationResult result =
		// parentResult.createSubresult(IMPORT_OBJECTS_FROM_FILE);
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void importObjectsFromStream(InputStream input, Task task, Boolean overwrite,
			OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
		// TODO: set summarization
		ObjectImporter.importObjects(input, task, result, repository);
		result.computeStatus("Couldn't import object from input stream.");
	}

	private <T extends ObjectType> T getObjectFromRepository(String oid, PropertyReferenceListType resolve,
			OperationResult result, Class<T> clazz) throws ObjectNotFoundException {
		return getObject(clazz, oid, resolve, result, false);
	}

	private <T extends ObjectType> T getObjectFromProvisioning(String oid, PropertyReferenceListType resolve,
			OperationResult result, Class<T> clazz) throws ObjectNotFoundException {
		return getObject(clazz, oid, resolve, result, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, PropertyReferenceListType resolve,
			OperationResult result, boolean fromProvisioning) throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");
		T object = null;

		OperationResult subResult = result.createSubresult(GET_OBJECT);
		addResultParams(subResult, new String[] { "oid", "resolve", "class", "fromProvisioning" }, oid,
				resolve, clazz, fromProvisioning);
		try {
			ObjectType objectType = null;
			if (fromProvisioning) {
				objectType = provisioning.getObject(oid, resolve, subResult);
			} else {
				objectType = repository.getObject(clazz, oid, resolve, subResult);
			}
			if (!clazz.isInstance(objectType)) {
				throw new ObjectNotFoundException("Bad object type returned for referenced oid '" + oid
						+ "'. Expected '" + clazz + "', but was '"
						+ (objectType == null ? "null" : objectType.getClass()) + "'.");
			} else {
				object = (T) objectType;
			}

			resolveObjectAttributes(object, resolve, subResult);
		} catch (SystemException ex) {
			throw ex;
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object with oid {}, expected type was {}.", ex,
					oid, clazz);
			throw new SystemException("Couldn't get object with oid '" + oid + "'.", ex);
		} finally {
			subResult.computeStatus("Couldn't get object with oid '" + oid + "'.");
			LOGGER.debug(subResult.dump());
		}

		return object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.model.api.ModelService#discoverConnectors(com.evolveum
	 * .midpoint.xml.ns._public.common.common_1.ConnectorHostType,
	 * com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(ModelService.class.getName()
				+ ".discoverConnectors");
		Set<ConnectorType> discoverConnectors = provisioning.discoverConnectors(hostType, result);
		result.computeStatus("Connector discovery failed");
		return discoverConnectors;
	}

	private String addProvisioningObject(ObjectType object, OperationResult result)
			throws ObjectNotFoundException, ObjectAlreadyExistsException {
		if (object instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) object;
			processAccountCredentials(account, result);

			ModelUtils.unresolveResourceObjectShadow(account);
		}

		try {
			ScriptsType scripts = getScripts(object, result);
			return provisioning.addObject(object, scripts, result);
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	private String addTask(TaskType task, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException {
		try {
			return taskManager.addTask(task, result);
		} catch (ObjectAlreadyExistsException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	private SystemConfigurationType getSystemConfiguration(OperationResult result)
			throws ObjectNotFoundException {
		OperationResult configResult = result.createSubresult(GET_SYSTEM_CONFIGURATION);
		SystemConfigurationType systemConfiguration = null;
		try {
			systemConfiguration = getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
					ModelUtils.createPropertyReferenceListType("defaultUserTemplate"), result,
					false);
			configResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			configResult.recordFatalError("Couldn't get system configuration.", ex);
			throw ex;
		}

		return systemConfiguration;
	}

	private String addRepositoryObject(ObjectType object, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException {
		if (object instanceof UserType) {
			UserType user = (UserType) object;
			processAddAccountFromUser(user, result);
			// At first we get default user template from system configuration
			SystemConfigurationType systemConfiguration = getSystemConfiguration(result);
			UserTemplateType userTemplate = systemConfiguration.getDefaultUserTemplate();
			processUserTemplateForUser(user, userTemplate, result);
		}

		try {
			return repository.addObject(object, result);
		} catch (ObjectAlreadyExistsException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to repository", ex, object.getName());
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	private void resolveObjectAttributes(ObjectType object, PropertyReferenceListType resolve,
			OperationResult result) {
		if (object == null) {
			return;
		}

		if (object instanceof UserType) {
			resolveUserAttributes((UserType) object, resolve, result);
		} else if (object instanceof AccountShadowType) {
			resolveAccountAttributes((AccountShadowType) object, resolve, result);
		}
	}

	private void resolveUserAttributes(UserType user, PropertyReferenceListType resolve,
			OperationResult result) {
		if (!Utils.haveToResolve("Account", resolve)) {
			return;
		}

		List<ObjectReferenceType> refToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			OperationResult subResult = result.createSubresult(RESOLVE_USER_ATTRIBUTES);
			addResultParams(subResult, new String[] { "user", "accountRef" }, user, accountRef);
			try {
				AccountShadowType account = getObjectFromProvisioning(accountRef.getOid(), resolve,
						subResult, AccountShadowType.class);
				user.getAccount().add(account);
				refToBeDeleted.add(accountRef);
				subResult.recordSuccess();
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't resolve account with oid {}", ex,
						accountRef.getOid());
				subResult.recordFatalError(
						"Couldn't resolve account with oid '" + accountRef.getOid() + "'.", ex);
			} finally {
				subResult.computeStatus("Couldn't resolve account with oid '" + accountRef.getOid() + "'.");
			}
		}
		user.getAccountRef().removeAll(refToBeDeleted);
	}

	private void resolveAccountAttributes(AccountShadowType account, PropertyReferenceListType resolve,
			OperationResult result) {
		if (!Utils.haveToResolve("Resource", resolve)) {
			return;
		}

		ObjectReferenceType reference = account.getResourceRef();
		if (reference == null || StringUtils.isEmpty(reference.getOid())) {
			LOGGER.debug("Skipping resolving resource for account {}, resource reference is null or "
					+ "doesn't contain oid.", new Object[] { account.getName() });
			return;
		}
		OperationResult subResult = result.createSubresult(RESOLVE_ACCOUNT_ATTRIBUTES);
		addResultParams(subResult, new String[] { "account", "resolve" }, account, resolve);
		try {
			ResourceType resource = getObjectFromProvisioning(account.getResourceRef().getOid(), resolve,
					result, ResourceType.class);
			account.setResource(resource);
			account.setResourceRef(null);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils
					.logException(LOGGER, "Couldn't resolve resource with oid {}", ex, reference.getOid());
			subResult
					.recordFatalError("Couldn't resolve resource with oid '" + reference.getOid() + "'.", ex);
		} finally {
			subResult.computeStatus("Couldn't resolve resource with oid '" + reference.getOid() + "'.");
		}
	}

	private ScriptsType getScripts(ObjectType object, OperationResult result) throws ObjectNotFoundException {
		ScriptsType scripts = null;
		if (object instanceof ResourceType) {
			ResourceType resource = (ResourceType) object;
			scripts = resource.getScripts();
		} else if (object instanceof ResourceObjectShadowType) {
			ResourceObjectShadowType resourceObject = (ResourceObjectShadowType) object;
			if (resourceObject.getResource() != null) {
				scripts = resourceObject.getResource().getScripts();
			} else {
				ObjectReferenceType reference = resourceObject.getResourceRef();
				ResourceType resObject = getObjectFromProvisioning(reference.getOid(),
						new PropertyReferenceListType(), result, ResourceType.class);
				scripts = resObject.getScripts();
			}
		}

		if (scripts == null) {
			scripts = new ScriptsType();
		}

		return scripts;
	}

	private void deleteUserAccounts(UserType user, OperationResult result) throws ObjectNotFoundException {
		List<AccountShadowType> accountsToBeDeleted = new ArrayList<AccountShadowType>();
		for (AccountShadowType account : user.getAccount()) {
			if (deleteObject(account.getOid(), result)) {
				accountsToBeDeleted.add(account);
			}
		}

		user.getAccount().removeAll(accountsToBeDeleted);

		List<ObjectReferenceType> refsToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			if (deleteObject(accountRef.getOid(), result)) {
				refsToBeDeleted.add(accountRef);
			}
		}
		user.getAccountRef().removeAll(refsToBeDeleted);

		// If list is empty then skip processing user have no accounts.
		if (accountsToBeDeleted.isEmpty() && refsToBeDeleted.isEmpty()) {
			return;
		}

		// TODO: save updated user, create property changes
		ObjectModificationType change = createUserModification(accountsToBeDeleted, refsToBeDeleted);
		change.setOid(user.getOid());
		try {
			repository.modifyObject(change, result);
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update user {} after accounts was deleted", ex,
					user.getName());
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	private ObjectModificationType createUserModification(List<AccountShadowType> accountsToBeDeleted,
			List<ObjectReferenceType> refsToBeDeleted) {
		ObjectModificationType change = new ObjectModificationType();
		for (ObjectReferenceType reference : refsToBeDeleted) {
			PropertyModificationType propertyChangeType = ObjectTypeUtil.createPropertyModificationType(
					PropertyModificationTypeType.delete, null, new QName(SchemaConstants.NS_C, "accountRef"),
					reference);

			change.getPropertyModification().add(propertyChangeType);
		}

		for (AccountShadowType account : accountsToBeDeleted) {
			PropertyModificationType propertyChangeType = ObjectTypeUtil.createPropertyModificationType(
					PropertyModificationTypeType.delete, null, new QName(SchemaConstants.NS_C, "account"),
					account.getOid());

			change.getPropertyModification().add(propertyChangeType);
		}

		return change;
	}

	private void processAccountCredentials(AccountShadowType account, OperationResult result)
			throws ObjectNotFoundException {
		// inserting credentials to account if needed (with generated password)
		ResourceType resource = account.getResource();
		if (resource == null) {
			resource = getObjectFromProvisioning(account.getResourceRef().getOid(),
					new PropertyReferenceListType(), result, ResourceType.class);
		}

		if (resource == null || resource.getSchemaHandling() == null) {
			return;
		}

		AccountType accountType = ModelUtils.getAccountTypeFromHandling(account, resource);
		if (accountType == null || accountType.getCredentials() == null) {
			return;
		}

		AccountType.Credentials credentials = accountType.getCredentials();
		int randomPasswordLength = -1;
		if (credentials.getRandomPasswordLength() != null) {
			randomPasswordLength = credentials.getRandomPasswordLength().intValue();
		}

		if (randomPasswordLength != -1 && ModelUtils.getPassword(account).getAny() == null) {
			ModelUtils.generatePassword(account, randomPasswordLength);
		}
	}

	@SuppressWarnings("unchecked")
	private void modifyProvisioningObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result, ObjectType object) throws ObjectNotFoundException, SchemaException,
			CommunicationException {
		if (object instanceof ResourceObjectShadowType) {
			object = getObject(ResourceObjectShadowType.class, object.getOid(), new PropertyReferenceListType(), result,
					true);

			UserType user = null;
			try {
				user = listAccountShadowOwner(object.getOid(), result);
				LOGGER.debug("Found owner {} for account shadow {}", ObjectTypeUtil.toShortString(user),
						ObjectTypeUtil.toShortString(object));
			} catch (ObjectNotFoundException ex) {
				// we didn't find shadow owner, in next step we skip outbound
				// schema handling processing.
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner for {}", ex,
						object.getName());
			}

			try {
				PatchXml patchXml = new PatchXml();
				String xmlPatchedObject = patchXml.applyDifferences(change, object);
				object = ((JAXBElement<ResourceObjectShadowType>) JAXBUtil.unmarshal(xmlPatchedObject))
						.getValue();

				ObjectModificationType newChange = processOutboundSchemaHandling(user,
						(ResourceObjectShadowType) object, result);
				if (newChange != null) {

					newChange.getPropertyModification().addAll(
							updateChange(change.getPropertyModification(),
									newChange.getPropertyModification()));
					change = newChange;
				}

			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't process outbound handling for object {}", ex,
						object.getName());
			}
		}

		ScriptsType scripts = getScripts(object, result);
		if (StringUtils.isNotEmpty(change.getOid())) {
			provisioning.modifyObject(change, scripts, result);
		}
	}

	private List<PropertyModificationType> updateChange(List<PropertyModificationType> oldChange,
			List<PropertyModificationType> newChange) {
		List<PropertyModificationType> updatedChange = new ArrayList<PropertyModificationType>();
		for (PropertyModificationType changeModification : oldChange) {
			if (!existPropertyModification(newChange, changeModification)) {
				updatedChange.add(changeModification);
			}
		}
		return updatedChange;
	}

	private boolean existPropertyModification(List<PropertyModificationType> newChange,
			PropertyModificationType changeModification) {
		for (PropertyModificationType newChangeModification : newChange) {
			if (existPropertyModification(newChangeModification.getValue().getAny(), changeModification
					.getValue().getAny())) {
				return true;
			}
		}
		return false;
	}

	private boolean existPropertyModification(List<Element> newChangeValues, List<Element> oldChangeValues) {
		for (Element newValue : newChangeValues) {
			for (Element oldValue : oldChangeValues) {
				if (newValue.getLocalName().equals(oldValue.getLocalName())
						&& newValue.getNamespaceURI().equals(oldValue.getNamespaceURI())) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void processAddAccountFromChanges(ObjectModificationType change, UserType userBeforeChange,
			OperationResult result) {
		// MID-73 password push from user to account
		// TODO: look for password property modification and next use it while
		// creating account. If account schemahandling credentials
		// outboundPassword is true, we have to push password

		for (PropertyModificationType propertyChange : change.getPropertyModification()) {
			if (!PropertyModificationTypeType.add.equals(propertyChange.getModificationType())
					|| propertyChange.getValue() == null || propertyChange.getValue().getAny().isEmpty()) {
				continue;
			}

			Node node = propertyChange.getValue().getAny().get(0);
			if ("account".equals(node.getLocalName()) && SchemaConstants.NS_C.equals(node.getNamespaceURI())) {
				try {
					AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(DOMUtil
							.serializeDOMToString(node))).getValue();

					ObjectModificationType accountChange = processOutboundSchemaHandling(userBeforeChange,
							account, result);
					if (accountChange != null) {
						PatchXml patchXml = new PatchXml();
						String accountXml = patchXml.applyDifferences(accountChange, account);
						account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(accountXml))
								.getValue();
					}

					String newAccountOid = addObject(account, result);
					ObjectReferenceType accountRef = ModelUtils.createReference(newAccountOid,
							ObjectTypes.ACCOUNT);
					Element accountRefElement = JAXBUtil.jaxbToDom(accountRef, SchemaConstants.I_ACCOUNT_REF,
							DOMUtil.getDocument());

					propertyChange.getValue().getAny().clear();
					propertyChange.getValue().getAny().add(accountRefElement);
				} catch (SystemException ex) {
					throw ex;
				} catch (Exception ex) {
					throw new SystemException("Couldn't process add account.", ex);
				}
			}
		}
	}

	private void processAddAccountFromUser(UserType user, OperationResult result) {
		List<AccountShadowType> accountsToDelete = new ArrayList<AccountShadowType>();
		for (AccountShadowType account : user.getAccount()) {
			try {
				if (account.getActivation() == null) {
					account.setActivation(user.getActivation());
				}
				// MID-73
				pushPasswordFromUserToAccount(user, account, result);

				String newAccountOid = addObject(account, result);
				ObjectReferenceType accountRef = ModelUtils.createReference(newAccountOid,
						ObjectTypes.ACCOUNT);
				user.getAccountRef().add(accountRef);
				accountsToDelete.add(account);
			} catch (SystemException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new SystemException("Couldn't process add account.", ex);
			}
		}
		user.getAccount().removeAll(accountsToDelete);
	}

	/**
	 * MID-73 password push from user to account
	 * 
	 * @throws ObjectNotFoundException
	 */
	private void pushPasswordFromUserToAccount(UserType user, AccountShadowType account,
			OperationResult result) throws ObjectNotFoundException {
		ResourceType resource = account.getResource();
		if (resource == null) {
			resource = getObjectFromProvisioning(account.getResourceRef().getOid(),
					new PropertyReferenceListType(), result, ResourceType.class);
		}
		AccountType accountHandling = ModelUtils.getAccountTypeFromHandling(account, resource);
		boolean pushPasswordToAccount = false;
		if (accountHandling.getCredentials() != null
				&& accountHandling.getCredentials().isOutboundPassword() != null) {
			pushPasswordToAccount = accountHandling.getCredentials().isOutboundPassword();
		}
		if (pushPasswordToAccount && user.getCredentials() != null
				&& user.getCredentials().getPassword() != null) {
			CredentialsType credentials = account.getCredentials();
			if (credentials == null) {
				credentials = new CredentialsType();
				account.setCredentials(credentials);
			}
			credentials.setPassword(user.getCredentials().getPassword());
		}
	}

	@SuppressWarnings("unchecked")
	private void modifyRepositoryObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result, ObjectType object) throws ObjectNotFoundException, SchemaException {
		if (object instanceof UserType) {
			UserType user = (UserType) object;
			// processing add account
			processAddAccountFromChanges(change, user, result);

			repository.modifyObject(change, result);
			try {
				PatchXml patchXml = new PatchXml();
				String u = patchXml.applyDifferences(change, user);
				user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(u)).getValue();
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't patch user {}", ex, object.getName());
			}

			PropertyModificationType userActivationChanged = hasUserActivationChanged(change);
			if (userActivationChanged != null) {
				LOGGER.debug("User activation status changed, enabling/disabling accounts in next step.");
			}

			// from now on we have updated user, next step is processing
			// outbound for every account or enable/disable account if needed
			modifyAccountsAfterUserWithExclusion(user, change, userActivationChanged, accountOid, result);
		} else {
			repository.modifyObject(change, result);
		}
	}

	private void modifyAccountsAfterUserWithExclusion(UserType user, ObjectModificationType change,
			PropertyModificationType userActivationChanged, String accountOid, OperationResult result) {
		List<ObjectReferenceType> accountRefs = user.getAccountRef();
		for (ObjectReferenceType accountRef : accountRefs) {
			OperationResult subResult = result.createSubresult(UPDATE_ACCOUNT);
			addResultParams(subResult, new String[] { "change", "accountOid", "object", "accountRef" },
					change, accountOid, user, accountRef);
			if (StringUtils.isNotEmpty(accountOid) && accountOid.equals(accountRef.getOid())) {
				subResult.computeStatus("Account excluded during modification, skipped.");
				// preventing cycles while updating resource object shadows
				continue;
			}

			try {
				AccountShadowType account = getObject(AccountShadowType.class, accountRef.getOid(),
						ModelUtils.createPropertyReferenceListType("Resource"), subResult,
						true);

				ObjectModificationType accountChange = null;
				try {
					schemaHandler.setModel(this);
					accountChange = schemaHandler.processOutboundHandling(user, account, subResult);
				} catch (SchemaHandlerException ex) {
					LoggingUtils.logException(LOGGER, "Couldn't update outbound handling for account {}", ex,
							accountRef.getOid());
					subResult.recordFatalError(ex);
				}

				if (accountChange == null) {
					accountChange = new ObjectModificationType();
					accountChange.setOid(account.getOid());
				}

				if (userActivationChanged != null) {
					PropertyModificationType modification = new PropertyModificationType();
					modification.setModificationType(PropertyModificationTypeType.replace);
					modification.setPath(userActivationChanged.getPath());
					modification.setValue(userActivationChanged.getValue());
					accountChange.getPropertyModification().add(modification);
				}

				modifyObjectWithExclusion(accountChange, accountOid, subResult);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't update account {}", ex, accountRef.getOid());
				subResult.recordFatalError(ex);
			} finally {
				subResult.computeStatus("Couldn't update account '" + accountRef.getOid() + "'.");
			}
		}
	}

	private PropertyModificationType hasUserActivationChanged(ObjectModificationType change) {
		for (PropertyModificationType modification : change.getPropertyModification()) {
			XPathHolder xpath = new XPathHolder(modification.getPath());
			List<XPathSegment> segments = xpath.toSegments();
			if (segments == null || segments.isEmpty() || segments.size() > 1) {
				continue;
			}

			if (SchemaConstants.ACTIVATION.equals(segments.get(0).getQName())) {
				return modification;
			}
		}

		return null;
	}

	private void modifyTaskWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result, ObjectType object) throws ObjectNotFoundException, SchemaException {
		taskManager.modifyTask(change, result);
	}

	private ObjectModificationType processOutboundSchemaHandling(UserType user,
			ResourceObjectShadowType object, OperationResult result) {
		ObjectModificationType change = null;
		if (user != null) {
			try {
				schemaHandler.setModel(this);
				change = schemaHandler.processOutboundHandling(user, (ResourceObjectShadowType) object,
						result);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't process outbound schema handling for {}", ex,
						object.getName());
			}
		} else {
			LOGGER.debug("Skipping outbound schema handling processing for {} (no user defined).",
					new Object[] { object.getName() });
		}

		return change;
	}

	private void processUserTemplateForUser(UserType user, UserTemplateType userTemplate,
			OperationResult result) {
		OperationResult subResult = result.createSubresult(PROCESS_USER_TEMPLATE);
		addResultParams(subResult, new String[] { "user", "userTemplate" }, user, userTemplate);
		if (userTemplate == null) {
			subResult.recordWarning("No user template defined, skipping.");
			return;
		}

		processUserTemplateProperty(user, userTemplate, subResult);
		processUserTemplateAccount(user, userTemplate, subResult);
		subResult.computeStatus("Couldn't finish process user template.");
	}

	@SuppressWarnings("unchecked")
	private void processUserTemplateAccount(UserType user, UserTemplateType userTemplate,
			OperationResult result) {
		for (AccountConstructionType construction : userTemplate.getAccountConstruction()) {
			OperationResult subResult = result.createSubresult(CREATE_ACCOUNT);
			addResultParams(subResult, new String[] { "user", "userTemplate" }, user, userTemplate);
			try {
				ObjectReferenceType resourceRef = construction.getResourceRef();
				ResourceType resource = getObject(ResourceType.class, resourceRef.getOid(), new PropertyReferenceListType(),
						subResult, true);

				AccountType accountType = ModelUtils.getAccountTypeFromHandling(construction.getType(),
						resource);

				AccountShadowType account = new AccountShadowType();
				account.setAttributes(new ResourceObjectShadowType.Attributes());
				account.setObjectClass(accountType.getObjectClass());
				account.setName(resource.getName() + "-" + user.getName());
				account.setResourceRef(resourceRef);
				account.setActivation(user.getActivation());

				ObjectModificationType changes = processOutboundSchemaHandling(user, account, subResult);
				if (changes != null) {
					PatchXml patchXml = new PatchXml();
					String accountXml = patchXml.applyDifferences(changes, account);
					account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(accountXml)).getValue();
				}

				String accountOid = addObject(account, subResult);
				user.getAccountRef().add(ModelUtils.createReference(accountOid, ObjectTypes.ACCOUNT));
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't process account construction {} for user {}", ex,
						construction.getType(), user.getName());
				subResult.recordFatalError("Something went terribly wrong.", ex);
				result.recordWarning("Couldn't process account construction '" + construction.getType()
						+ "'.", ex);
			} finally {
				subResult.computeStatus("Couldn't process account construction '" + construction.getType()
						+ "'.");
			}
		}
	}

	private void processUserTemplateProperty(UserType user, UserTemplateType userTemplate,
			OperationResult result) {
		for (PropertyConstructionType construction : userTemplate.getPropertyConstruction()) {
			OperationResult subResult = result.createSubresult("User Property Construction");
			addResultParams(subResult, new String[] { "user", "userTemplate" }, user, userTemplate);
			try {
				// TODO: process user template property construction

			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't process property construction {} for user {}",
						ex, construction.getProperty().getTextContent(), user.getName());
				subResult.recordWarning("Couldn't process property construction '"
						+ construction.getProperty().getTextContent() + "'.", ex);
			} finally {
				subResult.computeStatus("Couldn't process property construction '"
						+ construction.getProperty().getTextContent() + "'.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.model.api.ModelService#initialize(com.evolveum.
	 * midpoint.common.result.OperationResult)
	 */
	@Override
	public void postInit(OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(POST_INIT);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ModelControllerImpl.class);

		// TODO: initialize repository
		// TODO: initialize task manager

		// Initialize provisioning
		provisioning.postInit(result);

		result.computeStatus("Error occured during post initialization process.");
	}

	private void addResultParams(OperationResult result, String[] names, Object... objects) {
		if (names.length != objects.length) {
			throw new IllegalArgumentException("Bad result parameters size, names '" + names.length
					+ "', objects '" + objects.length + "'.");
		}

		for (int i = 0; i < names.length; i++) {
			result.addParam(names[i], objects[i]);
		}
	}

}
