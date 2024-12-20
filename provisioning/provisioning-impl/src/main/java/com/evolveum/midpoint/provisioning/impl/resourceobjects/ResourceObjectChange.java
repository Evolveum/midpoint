/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.*;

import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEvent;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEventListener;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.ucf.api.ShadowItemsToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.UcfChange;
import com.evolveum.midpoint.provisioning.util.ErrorState;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * *Lazily initialized change* (live sync, async update, or external) represented at the level of resource objects package.
 *
 * Usually derived from {@link UcfChange} but may be also provided externally -
 * see {@link ExternalResourceEventListener#notifyEvent(ExternalResourceEvent, Task, OperationResult)}.
 */
public abstract class ResourceObjectChange extends AbstractLazilyInitializableResourceEntity {

    /**
     * Sequence number that is local to the current live sync or async update operation.
     * It is used to ensure related changes are processed in the same order in which they came.
     *
     * See {@link UcfChange#localSequenceNumber}.
     */
    private final int localSequenceNumber;

    /**
     * Real value of the primary identifier of the object.
     *
     * See {@link UcfChange#primaryIdentifierValue}.
     */
    @NotNull private final Object primaryIdentifierRealValue;

    /**
     * Definition of the resource object present at change creation.
     * For UCF-based changes it comes from UcfChange.
     *
     * In OK state it must be present, except for LS delete deltas where it MAY be null.
     * See {@link UcfChange#resourceObjectDefinition}.
     */
    @Nullable private final ResourceObjectDefinition resourceObjectDefinition;

    /**
     * All identifiers of the object. Immutable.
     *
     * See {@link UcfChange#identifiers}.
     */
    @NotNull protected Collection<ShadowSimpleAttribute<?>> identifiers;

    /**
     * Delta from the resource - if known.
     * Definitions from the resource schema should be applied (in initialized/OK state). - TODO clarify + check
     *
     * See {@link UcfChange#objectDelta} and {@link ExternalResourceEvent#objectDelta}.
     */
    @Nullable protected final ObjectDelta<ShadowType> objectDelta;

    /**
     * Resource object after the change - if known.
     * This is the reference to the original resource object as received from UCF.
     * It may be modified during initialization of this instance.
     *
     * See {@link UcfChange#resourceObject} and {@link ExternalResourceEvent#resourceObject}.
     */
    @Nullable final UcfResourceObject ucfResourceObject;

    /**
     * The completed (processed, finalized) form of {@link #ucfResourceObject}.
     *
     * @see #getCompleteResourceObject()
     */
    CompleteResourceObject completeResourceObject;

    /** Represents the status if the original UCF change. */
    @NotNull private final ErrorState initialErrorState;

    ResourceObjectChange(
            int localSequenceNumber,
            @NotNull Object primaryIdentifierRealValue,
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<ShadowSimpleAttribute<?>> identifiers,
            @Nullable UcfResourceObject ucfResourceObject,
            @Nullable ObjectDelta<ShadowType> objectDelta,
            @NotNull ErrorState initialErrorState,
            @NotNull ProvisioningContext originalContext) {
        super(originalContext, true);
        this.localSequenceNumber = localSequenceNumber;
        this.primaryIdentifierRealValue = Objects.requireNonNull(primaryIdentifierRealValue);
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.identifiers = List.copyOf(identifiers);
        this.ucfResourceObject = determineUcfResourceObject(ucfResourceObject, primaryIdentifierRealValue, objectDelta);
        this.objectDelta = objectDelta;
        this.initialErrorState = initialErrorState;
    }

    /**
     * Although the UCF change of ADD type should contain the object in {@link UcfChange#resourceObject}, let us try to
     * obtain one from ADD delta if it's not the case.
     */
    private static UcfResourceObject determineUcfResourceObject(
            UcfResourceObject ucfResourceObject, Object primaryIdentifierRealValue, ObjectDelta<ShadowType> objectDelta) {
        if (ucfResourceObject != null) {
            return ucfResourceObject;
        } else if (ObjectDelta.isAdd(objectDelta)) {
            return UcfResourceObject.of(objectDelta.getObjectToAdd(), primaryIdentifierRealValue);
        } else {
            return null;
        }
    }

    ResourceObjectChange(@NotNull UcfChange ucfChange, @NotNull ProvisioningContext originalContext) {
        this(ucfChange.getLocalSequenceNumber(),
                ucfChange.getPrimaryIdentifierValue(),
                ucfChange.getResourceObjectDefinition(),
                ucfChange.getIdentifiers(),
                ucfChange.getResourceObject(),
                ucfChange.getObjectDelta(),
                ErrorState.fromUcfErrorState(ucfChange.getErrorState()),
                originalContext);
    }

    @Override
    public void initializeInternal(Task task, OperationResult result) throws CommonException, NotApplicableException {
        effectiveCtx = originalCtx.spawn(
                ucfResourceObject != null ? ucfResourceObject.getBean().getAuxiliaryObjectClass() : List.of(),
                task);
        if (initialErrorState.isOk()) {
            effectiveCtx = refineProvisioningContext(); // FIXME what about auxiliary OCs in the refined context?
            completeResourceObject = processObjectAndDelta(result);
        } else {
            getInitializationState().recordError(initialErrorState);
        }
    }

    /**
     * TODO description
     * Returns the complete resource object, if present in the original change or it could be fetched.
     */
    private @Nullable CompleteResourceObject processObjectAndDelta(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, NotApplicableException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

//        if (ucfResourceObject != null) {
//            effectiveCtx = effectiveCtx.applyDefinitionInNewCtx(ucfResourceObject.getPrismObject());
//        }
//
//        var definition = effectiveCtx.getObjectDefinition();
//        if (definition != null) {
//            if (objectDelta != null) {
//                effectiveCtx.applyDefinition(objectDelta);
//            }
//            applyDefinitionToIdentifiers(definition);
//        }

        if (isDelete()) {
            if (ucfResourceObject != null) {
                return CompleteResourceObject.ofDeleted(
                        ExistingResourceObjectShadow.fromUcf(ucfResourceObject, effectiveCtx.getResourceRef(), false));
            } else {
                return null;
            }
        }

        ShadowItemsToReturn actualShadowItemsToReturn = determineAttributesToReturn();
        if (ucfResourceObject == null) {
            if (!(this instanceof ResourceObjectAsyncChange)) {
                getLogger().trace("Trying to fetch object {} because it is not in the change", identifiers);
                return fetchResourceObject(actualShadowItemsToReturn, result);
            } else {
                // Really ugly hack to make story TestGrouperAsyncUpdate pass.
                // The problem is that if we fetch the object (a group) here, it will be without the membership, as it's
                // with fetchStrategy=MINIMAL. And, it won't get updated in ShadowedChange from the delta.
                // TODO fix this someday
                getLogger().trace("NOT fetching resource object {} because this is an asynchronous change, will be fetched later",
                        identifiers);
                return null;
            }
        }

        // This is a specialty of live synchronization
        if (originalCtx.isWildcard() && attributesToReturnAreDifferent(actualShadowItemsToReturn)) {
            getLogger().trace("Trying to re-fetch object {} because mismatching attributesToReturn", identifiers);
            return fetchResourceObject(actualShadowItemsToReturn, result);
        }

        // effectiveCtx is already related to the shadow
        ExistingResourceObjectShadow resourceObject = ExistingResourceObjectShadow.fromUcf(ucfResourceObject, effectiveCtx.getResourceRef());

        return ResourceObjectCompleter.completeResourceObject(effectiveCtx, resourceObject, fetchAssociations, result);
    }

    @Nullable ShadowItemsToReturn determineAttributesToReturn() {
        return effectiveCtx.createItemsToReturn();
    }

    boolean attributesToReturnAreDifferent(ShadowItemsToReturn actualShadowItemsToReturn) {
        return false;
    }

    private @Nullable CompleteResourceObject fetchResourceObject(ShadowItemsToReturn shadowItemsToReturn, OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, NotApplicableException {
        if (!effectiveCtx.hasRealReadCapability()) {
            getLogger().trace("NOT fetching object {} because the resource does not support it", identifiers);
            return null;
        }
        var primaryIdentification =
                ResourceObjectIdentification.fromAttributes(effectiveCtx.getObjectDefinitionRequired(), identifiers)
                        .ensurePrimary();
        try {
            // todo consider whether it is always necessary to fetch the entitlements
            return b.resourceObjectConverter
                    .fetchResourceObject(
                            effectiveCtx,
                            primaryIdentification,
                            shadowItemsToReturn,
                            true,
                            result);
        } catch (ObjectNotFoundException ex) {
            result.recordHandledError(
                    "Object related to the change no longer exists on the resource - skipping it.", ex);
            getLogger().warn("Object related to the change no longer exists on the resource - skipping it: " + ex.getMessage());
            throw new NotApplicableException();
        }
    }

    public boolean isDelete() {
        return ObjectDelta.isDelete(objectDelta);
    }

    public boolean isAdd() {
        return ObjectDelta.isAdd(objectDelta);
    }

    public @NotNull Collection<ShadowSimpleAttribute<?>> getIdentifiers() {
        return identifiers;
    }

    public @Nullable ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    /**
     * Returns {@link CompleteResourceObject}, either right from {@link #completeResourceObject} or incomplete one.
     * May return `null` if the object was not provided in the original change, and the resource does not support reading.
     */
    public @Nullable CompleteResourceObject getCompleteResourceObject() {
        checkInitialized();
        if (completeResourceObject != null) {
            return completeResourceObject;
        } else if (ucfResourceObject != null) {
            ErrorState errorState = initializationState.getErrorState();
            assert errorState.isError();
            return CompleteResourceObject.of(
                    ExistingResourceObjectShadow.fromUcf(ucfResourceObject, effectiveCtx.getResourceRef(), !isDelete()),
                    errorState);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(seq=" + localSequenceNumber
                + ", uid=" + primaryIdentifierRealValue
                + ", class=" + getObjectClassLocalName()
                + ", identifiers=" + identifiers
                + ", objectDelta=" + objectDelta
                + ", resourceObject=" + MiscUtil.getFirstNonNull(completeResourceObject, ucfResourceObject)
                + ", state=" + initializationState
                + toStringExtra() + ")";
    }

    private ProvisioningContext refineProvisioningContext() throws SchemaException {
        if (effectiveCtx.isWildcard()) {
            if (resourceObjectDefinition != null) {
                var refinedCtx = effectiveCtx.spawnForDefinition(resourceObjectDefinition);
                getLogger().trace("Updated provisioning context: {}", refinedCtx);
                return refinedCtx;
            } else {
                schemaCheck(isDelete(), "No object type or class definition in change %s", this);
                return effectiveCtx;
            }
        } else {
            return effectiveCtx;
        }
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }

    public @NotNull Object getPrimaryIdentifierRealValue() {
        return primaryIdentifierRealValue;
    }

    protected abstract String toStringExtra();

    private String getObjectClassLocalName() {
        ResourceObjectDefinition def = getResourceObjectDefinition();
        return def != null ? def.getTypeName().getLocalPart() : null;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "localSequenceNumber", localSequenceNumber, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierRealValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectDefinition", String.valueOf(resourceObjectDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "identifiers", identifiers, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectDelta", objectDelta, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", getFirstNonNull(completeResourceObject, ucfResourceObject), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "context", String.valueOf(effectiveCtx), indent + 1);

        debugDumpExtra(sb, indent);

        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

//    private void freezeIdentifiers() {
//        identifiers = Collections.unmodifiableCollection(identifiers);
//    }
//
//    private void addFakePrimaryIdentifierIfNeeded() throws SchemaException {
//        b.fakeIdentifierGenerator.addFakePrimaryIdentifierIfNeeded(
//                identifiers, primaryIdentifierRealValue, getResourceObjectDefinition());
//    }

    /**
     * @return The most precise object definition known at this moment. (May be null.)
     */
    public @Nullable ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    @Override
    public void checkConsistence() throws SchemaException {
        InitializationState state = getInitializationState();

        if (!state.isInitialized() || !state.isOk()) {
            return;
        }

        checkCollectionImmutable(identifiers);

        if (resourceObjectDefinition != null) {
            schemaCheck(!identifiers.isEmpty(), "No identifiers in the container but primary id value is known");
            for (ShadowSimpleAttribute<?> identifier : identifiers) {
                identifier.checkDefinitionConsistence(resourceObjectDefinition);
            }
        } else {
            stateCheck(isDelete(), "No resource object definition for non-delete change");
        }
    }
}
