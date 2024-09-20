/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents a change event detected by UCF.
 */
public abstract class UcfChange implements DebugDumpable {

    /**
     * Sequence number that is local to the current live sync or async update operation.
     * It is used to ensure related changes are processed in the same order in which they came.
     */
    @Experimental
    private final int localSequenceNumber;

    /**
     * Real value of the primary identifier of the object.
     *
     * Although we have {@link #resourceObject} that contains this value as well, we need it also here because
     * the object can be null.
     */
    @NotNull private final Object primaryIdentifierValue;

    /**
     * Definition of the resource object. Constraints:
     *
     * . LiveSync: if delta is not DELETE or if object class was specified at request -> not null
     * . AsyncUpdate: always not null
     */
    protected final ResourceObjectDefinition resourceObjectDefinition;

    /**
     * All identifiers of the object. Constraints:
     *
     * 1. The collection and its members are immutable.
     * 2. Must contain at least the primary identifier - if {@link #resourceObjectDefinition} is known,
     * unless there is a serious error, like the definition of UID attribute not present in the schema.
     */
    @NotNull private final Collection<ShadowSimpleAttribute<?>> identifiers;

    /**
     * Delta from the resource - if known.
     *
     * For live sync it is filled-in for ADD and DELETE events.
     * For asynchronous updates it should be always present (except for notification-only updates).
     *
     * Correct definitions must be applied.
     */
    @Nullable private final ObjectDelta<ShadowType> objectDelta;

    /**
     * Resource object after the change - if known.
     *
     * It could come e.g. from the ConnId sync delta. Since 4.3, it is present also for LiveSync ADD deltas.
     * It is null if not available or not existing.
     *
     * Correct definitions must be applied.
     */
    @Nullable private final UcfResourceObject resourceObject;

    /**
     * Was there any error while processing the change in UCF layer?
     * I.e. to what extent can we rely on the information in this object?
     */
    @NotNull protected final UcfErrorState errorState;

    UcfChange(
            int localSequenceNumber,
            @NotNull Object primaryIdentifierValue,
            ResourceObjectDefinition objectDefinition,
            @NotNull Collection<ShadowSimpleAttribute<?>> identifiers,
            @Nullable ObjectDelta<ShadowType> objectDelta,
            @Nullable UcfResourceObject resourceObject,
            @NotNull UcfErrorState errorState) {
        this.localSequenceNumber = localSequenceNumber;
        this.primaryIdentifierValue = primaryIdentifierValue;
        this.resourceObjectDefinition = objectDefinition;
        identifiers.forEach(id -> id.freeze());
        stateCheck(errorState.isError() || !identifiers.isEmpty(), "No identifiers (and no error)");
        this.identifiers = Collections.unmodifiableCollection(identifiers);
        this.resourceObject = resourceObject;
        this.objectDelta = objectDelta;
        this.errorState = errorState;
        checkConsistence();
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }

    public @NotNull Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    public ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public @NotNull Collection<ShadowSimpleAttribute<?>> getIdentifiers() {
        return identifiers;
    }

    public @Nullable ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public @Nullable UcfResourceObject getResourceObject() {
        return resourceObject;
    }

    public @NotNull UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(seq=" + localSequenceNumber
                + ", class=" + getObjectClassLocalName()
                + ", uid=" + primaryIdentifierValue
                + ", identifiers=" + identifiers
                + ", objectDelta=" + objectDelta
                + ", resourceObject=" + resourceObject
                + ", errorState=" + errorState
                + toStringExtra() + ")";
    }

    private String getObjectClassLocalName() {
        return resourceObjectDefinition != null ? resourceObjectDefinition.getTypeName().getLocalPart() : null;
    }

    protected abstract String toStringExtra();

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "errorState", errorState, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "localSequenceNumber", localSequenceNumber, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClassDefinition", resourceObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "identifiers", identifiers, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectDelta", objectDelta, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "resourceObject", resourceObject, indent + 1);
        debugDumpExtra(sb, indent);
        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

    public boolean isError() {
        return errorState.isError();
    }

    public boolean isDelete() {
        return ObjectDelta.isDelete(objectDelta);
    }

    public void checkConsistence() {
        if (!InternalsConfig.consistencyChecks) {
            return;
        }
        checkObjectClassDefinitionPresence();
    }

    protected abstract void checkObjectClassDefinitionPresence();
}
