/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.schema.config.ConfigurationItemOrigin.embedded;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType.RUNNABLE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType.READY;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.scripting.BulkActionsExecutor;
import com.evolveum.midpoint.model.impl.security.ModelSecurityPolicyFinder;
import com.evolveum.midpoint.repo.common.security.SecurityPolicyFinder;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.security.enforcer.api.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.api.util.MergeDeltas;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.model.api.visualizer.ModelContextVisualization;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.common.mapping.metadata.MetadataItemProcessingSpecImpl;
import com.evolveum.midpoint.model.common.stringpolicy.*;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.controller.tasks.ActivityExecutor;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableContainerDefinition;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableObjectDefinition;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableReferenceDefinition;
import com.evolveum.midpoint.model.impl.security.GuiProfileCompiler;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 */
@Component("modelInteractionService")
public class ModelInteractionServiceImpl implements ModelInteractionService {

    private static final Trace LOGGER = TraceManager.getTrace(ModelInteractionServiceImpl.class);

    @Autowired private ContextFactory contextFactory;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private SchemaTransformer schemaTransformer;
    @Autowired private ProvisioningService provisioning;
    @Autowired private BulkActionsExecutor bulkActionsExecutor;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private ObjectMerger objectMerger;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;
    @Autowired private ReferenceResolver referenceResolver;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ValuePolicyProcessor policyProcessor;
    @Autowired private Protector protector;
    @Autowired private PrismContext prismContext;
    @Autowired private Visualizer visualizer;
    @Autowired private ModelService modelService;
    @Autowired private ModelCrudService modelCrudService;
    @Autowired private SecurityHelper securityHelper;
    @Autowired private ActivationComputer activationComputer;
    @Autowired private Clock clock;
    @Autowired private GuiProfiledPrincipalManager guiProfiledPrincipalManager;
    @Autowired private GuiProfileCompiler guiProfileCompiler;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private Clockwork clockwork;
    @Autowired private CollectionProcessor collectionProcessor;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private ClusterwideUserSessionManager clusterwideUserSessionManager;
    @Autowired private ModelAuditService modelAuditService;
    @Autowired private TaskManager taskManager;
    @Autowired private SimulationResultManager simulationResultManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ClockworkHookHelper clockworkHookHelper;
    @Autowired private SecurityPolicyFinder securityPolicyFinder;
    @Autowired private ModelSecurityPolicyFinder modelSecurityPolicyFinder;

    private static final String OPERATION_GENERATE_VALUE = ModelInteractionService.class.getName() + ".generateValue";
    private static final String OPERATION_VALIDATE_VALUE = ModelInteractionService.class.getName() + ".validateValue";

