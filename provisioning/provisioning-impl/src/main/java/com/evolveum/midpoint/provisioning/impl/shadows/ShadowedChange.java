/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.provisioning.impl.InitializableObjectMixin;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.CompleteResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;

import com.evolveum.midpoint.provisioning.util.InitializationState;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectChange;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * A resource object change that was "shadowed".
 *
 * This means that it is connected to repository shadow, and this shadow is updated
 * with the appropriate information.
 */
public abstract class ShadowedChange<ROC extends ResourceObjectChange>
        extends AbstractShadowedEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedChange.class);

    /**
     * Original resource object change that is being shadowed.
     */
    @NotNull final ROC resourceObjectChange;

    /**
     * Resource object: the best that is known at given time instant.
     * May be even null for DELETE-only, no-OC changes in wildcard LS mode.
     *
     * @see #determineCurrentResourceObjectBeforeShadow()
     * @see #determineCurrentResourceObjectAfterShadow(ProvisioningContext, ShadowType, OperationResult)
     */
    private ResourceObject resourceObject;

    /**
     * Is {@link #resourceObject} temporary (like identifiers-only), so it should be re-determined after the shadow is acquired?
     */
    private boolean resourceObjectIsTemporary;

    /**
     * The delta brought about by the change. May or may not be known.
     *
     * Note that the delta is cloned here because of minor changes like applying the attributes definitions.
     * In the future we plan to stop the cloning.
     */
    @Nullable private ObjectDelta<ShadowType> resourceObjectDelta;

    /** Always not null if the shadow was found or created (true for all successful cases for non-delete changes). */
    private String repoShadowOid;

    /**
     * The resulting combination of resource object and its repo shadow. Special cases:
     *
     * 1. For resources without read capability it is based on the cached repo shadow.
     * 2. For delete deltas, it is the current shadow, with applied definitions.
     * 3. In emergency it is the same as the current repo shadow.
     *
     * The point #2 should be perhaps reconsidered.
     */
    private ShadowType shadowedObject;

    ShadowedChange(@NotNull ROC resourceObjectChange) {
        super(resourceObjectChange);
        this.resourceObjectChange = resourceObjectChange;
    }

    @Override
    public @NotNull InitializableObjectMixin getPrerequisite() {
        return resourceObjectChange;
    }

    @Override
    public void initializeInternalCommon(Task task, OperationResult result) {
        super.initializeInternalCommon(task, result);
        // Delta is not cloned in the constructor, because it may be changed during resource change initialization.
        // We need to get those changes here.
        resourceObjectDelta = CloneUtil.clone(resourceObjectChange.getObjectDelta());
        resourceObject = determineCurrentResourceObjectBeforeShadow();
    }

    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result)
            throws CommonException, NotApplicableException, EncryptionException {

        ShadowType repoShadow = lookupOrAcquireRepoShadow(result);

        try {
            var shadowCtx = globalCtx.adoptShadow(repoShadow, resourceObjectDelta);

            if (isDelete()) {
                markRepoShadowTombstone(repoShadow, result);
                shadowedObject = constructShadowedObjectForDeletion(repoShadow, result);
            } else {
                resourceObject = determineCurrentResourceObjectAfterShadow(shadowCtx, repoShadow, result);
                ShadowType updatedRepoShadow = updateShadowInRepository(shadowCtx, repoShadow, result);
                shadowedObject = createShadowedObject(shadowCtx, updatedRepoShadow, result);
            }

        } catch (Exception e) {
            shadowedObject = repoShadow;
            throw e;
        }
    }

    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result)
            throws CommonException, EncryptionException {
        @Nullable ShadowType repoShadow;
        if (isDelete()) {
            repoShadow = lookupShadowForDeletionChange(result);
        } else {
            repoShadow = acquireRepoShadowInEmergency(result);
        }
        setAcquiredRepoShadowInEmergency(repoShadow);
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result)
            throws CommonException, EncryptionException {
        initializeInternalForPrerequisiteError(task, result); // To be reviewed if the processing should be the same
    }

    private @NotNull ShadowType lookupOrAcquireRepoShadow(OperationResult result)
            throws CommonException, NotApplicableException, EncryptionException {
        ShadowType repoShadow;
        if (isDelete()) {
            repoShadow = lookupShadowForDeletionChange(result);
            if (repoShadow == null) {
                throw new NotApplicableException();
            }
        } else {
            repoShadow = acquireRepoShadow(result);
        }
        repoShadowOid = stateNonNull(repoShadow.getOid(), "No OID in %s", repoShadow);
        return repoShadow;
    }

    /**
     * For delete deltas we don't bother with creating a shadow if it does not exist. So, just looking for one.
     * (Maybe we could even refrain from throwing exceptions if there is no unique primary identifier in wildcard case?)
     */
    @SuppressWarnings("ExtractMethodRecommender")
    private @Nullable ShadowType lookupShadowForDeletionChange(OperationResult result)
            throws SchemaException, ConfigurationException {
        // This context is the best we know at this moment. It is possible that it is wildcard (no OC known).
        // But the only way how to detect the OC is to read existing repo shadow. So we must take the risk
        // of guessing identifiers' definition correctly - in other words, assuming that these definitions are
        // the same for all the object classes on the given resource.
        @Nullable ResourceObjectDefinition objectDefinition = globalCtx.getObjectDefinition();
        ResourceObjectDefinition effectiveObjectDefinition;
        if (objectDefinition != null) {
            effectiveObjectDefinition = objectDefinition;
        } else {
            effectiveObjectDefinition = globalCtx.getAnyDefinition();
        }

        List<ResourceAttribute<?>> primaryIdentifierAttributes = getIdentifiers().stream()
                .filter(identifier -> effectiveObjectDefinition.isPrimaryIdentifier(identifier.getElementName()))
                .toList();

        ResourceAttribute<?> primaryIdentifierAttribute = MiscUtil.extractSingletonRequired(
                primaryIdentifierAttributes,
                () -> new SchemaException("Multiple primary identifiers among " + getIdentifiers() + " in " + this),
                () -> new SchemaException("No primary identifier in " + this));

        // We need to learn about correct matching rule (among others).
        primaryIdentifierAttribute.forceDefinitionFrom(effectiveObjectDefinition);

        ResourceObjectIdentifier.Primary<?> primaryIdentifier = ResourceObjectIdentifier.Primary.of(primaryIdentifierAttribute);

        var repoShadow = b.shadowFinder.lookupLiveOrAnyShadowByPrimaryId(globalCtx, primaryIdentifier, result);
        if (repoShadow == null) {
            getLogger().debug(
                    "No old shadow for delete synchronization event {}, we probably did not know about "
                            + "that object anyway, so well be ignoring this event", this);
        }
        return repoShadow;
    }

    @Override
    public @NotNull ResourceObject getResourceObjectRequired() {
        return Objects.requireNonNull(resourceObject, "No resource object");
    }

    @Override
    public @Nullable ObjectDelta<ShadowType> getResourceObjectDelta() {
        return resourceObjectDelta;
    }

    @Override
    public void setAcquiredRepoShadowInEmergency(ShadowType repoShadow) {
        this.repoShadowOid = repoShadow != null ? repoShadow.getOid() : null;
        this.shadowedObject = repoShadow;
    }

    public void checkConsistence() {
        InitializationState state = getInitializationState();

        if (!state.isInitialized() || !state.isOk()) {
            return;
        }

        if (repoShadowOid == null) {
            throw new IllegalStateException("No repository shadow in " + this);
        }
        if (globalCtx.isWildcard()) {
            throw new IllegalStateException("Context is wildcard in " + this);
        }
    }

    /** Null value can be returned only for (some) delete events in wildcard context. */
    private @Nullable ResourceObject determineCurrentResourceObjectBeforeShadow() {
        CompleteResourceObject completeResourceObject = resourceObjectChange.getCompleteResourceObject();
        if (completeResourceObject != null) {
            return completeResourceObject.resourceObject().clone();
        } else if (resourceObjectDelta != null && resourceObjectDelta.isAdd()) {
            return ResourceObject.fromPrismObject(
                    resourceObjectDelta.getObjectToAdd().clone(),
                    getPrimaryIdentifierValue());
        } else if (!resourceObjectChange.getIdentifiers().isEmpty()) {
            resourceObjectIsTemporary = true;
            return createIdentifiersOnlyFakeResourceObject();
        } else {
            throw new IllegalStateException(
                    "Could not create shadow from change description. Neither current resource object"
                            + " nor its identifiers exist.");
        }
    }

    private @Nullable ResourceObject createIdentifiersOnlyFakeResourceObject() {
        ResourceObjectDefinition objectDefinition = resourceObjectChange.getCurrentResourceObjectDefinition();
        if (objectDefinition == null) {
            if (isDelete()) {
                // This can happen for wildcard live sync, with no-object-class DELETE event. We have to deal with it.
                return null;
            } else {
                throw new IllegalStateException(
                        "Could not create shadow from change description. Object definition is not specified: " + this);
            }
        }
        ShadowType fakeResourceObject = new ShadowType();
        fakeResourceObject.setObjectClass(objectDefinition.getTypeName());
        ResourceAttributeContainer attributeContainer = objectDefinition.toResourceAttributeContainerDefinition().instantiate();
        try {
            fakeResourceObject.asPrismObject().add(attributeContainer);
            for (ResourceAttribute<?> identifier : resourceObjectChange.getIdentifiers()) {
                attributeContainer.add(identifier.clone());
            }
        } catch (SchemaException e) {
            // All the operations are schema-safe, so this is really a kind of internal error.
            throw SystemException.unexpected(e, "when creating fake resource object");
        }
        return ResourceObject.fromBean(fakeResourceObject, getPrimaryIdentifierValue());
    }

    private @NotNull ResourceObject determineCurrentResourceObjectAfterShadow(
            @NotNull ProvisioningContext shadowCtx, @NotNull ShadowType repoShadow, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, EncryptionException, NotApplicableException {
        if (resourceObject != null && !resourceObjectIsTemporary) {
            return resourceObject;
        }
        LOGGER.trace("Going to determine current resource object, as the previous one was temporary");

        ResourceObject resourceObject;
        ReadCapabilityType readCapability = shadowCtx.getCapability(ReadCapabilityType.class);
        boolean canReadFromResource = readCapability != null && !Boolean.TRUE.equals(readCapability.isCachingOnly());
        if (canReadFromResource) {
            // We go for the fresh object here. TODO to be reconsidered with regards to shadow caching in 4.9.
            Collection<SelectorOptions<GetOperationOptions>> options =
                    b.schemaService.getOperationOptionsBuilder().doNotDiscovery().build();
            try {
                // TODO why we use shadow cache and not resource object converter?!
                //  Because of (planned) caching? But then why don't we do this also when isCachingOnly is true?
                var object = b.shadowsFacade.getShadow(
                        repoShadow.getOid(),
                        repoShadow,
                        resourceObjectChange.getIdentifiers(),
                        options,
                        shadowCtx.getOperationContext(),
                        shadowCtx.getTask(),
                        result);
                resourceObject = ResourceObject.fromBean(object, getPrimaryIdentifierValue());
            } catch (ObjectNotFoundException e) {
                // The object on the resource does not exist (any more?).
                LOGGER.warn("Object {} does not exist on the resource any more", repoShadow);
                throw new NotApplicableException();
            }
            LOGGER.trace("-> current object was taken from the resource:\n{}", resourceObject.debugDumpLazily());
        } else if (shadowCtx.isCachingEnabled()) {
            // This might not be correct, because of partial caching and/or index-only attributes!
            resourceObject = ResourceObject.fromBean(repoShadow.clone(), getPrimaryIdentifierValue());
            if (resourceObjectDelta != null) {
                resourceObjectDelta.applyTo(resourceObject.getPrismObject());
                markIndexOnlyItemsAsIncomplete(resourceObject.getBean());
                LOGGER.trace("-> current object was taken from old shadow + delta:\n{}", resourceObject.debugDumpLazily());
            } else {
                LOGGER.trace("-> current object was taken from old shadow:\n{}", resourceObject.debugDumpLazily());
            }
        } else {
            throw new IllegalStateException(
                    "Cannot get current resource object: read capability is not present and passive caching is not configured");
        }
        globalCtx.applyAttributesDefinition(resourceObject.getPrismObject()); // is this really needed?
        return resourceObject;
    }

    public boolean isDelete() {
        return resourceObjectChange.isDelete();
    }

    public ResourceObjectDefinition getObjectDefinition() {
        return resourceObjectChange.getCurrentResourceObjectDefinition();
    }

    /**
     * Index-only items in the resource object delta are necessarily incomplete: their old value was taken from repo
     * (i.e. was empty before delta application). We mark them as such. One of direct consequences is that
     * updateShadowInRepository method will know that it cannot use this data to update cached (index-only) attributes
     * in repo shadow.
     */
    private void markIndexOnlyItemsAsIncomplete(ShadowType resourceObject)
            throws SchemaException, ConfigurationException {
        ResourceObjectDefinition ocDef = globalCtx.computeCompositeObjectDefinition(resourceObject);
        for (ResourceAttributeDefinition<?> attrDef : ocDef.getAttributeDefinitions()) {
            if (attrDef.isIndexOnly()) {
                ItemPath path = ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getItemName());
                LOGGER.trace("Marking item {} as incomplete because it's index-only", path);
                //noinspection unchecked
                resourceObject.asPrismObject()
                        .findCreateItem(path, Item.class, attrDef, true)
                        .setIncomplete(true);
            }
        }
    }

    private String getChannel() {
        return ObjectUtils.defaultIfNull(globalCtx.getChannel(), getDefaultChannel());
    }

    /**
     * Default channel for given change. The usefulness of this method is questionable,
     * as the context should have the correct channel already set.
     */
    protected abstract String getDefaultChannel();

    public @NotNull Collection<ResourceAttribute<?>> getIdentifiers() {
        return resourceObjectChange.getIdentifiers();
    }

    private void markRepoShadowTombstone(ShadowType repoShadow, OperationResult result) throws SchemaException {
        if (!ShadowUtil.isDead(repoShadow) || ShadowUtil.isExists(repoShadow)) {
            b.shadowUpdater.markShadowTombstone(repoShadow, globalCtx.getTask(), result);
        }
    }

    /**
     * It looks like the current resource object should be present also for DELETE deltas.
     *
     * TODO clarify this
     *
     * TODO try to avoid repository get operation by applying known deltas to existing repo shadow object
     *
     * So until clarified, we provide here the shadow object, with properly applied definitions.
     */
    private ShadowType constructShadowedObjectForDeletion(ShadowType repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, NotApplicableException {
        ShadowType currentShadow;
        try {
            currentShadow = b.shadowFinder.getShadowBean(repoShadow.getOid(), result);
        } catch (ObjectNotFoundException e) {
            LOGGER.debug("Shadow for delete synchronization event {} disappeared recently. Skipping this event.", this);
            throw new NotApplicableException();
        }
        globalCtx = globalCtx.applyAttributesDefinition(currentShadow);
        globalCtx.updateShadowState(currentShadow);
        return currentShadow;
    }

    public ResourceObjectShadowChangeDescription getShadowChangeDescription() {
        checkInitialized();

        if (shadowedObject == null) {
            stateCheck(isError() || isNotApplicable(),
                    "Non-error & applicable change without shadowed object? %s", this);
            return null; // This is because in the description the shadowed object must be present. TODO reconsider this.
        }
        ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
        if (resourceObjectDelta != null) {
            resourceObjectDelta.setOid(shadowedObject.getOid());
        }
        shadowChangeDescription.setObjectDelta(resourceObjectDelta);
        shadowChangeDescription.setResource(globalCtx.getResource().asPrismObject());
        shadowChangeDescription.setSourceChannel(getChannel());
        shadowChangeDescription.setShadowedResourceObject(shadowedObject.asPrismObject());
        return shadowChangeDescription;
    }

    public Object getPrimaryIdentifierValue() {
        return resourceObjectChange.getPrimaryIdentifierRealValue();
    }

    public int getSequentialNumber() {
        return resourceObjectChange.getLocalSequenceNumber();
    }

    @Override
    public @NotNull Trace getLogger() {
        return LOGGER;
    }

    public String getRepoShadowOid() {
        return repoShadowOid;
    }

    public ShadowType getShadowedObject() {
        return shadowedObject;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObjectChange=" + resourceObjectChange +
                ", repoShadow OID " + repoShadowOid +
                ", shadowedObject=" + shadowedObject +
                ", state=" + initializationState +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectChange", resourceObjectChange, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "repoShadowOid", repoShadowOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedObject", shadowedObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "context", String.valueOf(globalCtx), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }
}
