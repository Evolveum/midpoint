/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 */
public class ObjectDeltaOperation<O extends ObjectType> implements DebugDumpable, Serializable, Cloneable {

    private ObjectDelta<O> objectDelta;
    @Nullable private OperationResult executionResult;
    private PolyString objectName;
    private String resourceOid;
    private PolyString resourceName;

    private ShadowKindType shadowKind;
    private String shadowIntent;

    public ObjectDeltaOperation() {
        super();
    }

    public ObjectDeltaOperation(ObjectDelta<O> objectDelta) {
        super();
        this.objectDelta = objectDelta;
    }

    public ObjectDeltaOperation(ObjectDelta<O> objectDelta, @Nullable OperationResult executionResult) {
        super();
        this.objectDelta = objectDelta;
        this.executionResult = executionResult;
    }

    public ObjectDelta<O> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<O> objectDelta) {
        this.objectDelta = objectDelta;
    }

    public @Nullable OperationResult getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(@Nullable OperationResult executionResult) {
        this.executionResult = executionResult;
    }

    public PolyString getObjectName() {
        return objectName;
    }

    public void setObjectName(PolyString objectName) {
        this.objectName = objectName;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public PolyString getResourceName() {
        return resourceName;
    }

    public void setResourceName(PolyString resourceName) {
        this.resourceName = resourceName;
    }

    public boolean containsDelta(ObjectDelta<O> delta, ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return objectDelta.equals(delta) ||
                objectDelta.isModify()
                        && delta.isModify()
                        && objectDelta.containsAllModifications(delta.getModifications(), equivalenceStrategy);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ObjectDeltaOperation<O> clone() {
        ObjectDeltaOperation<O> clone = new ObjectDeltaOperation<>();
        copyToClone(clone);
        return clone;
    }

    protected void copyToClone(ObjectDeltaOperation<O> clone) {
        if (this.objectDelta != null) {
            clone.objectDelta = this.objectDelta.clone();
        }
        clone.executionResult = this.executionResult;
        clone.objectName = this.objectName;
        clone.resourceOid = this.resourceOid;
        clone.resourceName = this.resourceName;
        clone.shadowIntent = this.shadowIntent;
        clone.shadowKind = this.shadowKind;
    }

    public static void checkConsistence(Collection<? extends ObjectDeltaOperation<?>> deltas) {
        for (ObjectDeltaOperation<?> delta : deltas) {
            delta.checkConsistence();
        }
    }

    public void checkConsistence() {
        if (objectDelta != null) {
            objectDelta.checkConsistence();
        }
    }

    public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneCollection(
            Collection<ObjectDeltaOperation<? extends ObjectType>> origCollection) {
        Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<>(origCollection.size());
        for (ObjectDeltaOperation<? extends ObjectType> origDeltaOp : origCollection) {
            ObjectDeltaOperation<? extends ObjectType> clonedDeltaOp = origDeltaOp.clone();
            clonedCollection.add(clonedDeltaOp);
        }
        return clonedCollection;
    }

    public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneDeltaCollection(
            Collection<ObjectDelta<? extends ObjectType>> origCollection) {
        Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<>(origCollection.size());
        for (ObjectDelta<? extends ObjectType> origDelta : origCollection) {
            ObjectDeltaOperation<? extends ObjectType> clonedDeltaOp = new ObjectDeltaOperation<>(origDelta.clone());
            clonedCollection.add(clonedDeltaOp);
        }
        return clonedCollection;
    }

    public static ObjectDeltaOperation<? extends ObjectType> findFocusDeltaInCollection(
            Collection<ObjectDeltaOperation<? extends ObjectType>> odos) {
        for (ObjectDeltaOperation<? extends ObjectType> odo : odos) {
            Class<? extends ObjectType> objectTypeClass = odo.getObjectDelta().getObjectTypeClass();
            if (!ShadowType.class.equals(objectTypeClass)) {
                return odo;
            }
        }
        return null;
    }

    public static String findFocusDeltaOidInCollection(Collection<ObjectDeltaOperation<? extends ObjectType>> odos) {
        ObjectDeltaOperation<? extends ObjectType> odo = findFocusDeltaInCollection(odos);
        if (odo == null) {
            return null;
        }
        return odo.getObjectDelta().getOid();
    }

    public static List<ObjectDeltaOperation<ShadowType>> findProjectionDeltasInCollection(
            Collection<ObjectDeltaOperation<? extends ObjectType>> odos) {
        List<ObjectDeltaOperation<ShadowType>> projectionDeltas = new ArrayList<>();
        for (ObjectDeltaOperation<? extends ObjectType> odo : odos) {
            Class<? extends ObjectType> objectTypeClass = odo.getObjectDelta().getObjectTypeClass();
            if (ShadowType.class.equals(objectTypeClass)) {
                //noinspection unchecked
                projectionDeltas.add((ObjectDeltaOperation<ShadowType>) odo);
            }
        }
        return projectionDeltas;
    }

    public static List<String> findProjectionDeltaOidsInCollection(
            Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges) {
        return findProjectionDeltasInCollection(executeChanges).stream()
                .map(odo -> odo.getObjectDelta().getOid())
                .distinct()
                .collect(Collectors.toList());
    }

    public static <O extends ObjectType> ObjectDeltaOperation<O> findAddDelta(
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges, PrismObject<O> object) {
        for (ObjectDeltaOperation<? extends ObjectType> odo : executedChanges) {
            Class<? extends ObjectType> objectTypeClass = odo.getObjectDelta().getObjectTypeClass();
            //noinspection ConstantConditions
            if (odo.getObjectDelta().isAdd() && object.getCompileTimeClass().equals(objectTypeClass)) {
                //noinspection unchecked
                return (ObjectDeltaOperation<O>) odo;
            }
        }
        return null;
    }

    public static @NotNull String findAddDeltaOidRequired(
            @NotNull Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges,
            @NotNull Class<? extends ObjectType> type) {
        var oids = findAddDeltas(executedChanges, type).stream()
                .map(odo -> odo.getOid())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return MiscUtil.extractSingletonRequired(
                oids,
                () -> new IllegalStateException(
                        "Multiple OIDs in ADD deltas of type %s: %s in %s".formatted(
                                type, oids, executedChanges)),
                () -> new IllegalStateException("No OID in ADD deltas of type %s in %s".formatted(
                        type, executedChanges)));
    }

    /** Returns ADD deltas corresponding to given `objectType` */
    public static @NotNull <O extends ObjectType> List<ObjectDeltaOperation<? extends O>> findAddDeltas(
            @NotNull Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges, @NotNull Class<O> objectType) {
        //noinspection unchecked
        return executedChanges.stream()
                .filter(odo -> odo.isAdd() && odo.isOfType(objectType))
                .map(odo -> (ObjectDeltaOperation<? extends O>) odo)
                .collect(Collectors.toList());
    }

    private boolean isAdd() {
        return ObjectDelta.isAdd(objectDelta);
    }

    private boolean isOfType(@NotNull Class<? extends ObjectType> objectType) {
        return objectDelta != null && objectDelta.isOfType(objectType);
    }

    public static <O extends ObjectType> String findAddDeltaOid(
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges, PrismObject<O> object) {
        ObjectDeltaOperation<O> odo = findAddDelta(executedChanges, object);
        if (odo == null) {
            return null;
        }
        return odo.getObjectDelta().getOid();
    }

    // Mostly for use in tests.
    public static String findProjectionDeltaOidInCollection(Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges) {
        List<String> oids = findProjectionDeltaOidsInCollection(executeChanges);
        if (oids.isEmpty()) {
            return null;
        }
        if (oids.size() > 1) {
            throw new IllegalStateException("More than one projection oid");
        }
        return oids.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectDeltaOperation<?> that = (ObjectDeltaOperation<?>) o;
        return Objects.equals(objectDelta, that.objectDelta)
                && Objects.equals(executionResult, that.executionResult)
                && Objects.equals(objectName, that.objectName)
                && Objects.equals(resourceOid, that.resourceOid)
                && Objects.equals(resourceName, that.resourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectDelta, executionResult, objectName, resourceOid, resourceName);
    }

    @Override
    public String toString() {
        return getDebugDumpClassName() + "(" + objectDelta
                + ": " + executionResult + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDump(sb, indent, true);
        return sb.toString();
    }

    public String shorterDebugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDump(sb, indent, false);
        return sb.toString();
    }

    private void debugDump(StringBuilder sb, int indent, boolean detailedResultDump) {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpClassName()).append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Delta", objectDelta, indent + 1);
        sb.append("\n");
        if (detailedResultDump) {
            DebugUtil.debugDumpWithLabel(sb, "Execution result", executionResult, indent + 1);
        } else {
            DebugUtil.debugDumpLabel(sb, "Execution result", indent + 1);
            if (executionResult == null) {
                sb.append("null");
            } else {
                executionResult.shortDump(sb);
            }
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Object name", objectName, indent + 1);
        if (resourceName != null || resourceOid != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Resource", resourceName + " (" + resourceOid + ")", indent + 1);
        }
    }

    protected String getDebugDumpClassName() {
        return "ObjectDeltaOperation";
    }

    public static <O extends ObjectType> String shorterDebugDump(List<? extends ObjectDeltaOperation<O>> deltaOps, int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("[\n");
        for (ObjectDeltaOperation<O> deltaOp : deltaOps) {
            deltaOp.debugDump(sb, indent + 1, false);
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public OperationResultStatus getStatus() {
        return executionResult != null ? executionResult.getStatus() : null;
    }

    /**
     * Approximate information if the delta was "really" executed i.e. if there's a real chance
     * that the delta was - at least partially - applied.
     *
     * Any solution based on operation result status will never be 100% accurate, e.g. because
     * a network timeout could occur just before returning a status value. So please use with care.
     */
    @Experimental
    public boolean wasReallyExecuted() {
        OperationResultStatus status = getStatus();
        return status != null && status != OperationResultStatus.FATAL_ERROR && status != OperationResultStatus.NOT_APPLICABLE;
    }

    public String getOid() {
        return objectDelta != null ? objectDelta.getOid() : null;
    }

    public void setShadowKind(ShadowKindType kind) {
        this.shadowKind = kind;
    }

    public void setShadowIntent(String intent) {
        this.shadowIntent = intent;
    }

    public ShadowKindType getShadowKind() {
        return this.shadowKind;
    }

    public String getShadowIntent() {
        return shadowIntent;
    }
}
