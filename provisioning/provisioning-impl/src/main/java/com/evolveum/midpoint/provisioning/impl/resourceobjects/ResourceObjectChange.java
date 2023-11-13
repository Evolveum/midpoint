/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEvent;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEventListener;
import com.evolveum.midpoint.provisioning.impl.AlreadyInitializedObject;
import com.evolveum.midpoint.provisioning.impl.InitializableObjectMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.UcfChange;
import com.evolveum.midpoint.provisioning.util.ErrorState;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Change (live sync, async update, or external) represented at the level of resource object
 * converter, i.e. completely processed - except for repository (shadow) connection.
 *
 * Usually derived from {@link UcfChange} but may be also provided externally -
 * see {@link ExternalResourceEventListener#notifyEvent(ExternalResourceEvent, Task, OperationResult)}.
 */
public abstract class ResourceObjectChange extends AbstractResourceEntity {

    /**
     * Sequence number that is local to the current live sync or async update operation.
     * It is used to ensure related changes are processed in the same order in which they came.
     *
     * See {@link UcfChange#localSequenceNumber}.
     */
    private final int localSequenceNumber;

    /**
     * Real value of the primary identifier of the object.
     * In initialized/OK state it must not be null.
     *
     * See {@link UcfChange#primaryIdentifierValue}.
     */
    private final Object primaryIdentifierRealValue;

    /**
     * Definition of the resource object present at change creation.
     * For UCF-based changes it comes from UcfChange.
     *
     * In OK state it must be present, except for LS delete deltas where it MAY be null.
     * See {@link UcfChange#resourceObjectDefinition}.
     *
     * Refined during initialization into {@link #resourceObjectDefinition}.
     */
    private final ResourceObjectDefinition initialResourceObjectDefinition;

    /**
     * Resource object definition as determined from {@link #effectiveCtx}.
     * May be null in exceptional cases (wildcard LS with delete event with object class not provided).
     */
    protected ResourceObjectDefinition resourceObjectDefinition;

    /**
     * All identifiers of the object.
     *
     * The collection is unmodifiable after this object is initialized.
     * The elements should be mutable because of possible future definition (re)application.
     *
     * This is why we don't use immutable {@link ResourceObjectIdentifiers} here (for now).
     *
     * See {@link UcfChange#identifiers}.
     *
     * After initialization it should contain either "real" identifiers, or an artificially crafted
     * one (from {@link #primaryIdentifierRealValue} - if possible. See {@link #checkConsistence()}.
     */
    @NotNull protected Collection<ResourceAttribute<?>> identifiers;

    /**
     * Delta from the resource - if known.
     * Definitions from the resource schema should be applied (in initialized/OK state). - TODO clarify + check
     *
     * See {@link UcfChange#objectDelta} and {@link ExternalResourceEvent#objectDelta}.
     */
    protected final ObjectDelta<ShadowType> objectDelta;

    /**
     * Resource object after the change - if known.
     * This is the reference to the original resource object as received from UCF.
     * It may be modified during initialization of this instance.
     *
     * See {@link UcfChange#resourceObject} and {@link ExternalResourceEvent#resourceObject}.
     */
    @Nullable private final ResourceObject rawResourceObject;

    /**
     * The completed (processed, finalized) form of {@link #rawResourceObject}.
     *
     * NOTE: The following conditions apply for LS/AU. The Ext is in "half-implementation" state.
     *
     * 1. When initialized-OK: The same object, with:
     *    a. protected flag set,
     *    b. exists flag not null,
     *    c. simulated activation done,
     *    d. associations fetched (if requested),
     *    e. definitions from the resource schema are applied,
     *    f. for LS: correct attributes-to-get present.
     *
     * 2. When initialized with error:
     *    a. has primary identifier present, assuming: object class known + primary identifier value known.
     *
     * 3. When not applicable or when the prerequisite initialization failed: this value is `null`.
     *
     * @see #getCompleteResourceObject()
     */
    CompleteResourceObject completeResourceObject;

    /** The initialization state for this change. */
    @NotNull private final InitializationState initializationState = InitializationState.created();

    /** Represents the status if the original UCF change. */
    @NotNull private final AlreadyInitializedObject ucfChangeStatus;

    ResourceObjectChange(
            int localSequenceNumber,
            Object primaryIdentifierRealValue,
            ResourceObjectDefinition initialResourceObjectDefinition,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            @Nullable ResourceObject rawResourceObject,
            ObjectDelta<ShadowType> objectDelta,
            @NotNull ErrorState initialErrorState,
            @NotNull ProvisioningContext originalContext) {
        super(originalContext);
        this.localSequenceNumber = localSequenceNumber;
        this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.initialResourceObjectDefinition = initialResourceObjectDefinition;
        this.identifiers = new ArrayList<>(identifiers);
        this.rawResourceObject = rawResourceObject;
        this.objectDelta = objectDelta;
        this.ucfChangeStatus = AlreadyInitializedObject.of(initialErrorState);
    }

    ResourceObjectChange(
            @NotNull UcfChange ucfChange, @NotNull ProvisioningContext originalContext) {
        this(ucfChange.getLocalSequenceNumber(),
                ucfChange.getPrimaryIdentifierValue(),
                ucfChange.getResourceObjectDefinition(),
                ucfChange.getIdentifiers(),
                ResourceObject.from(ucfChange),
                ucfChange.getObjectDelta(),
                ErrorState.fromUcfErrorState(ucfChange.getErrorState()),
                originalContext);
    }

    @Override
    public @Nullable InitializableObjectMixin getPrerequisite() {
        return ucfChangeStatus;
    }

    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result)
            throws CommonException, NotApplicableException {
        effectiveCtx = refineProvisioningContext();
        resourceObjectDefinition = effectiveCtx.getObjectDefinition();
        completeResourceObject = processObjectAndDelta(result);
        freezeIdentifiers();
    }

    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result) throws CommonException {
        effectiveCtx = globalCtx;
        addFakePrimaryIdentifierIfNeeded();
        freezeIdentifiers();
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result) {
        throw new IllegalStateException("UCF does not signal 'not applicable' state");
    }

    /** Returns the complete resource object, if present in the original change or it could be fetched. */
    private @Nullable CompleteResourceObject processObjectAndDelta(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, NotApplicableException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        if (rawResourceObject != null) {
            // This may be required for AU, but does not hurt for other cases.
            effectiveCtx.applyAttributesDefinition(rawResourceObject.getPrismObject());
        }
        if (objectDelta != null) {
            effectiveCtx.applyAttributesDefinition(objectDelta);
        }

        if (isDelete()) {
            return CompleteResourceObject.deletedNullable(rawResourceObject);
        }

        AttributesToReturn actualAttributesToReturn = determineAttributesToReturn();
        if (rawResourceObject == null) {
            getLogger().trace("Trying to fetch object {} because it is not in the change", identifiers);
            return fetchResourceObject(actualAttributesToReturn, result);
        }

        // This is a specialty of live synchronization
        if (originalCtx.isWildcard() && attributesToReturnAreDifferent(actualAttributesToReturn)) {
            getLogger().trace("Trying to re-fetch object {} because mismatching attributesToReturn", identifiers);
            return fetchResourceObject(actualAttributesToReturn, result);
        }

        completeResourceObject(effectiveCtx, rawResourceObject, true, result);

        // No exception, so we assume everything went well
        return CompleteResourceObject.of(rawResourceObject, ErrorState.ok());
    }

    @Nullable AttributesToReturn determineAttributesToReturn() {
        return effectiveCtx.createAttributesToReturn();
    }

    boolean attributesToReturnAreDifferent(AttributesToReturn actualAttributesToReturn) {
        return false;
    }

    private @Nullable CompleteResourceObject fetchResourceObject(AttributesToReturn attributesToReturn, OperationResult result)
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
                            attributesToReturn,
                            null,
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

    public @NotNull Collection<ResourceAttribute<?>> getIdentifiers() {
        return identifiers;
    }

    public ObjectDelta<ShadowType> getObjectDelta() {
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
        } else if (rawResourceObject != null) {
            ErrorState errorState = initializationState.getErrorState();
            assert errorState.isError();
            return CompleteResourceObject.of(rawResourceObject, errorState);
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
                + ", resourceObject=" + MiscUtil.getFirstNonNull(completeResourceObject, rawResourceObject)
                + ", state=" + initializationState
                + toStringExtra() + ")";
    }

    private ProvisioningContext refineProvisioningContext() throws SchemaException, ConfigurationException {
        if (globalCtx.isWildcard()) {
            if (initialResourceObjectDefinition != null) {
                var refinedCtx = globalCtx.spawnForObjectClass(initialResourceObjectDefinition.getTypeName());
                getLogger().trace("Updated provisioning context: {}", refinedCtx);
                return refinedCtx;
            } else {
                schemaCheck(isDelete(), "No object type or class definition in change %s", this);
                return globalCtx;
            }
        } else {
            return globalCtx;
        }
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }

    public Object getPrimaryIdentifierRealValue() {
        return primaryIdentifierRealValue;
    }

    protected abstract String toStringExtra();

    private String getObjectClassLocalName() {
        ResourceObjectDefinition def = getCurrentResourceObjectDefinition();
        return def != null ? def.getTypeName().getLocalPart() : null;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "localSequenceNumber", localSequenceNumber, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierRealValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "initialResourceObjectDefinition", String.valueOf(initialResourceObjectDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectDefinition", String.valueOf(resourceObjectDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "identifiers", identifiers, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectDelta", objectDelta, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", getFirstNonNull(completeResourceObject, rawResourceObject), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "context", String.valueOf(effectiveCtx), indent + 1);

        debugDumpExtra(sb, indent);

        DebugUtil.debugDumpWithLabel(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

    private void freezeIdentifiers() {
        identifiers = Collections.unmodifiableCollection(identifiers);
    }

    private void addFakePrimaryIdentifierIfNeeded() throws SchemaException {
        b.fakeIdentifierGenerator.addFakePrimaryIdentifierIfNeeded(
                identifiers, primaryIdentifierRealValue, getCurrentResourceObjectDefinition());
    }

    /**
     * @return The most precise object definition known at this moment. (May be null.)
     */
    public ResourceObjectDefinition getCurrentResourceObjectDefinition() {
        if (resourceObjectDefinition != null) {
            return resourceObjectDefinition;
        } else {
            return initialResourceObjectDefinition;
        }
    }

    private boolean hasDefinition() {
        return getCurrentResourceObjectDefinition() != null;
    }

    @Override
    public void checkConsistence() throws SchemaException {
        InitializationState state = getInitializationState();

        if (!state.isInitialized() || !state.isOk()) {
            return;
        }

        stateCheck(primaryIdentifierRealValue != null, "No primary identifier value");

        boolean hasDefinition = hasDefinition();
        stateCheck(isDelete() || hasDefinition, "No resource object definition for non-delete change");

        checkCollectionImmutable(identifiers);
        if (hasDefinition) {
            schemaCheck(!identifiers.isEmpty(), "No identifiers in the container but primary id value is known");
        }

        if (completeResourceObject != null) {
            stateCheck(completeResourceObject.getBean().isExists() != null, "Exists is null");
        }
    }
}
