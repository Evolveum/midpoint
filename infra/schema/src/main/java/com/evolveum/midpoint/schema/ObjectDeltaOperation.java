/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
public class ObjectDeltaOperation<O extends ObjectType> implements DebugDumpable {

    private ObjectDelta<O> objectDelta;
    private OperationResult executionResult;
    private PolyString objectName;
    private String resourceOid;
    private PolyString resourceName;

    public ObjectDeltaOperation() {
        super();
    }

    public ObjectDeltaOperation(ObjectDelta<O> objectDelta) {
        super();
        this.objectDelta = objectDelta;
    }

    public ObjectDeltaOperation(ObjectDelta<O> objectDelta, OperationResult executionResult) {
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

    public OperationResult getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(OperationResult executionResult) {
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

    public boolean containsDelta(ObjectDelta<O> delta) {
        return objectDelta.equals(delta) ||
                objectDelta.isModify() && delta.isModify() && objectDelta.containsAllModifications(delta.getModifications(), EquivalenceStrategy.IGNORE_METADATA);
    }

//    public static <T extends ObjectType> boolean containsDelta(Collection<? extends ObjectDeltaOperation<T>> deltaOps, ObjectDelta<T> delta) {
//        if (deltaOps == null) {
//            return false;
//        }
//        for (ObjectDeltaOperation<T> deltaOp: deltaOps) {
//            if (deltaOp.containsDelta(delta)) {
//                return true;
//            }
//        }
//        return false;
//    }

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
    }

    public static void checkConsistence(Collection<? extends ObjectDeltaOperation<?>> deltas) {
        for (ObjectDeltaOperation<?> delta: deltas) {
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
        for (ObjectDeltaOperation<? extends ObjectType> origDeltaOp: origCollection) {
            ObjectDeltaOperation<? extends ObjectType> clonedDeltaOp = origDeltaOp.clone();
            clonedCollection.add(clonedDeltaOp);
        }
        return clonedCollection;
    }

    public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneDeltaCollection(
            Collection<ObjectDelta<? extends ObjectType>> origCollection) {
        Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<>(origCollection.size());
        for (ObjectDelta<? extends ObjectType> origDelta: origCollection) {
            ObjectDeltaOperation<? extends ObjectType> clonedDeltaOp = new ObjectDeltaOperation(origDelta.clone());
            clonedCollection.add(clonedDeltaOp);
        }
        return clonedCollection;
    }

    public static ObjectDeltaOperation<? extends ObjectType> findFocusDeltaInCollection(Collection<ObjectDeltaOperation<? extends ObjectType>> odos) {
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

    public static List<ObjectDeltaOperation<ShadowType>> findProjectionDeltasInCollection(Collection<ObjectDeltaOperation<? extends ObjectType>> odos) {
        List<ObjectDeltaOperation<ShadowType>> projectionDeltas = new ArrayList<>();
        for (ObjectDeltaOperation<? extends ObjectType> odo : odos) {
            Class<? extends ObjectType> objectTypeClass = odo.getObjectDelta().getObjectTypeClass();
            if (ShadowType.class.equals(objectTypeClass)) {
                projectionDeltas.add((ObjectDeltaOperation<ShadowType>) odo);
            }
        }
        return projectionDeltas;
    }

    public static List<String> findProjectionDeltaOidsInCollection(Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges) {
        return findProjectionDeltasInCollection(executeChanges).stream()
                .map(odo -> odo.getObjectDelta().getOid())
                .distinct()
                .collect(Collectors.toList());
    }

    public static <O extends ObjectType> ObjectDeltaOperation<O> findAddDelta(Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges, PrismObject<O> object) {
        for (ObjectDeltaOperation<? extends ObjectType> odo : executedChanges) {
            Class<? extends ObjectType> objectTypeClass = odo.getObjectDelta().getObjectTypeClass();
            if (odo.getObjectDelta().isAdd() && object.getCompileTimeClass().equals(objectTypeClass)) {
                return (ObjectDeltaOperation<O>) odo;
            }
        }
        return null;
    }

    public static <O extends ObjectType> String findAddDeltaOid(Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges, PrismObject<O> object) {
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((executionResult == null) ? 0 : executionResult.hashCode());
        result = prime * result + ((objectDelta == null) ? 0 : objectDelta.hashCode());
        result = prime * result + ((objectName == null) ? 0 : objectName.hashCode());
        result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
        result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ObjectDeltaOperation other = (ObjectDeltaOperation) obj;
        if (executionResult == null) {
            if (other.executionResult != null)
                return false;
        } else if (!executionResult.equals(other.executionResult))
            return false;
        if (objectDelta == null) {
            if (other.objectDelta != null)
                return false;
        } else if (!objectDelta.equals(other.objectDelta))
            return false;
        // TODO are the following fields relevant for equality test?
        if (objectName == null) {
            if (other.objectName != null)
                return false;
        } else if (!objectName.equals(other.objectName))
            return false;
        if (resourceOid == null) {
            if (other.resourceOid != null)
                return false;
        } else if (!resourceOid.equals(other.resourceOid))
            return false;
        if (resourceName == null) {
            if (other.resourceName != null)
                return false;
        } else if (!resourceName.equals(other.resourceName))
            return false;
        return true;
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
        for (ObjectDeltaOperation<O> deltaOp: deltaOps) {
            deltaOp.debugDump(sb, indent + 1, false);
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

}
