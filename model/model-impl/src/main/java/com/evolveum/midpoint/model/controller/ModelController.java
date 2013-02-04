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
import com.evolveum.midpoint.common.CompiletimeConfig;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.policy.PasswordPolicyUtils;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.importer.ObjectImporter;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.lens.RewindException;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismValidate;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

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
	SystemConfigurationHandler systemConfigurationHandler;
	
	@Autowired(required = true)
	private AuditService auditService;
	
	@Autowired(required = true)
	Projector projector;
	
	@Autowired(required = true)
	Protector protector;
	
	@Autowired(required = true)
	ModelDiagController modelDiagController;
	
	
	public ModelObjectResolver getObjectResolver() {
		return objectResolver;
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(result, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");
		RepositoryCache.enter();

		PrismObject<T> object = null;
		try {
			OperationResult subResult = result.createSubresult(GET_OBJECT);
			subResult.addParams(new String[] { "oid", "options", "class" }, oid, options, clazz);

			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(oid);
			ref.setType(ObjectTypes.getObjectType(clazz).getTypeQName());
			GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
			object = objectResolver.getObject(clazz, oid, rootOptions, subResult).asPrismObject();

            if (!GetOperationOptions.isRaw(rootOptions)) {
			    updateDefinition(object, result);
            }

			// todo will be fixed after another interface cleanup
			// fix for resolving object properties.
			resolve(object, options, task, subResult);
		} finally {
			RepositoryCache.exit();
		}
		if (CompiletimeConfig.CONSISTENCY_CHECKS) object.checkConsistence(true, false);
		return object;
	}

	protected void resolve(PrismObject<?> object, Collection<SelectorOptions<GetOperationOptions>> options,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (object == null || options == null) {
			return;
		}

		for (SelectorOptions<GetOperationOptions> option: options) {
			try{
			resolve(object, option, task, result);
			} catch(ObjectNotFoundException ex){
				result.recordFatalError(ex.getMessage(), ex);
				return;
			}
		}
	}
	
	private void resolve(PrismObject<?> object, SelectorOptions<GetOperationOptions> option, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (!GetOperationOptions.isResolve(option.getOptions())) {
			return;
		}
		ObjectSelector selector = option.getSelector();
		if (selector == null) {
			return;
		}
		ItemPath path = selector.getPath();
		resolve (object, path, option, task, result);
	}
		
	private void resolve(PrismObject<?> object, ItemPath path, SelectorOptions<GetOperationOptions> option, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (path == null || path.isEmpty()) {
			return;
		}
		ItemPathSegment first = path.first();
		ItemPath rest = path.rest();
		QName refName = ItemPath.getName(first);
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
	public void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
			Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {

		OperationResult result = parentResult.createSubresult(EXECUTE_CHANGES);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("MODEL.executeChanges(\n  deltas:\n{}\n  options:{}", DebugUtil.debugDump(deltas, 2), options);
		}
		
		RepositoryCache.enter();
        setRequesteeIfNecessary(task, deltas, result);
		Collection<ObjectDelta<? extends ObjectType>> clonedDeltas = null;
		try {
		
			if (ModelExecuteOptions.isRaw(options)) {
				// Go directly to repository
				AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.EXECUTE_CHANGES_RAW, AuditEventStage.REQUEST);
				auditRecord.addDeltas(deltas);
				auditService.audit(auditRecord, task);
				for(ObjectDelta<? extends ObjectType> delta: deltas) {
					if (ModelExecuteOptions.isCrypt(options)) {
						encryptValues(delta, result);
					}
					if (delta.isAdd()) {
						String oid = cacheRepositoryService.addObject(delta.getObjectToAdd(), result);
						delta.setOid(oid);
					} else if (delta.isDelete()) {
						cacheRepositoryService.deleteObject(delta.getObjectTypeClass(), delta.getOid(), result);
					} else if (delta.isModify()) {
						cacheRepositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(), 
								delta.getModifications(), result);
					} else {
						throw new IllegalArgumentException("Wrong delta type "+delta.getChangeType()+" in "+delta);
					}
				}
				auditRecord.setTimestamp(null);
				auditRecord.setOutcome(OperationResultStatus.SUCCESS);
				auditRecord.setEventStage(AuditEventStage.EXECUTION);
				auditService.audit(auditRecord, task);
				
			} else {
				clonedDeltas = new ArrayList<ObjectDelta<? extends ObjectType>>(deltas.size());
				for (ObjectDelta delta : deltas){
					clonedDeltas.add(delta.clone());
				}
				
				int rewindAttempts = 0;
				while (true) {
					RewindException rewindException = null;
					LensContext<?, ?> context = LensUtil.objectDeltasToContext(deltas, provisioning, prismContext, task, result);
					context.setOptions(options);
					try {
						
						clockwork.run(context, task, result);
						
						// No rewind exception, the execution was acceptable
						break;
						
					} catch (RewindException e) {
						rewindException = e;
						LOGGER.debug("Rewind caused by {} (attempt {})", new Object[]{ e.getCause(), rewindAttempts, e.getCause()});
						rewindAttempts++;
						if (rewindAttempts >= Clockwork.MAX_REWIND_ATTEMPTS) {
							result.recordFatalError(rewindException.getCause());
							Clockwork.throwException(rewindException.getCause());
						}
						result.muteLastSubresultError();						
					}
				}
			}
			
			result.computeStatus();

            if (result.isInProgress()) {       // todo fix this hack (computeStatus does not take the root-level status into account, but clockwork.run sets "in-progress" flag just at the root level)
                if (result.isSuccess()) {
                    result.recordInProgress();
                }
            }
			
		} catch (ObjectAlreadyExistsException e) {
//			try {
//				//TODO: log reset operation, maybe add new result informing about the situation
//				result.muteLastSubresultError();
//				LOGGER.trace("Reseting add operation, recomputing user and his accounts.");
//				LensContext<?, ?> context = LensUtil.objectDeltasToContext(clonedDeltas, provisioning, prismContext, task, result);
//				clockwork.run(context, task, result);
//				result.computeStatus();
//			} catch (SystemException ex){
//				result.recordFatalError(e);
//				throw e;
//			}
			result.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ExpressionEvaluationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ConfigurationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (PolicyViolationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		} finally {
			RepositoryCache.exit();
		}
	}

    private void setRequesteeIfNecessary(Task task, Collection<ObjectDelta<? extends ObjectType>> deltas, OperationResult result) throws ObjectNotFoundException, SchemaException {

        if (task.getRequesteeOid() != null) {
            return;     // nothing to do
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Trying to find requestee within deltas: " + deltas);
        }
        String requesteeOid = null;
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            LOGGER.info("Trying to find requestee within delta: " + delta.debugDump());
            if (UserType.class.isAssignableFrom(delta.getObjectTypeClass()) && delta.getOid() != null) {
                if (requesteeOid == null) {
                    requesteeOid = delta.getOid();
                } else {
                    if (!requesteeOid.equals(delta.getOid())) {
                        LOGGER.warn("Ambiguous requestee in model operation; is it " + requesteeOid + " or " + delta.getOid() + "? deltas = " + deltas);
                    }
                }
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Result: requesteeOid = " + requesteeOid);
        }
        if (requesteeOid != null) {
            task.setRequesteeOidImmediate(requesteeOid, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Requestee OID set to " + requesteeOid + "; deltas = " + deltas);
            }
        }
    }

    private void encryptValues(ObjectDelta delta, OperationResult objectResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {

		if (!delta.hasCompleteDefinition()) {
			provisioning.applyDefinition(delta, objectResult);
		}

		Utils.encryptValues(protector, delta, objectResult);
		return;
	}
	
	private void encryptValue(PrismProperty password){
		PrismPropertyDefinition def = password.getDefinition();
		if (def == null || def.getTypeName() == null){
			LOGGER.trace("No definition for property " + password.getName());
			return;
		}
		if (!def.getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)){
			return;
		}
		
		ProtectedStringType passValue = (ProtectedStringType) password.getValue().getValue();
		
		if (passValue.getClearValue() != null) {
			try {
				LOGGER.info("Encrypting cleartext value for field " + password.getName() + ".");
				protector.encrypt(passValue);
			} catch (EncryptionException e) {
				LOGGER.info("Faild to encrypt cleartext value for field " + password.getName() + ".");
				return;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelInteractionService#previewChanges(com.evolveum.midpoint.prism.delta.ObjectDelta, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends ObjectType, P extends ObjectType> ModelContext<F, P> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes input:\n{}", DebugUtil.debugDump(deltas));
		}
		
		Collection<ObjectDelta<? extends ObjectType>> clonedDeltas = new ArrayList<ObjectDelta<? extends ObjectType>>(deltas.size());
		for (ObjectDelta delta : deltas){
			clonedDeltas.add(delta.clone());
		}
		
		OperationResult result = parentResult.createSubresult(PREVIEW_CHANGES);
		LensContext<F, P> context = null;
		
		try {
			
			//used cloned deltas instead of origin deltas, because some of the values should be lost later..
			context = (LensContext<F, P>) LensUtil.objectDeltasToContext(clonedDeltas, provisioning, prismContext, task, result);
			context.setOptions(options);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.trace("Preview changes context:\n{}", context.debugDump());
			}
		
			
			projector.project(context, "preview", result);
			context.distributeResource();
			
		} catch (ConfigurationException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (ExpressionEvaluationException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (PolicyViolationException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			LOGGER.error("model.previewChanges failed: {}", e.getMessage(), e);
			throw e;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes output:\n{}", context.dump());
		}
		
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
			PrismValidate.notEmpty(objectType.getName(), "Object name must not be null or empty.");
		}

		OperationResult result = parentResult.createSubresult(ADD_OBJECT);
		result.addParams(new String[] { "object" }, object);
		String oid = null;

		// Task task = taskManager.createTaskInstance(); // in the future, this
		// task instance will come from GUI

		RepositoryCache.enter();
		try {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Entering addObject with {}", object);
				LOGGER.trace(object.dump());
			}
			
			ObjectDelta<T> objectDelta = ObjectDelta.createAddDelta(object);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			executeChanges(deltas, null, task, result);
			
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

		ObjectDelta<UserType> userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD, prismContext);
		userDelta.setObjectToAdd(user);

		LensFocusContext<UserType> focusContext = syncContext.createFocusContext();
		focusContext.setObjectOld(null);
		focusContext.setObjectNew(user);
		focusContext.setPrimaryDelta(userDelta);

		return syncContext;
	}

	
	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		if (query != null) {
			ModelUtils.validatePaging(query.getPaging());
		}
		RepositoryCache.enter();

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		List<PrismObject<T>> list = null;
		try {
			if (query != null){
                if (query.getPaging() == null) {
                    LOGGER.trace("Searching objects with null paging (query in TRACE).");
                } else {
                    LOGGER.trace("Searching objects from {} to {} ordered {} by {} (query in TRACE).",
                            new Object[] { query.getPaging().getOffset(), query.getPaging().getMaxSize(),
                                    query.getPaging().getDirection(), query.getPaging().getOrderBy() });
                }
			}
			boolean searchInProvisioning = ObjectTypes.isClassManagedByProvisioning(type);
			String operationName = searchInProvisioning ? SEARCH_OBJECTS_IN_PROVISIONING
					: SEARCH_OBJECTS_IN_REPOSITORY;
			OperationResult subResult = result.createSubresult(operationName);
			subResult.addParams(new String[] { "query", "paging", "searchInProvisioning" },
                    query, (query != null ? query.getPaging() : "undefined"), searchInProvisioning);

			try {
				if (!GetOperationOptions.isRaw(rootOptions) && searchInProvisioning) {
					list = provisioning.searchObjects(type, query, subResult);
				} else {
					list = cacheRepositoryService.searchObjects(type, query, subResult);
				}
				subResult.recordSuccess();
			} catch (CommunicationException e) {
				processSearchException(e, rootOptions, searchInProvisioning, subResult);
				throw e;
			} catch (ConfigurationException e) {
				processSearchException(e, rootOptions, searchInProvisioning, subResult);
				throw e;
			} catch (ObjectNotFoundException e) {
				processSearchException(e, rootOptions, searchInProvisioning, subResult);
				throw e;
			} catch (SchemaException e) {
				processSearchException(e, rootOptions, searchInProvisioning, subResult);
				throw e;
			} catch (SecurityViolationException e) {
				processSearchException(e, rootOptions, searchInProvisioning, subResult);
				throw e;
			} catch (RuntimeException e) {
				processSearchException(e, rootOptions, searchInProvisioning, subResult);
				throw e;
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(subResult.dump(false));
				}
			}

			if (list == null) {
				list = new ArrayList<PrismObject<T>>();
			}

            if (!GetOperationOptions.isRaw(rootOptions)) {
			    updateDefinitions(list, result);
            }

		} finally {
			RepositoryCache.exit();
		}
		
		if (CompiletimeConfig.CONSISTENCY_CHECKS) {
			for (PrismObject<T> object: list) {
				object.checkConsistence(true, false);
			}
		}

		return list;
	}


	private void processSearchException(Exception e, GetOperationOptions rootOptions,
			boolean searchInProvisioning, OperationResult result) {
		String message;
		if (GetOperationOptions.isRaw(rootOptions) || !searchInProvisioning) {
			message = "Couldn't search objects in repository";
		} else {
			message = "Couldn't search objects in provisioning";
		}
		LoggingUtils.logException(LOGGER, message, e);
		result.recordFatalError(message, e);
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
		// TODO: implement properly

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		try {
			if (!GetOperationOptions.isRaw(rootOptions)
                    && ObjectTypes.isObjectTypeManagedByProvisioning(type)) {
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

		try {

			ObjectDelta<T> objectDelta = (ObjectDelta<T>) ObjectDelta.createModifyDelta(oid, modifications, type, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			executeChanges(deltas, null, task, result);

            result.computeStatus();
			
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
		}
	}

	private void applyAttributeDefinition(ItemDelta<?> itemDelta,
			Collection<? extends ResourceAttributeDefinition> attributeDefinitions) throws SchemaException {
		QName attributeName = ItemPath.getName(itemDelta.getPath().last());
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

		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(oid, modifications, UserType.class, prismContext);

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

		try {
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(clazz, ChangeType.DELETE, prismContext);
			objectDelta.setOid(oid);

			LOGGER.trace("Deleting object with oid {}.", new Object[] { oid });
			
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			executeChanges(deltas, null, task, result);

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
		} catch (ObjectAlreadyExistsException ex) {
			LOGGER.error("model.deleteObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw new SystemException(ex.getMessage(), ex);
		} catch (ExpressionEvaluationException ex) {
			LOGGER.error("model.deleteObject failed: {}", ex.getMessage(), ex);
			result.recordFatalError(ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			RepositoryCache.exit();
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
			QName objectClass, ObjectPaging paging, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {
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
							paging.getOrderBy(), paging.getDirection() });

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
			} catch (ConfigurationException ex) {
				RepositoryCache.exit();
				subResult.recordFatalError("Configuration error");
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
		
		result.recordStatus(OperationResultStatus.IN_PROGRESS, "Task running in background");

		importAccountsFromResourceTaskHandler.launch(resource, objectClass, task, result);

		// The launch should switch task to asynchronous. It is in/out, so no
		// other action is needed

		if (!task.isAsynchronous()) {
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
			ResourceType resourceType = null;
			try{
				resourceType = getResource((AccountShadowType)object.asObjectable(), result);
			} catch (ObjectNotFoundException ex){
				result.recordFatalError("Resource defined in account was not found: " + ex.getMessage(), ex);
				return;
			}
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
//    @Deprecated
//    private void executePostChange(ObjectDelta<? extends ObjectType> objectDelta, Task task,
//                                                OperationResult result) {
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        deltas.add(objectDelta);
//        executePostChange(deltas, task, result);
//    }

//    @Deprecated
//    private void executePostChange(Collection<ObjectDelta<? extends ObjectType>> objectDeltas,
//                                                Task task, OperationResult result) {
//
//        HookOperationMode resultMode = HookOperationMode.FOREGROUND;
//        if (hookRegistry != null) {
//            for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
//                hook.postChange(objectDeltas, task, result);
//            }
//        }
//    }

}