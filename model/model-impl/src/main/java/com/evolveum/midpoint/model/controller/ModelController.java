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
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.password.PasswordPolicyUtils;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.importer.ObjectImporter;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

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
public class ModelController implements ModelService, ModelInteractionService {

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
	private Clockwork clockwork;

	@Autowired(required = true)
	PrismContext prismContext;

	@Autowired(required = true)
	private ProvisioningService provisioning;

	@Autowired(required = true)
	private ModelObjectResolver objectResolver;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Autowired(required = true)
	private transient ImportAccountsFromResourceTaskHandler importAccountsFromResourceTaskHandler;

	@Autowired(required = true)
	private transient ObjectImporter objectImporter;

	@Autowired(required = false)
	private HookRegistry hookRegistry;

	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Autowired(required = true)
	private ChangeExecutor changeExecutor;

	@Autowired(required = true)
	private AuditService auditService;

	@Autowired(required = true)
	SystemConfigurationHandler systemConfigurationHandler;
	
	@Autowired(required = true)
	Projector projector;
	
	@Autowired(required = true)
	Protector protector;
	
	
	public ModelObjectResolver getObjectResolver() {
		return objectResolver;
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
			Collection<ObjectOperationOptions> options, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");
		RepositoryCache.enter();

		T object = null;
		try {
			OperationResult subResult = result.createSubresult(GET_OBJECT);
			subResult.addParams(new String[] { "oid", "options", "class" }, oid, options, clazz);

			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(oid);
			ref.setType(ObjectTypes.getObjectType(clazz).getTypeQName());
			Collection<ObjectOperationOption> rootOptions = ObjectOperationOptions.findRootOptions(options);
			object = objectResolver.getObject(clazz, oid, rootOptions, subResult);
			updateDefinition(object.asPrismObject(), result);

			// todo will be fixed after another interface cleanup
			// fix for resolving object properties.
			resolve(object.asPrismObject(), options, task, subResult);
		} finally {
			RepositoryCache.exit();
		}
		return object.asPrismObject();
	}

	protected void resolve(PrismObject<?> object, Collection<ObjectOperationOptions> options,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (object == null || options == null) {
			return;
		}

		for (ObjectOperationOptions option: options) {
			resolve(object, option, task, result);
		}
	}
	
	private void resolve(PrismObject<?> object, ObjectOperationOptions option, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (!option.hasOption(ObjectOperationOption.RESOLVE)) {
			return;
		}
		ObjectSelector selector = option.getSelector();
		if (selector == null) {
			return;
		}
		PropertyPath path = selector.getPath();
		resolve (object, path, option, task, result);
	}
		
	private void resolve(PrismObject<?> object, PropertyPath path, ObjectOperationOptions option, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (path == null || path.isEmpty()) {
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
				refObject = objectResolver.resolve(refVal, object.toString(), option.getOptions(), result);
				updateDefinition((PrismObject)refObject, result);
				refVal.setObject(refObject);
			}
			if (!rest.isEmpty()) {
				resolve(refObject, rest, option, task, result);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelService#executeChanges(java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, Task task,
			OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		OperationResult result = parentResult.createSubresult(EXECUTE_CHANGES);
		LensContext<?, ?> context = LensUtil.objectDeltaToContext(deltas, provisioning, prismContext, result);
		clockwork.run(context, task, result);
		// TODO: ERROR HANDLING
		result.computeStatus();
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelInteractionService#previewChanges(com.evolveum.midpoint.prism.delta.ObjectDelta, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends ObjectType, P extends ObjectType> ModelContext<F, P> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		OperationResult result = parentResult.createSubresult(PREVIEW_CHANGES);
		LensContext<F, P> context = (LensContext<F, P>) LensUtil.objectDeltaToContext(deltas, provisioning, prismContext, result);
		
		projector.project(context, "preview", result);
		context.distributeResource();
		
		// TODO: ERROR HANDLING
		result.computeStatus();
		
		return context;
	}

	@Deprecated
	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, Task task,
			OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");

		object.checkConsistence();
		
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

				LensContext<UserType, AccountShadowType> syncContext = userTypeAddToContext(userType.asPrismObject(), result);

				auditRecord.addDeltas(syncContext.getAllChanges());
				auditService.audit(auditRecord, task);

				objectDelta = (ObjectDelta<T>) syncContext.getFocusContext().getPrimaryDelta();

				if (clockwork.run(syncContext, task, result) != HookOperationMode.FOREGROUND) {
                    return null;
                }

				auditRecord.clearDeltas();
				auditRecord.addDeltas(syncContext.getAllChanges());

			} else {

				auditRecord.addDelta(objectDelta);
				auditService.audit(auditRecord, task);

				objectDelta.setObjectToAdd(object);

//				if (executePreChangePrimary(objectDelta, task, result) != HookOperationMode.FOREGROUND)
//					return null;

				LOGGER.trace("Executing GENERIC change " + objectDelta);
				Collection<ObjectDelta<T>> changes = MiscUtil.createCollection(objectDelta);
				changeExecutor.executeChanges((Collection)changes, result);

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

	private LensContext<UserType, AccountShadowType> userTypeAddToContext(PrismObject<UserType> user, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		UserType userType = user.asObjectable();
		LensContext<UserType, AccountShadowType> syncContext = new LensContext<UserType, AccountShadowType>(UserType.class, AccountShadowType.class, prismContext);

		ObjectDelta<UserType> userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD);
		userDelta.setObjectToAdd(user);

		LensFocusContext<UserType> focusContext = syncContext.createFocusContext();
		focusContext.setObjectOld(null);
		focusContext.setObjectNew(user);
		focusContext.setPrimaryDelta(userDelta);

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

	@Deprecated
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
			
			updateDefinitions(list, result);

		} finally {
			RepositoryCache.exit();
		}

