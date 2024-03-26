/*
 * Copyright (C) 2015-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.isDiscoveryAllowed;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.function.Supplier;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.AttributesToReturnProvider;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Context for provisioning operations. Contains key information like resolved resource,
 * object type and/or object class definitions, and so on.
 *
 * @author semancik
 */
public class ProvisioningContext implements DebugDumpable, ExecutionModeProvider {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningContext.class);

    /**
     * Task in the context of which the current operation is executed.
     */
    @NotNull private final Task task;

    /**
     * Resource we work with.
     *
     * Before midPoint 4.5 we resolved this field lazily. However, virtually all provisioning operations
     * require the resource to be resolved. Because the resolution process requires {@link OperationResult}
     * and can throw a lot of exceptions, it is much simpler to require the resolution to be done beforehand.
     */
    @NotNull private final ResourceType resource;

    /**
     * Type of objects that are to be processed by the current operation.
     *
     * If this is a bulk operation (like search or live sync), this also drives its scope - i.e. whether to
     * access the whole resource, an object class, or an object type.
     *
     * Note: the definition is always attached to the {@link #resource}.
     */
    @Nullable private final ResourceObjectDefinition resourceObjectDefinition;

    /**
     * If true, we want to process the whole object class even if {@link #resourceObjectDefinition} points to
     * a specific object type. This is used when the client specifies e.g. a search over object class of `inetOrgPerson`
     * and we have to apply a default type definition of `account/default` to know how to process objects of this class.
     *
     * If the {@link #resourceObjectDefinition} is not a type definition, this flag is ignored.
     *
     * This is a hack! The correct way how to specify this in the configuration is to use object class refinement,
     * instead of a default object type.
     *
     * If `null`, the option should not be needed. If it is, an (internal) error is signalled.
     */
    private final Boolean wholeClass;

    /**
     * The context factory.
     *
     * It gives us some useful beans, as well as methods helping with spawning new sub-contexts.
     */
    @NotNull private final ProvisioningContextFactory contextFactory;

    /**
     * {@link GetOperationOptions} for the current `get` or `search` operation.
     */
    private Collection<SelectorOptions<GetOperationOptions>> getOperationOptions;

    /**
     * Are we currently executing a propagation operation?
     */
    private boolean propagation;

    /**
     * Cached connector instances.
     */
    @NotNull private final Map<Class<? extends CapabilityType>, ConnectorInstance> connectorMap = new HashMap<>();

    /**
     * Cached resource schema.
     */
    private ResourceSchema resourceSchema;

    /**
     * Cached patterns for protected objects in given object type with their filter expressions evaluated.
     */
    private Collection<ResourceObjectPattern> evaluatedProtectedObjectPatterns;

    /** TODO document */
    private ObjectReferenceType associationShadowRef;

    private ProvisioningOperationContext operationContext;

    /** Creating context from scratch. */
    ProvisioningContext(
            @NotNull Task task,
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            Boolean wholeClass,
            @NotNull ProvisioningContextFactory contextFactory) {
        this.task = task;
        this.resource = resource;
        ResourceObjectDefinition.assertAttached(resourceObjectDefinition);
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.wholeClass = wholeClass;
        this.contextFactory = contextFactory;
        LOGGER.trace("Created {}", this);
    }

    /** Creating context from previous one (potentially overriding some parts). */
    ProvisioningContext(
            @NotNull ProvisioningContext originalCtx,
            @NotNull Task task,
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            Boolean wholeClass) {
        this.task = task;
        this.resource = originalCtx.resource;
        ResourceObjectDefinition.assertAttached(resourceObjectDefinition);
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.wholeClass = wholeClass;
        this.contextFactory = originalCtx.contextFactory;
        this.connectorMap.putAll(originalCtx.connectorMap);
        this.resourceSchema = originalCtx.resourceSchema;
        this.getOperationOptions = originalCtx.getOperationOptions; // OK?
        this.propagation = originalCtx.propagation;
        this.operationContext = originalCtx.operationContext;
        // Not copying protected account patters because these are object type specific.
        LOGGER.trace("Created/spawned {}", this);
    }

    public ProvisioningOperationContext getOperationContext() {
        return operationContext;
    }

    public ProvisioningContext setOperationContext(ProvisioningOperationContext operationContext) {
        this.operationContext = operationContext;
        return this;
    }

    public void setGetOperationOptions(Collection<SelectorOptions<GetOperationOptions>> getOperationOptions) {
        this.getOperationOptions = getOperationOptions;
    }

    public boolean isPropagation() {
        return propagation;
    }

    public void setPropagation(boolean value) {
        this.propagation = value;
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @NotNull ObjectReferenceType getResourceRef() {
        return ObjectTypeUtil.createObjectRef(resource);
    }

    public @NotNull ResourceSchema getResourceSchema() throws SchemaException, ConfigurationException {
        if (resourceSchema == null) {
            resourceSchema = ProvisioningUtil.getResourceSchema(resource);
        }
        return resourceSchema;
    }

    public @Nullable ResourceObjectDefinition getObjectDefinition() {
        return resourceObjectDefinition;
    }

    public @NotNull ResourceObjectDefinition getObjectDefinitionRequired() {
        return Objects.requireNonNull(
                resourceObjectDefinition,
                () -> "No resource object definition, because the context is wildcard: " + this);
    }

    public Boolean getWholeClass() {
        return wholeClass;
    }

    public @NotNull QName getObjectClassNameRequired() {
        return getObjectDefinitionRequired().getObjectClassName();
    }

    private @Nullable QName getObjectClassNameIfKnown() {
        return resourceObjectDefinition != null ? resourceObjectDefinition.getObjectClassName() : null;
    }

    /**
     * Returns the "raw" object class definition (if the context is not wildcard).
     *
     * TODO must be raw? Or may be refined?
     */
    public @Nullable ResourceObjectClassDefinition getObjectClassDefinition() {
        if (resourceObjectDefinition != null) {
            return resourceObjectDefinition.getObjectClassDefinition();
        } else {
            return null;
        }
    }

    /**
     * Returns evaluated protected object patterns.
     */
    public Collection<ResourceObjectPattern> getProtectedAccountPatterns(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        if (evaluatedProtectedObjectPatterns != null) {
            return evaluatedProtectedObjectPatterns;
        }

        evaluatedProtectedObjectPatterns = new ArrayList<>();

        ResourceObjectDefinition objectDefinition = getObjectDefinitionRequired();
        Collection<ResourceObjectPattern> rawPatterns = objectDefinition.getProtectedObjectPatterns();
        for (ResourceObjectPattern rawPattern : rawPatterns) {
            ObjectFilter filter = rawPattern.getFilter();
            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);
            variables.put(ExpressionConstants.VAR_CONFIGURATION,
                    getResourceManager().getSystemConfiguration(), SystemConfigurationType.class);
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(
                    filter, variables, MiscSchemaUtil.getExpressionProfile(), contextFactory.getCommonBeans().expressionFactory,
                     "protected filter", getTask(), result);
            evaluatedProtectedObjectPatterns.add(
                    new ResourceObjectPattern(
                            rawPattern.getObjectDefinition(),
                            evaluatedFilter));
        }

        return evaluatedProtectedObjectPatterns;
    }

    // we don't use additionalAuxiliaryObjectClassQNames as we don't know if they are initialized correctly [med] TODO: reconsider this
    public @NotNull ResourceObjectDefinition computeCompositeObjectDefinition(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<QName> auxObjectClassQNames)
            throws SchemaException, ConfigurationException {
        if (auxObjectClassQNames.isEmpty()) {
            return objectDefinition;
        }
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>(auxObjectClassQNames.size());
        for (QName auxObjectClassQName : auxObjectClassQNames) {
            ResourceObjectDefinition auxObjectClassDef = getResourceSchema().findObjectClassDefinition(auxObjectClassQName);
            if (auxObjectClassDef == null) {
                throw new SchemaException("Auxiliary object class " + auxObjectClassQName + " specified in " + this + " does not exist");
            }
            auxiliaryObjectClassDefinitions.add(auxObjectClassDef);
        }
        return CompositeObjectDefinition.of(objectDefinition, auxiliaryObjectClassDefinitions);
    }

    /**
     * Returns either real composite type definition, or just object definition - if that's not possible.
     *
     * USES the assumed definition from this context.
     *
     * TODO reconsider this method.
     */
    public @NotNull ResourceObjectDefinition computeCompositeObjectDefinition(@NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {
        return computeCompositeObjectDefinition(getObjectDefinitionRequired(), shadow.getAuxiliaryObjectClass());
    }

    public String getChannel() {
        return task.getChannel();
    }

    public <T extends CapabilityType> ConnectorInstance getConnector(Class<T> operationCapabilityClass, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ConnectorInstance connector = connectorMap.get(operationCapabilityClass);
        if (connector != null) {
            return connector;
        }

        ConnectorInstance newConnector = getConnectorInstance(operationCapabilityClass, result);
        connectorMap.put(operationCapabilityClass, newConnector);
        return newConnector;
    }

    public boolean isWildcard() {
        return resourceObjectDefinition == null;
    }

    /**
     * Creates a context for a different object type on the same resource.
     *
     * The returned context is based on "refined" resource type definition.
     */
    public @NotNull ProvisioningContext spawnForKindIntent(
            @NotNull ShadowKindType kind,
            @NotNull String intent)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForKindIntent(this, kind, intent);
    }

    /**
     * Creates an exact copy of the context but with different task.
     */
    public @NotNull ProvisioningContext spawn(Task task) {
        // No need to bother the factory because no population resolution is needed
        return new ProvisioningContext(
                this,
                task,
                resourceObjectDefinition,
                wholeClass);
    }

    /** A convenience method for {@link #spawn(Task)} */
    public @NotNull ProvisioningContext spawnIfNeeded(Task task) {
        if (task == this.task) {
            return this;
        } else {
            return spawn(task);
        }
    }

    /**
     * Creates an exact copy of the context but with different task + resource object class.
     */
    public @NotNull ProvisioningContext spawnForObjectClass(@NotNull Task task, @NotNull QName objectClassName)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForObjectClass(this, task, objectClassName, false);
    }

    /**
     * Creates an exact copy of the context but with different resource object class.
     */
    public @NotNull ProvisioningContext spawnForObjectClass(@NotNull QName objectClassName)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForObjectClass(this, task, objectClassName, false);
    }

    /**
     * Creates an exact copy of the context but with different resource object class.
     *
     * This method looks for the "real" object class definition (i.e. not a default object type
     * definition for given object class name).
     */
    public @NotNull ProvisioningContext spawnForObjectClassWithClassDefinition(@NotNull QName objectClassName)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForObjectClass(this, task, objectClassName, true);
    }

    /**
     * Creates a context for a different shadow on the same resource.
     */
    public ProvisioningContext spawnForShadow(ShadowType shadow)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForShadow(this, shadow);
    }

    /**
     * Creates a context for a (presumably refined) definition on the same resource.
     */
    public ProvisioningContext spawnForDefinition(@NotNull ResourceObjectDefinition newDefinition) {
        return new ProvisioningContext(this, task, newDefinition, wholeClass);
    }

    /**
     * Creates a wildcard context (but only if needed - hence it's not named "spawnWildcard").
     * This is to avoid mistakenly using a wrong object definition.
     */
    public ProvisioningContext toWildcard() {
        if (isWildcard()) {
            return this;
        } else {
            return new ProvisioningContext(this, task, null, null);
        }
    }

    public void assertWildcard() {
        stateCheck(isWildcard(), "Provisioning context must be a wildcard one: %s", this);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasDefinition() {
        return resourceObjectDefinition != null;
    }

    public void assertDefinition(String message) throws SchemaException {
        if (!hasDefinition()) {
            throw new SchemaException(message + " " + getDesc());
        }
    }

    public void assertDefinition() throws SchemaException {
        assertDefinition("Cannot locate object type or class definition");
    }

    public String getDesc() {
        if (resourceObjectDefinition != null) {
            return "for " + resourceObjectDefinition + " in " + resource;
        } else {
            return "for all objects in " + resource;
        }
    }

    private <T extends CapabilityType> ConnectorInstance getConnectorInstance(
            Class<T> operationCapabilityClass, OperationResult parentResult)
            throws CommunicationException, ConfigurationException {
        OperationResult result =
                parentResult.createMinorSubresult(ProvisioningContext.class.getName() + ".getConnectorInstance");
        try {
            return contextFactory.getResourceManager()
                    .getConfiguredConnectorInstance(resource, operationCapabilityClass, false, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            result.recordPartialError("Could not get connector instance " + getDesc() + ": " + e.getMessage(), e);
            // Wrap those exceptions to a configuration exception. In the context of the provisioning operation we really cannot throw
            // ObjectNotFoundException exception. If we do that then the consistency code will interpret that as if the resource object
            // (shadow) is missing. But that's wrong. We do not have connector therefore we do not know anything about the shadow. We cannot
            // throw ObjectNotFoundException here.
            throw new ConfigurationException(e.getMessage(), e);
        } catch (CommunicationException | ConfigurationException | RuntimeException e) {
            result.recordPartialError("Could not get connector instance " + getDesc() + ": " + e.getMessage(), e);
            throw e;
        } finally {
            result.close();
        }
    }

    /**
     * Gets a specific capability, looking in this order:
     *
     * 1. take additional connector capabilities if exist, if not, take resource capabilities,
     * 2. apply object class specific capabilities to the one selected in step 1,
     * 3. in the returned capabilities, check first configured capabilities and then native capabilities.
     *
     * TODO check if the clients assume that the returned capability is enabled
     */
    public <T extends CapabilityType> T getCapability(@NotNull Class<T> capabilityClass) {
        return getResourceManager().getCapability(
                resource, getObjectDefinition(), capabilityClass);
    }

    public <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass) {
        T capability = getCapability(capabilityClass);
        return CapabilityUtil.isCapabilityEnabled(capability) ? capability : null;
    }

    public boolean hasCapability(@NotNull Class<? extends CapabilityType> capabilityClass) {
        return getEnabledCapability(capabilityClass) != null;
    }

    public <C extends CapabilityType> void checkForCapability(Class<C> capabilityClass) {
        if (!hasCapability(capabilityClass)) {
            throw new UnsupportedOperationException(
                    String.format("Operation not supported %s as %s is missing", getDesc(), capabilityClass.getSimpleName()));
        }
    }

    public boolean hasReadCapability() {
        return hasCapability(ReadCapabilityType.class);
    }

    public boolean hasRealReadCapability() {
        return hasReadCapability() && !isReadingCachingOnly();
    }

    public boolean isReadingCachingOnly() {
        ReadCapabilityType readCapability = getEnabledCapability(ReadCapabilityType.class);
        if (readCapability == null) {
            return false; // TODO reconsider this
        } else {
            return Boolean.TRUE.equals(readCapability.isCachingOnly());
        }
    }

    @Override
    public String toString() {
        return "ProvisioningContext(" + getDesc() + ")";
    }

    public ItemPath path(Object... components) {
        return ItemPath.create(components);
    }

    public boolean shouldStoreAttributeInShadow(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ResourceAttributeDefinition<?> attrDef) {
        ItemName attrName = attrDef.getItemName();
        if (objectDefinition.isIdentifier(attrName)) {
            return true;
        }
        if (Boolean.FALSE.equals(getExplicitCachingStatus())) {
            return false;
        }
        if (isReadCachingOnlyCapabilityPresent()) {
            return true;
        }
        return attrDef.isEffectivelyCached(objectDefinition);
    }

    private Boolean getExplicitCachingStatus() {
        if (resourceObjectDefinition != null) {
            var objectLevel = resourceObjectDefinition.getEffectiveShadowCachingPolicy().getCachingStrategy();
            if (objectLevel == CachingStrategyType.NONE) {
                return false;
            } else if (objectLevel == CachingStrategyType.PASSIVE) {
                return true;
            } else if (objectLevel != null) {
                throw new AssertionError(objectLevel);
            }
        } else {
            // No object definition, we must go to the resource level
            ShadowCachingPolicyType resourceCaching = resource.getCaching();
            var resourceLevel = resourceCaching != null ? resourceCaching.getCachingStrategy() : null;
            if (resourceLevel == CachingStrategyType.NONE) {
                return false;
            } else if (resourceLevel == CachingStrategyType.PASSIVE) {
                return true;
            } else if (resourceLevel != null) {
                throw new AssertionError(resourceLevel);
            }
        }

        return null;
    }

    private boolean isReadCachingOnlyCapabilityPresent() {
        ReadCapabilityType readCapability = getEnabledCapability(ReadCapabilityType.class);
        return readCapability != null && Boolean.TRUE.equals(readCapability.isCachingOnly());
    }

    public boolean isCachingEnabled() {
        return Objects.requireNonNullElseGet(
                getExplicitCachingStatus(),
                this::isReadCachingOnlyCapabilityPresent);
    }

    public boolean isReadCachingOnlyCapabilityDisabled() {
        return isReadCachingOnlyCapabilityPresent() && !Boolean.FALSE.equals(getExplicitCachingStatus());
    }

    public String toHumanReadableDescription() {
        if (resourceObjectDefinition != null) {
            return resourceObjectDefinition.getHumanReadableName() + " @" + resource;
        } else {
            return "all objects @" + resource;
        }
    }

    public boolean isInMaintenance() {
        return ResourceTypeUtil.isInMaintenance(resource);
    }

    /**
     * Returns `true` if the definition of the current resource object is in "production" lifecycle state
     * (`active` or `deprecated`). This determines the behavior of some processing components, as described in
     * https://docs.evolveum.com/midpoint/devel/design/simulations/simulated-shadows/.
     */
    public boolean isObjectDefinitionInProduction() {
        if (!SimulationUtil.isVisible(resource, TaskExecutionMode.PRODUCTION)) {
            return false;
        }
        if (resourceObjectDefinition == null) {
            // Resource is in production, and we have no further information.
            throw new IllegalStateException(
                    "Asked for production state of the object definition, but there is no object definition: " + this);
        } else {
            return SimulationUtil.isVisible(resourceObjectDefinition, TaskExecutionMode.PRODUCTION);
        }
    }

    public void checkNotInMaintenance() throws MaintenanceException {
        ResourceTypeUtil.checkNotInMaintenance(resource);
    }

    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public @NotNull TaskExecutionMode getExecutionMode() {
        return task.getExecutionMode();
    }

    public UcfExecutionContext getUcfExecutionContext() {
        return new UcfExecutionContext(
                contextFactory.getLightweightIdentifierGenerator(),
                resource,
                task);
    }

    public boolean canRun() {
        return !(task instanceof RunningTask) || ((RunningTask) task).canRun();
    }

    public @NotNull String getResourceOid() {
        return Objects.requireNonNull(
                resource.getOid());
    }

    public @Nullable CachingStrategyType getPasswordCachingStrategy() {
        return ProvisioningUtil.getPasswordCachingStrategy(
                getObjectDefinitionRequired());
    }

    public void validateSchemaIfConfigured(ShadowType shadow) throws SchemaException {
        if (ResourceTypeUtil.isValidateSchema(resource)) {
            ShadowUtil.validateAttributeSchema(shadow, resourceObjectDefinition);
        }
    }

    private @NotNull ResourceManager getResourceManager() {
        return contextFactory.getResourceManager();
    }

    /**
     * Returns association definitions, or an empty list if we do not have appropriate definition available.
     */
    public @NotNull Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions() {
        return resourceObjectDefinition != null ?
                resourceObjectDefinition.getAssociationDefinitions() : List.of();
    }

    // TODO consider removal
    public @NotNull Collection<? extends ShadowAssociationDefinition> getVisibleAssociationDefinitions() {
        return getAssociationDefinitions().stream()
                .filter(def -> def.isVisible(this))
                .toList();
    }

    // TODO consider removal
    public @NotNull Collection<? extends ShadowAssociationDefinition> getVisibleSimulatedAssociationDefinitions() {
        return getAssociationDefinitions().stream()
                .filter(def -> def.isVisible(this))
                .filter(def -> def.isSimulated())
                .toList();
    }

    public <T> @Nullable ResourceAttributeDefinition<T> findAttributeDefinition(QName name) throws SchemaException {
        return resourceObjectDefinition != null ? resourceObjectDefinition.findAttributeDefinition(name) : null;
    }

    public <T> @NotNull ResourceAttributeDefinition<T> findAttributeDefinitionRequired(QName name) throws SchemaException {
        return getObjectDefinitionRequired().findAttributeDefinitionRequired(name);
    }

    public <T> @NotNull ResourceAttributeDefinition<T> findAttributeDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        return getObjectDefinitionRequired()
                .findAttributeDefinitionRequired(name, contextSupplier);
    }

    public @NotNull ShadowAssociationDefinition findAssociationDefinitionRequired(QName name) throws SchemaException {
        return findAssociationDefinitionRequired(name, () -> " in " + this);
    }

    public @NotNull ShadowAssociationDefinition findAssociationDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        return getObjectDefinitionRequired()
                .findAssociationDefinitionRequired(name, contextSupplier);
    }

    /**
     * It's more logical to call this method right on {@link ProvisioningContext}. The exact placement of the implementation
     * is to be decided yet.
     */
    public AttributesToReturn createAttributesToReturn() {
        return new AttributesToReturnProvider(resource, getObjectDefinitionRequired(), getOperationOptions)
                .createAttributesToReturn();
    }

    /** Beware! Creates a new context based on the shadow kind/intent/OC. */
    public ProvisioningContext applyDefinitionInNewCtx(@NotNull PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException {
        return applyDefinitionInNewCtx(shadow.asObjectable());
    }

    /** Beware! Creates a new context based on the shadow kind/intent/OC. TODO check if not redundant! */
    public ProvisioningContext applyDefinitionInNewCtx(@NotNull ResourceObject resourceObject)
            throws SchemaException, ConfigurationException {
        return applyDefinitionInNewCtx(resourceObject.getBean());
    }

    /** Beware! Creates a new context based on the shadow kind/intent/OC. TODO check if not redundant! */
    public ProvisioningContext applyDefinitionInNewCtx(@NotNull RepoShadow repoShadow)
            throws SchemaException, ConfigurationException {
        return applyDefinitionInNewCtx(repoShadow.getBean());
    }

    /** Beware! Creates a new context based on the shadow kind/intent/OC. */
    public ProvisioningContext applyDefinitionInNewCtx(@NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {
        ProvisioningContext subContext = spawnForShadow(shadow);
        subContext.assertDefinition();
        subContext.applyCurrentDefinition(shadow);
        return subContext;
    }

    /** Does not create a new context. The current context should be derived from the shadow. TODO reconsider */
    public void applyCurrentDefinition(@NotNull ShadowType shadow) throws SchemaException {
        new ShadowDefinitionApplicator(getObjectDefinitionRequired())
                .applyTo(shadow);
    }

    /**
     * Takes kind/intent/OC from the shadow, looks up the definition, and updates the attribute definitions accordingly.
     *
     * TODO reconsider
     *
     * TODO what about the shadow state?
     */
    private ProvisioningContext adoptShadowBean(@NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {
        return applyDefinitionInNewCtx(shadow);
    }

    /**
     * Creates a well-formed {@link RepoShadow} from provided raw shadow bean. The embedded {@link RawRepoShadow}
     * is not filled in - to avoid object cloning if not really necessary.
     *
     * TODO reconsider
     *
     * @see #adoptRawRepoShadow(PrismObject)
     */
    public @NotNull RepoShadow adoptRawRepoShadowSimple(@NotNull PrismObject<ShadowType> shadowPrismObject)
            throws SchemaException, ConfigurationException {
        @NotNull ShadowType shadowBean = shadowPrismObject.asObjectable();
        var shadowCtx = adoptShadowBean(shadowBean);
        shadowCtx.updateShadowState(shadowBean);
        return RepoShadow.of(shadowBean, null, shadowCtx.getResource());
    }

    /** TODO */
    public @NotNull RepoShadow adoptRawRepoShadow(@NotNull ShadowType bean)
            throws SchemaException, ConfigurationException {
        return adoptRawRepoShadow(RawRepoShadow.of(bean));
    }

    /** TODO */
    public @NotNull RepoShadow adoptRawRepoShadow(@NotNull PrismObject<ShadowType> prismObject)
            throws SchemaException, ConfigurationException {
        return adoptRawRepoShadow(RawRepoShadow.of(prismObject));
    }

    /** TODO */
    public @NotNull RepoShadow adoptRawRepoShadow(@NotNull RawRepoShadow rawRepoShadow)
            throws SchemaException, ConfigurationException {
        var shadowBean = rawRepoShadow.getBean().clone();
        var shadowCtx = adoptShadowBean(shadowBean);
        shadowCtx.updateShadowState(shadowBean);
        return RepoShadow.of(shadowBean, rawRepoShadow, shadowCtx.getResource());
    }

    /** The shadow should be a bean usable as a {@link ResourceObject} (except for the attribute definitions). */
    public @NotNull ResourceObject adoptNotYetExistingResourceObject(ShadowType bean)
            throws SchemaException, ConfigurationException {
        var shadowCtx = adoptShadowBean(bean);
        return ResourceObject.fromBean(bean, false, shadowCtx.getObjectDefinitionRequired());
    }

    public void applyCurrentDefinition(@NotNull ObjectDelta<ShadowType> delta) throws SchemaException {
        new ShadowDefinitionApplicator(getObjectDefinitionRequired())
                .applyTo(delta);
    }

    public void applyCurrentDefinition(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) throws SchemaException {
        new ShadowDefinitionApplicator(getObjectDefinitionRequired())
                .applyTo(modifications);
    }

    public void updateShadowState(ShadowType shadow) {
        ShadowLifecycleStateDeterminer.updateShadowState(this, shadow);
    }

    public void updateShadowState(RepoShadow shadow) {
        ShadowLifecycleStateDeterminer.updateShadowState(this, shadow.getBean());
    }

    public ShadowLifecycleStateType determineShadowState(ShadowType shadow) {
        return ShadowLifecycleStateDeterminer.determineShadowState(this, shadow);
    }

    public boolean isAllowNotFound() {
        return GetOperationOptions.isAllowNotFound(
                SelectorOptions.findRootOptions(getOperationOptions));
    }

    public boolean shouldExecuteResourceOperationDirectly() {
        if (propagation) {
            return true;
        } else {
            ResourceConsistencyType consistency = resource.getConsistency();
            return consistency == null || consistency.getOperationGroupingInterval() == null;
        }
    }

    public boolean shouldUseProposedShadows() {
        ResourceConsistencyType consistency = resource.getConsistency();
        return consistency != null && BooleanUtils.isTrue(consistency.isUseProposedShadows());
    }

    public @NotNull ShadowCheckType getShadowConstraintsCheck() {
        return ResourceTypeUtil.getShadowConstraintsCheck(resource);
    }

    public boolean shouldDoDiscoveryOnGet() {
        return isDiscoveryAllowed(resource)
                && !GetOperationOptions.isDoNotDiscovery(getOperationOptions);
    }

    public FetchErrorReportingMethodType getErrorReportingMethod() {
        return GetOperationOptions.getErrorReportingMethod(
                SelectorOptions.findRootOptions(getOperationOptions));
    }

    public boolean isProductionConfigurationTask() {
        return task.getExecutionMode().isProductionConfiguration();
    }

    /**
     * This is a check that we are not going to cause any modification on a resource.
     *
     * This method should be called before any modifying connector operation, as well as before any such operation is called
     * at the level of provisioning module itself - to ensure that e.g. no changes are queued for maintenance mode or
     * operation grouping scenarios.
     *
     * @see UcfExecutionContext#checkExecutionFullyPersistent()
     */
    public void checkExecutionFullyPersistent() {
        if (!isExecutionFullyPersistent()) {
            LOGGER.error("MidPoint tried to execute an operation on {}. This is unexpected, as the task is running in simulation"
                            + " mode ({}). Please report this as a bug. Resource object definition: {}",
                    resource, task.getExecutionMode(), resourceObjectDefinition);
            throw new IllegalStateException("Invoking 'modifying' provisioning operation while being in a simulation mode");
        }
    }

    /** Convenience method for {@link #getExceptionDescription(ConnectorInstance)}. */
    public String getExceptionDescription() {
        return getExceptionDescription(null);
    }

    public @NotNull Object getExceptionDescriptionLazy() {
        return lazy(() -> getExceptionDescription());
    }

    /** Provides basic information about the context in which an exception occurred. See MID-6712. */
    public String getExceptionDescription(ConnectorInstance connector) {
        StringBuilder sb = new StringBuilder();
        sb.append(getOrig(resource.getName()));
        if (resourceObjectDefinition != null) {
            sb.append(": ");
            ResourceObjectTypeDefinition typeDefinition = resourceObjectDefinition.getTypeDefinition();
            if (typeDefinition != null) {
                String typeName = typeDefinition.getDisplayName();
                if (typeName != null) {
                    sb.append(typeName);
                } else {
                    sb.append(typeDefinition.getTypeIdentification());
                }
                // TODO consider using object class display name, if present
                sb.append(" (").append(resourceObjectDefinition.getObjectClassName().getLocalPart()).append(")");
            } else {
                sb.append(resourceObjectDefinition.getObjectClassName().getLocalPart());
            }
        }
        if (connector != null) {
            sb.append(", ").append(connector.getHumanReadableDescription());
        }
        return sb.toString();
    }

    public ObjectReferenceType getAssociationShadowRef() {
        return associationShadowRef;
    }

    public void setAssociationShadowRef(ObjectReferenceType associationShadowRef) {
        this.associationShadowRef = associationShadowRef;
    }

    public @NotNull ResourceObjectIdentification.WithPrimary getIdentificationFromShadow(@NotNull ShadowType shadow) {
        return ResourceObjectIdentification.fromCompleteShadow(getObjectDefinitionRequired(), shadow);
    }

    public boolean isAvoidDuplicateValues() {
        return ResourceTypeUtil.isAvoidDuplicateValues(resource);
    }

    // The object should correspond to the raw schema to avoid comparison issues related to polystrings
    public void checkProtectedObjectAddition(ResourceObject object, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!ProvisioningUtil.isAddShadowEnabled(
                getProtectedAccountPatterns(result),
                object,
                result)) {
            throw new SecurityViolationException(
                    String.format("Cannot add protected resource object %s (%s)", object, getExceptionDescription()));
        }
    }

    public void checkProtectedObjectModification(RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!ProvisioningUtil.isModifyShadowEnabled(
                getProtectedAccountPatterns(result),
                repoShadow,
                result)) {
            throw new SecurityViolationException(
                    String.format("Cannot modify protected resource object (%s): %s", repoShadow, getExceptionDescription()));
        }
    }

    public void checkProtectedObjectDeletion(RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!ProvisioningUtil.isDeleteShadowEnabled(
                getProtectedAccountPatterns(result),
                repoShadow,
                result)) {
            throw new SecurityViolationException(
                    String.format("Cannot delete protected resource object (%s): %s", repoShadow, getExceptionDescription()));
        }
    }

    public @NotNull ResourceObjectDefinition getAnyDefinition() throws SchemaException, ConfigurationException {
        Collection<ResourceObjectDefinition> objectDefinitions = getResourceSchema().getResourceObjectDefinitions();
        if (objectDefinitions.isEmpty()) {
            throw new IllegalStateException("Resource without object definitions: " + resource);
        }
        return objectDefinitions.iterator().next();
    }

    public @Nullable Duration getGracePeriod() {
        ResourceConsistencyType consistency = getResource().getConsistency();
        if (consistency != null) {
            return consistency.getPendingOperationGracePeriod();
        } else {
            return null;
        }
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "task", task, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resourceObjectDefinition", resourceObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "wholeClass", wholeClass, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "getOperationOptions", getOperationOptions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "propagation", propagation, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "operationContext", operationContext, indent + 1);
        return sb.toString();
    }
}