    /** The "modern" implementation that uses the simulations feature. */
    @Override
    public @NotNull <F extends ObjectType> ModelContext<F> previewChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options,
            Task task,
            Collection<ProgressListener> listeners,
            OperationResult result)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

        var originalExecutionMode = switchModeToSimulationIfNeeded(task);
        try {

            // We extract the model context using special progress listener
            var collectingProgressListener = new ProgressListener.Collecting();

            var newListeners = ProgressListener.add(listeners, collectingProgressListener);
            modelService.executeChanges(deltas, options, task, newListeners, result);

            //noinspection unchecked
            var lastContext = (LensContext<F>) collectingProgressListener.getFinalContext();

            // Provides information from the hooks (e.g., approvals) to the model context, just like legacy previewChanges did.
            clockworkHookHelper.invokePreview(lastContext, task, result);

            // Hides sensitive information from non-root users.
            schemaTransformer.applySecurityToLensContext(lastContext, task, result);

            return lastContext;

        } finally {
            task.setExecutionMode(originalExecutionMode);
        }
    }

    private static @NotNull TaskExecutionMode switchModeToSimulationIfNeeded(Task task) {
        TaskExecutionMode executionMode = task.getExecutionMode();
        if (executionMode.isFullyPersistent()) {
            LOGGER.debug("Task {} has 'persistent' execution mode when executing previewChanges, setting to SIMULATED_PRODUCTION",
                    task.getName());

            task.setExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION);
        }
        return executionMode;
    }

    @NotNull
    private Collection<ObjectDelta<? extends ObjectType>> cloneDeltas(Collection<ObjectDelta<? extends ObjectType>> deltas) {
        Collection<ObjectDelta<? extends ObjectType>> clonedDeltas;
        if (deltas != null) {
            clonedDeltas = new ArrayList<>(deltas.size());
            for (ObjectDelta<? extends ObjectType> delta : deltas) {
                clonedDeltas.add(delta.clone());
            }
            return clonedDeltas;
        } else {
            return emptyList();
        }
    }

    @Override
    public <F extends ObjectType> ModelContext<F> unwrapModelContext(LensContextType wrappedContext, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        return LensContext.fromLensContextBean(wrappedContext, task, result);
    }

    @Override
    public <O extends ObjectType> @NotNull PrismObjectDefinition<O> getEditObjectDefinition(
            PrismObject<O> object, AuthorizationPhaseType phase, Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(GET_EDIT_OBJECT_DEFINITION);
        try {
            // Re-read the object from the repository to make sure we have all the properties.
            // the object from method parameters may be already processed by the security code
            // and properties needed to evaluate authorizations may not be there
            // MID-3126, see also MID-3435
            PrismObject<O> fullObject = getFullObjectReadWrite(object, result);

            // TODO: maybe we need to expose owner resolver in the interface?
            ObjectSecurityConstraints securityConstraints =
                    securityEnforcer.compileSecurityConstraints(fullObject, true, SecurityEnforcer.Options.create(), task, result);
            LOGGER.trace("Security constrains for {}:\n{}", object, DebugUtil.debugDumpLazily(securityConstraints));
            TransformableObjectDefinition<O> objectDefinition = schemaTransformer.transformableDefinition(object.getDefinition());
            applyArchetypePolicy(objectDefinition, object, task, result);
            schemaTransformer.applySecurityConstraintsToItemDef(objectDefinition, securityConstraints, phase);
            if (object.canRepresent(ShadowType.class)) {
                applyObjectClassDefinition(objectDefinition, object, phase, task, result);
            }
            return objectDefinition;
        } catch (ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | SchemaException e) {
            result.recordException(e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <O extends ObjectType> void applyObjectClassDefinition(TransformableObjectDefinition<O> objectDefinition,
            PrismObject<O> object, AuthorizationPhaseType phase, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {
        //noinspection unchecked
        PrismObject<ShadowType> shadow = (PrismObject<ShadowType>) object;
        String resourceOid = ShadowUtil.getResourceOid(shadow);
        if (resourceOid != null) {
            PrismObject<ResourceType> resource;
            try {
                resource = provisioning.getObject(ResourceType.class, resourceOid, readOnly(), task, result);
            } catch (CommunicationException | SecurityViolationException | ExpressionEvaluationException e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
            ResourceObjectDefinition resourceObjectDefinition =
                    getEditObjectClassDefinition(shadow, resource, phase, task, result);
            if (resourceObjectDefinition != null) {
                objectDefinition.replaceDefinition(ShadowType.F_ATTRIBUTES,
                        resourceObjectDefinition.toShadowAttributesContainerDefinition());
                objectDefinition.replaceDefinition(ShadowType.F_ASSOCIATIONS,
                        resourceObjectDefinition.toShadowAssociationsContainerDefinition());
            }
        }
    }

    private <O extends ObjectType> void applyArchetypePolicy(
            PrismObjectDefinition<O> objectDefinition, PrismObject<O> object, Task task, OperationResult result)
            throws SchemaException {
        try {
            ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(object, result);
            if (archetypePolicy != null) {

                schemaTransformer.applyItemsConstraints(objectDefinition, archetypePolicy);

                ObjectReferenceType objectTemplateRef = archetypePolicy.getObjectTemplateRef();
                if (objectTemplateRef != null) {
                    PrismObject<ObjectTemplateType> objectTemplate = cacheRepositoryService.getObject(
                            ObjectTemplateType.class, objectTemplateRef.getOid(), readOnly(), result);
                    schemaTransformer.applyObjectTemplateToDefinition(
                            objectDefinition, objectTemplate.asObjectable(), task, result);
                }

            }
        } catch (ConfigurationException | ObjectNotFoundException e) {
            result.recordFatalError(e);
        }
    }

    @Override
    public PrismObjectDefinition<ShadowType> getEditShadowDefinition(
            ResourceShadowCoordinates coordinates,
            AuthorizationPhaseType phase,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {
        // HACK hack hack
        // Make a dummy shadow instance here and evaluate the schema for that. It is not 100% correct. But good enough for now.
        // TODO: refactor when we add better support for multi-tenancy

        PrismObject<ShadowType> shadow = prismContext.createObject(ShadowType.class);
        ShadowType shadowType = shadow.asObjectable();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        if (coordinates != null) {
            resourceRef.setOid(coordinates.getResourceOid());
            shadowType.setResourceRef(resourceRef);
            shadowType.setKind(coordinates.getKind());
            shadowType.setIntent(coordinates.getIntent());
            shadowType.setObjectClass(coordinates.getObjectClass());
        }

        return getEditObjectDefinition(shadow, phase, task, parentResult);
    }

    @Override
    public ResourceObjectDefinition getEditObjectClassDefinition(
            @NotNull PrismObject<ShadowType> shadow,
            @NotNull PrismObject<ResourceType> resource,
            AuthorizationPhaseType phase,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Validate.notNull(resource, "Resource must not be null");

        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        ResourceObjectDefinition rocd = resourceSchema.findDefinitionForShadow(shadow.asObjectable());
        if (rocd == null) {
            LOGGER.debug("No resource object definition for shadow {}, returning null", shadow.getOid());
            return null;
        }

        // We are going to modify attribute definitions list, hence we ask for a mutable instance.
        ResourceObjectDefinition objectDefinition = rocd.forLayerMutable(LayerType.PRESENTATION);

        // TODO: maybe we need to expose owner resolver in the interface?
        ObjectSecurityConstraints securityConstraints =
                securityEnforcer.compileSecurityConstraints(shadow, true, SecurityEnforcer.Options.create(), task, result);
        LOGGER.trace("Security constrains for {}:\n{}", shadow, DebugUtil.debugDumpLazily(securityConstraints));

        AuthorizationDecisionType attributesReadDecision =
                securityConstraints.computeItemDecision(
                        ShadowType.F_ATTRIBUTES,
                        ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET,
                        securityConstraints.findAllItemsDecision(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, phase),
                        phase);
        AuthorizationDecisionType attributesAddDecision =
                securityConstraints.computeItemDecision(
                        ShadowType.F_ATTRIBUTES,
                        ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD,
                        securityConstraints.findAllItemsDecision(ModelAuthorizationAction.ADD.getUrl(), phase),
                        phase);
        AuthorizationDecisionType attributesModifyDecision =
                securityConstraints.computeItemDecision(
                        ShadowType.F_ATTRIBUTES,
                        ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY,
                        securityConstraints.findAllItemsDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase),
                        phase);
        LOGGER.trace("Attributes container access read:{}, add:{}, modify:{}",
                attributesReadDecision, attributesAddDecision, attributesModifyDecision);

        // Let's work on the copied list, as we modify (replace = delete+add) the definitions in the object definition.
        List<? extends ShadowSimpleAttributeDefinition<?>> definitionsCopy =
                new ArrayList<>(objectDefinition.getSimpleAttributeDefinitions());
        for (ShadowSimpleAttributeDefinition<?> rAttrDef : definitionsCopy) {
            ItemPath attributePath = ItemPath.create(ShadowType.F_ATTRIBUTES, rAttrDef.getItemName());
            AuthorizationDecisionType attributeReadDecision =
                    securityConstraints.computeItemDecision(
                            attributePath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, attributesReadDecision, phase);
            AuthorizationDecisionType attributeAddDecision =
                    securityConstraints.computeItemDecision(
                            attributePath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, attributesAddDecision, phase);
            AuthorizationDecisionType attributeModifyDecision =
                    securityConstraints.computeItemDecision(
                            attributePath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, attributesModifyDecision, phase);
            LOGGER.trace("Attribute {} access read:{}, add:{}, modify:{}", rAttrDef.getItemName(), attributeReadDecision,
                    attributeAddDecision, attributeModifyDecision);

            if (attributeReadDecision != AuthorizationDecisionType.ALLOW
                    || attributeAddDecision != AuthorizationDecisionType.ALLOW
                    || attributeModifyDecision != AuthorizationDecisionType.ALLOW) {

                // This opens up flag overriding
                ShadowSimpleAttributeDefinition<?> attrDefClone = rAttrDef.clone();
                if (attributeReadDecision != AuthorizationDecisionType.ALLOW) {
                    attrDefClone.setOverrideCanRead(false);
                }
                if (attributeAddDecision != AuthorizationDecisionType.ALLOW) {
                    attrDefClone.setOverrideCanAdd(false);
                }
                if (attributeModifyDecision != AuthorizationDecisionType.ALLOW) {
                    attrDefClone.setOverrideCanModify(false);
                }
                objectDefinition.replaceAttributeDefinition(rAttrDef.getItemName(), attrDefClone);
            }
        }

        // TODO what about associations, activation and credentials?

        objectDefinition.freeze();
        return objectDefinition;
    }

    @Override
    public <O extends ObjectType> MetadataItemProcessingSpec getMetadataItemProcessingSpec(ItemPath metadataItemPath,
            PrismObject<O> object, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {

        PrismObject<O> fullObject = getFullObjectReadOnly(object, result);
        ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(fullObject, result);
        ObjectReferenceType templateRef = archetypePolicy != null ? archetypePolicy.getObjectTemplateRef() : null;
        MetadataItemProcessingSpecImpl processingSpec = new MetadataItemProcessingSpecImpl(metadataItemPath);
        processingSpec.populateFromObjectTemplate(templateRef, objectResolver,
                "getting items with provenance support for " + object, task, result);

        LOGGER.trace(
                """
                        getMetadataSupportSpec for {} in {}:
                         - archetypePolicy = {}
                         - templateRef = {}
                         - processingSpec =
                        {}""",
                metadataItemPath, object, archetypePolicy, templateRef,
                DebugUtil.debugDumpLazily(processingSpec, 1));

        return processingSpec;
    }

    private @NotNull <O extends ObjectType> PrismObject<O> getFullObjectReadWrite(PrismObject<O> object, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (object.getOid() != null) {
            return cacheRepositoryService.getObject(object.getCompileTimeClass(), object.getOid(), null, result);
        } else {
            return object;
        }
    }

    private @NotNull <O extends ObjectType> PrismObject<O> getFullObjectReadOnly(PrismObject<O> object, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (object.getOid() != null) {
            return cacheRepositoryService.getObject(object.getCompileTimeClass(), object.getOid(), readOnly(), result);
        } else {
            return object;
        }
    }

    @Override
    public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            PrismObject<O> object, PrismObject<R> target, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        return securityEnforcer.getAllowedRequestAssignmentItems(
                securityContextManager.getPrincipal(), ModelAuthorizationAction.ASSIGN.getUrl(), object, target, task, result);
    }

    @Override
    public Collection<? extends DisplayableValue<String>> getActionUrls() {
        return Arrays.asList(ModelAuthorizationAction.values());
    }

    @Override
    public <H extends AssignmentHolderType, R extends AbstractRoleType> RoleSelectionSpecification getAssignableRoleSpecification(
            @NotNull PrismObject<H> focus, Class<R> targetType, int assignmentOrder, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(GET_ASSIGNABLE_ROLE_SPECIFICATION);

        ObjectSecurityConstraints securityConstraints;
        try {
            securityConstraints = securityEnforcer.compileSecurityConstraints(
                    focus, true, SecurityEnforcer.Options.create(), task, result);
        } catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | CommunicationException |
                SecurityViolationException e) {
            result.recordFatalError(e);
            throw e;
        }
        LOGGER.trace("Security constrains for getAssignableRoleSpecification on {}:\n{}",
                focus, securityConstraints.debugDumpLazily(1));

        // Global decisions: processing #modify authorizations: allow/deny for all items or allow/deny for assignment/inducement item.
        ItemPath assignmentPath;
        if (assignmentOrder == 0) {
            assignmentPath = FocusType.F_ASSIGNMENT;
        } else {
            assignmentPath = AbstractRoleType.F_INDUCEMENT;
        }
        AuthorizationDecisionType assignmentItemDecision = securityConstraints.findItemDecision(assignmentPath,
                ModelAuthorizationAction.MODIFY.getUrl(), AuthorizationPhaseType.REQUEST);
        LOGGER.trace("getAssignableRoleSpecification decision for {}:{}", assignmentPath, assignmentItemDecision);
        if (assignmentItemDecision == AuthorizationDecisionType.ALLOW) {
            RoleSelectionSpecification spec = new RoleSelectionSpecification();
            spec.setGlobalFilter(prismContext.queryFactory().createAll());
            result.recordSuccess();
            return spec;
        }
        if (assignmentItemDecision == AuthorizationDecisionType.DENY) {
            result.recordSuccess();
            RoleSelectionSpecification spec = new RoleSelectionSpecification();
            spec.setGlobalFilter(prismContext.queryFactory().createNone());
            return spec;
        }
        AuthorizationDecisionType allItemsDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.MODIFY.getUrl(), AuthorizationPhaseType.REQUEST);
        if (allItemsDecision == AuthorizationDecisionType.ALLOW) {
            RoleSelectionSpecification spec = new RoleSelectionSpecification();
            spec.setGlobalFilter(prismContext.queryFactory().createAll());
            result.recordSuccess();
            return spec;
        }
        if (allItemsDecision == AuthorizationDecisionType.DENY) {
            result.recordSuccess();
            RoleSelectionSpecification spec = new RoleSelectionSpecification();
            spec.setGlobalFilter(prismContext.queryFactory().createNone());
            return spec;
        }

        // Assignment decisions: processing #assign authorizations
        MidPointPrincipal principal = securityEnforcer.getMidPointPrincipal();
        OrderConstraintsType orderConstraints = new OrderConstraintsType();
        orderConstraints.setOrder(assignmentOrder);
        List<OrderConstraintsType> orderConstraintsList = new ArrayList<>(1);
        orderConstraintsList.add(orderConstraints);

        FilterGizmo<RoleSelectionSpecification> gizmo = new FilterGizmoAssignableRoles();

        try {
            return securityEnforcer.computeTargetSecurityFilter(
                    principal,
                    ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ASSIGN,
                    ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH_BY,
                    AuthorizationPhaseType.REQUEST, targetType, focus, prismContext.queryFactory().createAll(), null,
                    orderConstraintsList, gizmo, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public <T extends ObjectType> ObjectFilter getDonorFilter(
            Class<T> searchResultType, ObjectFilter origFilter, String targetAuthorizationAction,
            Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.preProcessObjectFilter(
                securityEnforcer.getMidPointPrincipal(), ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ATTORNEY,
                ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH_BY,
                null, searchResultType, origFilter, targetAuthorizationAction,
                List.of(), SecurityEnforcer.Options.create(), task, parentResult);
    }

    @Override
    public <T extends ObjectType> ObjectFilter getAccessibleForAssignmentObjectsFilter(
            Class<T> searchResultType, ObjectFilter origFilter, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.preProcessObjectFilter(
                securityEnforcer.getMidPointPrincipal(), ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ASSIGN,
                ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH_BY,
                AuthorizationPhaseType.REQUEST, searchResultType, origFilter, null,
                List.of(), SecurityEnforcer.Options.create(), task, parentResult);
    }

    @Override
    public AuthenticationsPolicyType getAuthenticationPolicy(PrismObject<UserType> user, Task task,
            OperationResult parentResult) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        // TODO: check for user membership in an organization (later versions)

        OperationResult result = parentResult.createMinorSubresult(GET_AUTHENTICATIONS_POLICY);
        try {
            return resolvePolicyTypeFromSecurityPolicy(SecurityPolicyType.F_AUTHENTICATION, user, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public RegistrationsPolicyType getFlowPolicy(PrismObject<? extends FocusType> focus, Task task, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        // TODO: check for user membership in an organization (later versions)
        OperationResult result = parentResult.createMinorSubresult(GET_REGISTRATIONS_POLICY);
        try {
            return resolvePolicyTypeFromSecurityPolicy(SecurityPolicyType.F_FLOW, focus, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public CredentialsPolicyType getCredentialsPolicy(PrismObject<? extends FocusType> focus, Task task, OperationResult parentResult) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        // TODO: check for user membership in an organization (later versions)

        OperationResult result = parentResult.createMinorSubresult(GET_CREDENTIALS_POLICY);
        try {
            return resolvePolicyTypeFromSecurityPolicy(SecurityPolicyType.F_CREDENTIALS, focus, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private <C extends Containerable> C resolvePolicyTypeFromSecurityPolicy(
            QName path, PrismObject<? extends FocusType> focus, Task task, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        SecurityPolicyType securityPolicyType = getSecurityPolicy(focus, task, parentResult);
        if (securityPolicyType == null) {
            return null;
        }
        PrismContainer<C> container = securityPolicyType.asPrismObject().findContainer(ItemName.fromQName(path));
        if (container == null) {
            return null;
        }
        PrismContainerValue<C> containerValue = container.getValue();
        return containerValue.asContainerable();
    }

    public <F extends FocusType> NonceCredentialsPolicyType determineNonceCredentialsPolicy(
            PrismObject<F> focus,
            String nonceCredentialName,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException {

        SecurityPolicyType securityPolicy = getSecurityPolicy(focus, task, parentResult);

        if (securityPolicy == null) {
            LOGGER.warn("No security policy, cannot process nonce credential"); //TODO correct level?
            return null;
        }
        if (securityPolicy.getCredentials() == null) {
            LOGGER.warn("No credential for security policy, cannot process nonce credential");
            return null;
        }
        List<NonceCredentialsPolicyType> noncePolicies = securityPolicy.getCredentials().getNonce();
        if (noncePolicies.isEmpty()) {
            LOGGER.warn("No nonce credential for security policy, cannot process nonce credential");
            return null;
        }
        for (NonceCredentialsPolicyType credential : securityPolicy.getCredentials().getNonce()) {
            if (nonceCredentialName.equals(credential.getName())) {
                return credential;
            }
        }
        return null;
    }

    @Override
    public SecurityPolicyType getSecurityPolicy(
            @Nullable PrismObject<? extends FocusType> focus, Task task, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createMinorSubresult(GET_SECURITY_POLICY);
        try {
            var systemConfiguration = systemObjectCache.getSystemConfiguration(result);
            var securityPolicy =
                    modelSecurityPolicyFinder.locateSecurityPolicyForFocus(focus, systemConfiguration, task, result);
            if (securityPolicy == null) {
                result.setNotApplicable("no security policy");
            }
            return securityPolicy;
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    @Override
    public SecurityPolicyType getSecurityPolicyForArchetype(
            @Nullable String archetypeOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        OperationResult result = parentResult.createMinorSubresult(GET_SECURITY_POLICY_FOR_ARCHETYPE);
        try {
            var systemConfiguration = systemObjectCache.getSystemConfiguration(result);
            var securityPolicy =
                    modelSecurityPolicyFinder.locateSecurityPolicyForArchetype(archetypeOid, systemConfiguration, task, result);
            if (securityPolicy == null) {
                result.setNotApplicable("no security policy");
            }
            return securityPolicy;
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    @Override
    public SecurityPolicyType getSecurityPolicy(ResourceObjectDefinition rOCDef, Task task, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(GET_SECURITY_POLICY);
        try {
            SecurityPolicyType securityPolicy = securityPolicyFinder.locateResourceObjectSecurityPolicyLegacy(rOCDef, result);
            if (securityPolicy == null) {
                result.recordNotApplicableIfUnknown();
            }
            return securityPolicy;
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    @Override
    @NotNull
    public CompiledGuiProfile getCompiledGuiProfile(Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = null;
        try {
            principal = securityContextManager.getPrincipal();
        } catch (SecurityViolationException e) {
            LOGGER.warn("Security violation while getting principal to get GUI config: {}", e.getMessage(), e);
        }

        if (!(principal instanceof GuiProfiledPrincipal guiProfiledPrincipal)) {
            // May be used for unauthenticated user, error pages and so on
            return guiProfileCompiler.getGlobalCompiledGuiProfile(task, parentResult);
        } else {
            CompiledGuiProfile profile = guiProfiledPrincipal.getCompiledGuiProfile();
            if (profile.isInvalid()) {
                return guiProfiledPrincipalManager.refreshCompiledProfile(guiProfiledPrincipal);
            }
            return profile;
        }
    }

    @Override
    public List<UserSessionManagementType> getLoggedInPrincipals(Task task, OperationResult result) {
        // TODO some authorization here?
        return clusterwideUserSessionManager.getLoggedInPrincipals(task, result);
    }

    @Override
    public void terminateSessions(TerminateSessionEvent terminateSessionEvent, Task task, OperationResult result) {
        // TODO some authorization here?
        clusterwideUserSessionManager.terminateSessions(terminateSessionEvent, task, result);
    }

    @Override
    public SystemConfigurationType getSystemConfiguration(OperationResult parentResult) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
        if (systemConfiguration == null) {
            return null;
        }
        return systemConfiguration.asObjectable();
    }

    @Override
    public DeploymentInformationType getDeploymentInformationConfiguration(OperationResult parentResult) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
        if (systemConfiguration == null) {
            return null;
        }
        return systemConfiguration.asObjectable().getDeploymentInformation();
    }

    @Override
    public SystemConfigurationAuditType getAuditConfiguration(OperationResult parentResult) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
        if (systemConfiguration == null) {
            return null;
        }
        return systemConfiguration.asObjectable().getAudit();
    }

    @Override
    public List<MergeConfigurationType> getMergeConfiguration(OperationResult parentResult) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
        if (systemConfiguration == null) {
            return null;
        }
        return systemConfiguration.asObjectable().getMergeConfiguration();
    }

    @Override
    public AccessCertificationConfigurationType getCertificationConfiguration(OperationResult parentResult) throws SchemaException {
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
            cmp = protector.compareCleartext(password, currentPassword);
        } catch (EncryptionException e) {
            result.recordFatalError(e);
            throw new SystemException(e.getMessage(), e);
        }
        result.recordSuccess();
        return cmp;
    }

    @Override
    public List<Visualization> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException {

        return visualizer.visualizeDeltas(deltas, task, result);
    }

    @Override
    public <O extends ObjectType> ModelContextVisualization visualizeModelContext(ModelContext<O> context, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        if (context == null) {
            return new ModelContextVisualization();
        }

        final List<ObjectDelta<? extends ObjectType>> primaryDeltas = new ArrayList<>();
        final List<ObjectDelta<? extends ObjectType>> secondaryDeltas = new ArrayList<>();

        final List<ModelProjectionContext> projectionContexts = new ArrayList<>();

        final List<Visualization> primary;
        final List<Visualization> secondary;

        if (context.getFocusContext() != null) {
            addIgnoreNull(primaryDeltas, CloneUtil.clone(context.getFocusContext().getPrimaryDelta()));
            ObjectDelta<O> summarySecondaryDelta = CloneUtil.clone(context.getFocusContext().getSummarySecondaryDelta());
            if (summarySecondaryDelta != null && !summarySecondaryDelta.getModifications().isEmpty()) {
                secondaryDeltas.add(summarySecondaryDelta);
            }
        }

        for (ModelProjectionContext projCtx : context.getProjectionContexts()) {
            ObjectDelta<ShadowType> primaryDelta = CloneUtil.clone(projCtx.getPrimaryDelta());
            addIgnoreNull(primaryDeltas, primaryDelta);

            ObjectDelta executable = CloneUtil.clone(projCtx.getExecutableDelta());
            if (executable != null && !isEquivalentWithoutOperationAttr(primaryDelta, executable)) {
                projectionContexts.add(projCtx);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Primary deltas:\n{}", DebugUtil.debugDump(primaryDeltas));
            LOGGER.trace("Secondary deltas:\n{}", DebugUtil.debugDump(secondaryDeltas));
        }

        primary = visualizeDeltas(primaryDeltas, task, result);
        secondary = visualizeDeltas(secondaryDeltas, task, result);

        List<Visualization> projectionVisualizations = visualizer.visualizeProjectionContexts(projectionContexts, task, result);
        secondary.addAll(projectionVisualizations);

        return new ModelContextVisualization(primary, secondary);
    }

    private boolean isEquivalentWithoutOperationAttr(ObjectDelta<ShadowType> primaryDelta, ObjectDelta<ShadowType> secondaryDelta) {
        if (primaryDelta == null || secondaryDelta == null) {
            return false;
        }

        List<ItemDelta<?, ?>> modifications = new ArrayList<>(secondaryDelta.getModifications());

        for (ItemDelta<?, ?> secondaryModification : modifications) {
            ItemDefinition<?> def = secondaryModification.getDefinition();
            if (def != null && def.isOperational()) {
                secondaryDelta.removeModification(secondaryModification);
            }
        }

        return primaryDelta.equivalent(secondaryDelta);
    }

    @Override
    @NotNull
    public Visualization visualizeDelta(ObjectDelta<? extends ObjectType> delta, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        return visualizer.visualizeDelta(delta, task, result);
    }

    @Override
    @NotNull
    public Visualization visualizeDelta(ObjectDelta<? extends ObjectType> delta, boolean includeOperationalItems, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        return visualizer.visualizeDelta(delta, null, includeOperationalItems, true, task, result);
    }

    @Override
    @NotNull
    public Visualization visualizeDelta(ObjectDelta<? extends ObjectType> delta, boolean includeOperationalItems, boolean includeOriginalObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        return visualizer.visualizeDelta(delta, null, includeOperationalItems, includeOriginalObject, task, result);
    }

    @Override
    @NotNull
    public Visualization visualizeDelta(ObjectDelta<? extends ObjectType> delta, boolean includeOperationalItems, ObjectReferenceType objectRef, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        return visualizer.visualizeDelta(delta, objectRef, task, result);
    }

    @Override
    public List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createMinorSubresult(GET_CONNECTOR_OPERATIONAL_STATUS);
        List<ConnectorOperationalStatus> status;
        try {
            status = provisioning.getConnectorOperationalStatus(resourceOid, task, result);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException e) {
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
    public  <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy,
            Task task, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ValuePolicyType policy = null;

        if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
            PrismObject<ValuePolicyType> valuePolicy = cacheRepositoryService.getObject(
                    ValuePolicyType.class, noncePolicy.getValuePolicyRef().getOid(), readOnly(), result);
            policy = valuePolicy.asObjectable();
        }

        return generateValue(policy,
                24, false, (PrismObject<O>) null, "nonce generation", task, result);
    }

    @Override
    public <O extends ObjectType> String generateValue(
            ValuePolicyType policy, int defaultLength, boolean generateMinimalSize, PrismObject<O> object, String shortDesc,
            Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return policyProcessor.generate(
                null, policy, defaultLength,
                createOriginResolver(object, parentResult),
                shortDesc, task, parentResult);
    }

    @Override
    public <O extends ObjectType> void generateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {

        OperationResult result = parentResult.createSubresult(OPERATION_GENERATE_VALUE);
        try {
            ValuePolicyType valuePolicy;
            try {
                valuePolicy = getValuePolicy(object, task, result);
            } catch (ObjectNotFoundException | SchemaException | CommunicationException
                     | ConfigurationException | SecurityViolationException
                     | ExpressionEvaluationException e) {
                LOGGER.error("Failed to get value policy for generating value. ", e);
                result.recordException("Error while getting value policy. Reason: " + e.getMessage(), e);
                throw e;
            }

            Collection<PropertyDelta<?>> deltasToExecute = new ArrayList<>();
            for (PolicyItemDefinitionType policyItemDefinition : policyItemsDefinition.getPolicyItemDefinition()) {
                OperationResult generateValueResult = result.createSubresult(OPERATION_GENERATE_VALUE);
                try {

                    LOGGER.trace("Default value policy: {}", valuePolicy);
                    try {
                        generateValue(object, valuePolicy, policyItemDefinition, task, generateValueResult);
                    } catch (ExpressionEvaluationException | SchemaException | ObjectNotFoundException
                             | CommunicationException | ConfigurationException | SecurityViolationException e) {
                        LOGGER.error("Failed to generate value for {} ", policyItemDefinition, e);
                        generateValueResult.recordException(
                                "Failed to generate value for " + policyItemDefinition + ". Reason: " + e.getMessage(), e);
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

                    collectDeltasForGeneratedValuesIfNeeded(
                            object, policyItemDefinition, deltasToExecute, path, propertyDef, generateValueResult);
                } finally {
                    generateValueResult.close();
                }
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
                result.recordException("Could not execute deltas for generated values. Reason: " + e.getMessage(), e);
                throw e;
            }
        } catch (Throwable t) {
            result.recordException(t); // just in case it was not recorded inside
            throw t;
        } finally {
            result.close();
        }
    }

    private boolean isExecute(PolicyItemDefinitionType policyItemDefinition) {
        if (policyItemDefinition.isExecute() == null) {
            return false;
        }

        return policyItemDefinition.isExecute().booleanValue();
    }

    private ItemPath getPath(PolicyItemDefinitionType policyItemDefinition) {
        PolicyItemTargetType target = policyItemDefinition.getTarget();
        if (target == null) {
            return null;
        }
        ItemPathType itemPathType = target.getPath();
        return itemPathType != null ? itemPathType.getItemPath() : null;
    }

    private <O extends ObjectType> PrismPropertyDefinition<?> getItemDefinition(PrismObject<O> object, ItemPath path) {
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

        if (itemDef != null) {
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
            PropertyDelta<?> propertyDelta = prismContext.deltaFactory().property()
                    .createModificationReplaceProperty(path, object.getDefinition(), value);
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
        if ((target == null || ItemPathTypeUtil.isEmpty(target.getPath())) && isExecute(policyItemDefinition)) {
            LOGGER.error("Target item path must be defined");
            throw new SchemaException("Target item path must be defined");
        }
        ItemPath targetPath;
        if (target != null) {
            targetPath = target.getPath().getItemPath();
        } else {
            targetPath = null;
        }

        ValuePolicyType valuePolicy = resolveValuePolicy(policyItemDefinition, defaultPolicy, task, result);
        LOGGER.trace("Value policy used for generating new value : {}", valuePolicy);
        StringPolicyType stringPolicy = valuePolicy != null ? valuePolicy.getStringPolicy() : null;
        if (stringPolicy == null) {
            LOGGER.trace("No sting policy defined. Cannot generate value.");
            result.recordFatalError("No string policy defined. Cannot generate value");
            return;
        }

        String newValue = policyProcessor.generate(
                targetPath,
                valuePolicy,
                10,
                createOriginResolver(object, result),
                "generating value for" + targetPath,
                task,
                result);
        policyItemDefinition.setValue(newValue);
    }

    private ValuePolicyType resolveValuePolicy(
            PolicyItemDefinitionType policyItemDefinition, ValuePolicyType defaultPolicy, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (policyItemDefinition.getValuePolicyRef() != null) {
            LOGGER.trace("Trying to resolve value policy {} for policy item definition", policyItemDefinition);
            return objectResolver.resolve(policyItemDefinition.getValuePolicyRef(), ValuePolicyType.class, null,
                    "valuePolicyRef in policyItemDefinition", task, result);
        }

        return defaultPolicy;
    }

    @Override
    public <O extends ObjectType> void validateValue(PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
        ValuePolicyType valuePolicy = getValuePolicy(object, task, parentResult);
        for (PolicyItemDefinitionType policyItemDefinition : policyItemsDefinition.getPolicyItemDefinition()) {
            validateValue(object, valuePolicy, policyItemDefinition, task, parentResult);
        }
    }

    // TODO unify with the other code for policies
    //  - e.g. this code ignores defaults for credentials and does its own "merging" of policies.
    //  Then consider moving to [Model]SecurityPolicyFinder.
    private <O extends ObjectType> ValuePolicyType getValuePolicy(PrismObject<O> object, Task task,
            OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        // user-level policy
        CredentialsPolicyType credentialsPolicy = null;
        if (object != null && object.getCompileTimeClass().isAssignableFrom(UserType.class)) {
            LOGGER.trace("Start to resolve policy for user");
            //noinspection unchecked
            credentialsPolicy = getCredentialsPolicy((PrismObject<UserType>) object, task, result);
            LOGGER.trace("Resolved user policy: {}", credentialsPolicy);
        }

        SystemConfigurationType systemConfigurationType = getSystemConfiguration(result);
        if (!containsValuePolicyDefinition(credentialsPolicy)) {
            var securityPolicy =
                    securityPolicyFinder.locateGlobalSecurityPolicy(systemConfigurationType.asPrismObject(), true, result);
            if (securityPolicy != null) {
                credentialsPolicy = securityPolicy.getCredentials();
                LOGGER.trace("Resolved policy from global security policy: {}", credentialsPolicy);
            }
        }

        if (containsValuePolicyDefinition(credentialsPolicy)) {
            if (credentialsPolicy.getPassword().getValuePolicyRef() != null) {
                return objectResolver.resolve(credentialsPolicy.getPassword().getValuePolicyRef(), ValuePolicyType.class, null, "valuePolicyRef in password credential policy", task, result);
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

        return false;
    }

    private <T, O extends ObjectType> boolean validateValue(PrismObject<O> object, ValuePolicyType policy, PolicyItemDefinitionType policyItemDefinition, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {

        ValuePolicyType stringPolicy = resolveValuePolicy(policyItemDefinition, policy, task, parentResult);

        Object value = policyItemDefinition.getValue();
        String valueToValidate;
        if (value instanceof RawType) {
            valueToValidate = ((RawType) value).getParsedRealValue(String.class);
        } else {
            valueToValidate = (String) value;
        }

        List<String> valuesToValidate = new ArrayList<>();
        PolicyItemTargetType target = policyItemDefinition.getTarget();
        ItemPath path = target != null ? target.getPath().getItemPath() : null;
        if (StringUtils.isNotEmpty(valueToValidate)) {
            valuesToValidate.add(valueToValidate);
        } else {
            if (target == null || target.getPath() == null) {
                LOGGER.error("Target item path must be defined");
                parentResult.recordFatalError("Target item path must be defined");
                throw new SchemaException("Target item path must be defined");
            }
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

                } else if (definitionName.equals(ProtectedStringType.COMPLEX_TYPE)) {
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
            if (path != null) {
                result.addParam("path", path.toString());
            }
            result.addParam("valueToValidate", newValue);

            ObjectValuePolicyEvaluator.Builder evaluatorBuilder = new ObjectValuePolicyEvaluator.Builder()
                    .valuePolicy(stringPolicy)
                    .valuePolicyProcessor(policyProcessor)
                    .protector(protector)
                    .valueItemPath(path)
                    .originResolver(getOriginResolver(object))
                    .task(task)
                    .shortDesc(" rest validate ");
            O objectable = object != null ? object.asObjectable() : null;
            if (path != null && objectable instanceof FocusType) {
                //noinspection unchecked
                PrismObject<? extends FocusType> focus = (PrismObject<? extends FocusType>) object;
                if (path.isSuperPathOrEquivalent(SchemaConstants.PATH_PASSWORD)) {
                    evaluatorBuilder.securityPolicy(getSecurityPolicy(focus, task, parentResult));
                    PrismContainer<PasswordType> passwordContainer = focus.findContainer(SchemaConstants.PATH_PASSWORD);
                    PasswordType password = passwordContainer != null ? passwordContainer.getValue().asContainerable() : null;
                    evaluatorBuilder.oldCredential(password);
                } else if (path.isSuperPathOrEquivalent(SchemaConstants.PATH_SECURITY_QUESTIONS)) {
                    LOGGER.trace("Setting security questions related policy.");
                    SecurityPolicyType securityPolicy = getSecurityPolicy(focus, task, parentResult);
                    evaluatorBuilder.securityPolicy(securityPolicy);
                    PrismContainer<SecurityQuestionsCredentialsType> securityQuestionsContainer =
                            focus.findContainer(SchemaConstants.PATH_SECURITY_QUESTIONS);
                    SecurityQuestionsCredentialsType securityQuestions = securityQuestionsContainer != null ?
                            securityQuestionsContainer.getValue().asContainerable() : null;
                    //evaluatorBuilder.oldCredentialType(securityQuestions);        // TODO what with this?
                    evaluatorBuilder.valuePolicy(resolveSecurityQuestionsPolicy(securityPolicy, task, parentResult));
                }
            }
            evaluatorBuilder.now(clock.currentTimeXMLGregorianCalendar());
            LOGGER.trace("Validating value started");
            evaluatorBuilder.build().validateStringValue(newValue, result);
            LOGGER.trace("Validating value finished");

            result.computeStatus();
        }

        parentResult.computeStatus();
        policyItemDefinition.setResult(parentResult.createOperationResultType());

        return parentResult.isAcceptable();
    }

    private ValuePolicyType resolveSecurityQuestionsPolicy(SecurityPolicyType securityPolicy, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (securityPolicy == null) {
            return null;
        }

        CredentialsPolicyType credentialsPolicy = securityPolicy.getCredentials();
        if (credentialsPolicy == null) {
            return null;
        }

        SecurityQuestionsCredentialsPolicyType securityQuestionsPolicy = credentialsPolicy.getSecurityQuestions();
        if (securityQuestionsPolicy == null) {
            return null;
        }

        ObjectReferenceType policyRef = securityQuestionsPolicy.getValuePolicyRef();
        if (policyRef == null) {
            return null;
        }

        return objectResolver.resolve(policyRef, ValuePolicyType.class, null, " resolve value policy for security questions", task, result);
    }

    private <O extends ObjectType> ObjectBasedValuePolicyOriginResolver<?> getOriginResolver(PrismObject<O> object) {
        if (object != null && UserType.class.equals(object.getCompileTimeClass())) {
            return new FocusValuePolicyOriginResolver<>((PrismObject<UserType>) object, objectResolver);
        }

        //TODO not supported yet, throw exception instead of null???
        return null;
    }

    private <O extends ObjectType> ObjectBasedValuePolicyOriginResolver<?> createOriginResolver(PrismObject<O> object, OperationResult result) throws SchemaException {
        if (object == null) {
            return null;
        }
        if (object.canRepresent(FocusType.class)) {
            return new FocusValuePolicyOriginResolver<>((PrismObject<FocusType>) object, objectResolver);
        }
        if (object.canRepresent(ShadowType.class)) {
            return new ShadowValuePolicyOriginResolver((PrismObject<ShadowType>) object, objectResolver);
        }
        SchemaException e = new SchemaException("Unsupport object type " + object);
        result.recordFatalError(e);
        throw e;
    }

    private boolean isSupportedType(QName type) {

        if (QNameUtil.qNameToUri(type).equals(QNameUtil.qNameToUri(DOMUtil.XSD_STRING))) {
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
        if (protectedString == null) {
            return null;
        }
        try {
            if (protectedString.isEncrypted() || protectedString.isExternal()) {

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
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
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
            RepositoryCache.exitLocalCaches();
        }
    }

    @NotNull
    @Override
    public List<ObjectReferenceType> getDeputyAssignees(
            ObjectReferenceType assigneeRef,
            OtherPrivilegesLimitations.Type limitationType,
            Task task,
            OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(GET_DEPUTY_ASSIGNEES);
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {
            Set<String> oidsToSkip = new HashSet<>();
            oidsToSkip.add(assigneeRef.getOid());
            List<ObjectReferenceType> deputies = new ArrayList<>();
            getDeputyAssigneesNoWorkItem(deputies, assigneeRef, limitationType, oidsToSkip, task, result);
            return deputies;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            RepositoryCache.exitLocalCaches();
        }
    }

    private void getDeputyAssignees(
            List<ObjectReferenceType> deputies, AbstractWorkItemType workItem, Set<String> oidsToSkip,
            Task task, OperationResult result) throws SchemaException {
        List<PrismReferenceValue> assigneeReferencesToQuery = workItem.getAssigneeRef().stream()
                .map(assigneeRef -> assigneeRef.clone().relation(PrismConstants.Q_ANY).asReferenceValue())
                .collect(Collectors.toList());
        if (assigneeReferencesToQuery.isEmpty()) {
            return;
        }
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_DELEGATED_REF).ref(assigneeReferencesToQuery)
                .build();
        SearchResultList<PrismObject<UserType>> potentialDeputies = cacheRepositoryService
                .searchObjects(UserType.class, query, null, result); // TODO consider read-only here
        for (PrismObject<UserType> potentialDeputy : potentialDeputies) {
            if (oidsToSkip.contains(potentialDeputy.getOid())) {
                continue;
            }
            // [EP:APSO] DONE potential deputy is from repository
            if (determineDeputyValidity(
                    potentialDeputy, workItem.getAssigneeRef(), workItem, OtherPrivilegesLimitations.Type.CASES, task, result)) {
                deputies.add(ObjectTypeUtil.createObjectRefWithFullObject(potentialDeputy));
                oidsToSkip.add(potentialDeputy.getOid());
            }
        }
    }

    private void getDeputyAssigneesNoWorkItem(
            List<ObjectReferenceType> deputies, ObjectReferenceType assigneeRef,
            OtherPrivilegesLimitations.Type limitationType, Set<String> oidsToSkip,
            Task task, OperationResult result) throws SchemaException {
        PrismReferenceValue assigneeReferenceToQuery = assigneeRef.clone().relation(PrismConstants.Q_ANY).asReferenceValue();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_DELEGATED_REF).ref(assigneeReferenceToQuery)
                .build();
        SearchResultList<PrismObject<UserType>> potentialDeputies = cacheRepositoryService
                .searchObjects(UserType.class, query, null, result); // TODO consider read-only here
        for (PrismObject<UserType> potentialDeputy : potentialDeputies) {
            if (oidsToSkip.contains(potentialDeputy.getOid())) {
                continue;
            }
            // [EP:APSO] DONE, potential deputy is from repository
            if (determineDeputyValidity(
                    potentialDeputy, List.of(assigneeRef), null, limitationType, task, result)) {
                deputies.add(ObjectTypeUtil.createObjectRefWithFullObject(potentialDeputy));
                oidsToSkip.add(potentialDeputy.getOid());
            }
        }
    }

    /**
     * Potential deputy must be from the repository.
     *
     * [EP:APSO] DONE it is so, currently
     */
    private boolean determineDeputyValidity(
            PrismObject<UserType> potentialDeputy, // [EP:APSO] DONE 2/2 verified that the object is from repository
            List<ObjectReferenceType> assignees,
            @Nullable AbstractWorkItemType workItem,
            @NotNull OtherPrivilegesLimitations.Type limitationType,
            Task task,
            OperationResult result) {
        AssignmentEvaluator.Builder<UserType> builder =
                new AssignmentEvaluator.Builder<UserType>()
                        .referenceResolver(referenceResolver)
                        .focusOdo(new ObjectDeltaObject<>(potentialDeputy, null, potentialDeputy, potentialDeputy.getDefinition()))
                        .now(clock.currentTimeXMLGregorianCalendar())
                        .loginMode(true)
                        // We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
                        // will need something to push on the stack. So give them context placeholder.
                        .lensContext(new LensContextPlaceholder<>(potentialDeputy, task.getExecutionMode()));
        AssignmentEvaluator<UserType> assignmentEvaluator = builder.build();

        UserType potentialDeputyBean = potentialDeputy.asObjectable();
        for (AssignmentType assignmentBean : potentialDeputyBean.getAssignment()) {
            if (!DeputyUtils.isDelegationAssignment(assignmentBean, relationRegistry)) {
                continue;
            }
            try {
                ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi =
                        new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(assignmentBean));
                // TODO some special mode for verification of the validity - we don't need complete calculation here!
                EvaluatedAssignment assignment = assignmentEvaluator
                        .evaluate(
                                assignmentIdi, null,
                                PlusMinusZero.ZERO, false,
                                potentialDeputyBean, potentialDeputy.toString(),
                                AssignmentOrigin.inObject(embedded(assignmentBean)), // [EP:APSO] DONE from object from repo
                                task, result);
                if (!assignment.isValid()) {
                    continue;
                }
                for (EvaluatedAssignmentTarget target : assignment.getRoles().getNonNegativeValues()) { // MID-6403
                    String targetOid = target.getTarget().getOid();
                    if (targetOid != null
                            && ObjectTypeUtil.containsOid(assignees, targetOid)
                            && target.getAssignmentPath().containsDelegation()
                            && target.getAssignmentPath().getOtherPrivilegesLimitation().allows(limitationType)) {
                        return true;
                    }
                }
            } catch (CommonException e) {
                LoggingUtils.logUnexpectedException(
                        LOGGER, "Couldn't verify 'deputy' relation between {} and {} for work item {}; assignment: {}",
                        e, potentialDeputy, assignees, workItem, assignmentBean);
            }
        }
        return false;
    }

    @Override
    public ActivationStatusType getAssignmentEffectiveStatus(String lifecycleStatus, ActivationType activationType) {
        return activationComputer.getEffectiveStatus(lifecycleStatus, activationType, null);
    }

    @Override
    public MidPointPrincipal assumePowerOfAttorney(PrismObject<? extends FocusType> donor, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        MidPointPrincipal attorneyPrincipal = securityContextManager.getPrincipal();
        MidPointPrincipal donorPrincipal = securityEnforcer.createDonorPrincipal(
                attorneyPrincipal, ModelAuthorizationAction.ATTORNEY.getUrl(), donor, task, result);

        // TODO: audit switch
        Authentication authentication = securityContextManager.getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            ((MidpointAuthentication) authentication).setPrincipal(donorPrincipal);
            ((MidpointAuthentication) authentication).setCredential(null);
            ((MidpointAuthentication) authentication).setAuthorities(donorPrincipal.getAuthorities());
        } else {
            securityContextManager.setupPreAuthenticatedSecurityContext(donorPrincipal);
        }

        return donorPrincipal;
    }

    @Override
    public MidPointPrincipal dropPowerOfAttorney(Task task, OperationResult result) throws SecurityViolationException {
        MidPointPrincipal donorPrincipal = securityContextManager.getPrincipal();
        if (donorPrincipal.getAttorney() == null) {
            throw new IllegalStateException("Attempt to drop attorney powers using non-donor principal " + donorPrincipal);
        }
        MidPointPrincipal previousPrincipal = donorPrincipal.getPreviousPrincipal();
        if (previousPrincipal == null) {
            throw new IllegalStateException("Attempt to drop attorney powers, but no previous principal in " + donorPrincipal);
        }

        // TODO: audit switch

        // TODO: maybe refresh previous principal using userProfileService?
        Authentication authentication = securityContextManager.getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            ((MidpointAuthentication) authentication).setPrincipal(previousPrincipal);
            ((MidpointAuthentication) authentication).setCredential(null);
            ((MidpointAuthentication) authentication).setAuthorities(previousPrincipal.getAuthorities());
        } else {
            securityContextManager.setupPreAuthenticatedSecurityContext(previousPrincipal);
        }

        return previousPrincipal;
    }

    @Override
    public <T> T runUnderPowerOfAttorney(Producer<T> producer,
            PrismObject<? extends FocusType> donor, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        assumePowerOfAttorney(donor, task, result);
        try {
            return producer.run();
        } finally {
            dropPowerOfAttorney(task, result);
        }
    }

    @Override
    @NotNull
    public LocalizableMessageType createLocalizableMessageType(
            LocalizableMessageTemplateType template, VariablesMap variables, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        VariablesMap vars = new VariablesMap();
        vars.putAll(variables);
        return LensExpressionUtil.interpretLocalizableMessageTemplate(template, vars, null, task, result);
    }

    @Override
    public ExecuteCredentialResetResponseType executeCredentialsReset(PrismObject<UserType> user,
            ExecuteCredentialResetRequestType executeCredentialResetRequest, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        LocalizableMessageBuilder builder = new LocalizableMessageBuilder();

        ExecuteCredentialResetResponseType response = new ExecuteCredentialResetResponseType();

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

        if (!credentialsResetPolicyMatch(resetPolicyType, resetMethod)) {
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
            userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class, user.getOid(),
                    SchemaConstants.PATH_PASSWORD_VALUE, newProtectedPassword);

        }

        if (BooleanUtils.isTrue(resetPolicyType.isForceChange())) {
            if (userDelta != null) {
                userDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_FORCE_CHANGE, Boolean.TRUE);
            }
        }

        try {
            Collection<ObjectDeltaOperation<? extends ObjectType>> result = modelService.executeChanges(
                    MiscUtil.createCollection(userDelta), ModelExecuteOptions.create().raw(), task, parentResult);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException |
                PolicyViolationException e) {
            response.message(LocalizationUtil.createForFallbackMessage("Failed to reset credential: " + e.getMessage()));
            throw e;
        }

        parentResult.recomputeStatus();
        LocalizableMessage message = builder.fallbackMessage("Reset password was successful").key("execute.reset.credential.successful").fallbackLocalizableMessage(null).build();
        response.setMessage(LocalizationUtil.createLocalizableMessageType(message));

        return response;
    }

    private boolean credentialsResetPolicyMatch(CredentialsResetPolicyType policy, String identifier) {
        return StringUtils.equals(policy.getIdentifier(), identifier);
    }

    @Override
    public void refreshPrincipal(String oid, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        try {
            // For refreshing current logged-in principal, we need to support GUI config
            MidPointPrincipal principal = guiProfiledPrincipalManager.getPrincipalByOid(
                    oid, clazz, ProfileCompilerOptions.create());
            Authentication authentication = securityContextManager.getAuthentication();
            if (authentication instanceof MidpointAuthentication) {
                ((MidpointAuthentication) authentication).setPrincipal(principal);
                ((MidpointAuthentication) authentication).setAuthorities(principal.getAuthorities());
            } else {
                securityContextManager.setupPreAuthenticatedSecurityContext(principal);
            }
        } catch (Throwable e) {
            LOGGER.error("Cannot refresh authentication for user identified with" + oid);
            throw e;
        }
    }

    @Override
    public List<RelationDefinitionType> getRelationDefinitions() {
        return relationRegistry.getRelationDefinitions();
    }

    @NotNull
    @Override
    public TaskType submitTaskFromTemplate(String templateTaskOid, List<Item<?, ?>> extensionItems, Task opTask, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        OperationResult result = parentResult.createMinorSubresult(SUBMIT_TASK_FROM_TEMPLATE);
        try {
            var principalRef = SecurityUtil.getPrincipalRequired().toObjectReference();
            TaskType newTask = modelService.getObject(
                    TaskType.class, templateTaskOid, createCollection(createExecutionPhase()), opTask, result)
                    .asObjectable();
            newTask.setName(PolyStringType.fromOrig(newTask.getName().getOrig() + " " + (int) (Math.random() * 10000)));
            newTask.setOid(null);
            newTask.setTaskIdentifier(null);
            newTask.setOwnerRef(principalRef);
            newTask.setExecutionState(RUNNABLE);
            newTask.setSchedulingState(READY);
            for (Item<?, ?> extensionItem : extensionItems) {
                newTask.asPrismObject().getOrCreateExtension()
                        .add(extensionItem.clone());
            }
            ObjectDelta<TaskType> taskAddDelta = DeltaFactory.Object.createAddDelta(newTask.asPrismObject());
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges = modelService.executeChanges(singleton(taskAddDelta), null, opTask, result);
            String newTaskOid = ObjectDeltaOperation.findAddDeltaOid(executedChanges, newTask.asPrismObject());
            newTask.setOid(newTaskOid);
            newTask.setTaskIdentifier(newTaskOid);
            result.computeStatus();
            return newTask;
        } catch (Throwable t) {
            result.recordFatalError("Couldn't submit task from template: " + t.getMessage(), t);
            throw t;
        }
    }

    @NotNull
    @Override
    public TaskType submitTaskFromTemplate(String templateTaskOid, Map<QName, Object> extensionValues, Task opTask, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        PrismContainerDefinition<?> extDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(TaskType.class).findContainerDefinition(TaskType.F_EXTENSION);
        List<Item<?, ?>> extensionItems = ObjectTypeUtil.mapToExtensionItems(extensionValues, extDef, prismContext);
        return submitTaskFromTemplate(templateTaskOid, extensionItems, opTask, parentResult);
    }

    @Override
    public @NotNull String submitTaskFromTemplate(
            @NotNull String templateOid,
            @NotNull ActivityCustomization customization,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommonException {
        OperationResult result = parentResult.createMinorSubresult(SUBMIT_TASK_FROM_TEMPLATE);
        try {
            TaskType template =
                    cacheRepositoryService
                            .getObject(TaskType.class, templateOid, null, result)
                            .asObjectable();

            securityEnforcer.authorize(
                    ModelAuthorizationAction.USE.getUrl(),
                    null,
                    AuthorizationParameters.forObject(template),
                    task, result);

            template.setName(PolyStringType.fromOrig(template.getName().getOrig() + " " + (int) (Math.random() * 10000)));
            template.setOid(null);
            template.setTaskIdentifier(null);
            template.setOwnerRef(null);

            ActivityDefinitionType activityDefinition = customization.applyTo(template);
            return submit(
                    activityDefinition,
                    ActivitySubmissionOptions.create().withTaskTemplate(template),
                    task, result);

        } catch (Throwable t) {
            result.recordFatalError("Couldn't submit task from template: " + t.getMessage(), t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public <O extends AssignmentHolderType> ArchetypePolicyType determineArchetypePolicy(PrismObject<O> assignmentHolder, OperationResult result) throws SchemaException, ConfigurationException {
        return archetypeManager.determineArchetypePolicy(assignmentHolder, result);
    }

    private ArchetypeType determineArchetype(PrismObject<? extends AssignmentHolderType> assignmentHolder, OperationResult result)
            throws SchemaException {
        return archetypeManager.determineStructuralArchetype(assignmentHolder.asObjectable(), result);
    }

    @Override
    public ArchetypePolicyType mergeArchetypePolicies(PrismObject<ArchetypeType> archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        return archetypeManager.getPolicyForArchetype(asObjectable(archetype), result);
    }

    @Override
    public <O extends AssignmentHolderType> AssignmentCandidatesSpecification determineAssignmentTargetSpecification(PrismObject<O> object, OperationResult result) throws SchemaException {
        SearchResultList<PrismObject<ArchetypeType>> archetypes = systemObjectCache.getAllArchetypes(result);
        List<AssignmentObjectRelation> assignmentTargetRelations = new ArrayList<>();
        for (PrismObject<ArchetypeType> archetype : archetypes) {
            List<QName> archetypeFocusTypes = null;
            for (AssignmentType inducement : archetype.asObjectable().getInducement()) {
                for (AssignmentRelationType assignmentRelation : inducement.getAssignmentRelation()) {
                    if (canBeAssignmentHolder(assignmentRelation, object)) {
                        if (archetypeFocusTypes == null) {
                            archetypeFocusTypes = determineArchetypeFocusTypes(archetype);
                        }
                        AssignmentObjectRelation targetRelation = new AssignmentObjectRelation();
                        targetRelation.addObjectTypes(archetypeFocusTypes);
                        targetRelation.addArchetypeRef(archetype);
                        targetRelation.addRelations(assignmentRelation.getRelation());
                        targetRelation.setDescription(assignmentRelation.getDescription());
                        assignmentTargetRelations.add(targetRelation);
                    }
                }
            }
        }

        AssignmentCandidatesSpecification spec = new AssignmentCandidatesSpecification();
        spec.setAssignmentObjectRelations(assignmentTargetRelations);
        // TODO: empty list vs null: default setting
        return spec;
    }

    @Override
    public <O extends AssignmentHolderType> List<ArchetypeType> getFilteredArchetypesByHolderType(
            Class<O> objectType, OperationResult result) throws SchemaException {
        SearchResultList<PrismObject<ArchetypeType>> archetypes = systemObjectCache.getAllArchetypes(result);
        List<ArchetypeType> filteredArchetypes = new ArrayList<>();
        for (PrismObject<ArchetypeType> archetype : archetypes) {
            for (AssignmentType assignment : archetype.asObjectable().getAssignment()) {
                for (AssignmentRelationType assignmentRelation : assignment.getAssignmentRelation()) {
                    if (isHolderType(assignmentRelation.getHolderType(), objectType)) {
                        filteredArchetypes.add(archetype.asObjectable());
                    }
                }
                if (filteredArchetypes.contains(archetype.asObjectable())) {
                    break;
                }
            }
        }
        return filteredArchetypes;
    }

    @Override
    public <O extends AssignmentHolderType> List<ArchetypeType> getFilteredArchetypesByHolderType(
            PrismObject<O> object, OperationResult result) throws SchemaException {
        return getFilteredArchetypesByHolderType(object.getCompileTimeClass(), result);
    }

    @Override
    public <O extends AbstractRoleType> AssignmentCandidatesSpecification determineAssignmentHolderSpecification(PrismObject<O> assignmentTarget, OperationResult result)
            throws SchemaException {

        if (assignmentTarget == null) {
            return null;
        }

        // assignmentRelation statements in the assignment - we want to control what objects can be assigned to the archetype definition
        if (ArchetypeType.class.isAssignableFrom(assignmentTarget.getCompileTimeClass())) {
            ArchetypeType archetypeType = (ArchetypeType) assignmentTarget.asObjectable();
            return determineArchetypeAssignmentCandidateSpecification(archetypeType.getAssignment(), archetypeType.getArchetypePolicy());
        }

        // apply assignmentRelation to "archetyped" objects
        ArchetypeType targetArchetype = determineArchetype(assignmentTarget, result);
        if (targetArchetype == null) {
            return null;
        }

        // TODO: empty list vs null: default setting
        // TODO what about super-archetypes?
        return determineArchetypeAssignmentCandidateSpecification(
                targetArchetype.getInducement(), targetArchetype.getArchetypePolicy());
    }

    private AssignmentCandidatesSpecification determineArchetypeAssignmentCandidateSpecification(List<AssignmentType> archetypeAssigmentsOrInducements, ArchetypePolicyType archetypePolicy) {
        AssignmentCandidatesSpecification spec = new AssignmentCandidatesSpecification();
        List<AssignmentObjectRelation> assignmentHolderRelations = new ArrayList<>();
        for (AssignmentType inducement : archetypeAssigmentsOrInducements) {
            for (AssignmentRelationType assignmentRelation : inducement.getAssignmentRelation()) {
                AssignmentObjectRelation holderRelation = new AssignmentObjectRelation();
                holderRelation.addObjectTypes(ObjectTypes.canonizeObjectTypes(assignmentRelation.getHolderType()));
                holderRelation.addArchetypeRefs(assignmentRelation.getHolderArchetypeRef());
                holderRelation.addRelations(assignmentRelation.getRelation());
                holderRelation.setDescription(assignmentRelation.getDescription());
                assignmentHolderRelations.add(holderRelation);
            }
        }
        spec.setAssignmentObjectRelations(assignmentHolderRelations);
        spec.setSupportGenericAssignment(archetypePolicy == null
                || AssignmentRelationApproachType.CLOSED != archetypePolicy.getAssignmentHolderRelationApproach());
        return spec;
    }

    private List<QName> determineArchetypeFocusTypes(PrismObject<ArchetypeType> archetype) {
        List<QName> focusTypes = new ArrayList<>();
        for (AssignmentType assignment : archetype.asObjectable().getAssignment()) {
            for (AssignmentRelationType assignmentRelation : assignment.getAssignmentRelation()) {
                focusTypes.addAll(ObjectTypes.canonizeObjectTypes(assignmentRelation.getHolderType()));
            }
        }
        if (focusTypes.isEmpty()) {
            focusTypes.add(AssignmentHolderType.COMPLEX_TYPE);
        }
        return focusTypes;
    }

    private <O extends AssignmentHolderType> boolean canBeAssignmentHolder(AssignmentRelationType assignmentRelation, PrismObject<O> holder) {
        return isHolderType(assignmentRelation.getHolderType(), holder.getCompileTimeClass())
                && isHolderArchetype(assignmentRelation.getHolderArchetypeRef(), holder);
    }

    private <O extends AssignmentHolderType> boolean isHolderType(List<QName> requiredHolderTypes, Class<O> holderType) {
        if (requiredHolderTypes.isEmpty()) {
            return true;
        }
        for (QName requiredHolderType : requiredHolderTypes) {
            if (MiscSchemaUtil.canBeAssignedFrom(requiredHolderType, holderType)) {
                return true;
            }
        }
        return false;
    }

    private <O extends AssignmentHolderType> boolean isHolderArchetype(List<ObjectReferenceType> requiredHolderArchetypeRefs, PrismObject<O> holder) {
        if (requiredHolderArchetypeRefs.isEmpty()) {
            return true;
        }
        List<ObjectReferenceType> presentHolderArchetypeRefs = holder.asObjectable().getArchetypeRef();
        for (ObjectReferenceType requiredHolderArchetypeRef : requiredHolderArchetypeRefs) {
            if (MiscSchemaUtil.contains(presentHolderArchetypeRefs, requiredHolderArchetypeRef)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Experimental
    @NotNull
    public Collection<EvaluatedPolicyRule> evaluateCollectionPolicyRules(
            @NotNull PrismObject<ObjectCollectionType> collection, // [EP:APSO] DONE 1/1
            @Nullable CompiledObjectCollectionView preCompiledView,
            @Nullable Class<? extends ObjectType> targetTypeClass,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return collectionProcessor.evaluateCollectionPolicyRules(collection, preCompiledView, targetTypeClass, task, result);
    }

    @Override
    @Experimental
    @NotNull
    public CompiledObjectCollectionView compileObjectCollectionView(@NotNull CollectionRefSpecificationType collectionRef, @Nullable Class<? extends Containerable> targetTypeClass, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        return collectionProcessor.compileObjectCollectionView(collectionRef, targetTypeClass, task, result);
    }

    @Override
    @Experimental
    @NotNull
    public CollectionStats determineCollectionStats(
            @NotNull CompiledObjectCollectionView collectionView, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        return collectionProcessor.determineCollectionStats(collectionView, task, result);
    }

    @Override
    public void applyView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        collectionProcessor.compileView(existingView, objectListViewType);
    }

    @Override
    public void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewsType, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        collectionProcessor.compileView(existingView, objectListViewsType, task, result);
    }

    @Override
    public <O extends ObjectType> List<StringLimitationResult> validateValue(
            ProtectedStringType protectedStringValue,
            ValuePolicyType valuePolicy,
            PrismObject<O> object,
            Task task,
            OperationResult result)
            throws SchemaException, PolicyViolationException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return policyProcessor.validateValue(
                getClearValue(protectedStringValue),
                valuePolicy,
                createOriginResolver(object, result),
                "validate string",
                task,
                result);
    }

    @Override
    public <T> SearchSpec<T> getSearchSpecificationFromCollection(CompiledObjectCollectionView compiledCollection,
            QName typeForFilter, Collection<SelectorOptions<GetOperationOptions>> defaultOptions, VariablesMap variables,
            Task task, OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        SearchSpec<T> searchSpec = new SearchSpec<>();

        Class<T> type;
        if (typeForFilter != null) {
            type = prismContext.getSchemaRegistry().determineClassForType(typeForFilter);
        } else {
            type = null;
        }

        if (compiledCollection != null) {
            searchSpec.type = determineTypeForSearch(compiledCollection, typeForFilter);
            searchSpec.query = parseFilterFromCollection(compiledCollection, variables, null, task, result);
            searchSpec.options = determineOptionsForSearch(compiledCollection, defaultOptions);
        } else {
            searchSpec.type = type;
            searchSpec.query = null;
            searchSpec.options = defaultOptions;
        }

        if (AuditEventRecordType.class.equals(type)) {
            searchSpec.query = checkOrdering(searchSpec.query, AuditEventRecordType.F_TIMESTAMP);
        } else if (type != null && ObjectType.class.isAssignableFrom(type)) {
            searchSpec.query = checkOrdering(searchSpec.query, ObjectType.F_NAME);
        }
        return searchSpec;
    }

    private ObjectQuery checkOrdering(ObjectQuery query, ItemPath defaultOrderBy) {
        if (query != null) {
            if (query.getPaging() == null) {
                ObjectPaging paging = ObjectQueryUtil.convertToObjectPaging(new PagingType());
                paging.setOrdering(defaultOrderBy, OrderDirection.ASCENDING);
                query.setPaging(paging);
            } else if (query.getPaging().getPrimaryOrderingPath() == null) {
                query.getPaging().setOrdering(defaultOrderBy, OrderDirection.ASCENDING);
            }
            return query;
        } else {
            return prismContext.queryFactory().createQuery(
                    prismContext.queryFactory().createPaging(defaultOrderBy, OrderDirection.ASCENDING));
        }
    }

    private void processContainerByHandler(SearchResultList<? extends Containerable> containers, Predicate<PrismContainer> handler) throws SchemaException {
        for (Containerable container : containers) {
            PrismContainerValue prismValue = container.asPrismContainerValue();
            PrismContainer prismContainer = prismValue.asSingleValuedContainer(prismValue.getTypeName());
            if (!handler.test(prismContainer)) {
                return;
            }
        }
    }

    @Override
    public List<? extends Serializable> searchObjectsFromCollection(
            CollectionRefSpecificationType collectionConfig, QName typeForFilter,
            Collection<SelectorOptions<GetOperationOptions>> defaultOptions, ObjectPaging usedPaging,
            VariablesMap variables, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Class<? extends Containerable> type = null;

        if (collectionConfig.getCollectionRef() != null && collectionConfig.getCollectionRef().getOid() != null && collectionConfig.getFilter() != null) {
            LOGGER.error("CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
            throw new IllegalArgumentException("CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
        }
        if (typeForFilter != null) {
            type = prismContext.getSchemaRegistry().determineClassForType(typeForFilter);
        }
        CompiledObjectCollectionView compiledCollection = compileObjectCollectionView(collectionConfig, type, task, task.getResult());
        ObjectQuery query = parseFilterFromCollection(compiledCollection, variables, usedPaging, task, result);
        type = determineTypeForSearch(compiledCollection, typeForFilter);
        Collection<SelectorOptions<GetOperationOptions>> options = determineOptionsForSearch(compiledCollection, defaultOptions);

        if (AuditEventRecordType.class.equals(type)) {
            return modelAuditService.searchObjects(query, options, task, result);
        } else if (ObjectType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            SearchResultList<PrismObject<ObjectType>> results = modelService.searchObjects(
                    (Class<ObjectType>) type, query, options, task, result);
            List<Containerable> list = new ArrayList<>();
            results.forEach(object -> list.add(object.asObjectable()));
            return list;
        } else if (Referencable.class.isAssignableFrom(type)) {
            return modelService.searchReferences(query, options, task, result);
        } else {
            return modelService.searchContainers(type, query, options, task, result);
        }
    }

    @Override
    public Integer countObjectsFromCollection(CollectionRefSpecificationType collectionConfig, QName typeForFilter,
            Collection<SelectorOptions<GetOperationOptions>> defaultOptions, ObjectPaging usedPaging, VariablesMap variables, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Class<? extends Containerable> type = null;

        if (collectionConfig.getCollectionRef() != null && collectionConfig.getCollectionRef().getOid() != null && collectionConfig.getFilter() != null) {
            LOGGER.error("CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
            throw new IllegalArgumentException("CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
        }
        if (typeForFilter != null) {
            type = prismContext.getSchemaRegistry().determineClassForType(typeForFilter);
        }
        CompiledObjectCollectionView compiledCollection = compileObjectCollectionView(collectionConfig, type, task, task.getResult());
        ObjectQuery query = parseFilterFromCollection(compiledCollection, variables, null, task, result);
        type = determineTypeForSearch(compiledCollection, typeForFilter);
        Collection<SelectorOptions<GetOperationOptions>> options = determineOptionsForSearch(compiledCollection, defaultOptions);
        return countObjectsFromCollectionByType(type, query, options, task, result);
    }

    private Integer countObjectsFromCollectionByType(Class<? extends Containerable> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException {
        if (AuditEventRecordType.class.equals(type)) {
            return modelAuditService.countObjects(query, options, task, result);
        } else if (ObjectType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return modelService.countObjects((Class<ObjectType>) type, query, options, task, result);
        } else if (Referencable.class.isAssignableFrom(type)) {
            return modelService.countReferences(query, options, task, result);
        }
        return modelService.countContainers(type, query, options, task, result);
    }

    private Collection<SelectorOptions<GetOperationOptions>> determineOptionsForSearch(
            CompiledObjectCollectionView compiledCollection,
            Collection<SelectorOptions<GetOperationOptions>> defaultOptions) {
        if (compiledCollection.getOptions() == null) {
            return defaultOptions;
        }
        return compiledCollection.getOptions();
    }

    private <T> Class<T> determineTypeForSearch(CompiledObjectCollectionView compiledCollection, QName typeForFilter) throws ConfigurationException {
        Class<T> targetClass = compiledCollection.getTargetClass();
        if (targetClass != null) {
            return targetClass;
        }
        if (typeForFilter == null) {
            throw new ConfigurationException("Type of objects is null");
        }
        return prismContext.getSchemaRegistry().determineClassForType(typeForFilter);
    }

    private ObjectQuery parseFilterFromCollection(CompiledObjectCollectionView compiledCollection, VariablesMap variables,
            ObjectPaging usedPaging, Task task, OperationResult result) throws ConfigurationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ObjectNotFoundException {
        ObjectFilter filter = ExpressionUtil.evaluateFilterExpressions(
                compiledCollection.getFilter(), variables, MiscSchemaUtil.getExpressionProfile(),
                expressionFactory, "collection filter", task, result);
        if (filter == null) {
            LOGGER.warn("Couldn't find filter");
        }

        ObjectQuery query = prismContext.queryFactory().createQuery();
        query.setPaging(usedPaging == null ? ObjectQueryUtil.convertToObjectPaging(compiledCollection.getPaging()) : usedPaging);
        query.setFilter(filter);
        return query;
    }

    @Override
    public void expandConfigurationObject(
            @NotNull PrismObject<? extends ObjectType> configurationObject,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        provisioning.expandConfigurationObject(configurationObject, task, result);
    }

    @Override
    public @NotNull String submit(
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull ActivitySubmissionOptions options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommonException {
        OperationResult result = parentResult.createSubresult(OP_SUBMIT);
        try {
            return new ActivityExecutor(activityDefinition, options, task)
                    .submit(result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull TaskType createExecutionTask(
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull ActivitySubmissionOptions options,
            @NotNull Task task,
            @NotNull OperationResult result) throws CommonException {
        return new ActivityExecutor(activityDefinition, options, task)
                .createExecutionTask();
    }

    @Override
    public PrismContainerDefinition<AssignmentType> assignmentTypeDefinitionWithConcreteTargetRefType(
            PrismContainerDefinition<AssignmentType> orig, QName targetType) {
        var transformed = TransformableContainerDefinition.of(orig);
        var targetRef = TransformableReferenceDefinition.of(orig.getComplexTypeDefinition().findReferenceDefinition(AssignmentType.F_TARGET_REF));
        targetRef.setTargetTypeName(targetType);
        transformed.getComplexTypeDefinition().replaceDefinition(AssignmentType.F_TARGET_REF, targetRef);
        return transformed;
    }

    @Override
    public PrismReferenceDefinition refDefinitionWithConcreteTargetRefType(PrismReferenceDefinition orig, QName targetType) {
        var transformed = TransformableReferenceDefinition.of(orig);
        transformed.setTargetTypeName(targetType);
        return transformed;
    }

    @Override
    public <X> X executeWithSimulationResult(
            @NotNull TaskExecutionMode mode,
            @Nullable SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull SimulationResultManager.SimulatedFunctionCall<X> functionCall) throws CommonException {
        return simulationResultManager.executeWithSimulationResult(mode, simulationDefinition, task, result, functionCall);
    }

    @Override
    public void authorizeBulkActionExecution(
            @Nullable BulkAction action,
            @Nullable AuthorizationPhaseType phase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        bulkActionsExecutor.authorizeBulkActionExecution(action, phase, task, result);
    }

    public void applyDefinitions(ShadowType shadow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
    }

    public boolean isOfArchetype(AssignmentHolderType assignmentHolderType, String archetypeOid, OperationResult result) throws SchemaException, ConfigurationException {
        return archetypeManager.isOfArchetype(assignmentHolderType, archetypeOid, result);
    }

    @Override
    public boolean isSubarchetypeOrArchetype(String archetypeOid, String parentArchetype, OperationResult result){
        try {
            return archetypeManager.isSubArchetypeOrArchetype(archetypeOid, parentArchetype, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check if archetype {} is subarchetype of {}", e, archetypeOid, parentArchetype);
            throw new SystemException(e);
        }
    }

    @Override
    public @NotNull List<ArchetypeType> determineArchetypes(@Nullable ObjectType object, OperationResult result)
            throws SchemaException {
        return archetypeManager.determineArchetypes(object, result);
    }
}