		return list;
	}
	
	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
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
			
			updateDefinitions(list, result);

		} finally {
			RepositoryCache.exit();
		}

		return list;
	}

	@Deprecated
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
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
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

	
	@Deprecated
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
		// TODO: check definitions, but tolerate missing definitions in <attributes>

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

				LensContext<UserType, AccountShadowType> syncContext = userTypeModifyToContext(oid, modifications, result);
				
				Collection<ObjectDelta<? extends ObjectType>> allChanges = syncContext.getAllChanges();

				auditRecord.addDeltas(syncContext.getAllChanges());
				auditService.audit(auditRecord, task);

                if (clockwork.run(syncContext, task, result) != HookOperationMode.FOREGROUND) {
                    return;
                }

				// Deltas after sync will be different
				auditRecord.clearDeltas();
				allChanges = syncContext.getAllChanges();
				auditRecord.addDeltas(allChanges);

                result.computeStatus();

			} else {
				RefinedAccountDefinition accountDefinition = null;
				if (ResourceObjectShadowType.class.isAssignableFrom(type)) {
					Collection<? extends ResourceAttributeDefinition> attributeDefinitions = null;
					ResourceObjectShadowType shadow = (ResourceObjectShadowType) cacheRepositoryService.getObject(type,
							oid, result).asObjectable();
					String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
					ResourceType resource = provisioning.getObject(ResourceType.class, resourceOid, null,
							result).asObjectable();
					
					
					if (AccountShadowType.class.isAssignableFrom(type)) {
						// Need to read the shadow to get reference to resource and
						// account type
						RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
								prismContext);
//						PrismObjectDefinition<AccountShadowType> accountDefinition = refinedSchema
//								.getObjectDefinition(shadow);
						accountDefinition = refinedSchema.getAccountDefinition((AccountShadowType)shadow);
						attributeDefinitions = accountDefinition.getAttributeDefinitions();
					} else {
						ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
						ObjectClassComplexTypeDefinition objectClassDefinition = resourceSchema.findObjectClassDefinition(shadow);
						attributeDefinitions = objectClassDefinition.getAttributeDefinitions();
					}
					for (ItemDelta itemDelta: modifications) {
						ItemDefinition definition = itemDelta.getDefinition();
						if (definition == null) {
							if (itemDelta.getParentPath().equals(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES))) {
								applyAttributeDefinition(itemDelta, attributeDefinitions);
							} else {
								throw new SchemaException("Missing definition of "+itemDelta);
							}
						}
					}
				}
				
				objectDelta = (ObjectDelta<T>) ObjectDelta.createModifyDelta(oid, modifications, type);
				objectDelta.checkConsistence();
				
				if (objectDelta.hasItemDelta(SchemaConstants.PATH_PASSWORD_VALUE) && accountDefinition != null
						&& accountDefinition.getPasswordPolicy() != null
						&& accountDefinition.getPasswordPolicy().getOid() != null) {

					PrismObject<PasswordPolicyType> passwordPolicy = cacheRepositoryService.getObject(
							PasswordPolicyType.class, accountDefinition.getPasswordPolicy().getOid(), parentResult);
					
					if (passwordPolicy != null) {
						PropertyDelta passwordDelta = objectDelta
								.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
						PrismProperty<PasswordType> password = null;
						if (passwordDelta.isReplace()) {
							password = passwordDelta.getPropertyNew();
						}

						if (password != null) {
							ProtectedStringType protectedPass = password.getValue(ProtectedStringType.class).getValue();
							String passValue = protectedPass.getClearValue();
							if (passValue == null) {
								try{
								passValue = protector.decryptString(protectedPass);
								} catch(EncryptionException ex){
									LOGGER.error("Failed to process decsyption of password value.");
									throw new SchemaException("Failed to process decsyption of password value.");
								}
							}
							if (!PasswordPolicyUtils.validatePassword(passValue, passwordPolicy.asObjectable(), result)){
								throw new PolicyViolationException("Provided password does not satisfy specified password policies.");
							}

						}
					}

				}

				auditRecord.addDelta(objectDelta);
				auditService.audit(auditRecord, task);

				Collection<ObjectDelta<? extends ObjectType>> changes = new HashSet<ObjectDelta<? extends ObjectType>>();
				changes.add(objectDelta);

				try {
					changeExecutor.executeChanges(changes, parentResult);

                    // todo: is objectDelta the correct holder of the modifications?
                    executePostChange(objectDelta, task, result);

                    result.recordSuccessIfUnknown();
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

	private void applyAttributeDefinition(ItemDelta<?> itemDelta,
			Collection<? extends ResourceAttributeDefinition> attributeDefinitions) throws SchemaException {
		QName attributeName = itemDelta.getPath().last().getName();
		for (ResourceAttributeDefinition attrDef: attributeDefinitions) {
			if (attrDef.getName().equals(attributeName)) {
				itemDelta.applyDefinition(attrDef);
				return;
			}
		}
		throw new SchemaException("No definition for attribute "+attributeName);
	}

	private void logDebugChange(Class<?> type, String oid, Collection<? extends ItemDelta> modifications) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("model.modifyObject class={}, oid={}, change:\n{}",
					new Object[] { oid, type.getName(), DebugUtil.debugDump(modifications) });
		}
	}

	private LensContext<UserType, AccountShadowType> userTypeModifyToContext(String oid, Collection<? extends ItemDelta> modifications,
			OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException {
		LensContext<UserType, AccountShadowType> syncContext = new LensContext<UserType, AccountShadowType>(UserType.class, AccountShadowType.class, prismContext);

		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(oid, modifications, UserType.class);

		LensFocusContext<UserType> focusContext = syncContext.createFocusContext();
		focusContext.setObjectOld(null);
		focusContext.setObjectNew(null);
		focusContext.setPrimaryDelta(userDelta);

		return syncContext;
	}

	@Deprecated
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
				LensContext<UserType, AccountShadowType> syncContext = new LensContext<UserType, AccountShadowType>(UserType.class, AccountShadowType.class, prismContext);
				LensFocusContext<UserType> focusContext = syncContext.createFocusContext();
				focusContext.setObjectOld(null);
				focusContext.setObjectNew(null);
				focusContext.setPrimaryDelta((ObjectDelta<UserType>) objectDelta);

				try {
					if (clockwork.run(syncContext, task, result) != HookOperationMode.FOREGROUND) {
                        return;
                    }
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
    			try {
    				changeExecutor.executeChanges(changes, result);
                    executePostChange(changes, task, result);

    				auditRecord.clearDeltas();
    				auditRecord.addDeltas(changes);

    			} catch (ObjectAlreadyExistsException e) {
    				// TODO Better handling
    				throw new SystemException(e.getMessage(), e);
    			} catch (SchemaException e) {
    				// TODO Better handling
    				throw new SystemException(e.getMessage(), e);
    			}

			}

			result.recordSuccess();

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
		LOGGER.trace("Testing resource OID: {}", new Object[]{resourceOid});

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
		LOGGER.trace("Launching import from resource with oid {} for object class {}.", new Object[]{
                resourceOid, objectClass});

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
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Import result:\n{}", result.dump());
		}
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
	
	private <T extends ObjectType> void updateDefinitions(Collection<PrismObject<T>> objects, OperationResult result) throws ObjectNotFoundException, SchemaException {
		// TODO: optimize resource resolution
		for (PrismObject<T> object: objects) {
			updateDefinition(object, result);
		}
	}
	
	private <T extends ObjectType>  void updateDefinition(PrismObject<T> object, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (object.canRepresent(AccountShadowType.class)) {
			ResourceType resourceType = getResource((AccountShadowType)object.asObjectable(), result);
			updateAccountShadowDefinition((PrismObject<? extends AccountShadowType>)object, resourceType);
		}
	}
	
	private ResourceType getResource(ResourceObjectShadowType shadowType, OperationResult result) throws ObjectNotFoundException, SchemaException {
		ObjectReferenceType resourceRef = shadowType.getResourceRef();
		return objectResolver.resolve(resourceRef, ResourceType.class, "resource reference in "+shadowType, result);
	}

	private <T extends AccountShadowType> void updateAccountShadowDefinition(PrismObject<T> shadow, ResourceType resourceType) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
		QName objectClass = shadow.asObjectable().getObjectClass();
		RefinedAccountDefinition rAccountDef = refinedSchema.findAccountDefinitionByObjectClass(objectClass);
		PrismContainer<Containerable> attributesContainer = shadow.findContainer(AccountShadowType.F_ATTRIBUTES);
		attributesContainer.applyDefinition(rAccountDef, true);
	}

    /**
     * Methods to invoke old-style hooks, necessary for keeping SystemConfigurationHandler updated.
     * (Will disappear after non-user-related model actions will be migrated to new 'lens' paradigm.)
     */
    @Deprecated
    private void executePostChange(ObjectDelta<? extends ObjectType> objectDelta, Task task,
                                                OperationResult result) {
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        deltas.add(objectDelta);
        executePostChange(deltas, task, result);
    }

    @Deprecated
    private void executePostChange(Collection<ObjectDelta<? extends ObjectType>> objectDeltas,
                                                Task task, OperationResult result) {

        HookOperationMode resultMode = HookOperationMode.FOREGROUND;
        if (hookRegistry != null) {
            for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
                hook.postChange(objectDeltas, task, result);
            }
        }
    }


}