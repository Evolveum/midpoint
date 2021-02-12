/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadowcache;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.shadowcache.sync.ChangeProcessingBeans;
import com.evolveum.midpoint.provisioning.impl.shadowcache.sync.SkipProcessingException;

import com.evolveum.midpoint.provisioning.util.ProcessingState;

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
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
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

import static java.util.Objects.requireNonNull;

/**
 * Change that was "adopted" at the level of ShadowCache.
 *
 * This means that it is connected to repository shadow, and this shadow is updated
 * with the appropriate information.
 *
 * TODO finish this class
 */
public class AdoptedChange<ROC extends ResourceObjectChange> implements InitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(AdoptedChange.class);

    /**
     * Original resource object change that is adopted by the shadow cache.
     */
    @NotNull protected final ROC resourceObjectChange;

    /**
     * Context of the processing. In most cases it is taken from the original change.
     * But for wildcard delete changes it is clarified using existing repo shadow.
     * After pre-processing it is no longer wildcard.
     */
    private ProvisioningContext context;

    // TODO reconsider, probably remove
    private final boolean simulate;

    @NotNull protected final ProcessingState processingState;

    @NotNull protected final ChangeProcessingBeans beans;

    @NotNull protected final LocalBeans localBeans;

    // TODO ???
    protected final ObjectDelta<ShadowType> objectDelta;

    /**
     * Normally, this is "shadowized" current resource object. (For resources without read capability it is the cached version.)
     *
     * For delete deltas, it is the current shadow, with applied definitions.
     * TODO reconsider this
     */
    protected PrismObject<ShadowType> currentResourceObject;

    /**
     * Repository shadow of the changed resource object.
     *
     * It is either the "old" one (i.e. existing before we learned about the change)
     * or a newly created one. In any case, the repository shadow itself is UPDATED as part of
     * preprocessing of this change.
     */
    private PrismObject<ShadowType> repoShadow;

    /**
     * Resulting change description that is propagated outwards.
     */
    private ResourceObjectShadowChangeDescription shadowChangeDescription;

    public AdoptedChange(@NotNull ROC resourceObjectChange, boolean simulate, ChangeProcessingBeans beans) {
        this.processingState = ProcessingState.fromLowerLevelState(resourceObjectChange.getProcessingState());
        this.resourceObjectChange = resourceObjectChange;
        this.context = resourceObjectChange.getContext();
        this.simulate = simulate;
        this.beans = beans;
        this.localBeans = beans.shadowCache.getLocalBeans();
        this.objectDelta = CloneUtil.clone(resourceObjectChange.getObjectDelta());
        this.currentResourceObject = CloneUtil.clone(resourceObjectChange.getResourceObject());
    }

    @Override
    public void initializeInternal(Task task, OperationResult result)
            throws CommonException, SkipProcessingException, EncryptionException {

        if (isDelete()) {
            lookupShadow(result);
            updateProvisioningContextFromRepoShadow();
        } else {
            acquireShadow(result);
        }

        assert repoShadow != null;
        assert !context.isWildcard();

        applyAttributesDefinition();

        LOGGER.trace("Initializing change, old shadow: {}", ShadowUtil.shortDumpShadowLazily(repoShadow));

        if (currentResourceObject == null && !isDelete()) {
            currentResourceObject = determineCurrentResourceObject(result);
        }

        updateRepoShadow(result);

        // TODO clean up
        if (objectDelta != null && objectDelta.getOid() == null) {
            objectDelta.setOid(repoShadow.getOid());
        }

        if (isDelete()) {
            markRepoShadowTombstone(result);
            setDeletedCurrentResourceObject(result); // TODO remove
        }

        shadowChangeDescription = createResourceShadowChangeDescription();
    }

    public void checkConsistence() {
        if (!isPreprocessed()) {
            return;
        }

        if (repoShadow == null) {
            throw new IllegalStateException("No repository shadow in " + this);
        }
        if (context.isWildcard()) {
            throw new IllegalStateException("Context is wildcard in " + this);
        }
        if (shadowChangeDescription == null) {
            throw new IllegalStateException("No shadow change description in " + this);
        }
    }

    private void updateProvisioningContextFromRepoShadow() {
        assert repoShadow != null;
        assert isDelete();
        if (context.isWildcard()) {
            context = context.spawn(repoShadow);
        }
    }

    @NotNull
    private PrismObject<ShadowType> determineCurrentResourceObject(OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, EncryptionException, SkipProcessingException {
        PrismObject<ShadowType> resourceObject;
        LOGGER.trace("Going to compute current resource object because it's null and delta is not delete");
        // Temporary measure: let us determine the current resource object; either by fetching it from the resource
        // (if possible) or by taking cached values and applying the delta. In the future we might implement
        // pure delta changes that do not need to know the current state.
        if (isAdd()) {
            resourceObject = objectDelta.getObjectToAdd().clone();
            LOGGER.trace("-> current object was taken from ADD delta:\n{}", resourceObject.debugDumpLazily());
        } else {
            boolean passiveCaching = context.getCachingStrategy() == CachingStategyType.PASSIVE;
            ReadCapabilityType readCapability = context.getEffectiveCapability(ReadCapabilityType.class);
            boolean canReadFromResource = readCapability != null && !Boolean.TRUE.equals(readCapability.isCachingOnly());
            if (canReadFromResource && (!passiveCaching || isNotificationOnly())) {
                // Either we don't use caching or we have a notification-only change. Such changes mean that we want to
                // refresh the object from the resource.
                Collection<SelectorOptions<GetOperationOptions>> options = beans.schemaHelper.getOperationOptionsBuilder()
                        .doNotDiscovery().build();
                try {
                    // TODO why we use shadow cache and not resource object converter?!
                    resourceObject = beans.shadowCache.getShadow(repoShadow.getOid(), repoShadow, getIdentifiers(),
                            options, context.getTask(), result);
                } catch (ObjectNotFoundException e) {
                    // The object on the resource does not exist (any more?).
                    LOGGER.warn("Object {} does not exist on the resource any more", repoShadow);
                    throw new SkipProcessingException();
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
        return resourceObject;
    }

    private boolean isNotificationOnly() {
        return resourceObjectChange instanceof ResourceObjectAsyncChange &&
                ((ResourceObjectAsyncChange) resourceObjectChange).isNotificationOnly();
    }

    private void applyAttributesDefinition() throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (repoShadow != null) {
            beans.shadowCaretaker.applyAttributesDefinition(context, repoShadow);
        }
        if (objectDelta != null) {
            beans.shadowCaretaker.applyAttributesDefinition(context, objectDelta);
        }
        if (currentResourceObject != null) {
            // already done in some cases, todo clarify this
            beans.shadowCaretaker.applyAttributesDefinition(context, currentResourceObject);
        }
    }

    // For delete deltas we don't bother with creating a shadow if it does not exist.
    private void lookupShadow(OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, SkipProcessingException {
        assert isDelete();
        // This context is the best we know at this moment. It is possible that it is wildcard (no OC known).
        // But the only way how to detect the OC is to read existing repo shadow. So we must take the risk
        // of guessing identifiers' definition correctly - in other words, assuming that these definitions are
        // the same for all the object classes on the given resource.
        ProvisioningContext context = resourceObjectChange.getContext();
        repoShadow = beans.shadowManager.lookupLiveOrAnyShadowByPrimaryIds(context, resourceObjectChange.getIdentifiers(), result);
        if (repoShadow == null) {
            LOGGER.debug("No old shadow for delete synchronization event {}, we probably did not know about "
                    + "that object anyway, so well be ignoring this event", this);
            throw new SkipProcessingException();
        }
    }

    private void acquireShadow(OperationResult result) throws SchemaException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, EncryptionException {
        assert !isDelete();

        PrismProperty<?> primaryIdentifier = resourceObjectChange.getPrimaryIdentifierRequired();
        QName objectClass = getObjectClassDefinition().getTypeName();

        repoShadow = localBeans.adoptionHelper.acquireRepoShadow(context, primaryIdentifier, objectClass,
                this::createResourceObjectFromChange, result);
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
        if (resourceObjectChange.getObjectClassDefinition() == null) {
            throw new IllegalStateException("Could not create shadow from change description. Object class is not specified.");
        }
        ShadowType fakeResourceObject = new ShadowType(beans.prismContext);
        fakeResourceObject.setObjectClass(resourceObjectChange.getObjectClassDefinition().getTypeName());
        ResourceAttributeContainer attributeContainer = resourceObjectChange.getObjectClassDefinition()
                .toResourceAttributeContainerDefinition().instantiate();
        fakeResourceObject.asPrismObject().add(attributeContainer);
        for (ResourceAttribute<?> identifier : resourceObjectChange.getIdentifiers()) {
            attributeContainer.add(identifier.clone());
        }
        return fakeResourceObject.asPrismObject();
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

    private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription() throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
        shadowChangeDescription.setObjectDelta(objectDelta);
        shadowChangeDescription.setResource(context.getResource().asPrismObject());
        shadowChangeDescription.setOldShadow(repoShadow);
        shadowChangeDescription.setSourceChannel(getChannel());
        shadowChangeDescription.setSimulate(simulate);
        shadowChangeDescription.setCurrentShadow(currentResourceObject);
        return shadowChangeDescription;
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

    private void updateRepoShadow(OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        // TODO why this?
        ProvisioningUtil.setProtectedFlag(context, repoShadow, beans.matchingRuleRegistry,
                beans.relationRegistry, beans.expressionFactory, result);

        if (currentResourceObject != null) {
            // TODO do we need to complete the shadow now? Why? MID-5834
            currentResourceObject = getAdoptionHelper().constructReturnedObject(context, repoShadow, currentResourceObject, result);
            // TODO: shadowState MID-5834
            beans.shadowManager.updateShadow(context, currentResourceObject, objectDelta, repoShadow, null, result);
        }
    }

    private void markRepoShadowTombstone(OperationResult result) throws SchemaException {
        if (!ShadowUtil.isDead(repoShadow) || ShadowUtil.isExists(repoShadow)) {
            beans.shadowManager.markShadowTombstone(repoShadow, result);
        }
    }

    /**
     * It looks like the current resource object should be present also for DELETE deltas.
     * TODO clarify this
     * TODO try to avoid repository get operation by applying known deltas to existing repo shadow object
     *
     * So until clarified, we provide here the shadow object, with properly applied definitions.
     */
    private void setDeletedCurrentResourceObject(OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException, SkipProcessingException,
            ObjectNotFoundException {
        PrismObject<ShadowType> currentShadow;
        try {
            currentShadow = beans.repositoryService.getObject(ShadowType.class, this.repoShadow.getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.debug("Shadow for delete synchronization event {} disappeared recently."
                    + "Skipping this event.", this);
            throw new SkipProcessingException();
        }
        context = beans.shadowCaretaker.applyAttributesDefinition(context, currentShadow);
        currentResourceObject = currentShadow;
    }

    public PrismObject<ShadowType> getCurrentResourceObject() {
        return currentResourceObject;
    }

    // todo what if delta is null, oldShadow is null, current is not null?
    public boolean isAdd() {
        return objectDelta != null && objectDelta.isAdd();
    }

    public ResourceObjectShadowChangeDescription getShadowChangeDescription() {
        return shadowChangeDescription;
    }

    public boolean isPreprocessed() {
        return processingState.isInitialized();
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

    public @NotNull ProcessingState getProcessingState() {
        return processingState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObjectChange=" + resourceObjectChange +
                ", processingState=" + processingState +
                ", repoShadow OID " + (repoShadow != null ? repoShadow.getOid() : null) +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectChange", resourceObjectChange, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "context", String.valueOf(context), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "processingState", String.valueOf(processingState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "repoShadow", repoShadow, indent + 1);
        return sb.toString();
    }

    public String getShadowOid() {
        return repoShadow != null ? repoShadow.getOid() : null;
    }

    private AdoptionHelper getAdoptionHelper() {
        return beans.shadowCache.getLocalBeans().adoptionHelper;
    }
}
