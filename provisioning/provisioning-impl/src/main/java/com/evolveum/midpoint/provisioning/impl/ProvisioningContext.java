/*
 * Copyright (C) 2015-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.*;
import java.util.function.Supplier;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.util.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * Context for provisioning operations. Contains key information like resolved resource,
 * object type and/or object class definitions, and so on.
 *
 * @author semancik
 */
public class ProvisioningContext {

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
     * If this is a bulk operation (like search or live sync), this also drives its scope - i.e. whether to
     * access the whole resource, an object class, or a object type.
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
     * Cached patterns for protected objects in given object type.
     *
     * TODO
     */
    private Collection<ResourceObjectPattern> protectedObjectPatterns;

    /** Creating context from scratch. */
    ProvisioningContext(
            @NotNull Task task,
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            Boolean wholeClass,
            @NotNull ProvisioningContextFactory contextFactory) {
        this.task = task;
        this.resource = resource;
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
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.wholeClass = wholeClass;
        this.contextFactory = originalCtx.contextFactory;
        this.connectorMap.putAll(originalCtx.connectorMap);
        this.resourceSchema = originalCtx.resourceSchema;
        this.getOperationOptions = originalCtx.getOperationOptions; // OK?
        this.propagation = originalCtx.propagation;
        // Not copying protected account patters because these are object type specific.
        LOGGER.trace("Created/spawned {}", this);
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
     * Returns the object type definition, or fails if there's none (because of being wildcard or being OC-based).
     */
    public @NotNull ResourceObjectTypeDefinition getObjectTypeDefinitionRequired() {
        return MiscUtil.requireNonNull(
                getObjectTypeDefinitionIfPresent(),
                () -> new IllegalStateException("No resource object type definition in " + this));
    }

    /**
     * Returns the object type definition, if applicable. (Null otherwise.)
     */
    private @Nullable ResourceObjectTypeDefinition getObjectTypeDefinitionIfPresent() {
        return resourceObjectDefinition != null ? resourceObjectDefinition.getTypeDefinition() : null;
    }

    /**
     * Returns evaluated protected object patterns.
     */
    public Collection<ResourceObjectPattern> getProtectedAccountPatterns(
            ExpressionFactory expressionFactory, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        if (protectedObjectPatterns != null) {
            return protectedObjectPatterns;
        }

        protectedObjectPatterns = new ArrayList<>();

        ResourceObjectDefinition objectDefinition = getObjectDefinitionRequired();
        Collection<ResourceObjectPattern> rawPatterns = objectDefinition.getProtectedObjectPatterns();
        for (ResourceObjectPattern rawPattern : rawPatterns) {
            ObjectFilter filter = rawPattern.getObjectFilter();
            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);
            variables.put(ExpressionConstants.VAR_CONFIGURATION,
                    getResourceManager().getSystemConfiguration(), SystemConfigurationType.class);
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(
                    filter, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory,
                    PrismContext.get(), "protected filter", getTask(), result);
            protectedObjectPatterns.add(
                    new ResourceObjectPattern(
                            rawPattern.getResourceObjectDefinition(),
                            evaluatedFilter));
        }

        return protectedObjectPatterns;
    }

    // we don't use additionalAuxiliaryObjectClassQNames as we don't know if they are initialized correctly [med] TODO: reconsider this
    public @NotNull ResourceObjectDefinition computeCompositeObjectDefinition(
            @NotNull Collection<QName> auxObjectClassQNames)
            throws SchemaException, ConfigurationException {
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>(auxObjectClassQNames.size());
        for (QName auxObjectClassQName : auxObjectClassQNames) {
            ResourceObjectDefinition auxObjectClassDef = getResourceSchema().findObjectClassDefinition(auxObjectClassQName);
            if (auxObjectClassDef == null) {
                throw new SchemaException("Auxiliary object class " + auxObjectClassQName + " specified in " + this + " does not exist");
            }
            auxiliaryObjectClassDefinitions.add(auxObjectClassDef);
        }
        return new CompositeObjectDefinitionImpl(
                getObjectDefinitionRequired(),
                auxiliaryObjectClassDefinitions);
    }

