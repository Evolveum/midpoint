/*
 * Copyright (C) 2015-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.*;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.isDiscoveryAllowed;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.*;
import java.util.function.Supplier;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState;
import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState.ShadowState;
import com.evolveum.midpoint.provisioning.ucf.api.SchemaAwareUcfExecutionContext;
import com.evolveum.midpoint.repo.common.ObjectMarkHelper.ObjectMarksComputer;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;

import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper.EffectiveMarksAndPolicies;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ShadowItemsToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.provisioning.util.ShadowItemsToReturnProvider;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
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
    private CompleteResourceSchema resourceSchema;

    /**
     * TODO update docs: Cached patterns for protected objects in given object type with their filter expressions evaluated.
     */
    private ShadowMarksComputerConfiguration shadowMarksComputerConfiguration;

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

    public @NotNull CompleteResourceSchema getResourceSchema() throws SchemaException, ConfigurationException {
        if (resourceSchema == null) {
            resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
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
    private @NotNull ShadowMarksComputerConfiguration getShadowMarksComputerConfiguration(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        if (shadowMarksComputerConfiguration == null) {
            shadowMarksComputerConfiguration = ShadowMarksComputerConfiguration.create(this, result);
        }
        return shadowMarksComputerConfiguration;
    }

    // Uses only the resource information from the context; TODO make that more explicit
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

    public <T extends CapabilityType> @NotNull ConnectorInstance getConnector(Class<T> operationCapabilityClass, OperationResult result)
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
     * Creates an exact copy of the context but with different task and probably additional aux OCs.
     */
    public @NotNull ProvisioningContext spawn(@NotNull Collection<QName> additionalAuxiliaryObjectClasses, Task task)
            throws SchemaException, ConfigurationException {
        // No need to bother the factory because no population resolution is needed
        return new ProvisioningContext(
                this,
                task,
                addAuxiliaryObjectClasses(resourceObjectDefinition, additionalAuxiliaryObjectClasses),
                wholeClass);
    }

    /**
     * The goal here is to create a definition that would present itself with specified auxiliary object classes.
     * (When asked via {@link ResourceObjectDefinition#hasAuxiliaryObjectClass(QName)}.)
     *
     * It does not matter if {@link CompositeObjectDefinition} is created or not.
     */
    private @Nullable ResourceObjectDefinition addAuxiliaryObjectClasses(
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<QName> additionalAuxObjClassNames) throws SchemaException, ConfigurationException {
        if (resourceObjectDefinition == null) {
            return null;
        }
        List<QName> missing = new ArrayList<>();
        for (QName additionalAuxObjClassName : additionalAuxObjClassNames) {
            if (!resourceObjectDefinition.hasAuxiliaryObjectClass(additionalAuxObjClassName)) {
                missing.add(additionalAuxObjClassName);
            }
        }
        return computeCompositeObjectDefinition(resourceObjectDefinition, missing);
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

    private <T extends CapabilityType> @NotNull ConnectorInstance getConnectorInstance(
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
        return CapabilityUtil.getCapability(resource, getObjectDefinition(), capabilityClass);
    }

    public <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass) {
        return CapabilityUtil.getEnabledOrNull(
                getCapability(capabilityClass));
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
        return CapabilityUtil.isReadingCachingOnly(resource, getObjectDefinition());
    }

    public boolean isPasswordReadable() {
        return CapabilityUtil.isPasswordReadable(
                getEnabledCapability(CredentialsCapabilityType.class));
    }

    @Override
    public String toString() {
        return "ProvisioningContext(" + getDesc() + ")";
    }

    public ItemPath path(Object... components) {
        return ItemPath.create(components);
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

    public @NotNull SchemaAwareUcfExecutionContext getUcfExecutionContext() throws SchemaException, ConfigurationException {
        return new SchemaAwareUcfExecutionContext(
                contextFactory.getLightweightIdentifierGenerator(),
                resource,
                getResourceSchema(),
                task);
    }

    public boolean canRun() {
        return !(task instanceof RunningTask runningTask) || runningTask.canRun();
    }

    public @NotNull String getResourceOid() {
        return Objects.requireNonNull(
                resource.getOid());
    }

    public void validateSchemaIfConfigured(ShadowType shadow) throws SchemaException {
        if (ResourceTypeUtil.isValidateSchema(resource)) {
            ShadowUtil.validateAttributeSchema(shadow, resourceObjectDefinition);
        }
    }

    /**
     * Returns association definitions, or an empty list if we do not have appropriate definition available.
     */
    public @NotNull Collection<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
        return resourceObjectDefinition != null ?
                resourceObjectDefinition.getReferenceAttributeDefinitions() : List.of();
    }

    public <T> @Nullable ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(QName name) throws SchemaException {
        return resourceObjectDefinition != null ? resourceObjectDefinition.findSimpleAttributeDefinition(name) : null;
    }

    public @NotNull ShadowAttributeDefinition<?, ?, ?, ?> findAttributeDefinitionRequired(QName name) throws SchemaException {
        return getObjectDefinitionRequired().findAttributeDefinitionRequired(name);
    }

    public @NotNull ShadowReferenceAttributeDefinition findReferenceAttributeDefinitionRequired(QName name) throws SchemaException {
        return findReferenceAttributeDefinitionRequired(name, () -> " in " + this);
    }

    public @NotNull ShadowReferenceAttributeDefinition findReferenceAttributeDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        return getObjectDefinitionRequired()
                .findReferenceAttributeDefinitionRequired(name, contextSupplier);
    }

    /**
     * It's more logical to call this method right on {@link ProvisioningContext}. The exact placement of the implementation
     * is to be decided yet.
     */
    public ShadowItemsToReturn createItemsToReturn() {
        return new ShadowItemsToReturnProvider(resource, getObjectDefinitionRequired(), getOperationOptions)
                .createAttributesToReturn();
    }

    /** Beware! Creates a new context based on the shadow kind/intent/OC. */
    public ProvisioningContext applyDefinitionInNewCtx(@NotNull PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException {
        return applyDefinitionInNewCtx(shadow.asObjectable());
    }

    /** Beware! Creates a new context based on the shadow kind/intent/OC. TODO check if not redundant! */
    public ProvisioningContext applyDefinitionInNewCtx(@NotNull ResourceObjectShadow resourceObject)
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
        if (shadow.getAttributes() == null) {
            shadow.setAttributes(new ShadowAttributesType()); // the definition will be applied below
        }
        subContext.applyCurrentDefinition(shadow);
        return subContext;
    }

    /** Does not create a new context. The current context should be derived from the shadow. TODO reconsider */
    public void applyCurrentDefinition(@NotNull ShadowType shadow) throws SchemaException {
        ShadowDefinitionApplicator.strict(getObjectDefinitionRequired())
                .applyToShadow(shadow);
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
     * Converts {@link RawRepoShadow} into {@link RepoShadow} by determining the correct definition (from OC/kind/intent),
     * updating the shadow state, and converting the attributes from raw repo to standard format (including reference attributes).
     *
     * Although it treats reference attributes, it does nothing with associations.
     */
    public @NotNull RepoShadow adoptRawRepoShadow(@NotNull ShadowType bean)
            throws SchemaException, ConfigurationException {
        return adoptRawRepoShadow(RawRepoShadow.of(bean), true);
    }

    /** Just a variant of {@link #adoptRawRepoShadow(ShadowType)}. */
    public @NotNull RepoShadow adoptRawRepoShadow(@NotNull PrismObject<ShadowType> prismObject)
            throws SchemaException, ConfigurationException {
        return adoptRawRepoShadow(RawRepoShadow.of(prismObject), true);
    }

    /** Just a variant of {@link #adoptRawRepoShadow(ShadowType)}. */
    public @NotNull RepoShadow adoptRawRepoShadow(@NotNull RawRepoShadow rawRepoShadow)
            throws SchemaException, ConfigurationException {
        return adoptRawRepoShadow(rawRepoShadow, true);
    }

    /**
     * As {@link #adoptRawRepoShadow(PrismObject)} but does not fill-in the embedded {@link RawRepoShadow} - to avoid object
     * cloning if not really necessary.
     */
    public @NotNull RepoShadow adoptRawRepoShadowSimple(@NotNull PrismObject<ShadowType> prismObject)
            throws SchemaException, ConfigurationException {
        return adoptRawRepoShadow(RawRepoShadow.of(prismObject), false);
    }

    private @NotNull RepoShadow adoptRawRepoShadow(@NotNull RawRepoShadow rawRepoShadow, boolean keepTheRawShadow)
            throws SchemaException, ConfigurationException {
        var rawShadowBean = rawRepoShadow.getBean();
        var resourceOid = rawRepoShadow.getResourceOidRequired();
        stateCheck(
                resourceOid.equals(getResourceOid()),
                "Resource OID mismatch: %s vs. %s", resourceOid, getResourceOid());
        var definition =
                stateNonNull(
                        getResourceSchema().findDefinitionForShadow(rawShadowBean),
                        "No definition for shadow %s on %s could be found."
                                + "Please fix the shadow or the resource configuration",
                        rawShadowBean, resource);
        var state = ShadowLifecycleStateDeterminer.determineShadowState(this, rawShadowBean);
        var fresh = ShadowUtil.getShadowCachedStatus(
                        rawRepoShadow.getPrismObject(),
                        definition,
                        ShadowContentDescriptionType.FROM_REPOSITORY,
                        CommonBeans.get().clock.currentTimeXMLGregorianCalendar())
                .isFresh();

        // The following drives how strict we are when reading the shadow. On one hand, we want to work with the correct data.
        // On the other, we do not want to fail hard when the resource schema changes. Hence, we are strict for fresh shadows,
        // but lax for the others, because the data will not be used anyway (except for the identifiers - but they do not
        // change as frequently). The general recommendation will be: after you change the schema, please invalidate the cache.
        var lax = !fresh;

        // Another aspect is that shadow may not be classified (yet). Legacy associations can cause issues there.
        // So, let's be more lax in such cases. The data should not be processed by inbounds anyway, as these are connected
        // to object types.
        var laxForReferenceAttributes = !ShadowUtil.isClassified(rawShadowBean);

        return RepoShadow.fromRaw(rawRepoShadow, resource, definition, state, keepTheRawShadow, lax, laxForReferenceAttributes);
    }

    /** The shadow should be a bean usable as a {@link ResourceObjectShadow} (except for the attribute definitions). */
    public @NotNull ResourceObjectShadow adoptNotYetExistingResourceObject(ShadowType bean)
            throws SchemaException, ConfigurationException {
        var shadowCtx = adoptShadowBean(bean);
        return ResourceObjectShadow.fromBean(bean, false, shadowCtx.getObjectDefinitionRequired());
    }

    public void applyCurrentDefinition(@NotNull ObjectDelta<ShadowType> delta) throws SchemaException {
        ShadowDefinitionApplicator.strict(getObjectDefinitionRequired())
                .applyToDelta(delta);
    }

    public void applyCurrentDefinition(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) throws SchemaException {
        ShadowDefinitionApplicator.strict(getObjectDefinitionRequired())
                .applyToItemDeltas(modifications);
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

    public boolean isAvoidDuplicateValues() {
        return ResourceTypeUtil.isAvoidDuplicateValues(resource);
    }

    public void checkProtectedObjectAddition(ResourceObjectShadow object)
            throws SecurityViolationException {
        if (isAddDisabled(object.getEffectiveOperationPolicyRequired())) { // TODO treat the severity as well
            throw new SecurityViolationException(
                    String.format("Cannot add protected resource object (%s): %s", object, getExceptionDescription()));
        }
    }

    public void checkProtectedObjectModification(RepoShadow repoShadow)
            throws SecurityViolationException {
        if (isModifyDisabled(repoShadow.getEffectiveOperationPolicyRequired())) { // TODO treat the severity as well
            throw new SecurityViolationException(
                    String.format("Cannot modify protected resource object (%s): %s", repoShadow, getExceptionDescription()));
        }
    }

    public void checkProtectedObjectDeletion(RepoShadow repoShadow) throws SecurityViolationException {
        if (isDeleteDisabled(repoShadow.getEffectiveOperationPolicyRequired())) { // TODO treat the severity as well
            throw new SecurityViolationException(
                    String.format("Cannot delete protected resource object (%s): %s", repoShadow, getExceptionDescription()));
        }
    }

    /** Calculates effective object marks and policies for the shadow, and updates the shadow accordingly. */
    public EffectiveMarksAndPolicies computeAndUpdateEffectiveMarksAndPolicies(
            @NotNull AbstractShadow shadow, @NotNull ShadowState shadowState, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var computer = createShadowMarksComputer(shadow, shadowState, result);
        var marksAndPolicies =
                ObjectOperationPolicyHelper.get().computeEffectiveMarksAndPolicies(
                        shadow.getBean(), computer, getExecutionMode(), result);
        marksAndPolicies.applyTo(shadow.getBean());
        return marksAndPolicies;
    }

    /** Does NOT update the provided shadow. We need the old state to correctly compute the delta. */
    public EffectiveMarksAndPolicies computeEffectiveMarksAndPolicies(
            @NotNull RepoShadowWithState repoShadowWithState,
            @NotNull ExistingResourceObjectShadow resourceObject,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        return ObjectOperationPolicyHelper.get().computeEffectiveMarksAndPolicies(
                repoShadowWithState.getBean(),
                createShadowMarksComputer(resourceObject, repoShadowWithState.state(), result),
                getExecutionMode(),
                result);
    }

    private ObjectMarksComputer createShadowMarksComputer(
            @NotNull AbstractShadow shadow,
            @NotNull ShadowState shadowState,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return getShadowMarksComputerConfiguration(result)
                .computerFor(shadow, shadowState);
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

    /**
     * Returns the classification mode.
     *
     * FIXME define the scope more precisely: is it for the main object, or for associated ones as well?
     *  (Actually, this is an issue for other {@link #getOperationOptions} settings.)
     */
    public @NotNull ShadowClassificationModeType getClassificationMode() {
        return Objects.requireNonNullElse(
                GetOperationOptions.getShadowClassificationMode(
                        SelectorOptions.findRootOptions(getOperationOptions)),
                ShadowClassificationModeType.NORMAL);
    }

    public boolean isCaseIgnoreAttributeNames() {
        return ResourceTypeUtil.isCaseIgnoreAttributeNames(resource);
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
