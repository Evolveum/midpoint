/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectPrimaryIdentifiers;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

import static com.evolveum.midpoint.util.MiscUtil.*;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceEventDescription;
import com.evolveum.midpoint.provisioning.api.ResourceEventListener;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.UcfChange;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Change (live sync, async update, or external) represented at the level of resource object
 * converter, i.e. completely processed - except for repository (shadow) connection.
 *
 * Usually derived from {@link UcfChange} but may be also provided externally -
 * see {@link ResourceEventListener#notifyEvent(ResourceEventDescription, Task, OperationResult)}.
 */
@SuppressWarnings("JavadocReference")
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
     * Must not be null unless {@link #skipFurtherProcessing} is true.
     *
     * See {@link UcfChange#primaryIdentifierRealValue}.
     */
    private final Object primaryIdentifierRealValue;

    /**
     * Definition of the object class. Can be missing for delete deltas.
     *
     * See {@link UcfChange#objectClassDefinition}.
     *
     * Can be updated during initialization. TODO or should we keep it fixed?
     */
    protected ObjectClassComplexTypeDefinition objectClassDefinition;

    /**
     * Refined object class definition as determined from the provisioning context.
     * Kept here e.g. to avoid having to check for exceptions when obtaining from the context.
     */
    protected RefinedObjectClassDefinition refinedObjectClassDefinition;

    /**
     * All identifiers of the object.
     *
     * The collection is unmodifiable after this object is initialized.
     * The elements should not be modified as well, although this is not enforced yet.
     *
     * See {@link UcfChange#identifiers}.
     *
     * After initialization it should contain either "real" identifiers, or an artificially crafted
     * one (from {@link #primaryIdentifierRealValue} - if possible. See {@link #checkConsistence()}.
     */
    @NotNull protected Collection<ResourceAttribute<?>> identifiers;

    /**
     * Delta from the resource - if known.
     * Definitions from the resource schema should be applied (in "processed" state). - TODO clarify + check
     *
     * See {@link UcfChange#objectDelta} and {@link ResourceEventDescription#objectDelta}.
     */
    protected final ObjectDelta<ShadowType> objectDelta;

    /**
     * Resource object after the change - if known.
     * Definitions from the resource schema should be applied (in "processed" state). - TODO clarify + check
     *
     * See {@link UcfChange#resourceObject} and {@link ResourceEventDescription#resourceObject}.
     */
    protected PrismObject<ShadowType> resourceObject;

    @NotNull protected final ProcessingState processingState;

    /**
     * Provisioning context specific for this resource object.
     *
     * Computed during pre-processing.
     */
    protected ProvisioningContext context;

    ResourceObjectChange(int localSequenceNumber, Object primaryIdentifierRealValue,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            PrismObject<ShadowType> resourceObject,
            ObjectDelta<ShadowType> objectDelta,
            @NotNull ProcessingState processingState) {
        this.localSequenceNumber = localSequenceNumber;
        this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.identifiers = identifiers;
        this.resourceObject = resourceObject;
        this.objectDelta = objectDelta;
        this.processingState = processingState;
    }

    ResourceObjectChange(UcfChange ucfChange) {
        this(ucfChange.getLocalSequenceNumber(),
                ucfChange.getPrimaryIdentifierRealValue(),
                ucfChange.getIdentifiers(),
                ucfChange.getResourceObject(),
                ucfChange.getObjectDelta(),
                ProcessingState.fromUcfErrorState(ucfChange.getErrorState()));
        this.objectClassDefinition = ucfChange.getObjectClassDefinition();
    }

    public void setObjectClassDefinition(RefinedObjectClassDefinition definition) {
        this.objectClassDefinition = definition;
    }

    public void setResourceObject(PrismObject<ShadowType> resourceObject) {
        this.resourceObject = resourceObject;
    }

    void setResourceRefIfMissing(String resourceOid) {
        setResourceRefIfMissing(resourceObject, resourceOid);
        if (objectDelta != null) {
            setResourceRefIfMissing(objectDelta.getObjectToAdd(), resourceOid);
        }
    }

    private void setResourceRefIfMissing(PrismObject<ShadowType> object, String resourceOid) {
        if (object != null && object.asObjectable().getResourceRef() == null && resourceOid != null) {
            object.asObjectable().setResourceRef(createObjectRef(resourceOid, ObjectTypes.RESOURCE));
        }
    }

    public @NotNull ProcessingState getProcessingState() {
        return processingState;
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

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(seq=" + localSequenceNumber
                + ", uid=" + primaryIdentifierRealValue
                + ", class=" + getObjectClassLocalName()
                + ", identifiers=" + identifiers
                + ", objectDelta=" + objectDelta
                + ", resourceObject=" + resourceObject
                + ", state=" + processingState
                + toStringExtra() + ")";
    }

    private void checkObjectClassDefinitionPresent(ProvisioningContext originalCtx) throws SchemaException {
        if (objectClassDefinition == null && (!originalCtx.isWildcard() || !isDelete())) {
            throw new SchemaException("No object class definition in change " + this);
        }
    }

    // FIXME this ugly hack with taskToSet
    void determineProvisioningContext(ProvisioningContext originalCtx, Task taskToSet) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        checkObjectClassDefinitionPresent(originalCtx);
        if (originalCtx.isWildcard()) {
            if (objectClassDefinition == null) {
                if (!isDelete()) {
                    throw new SchemaException("No object class definition in change " + this);
                } else {
                    // We accept missing object class definition for delete changes
                    context = spawnContextForNewTaskIfNeeded(originalCtx, taskToSet);
                }
            } else {
                if (taskToSet != null) {
                    context = originalCtx.spawn(objectClassDefinition.getTypeName(), taskToSet);
                } else {
                    context = originalCtx.spawn(objectClassDefinition.getTypeName());
                }
                if (context.isWildcard()) {
                    throw new SchemaException("Unknown object class " + objectClassDefinition.getTypeName()
                            + " found in change " + this);
                }
                objectClassDefinition = context.getObjectClassDefinition();
            }
        } else {
            if (objectClassDefinition == null && !isDelete()) {
                throw new SchemaException("No object class definition in change " + this);
            }
            context = spawnContextForNewTaskIfNeeded(originalCtx, taskToSet);
        }
    }

    protected void updateRefinedObjectClass() throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        refinedObjectClassDefinition = context.getObjectClassDefinition();
    }

    private ProvisioningContext spawnContextForNewTaskIfNeeded(ProvisioningContext originalCtx, Task taskToSet) {
        if (taskToSet != null && taskToSet != originalCtx.getTask()) {
            return originalCtx.spawn(taskToSet);
        } else {
            return originalCtx;
        }
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }

    public Object getPrimaryIdentifierRealValue() {
        return primaryIdentifierRealValue;
    }

    public ProvisioningContext getContext() {
        return context;
    }

    protected abstract String toStringExtra();

    private String getObjectClassLocalName() {
        return objectClassDefinition != null ? objectClassDefinition.getTypeName().getLocalPart() : null;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, 0);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "localSequenceNumber", localSequenceNumber, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierRealValue), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "objectClassDefinition", String.valueOf(objectClassDefinition), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "identifiers", identifiers, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "objectDelta", objectDelta, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "resourceObject", resourceObject, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "context", String.valueOf(context), indent + 1);

        debugDumpExtra(sb, indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "processingState", String.valueOf(processingState), indent + 1);
        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

    protected void completeIdentifiers() throws SchemaException {
        if (processingState.isError()) {
            // We do not want to mess with identifiers if the state is not an error.
            addFakePrimaryIdentifierIfNeeded();
        }
        identifiers = Collections.unmodifiableCollection(identifiers);
    }

    private void addFakePrimaryIdentifierIfNeeded() throws SchemaException {
        if (primaryIdentifierRealValue != null && hasObjectClassDefinition()) {
            if (getPrimaryIdentifiers().isEmpty()) {
                identifiers.add(createFakePrimaryIdentifier());
            }
        }
    }

    /**
     * @return The most precise object class definition known at this moment.
     */
    public ObjectClassComplexTypeDefinition getCurrentObjectClassDefinition() {
        if (refinedObjectClassDefinition != null) {
            return refinedObjectClassDefinition;
        } else {
            return objectClassDefinition;
        }
    }

    public boolean hasObjectClassDefinition() {
        return getCurrentObjectClassDefinition() != null;
    }

    /**
     * @return Primary identifiers selected from the list of all identifiers known for this change.
     */
    public Collection<ResourceAttribute<?>> getPrimaryIdentifiers() {
        ObjectClassComplexTypeDefinition objectClassDefinition = getCurrentObjectClassDefinition();
        if (objectClassDefinition != null) {
            return selectPrimaryIdentifiers(identifiers, objectClassDefinition);
        } else {
            return emptySet(); // Or should we throw an exception?
        }
    }

    public ResourceAttribute<?> getPrimaryIdentifierRequired() throws SchemaException {
        return MiscUtil.extractSingletonRequired(getPrimaryIdentifiers(),
                () -> new SchemaException("Multiple primary identifiers in " + this),
                () -> new SchemaException("No primary identifier in " + this));
    }

    private ResourceAttribute<?> createFakePrimaryIdentifier() throws SchemaException {
        Collection<? extends RefinedAttributeDefinition<?>> primaryIdDefs = refinedObjectClassDefinition.getPrimaryIdentifiers();
        RefinedAttributeDefinition<?> primaryIdDef = MiscUtil.extractSingletonRequired(primaryIdDefs,
                () -> new SchemaException("Multiple primary identifier definitions in " + refinedObjectClassDefinition),
                () -> new SchemaException("No primary identifier definition in " + refinedObjectClassDefinition));
        ResourceAttribute<?> primaryId = primaryIdDef.instantiate();
        //noinspection unchecked
        ((ResourceAttribute<Object>) primaryId).setRealValue(primaryIdentifierRealValue);
        return primaryId;
    }

    @Override
    public void checkConsistence() throws SchemaException {
        if (!getProcessingState().isAfterInitialization()) {
            return;
        }

        stateCheck(isDelete() || hasObjectClassDefinition(), "No object class definition for non-delete change");

        checkCollectionImmutable(identifiers);

        if (primaryIdentifierRealValue != null && hasObjectClassDefinition()) {
            schemaCheck(!identifiers.isEmpty(), "No identifiers in the container but primary id value is known");
        }
    }
}
