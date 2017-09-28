/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.hooks.ReadHook;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultRunner;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.CompareResultType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;

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
 *
 * Note: don't autowire this bean by implementing class (ModelController), as it is proxied by Spring AOP.
 * Use its interfaces instead.
 */
@Component
public class ModelController implements ModelService, TaskService, WorkflowService, ScriptingService, AccessCertificationService {

	// Constants for OperationResult
	public static final String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
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
	private PrismContext prismContext;

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

    @Autowired(required = false)                        // not required in all circumstances
    private WorkflowManager workflowManager;

	@Autowired(required = false)                        // not required in all circumstances
	private CertificationManager certificationManager;

    @Autowired(required = true)
    private ScriptingExpressionEvaluator scriptingExpressionEvaluator;

	@Autowired(required = true)
	private ChangeExecutor changeExecutor;

	@Autowired(required = true)
	private AuditService auditService;

	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;

	@Autowired(required = true)
	private UserProfileService userProfileService;

	@Autowired(required = true)
	private Projector projector;

	@Autowired(required = true)
	private Protector protector;

	@Autowired(required = true)
	private ModelDiagController modelDiagController;

	@Autowired(required = true)
	private ContextFactory contextFactory;

	@Autowired(required = true)
	private SchemaTransformer schemaTransformer;
	
	@Autowired(required = true)
	private ObjectMerger objectMerger;
	
	@Autowired(required = true)
	private SystemObjectCache systemObjectCache;

	public ModelObjectResolver getObjectResolver() {
		return objectResolver;
	}

	private WorkflowManager getWorkflowManagerChecked() {
		if (workflowManager == null) {
			throw new SystemException("Workflow manager not present");
		}
		return workflowManager;
	}

	private CertificationManager getCertificationManagerChecked() {
		if (certificationManager == null) {
			throw new SystemException("Certification manager not present");
		}
		return certificationManager;
	}


	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
			Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");
		RepositoryCache.enter();

