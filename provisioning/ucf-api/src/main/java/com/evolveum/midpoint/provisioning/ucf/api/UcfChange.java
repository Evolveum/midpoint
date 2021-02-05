/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

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
     * Can be null in the case of errors.
     */
    private final Object primaryIdentifierRealValue;

    /**
     * Definition of the object class. Can be missing for delete deltas.
     */
    private final ObjectClassComplexTypeDefinition objectClassDefinition;

    /**
     * All identifiers of the object.
     *
     * The collection is unmodifiable. The elements should not be modified as well, although this is not enforced yet.
     */
    @NotNull private final Collection<ResourceAttribute<?>> identifiers;

    /**
     * Delta from the resource - if known.
     *
     * For live sync it is filled-in for ADD and DELETE events.
     * For asynchronous updates it should be always present (except for notification-only updates).
     */
    private final ObjectDelta<ShadowType> objectDelta;

    /**
     * Resource object after the change - if known.
     *
     * It could come e.g. from the ConnId sync delta. Since 4.3, it is present also for LiveSync ADD deltas.
     * It is null if not available or not existing.
     */
    private final PrismObject<ShadowType> resourceObject;

    /**
     * Was there any error while processing the change in UCF layer?
     */
    @NotNull private final UcfErrorState errorState;

    UcfChange(int localSequenceNumber, Object primaryIdentifierRealValue,
            ObjectClassComplexTypeDefinition objectClassDefinition,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            ObjectDelta<ShadowType> objectDelta,
            PrismObject<ShadowType> resourceObject, @NotNull UcfErrorState errorState) {
        this.localSequenceNumber = localSequenceNumber;
        this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.objectClassDefinition = objectClassDefinition;
        this.identifiers = unmodifiableCollection(identifiers);
        this.resourceObject = resourceObject;
        this.objectDelta = objectDelta;
        this.errorState = errorState;
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }

    public Object getPrimaryIdentifierRealValue() {
        return primaryIdentifierRealValue;
    }

    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return objectClassDefinition;
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

    public @NotNull UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(seq=" + localSequenceNumber
                + ", class=" + getObjectClassLocalName()
                + ", uid=" + primaryIdentifierRealValue
                + ", identifiers=" + identifiers
                + ", objectDelta=" + objectDelta
                + ", resourceObject=" + resourceObject
                + ", errorState=" + errorState
                + toStringExtra() + ")";
    }

    private String getObjectClassLocalName() {
        return objectClassDefinition != null ? objectClassDefinition.getTypeName().getLocalPart() : null;
    }

    protected abstract String toStringExtra();

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "localSequenceNumber", localSequenceNumber, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierRealValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClassDefinition", objectClassDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "identifiers", identifiers, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectDelta", objectDelta, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "errorState", errorState, indent + 1);
        debugDumpExtra(sb, indent);
        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

    public boolean isError() {
        return errorState.isError();
    }
}
