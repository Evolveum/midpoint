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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ch.qos.logback.core.Context;

import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.importer.ObjectImporter;
import com.evolveum.midpoint.model.synchronizer.AssignmentProcessor;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ConsistencyViolationException;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDelta;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * This used to be an interface, but it was switched to class for simplicity. I don't expect that
 * the implementation of the controller will be ever replaced. In extreme case the whole Model will
 * be replaced by a different implementation, but not just the controller.
 * 
 * However, the common way to extend the functionality will be the use of hooks that are implemented
 * here.
 * 
 * Great deal of code is copied from the old ModelControllerImpl.
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
@Component
public class ModelController implements ModelService {

	// Constants for OperationResult
	public static final String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
	public static final String SEARCH_OBJECTS_IN_REPOSITORY = CLASS_NAME_WITH_DOT + "searchObjectsInRepository";
	public static final String SEARCH_OBJECTS_IN_PROVISIONING = CLASS_NAME_WITH_DOT + "searchObjectsInProvisioning";
	public static final String ADD_OBJECT_WITH_EXCLUSION = CLASS_NAME_WITH_DOT + "addObjectWithExclusion";
	public static final String MODIFY_OBJECT_WITH_EXCLUSION = CLASS_NAME_WITH_DOT + "modifyObjectWithExclusion";
	public static final String CHANGE_ACCOUNT = CLASS_NAME_WITH_DOT + "changeAccount";

