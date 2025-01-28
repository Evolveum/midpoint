/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectAsyncChange;
import com.evolveum.midpoint.provisioning.util.ErrorState;

import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.CompleteResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectChange;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * A resource object change that was "shadowed".
 *
 * This means that it is connected to repository shadow, and this shadow is updated
 * with the appropriate information.
 */
public abstract class ShadowedChange<ROC extends ResourceObjectChange>
        extends AbstractLazilyInitializableShadowedEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedChange.class);

    /**
     * Original resource object change that is being shadowed.
     */
    @NotNull final ROC resourceObjectChange;

    /**
     * Resource object: the best that is known at given time instant.
     * May be null for DELETE-only, no-OC changes in wildcard LS mode.
     *
     * @see #determineCurrentResourceObjectBeforeShadow()
     * @see #determineCurrentResourceObject(OperationResult)
     */
    private ExistingResourceObjectShadow resourceObject;

    /**
     * Is {@link #resourceObject} temporary (like identifiers-only), so it should be re-determined after the shadow is acquired?
     */
    private boolean resourceObjectIsTemporary;

    ShadowedChange(@NotNull ROC resourceObjectChange) {
        super(resourceObjectChange);
        this.resourceObjectChange = resourceObjectChange;
    }

    @Override
    public @NotNull LazilyInitializableMixin getPrerequisite() {
        return resourceObjectChange;
    }

    @Override
    protected RepoShadowWithState acquireOrLookupRepoShadow(OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {
        if (isDelete()) {
            return lookupRepoShadowForDeletionChange(result);
        } else {
            resourceObject = determineCurrentResourceObjectBeforeShadow();
            return acquireRepoShadow(resourceObject, result);
        }
    }

    /**
     * For delete deltas we don't bother with creating a shadow if it does not exist. So, just looking for one.
     * (Maybe we could even refrain from throwing exceptions if there is no unique primary identifier in wildcard case?)
     *
     * We look for live shadows, as we are not interested in dead ones. Most probably we were notified about them already.
     */
    private @Nullable RepoShadowWithState lookupRepoShadowForDeletionChange(OperationResult result)
            throws SchemaException, ConfigurationException {
        // This context is the best we know at this moment. It is possible that it is wildcard (no OC known).
        @Nullable ResourceObjectDefinition objectDefinition = effectiveCtx.getObjectDefinition();
        if (objectDefinition != null) {
            var identification = ResourceObjectIdentification.fromIdentifiers(objectDefinition, getIdentifiers());
            schemaCheck(identification.hasPrimaryIdentifier(), "No primary identifier in %s", this);
            return RepoShadowWithState.existingOptional(
                    b.shadowFinder.lookupLiveRepoShadowByPrimaryId(
                            effectiveCtx, identification.ensurePrimary(), false, result));
        } else {
            // This is the wildcard case. The only way how to detect the OC is to read existing repo shadow.
            // So we must take the risk of guessing the primary identifier definition correctly - in other words,
            // assuming that these definitions are the same for all the object classes on the given resource.
            var primaryIdentifier =
                    ResourceObjectIdentifier.primaryFromIdentifiers(
                            effectiveCtx.getAnyDefinition(), getIdentifiers(), this);
            return RepoShadowWithState.existingOptional(
                    b.shadowFinder.lookupLiveRepoShadowByPrimaryIdWithoutObjectClass(effectiveCtx, primaryIdentifier, result));
        }
    }

    @Override
    public void classifyUpdateAndCombine(Task task, OperationResult result)
            throws CommonException, NotApplicableException {

        if (isDelete()) {
            if (repoShadow == null) {
                getLogger().debug(
                        "No old live shadow for delete change {}, we probably did not know about "
                                + "that object anyway, so well be ignoring this event", this);
                throw new NotApplicableException();
            } else {
                postProcessForDeletion(result);
                // We'll return just the shadow when asked for the resulting object.
                return;
            }
        }

        assert repoShadow != null;

        shadowPostProcessor = new ShadowPostProcessor(
                effectiveCtx,
                repoShadow,
                determineCurrentResourceObject(result),
                resourceObjectChange.getObjectDelta());

        shadowPostProcessor.execute(result);
    }

    @Override
    public @NotNull ExistingResourceObjectShadow getExistingResourceObjectRequired() {
        return Objects.requireNonNull(resourceObject, "No resource object");
    }

    public void checkConsistence() {
        InitializationState state = getInitializationState();

        if (!state.isInitialized() || !state.isOk()) {
            return;
        }

        if (repoShadow == null) {
            throw new IllegalStateException("No repository shadow in " + this);
        }
        if (effectiveCtx.isWildcard()) {
            throw new IllegalStateException("Context is wildcard in " + this);
        }
    }

    private @NotNull ExistingResourceObjectShadow determineCurrentResourceObjectBeforeShadow() {
        assert !isDelete();
        CompleteResourceObject completeResourceObject = resourceObjectChange.getCompleteResourceObject();
        if (completeResourceObject != null) {
            return completeResourceObject.resourceObject().clone();
        } else if (!resourceObjectChange.getIdentifiers().isEmpty()) {
            resourceObjectIsTemporary = true;
            return createIdentifiersOnlyFakeResourceObject();
        } else {
            throw new IllegalStateException(
                    "Could not create shadow from change description. Neither current resource object"
                            + " nor its identifiers exist.");
        }
    }

    private @NotNull ExistingResourceObjectShadow createIdentifiersOnlyFakeResourceObject() {
        assert !isDelete();
        ResourceObjectDefinition objectDefinition = resourceObjectChange.getResourceObjectDefinition();
        if (objectDefinition == null) {
            throw new IllegalStateException(
                    "Could not create shadow from change description. Object definition is not specified: " + this);
        }
        ShadowType fakeResourceObject = new ShadowType();
        fakeResourceObject.setObjectClass(objectDefinition.getTypeName());
        ShadowAttributesContainer attributeContainer = objectDefinition.toShadowAttributesContainerDefinition().instantiate();
        try {
            fakeResourceObject.asPrismObject().add(attributeContainer);
            for (ShadowSimpleAttribute<?> identifier : resourceObjectChange.getIdentifiers()) {
                attributeContainer.addAttribute(identifier.clone());
            }
        } catch (SchemaException e) {
            // All the operations are schema-safe, so this is really a kind of internal error.
            throw SystemException.unexpected(e, "when creating fake resource object");
        }
        fakeResourceObject.setResourceRef(effectiveCtx.getResourceRef());
        fakeResourceObject.setExists(true); // the change is not "delete", so we assume the existence
        return ExistingResourceObjectShadow.of(
                fakeResourceObject,
                getPrimaryIdentifierValue(),
                ErrorState.ok());
    }

    private @NotNull ExistingResourceObjectShadow determineCurrentResourceObject(@NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, NotApplicableException {

        assert !isDelete();
        assert repoShadow != null;

        if (resourceObject != null && !resourceObjectIsTemporary) {
            return resourceObject;
        }
        LOGGER.trace("Going to determine current resource object, as the previous one was non-existent or temporary");

        ExistingResourceObjectShadow resourceObject;
        if (effectiveCtx.hasRealReadCapability() && !shouldUseCache()) {
            // We go for the fresh object here. TODO to be reconsidered with regards to shadow caching in 4.9.
            try {
                resourceObject =
                        b.resourceObjectConverter.locateResourceObject(
                                        effectiveCtx, getIdentification(), true, result)
                                .resourceObject();
            } catch (ObjectNotFoundException e) {
                // The object on the resource does not exist (any more?).
                LOGGER.warn("Object {} does not exist on the resource any more", repoShadow);
                throw new NotApplicableException();
            }
            LOGGER.trace("-> current object was taken from the resource:\n{}", resourceObject.debugDumpLazily());
        } else if (effectiveCtx.getObjectDefinitionRequired().isCachingEnabled()) {
            // This might not be correct, because of partial caching and/or index-only attributes!
            resourceObject = ExistingResourceObjectShadow.fromRepoShadow(
                    repoShadow.shadow().clone(),
                    getPrimaryIdentifierValue());
            var resourceObjectDelta = resourceObjectChange.getObjectDelta();
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
        effectiveCtx.applyDefinitionInNewCtx(resourceObject.getPrismObject()); // is this really needed?
        return resourceObject;
    }

    // This is a hack to make TestGrouperAsyncUpdate (story) work.
    private boolean shouldUseCache() {
        if (!(resourceObjectChange instanceof ResourceObjectAsyncChange asyncChange)) {
            return false; // this hack is only for asynchronous changes
        }
        if (asyncChange.isNotificationOnly()) {
            return false; // something has changed, cache would be outdated
        }
        // If caching is enabled, we should use it.
        return effectiveCtx.getObjectDefinitionRequired().isCachingEnabled();
    }

    public boolean isDelete() {
        return resourceObjectChange.isDelete();
    }

    /** Call only when there is a definition! I.e. not in untyped LS delete. */
    public @NotNull ResourceObjectDefinition getObjectDefinitionRequired() {
        return MiscUtil.stateNonNull(getObjectDefinition(), "No object definition in %s", this);
    }

    public ResourceObjectDefinition getObjectDefinition() {
        return resourceObjectChange.getResourceObjectDefinition();
    }

    /**
     * Index-only items in the resource object delta are necessarily incomplete: their old value was taken from repo
     * (i.e. was empty before delta application). We mark them as such. One of direct consequences is that
     * updateShadowInRepository method will know that it cannot use this data to update cached (index-only) attributes
     * in repo shadow.
     */
    private void markIndexOnlyItemsAsIncomplete(ShadowType resourceObject)
            throws SchemaException, ConfigurationException {
        // TODO the object should have the composite definition by now!
        ResourceObjectDefinition ocDef = effectiveCtx.computeCompositeObjectDefinition(resourceObject);
        for (ShadowSimpleAttributeDefinition<?> attrDef : ocDef.getSimpleAttributeDefinitions()) {
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
        return ObjectUtils.defaultIfNull(effectiveCtx.getChannel(), getDefaultChannel());
    }

    /**
     * Default channel for given change. The usefulness of this method is questionable,
     * as the context should have the correct channel already set.
     */
    protected abstract String getDefaultChannel();

    public @NotNull Collection<ShadowSimpleAttribute<?>> getIdentifiers() {
        return resourceObjectChange.getIdentifiers();
    }

    /** Beware! Call only when applicable (if all goes well, and if the change is not untyped delete). */
    public @NotNull ResourceObjectIdentification<?> getIdentification() throws SchemaException {
        return ResourceObjectIdentification.of(
                getObjectDefinitionRequired(),
                getIdentifiers());
    }

    private void postProcessForDeletion(OperationResult result) throws SchemaException, ConfigurationException {
        assert repoShadow != null;
        var shadow = repoShadow.shadow();
        if (!shadow.isDead() || shadow.doesExist()) {
            b.shadowUpdater.markShadowTombstone(shadow, effectiveCtx.getTask(), result);
        }
        var bean = shadow.getBean();
        bean.setEffectiveOperationPolicy(
                ObjectOperationPolicyHelper.get().computeEffectivePolicy(bean, effectiveCtx.getExecutionMode(), result));
    }

    public ResourceObjectShadowChangeDescription getShadowChangeDescription() {
        checkInitialized();

        ShadowType shadowedObjectBean = getShadowedObject();

        if (shadowedObjectBean == null) {
            stateCheck(isError() || isNotApplicable(),
                    "Non-error & applicable change without shadowed object? %s", this);
            return null; // This is because in the description the shadowed object must be present. TODO reconsider this.
        }
        ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
        var delta = resourceObjectChange.getObjectDelta();
        if (delta != null) {
            var deltaClone = delta.clone();
            deltaClone.setOid(shadowedObjectBean.getOid());
            shadowChangeDescription.setObjectDelta(deltaClone);
        }
        shadowChangeDescription.setResource(effectiveCtx.getResource().asPrismObject());
        shadowChangeDescription.setSourceChannel(getChannel());
        shadowChangeDescription.setShadowedResourceObject(shadowedObjectBean.asPrismObject());
        return shadowChangeDescription;
    }

    public @NotNull Object getPrimaryIdentifierValue() {
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
        return repoShadow != null ? repoShadow.shadow().getOid() : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObjectChange=" + resourceObjectChange +
                ", repoShadow OID " + getRepoShadowOid() +
                ", state=" + initializationState +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectChange", resourceObjectChange, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "repoShadow", repoShadow, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedObject", getShadowedObject(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "context", String.valueOf(effectiveCtx), indent + 1);
        return sb.toString();
    }
}
