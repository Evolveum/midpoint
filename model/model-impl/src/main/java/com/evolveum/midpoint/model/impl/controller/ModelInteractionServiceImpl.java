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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.CompositeRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.LayerRefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.LayerRefinedAttributeDefinitionImpl;
import com.evolveum.midpoint.common.refinery.LayerRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.api.util.MergeDeltas;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.stringpolicy.AbstractValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ObjectValuePolicyEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ShadowValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.StringPolicyUtils;
import com.evolveum.midpoint.model.common.stringpolicy.UserValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensContextPlaceholder;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.PasswordPolicyEvaluator;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.RawTypeUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.CacheRegistry;
import com.evolveum.midpoint.repo.common.Cacheable;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetResponseType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemTargetType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsResetPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
@Component("modelInteractionService")
public class ModelInteractionServiceImpl implements ModelInteractionService {

	private static final Trace LOGGER = TraceManager.getTrace(ModelInteractionServiceImpl.class);

	@Autowired private ContextFactory contextFactory;
	@Autowired private Projector projector;
	@Autowired private SecurityEnforcer securityEnforcer;
	@Autowired private SecurityContextManager securityContextManager;
	@Autowired private SchemaTransformer schemaTransformer;
	@Autowired private ProvisioningService provisioning;
	@Autowired private ModelObjectResolver objectResolver;
	@Autowired private ObjectMerger objectMerger;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	@Autowired private SystemObjectCache systemObjectCache;
	@Autowired private ValuePolicyProcessor policyProcessor;
	@Autowired private Protector protector;
	@Autowired private PrismContext prismContext;
	@Autowired private Visualizer visualizer;
	@Autowired private ModelService modelService;
	@Autowired private ModelCrudService modelCrudService;
	@Autowired private SecurityHelper securityHelper;
	@Autowired private MappingFactory mappingFactory;
	@Autowired private MappingEvaluator mappingEvaluator;
	@Autowired private ActivationComputer activationComputer;
	@Autowired private Clock clock;
	@Autowired private HookRegistry hookRegistry;
	@Autowired UserProfileService userProfileService;
	@Autowired private ExpressionFactory expressionFactory;
	@Autowired private CacheRegistry cacheRegistry;
	@Autowired private OperationalDataManager metadataManager;