	public static final String GET_SYSTEM_CONFIGURATION = CLASS_NAME_WITH_DOT + "getSystemConfiguration";
	public static final String RESOLVE_USER_ATTRIBUTES = CLASS_NAME_WITH_DOT + "resolveUserAttributes";
	public static final String RESOLVE_ACCOUNT_ATTRIBUTES = CLASS_NAME_WITH_DOT + "resolveAccountAttributes";
	public static final String CREATE_ACCOUNT = CLASS_NAME_WITH_DOT + "createAccount";
	public static final String UPDATE_ACCOUNT = CLASS_NAME_WITH_DOT + "updateAccount";
	public static final String PROCESS_USER_TEMPLATE = CLASS_NAME_WITH_DOT + "processUserTemplate";
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelController.class);
	
	@Autowired(required=true)
	private UserSynchronizer userSynchronizer;
	
	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;
	
	@Autowired(required = true)
	private ProvisioningService provisioning;
	
	@Autowired(required=true)
	private ObjectResolver objectResolver;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Autowired(required = true)
	private transient Protector protector;
	
	@Autowired(required = true)
	private transient TaskManager taskManager;

	@Autowired(required = true)
	private transient ImportAccountsFromResourceTaskHandler importAccountsFromResourceTaskHandler;
	
	@Autowired(required = true)
	private transient ObjectImporter objectImporter;

	
	@Override
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, PropertyReferenceListType resolve,
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");
		RepositoryCache.enter();

		T object = null;
		try {
			OperationResult subResult = result.createSubresult(GET_OBJECT);
			subResult.addParams(new String[] { "oid", "resolve", "class" }, oid, resolve, clazz);
	
			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(oid);
			ref.setType(ObjectTypes.getObjectType(clazz).getTypeQName());
			object = (T) objectResolver.resolve(ref , "getObject", subResult);
		} finally {
			RepositoryCache.exit();
		}
		return object;
	}
	
	@Override
	public <T extends ObjectType> String addObject(T object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		
		if (!(object instanceof ResourceObjectShadowType)) {
			Validate.notEmpty(object.getName(), "Object name must not be null or empty.");
		}
		
		OperationResult result = parentResult.createSubresult(ADD_OBJECT);
		result.addParams(new String[] { "object" }, object);
		String oid = null;
		
		RepositoryCache.enter();
		try {
			LOGGER.trace("Entering addObject with {}", ObjectTypeUtil.toShortString(object));
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(JAXBUtil.silentMarshalWrap(object));
			}
	
			ObjectDelta<T> objectDelta = null;
			Schema commonSchema = schemaRegistry.getCommonSchema();
			
			if (object instanceof UserType) {
				UserType userType = (UserType)object;
				
				SyncContext syncContext = userTypeAddToContext(userType, commonSchema, result);
				objectDelta = (ObjectDelta<T>) syncContext.getUserPrimaryDelta();
								
				userSynchronizer.synchronizeUser(syncContext, result);
				
				executeChanges(syncContext, result);
				
			} else {
				
				objectDelta = new ObjectDelta<T>((Class<T>) object.getClass(), ChangeType.ADD);
				MidPointObject<T> mpObject = commonSchema.parseObjectType(object);
				objectDelta.setObjectToAdd(mpObject);
				
				LOGGER.trace("Executing GENERIC change "+objectDelta);
				executeChange(objectDelta, result);
			}
			
			oid = objectDelta.getOid();
			result.computeStatus();
			
		} catch (ExpressionEvaluationException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}",ex.getMessage(), ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}",ex.getMessage(), ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}",ex.getMessage(), ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}",ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}",ex.getMessage(), ex);
			throw ex;			
		} finally {
			RepositoryCache.exit();
		}
		
		return oid;
	}
	
	private SyncContext userTypeAddToContext(UserType userType, Schema commonSchema, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {

		SyncContext syncContext = new SyncContext();
		
		// Convert all <account> instances to syncContext or <accountRef>s
		if (userType.getAccount() != null) {
			for (AccountShadowType accountType: userType.getAccount()) {
				String accountOid = accountType.getOid();
				if (accountOid != null) {
					// link to existing account expressed as <account> instead of <accountRef>
					ObjectReferenceType accountRef = ObjectTypeUtil.createObjectRef(accountType);
					userType.getAccountRef().add(accountRef);
				} else {
					// new account (no OID)					
					addAccountToContext(syncContext, accountType, ChangeType.ADD, commonSchema, result);
				}
				userType.getAccount().remove(accountType);
			}
		}
		
		ObjectDelta<UserType> userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD);
		MidPointObject<UserType> mpUser = commonSchema.parseObjectType(userType);
		userDelta.setObjectToAdd(mpUser);
		
		syncContext.setUserOld(null);
		syncContext.setUserNew(mpUser);
		syncContext.setUserPrimaryDelta(userDelta);

		return syncContext;
	}

	@Override
	public <T extends ObjectType> ResultList<T> listObjects(Class<T> objectType, PagingType paging,
			OperationResult result) {
		Validate.notNull(objectType, "Object type must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		
		RepositoryCache.enter();
		
		ResultList<T> list = null;
		
		try {
			if (paging == null) {
				LOGGER.trace("Listing objects of type {} (no paging).", objectType);
			} else {
				LOGGER.trace(
						"Listing objects of type {} offset {} count {} ordered {} by {}.",
						new Object[] { objectType, paging.getOffset(), paging.getMaxSize(),
								paging.getOrderDirection(), paging.getOrderBy() });
			}
	
			OperationResult subResult = result.createSubresult(LIST_OBJECTS);
			subResult.addParams(new String[] { "objectType", "paging" }, objectType, paging);
			
			try {
				if (ObjectTypes.isObjectTypeManagedByProvisioning(objectType)) {
					LOGGER.trace("Listing objects from provisioning.");
					list = provisioning.listObjects(objectType, paging, subResult);
				} else {
					LOGGER.trace("Listing objects from repository.");
					list = cacheRepositoryService.listObjects(objectType, paging, subResult);
				}
				subResult.recordSuccess();
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
				subResult.recordFatalError("Couldn't list objects.", ex);
				throw new SystemException(ex.getMessage(), ex);
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(subResult.dump(false));
				}
			}
	
			if (list == null) {
				list = new ResultArrayList<T>();
				list.setTotalResultCount(0);
			}
	
			LOGGER.trace("Returning {} objects.", new Object[] { list.size() });

		} finally {
			RepositoryCache.exit();
		}
		return list;
	}

	@Override
	public <T extends ObjectType> ResultList<T> searchObjects(Class<T> type, QueryType query,
			PagingType paging, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(query, "Query must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		RepositoryCache.enter();
		
		ResultList<T> list = null;

		try {
			if (paging == null) {
				LOGGER.trace("Searching objects with null paging (query in TRACE).");
			} else {
				LOGGER.trace("Searching objects from {} to {} ordered {} by {} (query in TRACE).",
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
			
			try {
				if (searchInProvisioning) {
					list = provisioning.searchObjects(type, query, paging, subResult);
				} else {
					list = cacheRepositoryService.searchObjects(type, query, paging, subResult);
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
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(subResult.dump(false));
				}
			}
	
			if (list == null) {
				list = new ResultArrayList<T>();
				list.setTotalResultCount(0);
			}
			
		} finally {
			RepositoryCache.exit();
		}
		
		return list;
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, ObjectModificationType change,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException {

		Validate.notNull(change, "Object modification must not be null.");
		Validate.notEmpty(change.getOid(), "Change oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying object with oid {}",
					new Object[] { change.getOid() });
			LOGGER.trace(JAXBUtil.silentMarshalWrap(change));
		}

		if (change.getPropertyModification().isEmpty()) {
			return;
		}
		
		OperationResult result = parentResult.createSubresult(MODIFY_OBJECT);
		result.addParams(new String[] { "change" }, change);

		RepositoryCache.enter();
				
		try {
			
			ObjectDelta<T> objectDelta = null;
			Schema commonSchema = schemaRegistry.getCommonSchema();
			Collection<ObjectDelta<?>> changes = null;
			
			if (UserType.class.isAssignableFrom(type)) {
				SyncContext syncContext = userTypeModifyToContext(change, commonSchema, result);
				
				userSynchronizer.synchronizeUser(syncContext, parentResult);
				
				changes = syncContext.getAllChanges();
			} else {
				objectDelta = ObjectDelta.createDelta(type, change, commonSchema);
				changes = new HashSet<ObjectDelta<?>>();
				changes.add(objectDelta);
			}
			
			try {
				executeChanges(changes, parentResult);
				result.computeStatus();
			} catch (ObjectAlreadyExistsException e) {
				// This should not happen
				// TODO Better handling
				throw new SystemException(e.getMessage(),e);
			}
			
		} catch (ExpressionEvaluationException ex) {
			LOGGER.error("model.modifyObject failed: {}",ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("model.modifyObject failed: {}",ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (SchemaException ex) {
			LOGGER.error("model.modifyObject failed: {}",ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("model.modifyObject failed: {}",ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}
	}

	private SyncContext userTypeModifyToContext(ObjectModificationType change, Schema commonSchema, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {
		SyncContext syncContext = new SyncContext();
		
		Iterator<PropertyModificationType> i = change.getPropertyModification().iterator();
		while (i.hasNext()) {
			PropertyModificationType propModType = i.next();
			XPathHolder propXPath = new XPathHolder(propModType.getPath());
			if (propXPath.isEmpty() &&
				!propModType.getValue().getAny().isEmpty() &&
				JAXBUtil.getElementQName(propModType.getValue().getAny().get(0)).equals(SchemaConstants.I_ACCOUNT)) {

				if (propModType.getModificationType() == PropertyModificationTypeType.add) {
			
					for (Object element: propModType.getValue().getAny()) {
						AccountShadowType accountShadowType = XsdTypeConverter.toJavaValue(element, AccountShadowType.class);
						
						addAccountToContext(syncContext, accountShadowType, ChangeType.ADD, commonSchema, result);
					}
										
				} else {
					throw new UnsupportedOperationException("Modification type "+propModType.getModificationType()+" with full <account> is not supported");
				}
				
				i.remove();
			}
			
		}
		
		// TODO? userOld?
		
		syncContext.setUserOld(null);
		syncContext.setUserNew(null);
		ObjectDelta<UserType> userDelta = ObjectDelta.createDelta(UserType.class, change, commonSchema);
		syncContext.setUserPrimaryDelta(userDelta);
		
		return syncContext;
	}

	private void addAccountToContext(SyncContext syncContext, AccountShadowType accountType,
			ChangeType changeType, Schema commonSchema, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {
		
		String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
		ResourceType resourceType = provisioning.getObject(ResourceType.class, resourceOid, null, result);
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, schemaRegistry);
		syncContext.rememberResource(resourceType);
		
		MidPointObject<AccountShadowType> mpAccount = refinedSchema.parseObjectType(accountType);
		ObjectDelta<AccountShadowType> accountDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, changeType);
		accountDelta.setObjectToAdd(mpAccount);
		ResourceAccountType rat = new ResourceAccountType(accountType.getResourceRef().getOid(), accountType.getAccountType());
		AccountSyncContext accountSyncContext = syncContext.createAccountSyncContext(rat);
		accountSyncContext.setAccountPrimaryDelta(accountDelta);
		
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> clazz, String oid, OperationResult result)
			throws ObjectNotFoundException, ConsistencyViolationException {
		Validate.notNull(clazz, "Class must not be null.");
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		
		RepositoryCache.enter();
		
		try {
			LOGGER.trace("Deleting object with oid {}.", new Object[] { oid });
	
			OperationResult subResult = result.createSubresult(DELETE_OBJECT);
			subResult.addParams(new String[] { "oid" }, oid);
			
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(clazz, ChangeType.DELETE);
			objectDelta.setOid(oid);
			
			Collection<ObjectDelta<?>> changes = null;
			
			if (UserType.class.isAssignableFrom(clazz)) {
				SyncContext syncContext = new SyncContext();
				syncContext.setUserOld(null);
				syncContext.setUserNew(null);
				syncContext.setUserPrimaryDelta((ObjectDelta<UserType>) objectDelta);
				
				try {
					userSynchronizer.synchronizeUser(syncContext, result);
				} catch (SchemaException e) {
					// TODO Better handling
					throw new SystemException(e.getMessage(),e);
				} catch (ExpressionEvaluationException e) {
					// TODO Better handling
					throw new SystemException(e.getMessage(),e);
				}
				
				changes = syncContext.getAllChanges();
			} else {
				changes = new HashSet<ObjectDelta<?>>();
				changes.add(objectDelta);
			}
			
			try {
				executeChanges(changes, result);
			} catch (ObjectAlreadyExistsException e) {
				// TODO Better handling
				throw new SystemException(e.getMessage(),e);
			} catch (SchemaException e) {
				// TODO Better handling
				throw new SystemException(e.getMessage(),e);
			}
		
		} finally {
			RepositoryCache.exit();
		}
	}
	
	
	
	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(properties, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		
		RepositoryCache.enter();
		LOGGER.trace("Getting property available values for object with oid {} (properties in TRACE).",
				new Object[] { oid });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(DebugUtil.prettyPrint(properties));
		}

		RepositoryCache.exit();
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		
		RepositoryCache.enter();
	
		UserType user = null;
		
		try {
			LOGGER.trace("Listing account shadow owner for account with oid {}.", new Object[] { accountOid });
	
			OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW_OWNER);
			subResult.addParams(new String[] { "accountOid" }, accountOid);
	
			try {
				user = cacheRepositoryService.listAccountShadowOwner(accountOid, subResult);
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
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(subResult.dump(false));
				}
			}
			
		} finally {
			RepositoryCache.exit();
		}
		
		return user;
	}

	@Override
	public <T extends ResourceObjectShadowType> ResultList<T> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		
		RepositoryCache.enter();
	
		ResultList<T> list = null;
		
		try {
			LOGGER.trace("Listing resource object shadows \"{}\" for resource with oid {}.", new Object[] {
					resourceObjectShadowType, resourceOid });
	
			OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
			subResult.addParams(new String[] { "resourceOid", "resourceObjectShadowType" }, resourceOid,
					resourceObjectShadowType);
	
			try {
				list = cacheRepositoryService.listResourceObjectShadows(resourceOid, resourceObjectShadowType,
						subResult);
				subResult.recordSuccess();
			} catch (ObjectNotFoundException ex) {
				subResult.recordFatalError("Resource with oid '" + resourceOid + "' was not found.", ex);
				RepositoryCache.exit();
				throw ex;
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't list resource object shadows type "
						+ "{} from repository for resource, oid {}", ex, resourceObjectShadowType, resourceOid);
				subResult.recordFatalError(
						"Couldn't list resource object shadows type '" + resourceObjectShadowType
								+ "' from repository for resource, oid '" + resourceOid + "'.", ex);
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(subResult.dump(false));
				}
			}
	
			if (list == null) {
				list = new ResultArrayList<T>();
				list.setTotalResultCount(0);
			}
			
		} finally {
			RepositoryCache.exit();
		}
		
		return list;
	}

	@Override
	public ResultList<? extends ResourceObjectShadowType> listResourceObjects(String resourceOid,
			QName objectClass, PagingType paging, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object type must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		
		RepositoryCache.enter();
	
		ResultList<? extends ResourceObjectShadowType> list = null;
		
		try {
			LOGGER.trace(
					"Listing resource objects {} from resource, oid {}, from {} to {} ordered {} by {}.",
					new Object[] { objectClass, resourceOid, paging.getOffset(), paging.getMaxSize(),
							paging.getOrderDirection(), paging.getOrderDirection() });
	
			OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECTS);
			subResult.addParams(new String[] { "resourceOid", "objectType", "paging" }, resourceOid, objectClass,
					paging);
	
			try {
	
				list = provisioning.listResourceObjects(resourceOid, objectClass, paging, subResult);
	
			} catch (SchemaException ex) {
				RepositoryCache.exit();
				subResult.recordFatalError("Schema violation");
				throw ex;
			} catch (CommunicationException ex) {
				RepositoryCache.exit();
				subResult.recordFatalError("Communication error");
				throw ex;
			} catch (ObjectNotFoundException ex) {
				RepositoryCache.exit();
				subResult.recordFatalError("Object not found");
				throw ex;
			}
			subResult.recordSuccess();
	
			if (list == null) {
				list = new ResultArrayList<ResourceObjectShadowType>();
				list.setTotalResultCount(0);
			}
		} finally {
			RepositoryCache.exit();
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
		RepositoryCache.enter();
		LOGGER.trace("Testing resource OID: {}", new Object[] { resourceOid });

		OperationResult testResult = null;
		try {
			testResult = provisioning.testResource(resourceOid);
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Error testing resource OID: {}: Object not found: {} ", new Object[] { resourceOid,
					ex.getMessage(), ex });
			RepositoryCache.exit();
			throw ex;
		} catch (SystemException ex) {
			LOGGER.error("Error testing resource OID: {}: Object not found: {} ", new Object[] { resourceOid,
					ex.getMessage(), ex });
			RepositoryCache.exit();
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Error testing resource OID: {}: {} ", new Object[] { resourceOid, ex.getMessage(),
					ex });
			RepositoryCache.exit();
			throw new SystemException(ex.getMessage(), ex);
		}

		if (testResult != null) {
			LOGGER.debug("Finished testing resource OID: {}, result: {} ", resourceOid,
					testResult.getStatus());
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Test result:\n{}", testResult.dump(false));
			}
		} else {
			LOGGER.error("Test resource returned null result");
		}
		RepositoryCache.exit();
		return testResult;
	}

	// Note: The result is in the task. No need to pass it explicitly
	@Override
	public void importAccountsFromResource(String resourceOid, QName objectClass, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(task, "Task must not be null.");
		RepositoryCache.enter();
		LOGGER.trace("Launching import from resource with oid {} for object class {}.", new Object[] {
				resourceOid, objectClass });

		OperationResult result = parentResult.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE);
		result.addParams(new String[] { "resourceOid", "objectClass", "task" }, resourceOid, objectClass,
				task);
		// TODO: add context to the result

		// Fetch resource definition from the repo/provisioning
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ResourceType resource = null;
		try {
			resource = getObject(ResourceType.class, resourceOid, resolve, result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found");
			RepositoryCache.exit();
			throw ex;
		}

		importAccountsFromResourceTaskHandler.launch(resource, objectClass, task, result);

		// The launch should switch task to asynchronous. It is in/out, so no
		// other action is needed

		if (task.isAsynchronous()) {
			result.recordStatus(OperationResultStatus.IN_PROGRESS, "Task running in background");
		} else {
			result.recordSuccess();
		}
		RepositoryCache.exit();
	}

	@Override
	public void importObjectsFromFile(File input, ImportOptionsType options, Task task,
			OperationResult parentResult) {
		// OperationResult result =
		// parentResult.createSubresult(IMPORT_OBJECTS_FROM_FILE);
		// TODO Auto-generated method stub
		RepositoryCache.enter();
		RepositoryCache.exit();
		throw new NotImplementedException();
	}

	@Override
	public void importObjectsFromStream(InputStream input, ImportOptionsType options, Task task,
			OperationResult parentResult) {
		RepositoryCache.enter();
		OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
		objectImporter.importObjects(input, options, task, result, cacheRepositoryService);
		// No need to compute status. The validator inside will do it.
		// result.computeStatus("Couldn't import object from input stream.");
		RepositoryCache.exit();
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
		RepositoryCache.enter();
		OperationResult result = parentResult.createSubresult(DISCOVER_CONNECTORS);
		Set<ConnectorType> discoverConnectors;
		try {
			discoverConnectors = provisioning.discoverConnectors(hostType, result);
		} catch (CommunicationException e) {
			result.recordFatalError(e.getMessage(), e);
			RepositoryCache.exit();
			throw e;
		}
		result.computeStatus("Connector discovery failed");
		RepositoryCache.exit();
		return discoverConnectors;
	}

	private String addProvisioningObject(ObjectType object, OperationResult result)
			throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {
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
	
	private void deleteProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid, OperationResult result)
			throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {
		
		try {
			// TODO: scripts
			provisioning.deleteObject(objectTypeClass, oid, null, result);
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SystemException(ex.getMessage(), ex);
		}
	}
	
	private void modifyProvisioningObject(Class<? extends ObjectType> objectTypeClass, ObjectModificationType objectChange, OperationResult result)
		throws ObjectNotFoundException {
		
		try {
			// TODO: scripts
			provisioning.modifyObject(objectTypeClass, objectChange, null, result);
		} catch (ObjectNotFoundException ex) {
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

	private ScriptsType getScripts(ObjectType object, OperationResult result) throws ObjectNotFoundException, SchemaException {
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
			throws ObjectNotFoundException, SchemaException {
		// inserting credentials to account if needed (with generated password)
		ResourceType resource = account.getResource();
		if (resource == null) {
			resource = getObject(ResourceType.class, account.getResourceRef().getOid(),
					new PropertyReferenceListType(), result);
		}

		if (resource == null || resource.getSchemaHandling() == null) {
			return;
		}

//		AccountType accountType = ModelUtils.getAccountTypeFromHandling(account, resource);
//		if (accountType == null || accountType.getCredentials() == null) {
//			return;
//		}

//		AccountType.Credentials credentials = accountType.getCredentials();
//		int randomPasswordLength = -1;
//		if (credentials.getRandomPasswordLength() != null) {
//			randomPasswordLength = credentials.getRandomPasswordLength().intValue();
//		}
//
//		if (randomPasswordLength != -1 && ModelUtils.getPassword(account).getProtectedString() == null) {
//			try {
//				ModelUtils.generatePassword(account, randomPasswordLength, protector);
//			} catch (Exception ex) {
//				throw new SystemException(ex.getMessage(), ex);
//			}
//		}
	}

	
	
	
	private void executeChanges(Collection<ObjectDelta<?>> changes, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		for (ObjectDelta<?> change: changes) {
			executeChange(change, result);
		}
	}
	
	private void executeChanges(SyncContext syncContext, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		
		ObjectDelta<UserType> userDelta = syncContext.getUserDelta();
		LOGGER.trace("Executing USER change "+userDelta);
		
		executeChange(userDelta, result);
		
		// userDelta is composite, mixed from primary and secondary. The OID set into
		// it will be lost ... unless we explicitly save it
		syncContext.setUserOid(userDelta.getOid());
		
		for (AccountSyncContext accCtx: syncContext.getAccountContexts()) {
			ObjectDelta<AccountShadowType> accDelta = accCtx.getAccountDelta();
			LOGGER.trace("Executing ACCOUNT change "+accDelta);
			executeChange(accDelta, result);
			// To make sure that the OID is set (e.g. after ADD operation)
			accCtx.setOid(accDelta.getOid());
			makeSureAccountIsLinked(syncContext.getUserNew(),accCtx, result);
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after change execution:\n{}",syncContext.dump());
		}
		
	}

	private void makeSureAccountIsLinked(MidPointObject<UserType> userNew, AccountSyncContext accCtx,
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		UserType userTypeNew = userNew.getOrParseObjectType();
		String accountOid = accCtx.getOid();
		if (accountOid == null) {
			throw new IllegalStateException("Account has null OID, this should not happen");
		}
		for (ObjectReferenceType accountRef: userTypeNew.getAccountRef()) {
			if (accountRef.getOid().equals(accountOid)) {
				// Already linked, nothing to do
				return;
			}
		}
		// Not linked, need to link
		linkAccount(userTypeNew.getOid(), accountOid, result);
	}

	private void linkAccount(String userOid, String accountOid, OperationResult result) throws ObjectNotFoundException, SchemaException {

		LOGGER.trace("Linking account "+accountOid+" to user "+userOid);
		ObjectReferenceType accountRef = new ObjectReferenceType();
		accountRef.setOid(accountOid);
		accountRef.setType(ObjectTypes.ACCOUNT.getTypeQName());
		
		PropertyModificationType accountRefMod = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.add, null, SchemaConstants.I_ACCOUNT_REF, accountRef);
		ObjectModificationType objectChange = new ObjectModificationType();
		objectChange.setOid(userOid);
		objectChange.getPropertyModification().add(accountRefMod);
		cacheRepositoryService.modifyObject(UserType.class, objectChange, result);
	}

	private void executeChange(ObjectDelta<?> change, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		
		if (change.getChangeType() == ChangeType.ADD) {
			executeAddition(change, result);
			
		} else if (change.getChangeType() == ChangeType.MODIFY) {
			executeModification(change, result);
			
		} else if (change.getChangeType() == ChangeType.DELETE) {
			executeDeletion(change, result);
		} 
	}
	
	private void executeAddition(ObjectDelta<?> change, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

		MidPointObject<?> mpObject = change.getObjectToAdd();
		mpObject.setObjectType(null);
		ObjectType object = mpObject.getOrParseObjectType();
		
		String oid = null;
		if (object instanceof TaskType) {
			oid = addTask((TaskType) object, result);
		} else if (ObjectTypes.isManagedByProvisioning(object)) {
			oid = addProvisioningObject(object, result);
		} else {
			oid = cacheRepositoryService.addObject(object, result);
		}
		change.setOid(oid);

	}

	private void executeDeletion(ObjectDelta<? extends ObjectType> change, OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {
		
		String oid = change.getOid();
		Class<? extends ObjectType> objectTypeClass = change.getObjectTypeClass();
		
		if (TaskType.class.isAssignableFrom(objectTypeClass)) {
			taskManager.deleteTask(oid, result);
		} else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
			deleteProvisioningObject(objectTypeClass, oid, result);
		} else {
			cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
		}
	}

	private void executeModification(ObjectDelta<?> change, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (change.isEmpty()) {
			// Nothing to do
			return;
		}
		String oid = change.getOid();
		Class<? extends ObjectType> objectTypeClass = change.getObjectTypeClass();
		ObjectModificationType objectChange = change.toObjectModificationType();
		
		if (TaskType.class.isAssignableFrom(objectTypeClass)) {
			taskManager.modifyTask(objectChange, result);
		} else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
			modifyProvisioningObject(objectTypeClass, objectChange, result);
		} else {
			cacheRepositoryService.modifyObject(objectTypeClass, objectChange, result);
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
		RepositoryCache.enter();
		OperationResult result = parentResult.createSubresult(POST_INIT);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ModelController.class);

		// TODO: initialize repository
		// TODO: initialize task manager

		// Initialize provisioning
		provisioning.postInit(result);

		result.computeStatus("Error occured during post initialization process.");

		RepositoryCache.exit();
	}

}