		PrismObject<T> object;
		OperationResult result = parentResult.createMinorSubresult(GET_OBJECT);
        result.addParam("oid", oid);
        result.addCollectionOfSerializablesAsParam("options", rawOptions);
        result.addParam("class", clazz);

		Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		try {
            if (GetOperationOptions.isRaw(rootOptions)) {       // MID-2218
                QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
            }
			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(oid);
			ref.setType(ObjectTypes.getObjectType(clazz).getTypeQName());
            Utils.clearRequestee(task);

//            Special-purpose code to hunt down read-write resource fetch from GUI.
//            Normally the code is not active. It is too brutal. Just for MID-3424.
//            if (ResourceType.class == clazz && !GetOperationOptions.isRaw(rootOptions) && !GetOperationOptions.isReadOnly(rootOptions)) {
//            	LOGGER.info("READWRITE resource get: {} {}:\n{}", oid, options, 
//            			LoggingUtils.dumpStackTrace());
//            }

            object = (PrismObject<T>) objectResolver.getObject(clazz, oid, options, task, result).asPrismObject();

            object = object.cloneIfImmutable();
            schemaTransformer.applySchemasAndSecurity(object, rootOptions, null, task, result);
			resolve(object, options, task, result);

		} catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			if (GetOperationOptions.isAllowNotFound(rootOptions)){
				result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
			} else {
				ModelUtils.recordFatalError(result, e);
			}
			throw e;
		} finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
			RepositoryCache.exit();
		}

		result.cleanupResult();

		return object;
	}

	private void resolve(PrismObject<?> object, Collection<SelectorOptions<GetOperationOptions>> options,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
		if (object == null) {
			return;
		}
		resolve(object.asObjectable(), options, task, result);
	}

	private void resolve(Containerable containerable, Collection<SelectorOptions<GetOperationOptions>> options,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
		if (containerable == null || options == null) {
			return;
		}
		for (SelectorOptions<GetOperationOptions> option: options) {
			try {
				resolve(containerable, option, task, result);
			} catch (ObjectNotFoundException ex) {
				result.recordWarning(ex.getMessage(), ex);
			}
		}
	}

    private void resolve(Containerable object, SelectorOptions<GetOperationOptions> option, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
		if (!GetOperationOptions.isResolve(option.getOptions())) {
			return;
		}
		ObjectSelector selector = option.getSelector();
		if (selector == null) {
			return;
		}
		ItemPath path = selector.getPath();
		ItemPath.checkNoSpecialSymbolsExceptParent(path);
		resolve(object, path, option, task, result);
	}

	// TODO clean this mess
	private <O extends ObjectType> void resolve(Containerable containerable, ItemPath path, SelectorOptions<GetOperationOptions> option, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
		if (path == null || path.isEmpty()) {
			return;
		}
		ItemPathSegment first = path.first();
		ItemPath rest = path.rest();
		PrismContainerValue<?> containerValue = containerable.asPrismContainerValue();
		if (first instanceof NameItemPathSegment) {
			QName refName = ItemPath.getName(first);
			PrismReference reference = containerValue.findReferenceByCompositeObjectElementName(refName);
			if (reference == null) {
				reference = containerValue.findReference(refName);	// alternatively look up by reference name (e.g. linkRef)
			}
			if (reference != null) {
				for (PrismReferenceValue refVal : reference.getValues()) {
					PrismObject<O> refObject = refVal.getObject();
					if (refObject == null) {
						refObject = objectResolver.resolve(refVal, containerable.toString(), option.getOptions(), task, result);
						refObject = refObject.cloneIfImmutable();
						schemaTransformer.applySchemasAndSecurity(refObject, option.getOptions(), null, task, result);
						refVal.setObject(refObject);
					}
					if (!rest.isEmpty()) {
						resolve(refObject.asObjectable(), rest, option, task, result);
					}
				}
				return;
			}
		}
		if (rest.isEmpty()) {
			return;
		}
		if (first instanceof ParentPathSegment) {
			PrismContainerValue<?> parent = containerValue.getParentContainerValue();
			if (parent != null) {
				resolve(parent.asContainerable(), rest, option, task, result);
			}
		} else {
			QName nextName = ItemPath.getName(first);
			PrismContainer<?> nextContainer = containerValue.findContainer(nextName);
			if (nextContainer != null) {
				for (PrismContainerValue<?> pcv : nextContainer.getValues()) {
					resolve(pcv.asContainerable(), rest, option, task, result);
				}
			}
		}
	}

    @Override
    public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(final Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
                                                                                 Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        return executeChanges(deltas, options, task, null, parentResult);
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelService#executeChanges(java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(final Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
			Task task, Collection<ProgressListener> statusListeners, OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();

		OperationResult result = parentResult.createSubresult(EXECUTE_CHANGES);
		result.addParam(OperationResult.PARAM_OPTIONS, options);

		// Search filters treatment: if reevaluation is requested, we have to deal with three cases:
		// 1) for ADD operation: filters contained in object-to-be-added -> these are treated here
		// 2) for MODIFY operation: filters contained in existing object (not touched by deltas) -> these are treated after the modify operation
		// 3) for MODIFY operation: filters contained in deltas -> these have to be treated here, because if OID is missing from such a delta, the change would be rejected by the repository
		if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {
			for (ObjectDelta<? extends ObjectType> delta : deltas) {
				Utils.resolveReferences(delta, cacheRepositoryService, false, true, EvaluationTimeType.IMPORT, true, prismContext, result);
			}
		} else if (ModelExecuteOptions.isIsImport(options)) {
			// if plain import is requested, we simply evaluate filters in ADD operation (and we do not force reevaluation if OID is already set)
			for (ObjectDelta<? extends ObjectType> delta : deltas) {
				if (delta.isAdd()) {
					Utils.resolveReferences(delta.getObjectToAdd(), cacheRepositoryService, false, false, EvaluationTimeType.IMPORT, true, prismContext, result);
				}
			}
		}
		// Make sure everything is encrypted as needed before logging anything.
		// But before that we need to make sure that we have proper definition, otherwise we
		// might miss some encryptable data in dynamic schemas
		applyDefinitions(deltas, options, task, result);
		Utils.encrypt(deltas, protector, options, result);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("MODEL.executeChanges(\n  deltas:\n{}\n  options:{}", DebugUtil.debugDump(deltas, 2), options);
		}

		if (InternalsConfig.consistencyChecks) {
			OperationResultRunner.run(result, () -> {
				for (ObjectDelta<? extends ObjectType> delta : deltas) {
					delta.checkConsistence();
				}
			});
		}
		
		RepositoryCache.enter();

		try {
		
			if (ModelExecuteOptions.isRaw(options)) {
				// Go directly to repository
				AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.EXECUTE_CHANGES_RAW, AuditEventStage.REQUEST);
				auditRecord.addDeltas(ObjectDeltaOperation.cloneDeltaCollection(deltas));
				auditRecord.setTarget(Utils.determineAuditTarget(deltas));
				// we don't know auxiliary information (resource, objectName) at this moment -- so we do nothing
				auditService.audit(auditRecord, task);
				try {
					for (ObjectDelta<? extends ObjectType> delta : deltas) {
						OperationResult result1 = result.createSubresult(EXECUTE_CHANGE);

						// MID-2486
						if (delta.getObjectTypeClass() == ShadowType.class || delta.getObjectTypeClass() == ResourceType.class) {
							try {
								provisioning.applyDefinition(delta, task, result1);
							} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | RuntimeException e) {
								// we can tolerate this - if there's a real problem with definition, repo call below will fail
								LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't apply definition on shadow/resource raw-mode delta {} -- continuing the operation.", e, delta);
								result1.muteLastSubresultError();
							}
						}

						final boolean preAuthorized = ModelExecuteOptions.isPreAuthorized(options);
						PrismObject objectToDetermineDetailsForAudit = null;
						try {
							if (delta.isAdd()) {
								RepoAddOptions repoOptions = new RepoAddOptions();
								if (ModelExecuteOptions.isNoCrypt(options)) {
									repoOptions.setAllowUnencryptedValues(true);
								}
								if (ModelExecuteOptions.isOverwrite(options)) {
									repoOptions.setOverwrite(true);
								}
								PrismObject<? extends ObjectType> objectToAdd = delta.getObjectToAdd();
								if (!preAuthorized) {
									securityEnforcer.authorize(ModelAuthorizationAction.ADD.getUrl(), null, objectToAdd, null, null, null, result1);
								}
								String oid;
								try {
									oid = cacheRepositoryService.addObject(objectToAdd, repoOptions, result1);
									task.recordObjectActionExecuted(objectToAdd, null, oid, ChangeType.ADD, task.getChannel(), null);
								} catch (Throwable t) {
									task.recordObjectActionExecuted(objectToAdd, null, null, ChangeType.ADD, task.getChannel(), t);
									throw t;
								}
								delta.setOid(oid);
								objectToDetermineDetailsForAudit = objectToAdd;
							} else if (delta.isDelete()) {
								QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);  // MID-2218
								try {
									PrismObject<? extends ObjectType> existingObject = null;
									try {
										existingObject = cacheRepositoryService.getObject(delta.getObjectTypeClass(), delta.getOid(), null, result1);
										objectToDetermineDetailsForAudit = existingObject;
									} catch (Throwable t) {
										if (!securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)) {
											throw t;
										} else {
											// in case of administrator's request we continue - in order to allow deleting malformed (unreadable) objects
										}
									}
									if (!preAuthorized) {
										securityEnforcer.authorize(ModelAuthorizationAction.DELETE.getUrl(), null, existingObject, null, null, null, result1);
									}
									try {
										if (ObjectTypes.isClassManagedByProvisioning(delta.getObjectTypeClass())) {
											Utils.clearRequestee(task);
											provisioning.deleteObject(delta.getObjectTypeClass(), delta.getOid(),
													ProvisioningOperationOptions.createRaw(), null, task, result1);
										} else {
											cacheRepositoryService.deleteObject(delta.getObjectTypeClass(), delta.getOid(),
													result1);
										}
										task.recordObjectActionExecuted(objectToDetermineDetailsForAudit, delta.getObjectTypeClass(), delta.getOid(), ChangeType.DELETE, task.getChannel(), null);
									} catch (Throwable t) {
										task.recordObjectActionExecuted(objectToDetermineDetailsForAudit, delta.getObjectTypeClass(), delta.getOid(), ChangeType.DELETE, task.getChannel(), t);
										throw t;
									}
								} finally {
									QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
								}
							} else if (delta.isModify()) {
								QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);  // MID-2218
								try {
									PrismObject existingObject = cacheRepositoryService.getObject(delta.getObjectTypeClass(), delta.getOid(), null, result1);
									objectToDetermineDetailsForAudit = existingObject;
									if (!preAuthorized) {
										securityEnforcer.authorize(ModelAuthorizationAction.MODIFY.getUrl(), null, existingObject, delta, null, null, result1);
									}
									try {
										cacheRepositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(),
												delta.getModifications(), result1);
										task.recordObjectActionExecuted(existingObject, ChangeType.MODIFY, null);
									} catch (Throwable t) {
										task.recordObjectActionExecuted(existingObject, ChangeType.MODIFY, t);
										throw t;
									}
								} finally {
									QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
								}
								if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {    // treat filters that already exist in the object (case #2 above)
									reevaluateSearchFilters(delta.getObjectTypeClass(), delta.getOid(), task, result1);
								}
							} else {
								throw new IllegalArgumentException("Wrong delta type " + delta.getChangeType() + " in " + delta);
							}
						} catch (ObjectAlreadyExistsException | SchemaException | ObjectNotFoundException | ConfigurationException | CommunicationException | SecurityViolationException | RuntimeException e) {
							ModelUtils.recordFatalError(result1, e);
							throw e;
						} finally {		// to have a record with the failed delta as well
							result1.computeStatus();
							ObjectDeltaOperation<? extends ObjectType> odoToAudit = new ObjectDeltaOperation<>(delta, result1);
							if (objectToDetermineDetailsForAudit != null) {
								odoToAudit.setObjectName(objectToDetermineDetailsForAudit.getName());
								if (objectToDetermineDetailsForAudit.asObjectable() instanceof ShadowType) {
									ShadowType shadow = (ShadowType) objectToDetermineDetailsForAudit.asObjectable();
									odoToAudit.setResourceOid(ShadowUtil.getResourceOid(shadow));
									odoToAudit.setResourceName(ShadowUtil.getResourceName(shadow));
								}
							}
							executedDeltas.add(odoToAudit);
						}
					}
				} finally {
					cleanupOperationResult(result);
					auditRecord.setTimestamp(System.currentTimeMillis());
					auditRecord.setOutcome(result.getStatus());
					auditRecord.setEventStage(AuditEventStage.EXECUTION);
					auditRecord.getDeltas().clear();
					auditRecord.getDeltas().addAll(executedDeltas);
					auditService.audit(auditRecord, task);

					task.markObjectActionExecutedBoundary();
				}

			} else {

				try {
					LensContext<? extends ObjectType> context = contextFactory.createContext(deltas, options, task, result);

					if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {
						String m = "ReevaluateSearchFilters option is not fully supported for non-raw operations yet. Filters already present in the object will not be touched.";
						LOGGER.warn("{} Context = {}", m, context.debugDump());
						result.createSubresult(CLASS_NAME_WITH_DOT+"reevaluateSearchFilters").recordWarning(m);
					}

					context.setProgressListeners(statusListeners);
					// Note: Request authorization happens inside clockwork

					clockwork.run(context, task, result);

					// prepare return value
					if (context.getFocusContext() != null) {
						executedDeltas.addAll(context.getFocusContext().getExecutedDeltas());
					}
					for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
						executedDeltas.addAll(projectionContext.getExecutedDeltas());
					}
					
					if (context.hasExplosiveProjection()) {
						PrismObject<? extends ObjectType> focus = context.getFocusContext().getObjectAny();
						
						LOGGER.debug("Recomputing {} because there was explosive projection", focus);

						LensContext<? extends ObjectType> recomputeContext = contextFactory.createRecomputeContext(focus, options, task, result);
						recomputeContext.setDoReconciliationForAllProjections(true);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Recomputing {}, context:\n{}", focus, recomputeContext.debugDump());
						}
						clockwork.run(recomputeContext, task, result);
					}

					cleanupOperationResult(result);

				} catch (ObjectAlreadyExistsException|ObjectNotFoundException|SchemaException|ExpressionEvaluationException|
						CommunicationException|ConfigurationException|PolicyViolationException|SecurityViolationException|RuntimeException e) {
					ModelUtils.recordFatalError(result, e);
					throw e;
					
				} catch (PreconditionViolationException e) {
					ModelUtils.recordFatalError(result, e);
					// TODO: Temporary fix for 3.6.1
					// We do not want to propagate PreconditionViolationException to model API as that might break compatiblity
					// ... and we do not really need that in 3.6.1
					// TODO: expose PreconditionViolationException in 3.7
					throw new SystemException(e);
					
				} finally {
					task.markObjectActionExecutedBoundary();
				}
			}
			
			invalidateCaches(executedDeltas);

		} catch (RuntimeException e) {		// just for sure (TODO split this method into two: raw and non-raw case)
			ModelUtils.recordFatalError(result, e);
			throw e;
		} finally {
			RepositoryCache.exit();
		}
		
        return executedDeltas;
	}


	private void invalidateCaches(Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas) {
		if (executedDeltas == null) {
			return;
		}
		for (ObjectDeltaOperation<? extends ObjectType> executedDelta: executedDeltas) {
			ObjectDelta<? extends ObjectType> objectDelta = executedDelta.getObjectDelta();
			if (objectDelta != null) {
				if (objectDelta.getObjectTypeClass() == SystemConfigurationType.class) {
					systemObjectCache.invalidateCaches();
				}
			}
		}
	}

	protected void cleanupOperationResult(OperationResult result) {
		// Clockwork.run sets "in-progress" flag just at the root level
		// and result.computeStatus() would erase it.
		// So we deal with it in a special way, in order to preserve this information for the user.
		if (result.isInProgress()) {
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordInProgress();
            }
        } else {
            result.computeStatus();
        }

		result.cleanupResult();
	}

	private <T extends ObjectType> void reevaluateSearchFilters(Class<T> objectTypeClass, String oid, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		OperationResult result = parentResult.createSubresult(CLASS_NAME_WITH_DOT+"reevaluateSearchFilters");
		try {
			PrismObject<T> storedObject = cacheRepositoryService.getObject(objectTypeClass, oid, null, result);
			PrismObject<T> updatedObject = storedObject.clone();
			Utils.resolveReferences(updatedObject, cacheRepositoryService, false, true, EvaluationTimeType.IMPORT, true, prismContext, result);
			ObjectDelta<T> delta = storedObject.diff(updatedObject);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("reevaluateSearchFilters found delta: {}", delta.debugDump());
			}
			if (!delta.isEmpty()) {
				try {
					cacheRepositoryService.modifyObject(objectTypeClass, oid, delta.getModifications(), result);
					task.recordObjectActionExecuted(updatedObject, ChangeType.MODIFY, null);
				} catch (Throwable t) {
					task.recordObjectActionExecuted(updatedObject, ChangeType.MODIFY, t);
					throw t;
				}
			}
			result.recordSuccess();
		} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
			result.recordFatalError("Couldn't reevaluate search filters: "+e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public <F extends ObjectType> void recompute(Class<F> type, String oid, Task task, OperationResult parentResult) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();
		recompute(type, oid, options, task, parentResult);
	}
	
	@Override
	public <F extends ObjectType> void recompute(Class<F> type, String oid, ModelExecuteOptions options, Task task, OperationResult parentResult) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
			
		OperationResult result = parentResult.createMinorSubresult(RECOMPUTE);
		result.addParams(new String[] { "oid", "type" }, oid, type);
		
		RepositoryCache.enter();
		
		try {

            Utils.clearRequestee(task);
			PrismObject<F> focus = objectResolver.getObject(type, oid, null, task, result).asPrismContainer();
			
			LOGGER.debug("Recomputing {}", focus);

			LensContext<F> lensContext = contextFactory.createRecomputeContext(focus, options, task, result); 
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recomputing {}, context:\n{}", focus, lensContext.debugDump());
			}
			clockwork.run(lensContext, task, result);
			
			result.computeStatus();
			
			LOGGER.trace("Recomputing of {}: {}", focus, result.getStatus());
			
			result.cleanupResult();

		} catch (ExpressionEvaluationException | SchemaException | PolicyViolationException | ObjectNotFoundException | 
				ObjectAlreadyExistsException | CommunicationException | ConfigurationException | SecurityViolationException |
				RuntimeException | Error e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
			
		} catch (PreconditionViolationException e) {
			ModelUtils.recordFatalError(result, e);
			// TODO: Temporary fix for 3.6.1
			// We do not want to propagate PreconditionViolationException to model API as that might break compatiblity
			// ... and we do not really need that in 3.6.1
			// TODO: expose PreconditionViolationException in 3.7
			throw new SystemException(e);
			
		} finally {
			RepositoryCache.exit();
		}
	}

	private void applyDefinitions(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		for(ObjectDelta<? extends ObjectType> delta: deltas) {
			Class<? extends ObjectType> type = delta.getObjectTypeClass();
			if (delta.hasCompleteDefinition()) {
				continue;
			}
			if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type)) {
				try {
					provisioning.applyDefinition(delta, task, result);
				} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
					if (ModelExecuteOptions.isRaw(options)) {
						ModelUtils.recordPartialError(result, e);
						// just go on, this is raw, we need to continue even without complete schema
					} else {
						ModelUtils.recordFatalError(result, e);
						throw e;
					}
				}
			} else {
				PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(delta.getObjectTypeClass());
                if (objDef == null) {
                    throw new SchemaException("No definition for delta object type class: " + delta.getObjectTypeClass());
                }
                boolean tolerateNoDefinition = ModelExecuteOptions.isRaw(options);
				delta.applyDefinitionIfPresent(objDef, tolerateNoDefinition);
			}
		}
	}

	@Override
	public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		if (query != null) {
			ModelUtils.validatePaging(query.getPaging());
		}

		Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        ObjectTypes.ObjectManager searchProvider = ObjectTypes.getObjectManagerForClass(type);
        if (searchProvider == null || searchProvider == ObjectTypes.ObjectManager.MODEL || GetOperationOptions.isRaw(rootOptions)) {
            searchProvider = ObjectTypes.ObjectManager.REPOSITORY;
        }

		OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS);
		result.addParams(new String[] { "query", "paging", "searchProvider" },
                query, (query != null ? query.getPaging() : "undefined"), searchProvider);

		query = preProcessQuerySecurity(type, query);
		if (isFilterNone(query, result)) {
			return new SearchResultList<>(new ArrayList<>());
		}

		SearchResultList<PrismObject<T>> list;
		try {
			RepositoryCache.enter();
			logQuery(query);

			try {
                if (GetOperationOptions.isRaw(rootOptions)) {       // MID-2218
                    QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
                }
                switch (searchProvider) {
                    case REPOSITORY: list = cacheRepositoryService.searchObjects(type, query, options, result); break;
                    case PROVISIONING: list = provisioning.searchObjects(type, query, options, task, result); break;
                    case TASK_MANAGER:
						list = taskManager.searchObjects(type, query, options, result);
						if (workflowManager != null && TaskType.class.isAssignableFrom(type) && !GetOperationOptions.isRaw(rootOptions) && !GetOperationOptions.isNoFetch(rootOptions)) {
							workflowManager.augmentTaskObjectList(list, options, task, result);
						}
						break;
                    default: throw new AssertionError("Unexpected search provider: " + searchProvider);
                }
				result.computeStatus();
				result.cleanupResult();
			} catch (CommunicationException | ConfigurationException | SchemaException | SecurityViolationException | RuntimeException | ObjectNotFoundException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} finally {
                QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(result.dump(false));
				}
			}

			if (list == null) {
				list = new SearchResultList<>(new ArrayList<PrismObject<T>>());
			}

            for (PrismObject<T> object : list) {
                if (hookRegistry != null) {
                    for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                        hook.invoke(object, options, task, result);
                    }
                }
				resolve(object, options, task, result);
            }

			// postprocessing objects that weren't handled by their correct provider (e.g. searching for ObjectType, and retrieving tasks, resources, shadows)
			// currently only resources and shadows are handled in this way
			// TODO generalize this approach somehow (something like "postprocess" in task/provisioning interface)
			if (searchProvider == ObjectTypes.ObjectManager.REPOSITORY && !GetOperationOptions.isRaw(rootOptions)) {
				for (PrismObject<T> object : list) {
					if (object.asObjectable() instanceof ResourceType || object.asObjectable() instanceof ShadowType) {
						provisioning.applyDefinition(object, task, result);
					}
				}
			}
			// better to use cache here (MID-4059)
			schemaTransformer.applySchemasAndSecurityToObjects(list, rootOptions, null, task, result);

		} finally {
			RepositoryCache.exit();
		}

		return list;
	}

	private class ContainerOperationContext<T extends Containerable> {
		final boolean isCase;
		final boolean isWorkItem;
		final ObjectTypes.ObjectManager manager;
		final ObjectQuery refinedQuery;

		ContainerOperationContext(Class<T> type, ObjectQuery query) throws SchemaException, SecurityViolationException {
			isCase = AccessCertificationCaseType.class.equals(type);
			isWorkItem = WorkItemType.class.equals(type);

			if (!isCase && !isWorkItem) {
				throw new UnsupportedOperationException("searchContainers/countContainers methods are currently supported only for AccessCertificationCaseType and WorkItemType classes");
			}

			if (isCase) {
				refinedQuery  = preProcessSubobjectQuerySecurity(AccessCertificationCaseType.class, AccessCertificationCampaignType.class, query);
				manager = ObjectTypes.ObjectManager.REPOSITORY;
			} else if (isWorkItem) {
				refinedQuery = preProcessWorkItemSecurity(query);
				manager = ObjectTypes.ObjectManager.WORKFLOW;
			} else {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public <T extends Containerable> SearchResultList<T> searchContainers(
			Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> rawOptions,
			Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {

		Validate.notNull(type, "Container value type must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		if (query != null) {
			ModelUtils.validatePaging(query.getPaging());
		}

		final ContainerOperationContext<T> ctx = new ContainerOperationContext<>(type, query);

		Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
		final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		final OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS);
		result.addParams(new String[] { "type", "query", "paging" },
				type, query, (query != null ? query.getPaging() : "undefined"));

		query = ctx.refinedQuery;

		if (isFilterNone(query, result)) {
			return new SearchResultList<>(new ArrayList<>());
		}

		SearchResultList<T> list;
		try {
			RepositoryCache.enter();

			logQuery(query);

			try {
				if (GetOperationOptions.isRaw(rootOptions)) {       // MID-2218
					QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
				}
				switch (ctx.manager) {
					case REPOSITORY: list = cacheRepositoryService.searchContainers(type, query, options, result); break;
					case WORKFLOW: list = workflowManager.searchContainers(type, query, options, result); break;
					default: throw new IllegalStateException();
				}
				result.computeStatus();
				result.cleanupResult();
			} catch (SchemaException|RuntimeException e) {
				processSearchException(e, rootOptions, ctx.manager, result);
				throw e;
			} finally {
				QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(result.dump(false));
				}
			}

			if (list == null) {
				list = new SearchResultList<>(new ArrayList<>());
			}

			for (T object : list) {
				// TODO implement read hook, if necessary
				resolve(object, options, task, result);
			}
		} finally {
			RepositoryCache.exit();
		}

		if (ctx.isCase) {
			list = schemaTransformer.applySchemasAndSecurityToContainers(list, AccessCertificationCampaignType.class,
					AccessCertificationCampaignType.F_CASE, rootOptions, null, task, result);
		} else if (ctx.isWorkItem) {
			// TODO implement security post processing for WorkItems
		} else {
			throw new IllegalStateException();
		}

		return list;
	}

	@Override
	public <T extends Containerable> Integer countContainers(
			Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> rawOptions,
			Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException {

		Validate.notNull(type, "Container value type must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");

		final ContainerOperationContext<T> ctx = new ContainerOperationContext<>(type, query);

		final Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
		final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		final OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS);
		result.addParams(new String[] { "type", "query"}, type, query);

		query = ctx.refinedQuery;

		if (isFilterNone(query, result)) {
			return 0;
		}

		Integer count;
		try {
			RepositoryCache.enter();

			logQuery(query);

			try {
				switch (ctx.manager) {
					case REPOSITORY: count = cacheRepositoryService.countContainers(type, query, options, result); break;
					case WORKFLOW: count = workflowManager.countContainers(type, query, options, result); break;
					default: throw new IllegalStateException();
				}
				result.computeStatus();
				result.cleanupResult();
			} catch (SchemaException|RuntimeException e) {
				processSearchException(e, rootOptions, ctx.manager, result);
				throw e;
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(result.dump(false));
				}
			}
		} finally {
			RepositoryCache.exit();
		}

		return count;
	}

//	// TODO - fix this temporary implementation (perhaps by storing 'groups' in user context on logon)
//	// TODO: currently we check only the direct assignments, we need to implement more complex mechanism
//	public List<PrismReferenceValue> getGroupsForUser(UserType user) {
//		List<PrismReferenceValue> retval = new ArrayList<>();
//		for (AssignmentType assignmentType : user.getAssignmentNew()) {
//			ObjectReferenceType ref = assignmentType.getTargetRef();
//			if (ref != null) {
//				retval.add(ref.clone().asReferenceValue());
//			}
//		}
//		return retval;
//	}

	private ObjectQuery preProcessWorkItemSecurity(ObjectQuery query) throws SchemaException, SecurityViolationException {
		// TODO uncomment the following, after our "query interpreter" will be able to interpret OR-clauses
		return query;

//		if (securityEnforcer.isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl(), null, null, null, null, null)) {
//			return query;
//		}
//		ObjectFilter filter = query != null ? query.getFilter() : null;
//		UserType currentUser = securityEnforcer.getPrincipal().getUser();
//
//		ObjectFilter secFilter = QueryBuilder.queryFor(WorkItemType.class, getPrismContext())
//				.item(WorkItemType.F_CANDIDATE_ROLES_REF).ref(getGroupsForUser(currentUser))
//				.or().item(WorkItemType.F_ASSIGNEE_REF).ref(ObjectTypeUtil.createObjectRef(currentUser).asReferenceValue())
//				.buildFilter();
//
//		return updateObjectQuery(query,
//				filter != null ? AndFilter.createAnd(filter, secFilter) : secFilter);
	}

	protected boolean isFilterNone(ObjectQuery query, OperationResult result) {
		if (query != null && query.getFilter() != null && query.getFilter() instanceof NoneFilter) {
			LOGGER.trace("Security denied the search");
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Denied");
			return true;
		}
		return false;
	}

	protected void logQuery(ObjectQuery query) {
		if (query != null){
            if (query.getPaging() == null) {
                LOGGER.trace("Searching objects with null paging (query in TRACE).");
            } else {
                LOGGER.trace("Searching objects from {} to {} ordered {} by {} (query in TRACE).",
						query.getPaging().getOffset(), query.getPaging().getMaxSize(),
						query.getPaging().getDirection(), query.getPaging().getOrderBy());
            }
        }
	}

	@Override
	public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
			final ResultHandler<T> handler, final Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            final Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		if (query != null) {
			ModelUtils.validatePaging(query.getPaging());
		}

		final Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
		final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        ObjectTypes.ObjectManager searchProvider = ObjectTypes.getObjectManagerForClass(type);
        if (searchProvider == null || searchProvider == ObjectTypes.ObjectManager.MODEL || GetOperationOptions.isRaw(rootOptions)) {
            searchProvider = ObjectTypes.ObjectManager.REPOSITORY;
        }

		final OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS);
		result.addParams(new String[] { "query", "paging", "searchProvider" },
                query, (query != null ? query.getPaging() : "undefined"), searchProvider);
		
		query = preProcessQuerySecurity(type, query);
		if (isFilterNone(query, result)) {
			return null;
		}

		ResultHandler<T> internalHandler = (object, parentResult1) -> {
			try {
				object = object.cloneIfImmutable();
				if (hookRegistry != null) {
					for (ReadHook hook : hookRegistry.getAllReadHooks()) {
						hook.invoke(object, options, task, result);     // TODO result or parentResult??? [med]
					}
				}
				if (workflowManager != null && TaskType.class.isAssignableFrom(type) && !GetOperationOptions.isRaw(rootOptions) && !GetOperationOptions.isNoFetch(rootOptions)) {
					workflowManager.augmentTaskObject(object, options, task, result);
				}
				resolve(object, options, task, result);
				schemaTransformer.applySchemasAndSecurity(object, rootOptions, null, task, parentResult1);
			} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
					| CommunicationException | ConfigurationException ex) {
				parentResult1.recordFatalError(ex);
				throw new SystemException(ex.getMessage(), ex);
			}

			return handler.handle(object, parentResult1);
		};
        
		SearchResultMetadata metadata;
		try {
			RepositoryCache.enter();
			logQuery(query);

			try {
                switch (searchProvider) {
                    case REPOSITORY: metadata = cacheRepositoryService.searchObjectsIterative(type, query, internalHandler, options, false, result); break;		// TODO move strictSequential flag to model API in some form
                    case PROVISIONING: metadata = provisioning.searchObjectsIterative(type, query, options, internalHandler, task, result); break;
                    case TASK_MANAGER: metadata = taskManager.searchObjectsIterative(type, query, options, internalHandler, result); break;
                    default: throw new AssertionError("Unexpected search provider: " + searchProvider);
                }
				result.computeStatusIfUnknown();
				result.cleanupResult();
			} catch (CommunicationException | ConfigurationException | ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(result.dump(false));
				}
			}
		} finally {
			RepositoryCache.exit();
		}
		
		return metadata;
	}

	private void processSearchException(Throwable e, GetOperationOptions rootOptions,
			ObjectTypes.ObjectManager searchProvider, OperationResult result) {
		String message;
        switch (searchProvider) {
            case REPOSITORY: message = "Couldn't search objects in repository"; break;
            case PROVISIONING: message = "Couldn't search objects in provisioning"; break;
            case TASK_MANAGER: message = "Couldn't search objects in task manager"; break;
			case WORKFLOW: message = "Couldn't search objects in workflow engine"; break;
            default: message = "Couldn't search objects"; break;    // should not occur
        }
		LoggingUtils.logUnexpectedException(LOGGER, message, e);
		result.recordFatalError(message, e);
		result.cleanupResult(e);
	}

	@Override
	public <T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, CommunicationException, ExpressionEvaluationException {

		OperationResult result = parentResult.createMinorSubresult(COUNT_OBJECTS);
		result.addParams(new String[] { "query", "paging"},
                query, (query != null ? query.getPaging() : "undefined"));
		
		query = preProcessQuerySecurity(type, query);
		if (isFilterNone(query, result)) {
			return 0;
		}

		Integer count;
		try {
			RepositoryCache.enter();

			Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
			GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

            ObjectTypes.ObjectManager objectManager = ObjectTypes.getObjectManagerForClass(type);
            if (GetOperationOptions.isRaw(rootOptions) || objectManager == null || objectManager == ObjectTypes.ObjectManager.MODEL) {
                objectManager = ObjectTypes.ObjectManager.REPOSITORY;
            }
            switch (objectManager) {
                case PROVISIONING: count = provisioning.countObjects(type, query, options, task, parentResult); break;
                case REPOSITORY: count = cacheRepositoryService.countObjects(type, query, parentResult); break;
                case TASK_MANAGER: count = taskManager.countObjects(type, query, parentResult); break;
                default: throw new AssertionError("Unexpected objectManager: " + objectManager);
            }
		} catch (ConfigurationException | SecurityViolationException | SchemaException | ObjectNotFoundException | CommunicationException | ExpressionEvaluationException | RuntimeException | Error e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} finally {
			RepositoryCache.exit();
		}
		
		result.computeStatus();
		result.cleanupResult();
		return count;
        
	}
	
	@Override
	@Deprecated
	public PrismObject<UserType> findShadowOwner(String accountOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		RepositoryCache.enter();

		PrismObject<UserType> user;
		
		LOGGER.trace("Listing account shadow owner for account with oid {}.", new Object[]{accountOid});

		OperationResult result = parentResult.createSubresult(LIST_ACCOUNT_SHADOW_OWNER);
		result.addParams(new String[] { "accountOid" }, accountOid);

		try {
			
			user = cacheRepositoryService.listAccountShadowOwner(accountOid, result);
			result.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Account with oid {} doesn't exists", ex, accountOid);
			result.recordFatalError("Account with oid '" + accountOid + "' doesn't exists", ex);
			throw ex;
		} catch (RuntimeException | Error ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, accountOid);
			result.recordFatalError("Couldn't list account shadow owner for account with oid '"
					+ accountOid + "'.", ex);
			throw ex;
		} finally {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(result.dump(false));
			}
			RepositoryCache.exit();
			result.cleanupResult();
		}

		if (user != null) {
			try {
				user = user.cloneIfImmutable();
				schemaTransformer.applySchemasAndSecurity(user, null, null, task, result);
			} catch (SchemaException | SecurityViolationException | ConfigurationException
					| ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
						+ " for account with oid {}", ex, accountOid);
				result.recordFatalError("Couldn't list account shadow owner for account with oid '"
						+ accountOid + "'.", ex);
				throw ex;
			}
		}
		
		return user;
	}
	
	@Override
	public PrismObject<? extends FocusType> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException {
		Validate.notEmpty(shadowOid, "Account oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		RepositoryCache.enter();

		PrismObject<? extends FocusType> focus;

		LOGGER.trace("Listing account shadow owner for account with oid {}.", new Object[]{shadowOid});

		OperationResult result = parentResult.createSubresult(LIST_ACCOUNT_SHADOW_OWNER);
		result.addParams(new String[] { "accountOid" }, shadowOid);

		try {
			Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
			focus = cacheRepositoryService.searchShadowOwner(shadowOid, options, result);
			result.recordSuccess();
		} catch (RuntimeException | Error ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, shadowOid);
			result.recordFatalError("Couldn't list account shadow owner for account with oid '"
					+ shadowOid + "'.", ex);
			throw ex;
		} finally {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(result.dump(false));
			}
			RepositoryCache.exit();
			result.cleanupResult();
		}

		if (focus != null) {
			try {
				focus = focus.cloneIfImmutable();
				schemaTransformer.applySchemasAndSecurity(focus, null, null, task, result);
			} catch (SchemaException | SecurityViolationException | ConfigurationException
					| ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
						+ " for account with oid {}", ex, shadowOid);
				result.recordFatalError("Couldn't list account shadow owner for account with oid '"
						+ shadowOid + "'.", ex);
				throw ex;
			}
		}
		
		return focus;
	}

	@Deprecated
	@Override
	public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid,
			QName objectClass, ObjectPaging paging, Task task, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object type must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		RepositoryCache.enter();

		List<PrismObject<? extends ShadowType>> list;

		try {
			LOGGER.trace(
					"Listing resource objects {} from resource, oid {}, from {} to {} ordered {} by {}.",
					objectClass, resourceOid, paging.getOffset(), paging.getMaxSize(),
					paging.getOrderBy(), paging.getDirection());

			OperationResult result = parentResult.createSubresult(LIST_RESOURCE_OBJECTS);
			result.addParams(new String[] { "resourceOid", "objectType", "paging" }, resourceOid,
					objectClass, paging);

			try {

				list = provisioning.listResourceObjects(resourceOid, objectClass, paging, task, result);

			} catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ObjectNotFoundException | ExpressionEvaluationException | RuntimeException | Error ex) {
				ModelUtils.recordFatalError(result, ex);
				throw ex;
			}
			result.recordSuccess();
			result.cleanupResult();

			if (list == null) {
				list = new ArrayList<>();
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

		OperationResult testResult;
		try {
			testResult = provisioning.testResource(resourceOid, task);
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Error testing resource OID: {}: Object not found: {} ", resourceOid, ex.getMessage(), ex);
			RepositoryCache.exit();
			throw ex;
		} catch (SystemException ex) {
			LOGGER.error("Error testing resource OID: {}: {} ", resourceOid, ex.getMessage(), ex);
			RepositoryCache.exit();
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Error testing resource OID: {}: {} ", resourceOid, ex.getMessage(), ex);
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
	public void importFromResource(String resourceOid, QName objectClass, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(task, "Task must not be null.");
		RepositoryCache.enter();
		LOGGER.trace("Launching import from resource with oid {} for object class {}.", new Object[]{
                resourceOid, objectClass});

		OperationResult result = parentResult.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE);
        result.addParam("resourceOid", resourceOid);
        result.addParam("objectClass", objectClass);
        result.addArbitraryObjectAsParam("task", task);
		// TODO: add context to the result

		// Fetch resource definition from the repo/provisioning
		ResourceType resource;
		try {
			resource = getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();

			if (resource.getSynchronization() == null || resource.getSynchronization().getObjectSynchronization().isEmpty()) {
				OperationResult subresult = result.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE+".check");
				subresult.recordWarning("No synchronization settings in "+resource+", import will probably do nothing");
				LOGGER.warn("No synchronization settings in "+resource+", import will probably do nothing");
			} else {
				ObjectSynchronizationType syncType = resource.getSynchronization().getObjectSynchronization().iterator().next();
				if (syncType.isEnabled() != null && !syncType.isEnabled()) {
					OperationResult subresult = result.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE+".check");
					subresult.recordWarning("Synchronization is disabled for "+resource+", import will probably do nothing");
					LOGGER.warn("Synchronization is disabled for "+resource+", import will probably do nothing");
				}
			}
			
			result.recordStatus(OperationResultStatus.IN_PROGRESS, "Task running in background");

			importAccountsFromResourceTaskHandler.launch(resource, objectClass, task, result);

			// The launch should switch task to asynchronous. It is in/out, so no
			// other action is needed

			if (!task.isAsynchronous()) {
				result.recordSuccess();
			}
			
			result.cleanupResult();
		
		} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}
		
	}
	
	@Override
	public void importFromResource(String shadowOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException,
			ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Validate.notNull(shadowOid, "Shadow OID must not be null.");
		Validate.notNull(task, "Task must not be null.");
		RepositoryCache.enter();
		LOGGER.trace("Launching importing shadow {} from resource.", shadowOid);

		OperationResult result = parentResult.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE);
        result.addParam(OperationResult.PARAM_OID, shadowOid);
        result.addArbitraryObjectAsParam("task", task);
		// TODO: add context to the result

        try {
        	boolean wasOk = importAccountsFromResourceTaskHandler.importSingleShadow(shadowOid, task, result);
			
        	if (wasOk) {
        		result.recordSuccess();
        	} else {
        		// the error should be in the result already, compute should reveal that to the top-level
        		result.computeStatus();
        	}
			
			
			result.cleanupResult();
		
		} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException| RuntimeException | Error ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}
		
	}

	@Override
	public void importObjectsFromFile(File input, ImportOptionsType options, Task task,
			OperationResult parentResult) throws FileNotFoundException {
		OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_FILE);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(input);
		} catch (FileNotFoundException e) {
			IOUtils.closeQuietly(fis);
			String msg = "Error reading from file " + input + ": " + e.getMessage();
			result.recordFatalError(msg, e);
			throw e;
		}
		try {
			importObjectsFromStream(fis, options, task, parentResult);
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				LOGGER.error("Error closing file " + input + ": " + e.getMessage(), e);
			}
		}
		result.computeStatus();
	}

	@Override
	public void importObjectsFromStream(InputStream input, ImportOptionsType options, Task task,
			OperationResult parentResult) {
		RepositoryCache.enter();
		OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
		result.addParam("options", options);
		objectImporter.importObjects(input, options, task, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Import result:\n{}", result.debugDump());
		}
		// No need to compute status. The validator inside will do it.
		// result.computeStatus("Couldn't import object from input stream.");
		RepositoryCache.exit();
		result.cleanupResult();
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
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, Task task, OperationResult parentResult)
			throws CommunicationException, SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException {
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
		List<ConnectorType> connectorList = new ArrayList<>(discoverConnectors);
		schemaTransformer.applySchemasAndSecurityToObjectTypes(connectorList, null, null, task, result);
		result.computeStatus("Connector discovery failed");
		RepositoryCache.exit();
		result.cleanupResult();
		return new HashSet<>(connectorList);
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
		systemObjectCache.invalidateCaches(); // necessary for testing situations where we re-import different system configurations with the same version (on system init)        

		RepositoryCache.enter();
		OperationResult result = parentResult.createSubresult(POST_INIT);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ModelController.class);

		try {
			SecurityUtil.setRemoteHostAddressHeaders(ObjectTypeUtil.asObjectable(systemObjectCache.getSystemConfiguration(result)));
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't set 'forwardedFor' headers because system configuration couldn't be read", e);
		}

		securityEnforcer.setUserProfileService(userProfileService);
		// TODO: initialize repository

		// repository (including logging config) is initialized in its own method

        taskManager.postInit(result);

		// Initialize provisioning
		provisioning.postInit(result);

        if (result.isUnknown()) {
		    result.computeStatus();
        }

		RepositoryCache.exit();
		result.cleanupResult();
	}

	@Override
	public <T extends ObjectType> CompareResultType compareObject(PrismObject<T> provided,
			Collection<SelectorOptions<GetOperationOptions>> rawReadOptions, ModelCompareOptions compareOptions,
			@NotNull List<ItemPath> ignoreItems, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		Validate.notNull(provided, "Object must not be null or empty.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		OperationResult result = parentResult.createMinorSubresult(COMPARE_OBJECT);
		result.addParam("oid", provided.getOid());
		result.addParam("name", provided.getName());
		result.addCollectionOfSerializablesAsParam("readOptions", rawReadOptions);
		result.addParam("compareOptions", compareOptions);
		result.addCollectionOfSerializablesAsParam("ignoreItems", ignoreItems);

		Collection<SelectorOptions<GetOperationOptions>> readOptions = preProcessOptionsSecurity(rawReadOptions);

		CompareResultType rv = new CompareResultType();

		try {
			boolean c2p = ModelCompareOptions.isComputeCurrentToProvided(compareOptions);
			boolean p2c = ModelCompareOptions.isComputeProvidedToCurrent(compareOptions);
			boolean returnC = ModelCompareOptions.isReturnCurrent(compareOptions);
			boolean returnP = ModelCompareOptions.isReturnNormalized(compareOptions);
			boolean ignoreOperational = ModelCompareOptions.isIgnoreOperationalItems(compareOptions);

			if (!c2p && !p2c && !returnC && !returnP) {
				return rv;
			}
			PrismObject<T> current = null;
			if (c2p || p2c || returnC) {
				current = fetchCurrentObject(provided.getCompileTimeClass(), provided.getOid(), provided.getName(), readOptions, task, result);
				removeIgnoredItems(current, ignoreItems);
				if (ignoreOperational) {
					removeOperationalItems(current);
				}
			}
			removeIgnoredItems(provided, ignoreItems);
			if (ignoreOperational) {
				removeOperationalItems(provided);
			}

			if (c2p) {
				rv.setCurrentToProvided(DeltaConvertor.toObjectDeltaType(DiffUtil.diff(current, provided)));
			}
			if (p2c) {
				rv.setProvidedToCurrent(DeltaConvertor.toObjectDeltaType(DiffUtil.diff(provided, current)));
			}
			if (returnC && current != null) {
				rv.setCurrentObject(current.asObjectable());
			}
			if (returnP) {
				rv.setNormalizedObject(provided.asObjectable());
			}
		} finally {
			result.computeStatus();
			result.cleanupResult();
		}
		return rv;
	}

	private <T extends ObjectType> PrismObject<T> fetchCurrentObject(Class<T> type, String oid, PolyString name,
			Collection<SelectorOptions<GetOperationOptions>> readOptions, Task task,
			OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {

		if (readOptions == null) {
			readOptions = new ArrayList<>();
		}
		GetOperationOptions root = SelectorOptions.findRootOptions(readOptions);
		if (root == null) {
			readOptions.add(SelectorOptions.create(GetOperationOptions.createAllowNotFound()));
		} else {
			root.setAllowNotFound(true);
		}

		if (oid != null) {
			try {
				return getObject(type, oid, readOptions, task, result);
			} catch (ObjectNotFoundException e) {
				return null;
			}
		}
		if (name == null || name.getOrig() == null) {
			throw new IllegalArgumentException("Neither OID nor name of the object is known.");
		}
		ObjectQuery nameQuery = QueryBuilder.queryFor(type, prismContext)
				.item(ObjectType.F_NAME).eqPoly(name.getOrig())
				.build();
		List<PrismObject<T>> objects = searchObjects(type, nameQuery, readOptions, task, result);
		if (objects.isEmpty()) {
			return null;
		} else if (objects.size() == 1) {
			return objects.get(0);
		} else {
			throw new SchemaException("More than 1 object of type " + type + " with the name of " + name + ": There are " + objects.size() + " of them.");
		}
	}

	private <T extends ObjectType> void removeIgnoredItems(PrismObject<T> object, List<ItemPath> ignoreItems) {
		if (object == null) {
			return;
		}
		for (ItemPath path : ignoreItems) {
			Item item = object.findItem(path);		// reduce to "removeItem" after fixing that method implementation
			if (item != null) {
				object.removeItem(item.getPath(), Item.class);
			}
		}
	}

	// TODO write in cleaner way
	private <T extends ObjectType> void removeOperationalItems(PrismObject<T> object) {
		if (object == null) {
			return;
		}
		final List<ItemPath> operationalItems = new ArrayList<>();
		object.accept(visitable -> {
			if (visitable instanceof Item) {
				Item item = ((Item) visitable);
				if (item.getDefinition() != null && item.getDefinition().isOperational()) {
					operationalItems.add(item.getPath());
					// it would be nice if we could stop visiting children here but that's not possible now
				}
			}
		});
		LOGGER.trace("Operational items: {}", operationalItems);
		removeIgnoredItems(object, operationalItems);
	}

	private Collection<SelectorOptions<GetOperationOptions>> preProcessOptionsSecurity(Collection<SelectorOptions<GetOperationOptions>> options)
			throws SchemaException {
		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		if (GetOperationOptions.isAttachDiagData(rootOptions) &&
				!securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)) {
			Collection<SelectorOptions<GetOperationOptions>> reducedOptions = CloneUtil.cloneCollectionMembers(options);
			SelectorOptions.findRootOptions(reducedOptions).setAttachDiagData(false);
			return reducedOptions;
		} else {
			return options;
		}
	}

	private <O extends ObjectType> ObjectQuery preProcessQuerySecurity(Class<O> objectType, ObjectQuery origQuery) throws SchemaException {
    	ObjectFilter origFilter = null;
    	if (origQuery != null) {
    		origFilter = origQuery.getFilter();
    	}
		ObjectFilter secFilter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.READ.getUrl(), null, objectType, null, origFilter);
		return updateObjectQuery(origQuery, secFilter);
	}

	// we expect that objectType is a direct parent of containerType
	private <C extends Containerable, O extends ObjectType>
	ObjectQuery preProcessSubobjectQuerySecurity(Class<C> containerType, Class<O> objectType, ObjectQuery origQuery) throws SchemaException {
		ObjectFilter secParentFilter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.READ.getUrl(), null, objectType, null, null);
		if (secParentFilter == null || secParentFilter instanceof AllFilter) {
			return origQuery;				// no need to update the query
		}
		ObjectFilter secChildFilter;
		if (secParentFilter instanceof NoneFilter) {
			secChildFilter = NoneFilter.createNone();
		} else {
			ObjectFilter origChildFilter = origQuery != null ? origQuery.getFilter() : null;
			ObjectFilter secChildFilterParentPart = ExistsFilter.createExists(new ItemPath(PrismConstants.T_PARENT),
					containerType, prismContext, secParentFilter);
			if (origChildFilter == null) {
				secChildFilter = secChildFilterParentPart;
			} else {
				secChildFilter = AndFilter.createAnd(origChildFilter, secChildFilterParentPart);
			}
		}
		return updateObjectQuery(origQuery, secChildFilter);
	}

	private ObjectQuery updateObjectQuery(ObjectQuery origQuery, ObjectFilter updatedFilter) {
		if (origQuery != null) {
			origQuery.setFilter(updatedFilter);
			return origQuery;
		} else if (updatedFilter == null) {
			return null;
		} else {
			ObjectQuery objectQuery = new ObjectQuery();
			objectQuery.setFilter(updatedFilter);
			return objectQuery;
		}
	}
	
	//region Task-related operations

    @Override
    public boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeTaskCollectionOperation(ModelAuthorizationAction.SUSPEND_TASK, taskOids, parentResult);
        return taskManager.suspendTasks(taskOids, waitForStop, parentResult);
    }

	@Override
    public void suspendAndDeleteTasks(Collection<String> taskOids, long waitForStop, boolean alsoSubtasks, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeTaskCollectionOperation(ModelAuthorizationAction.DELETE, taskOids, parentResult);
        taskManager.suspendAndDeleteTasks(taskOids, waitForStop, alsoSubtasks, parentResult);
    }

    @Override
    public void resumeTasks(Collection<String> taskOids, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeTaskCollectionOperation(ModelAuthorizationAction.RESUME_TASK, taskOids, parentResult);
        taskManager.resumeTasks(taskOids, parentResult);
    }

    @Override
    public void scheduleTasksNow(Collection<String> taskOids, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeTaskCollectionOperation(ModelAuthorizationAction.RUN_TASK_IMMEDIATELY, taskOids, parentResult);
        taskManager.scheduleTasksNow(taskOids, parentResult);
    }

    @Override
    public PrismObject<TaskType> getTaskByIdentifier(String identifier, Collection<SelectorOptions<GetOperationOptions>> rawOptions, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
        PrismObject<TaskType> task = taskManager.getTaskTypeByIdentifier(identifier, options, parentResult);
		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		task = task.cloneIfImmutable();
		schemaTransformer.applySchemasAndSecurity(task, rootOptions, null, null, parentResult);
		return task;
    }

    @Override
    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) throws SchemaException, SecurityViolationException {
		securityEnforcer.authorize(ModelAuthorizationAction.STOP_SERVICE_THREADS.getUrl(), null, null, null, null, null, parentResult);
        return taskManager.deactivateServiceThreads(timeToWait, parentResult);
    }

    @Override
    public void reactivateServiceThreads(OperationResult parentResult) throws SchemaException, SecurityViolationException {
		securityEnforcer.authorize(ModelAuthorizationAction.START_SERVICE_THREADS.getUrl(), null, null, null, null, null, parentResult);
        taskManager.reactivateServiceThreads(parentResult);
    }

    @Override
    public boolean getServiceThreadsActivationState() {
        return taskManager.getServiceThreadsActivationState();
    }

    @Override
    public void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeNodeCollectionOperation(ModelAuthorizationAction.STOP_TASK_SCHEDULER, nodeIdentifiers, parentResult);
        taskManager.stopSchedulers(nodeIdentifiers, parentResult);
    }

    @Override
    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long waitTime, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeNodeCollectionOperation(ModelAuthorizationAction.STOP_TASK_SCHEDULER, nodeIdentifiers, parentResult);
        return taskManager.stopSchedulersAndTasks(nodeIdentifiers, waitTime, parentResult);
    }

    @Override
    public void startSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
		authorizeNodeCollectionOperation(ModelAuthorizationAction.START_TASK_SCHEDULER, nodeIdentifiers, parentResult);
        taskManager.startSchedulers(nodeIdentifiers, parentResult);
    }

    @Override
    public void synchronizeTasks(OperationResult parentResult) throws SchemaException, SecurityViolationException {
		securityEnforcer.authorize(ModelAuthorizationAction.SYNCHRONIZE_TASKS.getUrl(), null, null, null, null, null, parentResult);
		taskManager.synchronizeTasks(parentResult);
    }

	@Override
	public void synchronizeWorkflowRequests(OperationResult parentResult) throws SchemaException, SecurityViolationException {
		securityEnforcer.authorize(ModelAuthorizationAction.SYNCHRONIZE_WORKFLOW_REQUESTS.getUrl(), null, null, null, null, null, parentResult);
		workflowManager.synchronizeWorkflowRequests(parentResult);
	}

	@Override
    public List<String> getAllTaskCategories() {
        return taskManager.getAllTaskCategories();
    }

    @Override
    public String getHandlerUriForCategory(String category) {
        return taskManager.getHandlerUriForCategory(category);
    }

	private void authorizeTaskCollectionOperation(ModelAuthorizationAction action, Collection<String> oids, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
		if (securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)) {
			return;
		}
		for (String oid : oids) {
			PrismObject existingObject = cacheRepositoryService.getObject(TaskType.class, oid, null, parentResult);
			securityEnforcer.authorize(action.getUrl(), null, existingObject, null, null, null, parentResult);
		}
	}

	private void authorizeNodeCollectionOperation(ModelAuthorizationAction action, Collection<String> identifiers, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
		if (securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)) {
			return;
		}
		for (String identifier : identifiers) {
			PrismObject existingObject;
			ObjectQuery q = ObjectQueryUtil.createNameQuery(NodeType.class, prismContext, identifier);
			List<PrismObject<NodeType>> nodes = cacheRepositoryService.searchObjects(NodeType.class, q, null, parentResult);
			if (nodes.isEmpty()) {
				throw new ObjectNotFoundException("Node with identifier '" + identifier + "' couldn't be found.");
			} else if (nodes.size() > 1) {
				throw new SystemException("Multiple nodes with identifier '" + identifier + "'");
			}
			existingObject = nodes.get(0);
			securityEnforcer.authorize(action.getUrl(), null, existingObject, null, null, null, parentResult);
		}
	}

	//endregion

    //region Workflow-related operations
    @Override
    public void completeWorkItem(String workItemId, boolean decision, String comment, ObjectDelta additionalDelta,
			OperationResult parentResult)
			throws SecurityViolationException, SchemaException {
        getWorkflowManagerChecked().completeWorkItem(workItemId, decision, comment, additionalDelta, null, parentResult);
    }

    @Override
    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, SecurityViolationException {
		if (!securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)) {
			ObjectQuery query = QueryBuilder.queryFor(TaskType.class, prismContext)
					.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_PROCESS_INSTANCE_ID).eq(instanceId)
					.build();
			List<PrismObject<TaskType>> tasks = cacheRepositoryService.searchObjects(TaskType.class, query, GetOperationOptions.createRawCollection(), parentResult);
			if (tasks.size() > 1) {
				throw new IllegalStateException("More than one task for process instance ID " + instanceId);
			} else if (tasks.size() == 0) {
				throw new ObjectNotFoundException("No task for process instance ID " + instanceId, instanceId);
			}
			securityEnforcer.authorize(ModelAuthorizationAction.STOP_APPROVAL_PROCESS_INSTANCE.getUrl(), null, tasks.get(0), null, null, null, parentResult);
		}
        getWorkflowManagerChecked().stopProcessInstance(instanceId, username, parentResult);
    }

    @Override
    public void claimWorkItem(String workItemId, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException {
        getWorkflowManagerChecked().claimWorkItem(workItemId, parentResult);
    }

    @Override
    public void releaseWorkItem(String workItemId, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException {
        getWorkflowManagerChecked().releaseWorkItem(workItemId, parentResult);
    }

    @Override
    public void delegateWorkItem(String workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException, SchemaException {
        getWorkflowManagerChecked().delegateWorkItem(workItemId, delegates, method, parentResult);
    }

	@Override
	public void cleanupActivitiProcesses(OperationResult parentResult) throws SchemaException, SecurityViolationException {
		securityEnforcer.authorize(ModelAuthorizationAction.CLEANUP_PROCESS_INSTANCES.getUrl(), null, null, null, null, null, parentResult);
		getWorkflowManagerChecked().cleanupActivitiProcesses(parentResult);
	}

	//endregion

    //region Scripting (bulk actions)
	@Deprecated
    @Override
    public void evaluateExpressionInBackground(QName objectType, ObjectFilter filter, String actionName, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException {
        checkScriptingAuthorization(parentResult);
        scriptingExpressionEvaluator.evaluateExpressionInBackground(objectType, filter, actionName, task, parentResult);
    }

    @Override
    public void evaluateExpressionInBackground(ScriptingExpressionType expression, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException {
        checkScriptingAuthorization(parentResult);
        scriptingExpressionEvaluator.evaluateExpressionInBackground(expression, task, parentResult);
    }

    @Override
    public void evaluateExpressionInBackground(ExecuteScriptType executeScriptCommand, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException {
        checkScriptingAuthorization(parentResult);
        scriptingExpressionEvaluator.evaluateExpressionInBackground(executeScriptCommand, task, parentResult);
    }

    @Override
    public ScriptExecutionResult evaluateExpression(ScriptingExpressionType expression, Task task, OperationResult result) throws ScriptExecutionException, SchemaException, SecurityViolationException {
        checkScriptingAuthorization(result);
        ExecutionContext executionContext = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);
        return executionContext.toExecutionResult();
    }

    @Override
    public ScriptExecutionResult evaluateExpression(ExecuteScriptType scriptExecutionCommand, Task task, OperationResult result)
			throws ScriptExecutionException, SchemaException, SecurityViolationException {
        checkScriptingAuthorization(result);
        ExecutionContext executionContext = scriptingExpressionEvaluator.evaluateExpression(scriptExecutionCommand, task, result);
        return executionContext.toExecutionResult();
    }

    private void checkScriptingAuthorization(OperationResult parentResult) throws SchemaException, SecurityViolationException {
        securityEnforcer.authorize(ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl(), null, null, null, null, null, parentResult);
    }
	//endregion

	//region Certification

	@Override
	public AccessCertificationCasesStatisticsType getCampaignStatistics(String campaignOid, boolean currentStageOnly, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException {
		return getCertificationManagerChecked().getCampaignStatistics(campaignOid, currentStageOnly, task, parentResult);
	}

	@Override
	public void recordDecision(String campaignOid, long caseId, long workItemId, AccessCertificationResponseType response, String comment, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException {
		getCertificationManagerChecked().recordDecision(campaignOid, caseId, workItemId, response, comment, task, parentResult);
	}

	@Deprecated
	@Override
	public List<AccessCertificationWorkItemType> searchOpenWorkItems(ObjectQuery baseWorkItemsQuery, boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
		return getCertificationManagerChecked().searchOpenWorkItems(baseWorkItemsQuery, notDecidedOnly, options, task, parentResult);
	}

	@Deprecated
	@Override
	public int countOpenWorkItems(ObjectQuery baseWorkItemsQuery, boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions);
		return getCertificationManagerChecked().countOpenWorkItems(baseWorkItemsQuery, notDecidedOnly, options, task, parentResult);
	}

	@Override
	public void closeCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException {
		getCertificationManagerChecked().closeCampaign(campaignOid, task, result);
	}

	@Override
	public void startRemediation(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException {
		getCertificationManagerChecked().startRemediation(campaignOid, task, result);
	}

	@Override
	public void closeCurrentStage(String campaignOid, int stageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		getCertificationManagerChecked().closeCurrentStage(campaignOid, stageNumber, task, parentResult);
	}

	@Override
	public void openNextStage(String campaignOid, int stageNumber, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		getCertificationManagerChecked().openNextStage(campaignOid, stageNumber, task, parentResult);
	}

	@Override
	public AccessCertificationCampaignType createCampaign(String definitionOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		return getCertificationManagerChecked().createCampaign(definitionOid, task, parentResult);
	}
	//endregion

	@Override
	public <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> mergeObjects(Class<O> type, 
			String leftOid, String rightOid, String mergeConfigurationName, Task task, OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, PolicyViolationException, SecurityViolationException {
		
		OperationResult result = parentResult.createSubresult(MERGE_OBJECTS);
        result.addParam("leftOid", leftOid);
        result.addParam("rightOid", rightOid);
        result.addParam("class", type);
        
        RepositoryCache.enter();
        
        try {
			
			Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = 
					objectMerger.mergeObjects(type, leftOid, rightOid, mergeConfigurationName, task, result);
			
			result.computeStatus();
			return deltas;
			
		} catch (ObjectNotFoundException | SchemaException | ConfigurationException | ObjectAlreadyExistsException | ExpressionEvaluationException | CommunicationException | PolicyViolationException | SecurityViolationException | RuntimeException | Error e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
			RepositoryCache.exit();
		}
			
	}
}