    /**
     * Returns either real composite type definition, or just object definition - if that's not possible.
     */
    public @NotNull ResourceObjectDefinition computeCompositeObjectDefinition(@NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {
        return computeCompositeObjectDefinition(shadow.getAuxiliaryObjectClass());
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
    public ProvisioningContext spawnForKindIntent(
            @NotNull ShadowKindType kind,
            @NotNull String intent)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForKindIntent(this, kind, intent);
    }

    /**
     * Creates an exact copy of the context but with different task.
     */
    public ProvisioningContext spawn(Task task) {
        // No need to bother the factory because no population resolution is needed
        return new ProvisioningContext(
                this,
                task,
                resourceObjectDefinition,
                wholeClass);
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
     * This method looks fort the "real" raw object class definition (i.e. not a default object type
     * definition for given object class name)
     */
    public @NotNull ProvisioningContext spawnForObjectClassWithRawDefinition(@NotNull QName objectClassName)
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

    public void assertDefinition(String message) throws SchemaException {
        if (resourceObjectDefinition == null) {
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
                    .getConfiguredConnectorInstance(resource.asPrismObject(), operationCapabilityClass, false, result);
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
                resource, getObjectTypeDefinitionIfPresent(), capabilityClass);
    }

    public <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass) {
        T capability = getCapability(capabilityClass);
        return CapabilityUtil.isCapabilityEnabled(capability) ? capability : null;
    }

    public boolean hasCapability(@NotNull Class<? extends CapabilityType> capabilityClass) {
        return getEnabledCapability(capabilityClass) != null;
    }

    public boolean hasReadCapability() {
        return hasCapability(ReadCapabilityType.class);
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

    public @NotNull CachingStrategyType getCachingStrategy() {
        CachingPolicyType cachingPolicy = resource.getCaching();
        CachingStrategyType explicitCachingStrategy = cachingPolicy != null ? cachingPolicy.getCachingStrategy() : null;
        if (explicitCachingStrategy != null) {
            return explicitCachingStrategy;
        } else {
            ReadCapabilityType readCapability = getEnabledCapability(ReadCapabilityType.class);
            if (readCapability != null && Boolean.TRUE.equals(readCapability.isCachingOnly())) {
                return CachingStrategyType.PASSIVE;
            } else {
                return CachingStrategyType.NONE;
            }
        }
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

    // Preliminary code
    public boolean isResourceInProduction() {
        return LifecycleUtil.isInProduction(
                resource.getLifecycleState());
    }

    // Preliminary code
    public boolean isObjectDefinitionInProduction() {
        if (!isResourceInProduction()) {
            return false; // We ignore any object class/type level settings here.
        }
        return resourceObjectDefinition == null
                || LifecycleUtil.isInProduction(resourceObjectDefinition.getLifecycleState());
    }

    public void checkNotInMaintenance() throws MaintenanceException {
        ResourceTypeUtil.checkNotInMaintenance(resource);
    }

    public @NotNull Task getTask() {
        return task;
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

    /**
     * Returns true if the object definition is "refined" (i.e. object type based).
     */
    public boolean isTypeBased() {
        return resourceObjectDefinition instanceof ResourceObjectTypeDefinition;
    }

    public @Nullable CachingStrategyType getPasswordCachingStrategy() {
        return ProvisioningUtil.getPasswordCachingStrategy(
                getObjectDefinitionRequired());
    }

    public void validateSchema(ShadowType shadow)
            throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (ResourceTypeUtil.isValidateSchema(resource)) {
            ShadowUtil.validateAttributeSchema(shadow, getObjectDefinition());
        }
    }

    private @NotNull ResourceManager getResourceManager() {
        return contextFactory.getResourceManager();
    }

    public boolean isFetchingRequested(ItemPath path) {
        return SelectorOptions.hasToIncludePath(path, getOperationOptions, false);
    }

    public boolean isFetchingNotDisabled(ItemPath path) {
        return SelectorOptions.hasToIncludePath(path, getOperationOptions, true);
    }

    /**
     * Returns association definitions, or an empty list if we do not have appropriate definition available.
     */
    public @NotNull Collection<ResourceAssociationDefinition> getAssociationDefinitions() {
        return resourceObjectDefinition != null ?
                resourceObjectDefinition.getAssociationDefinitions() : List.of();
    }

    public @Nullable ResourceAttributeDefinition<?> findAttributeDefinition(QName name) throws SchemaException {
        return resourceObjectDefinition != null ? resourceObjectDefinition.findAttributeDefinition(name) : null;
    }

    public @NotNull ResourceAttributeDefinition<?> findAttributeDefinitionRequired(QName name) throws SchemaException {
        return getObjectDefinitionRequired().findAttributeDefinitionRequired(name);
    }

    public @NotNull ResourceAttributeDefinition<?> findAttributeDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        return getObjectDefinitionRequired()
                .findAttributeDefinitionRequired(name, contextSupplier);
    }

    /**
     * It's more logical to call this method right on {@link ProvisioningContext}. The exact placement of the implementation
     * is to be decided yet.
     */
    public AttributesToReturn createAttributesToReturn() {
        return ProvisioningUtil.createAttributesToReturn(this);
    }

    // Methods delegated to shadow caretaker (convenient to be here, but not sure if it's ok...)

    public ProvisioningContext applyAttributesDefinition(@NotNull PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException {
        return getCaretaker().applyAttributesDefinition(this, shadow);
    }

    public ProvisioningContext applyAttributesDefinition(@NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {
        return getCaretaker().applyAttributesDefinition(this, shadow);
    }

    public void applyAttributesDefinition(@NotNull ObjectDelta<ShadowType> delta)
            throws SchemaException, ConfigurationException {
        getCaretaker().applyAttributesDefinition(this, delta);
    }

    private @NotNull ShadowCaretaker getCaretaker() {
        return contextFactory.getCommonBeans().shadowCaretaker;
    }

    public void updateShadowState(ShadowType shadow) {
        getCaretaker().updateShadowState(this, shadow);
    }

    // TODO not sure if it's ok here
    public @NotNull ShadowType futurizeShadow(
            @NotNull ShadowType repoShadow,
            ShadowType resourceShadow,
            Collection<SelectorOptions<GetOperationOptions>> options,
            XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        if (!ProvisioningUtil.isFuturePointInTime(options)) {
            return Objects.requireNonNullElse(resourceShadow, repoShadow);
        } else {
            return getCaretaker().applyPendingOperations(
                            this,
                            ObjectTypeUtil.asPrismObject(repoShadow),
                            ObjectTypeUtil.asPrismObject(resourceShadow),
                            false,
                            now)
                    .asObjectable();
        }
    }

    public boolean isAllowNotFound() {
        return GetOperationOptions.isAllowNotFound(
                SelectorOptions.findRootOptions(getOperationOptions));
    }
}
