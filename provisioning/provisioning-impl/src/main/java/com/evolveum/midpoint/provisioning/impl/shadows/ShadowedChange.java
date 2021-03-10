/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.ChangeProcessingBeans;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;

import com.evolveum.midpoint.provisioning.util.InitializationState;

import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.DebugUtil;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectAsyncChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectChange;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static java.util.Objects.requireNonNull;

/**
 * A resource object change that was "shadowed".
 *
 * This means that it is connected to repository shadow, and this shadow is updated
 * with the appropriate information.
 */
public class ShadowedChange<ROC extends ResourceObjectChange> implements InitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedChange.class);

    /**
     * Original resource object change that is being shadowed.
     */
    @NotNull protected final ROC resourceObjectChange;

    /**
     * Resource object delta. It is cloned here because of minor changes like applying the attributes.
     */
    protected final ObjectDelta<ShadowType> objectDelta;

    /**
     * Repository shadow of the changed resource object.
     *
     * It is either the "old" one (i.e. existing before we learned about the change)
     * or a newly acquired one. In any case, the repository shadow itself is UPDATED as part of
     * initialization of this change.
     */
    private PrismObject<ShadowType> repoShadow;

    /**
     * The resulting combination of resource object and its repo shadow. Special cases:
     *
     * 1. For resources without read capability it is based on the cached repo shadow.
     * 2. For delete deltas, it is the current shadow, with applied definitions.
     * 3. In emergency it is the same as the current repo shadow.
     *
     * The point #2 should be perhaps reconsidered.
     */
    protected PrismObject<ShadowType> shadowedObject;

    /**
     * Context of the processing. In most cases it is taken from the original change.
     * But for wildcard delete changes it is clarified using existing repo shadow.
     * After pre-processing it is no longer wildcard.
     */
    @NotNull private ProvisioningContext context;

    /** State of the initialization of this object. */
    @NotNull protected final InitializationState initializationState;

    /** Useful provisioning beans. */
    @NotNull protected final ChangeProcessingBeans beans;

    /** Useful beans local to the Shadows package. */
    @NotNull protected final ShadowsLocalBeans localBeans;

    public ShadowedChange(@NotNull ROC resourceObjectChange, ChangeProcessingBeans beans) {
        this.initializationState = InitializationState.fromPreviousState(resourceObjectChange.getInitializationState());
        this.resourceObjectChange = resourceObjectChange;
        this.context = resourceObjectChange.getContext();
        this.beans = beans;
        this.localBeans = beans.shadowsFacade.getLocalBeans();
        this.objectDelta = CloneUtil.clone(resourceObjectChange.getObjectDelta());
    }

    @Override
    public void initializeInternal(Task task, OperationResult result)
            throws CommonException, NotApplicableException, EncryptionException {

        if (!initializationState.isInitialStateOk()) {
            setShadowedResourceObjectInEmergency(result);
            return;
        }

        lookupOrAcquireShadow(result);

        assert repoShadow != null;
        assert !context.isWildcard();

        try {
            applyAttributesDefinition();

            if (!isDelete()) {
                PrismObject<ShadowType> resourceObject = determineCurrentResourceObject(result);
                updateRepoShadow(resourceObject, result);
                shadowedObject = constructShadowedObject(resourceObject, result);
            } else {
                markRepoShadowTombstone(result);
                shadowedObject = constructShadowedObjectForDeletion(result);
            }

        } catch (Exception e) {
            shadowedObject = repoShadow;
            throw e;
        }
    }

    private void lookupOrAcquireShadow(OperationResult result) throws SchemaException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, NotApplicableException,
            EncryptionException, SecurityViolationException {
        if (isDelete()) {
            lookupShadow(result);
            updateProvisioningContextFromRepoShadow();
        } else {
            try {
                acquireShadow(result);
            } catch (Exception e) {
                setShadowedResourceObjectInEmergency(result);
                throw e;
            }
        }
    }

    // For delete deltas we don't bother with creating a shadow if it does not exist.
    private void lookupShadow(OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, NotApplicableException {
        assert isDelete();
        // This context is the best we know at this moment. It is possible that it is wildcard (no OC known).
        // But the only way how to detect the OC is to read existing repo shadow. So we must take the risk
        // of guessing identifiers' definition correctly - in other words, assuming that these definitions are
        // the same for all the object classes on the given resource.
        repoShadow = beans.shadowManager.lookupLiveOrAnyShadowByPrimaryIds(context, resourceObjectChange.getIdentifiers(), result);
        if (repoShadow == null) {
            LOGGER.debug("No old shadow for delete synchronization event {}, we probably did not know about "
                    + "that object anyway, so well be ignoring this event", this);
            throw new NotApplicableException();
        }
    }

    private void updateProvisioningContextFromRepoShadow() {
        assert repoShadow != null;
        assert isDelete();
        if (context.isWildcard()) {
            context = context.spawn(repoShadow);
        }
    }

    private void acquireShadow(OperationResult result) throws SchemaException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, EncryptionException {
        assert !isDelete();

        PrismProperty<?> primaryIdentifier = resourceObjectChange.getPrimaryIdentifierRequired();
        QName objectClass = getObjectClassDefinition().getTypeName();

        repoShadow = localBeans.shadowAcquisitionHelper.acquireRepoShadow(context, primaryIdentifier, objectClass,
                this::createResourceObjectFromChange, false, result);
    }

    /**
     * TODO deduplicate with {@link ShadowedObjectFound}.
     */
    private void setShadowedResourceObjectInEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        PrismObject<ShadowType> resourceObject = createResourceObjectFromChange();
        LOGGER.trace("Acquiring repo shadow in emergency:\n{}", debugDumpLazily(1));
        try {
            setEmergencyRepoShadow(
                    localBeans.shadowAcquisitionHelper
                            .acquireRepoShadow(context, resourceObject, true, result));
        } catch (Exception e) {
            setShadowedResourceObjectInUltraEmergency(resourceObject, result);
            throw e;
        }
    }

    private void setEmergencyRepoShadow(PrismObject<ShadowType> repoShadow) {
        this.repoShadow = repoShadow;
        this.shadowedObject = repoShadow;
    }

    @NotNull
    private PrismObject<ShadowType> createResourceObjectFromChange() throws SchemaException {
        if (resourceObjectChange.getResourceObject() != null) {
            return resourceObjectChange.getResourceObject();
        } else if (resourceObjectChange.isAdd()) {
            return requireNonNull(resourceObjectChange.getObjectDelta().getObjectToAdd());
        } else if (!resourceObjectChange.getIdentifiers().isEmpty()) {
            return createIdentifiersOnlyFakeResourceObject();
        } else {
            throw new IllegalStateException("Could not create shadow from change description. Neither current resource object"
                    + " nor its identifiers exist.");
        }
    }

    private PrismObject<ShadowType> createIdentifiersOnlyFakeResourceObject() throws SchemaException {
        ObjectClassComplexTypeDefinition objectClassDefinition = getObjectClassDefinition();
        if (objectClassDefinition == null) {
            throw new IllegalStateException("Could not create shadow from change description. Object class is not specified.");
        }
        ShadowType fakeResourceObject = new ShadowType(beans.prismContext);
        fakeResourceObject.setObjectClass(objectClassDefinition.getTypeName());
        ResourceAttributeContainer attributeContainer = objectClassDefinition
                .toResourceAttributeContainerDefinition().instantiate();
        fakeResourceObject.asPrismObject().add(attributeContainer);
        for (ResourceAttribute<?> identifier : resourceObjectChange.getIdentifiers()) {
            attributeContainer.add(identifier.clone());
        }
        return fakeResourceObject.asPrismObject();
    }

    /**
     * Something prevents us from creating a shadow (most probably). Let us be minimalistic, and create
     * a shadow having only the primary identifier.
     *
     * TODO deduplicate with {@link ShadowedObjectFound}.
     */
    private void setShadowedResourceObjectInUltraEmergency(PrismObject<ShadowType> resourceObject,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        PrismObject<ShadowType> minimalResourceObject = Util.minimize(resourceObject, context.getObjectClassDefinition());
        LOGGER.trace("Minimal resource object to acquire a shadow for:\n{}",
                DebugUtil.debugDumpLazily(minimalResourceObject, 1));
        if (minimalResourceObject != null) {
            setEmergencyRepoShadow(
                    localBeans.shadowAcquisitionHelper
                            .acquireRepoShadow(context, minimalResourceObject, true, result));
        }
    }

    public void checkConsistence() {
        InitializationState state = getInitializationState();

        if (!state.isAfterInitialization() || !state.isOk()) {
            return;
        }

        if (repoShadow == null) {
            throw new IllegalStateException("No repository shadow in " + this);
        }
        if (context.isWildcard()) {
            throw new IllegalStateException("Context is wildcard in " + this);
        }
    }

    @NotNull
    private PrismObject<ShadowType> determineCurrentResourceObject(OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, EncryptionException, NotApplicableException {
        PrismObject<ShadowType> resourceObject;
        LOGGER.trace("Going to determine current resource object");

        if (resourceObjectChange.getResourceObject() != null) {
            resourceObject = resourceObjectChange.getResourceObject().clone();
            LOGGER.trace("-> current object was taken from the resource object change:\n{}", resourceObject.debugDumpLazily());
        } else if (isAdd()) {
            resourceObject = objectDelta.getObjectToAdd().clone();
            LOGGER.trace("-> current object was taken from ADD delta:\n{}", resourceObject.debugDumpLazily());
        } else {
            boolean passiveCaching = context.getCachingStrategy() == CachingStategyType.PASSIVE;
            ReadCapabilityType readCapability = context.getEffectiveCapability(ReadCapabilityType.class);
            boolean canReadFromResource = readCapability != null && !Boolean.TRUE.equals(readCapability.isCachingOnly());
            if (canReadFromResource && (!passiveCaching || isNotificationOnly())) {
                // Either we don't use caching or we have a notification-only change. Such changes mean that we want to
                // refresh the object from the resource.
                Collection<SelectorOptions<GetOperationOptions>> options = beans.schemaService.getOperationOptionsBuilder()
                        .doNotDiscovery().build();
                try {
                    // TODO why we use shadow cache and not resource object converter?!
                    resourceObject = beans.shadowsFacade.getShadow(repoShadow.getOid(), repoShadow, getIdentifiers(),
                            options, context.getTask(), result);
                } catch (ObjectNotFoundException e) {
                    // The object on the resource does not exist (any more?).
                    LOGGER.warn("Object {} does not exist on the resource any more", repoShadow);
                    throw new NotApplicableException();
                }
                LOGGER.trace("-> current object was taken from the resource:\n{}", resourceObject.debugDumpLazily());
            } else if (passiveCaching) {
                resourceObject = repoShadow.clone(); // this might not be correct w.r.t. index-only attributes!
                if (objectDelta != null) {
                    objectDelta.applyTo(resourceObject);
                    markIndexOnlyItemsAsIncomplete(resourceObject);
                    LOGGER.trace("-> current object was taken from old shadow + delta:\n{}", resourceObject.debugDumpLazily());
                } else {
                    LOGGER.trace("-> current object was taken from old shadow:\n{}", resourceObject.debugDumpLazily());
                }
            } else {
                throw new IllegalStateException("Cannot get current resource object: read capability is not present and passive caching is not configured");
            }
        }
        beans.shadowCaretaker.applyAttributesDefinition(context, resourceObject); // is this really needed?
        return resourceObject;
    }

    private boolean isNotificationOnly() {
        return resourceObjectChange instanceof ResourceObjectAsyncChange &&
                ((ResourceObjectAsyncChange) resourceObjectChange).isNotificationOnly();
    }

    private void applyAttributesDefinition() throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        beans.shadowCaretaker.applyAttributesDefinition(context, repoShadow);
        if (objectDelta != null) {
            beans.shadowCaretaker.applyAttributesDefinition(context, objectDelta);
        }
    }

    public boolean isDelete() {
        return resourceObjectChange.isDelete();
    }

    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return resourceObjectChange.getCurrentObjectClassDefinition();
    }

    /**
     * Index-only items in the resource object delta are necessarily incomplete: their old value was taken from repo
     * (i.e. was empty before delta application). We mark them as such. One of direct consequences is that updateShadow method
     * will know that it cannot use this data to update cached (index-only) attributes in repo shadow.
     */
    private void markIndexOnlyItemsAsIncomplete(PrismObject<ShadowType> resourceObject)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        RefinedObjectClassDefinition ocDef = context.computeCompositeObjectClassDefinition(resourceObject);
        for (RefinedAttributeDefinition<?> attrDef : ocDef.getAttributeDefinitions()) {
            if (attrDef.isIndexOnly()) {
                ItemPath path = ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getItemName());
                LOGGER.trace("Marking item {} as incomplete because it's index-only", path);
                //noinspection unchecked
                resourceObject.findCreateItem(path, Item.class, attrDef, true).setIncomplete(true);
            }
        }
    }

    private String getChannel() {
        return ObjectUtils.defaultIfNull(context.getChannel(), SchemaConstants.CHANNEL_LIVE_SYNC_URI);
    }

    public Collection<ResourceAttribute<?>> getIdentifiers() {
        return resourceObjectChange.getIdentifiers();
    }

    public ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    private void updateRepoShadow(@NotNull PrismObject<ShadowType> resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        // TODO: shadowState MID-5834: We might not want to update exists flag in quantum states
        // TODO should we update the repo even if we obtained current resource object from the cache? (except for e.g. metadata)
        beans.shadowManager.updateShadow(context, resourceObject, objectDelta, repoShadow, null, result);
    }

    private void markRepoShadowTombstone(OperationResult result) throws SchemaException {
        if (!ShadowUtil.isDead(repoShadow) || ShadowUtil.isExists(repoShadow)) {
            beans.shadowManager.markShadowTombstone(repoShadow, result);
        }
    }

    private PrismObject<ShadowType> constructShadowedObject(@NotNull PrismObject<ShadowType> resourceObject, OperationResult result)
            throws CommunicationException, EncryptionException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        assert !isDelete();

        // TODO should we bother merging if we obtained resource object from the cache?
        return localBeans.shadowedObjectConstructionHelper
                .constructShadowedObject(context, repoShadow, resourceObject, result);
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
    private PrismObject<ShadowType> constructShadowedObjectForDeletion(OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException, NotApplicableException,
            ObjectNotFoundException {
        PrismObject<ShadowType> currentShadow;
        try {
            currentShadow = beans.repositoryService.getObject(ShadowType.class, this.repoShadow.getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.debug("Shadow for delete synchronization event {} disappeared recently."
                    + "Skipping this event.", this);
            throw new NotApplicableException();
        }
        context = beans.shadowCaretaker.applyAttributesDefinition(context, currentShadow);
        return currentShadow;
    }

    // todo what if delta is null, oldShadow is null, current is not null?
    public boolean isAdd() {
        return objectDelta != null && objectDelta.isAdd();
    }

    public ResourceObjectShadowChangeDescription getShadowChangeDescription() {
        stateCheck(initializationState.isAfterInitialization(),
                "Do not ask for shadow change description on uninitialized change! %s", this);

        if (shadowedObject == null) {
            stateCheck(initializationState.isError(), "Non-error change without shadowed object? %s", this);
            return null; // This is because in the description the shadowed object must be present. TODO reconsider this.
        }
        try {
            ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
            if (objectDelta != null) {
                objectDelta.setOid(shadowedObject.getOid());
            }
            shadowChangeDescription.setObjectDelta(objectDelta);
            shadowChangeDescription.setResource(context.getResource().asPrismObject());
            shadowChangeDescription.setSourceChannel(getChannel());
            shadowChangeDescription.setShadowedResourceObject(shadowedObject);
            return shadowChangeDescription;
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException e) {
            // The resource should have been already resolved. (It is the only source of exceptions.)
            throw new SystemException("Unexpected exception while creating shadow change description", e);
        }
    }

    public Object getPrimaryIdentifierValue() {
        return resourceObjectChange.getPrimaryIdentifierRealValue();
    }

    public int getSequentialNumber() {
        return resourceObjectChange.getLocalSequenceNumber();
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObjectChange=" + resourceObjectChange +
                ", repoShadow OID " + (repoShadow != null ? repoShadow.getOid() : null) +
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
        DebugUtil.debugDumpWithLabelLn(sb, "repoShadow", repoShadow, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedObject", shadowedObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "context", String.valueOf(context), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }

    public String getShadowOid() {
        return repoShadow != null ? repoShadow.getOid() : null;
    }

    public PrismObject<ShadowType> getShadowedObject() {
        return shadowedObject;
    }
}