	private static final String OPERATION_GENERATE_VALUE = ModelInteractionService.class.getName() +  ".generateValue";
	private static final String OPERATION_VALIDATE_VALUE = ModelInteractionService.class.getName() +  ".validateValue";

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelInteractionService#previewChanges(com.evolveum.midpoint.prism.delta.ObjectDelta, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends ObjectType> ModelContext<F> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		return previewChanges(deltas, options, task, Collections.emptyList(), parentResult);
	}

	@Override
	public <F extends ObjectType> ModelContext<F> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task,
			Collection<ProgressListener> listeners, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes input:\n{}", DebugUtil.debugDump(deltas));
		}
		int size = 0;
		if (deltas != null) {
			size = deltas.size();
		}
		Collection<ObjectDelta<? extends ObjectType>> clonedDeltas = new ArrayList<>(size);
		if (deltas != null) {
			for (ObjectDelta delta : deltas){
				clonedDeltas.add(delta.clone());
			}
		}

		OperationResult result = parentResult.createSubresult(PREVIEW_CHANGES);
		LensContext<F> context;

		try {
			RepositoryCache.enter();
			//used cloned deltas instead of origin deltas, because some of the values should be lost later..
			context = contextFactory.createContext(clonedDeltas, options, task, result);
//			context.setOptions(options);
			LOGGER.trace("Preview changes context:\n{}", context.debugDumpLazily());
			context.setProgressListeners(listeners);

			projector.projectAllWaves(context, "preview", task, result);
			context.distributeResource();

			if (hookRegistry != null) {
				for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
					hook.invokePreview(context, task, result);
				}
			}

		} catch (ConfigurationException | SecurityViolationException | ObjectNotFoundException | SchemaException |
				CommunicationException | PolicyViolationException | RuntimeException | ObjectAlreadyExistsException |
				ExpressionEvaluationException e) {
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

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Preview changes output:\n{}", context.debugDump());
		}

		result.computeStatus();
		result.cleanupResult();

		return context;
	}

    @Override
    public <F extends ObjectType> ModelContext<F> unwrapModelContext(LensContextType wrappedContext, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        return LensContext.fromLensContextType(wrappedContext, prismContext, provisioning, task, result);
    }

	@Override
	public <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object, AuthorizationPhaseType phase, Task task, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(GET_EDIT_OBJECT_DEFINITION);
		PrismObjectDefinition<O> objectDefinition = object.getDefinition().deepClone(true, schemaTransformer::setFullAccessFlags );

		try {
		
			PrismObject<O> baseObject = object;
			if (object.getOid() != null) {
				// Re-read the object from the repository to make sure we have all the properties.
				// the object from method parameters may be already processed by the security code
				// and properties needed to evaluate authorizations may not be there
				// MID-3126, see also MID-3435
				baseObject = cacheRepositoryService.getObject(object.getCompileTimeClass(), object.getOid(), null, result);
			}
	
			// TODO: maybe we need to expose owner resolver in the interface?
			ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(baseObject, null, task, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Security constrains for {}:\n{}", object, securityConstraints==null?"null":securityConstraints.debugDump());
			}
			if (securityConstraints == null) {
				// Nothing allowed => everything denied
				result.setStatus(OperationResultStatus.NOT_APPLICABLE);
				return null;
			}
	
			ObjectTemplateType objectTemplateType = null;
			try {
				objectTemplateType = schemaTransformer.determineObjectTemplate(object, phase, result);
			} catch (ConfigurationException | ObjectNotFoundException e) {
				result.recordFatalError(e);
			}
			schemaTransformer.applyObjectTemplateToDefinition(objectDefinition, objectTemplateType, result);
	
			schemaTransformer.applySecurityConstraints(objectDefinition, securityConstraints, phase);
	
			if (object.canRepresent(ShadowType.class)) {
				PrismObject<ShadowType> shadow = (PrismObject<ShadowType>)object;
				String resourceOid = ShadowUtil.getResourceOid(shadow);
				if (resourceOid != null) {
					Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
					PrismObject<ResourceType> resource;
					try {
						resource = provisioning.getObject(ResourceType.class, resourceOid, options, task, result);
					} catch (CommunicationException | SecurityViolationException | ExpressionEvaluationException e) {
						throw new ConfigurationException(e.getMessage(), e);
					}
					RefinedObjectClassDefinition refinedObjectClassDefinition = getEditObjectClassDefinition(shadow, resource, phase, task, result);
					if (refinedObjectClassDefinition != null) {
						((ComplexTypeDefinitionImpl) objectDefinition.getComplexTypeDefinition()).replaceDefinition(ShadowType.F_ATTRIBUTES,
							refinedObjectClassDefinition.toResourceAttributeContainerDefinition());
					}
				}
			}
	
			result.computeStatus();
			return objectDefinition;
			
		} catch (ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}

	}

	@Override
	public PrismObjectDefinition<ShadowType> getEditShadowDefinition(ResourceShadowDiscriminator discr, AuthorizationPhaseType phase, Task task, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		// HACK hack hack
		// Make a dummy shadow instance here and evaluate the schema for that. It is not 100% correct. But good enough for now.
		// TODO: refactor when we add better support for multi-tenancy

		PrismObject<ShadowType> shadow = prismContext.createObject(ShadowType.class);
		ShadowType shadowType = shadow.asObjectable();
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		if (discr != null) {
			resourceRef.setOid(discr.getResourceOid());
			shadowType.setResourceRef(resourceRef);
			shadowType.setKind(discr.getKind());
			shadowType.setIntent(discr.getIntent());
			shadowType.setObjectClass(discr.getObjectClass());
		}

		return getEditObjectDefinition(shadow, phase, task, parentResult);
	}

    @Override
	public RefinedObjectClassDefinition getEditObjectClassDefinition(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, AuthorizationPhaseType phase, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	Validate.notNull(resource, "Resource must not be null");

    	RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
    	CompositeRefinedObjectClassDefinition rocd = refinedSchema.determineCompositeObjectClassDefinition(shadow);
    	if (rocd == null) {
    		LOGGER.debug("No object class definition for shadow {}, returning null");
    		return null;
    	}
        LayerRefinedObjectClassDefinition layeredROCD = rocd.forLayer(LayerType.PRESENTATION);

    	// TODO: maybe we need to expose owner resolver in the interface?
		ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(shadow, null, task, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security constrains for {}:\n{}", shadow, securityConstraints==null?"null":securityConstraints.debugDump());
		}
		if (securityConstraints == null) {
			return null;
		}

    	ItemPath attributesPath = SchemaConstants.PATH_ATTRIBUTES;
		AuthorizationDecisionType attributesReadDecision = schemaTransformer.computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.READ.getUrl(),
    			securityConstraints.getActionDecision(ModelAuthorizationAction.READ.getUrl(), phase), phase);
		AuthorizationDecisionType attributesAddDecision = schemaTransformer.computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.ADD.getUrl(),
				securityConstraints.getActionDecision(ModelAuthorizationAction.ADD.getUrl(), phase), phase);
		AuthorizationDecisionType attributesModifyDecision = schemaTransformer.computeItemDecision(securityConstraints, attributesPath, ModelAuthorizationAction.MODIFY.getUrl(),
				securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase), phase);
		LOGGER.trace("Attributes container access read:{}, add:{}, modify:{}", attributesReadDecision, attributesAddDecision,
				attributesModifyDecision);

        /*
         *  We are going to modify attribute definitions list.
         *  So let's make a (shallow) clone here, although it is probably not strictly necessary.
         */
        layeredROCD = layeredROCD.clone();
        for (LayerRefinedAttributeDefinition rAttrDef: layeredROCD.getAttributeDefinitions()) {
			ItemPath attributePath = new ItemPath(ShadowType.F_ATTRIBUTES, rAttrDef.getName());
			AuthorizationDecisionType attributeReadDecision = schemaTransformer.computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.READ.getUrl(), attributesReadDecision, phase);
			AuthorizationDecisionType attributeAddDecision = schemaTransformer.computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.ADD.getUrl(), attributesAddDecision, phase);
			AuthorizationDecisionType attributeModifyDecision = schemaTransformer.computeItemDecision(securityConstraints, attributePath, ModelAuthorizationAction.MODIFY.getUrl(), attributesModifyDecision, phase);
			LOGGER.trace("Attribute {} access read:{}, add:{}, modify:{}", rAttrDef.getName(), attributeReadDecision,
					attributeAddDecision, attributeModifyDecision);
			if (attributeReadDecision != AuthorizationDecisionType.ALLOW) {
				((LayerRefinedAttributeDefinitionImpl) rAttrDef).setOverrideCanRead(false);
			}
			if (attributeAddDecision != AuthorizationDecisionType.ALLOW) {
				((LayerRefinedAttributeDefinitionImpl) rAttrDef).setOverrideCanAdd(false);
			}
			if (attributeModifyDecision != AuthorizationDecisionType.ALLOW) {
				((LayerRefinedAttributeDefinitionImpl) rAttrDef).setOverrideCanModify(false);
			}
		}

        // TODO what about activation and credentials?

    	return layeredROCD;
	}

	public <O extends ObjectType,R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(PrismObject<O> object, PrismObject<R> target, Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException  {
		return securityEnforcer.getAllowedRequestAssignmentItems(securityContextManager.getPrincipal(), ModelAuthorizationAction.ASSIGN.getUrl(), object, target, null, task, result);
	}

	@Override
	public Collection<? extends DisplayableValue<String>> getActionUrls() {
		return Arrays.asList(ModelAuthorizationAction.values());
	}

	@Override
	public <F extends FocusType> RoleSelectionSpecification getAssignableRoleSpecification(PrismObject<F> focus, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(GET_ASSIGNABLE_ROLE_SPECIFICATION);

		RoleSelectionSpecification spec = new RoleSelectionSpecification();

		ObjectSecurityConstraints securityConstraints;
		try {
			securityConstraints = securityEnforcer.compileSecurityConstraints(focus, null, task, result);
		} catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | CommunicationException | SecurityViolationException e) {
			result.recordFatalError(e);
			throw e;
		}
		if (securityConstraints == null) {
			return null;
		}
		AuthorizationDecisionType decision = securityConstraints.findItemDecision(SchemaConstants.PATH_ASSIGNMENT,
				ModelAuthorizationAction.MODIFY.getUrl(), AuthorizationPhaseType.REQUEST);
		if (decision == AuthorizationDecisionType.ALLOW) {
			 getAllRoleTypesSpec(spec, result);
			result.recordSuccess();
			return spec;
		}
		if (decision == AuthorizationDecisionType.DENY) {
			result.recordSuccess();
			spec.setNoRoleTypes();
			spec.setFilter(NoneFilter.createNone());
			return spec;
		}
		decision = securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), AuthorizationPhaseType.REQUEST);
		if (decision == AuthorizationDecisionType.ALLOW) {
			getAllRoleTypesSpec(spec, result);
			result.recordSuccess();
			return spec;
		}
		if (decision == AuthorizationDecisionType.DENY) {
			result.recordSuccess();
			spec.setNoRoleTypes();
			spec.setFilter(NoneFilter.createNone());
			return spec;
		}

		try {
			ObjectFilter filter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.ASSIGN.getUrl(),
					AuthorizationPhaseType.REQUEST, AbstractRoleType.class, focus, AllFilter.createAll(), null, task, result);
			LOGGER.trace("assignableRoleSpec filter: {}", filter);
			spec.setFilter(filter);
			if (filter instanceof NoneFilter) {
				result.recordSuccess();
				spec.setNoRoleTypes();
				return spec;
			} else if (filter == null || filter instanceof AllFilter) {
				getAllRoleTypesSpec(spec, result);
				result.recordSuccess();
				return spec;
			} else if (filter instanceof OrFilter) {
				Collection<RoleSelectionSpecEntry> allRoleTypeDvals = new ArrayList<>();
				for (ObjectFilter subfilter: ((OrFilter)filter).getConditions()) {
					Collection<RoleSelectionSpecEntry> roleTypeDvals =  getRoleSelectionSpecEntries(subfilter);
					if (roleTypeDvals == null || roleTypeDvals.isEmpty()) {
						// This branch of the OR clause does not have any constraint for roleType
						// therefore all role types are possible (regardless of other branches, this is OR)
						spec = new RoleSelectionSpecification();
						spec.setFilter(filter);
						getAllRoleTypesSpec(spec, result);
						result.recordSuccess();
						return spec;
					} else {
						allRoleTypeDvals.addAll(roleTypeDvals);
					}
				}
				addRoleTypeSpecEntries(spec, allRoleTypeDvals, result);
			} else {
				Collection<RoleSelectionSpecEntry> roleTypeDvals = getRoleSelectionSpecEntries(filter);
				if (roleTypeDvals == null || roleTypeDvals.isEmpty()) {
					getAllRoleTypesSpec(spec, result);
					result.recordSuccess();
					return spec;
				} else {
					addRoleTypeSpecEntries(spec, roleTypeDvals, result);
				}
			}
			result.recordSuccess();
			return spec;
		} catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	private void addRoleTypeSpecEntries(RoleSelectionSpecification spec,
			Collection<RoleSelectionSpecEntry> roleTypeDvals, OperationResult result) throws ObjectNotFoundException, SchemaException, ConfigurationException {
		if (roleTypeDvals == null || roleTypeDvals.isEmpty()) {
			getAllRoleTypesSpec(spec, result);
		} else {
			if (RoleSelectionSpecEntry.hasNegative(roleTypeDvals)) {
				Collection<RoleSelectionSpecEntry> positiveList = RoleSelectionSpecEntry.getPositive(roleTypeDvals);
				if (positiveList == null || positiveList.isEmpty()) {
					positiveList = getRoleSpecEntriesForAllRoles(result);
				}
				if (positiveList == null || positiveList.isEmpty()) {
					return;
				}
				for (RoleSelectionSpecEntry positiveEntry: positiveList) {
					if (!RoleSelectionSpecEntry.hasNegativeValue(roleTypeDvals,positiveEntry.getValue())) {
						spec.addRoleType(positiveEntry);
					}
				}

			} else {
				spec.addRoleTypes(roleTypeDvals);
			}
		}
	}

	private RoleSelectionSpecification getAllRoleTypesSpec(RoleSelectionSpecification spec, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		Collection<RoleSelectionSpecEntry> allEntries = getRoleSpecEntriesForAllRoles(result);
		if (allEntries == null || allEntries.isEmpty()) {
			return spec;
		}
		spec.addRoleTypes(allEntries);
		return spec;
	}

	private Collection<RoleSelectionSpecEntry> getRoleSpecEntriesForAllRoles(OperationResult result)
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		ObjectTemplateType objectTemplateType = schemaTransformer.determineObjectTemplate(RoleType.class, AuthorizationPhaseType.REQUEST, result);
		if (objectTemplateType == null) {
			return null;
		}
		Collection<RoleSelectionSpecEntry> allEntries = new ArrayList();
		for(ObjectTemplateItemDefinitionType itemDef: objectTemplateType.getItem()) {
			ItemPathType ref = itemDef.getRef();
			if (ref == null) {
				continue;
			}
			ItemPath itemPath = ref.getItemPath();
			QName itemName = ItemPath.getName(itemPath.first());
			if (itemName == null) {
				continue;
			}
			if (QNameUtil.match(RoleType.F_ROLE_TYPE, itemName)) {
				ObjectReferenceType valueEnumerationRef = itemDef.getValueEnumerationRef();
				if (valueEnumerationRef == null || valueEnumerationRef.getOid() == null) {
					return allEntries;
				}
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
		    			GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
				PrismObject<LookupTableType> lookup = cacheRepositoryService.getObject(LookupTableType.class, valueEnumerationRef.getOid(),
						options, result);
				for (LookupTableRowType row: lookup.asObjectable().getRow()) {
					PolyStringType polyLabel = row.getLabel();
					String key = row.getKey();
					String label = key;
					if (polyLabel != null) {
						label = polyLabel.getOrig();
					}
					RoleSelectionSpecEntry roleTypeDval = new RoleSelectionSpecEntry(key, label, null);
					allEntries.add(roleTypeDval);
				}
				return allEntries;
			}
		}
		return allEntries;
	}

	private Collection<RoleSelectionSpecEntry> getRoleSelectionSpecEntries(ObjectFilter filter) throws SchemaException {
		LOGGER.trace("getRoleSelectionSpec({})", filter);
		if (filter == null || filter instanceof AllFilter) {
			return null;
		} else if (filter instanceof EqualFilter<?>) {
			return createSingleDisplayableValueCollection(getRoleSelectionSpecEq((EqualFilter)filter));
		} else if (filter instanceof AndFilter) {
			for (ObjectFilter subfilter: ((AndFilter)filter).getConditions()) {
				if (subfilter instanceof EqualFilter<?>) {
					RoleSelectionSpecEntry roleTypeDval = getRoleSelectionSpecEq((EqualFilter)subfilter);
					if (roleTypeDval != null) {
						return createSingleDisplayableValueCollection(roleTypeDval);
					}
				}
			}
			return null;
		} else if (filter instanceof OrFilter) {
			Collection<RoleSelectionSpecEntry> col = new ArrayList<>(((OrFilter)filter).getConditions().size());
			for (ObjectFilter subfilter: ((OrFilter)filter).getConditions()) {
				if (subfilter instanceof EqualFilter<?>) {
					RoleSelectionSpecEntry roleTypeDval = getRoleSelectionSpecEq((EqualFilter)subfilter);
					if (roleTypeDval != null) {
						col.add(roleTypeDval);
					}
				}
			}
			return col;
		} else if (filter instanceof TypeFilter) {
			return getRoleSelectionSpecEntries(((TypeFilter)filter).getFilter());
		} else if (filter instanceof NotFilter) {
			Collection<RoleSelectionSpecEntry> subRoleSelectionSpec = getRoleSelectionSpecEntries(((NotFilter)filter).getFilter());
			RoleSelectionSpecEntry.negate(subRoleSelectionSpec);
			return subRoleSelectionSpec;
		} else if (filter instanceof RefFilter) {
			return null;
		} else {
			throw new UnsupportedOperationException("Unexpected filter "+filter);
		}
	}

	private Collection<RoleSelectionSpecEntry> createSingleDisplayableValueCollection(
			RoleSelectionSpecEntry dval) {
		if (dval == null) {
			return null;
		}
		Collection<RoleSelectionSpecEntry> col = new ArrayList<>(1);
		col.add(dval);
		return col;
	}

	private RoleSelectionSpecEntry getRoleSelectionSpecEq(EqualFilter<String> eqFilter) throws SchemaException {
		if (QNameUtil.match(RoleType.F_ROLE_TYPE, eqFilter.getElementName())) {
			List<PrismPropertyValue<String>> ppvs = eqFilter.getValues();
			if (ppvs.size() > 1) {
				throw new SchemaException("More than one value in roleType search filter");
			}
			String roleType = ppvs.get(0).getValue();
			RoleSelectionSpecEntry roleTypeDval = new RoleSelectionSpecEntry(roleType, roleType, null);
			return roleTypeDval;
		}
		return null;
	}


	@Override
	public <T extends ObjectType> ObjectFilter getDonorFilter(Class<T> searchResultType, ObjectFilter origFilter, String targetAuthorizationAction, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.ATTORNEY.getUrl(), null, searchResultType, null, origFilter, targetAuthorizationAction, task, parentResult);
	}

	@Override
	public <T extends ObjectType, O extends ObjectType> boolean canSearch(Class<T> resultType,
			Class<O> objectType, String objectOid, boolean includeSpecial, ObjectQuery query, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		PrismObject<O> object = null;
		if (objectOid != null) {
			object = (PrismObject<O>) objectResolver.getObject(objectType, objectOid, null, task, result).asPrismObject();
		}
		return securityEnforcer.canSearch(ModelAuthorizationAction.READ.getUrl(), null, resultType, object, includeSpecial, query.getFilter(), task, result);
	}

	@Override
	public AuthenticationsPolicyType getAuthenticationPolicy(PrismObject<UserType> user, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO: check for user membership in an organization (later versions)

				OperationResult result = parentResult.createMinorSubresult(GET_AUTHENTICATIONS_POLICY);
					return resolvePolicyTypeFromSecurityPolicy(AuthenticationsPolicyType.class, SecurityPolicyType.F_AUTHENTICATION, user, task, result);

	}

	@Override
	public RegistrationsPolicyType getRegistrationPolicy(PrismObject<UserType> user, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO: check for user membership in an organization (later versions)

					OperationResult result = parentResult.createMinorSubresult(GET_REGISTRATIONS_POLICY);
					return resolvePolicyTypeFromSecurityPolicy(RegistrationsPolicyType.class, SecurityPolicyType.F_REGISTRATION, user, task, result);
	}

	@Override
	public CredentialsPolicyType getCredentialsPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO: check for user membership in an organization (later versions)

			OperationResult result = parentResult.createMinorSubresult(GET_CREDENTIALS_POLICY);
			return resolvePolicyTypeFromSecurityPolicy(CredentialsPolicyType.class, SecurityPolicyType.F_CREDENTIALS, user, task, result);


	}

	private <C extends Containerable> C  resolvePolicyTypeFromSecurityPolicy(Class<C> type, QName path, PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		SecurityPolicyType securityPolicyType = getSecurityPolicy(user, task, parentResult);
		if (securityPolicyType == null) {
			return null;
		}
		PrismContainer<C> container = securityPolicyType.asPrismObject().findContainer(path);
		if (container == null) {
			return null;
		}
		PrismContainerValue<C> containerValue = container.getValue();
		parentResult.recordSuccess();
		return containerValue.asContainerable();
	}

	@Override
	public SecurityPolicyType getSecurityPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(GET_SECURITY_POLICY);
		try {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		if (systemConfiguration == null) {
			result.recordNotApplicableIfUnknown();
			return null;
		}

		SecurityPolicyType securityPolicyType = securityHelper.locateSecurityPolicy(user, systemConfiguration, task, result);
		if (securityPolicyType == null) {
			result.recordNotApplicableIfUnknown();
			return null;
		}

		return securityPolicyType;
		}catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	@NotNull
	@Override
	public AdminGuiConfigurationType getAdminGuiConfiguration(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		AdminGuiConfigurationType config = getAdminGuiConfigurationInternal(task, parentResult);
		return config != null ? config : new AdminGuiConfigurationType();
	}

	public AdminGuiConfigurationType getAdminGuiConfigurationInternal(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		MidPointPrincipal principal = null;
		try {
			principal = securityContextManager.getPrincipal();
		} catch (SecurityViolationException e) {
			LOGGER.warn("Security violation while getting principlal to get GUI config: {}", e.getMessage(), e);
		}

		if (principal == null) {
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
			if (systemConfiguration == null) {
				return null;
			}
			return systemConfiguration.asObjectable().getAdminGuiConfiguration();
		} else {
			return principal.getAdminGuiConfiguration();
		}
	}

	@Override
	public SystemConfigurationType getSystemConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
			if (systemConfiguration == null) {
				return null;
			}
			return systemConfiguration.asObjectable();
	}

	@Override
	public DeploymentInformationType getDeploymentInformationConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
		if (systemConfiguration == null) {
			return null;
		}
			return systemConfiguration.asObjectable().getDeploymentInformation();
	}

	@Override
	public AccessCertificationConfigurationType getCertificationConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
		if (systemConfiguration == null) {
			return null;
		}
		return systemConfiguration.asObjectable().getAccessCertification();
	}

	@Override
	public boolean checkPassword(String userOid, ProtectedStringType password, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(CHECK_PASSWORD);
		UserType userType;
		try {
			 userType = objectResolver.getObjectSimple(UserType.class, userOid, null, task, result);
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		}
		if (userType.getCredentials() == null || userType.getCredentials().getPassword() == null
				|| userType.getCredentials().getPassword().getValue() == null) {
			return password == null;
		}
		ProtectedStringType currentPassword = userType.getCredentials().getPassword().getValue();
		boolean cmp;
		try {
			cmp = protector.compare(password, currentPassword);
		} catch (EncryptionException e) {
			result.recordFatalError(e);
			throw new SystemException(e.getMessage(),e);
		}
		result.recordSuccess();
		return cmp;
	}

	@Override
	public List<? extends Scene> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		return visualizer.visualizeDeltas(deltas, task, result);
	}

	@Override
	@NotNull
	public Scene visualizeDelta(ObjectDelta<? extends ObjectType> delta, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		return visualizer.visualizeDelta(delta, task, result);
	}

	@Override
	public List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		OperationResult result = parentResult.createMinorSubresult(GET_CONNECTOR_OPERATIONAL_STATUS);
		List<ConnectorOperationalStatus> status;
		try {
			status = provisioning.getConnectorOperationalStatus(resourceOid, task, result);
		} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			result.recordFatalError(e);
			throw e;
		}
		result.computeStatus();
		return status;
	}

	@Override
	public <O extends ObjectType> MergeDeltas<O> mergeObjectsPreviewDeltas(Class<O> type, String leftOid,
			String rightOid, String mergeConfigurationName, Task task, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(MERGE_OBJECTS_PREVIEW_DELTA);

		try {

			MergeDeltas<O> mergeDeltas = objectMerger.computeMergeDeltas(type, leftOid, rightOid, mergeConfigurationName, task, result);

			result.computeStatus();
			return mergeDeltas;

		} catch (ObjectNotFoundException | SchemaException | ConfigurationException | ExpressionEvaluationException |
				CommunicationException | SecurityViolationException | RuntimeException | Error e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	@Override
	public <O extends ObjectType> PrismObject<O> mergeObjectsPreviewObject(Class<O> type, String leftOid,
			String rightOid, String mergeConfigurationName, Task task, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(MERGE_OBJECTS_PREVIEW_OBJECT);

		try {

			MergeDeltas<O> mergeDeltas = objectMerger.computeMergeDeltas(type, leftOid, rightOid, mergeConfigurationName, task, result);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Merge preview {} + {} deltas:\n{}", leftOid, rightOid, mergeDeltas.debugDump(1));
			}

			final PrismObject<O> objectLeft = (PrismObject<O>) objectResolver.getObjectSimple(type, leftOid, null, task, result).asPrismObject();

			if (mergeDeltas == null) {
				result.computeStatus();
				return objectLeft;
			}

			mergeDeltas.getLeftObjectDelta().applyTo(objectLeft);
			mergeDeltas.getLeftLinkDelta().applyTo(objectLeft);

			result.computeStatus();
			return objectLeft;

		} catch (ObjectNotFoundException | SchemaException | ConfigurationException | ExpressionEvaluationException |
				CommunicationException | SecurityViolationException | RuntimeException | Error e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	@Override
	public <O extends ObjectType> String generateValue(ValuePolicyType policy, int defaultLength, boolean generateMinimalSize,
			PrismObject<O> object, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		return policyProcessor.generate(null, policy, defaultLength, generateMinimalSize, createOriginResolver(object, parentResult), shortDesc, task, parentResult);
	}

	@Override
	public <O extends ObjectType> void generateValue(PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
			Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException   {
		
		OperationResult result = parentResult.createSubresult(OPERATION_GENERATE_VALUE);

			
			ValuePolicyType valuePolicy = null;
			try {
				valuePolicy = getValuePolicy(object, task, result);
			} catch (ObjectNotFoundException | SchemaException | CommunicationException
					| ConfigurationException | SecurityViolationException
					| ExpressionEvaluationException e) {
				LOGGER.error("Failed to get value policy for generating value. ", e);
				result.recordFatalError("Error while getting value policy. Reason: " + e.getMessage(), e);
				throw e;
			}

			Collection<PropertyDelta<?>> deltasToExecute = new ArrayList<>();
			for (PolicyItemDefinitionType policyItemDefinition : policyItemsDefinition.getPolicyItemDefinition()) {
				OperationResult generateValueResult = parentResult.createSubresult(OPERATION_GENERATE_VALUE);
				
				LOGGER.trace("Default value policy: {}" , valuePolicy);
				try {
					generateValue(object, valuePolicy, policyItemDefinition, task, generateValueResult);
				} catch (ExpressionEvaluationException | SchemaException | ObjectNotFoundException
						| CommunicationException | ConfigurationException | SecurityViolationException e) {
					LOGGER.error("Failed to generate value for {} " + policyItemDefinition, e);
					generateValueResult.recordFatalError("Failed to generate value for " + policyItemDefinition + ". Reason: " + e.getMessage(), e);
					policyItemDefinition.setResult(generateValueResult.createOperationResultType());
					continue;
				}
				
				//TODO: not sure about the bulk actions here
				ItemPath path = getPath(policyItemDefinition);
				if (path == null) {
					if (isExecute(policyItemDefinition)) {
						LOGGER.error("No item path defined in the target for policy item definition. Cannot generate value");
						generateValueResult.recordFatalError(
								"No item path defined in the target for policy item definition. Cannot generate value");
						continue;
					}
				}
	
				PrismPropertyDefinition<?> propertyDef = null;
				if (path != null) {
					result.addArbitraryObjectAsParam("policyItemPath", path);
					
					propertyDef = getItemDefinition(object, path);
					if (propertyDef == null) {
						if (isExecute(policyItemDefinition)) {
							LOGGER.error("No definition for property {} in object. Is the path referencing prism property?" + path,
									object);
							generateValueResult.recordFatalError("No definition for property " + path + " in object " + object
									+ ". Is the path referencing prism property?");
							continue;
						}
		
					}
				}
			// end of not sure
				
				collectDeltasForGeneratedValuesIfNeeded(object, policyItemDefinition, deltasToExecute, path, propertyDef, generateValueResult);
				generateValueResult.computeStatusIfUnknown();
			}
			result.computeStatus();
			if (!result.isAcceptable()) {
				return;
			}
		try {
			if (!deltasToExecute.isEmpty()) {
				if (object == null) {
					LOGGER.error("Cannot execute changes for generated values, no object specified in request.");
					result.recordFatalError("Cannot execute changes for generated values, no object specified in request.");
					throw new SchemaException("Cannot execute changes for generated values, no object specified in request.");
				}
					String oid = object.getOid();
					Class<O> clazz = (Class<O>) object.asObjectable().getClass();
					modelCrudService.modifyObject(clazz, oid, deltasToExecute, null, task, result);
				
			}
		} catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException
				| CommunicationException | ConfigurationException | ObjectAlreadyExistsException
				| PolicyViolationException | SecurityViolationException e) {
			LOGGER.error("Could not execute deltas for generated values. Reason: " + e.getMessage(), e);
			result.recordFatalError("Could not execute deltas for gegenerated values. Reason: "+ e.getMessage(), e);
			throw e;
		}


	}

	private boolean isExecute(PolicyItemDefinitionType policyItemDefinition) {
		if (policyItemDefinition.isExecute() == null) {
			return false;
		}
		
		return policyItemDefinition.isExecute().booleanValue();
	}
	private ItemPath getPath(PolicyItemDefinitionType policyItemDefinition){
		PolicyItemTargetType target = policyItemDefinition.getTarget();

		if (target == null ) {
			return null;
		}

		ItemPathType itemPathType = target.getPath();
		if (itemPathType == null) {

			return null;
		}
		return itemPathType.getItemPath();
	}

	private <O extends ObjectType> PrismPropertyDefinition<?> getItemDefinition(PrismObject<O> object, ItemPath path){
		ItemDefinition<?> itemDef = object.getDefinition().findItemDefinition(path);
		if (itemDef == null) {
			return null;
		} else if (!(itemDef instanceof PrismPropertyDefinition)) {
			return null;
		}

		return (PrismPropertyDefinition<?>) itemDef;
	}

	private <O extends ObjectType> void collectDeltasForGeneratedValuesIfNeeded(PrismObject<O> object,
			PolicyItemDefinitionType policyItemDefinition, Collection<PropertyDelta<?>> deltasToExecute, ItemPath path,
			PrismPropertyDefinition<?> itemDef, OperationResult result) throws SchemaException {

		Object value = policyItemDefinition.getValue();

		if (itemDef != null){
			if (ProtectedStringType.COMPLEX_TYPE.equals(itemDef.getTypeName())) {
				ProtectedStringType pst = new ProtectedStringType();
				pst.setClearValue((String) value);
				value = pst;
			} else if (PolyStringType.COMPLEX_TYPE.equals(itemDef.getTypeName())) {
				value = new PolyString((String) value);
			}
		}
		if (object == null && isExecute(policyItemDefinition)) {
			LOGGER.warn("Cannot apply generated changes and cannot execute them becasue there is no target object specified.");
			result.recordFatalError("Cannot apply generated changes and cannot execute them becasue there is no target object specified.");
			return;
		}
		if (object != null) {
			PropertyDelta<?> propertyDelta = PropertyDelta.createModificationReplaceProperty(path, object.getDefinition(), value);
			propertyDelta.applyTo(object); // in bulk actions we need to modify original objects - hope that REST is OK with this
			if (BooleanUtils.isTrue(policyItemDefinition.isExecute())) {
				deltasToExecute.add(propertyDelta);
			}
		}
		
	}

	private <O extends ObjectType> void generateValue(PrismObject<O> object, ValuePolicyType defaultPolicy,
			PolicyItemDefinitionType policyItemDefinition, Task task, OperationResult result)
			throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, SecurityViolationException {

		PolicyItemTargetType target = policyItemDefinition.getTarget();
		if ((target == null || ItemPath.isNullOrEmpty(target.getPath())) && isExecute(policyItemDefinition)) {
			LOGGER.error("Target item path must be defined");
			throw new SchemaException("Target item path must be defined");
		}
		ItemPath targetPath = null;
		
		if (target != null) {
			targetPath = target.getPath().getItemPath();
		}

		ValuePolicyType valuePolicy = resolveValuePolicy(policyItemDefinition, defaultPolicy, task, result);
		LOGGER.trace("Value policy used for generating new value : {}", valuePolicy);
		StringPolicyType stringPolicy = valuePolicy != null ? valuePolicy.getStringPolicy() : null;
		if (stringPolicy == null) {
			LOGGER.trace("No sting policy defined. Cannot generate value.");
			result.recordFatalError("No string policy defined. Cannot generate value");
			return;
//			throw new SchemaException("No value policy for " + targetPath);
		}

		String newValue = policyProcessor.generate(targetPath, valuePolicy, 10, false, createOriginResolver(object, result),
				"generating value for" + targetPath, task, result);
		policyItemDefinition.setValue(newValue);
	}

	private ValuePolicyType resolveValuePolicy(PolicyItemDefinitionType policyItemDefinition, ValuePolicyType defaultPolicy,
			Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (policyItemDefinition.getValuePolicyRef() != null) {
			LOGGER.trace("Trying to resolve value policy {} for policy item definition", policyItemDefinition);
			return objectResolver.resolve(policyItemDefinition.getValuePolicyRef(), ValuePolicyType.class, null,
					"valuePolicyRef in policyItemDefinition", task, result);
		}

		return defaultPolicy;
	}

	public <O extends ObjectType> void validateValue(PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
			Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
		ValuePolicyType valuePolicy = getValuePolicy(object, task, parentResult);
		for (PolicyItemDefinitionType policyItemDefinition : policyItemsDefinition.getPolicyItemDefinition()) {
			validateValue(object, valuePolicy, policyItemDefinition, task, parentResult);
		}
	}

	private <O extends ObjectType> ValuePolicyType getValuePolicy(PrismObject<O> object, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException,
			ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		// user-level policy
		CredentialsPolicyType policy = null;
		if (object != null && object.getCompileTimeClass().isAssignableFrom(UserType.class)) {
			policy = getCredentialsPolicy((PrismObject<UserType>) object, task, parentResult);
		}

		SystemConfigurationType systemConfigurationType = getSystemConfiguration(parentResult);
		if (!containsValuePolicyDefinition(policy)) {
			SecurityPolicyType securityPolicy = securityHelper.locateGlobalSecurityPolicy(systemConfigurationType, task, parentResult);
			if (securityPolicy != null) {
				policy = securityPolicy.getCredentials();
			}
		}

		if (!containsValuePolicyDefinition(policy)) {
			SecurityPolicyType securityPolicy = securityHelper.locateGlobalPasswordPolicy(systemConfigurationType, task, parentResult);
			if (securityPolicy != null) {
				policy = securityPolicy.getCredentials();
			}
		}

		if (containsValuePolicyDefinition(policy)){

				if (policy.getPassword().getValuePolicyRef() != null) {
					return objectResolver.resolve(policy.getPassword().getValuePolicyRef(), ValuePolicyType.class, null, "valuePolicyRef in password credential policy", task, parentResult);
				} else if (policy.getPassword().getPasswordPolicyRef() != null) {
					// DEPRECATED
					return objectResolver.resolve(policy.getPassword().getPasswordPolicyRef(), ValuePolicyType.class, null, "valuePolicyRef in password credential policy", task, parentResult);
				}
		}

		return null;
	}

	private boolean containsValuePolicyDefinition(CredentialsPolicyType policy) {
		if (policy == null) {
			return false;
		}

		if (policy.getPassword() == null) {
			return false;
		}

		if (policy.getPassword().getValuePolicyRef() != null) {
			return true;
		}

		if (policy.getPassword().getPasswordPolicyRef() != null) {
			return true;
		}

		return false;
	}

	private <T, O extends ObjectType> boolean validateValue(PrismObject<O> object, ValuePolicyType policy, PolicyItemDefinitionType policyItemDefinition, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {

		ValuePolicyType stringPolicy = resolveValuePolicy(policyItemDefinition, policy, task, parentResult);

		Object value = policyItemDefinition.getValue();
		String valueToValidate = null;
		if (value instanceof RawType) {
			valueToValidate = ((RawType) value).getParsedRealValue(String.class);
		} else {
			valueToValidate = (String) value;
		}
		
		List<String> valuesToValidate = new ArrayList<>();
		PolicyItemTargetType target = policyItemDefinition.getTarget();
		ItemPath path = null;
		if (target != null) {
			path = target.getPath().getItemPath();
		}
		if (StringUtils.isNotEmpty(valueToValidate)) {
			valuesToValidate.add(valueToValidate);
		} else {
			if (target == null || target.getPath() == null) {
				LOGGER.error("Target item path must be defined");
				parentResult.recordFatalError("Target item path must be defined");
				throw new SchemaException("Target item path must be defined");
			}
			path = target.getPath().getItemPath();

			if (object == null) {
				LOGGER.error("Object which values should be validated is null. Nothing to validate.");
				parentResult.recordFatalError("Object which values should be validated is null. Nothing to validate.");
				throw new SchemaException("Object which values should be validated is null. Nothing to validate.");
			}
			
			PrismProperty<T> property = object.findProperty(path);
			if (property == null || property.isEmpty()) {
				LOGGER.error("Attribute {} has no value. Nothing to validate.", property);
				parentResult.recordFatalError("Attribute " + property + " has no value. Nothing to validate");
				throw new SchemaException("Attribute " + property + " has no value. Nothing to validate");
			}


			PrismPropertyDefinition<T> itemToValidateDefinition = property.getDefinition();
			QName definitionName = itemToValidateDefinition.getTypeName();
			if (!isSupportedType(definitionName)) {
				LOGGER.error("Trying to validate string policy on the property of type {} failed. Unsupported type.",
						itemToValidateDefinition);
				parentResult.recordFatalError("Trying to validate string policy on the property of type "
						+ itemToValidateDefinition + " failed. Unsupported type.");
				throw new SchemaException("Trying to validate string policy on the property of type "
						+ itemToValidateDefinition + " failed. Unsupported type.");
			}

			if (itemToValidateDefinition.isSingleValue()) {
				if (definitionName.equals(PolyStringType.COMPLEX_TYPE)) {
					valueToValidate = ((PolyString) property.getRealValue()).getOrig();

				} else if (definitionName.equals(ProtectedStringType.COMPLEX_TYPE)){
					ProtectedStringType protectedString = ((ProtectedStringType) property.getRealValue());
					valueToValidate = getClearValue(protectedString);


				} else {
					valueToValidate = (String) property.getRealValue();
				}
				valuesToValidate.add(valueToValidate);
			} else {
				if (definitionName.equals(DOMUtil.XSD_STRING)) {
					valuesToValidate.addAll(property.getRealValues(String.class));
				} else if (definitionName.equals(ProtectedStringType.COMPLEX_TYPE)) {
					for (ProtectedStringType protectedString : property.getRealValues(ProtectedStringType.class)) {
						valuesToValidate.add(getClearValue(protectedString));
					}
				} else {
					for (PolyString val : property.getRealValues(PolyString.class)) {
						valuesToValidate.add(val.getOrig());
					}
				}
			}

		}

		for (String newValue : valuesToValidate) {
			OperationResult result = parentResult.createSubresult(OPERATION_VALIDATE_VALUE + ".value");
			if (path != null ) result.addParam("path", path.toString());
			result.addParam("valueToValidate", newValue);
			if (stringPolicy == null) {
				stringPolicy = new ValuePolicyType();
				stringPolicy.setName(PolyString.toPolyStringType(new PolyString("Default policy")));
			}
			
			ObjectValuePolicyEvaluator evaluator = new ObjectValuePolicyEvaluator();
			evaluator.setValuePolicy(stringPolicy);
			evaluator.setValuePolicyProcessor(policyProcessor);
			evaluator.setProtector(protector);
			evaluator.setValueItemPath(path);
			evaluator.setOriginResolver(getOriginResolver(object));
			evaluator.setTask(task);
			evaluator.setShortDesc(" rest validate ");
			if (object != null && path != null && SchemaConstants.PATH_PASSWORD.isSuperPathOrEquivalent(path)) {
				evaluator.setSecurityPolicy(getSecurityPolicy((PrismObject<UserType>) object, task, parentResult));
				PrismContainer<PasswordType> password = object.findContainer(SchemaConstants.PATH_PASSWORD);
				PasswordType passwordType = null;
				if (password != null) {
					PrismContainerValue<PasswordType> passwordPcv = password.getValue();
					passwordType = passwordPcv != null ? passwordPcv.asContainerable() : null;
				}
				evaluator.setOldCredentialType(passwordType);
			}
			evaluator.setNow(clock.currentTimeXMLGregorianCalendar());
			try { 
				LOGGER.trace("Validating value started");
				OperationResult subResult = evaluator.validateStringValue(newValue);
				LOGGER.trace("Validating value finished");
				result.addSubresult(subResult);
//				result.recomputeStatus();
			} catch(Exception e) {
				result.recordFatalError("Cannot validate value. " + e.getMessage());
			}
			
			result.computeStatus();
			
//			if (!policyProcessor.validateValue(newValue, stringPolicy, createOriginResolver(object, result), "validate value " + (path!= null ? "for " + path : "") + " for " + object + " value " + valueToValidate, task, result)) {
//				result.recordFatalError("Validation for value " + newValue + " against policy " + stringPolicy + " failed");
//				LOGGER.error("Validation for value {} against policy {} failed", newValue, stringPolicy);
//			}
			
		}

		parentResult.computeStatus();
		policyItemDefinition.setResult(parentResult.createOperationResultType());

		return parentResult.isAcceptable();

	}

	private <O extends ObjectType> AbstractValuePolicyOriginResolver<O> getOriginResolver(PrismObject<O> object) {
		if (object != null && UserType.class.equals(object.getCompileTimeClass())) {
			new UserValuePolicyOriginResolver((PrismObject<UserType>) object, objectResolver);
		}
		
		//TODO not supported yet
		return null;
	}
	
	private <O extends ObjectType> AbstractValuePolicyOriginResolver<O> createOriginResolver(PrismObject<O> object, OperationResult result) throws SchemaException {
		if (object == null) {
			return null;
		}
		if (object.canRepresent(UserType.class)) {
			return (AbstractValuePolicyOriginResolver<O>) new UserValuePolicyOriginResolver((PrismObject<UserType>) object, objectResolver);
		}
		if (object.canRepresent(ShadowType.class)) {
			return (AbstractValuePolicyOriginResolver<O>) new ShadowValuePolicyOriginResolver((PrismObject<ShadowType>) object, objectResolver);
		}
		SchemaException e = new SchemaException("Unsupport object type "+object);
		result.recordFatalError(e);
		throw e;
	}

	private boolean isSupportedType(QName type) {

		if (QNameUtil.qNameToUri(type).equals(QNameUtil.qNameToUri(DOMUtil.XSD_STRING))){
			return true;
		}

		if (QNameUtil.qNameToUri(type).equals(QNameUtil.qNameToUri(PolyStringType.COMPLEX_TYPE))) {
			return true;
		}

		if (QNameUtil.qNameToUri(type).equals(QNameUtil.qNameToUri(ProtectedStringType.COMPLEX_TYPE))) {
			return true;
		}

		return false;
	}

	private String getClearValue(ProtectedStringType protectedString) throws SchemaException, PolicyViolationException {
		try {
			if (protectedString.isEncrypted()) {

				return protector.decryptString(protectedString);

			} else if (protectedString.getClearValue() != null) {
				return protector.decryptString(protectedString);
			} else if (protectedString.isHashed()) {
				throw new SchemaException("Cannot validate value of hashed password");
			}
		} catch (EncryptionException e) {
			throw new PolicyViolationException(e.getMessage(), e);
		}
		return null;
	}

	// TODO TODO TODO deduplicate this somehow!

	@NotNull
	@Override
	public List<ObjectReferenceType> getDeputyAssignees(AbstractWorkItemType workItem, Task task, OperationResult parentResult)
			throws SchemaException {
		OperationResult result = parentResult.createMinorSubresult(GET_DEPUTY_ASSIGNEES);
		RepositoryCache.enter();
		try {
			Set<String> oidsToSkip = new HashSet<>();
			List<ObjectReferenceType> deputies = new ArrayList<>();
			workItem.getAssigneeRef().forEach(a -> oidsToSkip.add(a.getOid()));
			getDeputyAssignees(deputies, workItem, oidsToSkip, task, result);
			result.computeStatusIfUnknown();
			return deputies;
		} catch (Throwable t) {
			result.recordFatalError(t.getMessage(), t);
			throw t;
		} finally {
			RepositoryCache.exit();
		}
	}

	@NotNull
	@Override
	public List<ObjectReferenceType> getDeputyAssignees(ObjectReferenceType assigneeRef, QName limitationItemName, Task task,
			OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createMinorSubresult(GET_DEPUTY_ASSIGNEES);
		RepositoryCache.enter();
		try {
			Set<String> oidsToSkip = new HashSet<>();
			oidsToSkip.add(assigneeRef.getOid());
			List<ObjectReferenceType> deputies = new ArrayList<>();
			getDeputyAssigneesNoWorkItem(deputies, assigneeRef, limitationItemName, oidsToSkip, task, result);
			result.computeStatusIfUnknown();
			return deputies;
		} catch (Throwable t) {
			result.recordFatalError(t.getMessage(), t);
			throw t;
		} finally {
			RepositoryCache.exit();
		}
	}

	private void getDeputyAssignees(List<ObjectReferenceType> deputies, AbstractWorkItemType workItem, Set<String> oidsToSkip,
			Task task, OperationResult result) throws SchemaException {
		List<PrismReferenceValue> assigneeReferencesToQuery = workItem.getAssigneeRef().stream()
				.map(assigneeRef -> assigneeRef.clone().relation(PrismConstants.Q_ANY).asReferenceValue())
				.collect(Collectors.toList());
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_DELEGATED_REF).ref(assigneeReferencesToQuery)
				.build();
		SearchResultList<PrismObject<UserType>> potentialDeputies = cacheRepositoryService
				.searchObjects(UserType.class, query, null, result);
		for (PrismObject<UserType> potentialDeputy : potentialDeputies) {
			if (oidsToSkip.contains(potentialDeputy.getOid())) {
				continue;
			}
			if (determineDeputyValidity(potentialDeputy, workItem.getAssigneeRef(), workItem, OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, task, result)) {
				deputies.add(ObjectTypeUtil.createObjectRefWithFullObject(potentialDeputy));
				oidsToSkip.add(potentialDeputy.getOid());
			}
		}
	}

	private void getDeputyAssigneesNoWorkItem(List<ObjectReferenceType> deputies, ObjectReferenceType assigneeRef,
			QName limitationItemName, Set<String> oidsToSkip,
			Task task, OperationResult result) throws SchemaException {
		PrismReferenceValue assigneeReferenceToQuery = assigneeRef.clone().relation(PrismConstants.Q_ANY).asReferenceValue();
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_DELEGATED_REF).ref(assigneeReferenceToQuery)
				.build();
		SearchResultList<PrismObject<UserType>> potentialDeputies = cacheRepositoryService
				.searchObjects(UserType.class, query, null, result);
		for (PrismObject<UserType> potentialDeputy : potentialDeputies) {
			if (oidsToSkip.contains(potentialDeputy.getOid())) {
				continue;
			}
			if (determineDeputyValidity(potentialDeputy, Collections.singletonList(assigneeRef), null, limitationItemName, task, result)) {
				deputies.add(ObjectTypeUtil.createObjectRefWithFullObject(potentialDeputy));
				oidsToSkip.add(potentialDeputy.getOid());
			}
		}
	}

	private boolean determineDeputyValidity(PrismObject<UserType> potentialDeputy, List<ObjectReferenceType> assignees,
			@Nullable AbstractWorkItemType workItem, QName privilegeLimitationItemName, Task task, OperationResult result) {
		AssignmentEvaluator.Builder<UserType> builder =
				new AssignmentEvaluator.Builder<UserType>()
						.repository(cacheRepositoryService)
						.focusOdo(new ObjectDeltaObject<>(potentialDeputy, null, potentialDeputy))
						.channel(null)
						.objectResolver(objectResolver)
						.systemObjectCache(systemObjectCache)
						.prismContext(prismContext)
						.mappingFactory(mappingFactory)
						.mappingEvaluator(mappingEvaluator)
						.activationComputer(activationComputer)
						.now(clock.currentTimeXMLGregorianCalendar())
						.loginMode(true)
						// We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
						// will need something to push on the stack. So give them context placeholder.
						.lensContext(new LensContextPlaceholder<>(potentialDeputy, prismContext));
		AssignmentEvaluator<UserType> assignmentEvaluator = builder.build();

		for (AssignmentType assignmentType: potentialDeputy.asObjectable().getAssignment()) {
			if (!DeputyUtils.isDelegationAssignment(assignmentType)) {
				continue;
			}
			try {
				ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
				assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
				assignmentIdi.recompute();
				// TODO some special mode for verification of the validity - we don't need complete calculation here!
				EvaluatedAssignment<UserType> assignment = assignmentEvaluator
						.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, potentialDeputy.asObjectable(),
								potentialDeputy.toString(), task, result);
				if (!assignment.isValid()) {
					continue;
				}
				for (EvaluatedAssignmentTarget target : assignment.getRoles().getNonNegativeValues()) {
					if (target.getTarget() != null && target.getTarget().getOid() != null
							&& DeputyUtils.isDelegationPath(target.getAssignmentPath())
							&& ObjectTypeUtil.containsOid(assignees, target.getTarget().getOid())) {
						List<OtherPrivilegesLimitationType> limitations = DeputyUtils.extractLimitations(target.getAssignmentPath());
						if (workItem != null && DeputyUtils.limitationsAllow(limitations, privilegeLimitationItemName, workItem)
								|| workItem == null && DeputyUtils.limitationsAllow(limitations, privilegeLimitationItemName)) {
							return true;
						}
					}
				}
			} catch (CommonException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't verify 'deputy' relation between {} and {} for work item {}; assignment: {}",
						e, potentialDeputy, assignees, workItem, assignmentType);
			}
		}
		return false;
	}

	@Override
	public ActivationStatusType getEffectiveStatus(String lifecycleStatus, ActivationType activationType) {
		return activationComputer.getEffectiveStatus(lifecycleStatus, activationType);
	}
	
	@Override
	public MidPointPrincipal assumePowerOfAttorney(PrismObject<UserType> donor, Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		MidPointPrincipal attorneyPrincipal = securityContextManager.getPrincipal();
		MidPointPrincipal donorPrincipal =  securityEnforcer.createDonorPrincipal(attorneyPrincipal, ModelAuthorizationAction.ATTORNEY.getUrl(), donor, task, result);

		// TODO: audit switch

		securityContextManager.setupPreAuthenticatedSecurityContext(donorPrincipal);
		
		return donorPrincipal;
	}
	
	@Override
	public MidPointPrincipal dropPowerOfAttorney(Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		MidPointPrincipal donorPrincipal = securityContextManager.getPrincipal();
		if (donorPrincipal.getAttorney() == null) {
			throw new IllegalStateException("Attempt to drop attorney powers using non-donor principal "+donorPrincipal);
		}
		MidPointPrincipal previousPrincipal = donorPrincipal.getPreviousPrincipal();
		if (previousPrincipal == null) {
			throw new IllegalStateException("Attempt to drop attorney powers, but no previous principal in "+donorPrincipal);
		}
		
		// TODO: audit switch
		
		// TODO: maybe refresh previous principal using userProfileService?
		securityContextManager.setupPreAuthenticatedSecurityContext(previousPrincipal);
		
		return previousPrincipal;
	}

	@Override
	@NotNull
	public LocalizableMessageType createLocalizableMessageType(LocalizableMessageTemplateType template,
			Map<QName, Object> variables, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, SecurityViolationException {
		ExpressionVariables vars = new ExpressionVariables();
		vars.addVariableDefinitions(variables);
		return LensUtil.interpretLocalizableMessageTemplate(template, vars, expressionFactory, prismContext, task, result);
	}

	@Override
	public ExecuteCredentialResetResponseType executeCredentialsReset(PrismObject<UserType> user,
			ExecuteCredentialResetRequestType executeCredentialResetRequest, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
		LocalizableMessageBuilder builder = new LocalizableMessageBuilder();
		
		ExecuteCredentialResetResponseType response = new ExecuteCredentialResetResponseType(prismContext);
		
		String resetMethod = executeCredentialResetRequest.getResetMethod();
		if (StringUtils.isBlank(resetMethod)) {
			LocalizableMessage localizableMessage = builder.fallbackMessage("Failed to execute reset password. Bad request.").key("execute.reset.credential.bad.request").build();
			response = response.message(LocalizationUtil.createLocalizableMessageType(localizableMessage));
			throw new SchemaException(localizableMessage);
			
		}
		
		SecurityPolicyType securityPolicy = getSecurityPolicy(user, task, parentResult);
		CredentialsResetPolicyType resetPolicyType = securityPolicy.getCredentialsReset();
		//TODO: search according tot he credentialID and others
		if (resetPolicyType == null) {
			LocalizableMessage localizableMessage = builder.fallbackMessage("Failed to execute reset password. Bad configuration.").key("execute.reset.credential.bad.configuration").build();
			response = response.message(LocalizationUtil.createLocalizableMessageType(localizableMessage));
			throw new SchemaException(localizableMessage);
		}
		
		if (!resetMethod.equals(resetPolicyType.getName())) {
			LocalizableMessage localizableMessage = builder.fallbackMessage("Failed to execute reset password. Bad method.").key("execute.reset.credential.bad.method").build();
			response = response.message(LocalizationUtil.createLocalizableMessageType(localizableMessage));
			throw new SchemaException(localizableMessage);
		}
		
		
		CredentialSourceType credentialSourceType = resetPolicyType.getNewCredentialSource();
		
		if (credentialSourceType == null) {
			//TODO: go through deprecated functionality
			LocalizableMessage localizableMessage = builder.fallbackMessage("Failed to execute reset password. No credential source.").key("execute.reset.credential.no.credential.source").build();
			response = response.message(LocalizationUtil.createLocalizableMessageType(localizableMessage));
			//for now just let the user know that he needs to specify it
			return response;
		}
		
		ValuePolicyType valuePolicy = getValuePolicy(user, task, parentResult);
		
		ObjectDelta<UserType> userDelta = null;
		if (credentialSourceType.getUserEntry() != null) {
			PolicyItemDefinitionType policyItemDefinitione = new PolicyItemDefinitionType();
			policyItemDefinitione.setValue(executeCredentialResetRequest.getUserEntry());
			
			if (!validateValue(user, valuePolicy, policyItemDefinitione, task, parentResult)) {
				LOGGER.error("Cannot execute reset password. New password doesn't satisfy policy constraints");
				parentResult.recordFatalError("Cannot execute reset password. New password doesn't satisfy policy constraints");
				LocalizableMessage localizableMessage = builder.fallbackMessage("New password doesn't satisfy policy constraints.").key("execute.reset.credential.validation.failed").build();
				throw new PolicyViolationException(localizableMessage);
			}
			
			ProtectedStringType newProtectedPassword = new ProtectedStringType();
			newProtectedPassword.setClearValue(executeCredentialResetRequest.getUserEntry());
			userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, user.getOid(),
					SchemaConstants.PATH_PASSWORD_VALUE, prismContext, newProtectedPassword);

		}
		
		if (BooleanUtils.isTrue(resetPolicyType.isForceChange())) {
			if (userDelta != null) {
				userDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_FORCE_CHANGE, Boolean.TRUE);
			}
		}
		

		try {
		Collection<ObjectDeltaOperation<? extends ObjectType>> result = modelService.executeChanges(
				MiscUtil.createCollection(userDelta), ModelExecuteOptions.createRaw(), task, parentResult);
		} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException 
			| SecurityViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException | PolicyViolationException e) {
			response.message(LocalizationUtil.createForFallbackMessage("Failed to reset credential: " + e.getMessage()));
			throw e;
		}

		parentResult.recomputeStatus();
		LocalizableMessage message = builder.fallbackMessage("Reset password was successful").key("execute.reset.credential.successful").fallbackLocalizableMessage(null).build();
		response.setMessage(LocalizationUtil.createLocalizableMessageType(message));
		
		return response;
	}
	
	public void clearCaches() {
		cacheRegistry.clearAllCaches();
	}
	

}
