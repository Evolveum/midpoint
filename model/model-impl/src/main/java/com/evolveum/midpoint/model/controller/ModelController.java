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
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.importer.ObjectImporter;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * This used to be an interface, but it was switched to class for simplicity. I
 * don't expect that the implementation of the controller will be ever replaced.
 * In extreme case the whole Model will be replaced by a different
 * implementation, but not just the controller.
 * <p/>
 * However, the common way to extend the functionality will be the use of hooks
 * that are implemented here.
 * <p/>
 * Great deal of code is copied from the old ModelControllerImpl.
 * 
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
public class ModelController implements ModelService {

	// Constants for OperationResult
	public static final String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
	public static final String SEARCH_OBJECTS_IN_REPOSITORY = CLASS_NAME_WITH_DOT
			+ "searchObjectsInRepository";
	public static final String SEARCH_OBJECTS_IN_PROVISIONING = CLASS_NAME_WITH_DOT
			+ "searchObjectsInProvisioning";
	public static final String ADD_OBJECT_WITH_EXCLUSION = CLASS_NAME_WITH_DOT + "addObjectWithExclusion";
	public static final String MODIFY_OBJECT_WITH_EXCLUSION = CLASS_NAME_WITH_DOT
			+ "modifyObjectWithExclusion";
	public static final String CHANGE_ACCOUNT = CLASS_NAME_WITH_DOT + "changeAccount";

	public static final String GET_SYSTEM_CONFIGURATION = CLASS_NAME_WITH_DOT + "getSystemConfiguration";
	public static final String RESOLVE_USER_ATTRIBUTES = CLASS_NAME_WITH_DOT + "resolveUserAttributes";
	public static final String RESOLVE_ACCOUNT_ATTRIBUTES = CLASS_NAME_WITH_DOT + "resolveAccountAttributes";
	public static final String CREATE_ACCOUNT = CLASS_NAME_WITH_DOT + "createAccount";
	public static final String UPDATE_ACCOUNT = CLASS_NAME_WITH_DOT + "updateAccount";
	public static final String PROCESS_USER_TEMPLATE = CLASS_NAME_WITH_DOT + "processUserTemplate";

	private static final Trace LOGGER = TraceManager.getTrace(ModelController.class);

	@Autowired(required = true)
	private UserSynchronizer userSynchronizer;

	@Autowired(required = true)
	PrismContext prismContext;

	@Autowired(required = true)
	private ProvisioningService provisioning;

	@Autowired(required = true)
	private ModelObjectResolver objectResolver;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Autowired
	private ChangeExecutor changeExecutor;

	@Autowired(required = true)
	private transient ImportAccountsFromResourceTaskHandler importAccountsFromResourceTaskHandler;

	@Autowired(required = true)
	private transient ObjectImporter objectImporter;

	@Autowired(required = false)
	private HookRegistry hookRegistry;

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private AuditService auditService;

	@Autowired(required = true)
	SystemConfigurationHandler systemConfigurationHandler;
	
	public ModelObjectResolver getObjectResolver() {
		return objectResolver;
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
			Collection<PropertyPath> resolve, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException {
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
			object = objectResolver.resolve(ref, clazz, "getObject", subResult);

			// todo will be fixed after another interface cleanup
			// fix for resolving object properties.
			resolve(object.asPrismObject(), resolve, task, subResult);
		} finally {
			RepositoryCache.exit();
		}
		return object.asPrismObject();
	}

	protected void resolve(PrismObject<?> object, Collection<PropertyPath> resolve,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (object == null || resolve == null) {
			return;
		}

		for (PropertyPath path: resolve) {
			resolve(object, path, task, result);
		}
	}
	
