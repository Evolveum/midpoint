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

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.handler.BasicHandler;
import com.evolveum.midpoint.model.controller.handler.UserTypeHandler;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.importer.ObjectImporter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ConsistencyViolationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

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
	private transient Protector protector;
	@Autowired(required = true)
	private transient TaskManager taskManager;

	@Autowired(required = true)
	private transient ImportAccountsFromResourceTaskHandler importAccountsFromResourceTaskHandler;
	@Autowired(required = true)
	private transient ObjectImporter objectImporter;

	@Override
	public String addObject(ObjectType object, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		if (!(object instanceof ResourceObjectShadowType)) {
			Validate.notEmpty(object.getName(), "Object name must not be null or empty.");
		}
		LOGGER.debug("Adding object {} with oid {} and name {}.", new Object[] {
				object.getClass().getSimpleName(), object.getOid(), object.getName() });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(JAXBUtil.silentMarshalWrap(object));
		}

		OperationResult subResult = result.createSubresult(ADD_OBJECT);
		subResult.addParams(new String[] { "object" }, object);
		String oid = null;
		try {
			if (object instanceof TaskType) {
				oid = addTask((TaskType) object, subResult);
			} else if (ObjectTypes.isManagedByProvisioning(object)) {
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

		OperationResult subResult = result.createSubresult(ADD_USER);
		UserTypeHandler handler = new UserTypeHandler(this, provisioning, repository, schemaHandler);
		return handler.addUser(user, userTemplate, subResult);
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
					"Listing objects of type {} offset {} count {} ordered {} by {}.",
					new Object[] { objectType, paging.getOffset(), paging.getMaxSize(),
							paging.getOrderDirection(), paging.getOrderBy() });
		}

		OperationResult subResult = result.createSubresult(LIST_OBJECTS);
		subResult.addParams(new String[] { "objectType", "paging" }, objectType, paging);
		List<T> list = null;
		try {
			if (ObjectTypes.isObjectTypeManagedByProvisioning(objectType)) {
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

	@Override
	public <T extends ObjectType> List<T> searchObjects(Class<T> type, QueryType query, PagingType paging,
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		Validate.notNull(type, "Object type must not be null.");
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

		boolean searchInProvisioning = ObjectTypes.isClassManagedByProvisioning(type);
		String operationName = searchInProvisioning ? SEARCH_OBJECTS_IN_PROVISIONING
				: SEARCH_OBJECTS_IN_REPOSITORY;
		OperationResult subResult = result.createSubresult(operationName);
		subResult.addParams(new String[] { "query", "paging", "searchInProvisioning" }, query, paging,
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
	public <T extends ObjectType> void modifyObject(Class<T> type, ObjectModificationType change,
			OperationResult result) throws ObjectNotFoundException {
		modifyObjectWithExclusion(type, change, null, result);
	}

	@Override
	public <T extends ObjectType> void modifyObjectWithExclusion(Class<T> type,
			ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException {
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
		subResult.addParams(new String[] { "change", "accountOid" }, change, accountOid);
		try {
			if (type.isAssignableFrom(TaskType.class)) {
				modifyTaskWithExclusion(change, accountOid, subResult);
			} else if (ObjectTypes.isClassManagedByProvisioning(type)) {
				modifyProvisioningObjectWithExclusion(type, change, accountOid, subResult);
			} else {
				modifyRepositoryObjectWithExclusion(type, change, accountOid, subResult);
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
	public <T extends ObjectType> void deleteObject(Class<T> clazz, String oid, OperationResult result)
			throws ObjectNotFoundException, ConsistencyViolationException {
		Validate.notNull(clazz, "Class must not be null.");
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Deleting object with oid {}.", new Object[] { oid });

		OperationResult subResult = result.createSubresult(DELETE_OBJECT);
		subResult.addParams(new String[] { "oid" }, oid);
		if (UserType.class.equals(clazz)) {
			UserTypeHandler handler = new UserTypeHandler(this, provisioning, repository, schemaHandler);
			handler.deleteObject(clazz, oid, subResult);
			return;
		}

		try {
			ObjectType object = getObject(ObjectType.class, oid, new PropertyReferenceListType(), subResult);

			if (object instanceof TaskType) {
				taskManager.deleteTask(oid, subResult);
			} else if (ObjectTypes.isManagedByProvisioning(object)) {
				ScriptsType scripts = getScripts(object, subResult);
				provisioning.deleteObject(clazz, oid, scripts, subResult);
			} else {
				repository.deleteObject(clazz, oid, subResult);
			}
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			subResult.recordFatalError("Couldn't find object with oid '" + oid + "'.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER,
					"Couldn't delete object with oid {}, potential consistency violation", ex, oid);
			subResult.recordFatalError("Couldn't delete object with oid '" + oid
					+ "', potential consistency violation");
			throw new ConsistencyViolationException("Couldn't delete object with oid '" + oid
					+ "', potential consistency violation", ex);
		} finally {
			LOGGER.debug(subResult.dump());
		}
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
		subResult.addParams(new String[] { "accountOid" }, accountOid);

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
		subResult.addParams(new String[] { "resourceOid", "resourceObjectShadowType" }, resourceOid,
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
		subResult.addParams(new String[] { "resourceOid", "objectType", "paging" }, resourceOid, objectClass,
				paging);

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

		OperationResult testResult = null;
		try {
			testResult = provisioning.testResource(resourceOid);
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (SystemException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SystemException(ex.getMessage(), ex);
		}

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
		result.addParams(new String[] { "resourceOid", "objectClass", "task" }, resourceOid, objectClass,
				task);
		// TODO: add context to the result

		// Fetch resource definition from the repo/provisioning
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ResourceType resource = getObject(ResourceType.class, resourceOid, resolve, result);

		importAccountsFromResourceTaskHandler.launch(resource, objectClass, task, result);

		// The launch should switch task to asynchronous. It is in/out, so no
		// other action is needed
	}

	@Override
	public void importObjectsFromFile(File input, ImportOptionsType options, Task task,
			OperationResult parentResult) {
		// OperationResult result =
		// parentResult.createSubresult(IMPORT_OBJECTS_FROM_FILE);
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void importObjectsFromStream(InputStream input, ImportOptionsType options, Task task,
			OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
		objectImporter.importObjects(input, options, task, result, repository);
		result.computeStatus("Couldn't import object from input stream.");
	}

	@Override
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, PropertyReferenceListType resolve,
			OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");

		OperationResult subResult = result.createSubresult(GET_OBJECT);
		subResult.addParams(new String[] { "oid", "resolve", "class" }, oid, resolve, clazz);

		BasicHandler handler = new BasicHandler(this, provisioning, repository, schemaHandler);
		return handler.getObject(clazz, oid, resolve, subResult);
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
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
			throws CommunicationException {
		OperationResult result = parentResult.createSubresult(DISCOVER_CONNECTORS);
		Set<ConnectorType> discoverConnectors;
		try {
			discoverConnectors = provisioning.discoverConnectors(hostType, result);
		} catch (CommunicationException e) {
			result.recordFatalError(e.getMessage(), e);
			throw e;
		}
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

	private String addRepositoryObject(ObjectType object, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException {
		if (object instanceof UserType) {
			UserTypeHandler handler = new UserTypeHandler(this, provisioning, repository, schemaHandler);
			return handler.addObject(object, result);
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
				ResourceType resObject = getObject(ResourceType.class, reference.getOid(),
						new PropertyReferenceListType(), result);
				scripts = resObject.getScripts();
			}
		}

		if (scripts == null) {
			scripts = new ScriptsType();
		}

		return scripts;
	}

	private void processAccountCredentials(AccountShadowType account, OperationResult result)
			throws ObjectNotFoundException {
		// inserting credentials to account if needed (with generated password)
		ResourceType resource = account.getResource();
		if (resource == null) {
			resource = getObject(ResourceType.class, account.getResourceRef().getOid(),
					new PropertyReferenceListType(), result);
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

		if (randomPasswordLength != -1 && ModelUtils.getPassword(account).getProtectedString() == null) {
			try {
				ModelUtils.generatePassword(account, randomPasswordLength, protector);
			} catch (Exception ex) {
				throw new SystemException(ex.getMessage(), ex);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends ObjectType> void modifyProvisioningObjectWithExclusion(Class<T> type,
			ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		T object = getObject(type, change.getOid(), new PropertyReferenceListType(), result);

		if (object instanceof ResourceObjectShadowType) {
			object = (T) getObject(ResourceObjectShadowType.class, object.getOid(),
					new PropertyReferenceListType(), result);

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
				object = (T) ((JAXBElement<ResourceObjectShadowType>) JAXBUtil.unmarshal(xmlPatchedObject))
						.getValue();

				ObjectModificationType newChange = new BasicHandler(this, provisioning, repository,
						schemaHandler).processOutboundSchemaHandling(user, (ResourceObjectShadowType) object,
						result);
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
			provisioning.modifyObject(object.getClass(), change, scripts, result);
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

	private boolean existPropertyModification(List<Object> newChangeValues, List<Object> oldChangeValues) {
		for (Object newValue : newChangeValues) {
			for (Object oldValue : oldChangeValues) {
				if (JAXBUtil.getElementQName(oldValue).equals(JAXBUtil.getElementQName(newValue))) {
					return true;
				}
			}
		}
		return false;
	}

	private <T extends ObjectType> void modifyRepositoryObjectWithExclusion(Class<T> type,
			ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		if (type.isAssignableFrom(UserType.class)) {
			UserTypeHandler handler = new UserTypeHandler(this, provisioning, repository, schemaHandler);
			handler.modifyObjectWithExclusion(type, change, accountOid, result);
			return;
		}

		repository.modifyObject(type, change, result);
	}

	private void modifyTaskWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		taskManager.modifyTask(change, result);
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
}
