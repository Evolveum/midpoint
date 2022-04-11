/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

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

//    /**
//     * Scope of our interest: single object, object type, object class, or event the whole resource.
//     */
//    @NotNull private final ResourceObjectsScope resourceObjectsScope;

    /**
     * Definition applicable to individual resource objects in the scope.
     */
    @Nullable private final ResourceObjectDefinition resourceObjectDefinition;

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
            @NotNull ProvisioningContextFactory contextFactory) {
        this.task = task;
        this.resource = resource;
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.contextFactory = contextFactory;
        LOGGER.trace("Created {}", this);
    }

    /** Creating context from previous one (potentially overriding some parts). */
    ProvisioningContext(
            @NotNull ProvisioningContext originalCtx,
            @NotNull Task task,
            @Nullable ResourceObjectDefinition resourceObjectDefinition) {
        this.task = task;
        this.resource = originalCtx.resource;
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.contextFactory = originalCtx.contextFactory;
        this.connectorMap.putAll(originalCtx.connectorMap);
        this.resourceSchema = originalCtx.resourceSchema;
        this.getOperationOptions = originalCtx.getOperationOptions; // OK?
        this.propagation = originalCtx.propagation;
        // Not copying protected account patters because these are object type specific.
        LOGGER.trace("Created/spawned {}", this);
    }

    public Collection<SelectorOptions<GetOperationOptions>> getGetOperationOptions() {
        return getOperationOptions;
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

    @NotNull public ResourceType getResource() {
        return resource;
    }

    public ResourceSchema getResourceSchema() throws SchemaException, ConfigurationException {
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

    /**
     * Returns the "raw" object class definition for the current context.
     * (If the context is given by object type, then its OC is returned. If it's the OC itself, the OC is returned.)
     */
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinitionRequired() {
        return getObjectDefinitionRequired()
                .getObjectClassDefinition();
    }

    public @NotNull QName getObjectClassNameRequired() {
        return getObjectClassDefinitionRequired().getObjectClassName();
    }

    /**
     * Returns the "raw" object class definition (if the context is not wildcard).
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
        if (resourceObjectDefinition instanceof ResourceObjectTypeDefinition) {
            return (ResourceObjectTypeDefinition) resourceObjectDefinition;
        } else {
            throw new IllegalStateException("No resource object type definition in " + this);
        }
    }

    /**
     * Returns the object type definition, if applicable. (Null otherwise.)
     */
    public @Nullable ResourceObjectTypeDefinition getRefinedObjectClassDefinitionIfPresent() {
        if (resourceObjectDefinition instanceof ResourceObjectTypeDefinition) {
            return (ResourceObjectTypeDefinition) resourceObjectDefinition;
        } else {
            return null;
        }
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

        if (!isTypeBased()) {
            return protectedObjectPatterns;
        }

        ResourceObjectTypeDefinition objectClassDefinition = getObjectTypeDefinitionRequired();
        Collection<ResourceObjectPattern> patterns = objectClassDefinition.getProtectedObjectPatterns();
        for (ResourceObjectPattern pattern : patterns) {
            ObjectFilter filter = pattern.getObjectFilter();
            if (filter == null) {
                continue;
            }
            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);
            variables.put(ExpressionConstants.VAR_CONFIGURATION,
                    getResourceManager().getSystemConfiguration(), SystemConfigurationType.class);
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(
                    filter, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory,
                    PrismContext.get(), "protected filter", getTask(), result);
            pattern.setFilter(evaluatedFilter);
            protectedObjectPatterns.add(pattern);
        }

        return protectedObjectPatterns;
    }

    // we don't use additionalAuxiliaryObjectClassQNames as we don't know if they are initialized correctly [med] TODO: reconsider this
    public @NotNull ResourceObjectDefinition computeCompositeObjectDefinition(
            @NotNull Collection<QName> auxObjectClassQNames)
            throws SchemaException, ConfigurationException {
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>(auxObjectClassQNames.size());
        for (QName auxObjectClassQName : auxObjectClassQNames) {
            ResourceObjectDefinition auxObjectClassDef = getResourceSchema().findDefinitionForObjectClass(auxObjectClassQName);
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
    public @NotNull ResourceObjectDefinition computeCompositeObjectDefinition(@NotNull PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException {
        return computeCompositeObjectDefinition(
                shadow.asObjectable().getAuxiliaryObjectClass());
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
    public ProvisioningContext spawnForKindIntent(@NotNull ShadowKindType kind, @NotNull String intent)
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
                resourceObjectDefinition);
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
    public ProvisioningContext spawnForShadow(PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException {
        return contextFactory.spawnForShadow(this, shadow);
    }

    public void assertDefinition(String message) throws SchemaException {
        if (resourceObjectDefinition == null) {
            throw new SchemaException(message + " " + getDesc());
        }
    }

    public void assertDefinition() throws SchemaException {
        assertDefinition("Cannot locate object class definition");
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
            result.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
            // Wrap those exceptions to a configuration exception. In the context of the provisioning operation we really cannot throw
            // ObjectNotFoundException exception. If we do that then the consistency code will interpret that as if the resource object
            // (shadow) is missing. But that's wrong. We do not have connector therefore we do not know anything about the shadow. We cannot
            // throw ObjectNotFoundException here.
            throw new ConfigurationException(e.getMessage(), e);
        } catch (CommunicationException | ConfigurationException | RuntimeException e) {
            result.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
            throw e;
        } finally {
            result.close();
        }
    }

    /**
     * Check connector capabilities in this order:
     *
     * 1. take additional connector capabilities if exist, if not, take resource capabilities,
     * 2. apply object class specific capabilities to the one selected in step 1,
     * 3. in the returned capabilities, check first configured capabilities and then native capabilities.
     *
     * TODO why we call this method "effective"? It implies that the capability is enabled. But it is not the case
     *  according to the code.
     */
    public <T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass) {
        CapabilitiesType connectorCapabilities = getConnectorCapabilities(capabilityClass);
        if (connectorCapabilities == null) {
            return null;
        }
        return CapabilityUtil.getEffectiveCapability(connectorCapabilities, capabilityClass);
    }

    public <T extends CapabilityType> boolean hasNativeCapability(Class<T> capabilityClass) {
        CapabilitiesType connectorCapabilities = getConnectorCapabilities(capabilityClass);
        if (connectorCapabilities == null) {
            return false;
        }
        return CapabilityUtil.hasNativeCapability(connectorCapabilities, capabilityClass);
    }

    public <T extends  CapabilityType> boolean hasConfiguredCapability(Class<T> capabilityClass) {
        CapabilitiesType connectorCapabilities = getConnectorCapabilities(capabilityClass);
        if (connectorCapabilities == null) {
            return false;
        }
        return CapabilityUtil.hasConfiguredCapability(connectorCapabilities, capabilityClass);
    }

    private <T extends CapabilityType> CapabilitiesType getConnectorCapabilities(Class<T> operationCapabilityClass) {
        return getResourceManager().getConnectorCapabilities(
                resource, getRefinedObjectClassDefinitionIfPresent(), operationCapabilityClass);
    }

    @Override
    public String toString() {
        return "ProvisioningContext("+getDesc()+")";
    }

    public ItemPath path(Object... components) {
        return ItemPath.create(components);
    }

    public CachingStategyType getCachingStrategy()
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return ProvisioningUtil.getCachingStrategy(this);
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

    public @Nullable CachingStategyType getPasswordCachingStrategy() {
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
        return SelectorOptions.hasToLoadPath(path, getOperationOptions, false);
    }

    public boolean isFetchingNotDisabled(ItemPath path) {
        return SelectorOptions.hasToLoadPath(path, getOperationOptions, true);
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
}