	private void resolve(PrismObject<?> object, PropertyPath path, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (path.isEmpty()) {
			return;
		}
		PropertyPathSegment first = path.first();
		PropertyPath rest = path.rest();
		QName refName = first.getName();
		PrismReference reference = object.findReferenceByCompositeObjectElementName(refName);
		if (reference == null) {
			return;//throw new SchemaException("Cannot resolve: No reference "+refName+" in "+object);
		}
		for (PrismReferenceValue refVal: reference.getValues()) {
			PrismObject<?> refObject = refVal.getObject();
			if (refObject == null) {
				refObject = objectResolver.resolve(refVal, object.toString(), result);
				refVal.setObject(refObject);
			}
			if (!rest.isEmpty()) {
				resolve(refObject, rest, task, result);
			}
		}
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, Task task,
			OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");

		T objectType = object.asObjectable();
		// FIXME??
		prismContext.adopt(objectType);
		if (!(objectType instanceof ResourceObjectShadowType)) {
			Validate.notEmpty(objectType.getName(), "Object name must not be null or empty.");
		}

		OperationResult result = parentResult.createSubresult(ADD_OBJECT);
		result.addParams(new String[] { "object" }, object);
		String oid = null;

		// Task task = taskManager.createTaskInstance(); // in the future, this
		// task instance will come from GUI

		AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.ADD_OBJECT,
				AuditEventStage.REQUEST);

		RepositoryCache.enter();
		try {

			auditRecord.setTarget(object);
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(object.getCompileTimeClass(), ChangeType.ADD);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Entering addObject with {}", object);
				LOGGER.trace(object.dump());
			}

			if (objectType instanceof UserType) {
				UserType userType = (UserType) objectType;

				SyncContext syncContext = userTypeAddToContext(userType.asPrismObject(), result);

				auditRecord.addDeltas(syncContext.getAllChanges());
				auditService.audit(auditRecord, task);

				objectDelta = (ObjectDelta<T>) syncContext.getUserPrimaryDelta();

				if (executePreChangePrimary(objectDelta, task, result) != HookOperationMode.FOREGROUND)
					return null;

				userSynchronizer.synchronizeUser(syncContext, result);

				if (executePreChangeSecondary(syncContext.getAllChanges(), task, result) != HookOperationMode.FOREGROUND)
					return null;

				auditRecord.clearDeltas();
				auditRecord.addDeltas(syncContext.getAllChanges());

				changeExecutor.executeChanges(syncContext, result);

				executePostChange(syncContext.getAllChanges(), task, result);
				// here we don't care about the result (FOREGROUND/BACKGROUND)

			} else {

				auditRecord.addDelta(objectDelta);
				auditService.audit(auditRecord, task);

				objectDelta.setObjectToAdd(object);

				if (executePreChangePrimary(objectDelta, task, result) != HookOperationMode.FOREGROUND)
					return null;

				LOGGER.trace("Executing GENERIC change " + objectDelta);
				changeExecutor.executeChange(objectDelta, result);

				executePostChange(objectDelta, task, result);
			}

			oid = objectDelta.getOid();

