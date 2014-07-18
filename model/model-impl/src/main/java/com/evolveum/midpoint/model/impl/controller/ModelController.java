/*
 * Copyright (c) 2010-2014 Evolveum
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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.model.api.hooks.ReadHook;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.LayerRefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.LayerRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.LayerRefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultRunner;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;

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
public class ModelController implements ModelService, ModelInteractionService, TaskService, WorkflowService, ScriptingService {

	// Constants for OperationResult
	public static final String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
	public static final String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
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

    @Autowired(required = false)                        // not required in all circumstances
    private WorkflowManager workflowManager;

    @Autowired
    private ScriptingExpressionEvaluator scriptingExpressionEvaluator;
	
	@Autowired(required = true)
	private ChangeExecutor changeExecutor;

	@Autowired(required = true)
	SystemConfigurationHandler systemConfigurationHandler;
	
	@Autowired(required = true)
	private AuditService auditService;
	
	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;
	
	@Autowired(required = true)
	private UserProfileService userProfileService;
	
	@Autowired(required = true)
	Projector projector;
	
	@Autowired(required = true)
	Protector protector;
	
	@Autowired(required = true)
	ModelDiagController modelDiagController;
	
	@Autowired(required = true)
	ContextFactory contextFactory;
	
	
	public ModelObjectResolver getObjectResolver() {
		return objectResolver;
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(clazz, "Object class must not be null.");
		RepositoryCache.enter();

		PrismObject<T> object = null;
		OperationResult result = parentResult.createMinorSubresult(GET_OBJECT);
        result.addParam("oid", oid);
        result.addCollectionOfSerializablesAsParam("options", options);
        result.addParam("class", clazz);

		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
				
		try {	

			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(oid);
			ref.setType(ObjectTypes.getObjectType(clazz).getTypeQName());
            Utils.clearRequestee(task);
            object = objectResolver.getObject(clazz, oid, options, task, result).asPrismObject();
            
			resolve(object, options, task, result);
            resolveNames(object, options, task, result);
		} catch (SchemaException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (CommunicationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ConfigurationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SecurityViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (RuntimeException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} finally {
			RepositoryCache.exit();
		}
		
		result.cleanupResult();
		
        postProcessObject(object, rootOptions, result);
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

    protected void resolveNames(PrismObject<?> object, Collection<SelectorOptions<GetOperationOptions>> options,
                           final Task task, final OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (object == null || options == null) {
            return;
        }

        // currently, only all-or-nothing names resolving is provided
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (!GetOperationOptions.isResolveNames(rootOptions)) {
            return;
        }

        final GetOperationOptions rootOptionsNoResolve = rootOptions.clone();
        rootOptionsNoResolve.setResolveNames(false);
        rootOptionsNoResolve.setResolve(false);
        rootOptionsNoResolve.setRaw(true);

        object.accept(new Visitor() {
            @Override
            public void visit(Visitable visitable) {
                if (visitable instanceof PrismReferenceValue) {
                    PrismReferenceValue refVal = (PrismReferenceValue) visitable;
                    PrismObject<?> refObject = refVal.getObject();
                    if (refObject == null) {
                        try {
                            // TODO what about security here?!
                            // TODO use some minimalistic get options (e.g. retrieve name only)
                            refObject = objectResolver.resolve(refVal, "", rootOptionsNoResolve, task, result);
                        } catch (ObjectNotFoundException e) {
                            // can be safely ignored
                            result.recordHandledError(e.getMessage());
                        }
                    }
                    String name;
                    if (refObject != null) {
                        name = PolyString.getOrig(refObject.asObjectable().getName());
                    } else {
                        name = "(object not found)";
                    }
                    if (StringUtils.isNotEmpty(name)) {
                        refVal.setUserData(XNodeSerializer.USER_DATA_KEY_COMMENT, " " + name + " ");
                    }
                }
            }
        });
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
				refObject = objectResolver.resolve(refVal, object.toString(), option.getOptions(), task, result);
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
	public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(final Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
			Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException {

        Collection<ObjectDeltaOperation<? extends ObjectType>> retval = new ArrayList<>();

		OperationResult result = parentResult.createSubresult(EXECUTE_CHANGES);
		result.addParam(OperationResult.PARAM_OPTIONS, options);
		
		if (ModelExecuteOptions.isIsImport(options)){
			for (ObjectDelta<? extends ObjectType> delta : deltas){
				if (delta.isAdd()){
					Utils.resolveReferences(delta.getObjectToAdd(), cacheRepositoryService, false, prismContext, result);
				}
			}
		}
		// Make sure everything is encrypted as needed before logging anything.
		// But before that we need to make sure that we have proper definition, otherwise we
		// might miss some encryptable data in dynamic schemas
		applyDefinitions(deltas, options, result);
		Utils.encrypt(deltas, protector, options, result);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("MODEL.executeChanges(\n  deltas:\n{}\n  options:{}", DebugUtil.debugDump(deltas, 2), options);
		}
		
		OperationResultRunner.run(result, new Runnable() {
			@Override
			public void run() {
				for(ObjectDelta<? extends ObjectType> delta: deltas) {
					delta.checkConsistence();
				}
			}
		});
		
		RepositoryCache.enter();

		try {
		
			if (ModelExecuteOptions.isRaw(options)) {
				// Go directly to repository
				AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.EXECUTE_CHANGES_RAW, AuditEventStage.REQUEST);
				auditRecord.addDeltas(ObjectDeltaOperation.cloneDeltaCollection(deltas));
				auditService.audit(auditRecord, task);
				for(ObjectDelta<? extends ObjectType> delta: deltas) {
                    OperationResult result1 = result.createSubresult(EXECUTE_CHANGE);
                    try {
                        if (delta.isAdd()) {
                            RepoAddOptions repoOptions = new RepoAddOptions();
                            if (ModelExecuteOptions.isNoCrypt(options)) {
                                repoOptions.setAllowUnencryptedValues(true);
                            }
                            if (ModelExecuteOptions.isOverwrite(options)) {
                                repoOptions.setOverwrite(true);
                            }
                            securityEnforcer.authorize(ModelAuthorizationAction.ADD.getUrl(), null, delta.getObjectToAdd(), null, null, null, result1);
                            String oid = cacheRepositoryService.addObject(delta.getObjectToAdd(), repoOptions, result1);
                            delta.setOid(oid);
                        } else if (delta.isDelete()) {
                            PrismObject<? extends ObjectType> existingObject = cacheRepositoryService.getObject(delta.getObjectTypeClass(), delta.getOid(), null, result1);
                            securityEnforcer.authorize(ModelAuthorizationAction.DELETE.getUrl(), null, existingObject, null, null, null, result1);
                            if (ObjectTypes.isClassManagedByProvisioning(delta.getObjectTypeClass())) {
                                Utils.clearRequestee(task);
                                provisioning.deleteObject(delta.getObjectTypeClass(), delta.getOid(),
                                        ProvisioningOperationOptions.createRaw(), null, task, result1);
                            } else {
                                cacheRepositoryService.deleteObject(delta.getObjectTypeClass(), delta.getOid(),
                                        result1);
                            }
                        } else if (delta.isModify()) {
                            PrismObject existingObject = cacheRepositoryService.getObject(delta.getObjectTypeClass(), delta.getOid(), null, result1);
                            securityEnforcer.authorize(ModelAuthorizationAction.MODIFY.getUrl(), null, existingObject, delta, null, null, result1);
                            cacheRepositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(),
                                    delta.getModifications(), result1);
                        } else {
                            throw new IllegalArgumentException("Wrong delta type "+delta.getChangeType()+" in "+delta);
                        }
                    } catch (ObjectAlreadyExistsException|SchemaException|ObjectNotFoundException|ConfigurationException|CommunicationException|SecurityViolationException|RuntimeException e) {
                        ModelUtils.recordFatalError(result1, e);
                        throw e;
                    }
                    result1.computeStatus();
                    retval.add(new ObjectDeltaOperation<>(delta, result1));
				}
				auditRecord.setTimestamp(null);
				auditRecord.setOutcome(OperationResultStatus.SUCCESS);
				auditRecord.setEventStage(AuditEventStage.EXECUTION);
				auditService.audit(auditRecord, task);
				
			} else {				
				
				LensContext<? extends ObjectType> context = contextFactory.createContext(deltas, options, task, result);
				// Note: Request authorization happens inside clockwork
				clockwork.run(context, task, result);

                // prepare return value
                if (context.getFocusContext() != null) {
                    retval.addAll(context.getFocusContext().getExecutedDeltas());
                }
                for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                    retval.addAll(projectionContext.getExecutedDeltas());
                }
			}
			
			result.computeStatus();

            if (result.isInProgress()) {       // todo fix this hack (computeStatus does not take the root-level status into account, but clockwork.run sets "in-progress" flag just at the root level)
                if (result.isSuccess()) {
                    result.recordInProgress();
                }
            }
            
            result.cleanupResult();
			
		} catch (ObjectAlreadyExistsException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SchemaException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ExpressionEvaluationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (CommunicationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ConfigurationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (PolicyViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SecurityViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (RuntimeException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} finally {
			RepositoryCache.exit();
		}
        return retval;
	}
	
	@Override
	public <F extends ObjectType> void recompute(Class<F> type, String oid, Task task, OperationResult parentResult) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
			
		OperationResult result = parentResult.createMinorSubresult(RECOMPUTE);
		result.addParams(new String[] { "oid", "type" }, oid, type);
		
		RepositoryCache.enter();
		
		try {

            Utils.clearRequestee(task);
			PrismObject<F> focus = objectResolver.getObject(type, oid, null, task, result).asPrismContainer();
			
			LOGGER.trace("Recomputing {}", focus);

			LensContext<F> syncContext = contextFactory.createRecomputeContext(focus, task, result); 
			LOGGER.trace("Recomputing {}, context:\n{}", focus, syncContext.debugDump());
			clockwork.run(syncContext, task, result);
			
			result.computeStatus();
			
			LOGGER.trace("Recomputing of {}: {}", focus, result.getStatus());
			
			result.cleanupResult();
			
		} catch (ExpressionEvaluationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SchemaException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (PolicyViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (CommunicationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ConfigurationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SecurityViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (RuntimeException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} finally {
			RepositoryCache.exit();
		}
	}

	private void applyDefinitions(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
			OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		for(ObjectDelta<? extends ObjectType> delta: deltas) {
			Class<? extends ObjectType> type = delta.getObjectTypeClass();
			if (delta.hasCompleteDefinition()) {
				continue;
			}
			if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type)) {
				try {
					provisioning.applyDefinition(delta, result);
				} catch (SchemaException e) {
					if (ModelExecuteOptions.isRaw(options)) {
						ModelUtils.recordPartialError(result, e);
						// just go on, this is raw, we need to continue even without complete schema
					} else {
						ModelUtils.recordFatalError(result, e);
						throw e;
					}
				} catch (ObjectNotFoundException e) {
					if (ModelExecuteOptions.isRaw(options)) {
						ModelUtils.recordPartialError(result, e);
						// just go on, this is raw, we need to continue even without complete schema
					} else {
						ModelUtils.recordFatalError(result, e);
						throw e;
					}
				} catch (CommunicationException e) {
					if (ModelExecuteOptions.isRaw(options)) {
						ModelUtils.recordPartialError(result, e);
						// just go on, this is raw, we need to continue even without complete schema
					} else {
						ModelUtils.recordFatalError(result, e);
						throw e;
					}
				} catch (ConfigurationException e) {
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
				delta.applyDefinition(objDef);
			}
		}
	}

//	private void encrypt(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
//			OperationResult result) {
//		// Encrypt values even before we log anything. We want to avoid showing unencrypted values in the logfiles
//		if (!ModelExecuteOptions.isNoCrypt(options)) {
//			for(ObjectDelta<? extends ObjectType> delta: deltas) {				
//				try {
//					CryptoUtil.encryptValues(protector, delta);
//				} catch (EncryptionException e) {
//					result.recordFatalError(e);
//					throw new SystemException(e.getMessage(), e);
//				}
//			}
//		}
//	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelInteractionService#previewChanges(com.evolveum.midpoint.prism.delta.ObjectDelta, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends ObjectType> ModelContext<F> previewChanges(
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
		LensContext<F> context = null;
		
		try {
			
			//used cloned deltas instead of origin deltas, because some of the values should be lost later..
			context = contextFactory.createContext(clonedDeltas, options, task, result);
//			context.setOptions(options);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.trace("Preview changes context:\n{}", context.debugDump());
			}
		
			
			projector.project(context, "preview", task, result);
			context.distributeResource();
			
		} catch (ConfigurationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SecurityViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (CommunicationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SchemaException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ExpressionEvaluationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (PolicyViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (RuntimeException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes output:\n{}", context.debugDump());
		}
		
		result.computeStatus();
		result.cleanupResult();

		return context;
	}
	
	@Override
	public <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object, AuthorizationPhaseType phase) throws SchemaException {
		PrismObjectDefinition<O> origDefinition = object.getDefinition();
		// TODO: maybe we need to expose owner resolver in the interface?
		ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(object, null);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security constrains for {}:\n{}", object, securityConstraints==null?"null":securityConstraints.debugDump());
		}
		if (securityConstraints == null) {
			return null;
		}
		PrismObjectDefinition<O> finalDefinition = applySecurityContraints(origDefinition, new ItemPath(), securityConstraints,
				securityConstraints.getActionDecision(ModelAuthorizationAction.READ.getUrl(), phase),
				securityConstraints.getActionDecision(ModelAuthorizationAction.ADD.getUrl(), phase),
				securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase), phase);
		return finalDefinition;
	}
	
	private <D extends ItemDefinition> D applySecurityContraints(D origItemDefinition, ItemPath itemPath, ObjectSecurityConstraints securityConstraints,
			AuthorizationDecisionType defaultReadDecition, AuthorizationDecisionType defaultAddDecition, AuthorizationDecisionType defaultModifyDecition,
            AuthorizationPhaseType phase) {
		D itemDefinition = (D) origItemDefinition.clone();
		// We need to make a super-deep clone here. Even make sure that the complex types inside are cloned.
		// Otherwise permissions from one part of the definition tree may be incorrectly propagated to another part
		if (itemDefinition instanceof PrismContainerDefinition<?>) {
			PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>)itemDefinition;
			ComplexTypeDefinition origCtd = containerDefinition.getComplexTypeDefinition();
			containerDefinition.setComplexTypeDefinition(origCtd.clone());
		}
		AuthorizationDecisionType readDecision = computeItemDecision(securityConstraints, itemPath, ModelAuthorizationAction.READ.getUrl(), defaultReadDecition, phase);
		AuthorizationDecisionType addDecision = computeItemDecision(securityConstraints, itemPath, ModelAuthorizationAction.ADD.getUrl(), defaultAddDecition, phase);
		AuthorizationDecisionType modifyDecision = computeItemDecision(securityConstraints, itemPath, ModelAuthorizationAction.MODIFY.getUrl(), defaultModifyDecition, phase);
//		LOGGER.trace("Decision for {}: {}", itemPath, readDecision);
		if (readDecision != AuthorizationDecisionType.ALLOW) {
			itemDefinition.setCanRead(false);
		}
		if (addDecision != AuthorizationDecisionType.ALLOW) {
			itemDefinition.setCanAdd(false);
		}
		if (modifyDecision != AuthorizationDecisionType.ALLOW) {
			itemDefinition.setCanModify(false);
		}
		
		if (itemDefinition instanceof PrismContainerDefinition<?>) {
			PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>)itemDefinition;
			// The items are still shallow-clonned, we need to deep-clone them
			List<? extends ItemDefinition> origSubDefinitions = ((PrismContainerDefinition<?>)origItemDefinition).getDefinitions();
			containerDefinition.getDefinitions().clear();
			for (ItemDefinition subDef: origSubDefinitions) {
                // TODO fix this brutal hack - it is necessary to avoid endless recursion in the style of "Decision for authorization/object/owner/owner/......../owner/special: ALLOW"
                // it's too late to come up with a serious solution
                if (!(itemPath.lastNamed() != null && ObjectSpecificationType.F_OWNER.equals(itemPath.lastNamed().getName()) && ObjectSpecificationType.F_OWNER.equals(subDef.getName()))) {
				    ItemDefinition newDef = applySecurityContraints(subDef, new ItemPath(itemPath, subDef.getName()), securityConstraints,
						    readDecision, addDecision, modifyDecision, phase);
				    containerDefinition.getComplexTypeDefinition().add(newDef);
                }
			}
		}
		return itemDefinition;
	}
		
    private AuthorizationDecisionType computeItemDecision(ObjectSecurityConstraints securityConstraints, ItemPath itemPath, String actionUrl,
			AuthorizationDecisionType defaultDecision, AuthorizationPhaseType phase) {
    	AuthorizationDecisionType explicitDecision = securityConstraints.findItemDecision(itemPath, actionUrl, phase);
//    	LOGGER.trace("Explicit decision for {}: {}", itemPath, explicitDecision);
    	if (explicitDecision != null) {
    		return explicitDecision;
    	} else {
    		return defaultDecision;
    	}
	}
    
    @Override
	public RefinedObjectClassDefinition getEditObjectClassDefinition(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, AuthorizationPhaseType phase)
			throws SchemaException {
    	// TODO: maybe we need to expose owner resolver in the interface?
		ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(shadow, null);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security constrains for {}:\n{}", shadow, securityConstraints==null?"null":securityConstraints.debugDump());
		}
		if (securityConstraints == null) {
			return null;
		}
    	
    	RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
    	ShadowType shadowType = shadow.asObjectable();
    	ShadowKindType kind = shadowType.getKind();
    	String intent = shadowType.getIntent();
        RefinedObjectClassDefinition rocd;
    	if (kind != null) {
    		rocd = refinedSchema.getRefinedDefinition(kind, intent);
    	} else {
    		QName objectClassName = shadowType.getObjectClass();
    		if (objectClassName == null) {
    			// No data. Fall back to the default
    			rocd = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
    		} else {
    			rocd = refinedSchema.getRefinedDefinition(objectClassName);
    		}
    	}
        LayerRefinedObjectClassDefinition layeredROCD = rocd.forLayer(LayerType.PRESENTATION);

    	ItemPath attributesPath = new ItemPath(ShadowType.F_ATTRIBUTES);
		AuthorizationDecisionType attributesReadDecision = computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.READ.getUrl(), 
    			securityConstraints.getActionDecision(ModelAuthorizationAction.READ.getUrl(), phase), phase);
		AuthorizationDecisionType attributesAddDecision = computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.ADD.getUrl(),
				securityConstraints.getActionDecision(ModelAuthorizationAction.ADD.getUrl(), phase), phase);
		AuthorizationDecisionType attributesModifyDecision = computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.MODIFY.getUrl(),
				securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase), phase);
		LOGGER.trace("Attributes container access read:{}, add:{}, modify:{}", new Object[]{attributesReadDecision, attributesAddDecision, attributesModifyDecision});

        /*
         *  We are going to modify attribute definitions list.
         *  So let's make a (shallow) clone here, although it is probably not strictly necessary.
         */
        layeredROCD = layeredROCD.clone();
        for (LayerRefinedAttributeDefinition rAttrDef: layeredROCD.getAttributeDefinitions()) {
			ItemPath attributePath = new ItemPath(ShadowType.F_ATTRIBUTES, rAttrDef.getName());
			AuthorizationDecisionType attributeReadDecision = computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.READ.getUrl(), attributesReadDecision, phase);
			AuthorizationDecisionType attributeAddDecision = computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.ADD.getUrl(), attributesAddDecision, phase);
			AuthorizationDecisionType attributeModifyDecision = computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.MODIFY.getUrl(), attributesModifyDecision, phase);
			LOGGER.trace("Attribute {} access read:{}, add:{}, modify:{}", new Object[]{rAttrDef.getName(), attributeReadDecision, attributeAddDecision, attributeModifyDecision});
			if (attributeReadDecision != AuthorizationDecisionType.ALLOW) {
				rAttrDef.setOverrideCanRead(false);
			}
			if (attributeAddDecision != AuthorizationDecisionType.ALLOW) {
				rAttrDef.setOverrideCanAdd(false);
			}
			if (attributeModifyDecision != AuthorizationDecisionType.ALLOW) {
				rAttrDef.setOverrideCanModify(false);
			}
		}

        // TODO what about activation and credentials?
    	
    	return layeredROCD;
	}

	@Override
	public Collection<? extends DisplayableValue<String>> getActionUrls() {
		return Arrays.asList(ModelAuthorizationAction.values());
	}


	private PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("System configuration version read from repo: " + config.getVersion());
        }
        return config;
    }

	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		if (query != null) {
			ModelUtils.validatePaging(query.getPaging());
		}
		RepositoryCache.enter();

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        ObjectTypes.ObjectManager searchProvider = ObjectTypes.getObjectManagerForClass(type);
        if (searchProvider == null || searchProvider == ObjectTypes.ObjectManager.MODEL || GetOperationOptions.isRaw(rootOptions)) {
            searchProvider = ObjectTypes.ObjectManager.REPOSITORY;
        }

		OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS);
		result.addParams(new String[] { "query", "paging", "searchProvider" },
                query, (query != null ? query.getPaging() : "undefined"), searchProvider);
		
		query = preProcessQuerySecurity(type, query);
		if (query != null && query.getFilter() != null && query.getFilter() instanceof NoneFilter) {
			LOGGER.trace("Security denied the search");
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Denied");
			RepositoryCache.exit();
			return new ArrayList<>();
		}
		
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
			
			try {
                switch (searchProvider) {
                    case REPOSITORY: list = cacheRepositoryService.searchObjects(type, query, options, result); break;
                    case PROVISIONING: list = provisioning.searchObjects(type, query, options, result); break;
                    case TASK_MANAGER: list = taskManager.searchObjects(type, query, options, result); break;
                    case WORKFLOW: throw new UnsupportedOperationException();
                    default: throw new AssertionError("Unexpected search provider: " + searchProvider);
                }
				result.recordSuccess();
				result.cleanupResult();
			} catch (CommunicationException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (ConfigurationException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (ObjectNotFoundException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (SchemaException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (SecurityViolationException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (RuntimeException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} finally {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(result.dump(false));
				}
			}

			if (list == null) {
				list = new ArrayList<PrismObject<T>>();
			}

            for (PrismObject<T> object : list) {
                if (hookRegistry != null) {
                    for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                        hook.invoke(object, options, task, result);
                    }
                }
                // TODO enable when necessary
                //resolveNames(object, options, task, result);
            }

		} finally {
			RepositoryCache.exit();
		}
		
		postProcessObjects(list, rootOptions, result);

		return list;
	}
	
	@Override
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query,
			final ResultHandler<T> handler, final Collection<SelectorOptions<GetOperationOptions>> options,
            final Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		Validate.notNull(type, "Object type must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		if (query != null) {
			ModelUtils.validatePaging(query.getPaging());
		}
		RepositoryCache.enter();

        final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        ObjectTypes.ObjectManager searchProvider = ObjectTypes.getObjectManagerForClass(type);
        if (searchProvider == null || searchProvider == ObjectTypes.ObjectManager.MODEL || GetOperationOptions.isRaw(rootOptions)) {
            searchProvider = ObjectTypes.ObjectManager.REPOSITORY;
        }

		final OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS);
		result.addParams(new String[] { "query", "paging", "searchProvider" },
                query, (query != null ? query.getPaging() : "undefined"), searchProvider);
		
		query = preProcessQuerySecurity(type, query);
		if (query != null && query.getFilter() != null && query.getFilter() instanceof NoneFilter) {
			LOGGER.trace("Security denied the search");
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Denied");
			RepositoryCache.exit();
			return;
		}
		
        ResultHandler<T> internalHandler = new ResultHandler<T>() {

            @Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
                try {
                    if (hookRegistry != null) {
                        for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                            hook.invoke(object, options, task, result);     // TODO result or parentResult??? [med]
                        }
                    }
                    // TODO enable when necessary
                    //resolveNames(object, options, task, parentResult);
                    postProcessObject(object, rootOptions, parentResult);
                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException
                        | CommunicationException | ConfigurationException ex) {
                    parentResult.recordFatalError(ex);
                    throw new SystemException(ex.getMessage(), ex);
                }

				return handler.handle(object, parentResult);
			}
		};
        
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
			
			try {
                switch (searchProvider) {
                    case REPOSITORY: cacheRepositoryService.searchObjectsIterative(type, query, internalHandler, options, result); break;
                    case PROVISIONING: provisioning.searchObjectsIterative(type, query, options, internalHandler, result); break;
                    case TASK_MANAGER: throw new UnsupportedOperationException("searchIterative in task manager is currently not supported");
                    case WORKFLOW: throw new UnsupportedOperationException("searchIterative in task manager is currently not supported");
                    default: throw new AssertionError("Unexpected search provider: " + searchProvider);
                }
				result.computeStatusIfUnknown();
				result.cleanupResult();
			} catch (CommunicationException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (ConfigurationException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (ObjectNotFoundException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (SchemaException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (SecurityViolationException e) {
				processSearchException(e, rootOptions, searchProvider, result);
				throw e;
			} catch (RuntimeException e) {
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
	}

	private void processSearchException(Exception e, GetOperationOptions rootOptions,
			ObjectTypes.ObjectManager searchProvider, OperationResult result) {
		String message;
        switch (searchProvider) {
            case REPOSITORY: message = "Couldn't search objects in repository"; break;
            case PROVISIONING: message = "Couldn't search objects in provisioning"; break;
            case TASK_MANAGER: message = "Couldn't search objects in task manager"; break;
            case WORKFLOW: message = "Couldn't search objects in workflow module"; break;
            default: message = "Couldn't search objects"; break;    // should not occur
        }
		LoggingUtils.logException(LOGGER, message, e);
		result.recordFatalError(message, e);
		result.cleanupResult(e);
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, CommunicationException {

		RepositoryCache.enter();
		
		OperationResult result = parentResult.createMinorSubresult(COUNT_OBJECTS);
		result.addParams(new String[] { "query", "paging"},
                query, (query != null ? query.getPaging() : "undefined"));
		
		query = preProcessQuerySecurity(type, query);
		if (query != null && query.getFilter() != null && query.getFilter() instanceof NoneFilter) {
			LOGGER.trace("Security denied the search");
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Denied");
			RepositoryCache.exit();
			return 0;
		}

		int count;
		try {
			GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

            ObjectTypes.ObjectManager objectManager = ObjectTypes.getObjectManagerForClass(type);
            if (GetOperationOptions.isRaw(rootOptions) || objectManager == null || objectManager == ObjectTypes.ObjectManager.MODEL) {
                objectManager = ObjectTypes.ObjectManager.REPOSITORY;
            }
            switch (objectManager) {
                case PROVISIONING: count = provisioning.countObjects(type, query, parentResult); break;
                case REPOSITORY: count = cacheRepositoryService.countObjects(type, query, parentResult); break;
                case TASK_MANAGER: count = taskManager.countObjects(type, query, parentResult); break;
                default: throw new AssertionError("Unexpected objectManager: " + objectManager);
            }
		} catch (ConfigurationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SecurityViolationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (SchemaException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (ObjectNotFoundException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (CommunicationException e) {
			ModelUtils.recordFatalError(result, e);
			throw e;
		} catch (RuntimeException e) {
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
	public PrismObject<UserType> findShadowOwner(String accountOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		RepositoryCache.enter();

		PrismObject<UserType> user = null;
		
		LOGGER.trace("Listing account shadow owner for account with oid {}.", new Object[] { accountOid });

		OperationResult result = parentResult.createSubresult(LIST_ACCOUNT_SHADOW_OWNER);
		result.addParams(new String[] { "accountOid" }, accountOid);

		try {
			
			user = cacheRepositoryService.listAccountShadowOwner(accountOid, result);
			result.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Account with oid {} doesn't exists", ex, accountOid);
			result.recordFatalError("Account with oid '" + accountOid + "' doesn't exists", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, accountOid);
			result.recordFatalError("Couldn't list account shadow owner for account with oid '"
					+ accountOid + "'.", ex);
		} finally {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(result.dump(false));
			}
			RepositoryCache.exit();
			result.cleanupResult();
		}

		if (user != null) {
			postProcessObject(user, null, result);
		}
		
		return user;
	}

	@Override
	public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid,
			QName objectClass, ObjectPaging paging, Task task, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object type must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		RepositoryCache.enter();

		List<PrismObject<? extends ShadowType>> list = null;

		try {
			LOGGER.trace(
					"Listing resource objects {} from resource, oid {}, from {} to {} ordered {} by {}.",
					new Object[] { objectClass, resourceOid, paging.getOffset(), paging.getMaxSize(),
							paging.getOrderBy(), paging.getDirection() });

			OperationResult result = parentResult.createSubresult(LIST_RESOURCE_OBJECTS);
			result.addParams(new String[] { "resourceOid", "objectType", "paging" }, resourceOid,
					objectClass, paging);

			try {

				list = provisioning.listResourceObjects(resourceOid, objectClass, paging, result);

			} catch (SchemaException ex) {
				ModelUtils.recordFatalError(result, ex);
				throw ex;
			} catch (CommunicationException ex) {
				ModelUtils.recordFatalError(result, ex);
				throw ex;
			} catch (ConfigurationException ex) {
				ModelUtils.recordFatalError(result, ex);
				throw ex;
			} catch (ObjectNotFoundException ex) {
				ModelUtils.recordFatalError(result, ex);
				throw ex;
			}
			result.recordSuccess();
			result.cleanupResult();

			if (list == null) {
				list = new ArrayList<PrismObject<? extends ShadowType>>();
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
	public void importFromResource(String resourceOid, QName objectClass, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
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
		ResourceType resource = null;
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
		
		} catch (ObjectNotFoundException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (CommunicationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ConfigurationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (RuntimeException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}
		
	}
	
	@Override
	public void importFromResource(String shadowOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
			ConfigurationException, SecurityViolationException {
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
		
		} catch (ObjectNotFoundException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (CommunicationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ConfigurationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (RuntimeException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} finally {
			RepositoryCache.exit();
		}
		
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
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
			throws CommunicationException, SecurityViolationException, SchemaException {
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
		postProcessObjectTypes(discoverConnectors, null, result);
		result.computeStatus("Connector discovery failed");
		RepositoryCache.exit();
		result.cleanupResult();
		return discoverConnectors;
	}
	
	private <T extends ObjectType> void postProcessObjectTypes(Collection<T> objectTypes, GetOperationOptions options, OperationResult result) throws SecurityViolationException, SchemaException {
		for (T objectType: objectTypes) {
			postProcessObject(objectType.asPrismObject(), options, result);
		}
	}
	
	private <T extends ObjectType> void postProcessObjects(Collection<PrismObject<T>> objects, GetOperationOptions options, OperationResult result) throws SecurityViolationException, SchemaException {
		for (PrismObject<T> object: objects) {
			postProcessObject(object, options, result);
		}
	}
	
	/**
	 * Validate the objects, remove any non-visible properties (security) and so on. This method is called for
	 * any object that is returned from the Model Service.  
	 */
	private <T extends ObjectType> void postProcessObject(PrismObject<T> object, GetOperationOptions options, OperationResult result) throws SecurityViolationException, SchemaException {
		validateObject(object, options, result);
		try {
			ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(object, null);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Security constrains for {}:\n{}", object, securityConstraints==null?"null":securityConstraints.debugDump());
			}
			if (securityConstraints == null) {
				throw new SecurityViolationException("Access denied");
			}
			AuthorizationDecisionType globalDecision = securityConstraints.getActionDecision(ModelAuthorizationAction.READ.getUrl(), null);
			if (globalDecision == AuthorizationDecisionType.DENY) {
				// shortcut
				throw new SecurityViolationException("Access denied");
			}
			if (globalDecision == AuthorizationDecisionType.ALLOW && securityConstraints.hasNoItemDecisions()) {
				// shortcut, nothing to do
			} else {
				removeDeniedItems((List)object.getValue().getItems(), securityConstraints, globalDecision);
				if (object.isEmpty()) {
					// let's make it explicit
					throw new SecurityViolationException("Access denied");
				}
			}
			
		} catch (SecurityViolationException | SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}
	}
	
	private void removeDeniedItems(List<Item<? extends PrismValue>> items, ObjectSecurityConstraints securityContraints, AuthorizationDecisionType defaultDecision) {
		Iterator<Item<?>> iterator = items.iterator();
		while (iterator.hasNext()) {
			Item<? extends PrismValue> item = iterator.next();
			ItemPath itemPath = item.getPath();
			AuthorizationDecisionType itemDecision = securityContraints.findItemDecision(itemPath, ModelAuthorizationAction.READ.getUrl(), null);
			if (item instanceof PrismContainer<?>) {
				if (itemDecision == AuthorizationDecisionType.DENY) {
					// Explicitly denied access to the entire container
					iterator.remove();
				} else {
					// No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
					AuthorizationDecisionType subDefaultDecision = defaultDecision;
					if (itemDecision == AuthorizationDecisionType.ALLOW) {
						// This means allow to all subitems unless otherwise denied.
						subDefaultDecision = AuthorizationDecisionType.ALLOW;
					}
					List<? extends PrismContainerValue<?>> values = ((PrismContainer<?>)item).getValues();
					Iterator<? extends PrismContainerValue<?>> vi = values.iterator();
					while (vi.hasNext()) {
						PrismContainerValue<?> cval = vi.next();
						List<Item<?>> subitems = cval.getItems();
						removeDeniedItems(subitems, securityContraints, subDefaultDecision);
						if (cval.getItems().isEmpty()) {
							vi.remove();
						}
					}
					if (item.isEmpty()) {
						iterator.remove();
					}
				}
			} else {
				if (itemDecision == AuthorizationDecisionType.DENY || (itemDecision == null && defaultDecision == null)) {
					iterator.remove();
				}
			}
		}
	}
	
	private <T extends ObjectType> void validateObject(PrismObject<T> object, GetOperationOptions options, OperationResult result) {
		try {
			if (InternalsConfig.readEncryptionChecks) {
				CryptoUtil.checkEncrypted(object);
			}
			if (!InternalsConfig.consistencyChecks) {
				return;
			}
			Class<T> type = object.getCompileTimeClass();
			boolean tolerateRaw = false;
			if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type) || type == ReportType.class) {
				// We tolarate raw values for resource and shadows in case the user has requested so
				tolerateRaw = options.isRaw(options);
				if (hasError(object, result)) {
					// If there is an error then the object might not be complete.
					// E.g. we do not have a complete dynamic schema to apply to the object
					// Tolerate some raw meat in that case.
					tolerateRaw = true;
				}
			}
			object.checkConsistence(true, !tolerateRaw);
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}
	}
	
	private <T extends ObjectType> boolean hasError(PrismObject<T> object, OperationResult result) {
		if (result != null && result.isError()) {
			return true;
		}
		OperationResultType fetchResult = object.asObjectable().getFetchResult();
		if (fetchResult != null && 
				(fetchResult.getStatus() == OperationResultStatusType.FATAL_ERROR ||
				fetchResult.getStatus() == OperationResultStatusType.PARTIAL_ERROR)) {
			return true;
		}
		return false;
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

		securityEnforcer.setUserProfileService(userProfileService);
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
		result.cleanupResult();
	}

    @Override
    public <F extends ObjectType> ModelContext<F> unwrapModelContext(LensContextType wrappedContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        return LensContext.fromLensContextType(wrappedContext, prismContext, provisioning, result);
    }
    
    private <O extends ObjectType> ObjectQuery preProcessQuerySecurity(Class<O> objectType, ObjectQuery origQuery) throws SchemaException {
    	ObjectFilter origFilter = null;
    	if (origQuery != null) {
    		origFilter = origQuery.getFilter();
    	}
		ObjectFilter secFilter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.READ.getUrl(), null, objectType, origFilter);
		if (origQuery != null) {
			origQuery.setFilter(secFilter);
			return origQuery;
		} else if (secFilter == null) {
			return null;
		} else {
			ObjectQuery objectQuery = new ObjectQuery();
			objectQuery.setFilter(secFilter);
			return objectQuery;
		}
	}

    //region Task-related operations

    @Override
    public boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult parentResult) {
        return taskManager.suspendTasks(taskOids, waitForStop, parentResult);
    }

    @Override
    public void suspendAndDeleteTasks(Collection<String> taskOids, long waitForStop, boolean alsoSubtasks, OperationResult parentResult) {
        taskManager.suspendAndDeleteTasks(taskOids, waitForStop, alsoSubtasks, parentResult);
    }

    @Override
    public void resumeTasks(Collection<String> taskOids, OperationResult parentResult) {
        taskManager.resumeTasks(taskOids, parentResult);
    }

    @Override
    public void scheduleTasksNow(Collection<String> taskOids, OperationResult parentResult) {
        taskManager.scheduleTasksNow(taskOids, parentResult);
    }

    @Override
    public PrismObject<TaskType> getTaskByIdentifier(String identifier, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        return taskManager.getTaskTypeByIdentifier(identifier, options, parentResult);
    }

    @Override
    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) {
        return taskManager.deactivateServiceThreads(timeToWait, parentResult);
    }

    @Override
    public void reactivateServiceThreads(OperationResult parentResult) {
        taskManager.reactivateServiceThreads(parentResult);
    }

    @Override
    public boolean getServiceThreadsActivationState() {
        return taskManager.getServiceThreadsActivationState();
    }

    @Override
    public void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
        taskManager.stopSchedulers(nodeIdentifiers, parentResult);
    }

    @Override
    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long waitTime, OperationResult parentResult) {
        return taskManager.stopSchedulersAndTasks(nodeIdentifiers, waitTime, parentResult);
    }

    @Override
    public void startSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
        taskManager.startSchedulers(nodeIdentifiers, parentResult);
    }

    @Override
    public void synchronizeTasks(OperationResult parentResult) {
        taskManager.synchronizeTasks(parentResult);
    }

    @Override
    public List<String> getAllTaskCategories() {
        return taskManager.getAllTaskCategories();
    }

    @Override
    public String getHandlerUriForCategory(String category) {
        return taskManager.getHandlerUriForCategory(category);
    }
    //endregion

    //region Workflow-related operations
    @Override
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        return workflowManager.countWorkItemsRelatedToUser(userOid, assigned, parentResult);
    }

    @Override
    public List<WorkItemType> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        return workflowManager.listWorkItemsRelatedToUser(userOid, assigned, first, count, parentResult);
    }

    @Override
    public WorkItemType getWorkItemDetailsById(String workItemId, OperationResult parentResult) throws ObjectNotFoundException {
        return workflowManager.getWorkItemDetailsById(workItemId, parentResult);
    }

    @Override
    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) {
        return workflowManager.countProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished, parentResult);
    }

    @Override
    public List<WfProcessInstanceType> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) {
        return workflowManager.listProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished, first, count, parentResult);
    }

    @Override
    public WfProcessInstanceType getProcessInstanceByWorkItemId(String workItemId, OperationResult parentResult) throws ObjectNotFoundException {
        return workflowManager.getProcessInstanceByWorkItemId(workItemId, parentResult);
    }

    @Override
    public WfProcessInstanceType getProcessInstanceById(String instanceId, boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException {
        return workflowManager.getProcessInstanceById(instanceId, historic, getWorkItems, parentResult);
    }

    @Override
    public void approveOrRejectWorkItem(String workItemId, boolean decision, OperationResult parentResult) {
        workflowManager.approveOrRejectWorkItem(workItemId, decision, parentResult);
    }

    @Override
    public void approveOrRejectWorkItemWithDetails(String workItemId, PrismObject specific, boolean decision, OperationResult result) {
        workflowManager.approveOrRejectWorkItemWithDetails(workItemId, specific, decision, result);
    }

    @Override
    public void completeWorkItemWithDetails(String workItemId, PrismObject specific, String decision, OperationResult parentResult) {
        workflowManager.completeWorkItemWithDetails(workItemId, specific, decision, parentResult);
    }

    @Override
    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) {
        workflowManager.stopProcessInstance(instanceId, username, parentResult);
    }

    @Override
    public void deleteProcessInstance(String instanceId, OperationResult parentResult) {
        workflowManager.deleteProcessInstance(instanceId, parentResult);
    }

    @Override
    public void claimWorkItem(String workItemId, OperationResult parentResult) {
        workflowManager.claimWorkItem(workItemId, parentResult);
    }

    @Override
    public void releaseWorkItem(String workItemId, OperationResult parentResult) {
        workflowManager.releaseWorkItem(workItemId, parentResult);
    }
    //endregion

    //region Scripting (bulka actions)
    @Override
    public void evaluateExpressionInBackground(QName objectType, ObjectFilter filter, String actionName, Task task, OperationResult parentResult) throws SchemaException {
        scriptingExpressionEvaluator.evaluateExpressionInBackground(objectType, filter, actionName, task, parentResult);
    }

    @Override
    public void evaluateExpressionInBackground(JAXBElement<? extends ScriptingExpressionType> expression, Task task, OperationResult parentResult) throws SchemaException {
        scriptingExpressionEvaluator.evaluateExpressionInBackground(expression, task, parentResult);
    }
    //endregion

}