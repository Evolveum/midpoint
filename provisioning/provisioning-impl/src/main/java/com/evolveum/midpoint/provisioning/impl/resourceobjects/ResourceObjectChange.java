/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import static java.util.Collections.emptySet;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectPrimaryIdentifiers;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEvent;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEventListener;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfChange;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * Change (live sync, async update, or external) represented at the level of resource object
 * converter, i.e. completely processed - except for repository (shadow) connection.
 *
 * Usually derived from {@link UcfChange} but may be also provided externally -
 * see {@link ExternalResourceEventListener#notifyEvent(ExternalResourceEvent, Task, OperationResult)}.
 */
public abstract class ResourceObjectChange implements InitializableMixin {

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
     * See {@link UcfChange#primaryIdentifierRealValue}.
     */
    private final Object primaryIdentifierRealValue;

    /**
     * Definition of the resource object present at change creation.
     * For UCF-based changes it came from UcfChange.
     *
     * In OK state it must be present, except for LS delete deltas where it MAY be null.
     * See {@link UcfChange#resourceObjectDefinition}.
     *
     * Refined during initialization, see {@link #resourceObjectDefinition}.
     */
    private final ResourceObjectDefinition initialResourceObjectDefinition;

    /**
     * Resource object definition as determined from (possibly updated) provisioning context.
     * May be null in exceptional cases (wildcard LS with delete event with object class not provided).
     */
    protected ResourceObjectDefinition resourceObjectDefinition;

    /**
     * All identifiers of the object.
     *
     * The collection is unmodifiable after this object is initialized.
     * The elements should be mutable because of possible future definition (re)application.
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
     *
     * The following conditions apply for LS/AU. The Ext is in "half-implementation" state.
     *
     * 1. When created: Object as received from UCF.
     *
     * 2. When initialized-OK: The same object, with:
     *    a. protected flag set,
     *    b. exists flag not null,
     *    c. simulated activation done,
     *    d. associations fetched,
     *    e. for LS: correct attributes-to-get present
     *    f. for AU: definitions from the resource schema are applied (TODO clarify)
     *
     * 3. When initialized-error:
     *    a. has primary identifier present, assuming: object class known + primary identifier value known.
     *
     * 4. When initialized-not-applicable:
     *    a. Nothing guaranteed.
     *
     * 5. If initialization failed:
     *    a. Nothing guaranteed.
     *
     * See {@link UcfChange#resourceObject} and {@link ExternalResourceEvent#resourceObject}.
     */
    protected PrismObject<ShadowType> resourceObject;

    /** The initialization state for this change. */
    @NotNull protected final InitializationState initializationState;

    /**
     * Provisioning context specific to this change.
     *
     * Original value provided at creation time. Refined during initialization, when specific object/change is known.
     */
    @NotNull protected ProvisioningContext context;

    @NotNull protected final ResourceObjectsBeans beans;

    ResourceObjectChange(
            int localSequenceNumber,
            Object primaryIdentifierRealValue,
            ResourceObjectDefinition initialResourceObjectDefinition,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            PrismObject<ShadowType> resourceObject,
            ObjectDelta<ShadowType> objectDelta,
            @NotNull InitializationState initializationState,
            @NotNull ProvisioningContext originalContext,
            @NotNull ResourceObjectsBeans beans) {
        this.localSequenceNumber = localSequenceNumber;
        this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.initialResourceObjectDefinition = initialResourceObjectDefinition;
        this.identifiers = new ArrayList<>(identifiers);
        this.resourceObject = resourceObject;
        this.objectDelta = objectDelta;
        this.initializationState = initializationState;
        this.context = originalContext;
        this.beans = beans;
    }

    ResourceObjectChange(
            UcfChange ucfChange,
            Exception preInitializationException,
            @NotNull ProvisioningContext originalContext,
            ResourceObjectsBeans beans) {
        this(ucfChange.getLocalSequenceNumber(),
                ucfChange.getPrimaryIdentifierRealValue(),
                ucfChange.getResourceObjectDefinition(),
                ucfChange.getIdentifiers(),
                ucfChange.getResourceObject(),
                ucfChange.getObjectDelta(),
                InitializationState.fromUcfErrorState(ucfChange.getErrorState(), preInitializationException),
                originalContext,
                beans);
    }

    /**
     * The meat is in subclasses. (In the future we might pull up common parts here.)
     */
    @Override
    public void initializeInternal(Task task, OperationResult result)
            throws CommonException, NotApplicableException {

        if (initializationState.isInitialStateOk()) {
            updateProvisioningContext(task);
            setResourceObjectDefinition();

            processObjectAndDelta(result);
        } else {
            addFakePrimaryIdentifierIfNeeded();
        }

        freezeIdentifiers();
    }

    /**
     * TODO there are strange differences among LS, AU, Ext implementations. Investigate.
     */
    protected abstract void processObjectAndDelta(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, NotApplicableException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException;

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
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

    public PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public ShadowType getResourceObjectBean() {
        return asObjectable(resourceObject);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(seq=" + localSequenceNumber
                + ", uid=" + primaryIdentifierRealValue
                + ", class=" + getObjectClassLocalName()
                + ", identifiers=" + identifiers
                + ", objectDelta=" + objectDelta
                + ", resourceObject=" + resourceObject
                + ", state=" + initializationState
                + toStringExtra() + ")";
    }

    private void updateProvisioningContext(@NotNull Task task) throws SchemaException, ConfigurationException {

        schemaCheck(initialResourceObjectDefinition != null || isDelete() && context.isWildcard(),
                "No object type or class definition in change %s", this);

        ProvisioningContext contextBefore = context;

        if (context.isWildcard()) {
            if (initialResourceObjectDefinition != null) {
                context = applyObjectClassAndTask(initialResourceObjectDefinition.getTypeName(), task);
                assert !context.isWildcard();
            } else {
                assert isDelete(); // see the schema check above
                context = applyTask(task);
            }
        } else {
            assert initialResourceObjectDefinition != null || !isDelete(); // see the schema check above
            context = applyTask(task);
            assert !context.isWildcard();
        }

        if (context != contextBefore) {
            getLogger().trace("Updated provisioning context: {}", context);
        }
    }

    /** Post-condition: Returned context is not wildcard. */
    private ProvisioningContext applyObjectClassAndTask(@NotNull QName ocName, @NotNull Task task)
            throws SchemaException, ConfigurationException {
        // We know that current context has now OC name
        if (task != context.getTask()) {
            return context.spawnForObjectClass(task, ocName);
        } else {
            return context.spawnForObjectClass(ocName);
        }
    }

    private ProvisioningContext applyTask(Task task) {
        if (task != context.getTask()) {
            return context.spawn(task);
        } else {
            return context;
        }
    }

    protected void setResourceObjectDefinition() {
        resourceObjectDefinition = context.getObjectDefinition();
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }

    public Object getPrimaryIdentifierRealValue() {
        return primaryIdentifierRealValue;
    }

    public @NotNull ProvisioningContext getContext() {
        return context;
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
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "context", String.valueOf(context), indent + 1);

        debugDumpExtra(sb, indent);

        DebugUtil.debugDumpWithLabel(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

    private void freezeIdentifiers() {
        identifiers = Collections.unmodifiableCollection(identifiers);
    }

    private void addFakePrimaryIdentifierIfNeeded() throws SchemaException {
        beans.fakeIdentifierGenerator.addFakePrimaryIdentifierIfNeeded(
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

    /**
     * @return Primary identifiers selected from the list of all identifiers known for this change.
     */
    public Collection<ResourceAttribute<?>> getPrimaryIdentifiers() {
        ResourceObjectDefinition definition = getCurrentResourceObjectDefinition();
        if (definition != null) {
            return selectPrimaryIdentifiers(identifiers, definition);
        } else {
            return emptySet(); // Or should we throw an exception right here?
        }
    }

    public ResourceAttribute<?> getPrimaryIdentifierRequired() throws SchemaException {
        return MiscUtil.extractSingletonRequired(getPrimaryIdentifiers(),
                () -> new SchemaException("Multiple primary identifiers in " + this),
                () -> new SchemaException("No primary identifier in " + this));
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

        if (resourceObject != null) {
            stateCheck(resourceObject.asObjectable().isExists() != null, "Exists is null");
            // Unfortunately, other aspects cannot be checked here.
        }
    }
}