			result.computeStatus();

		} catch (ExpressionEvaluationException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} catch (ConfigurationException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			result.recordFatalError(ex);
			LOGGER.error("model.addObject failed: {}", ex.getMessage(), ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
			auditRecord.setEventStage(AuditEventStage.EXECUTION);
			auditRecord.setResult(result);
			auditRecord.clearTimestamp();
			auditService.audit(auditRecord, task);
		}

		return oid;
	}

    private PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("System configuration version read from repo: " + config.getVersion());
        }
        return config;
    }

    /**
	 * Executes preChangePrimary on all registered hooks. Parameters (delta,
	 * task, result) are simply passed to these hooks.
	 * 
	 * @return FOREGROUND, if all hooks returns FOREGROUND; BACKGROUND if not.
	 * 
	 *         TODO in the future, maybe some error status returned from hooks
	 *         should be considered here.
	 */
	private HookOperationMode executePreChangePrimary(
			Collection<ObjectDelta<? extends ObjectType>> objectDeltas, Task task, OperationResult result) {

		HookOperationMode resultMode = HookOperationMode.FOREGROUND;
		if (hookRegistry != null) {
			for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
				HookOperationMode mode = hook.preChangePrimary(objectDeltas, task, result);
				if (mode == HookOperationMode.BACKGROUND)
					resultMode = HookOperationMode.BACKGROUND;
			}
		}
		return resultMode;
	}

	/**
	 * A convenience method when there is only one delta.
	 */
	private HookOperationMode executePreChangePrimary(ObjectDelta<? extends ObjectType> objectDelta,
			Task task, OperationResult result) {
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(objectDelta);
		return executePreChangePrimary(deltas, task, result);
	}

	/**
	 * Executes preChangeSecondary. See above for comments.
	 */
	private HookOperationMode executePreChangeSecondary(
			Collection<ObjectDelta<? extends ObjectType>> objectDeltas, Task task, OperationResult result) {

		HookOperationMode resultMode = HookOperationMode.FOREGROUND;
		if (hookRegistry != null) {
			for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
				HookOperationMode mode = hook.preChangeSecondary(objectDeltas, task, result);
				if (mode == HookOperationMode.BACKGROUND)
					resultMode = HookOperationMode.BACKGROUND;
			}
		}
		return resultMode;
	}

	/**
	 * Executes postChange. See above for comments.
	 */
	private HookOperationMode executePostChange(Collection<ObjectDelta<? extends ObjectType>> objectDeltas,
			Task task, OperationResult result) {

		HookOperationMode resultMode = HookOperationMode.FOREGROUND;
		if (hookRegistry != null) {
			for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
				HookOperationMode mode = hook.postChange(objectDeltas, task, result);
				if (mode == HookOperationMode.BACKGROUND)
					resultMode = HookOperationMode.BACKGROUND;
			}
		}
		return resultMode;
	}

	/**
	 * A convenience method when there is only one delta.
	 */
	private HookOperationMode executePostChange(ObjectDelta<? extends ObjectType> objectDelta, Task task,
			OperationResult result) {
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(objectDelta);
		return executePostChange(deltas, task, result);
	}

	private SyncContext userTypeAddToContext(PrismObject<UserType> user, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		UserType userType = user.asObjectable();
		SyncContext syncContext = new SyncContext(prismContext);

		ObjectDelta<UserType> userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD);
		userDelta.setObjectToAdd(user);

		syncContext.setUserOld(null);
		syncContext.setUserNew(user);
		syncContext.setUserPrimaryDelta(userDelta);

		return syncContext;
	}

	@Override
	public <T extends ObjectType> List<PrismObject<T>> listObjects(Class<T> objectType, PagingType paging,
			Task task, OperationResult result) {
		Validate.notNull(objectType, "Object type must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		RepositoryCache.enter();

		List<PrismObject<T>> list = null;

		try {
			if (paging == null) {
				LOGGER.trace("Listing objects of type {} (no paging).", objectType);
			} else {
				LOGGER.trace("Listing objects of type {} offset {} count {} ordered {} by {}.", new Object[] {
						objectType, paging.getOffset(), paging.getMaxSize(), paging.getOrderDirection(),
						paging.getOrderBy() });
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
				subResult.computeStatus();
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
				list = new ArrayList<PrismObject<T>>();
			}
			LOGGER.trace("Returning {} objects.", new Object[] { list.size() });

		} finally {
			RepositoryCache.exit();
		}
		return list;
	}

	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, QueryType query,
			PagingType paging, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		RepositoryCache.enter();

		List<PrismObject<T>> list = null;

		try {
			if (paging == null) {
				LOGGER.trace("Searching objects with null paging (query in TRACE).");
			} else {
				LOGGER.trace("Searching objects from {} to {} ordered {} by {} (query in TRACE).",
						new Object[] { paging.getOffset(), paging.getMaxSize(), paging.getOrderDirection(),
								paging.getOrderBy() });
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
				list = new ArrayList<PrismObject<T>>();
			}

		} finally {
			RepositoryCache.exit();
		}

		return list;
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, QueryType query,
			Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		// TODO: implement properly

		try {
			if (ObjectTypes.isObjectTypeManagedByProvisioning(type)) {
				return provisioning.countObjects(type, query, parentResult);
			} else {
				return cacheRepositoryService.countObjects(type, query, parentResult);
			}
		} catch (Exception ex) {
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {

		Validate.notNull(modifications, "Object modification must not be null.");
		Validate.notEmpty(oid, "Change oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying object with oid {}", oid);
			LOGGER.trace(DebugUtil.debugDump(modifications));
		}

		if (modifications.isEmpty()) {
			LOGGER.warn("Calling modifyObject with empty modificaiton set");
			return;
		}

		ItemDelta.checkConsistence(modifications);

		OperationResult result = parentResult.createSubresult(MODIFY_OBJECT);
		result.addParams(new String[] { "modifications" }, modifications);

		RepositoryCache.enter();

		AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.MODIFY_OBJECT,
				AuditEventStage.REQUEST);
		PrismObject<T> object = null;
		try {
			object = cacheRepositoryService.getObject(type, oid, result);
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			RepositoryCache.exit();
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			RepositoryCache.exit();
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			RepositoryCache.exit();
			throw e;
		}

		try {

			auditRecord.setTarget(object);

			ObjectDelta<T> objectDelta = null;

			if (UserType.class.isAssignableFrom(type)) {

				SyncContext syncContext = userTypeModifyToContext(oid, modifications, result);

				auditRecord.addDeltas(syncContext.getAllChanges());
				auditService.audit(auditRecord, task);

				Collection<ItemDelta> modificationsCloned = new ArrayList<ItemDelta>();

				for (ItemDelta delta : modifications) {
					ItemDelta deltaNew = delta.clone();
					modificationsCloned.add(deltaNew);

				}
				
				userSynchronizer.synchronizeUser(syncContext, result);

				// Deltas after sync will be different
				auditRecord.clearDeltas();
				auditRecord.addDeltas(syncContext.getAllChanges());

				try {
					changeExecutor.executeChanges(syncContext, result);
					result.computeStatus();
				} catch (ObjectAlreadyExistsException e) {
					LOGGER.debug("Restarting user synchronizer as a reaction to ObjectAlreadyExistsException", e);
					syncContext = userTypeModifyToContextAssertRecon(oid, modificationsCloned, result);
					result = parentResult.createSubresult(MODIFY_OBJECT);
					result.addParam("syncContext", syncContext);
					userSynchronizer.synchronizeUser(syncContext, result);
					try {
						changeExecutor.executeChanges(syncContext, parentResult);

                        // todo: call executePostChange with correct collection of modifications

					} catch (ObjectAlreadyExistsException ex) {
						throw new SystemException(ex.getMessage(), ex);
					}

				}

			} else {
				if (AccountShadowType.class.isAssignableFrom(type)) {
					// Need to read the shadow to get reference to resource and
					// account type
					AccountShadowType shadow = (AccountShadowType) cacheRepositoryService.getObject(type,
							oid, result).asObjectable();
					String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
					ResourceType resource = provisioning.getObject(ResourceType.class, resourceOid,
							result).asObjectable();
					RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
							prismContext);
					PrismObjectDefinition<AccountShadowType> accountDefinition = refinedSchema
							.getObjectDefinition(shadow);

					// This creates a better delta than the one above
				}
				objectDelta = (ObjectDelta<T>) ObjectDelta.createModifyDelta(oid, modifications, type);

				auditRecord.addDelta(objectDelta);
				auditService.audit(auditRecord, task);

				Collection<ObjectDelta<? extends ObjectType>> changes = new HashSet<ObjectDelta<? extends ObjectType>>();
				changes.add(objectDelta);

				try {
					changeExecutor.executeChanges(changes, parentResult);

                    // todo: is objectDelta the correct holder of the modifications?
                    executePostChange(objectDelta, task, result);

                    result.computeStatus();
				} catch (ObjectAlreadyExistsException e) {
					// This should not happen
					// TODO Better handling
					throw new SystemException(e.getMessage(), e);
				}
				// catch (ObjectNotFoundException ex) {
				// cacheRepositoryService.listAccountShadowOwner(oid,
				// parentResult);
				// }
            }


        } catch (ExpressionEvaluationException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
			// } catch (ObjectAlreadyExistsException ex) {
			// LOGGER.error("model.modifyObject failed: {}", ex.getMessage(),
			// ex);
			// result.recordFatalError(ex);
			// throw ex;
		} catch (SchemaException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			logDebugChange(type, oid, modifications);
			result.recordFatalError(ex);
			throw ex;
		} catch (ConfigurationException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			logDebugChange(type, oid, modifications);
			result.recordFatalError(ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			logDebugChange(type, oid, modifications);
			result.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
			logDebugChange(type, oid, modifications);
			result.recordFatalError(ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
			auditRecord.setEventStage(AuditEventStage.EXECUTION);
			auditRecord.setResult(result);
			auditRecord.clearTimestamp();
			auditService.audit(auditRecord, task);
		}
	}

	private void logDebugChange(Class<?> type, String oid, Collection<? extends ItemDelta> modifications) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("model.modifyObject class={}, oid={}, change:\n{}",
					new Object[] { oid, type.getName(), DebugUtil.debugDump(modifications) });
		}
	}

	private SyncContext userTypeModifyToContext(String oid, Collection<? extends ItemDelta> modifications,
			OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {
		SyncContext syncContext = new SyncContext(prismContext);

		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(oid, modifications, UserType.class);

		syncContext.setUserOld(null);
		syncContext.setUserNew(null);
		syncContext.setUserPrimaryDelta(userDelta);

		return syncContext;
	}

	private SyncContext userTypeModifyToContextAssertRecon(String oid, Collection<? extends ItemDelta> modifications,
			OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {
		
		SyncContext syncContext = new SyncContext(prismContext);
		syncContext.setDoReconciliationForAllAccounts(true);

		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(oid, modifications, UserType.class);

		// TODO? userOld?

		syncContext.setUserOld(null);
		syncContext.setUserNew(null);
		syncContext.setUserPrimaryDelta(userDelta);

		return syncContext;
	}
	
	private void addAccountToContext(SyncContext syncContext, AccountShadowType accountType,
			ChangeType changeType, OperationResult result) throws SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException, SecurityViolationException {

		String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
		if (resourceOid == null) {
			throw new SchemaException("Account shadow does not contain resource OID");
		}
		// We don't try to use resource that may be embedded in the account.
		// This is pure XML and does not contain
		// parsed schema. Fetching cached resource from provisioning is much
		// more efficient
		ResourceType resourceType = provisioning.getObject(ResourceType.class, resourceOid, result)
				.asObjectable();
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType,
				prismContext);
		syncContext.rememberResource(resourceType);

		// Chances are that the account is already "refined", but let's make
		// sure
		PrismObject<AccountShadowType> refinedAccount = refinedSchema.refine(accountType.asPrismObject());
		ObjectDelta<AccountShadowType> accountDelta = new ObjectDelta<AccountShadowType>(
				AccountShadowType.class, changeType);
		accountDelta.setObjectToAdd(refinedAccount);
		ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType.getAccountType());
		AccountSyncContext accountSyncContext = syncContext.createAccountSyncContext(rat);
		accountSyncContext.setAccountPrimaryDelta(accountDelta);

	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> clazz, String oid, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, ConsistencyViolationException,
			CommunicationException, SchemaException, ConfigurationException, PolicyViolationException,
			SecurityViolationException {
		Validate.notNull(clazz, "Class must not be null.");
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		OperationResult result = parentResult.createSubresult(DELETE_OBJECT);
		result.addParams(new String[] { "oid" }, oid);

		RepositoryCache.enter();

		AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.DELETE_OBJECT,
				AuditEventStage.REQUEST);
		PrismObject<T> object = null;
		try {
			object = cacheRepositoryService.getObject(clazz, oid, result);
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			RepositoryCache.exit();
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			RepositoryCache.exit();
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			RepositoryCache.exit();
			throw e;
		}

		try {
			auditRecord.setTarget(object);
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(clazz, ChangeType.DELETE);
			objectDelta.setOid(oid);
			auditRecord.addDelta(objectDelta);
			auditService.audit(auditRecord, task);

			LOGGER.trace("Deleting object with oid {}.", new Object[] { oid });

			Collection<ObjectDelta<? extends ObjectType>> changes = null;

			if (UserType.class.isAssignableFrom(clazz)) {
				SyncContext syncContext = new SyncContext(prismContext);
				syncContext.setUserOld(null);
				syncContext.setUserNew(null);
				syncContext.setUserPrimaryDelta((ObjectDelta<UserType>) objectDelta);

				try {
					userSynchronizer.synchronizeUser(syncContext, result);
				} catch (SchemaException e) {
					// TODO Better handling
					throw new SystemException(e.getMessage(), e);
				} catch (ExpressionEvaluationException e) {
					// TODO Better handling
					throw new SystemException(e.getMessage(), e);
				} catch (ConfigurationException e) {
					// TODO Better handling
					throw e;
				} catch (ObjectAlreadyExistsException e) {
					// TODO Better handling
					throw new SystemException(e.getMessage(), e);
				}

				changes = syncContext.getAllChanges();
			} else {
				changes = new HashSet<ObjectDelta<? extends ObjectType>>();
				changes.add(objectDelta);
			}

			try {
				changeExecutor.executeChanges(changes, result);

                executePostChange(changes, task, result);

				auditRecord.clearDeltas();
				auditRecord.addDeltas(changes);

                result.computeStatus();
			} catch (ObjectAlreadyExistsException e) {
				// TODO Better handling
				throw new SystemException(e.getMessage(), e);
			} catch (SchemaException e) {
				// TODO Better handling
				throw new SystemException(e.getMessage(), e);
			}

		} catch (ObjectNotFoundException ex) {
			LOGGER.error("model.deleteObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (CommunicationException ex) {
			LOGGER.error("model.deleteObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			LOGGER.error("model.deleteObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("model.deleteObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
			auditRecord.setEventStage(AuditEventStage.EXECUTION);
			auditRecord.setResult(result);
			auditRecord.clearTimestamp();
			auditService.audit(auditRecord, task);
		}
	}

	@Override
	public PrismObject<UserType> listAccountShadowOwner(String accountOid, Task task, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		RepositoryCache.enter();

		PrismObject<UserType> user = null;

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
	public <T extends ResourceObjectShadowType> List<PrismObject<T>> listResourceObjectShadows(
			String resourceOid, Class<T> resourceObjectShadowType, Task task, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		RepositoryCache.enter();

		List<PrismObject<T>> list = null;

		try {
			LOGGER.trace("Listing resource object shadows \"{}\" for resource with oid {}.", new Object[] {
					resourceObjectShadowType, resourceOid });

			OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
			subResult.addParams(new String[] { "resourceOid", "resourceObjectShadowType" }, resourceOid,
					resourceObjectShadowType);

			try {
				list = cacheRepositoryService.listResourceObjectShadows(resourceOid,
						resourceObjectShadowType, subResult);
				subResult.recordSuccess();
			} catch (ObjectNotFoundException ex) {
				subResult.recordFatalError("Resource with oid '" + resourceOid + "' was not found.", ex);
				RepositoryCache.exit();
				throw ex;
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't list resource object shadows type "
						+ "{} from repository for resource, oid {}", ex, resourceObjectShadowType,
						resourceOid);
				subResult.recordFatalError("Couldn't list resource object shadows type '"
						+ resourceObjectShadowType + "' from repository for resource, oid '" + resourceOid
						+ "'.", ex);
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(subResult.dump(false));
				}
			}

			if (list == null) {
				list = new ArrayList<PrismObject<T>>();
			}

		} finally {
			RepositoryCache.exit();
		}

		return list;
	}

	@Override
	public List<PrismObject<? extends ResourceObjectShadowType>> listResourceObjects(String resourceOid,
			QName objectClass, PagingType paging, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object type must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		RepositoryCache.enter();

		List<PrismObject<? extends ResourceObjectShadowType>> list = null;

		try {
			LOGGER.trace(
					"Listing resource objects {} from resource, oid {}, from {} to {} ordered {} by {}.",
					new Object[] { objectClass, resourceOid, paging.getOffset(), paging.getMaxSize(),
							paging.getOrderDirection(), paging.getOrderDirection() });

			OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECTS);
			subResult.addParams(new String[] { "resourceOid", "objectType", "paging" }, resourceOid,
					objectClass, paging);

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
				list = new ArrayList<PrismObject<? extends ResourceObjectShadowType>>();
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
	public OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException {
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
		ResourceType resource = null;
		try {
			resource = getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();
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

		PrismObject<SystemConfigurationType> systemConfiguration;
		try {
			systemConfiguration = getSystemConfiguration(result);
			systemConfigurationHandler.postInit(systemConfiguration, result);
		} catch (ObjectNotFoundException e) {
			String message = "No system configuration found, skipping application of initial system settings";
			LOGGER.error(message + ": " + e.getMessage(), e);
			result.recordWarning(message, e);
		} catch (SchemaException e) {
			String message = "Schema error in system configuration, skipping application of initial system settings";
			LOGGER.error(message + ": " + e.getMessage(), e);
			result.recordWarning(message, e);
		}

        taskManager.postInit(result);

		// Initialize provisioning
		provisioning.postInit(result);

        if (result.isUnknown()) {
		    result.computeStatus();
        }

		RepositoryCache.exit();
	}

}