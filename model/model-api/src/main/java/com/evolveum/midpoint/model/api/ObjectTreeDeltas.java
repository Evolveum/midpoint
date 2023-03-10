/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Structure that contains all primary changes requested: from focus as well as from projections.
 */
public class ObjectTreeDeltas<T extends ObjectType> implements DebugDumpable {

    private ObjectDelta<T> focusChange;
    private final Map<ProjectionContextKey, ObjectDelta<ShadowType>> projectionChangeMap = new HashMap<>();    // values are non null here
    private final PrismContext prismContext = PrismContext.get();

    public ObjectTreeDeltas() {
    }

    public ObjectTreeDeltas(ObjectDelta<T> focusChange) {
        this.focusChange = focusChange;
    }

    public ObjectDelta<T> getFocusChange() {
        return focusChange;
    }

    @SuppressWarnings("unused")
    public ObjectDelta<ShadowType> getProjectionChange(ProjectionContextKey key) {
        return projectionChangeMap.get(key);
    }

    @SuppressWarnings("unused")
    public Map<ProjectionContextKey, ObjectDelta<ShadowType>> getProjectionChangeMap() {
        return projectionChangeMap;
    }

    public void setFocusChange(ObjectDelta<T> focusChange) {
        this.focusChange = focusChange;
    }

    public void addProjectionChange(ProjectionContextKey key, ObjectDelta<ShadowType> primaryDelta) {
        if (projectionChangeMap.containsKey(key)) {
            throw new IllegalStateException("Duplicate contexts for " + key);
        }
        projectionChangeMap.put(key, primaryDelta.clone());
    }

    public boolean isEmpty() {
        if (focusChange != null && !focusChange.isEmpty()) {
            return false;
        }
        for (ObjectDelta<ShadowType> projectionDelta : projectionChangeMap.values()) {
            if (!projectionDelta.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(ObjectTreeDeltasType deltas) {
        if (deltas == null) {
            return true;
        }
        if (deltas.getFocusPrimaryDelta() != null) {
            if (!ObjectDeltaUtil.isEmpty(deltas.getFocusPrimaryDelta())) {
                return false;
            }
        }
        for (ProjectionObjectDeltaType projDelta: deltas.getProjectionPrimaryDelta()) {
            if (!ObjectDeltaUtil.isEmpty(projDelta.getPrimaryDelta())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ObjectTreeDeltas<T> clone() {
        ObjectTreeDeltas<T> clone = new ObjectTreeDeltas<>();
        if (focusChange != null) {
            clone.setFocusChange(focusChange.clone());
        }
        for (Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>> entry : projectionChangeMap.entrySet()) {
            clone.addProjectionChange(entry.getKey(), entry.getValue());        // TODO clone RSD?
        }
        return clone;
    }

    public Set<Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>>> getProjectionChangeMapEntries() {
        return projectionChangeMap.entrySet();
    }

    private String toObjectTreeDeltasTypeXml() throws SchemaException {
        ObjectTreeDeltasType jaxb = toObjectTreeDeltasType();
        return prismContext.xmlSerializer().serializeRealValue(jaxb, SchemaConstantsGenerated.C_OBJECT_TREE_DELTAS);
    }

    private ObjectTreeDeltasType toObjectTreeDeltasType() throws SchemaException {
        ObjectTreeDeltasType jaxb = new ObjectTreeDeltasType();
        if (getFocusChange() != null) {
            jaxb.setFocusPrimaryDelta(DeltaConvertor.toObjectDeltaType(getFocusChange()));
        }
        Set<Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>>> entries = getProjectionChangeMapEntries();
        for (Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>> entry : entries) {
            ProjectionObjectDeltaType projChange = new ProjectionObjectDeltaType();
            projChange.setResourceShadowDiscriminator(entry.getKey().toResourceShadowDiscriminatorType());
            projChange.setPrimaryDelta(DeltaConvertor.toObjectDeltaType(entry.getValue()));
            jaxb.getProjectionPrimaryDelta().add(projChange);
        }
        return jaxb;
    }

    @SuppressWarnings("unused")
    public static String toObjectTreeDeltasTypeXml(ObjectTreeDeltas<?> objectTreeDeltas) throws SchemaException {
        return objectTreeDeltas != null ? objectTreeDeltas.toObjectTreeDeltasTypeXml() : null;
    }

    @SuppressWarnings("unused")
    public static String toObjectTreeDeltasTypeXml(ObjectTreeDeltasType objectTreeDeltasType, PrismContext prismContext) throws SchemaException {
        if (objectTreeDeltasType != null) {
            return prismContext.xmlSerializer().serializeRealValue(objectTreeDeltasType, SchemaConstantsGenerated.C_OBJECT_TREE_DELTAS);
        } else {
            return null;
        }
    }

    public static ObjectTreeDeltasType toObjectTreeDeltasType(ObjectTreeDeltas<?> objectTreeDeltas) throws SchemaException {
        return objectTreeDeltas != null ? objectTreeDeltas.toObjectTreeDeltasType() : null;
    }

    @Contract("null -> null; !null -> !null")
    public static <T extends ObjectType> ObjectTreeDeltas<T> fromObjectTreeDeltasType(ObjectTreeDeltasType deltasType) throws SchemaException {
        if (deltasType == null) {
            return null;
        }
        ObjectTreeDeltas<T> deltas = new ObjectTreeDeltas<>();
        if (deltasType.getFocusPrimaryDelta() != null) {
            deltas.setFocusChange(DeltaConvertor.createObjectDelta(deltasType.getFocusPrimaryDelta()));
        }
        for (ProjectionObjectDeltaType projectionObjectDeltaType : deltasType.getProjectionPrimaryDelta()) {
            ProjectionContextKey rsd = ProjectionContextKey.fromBean(
                    projectionObjectDeltaType.getResourceShadowDiscriminator());
            ObjectDelta<ShadowType> objectDelta = DeltaConvertor.createObjectDelta(projectionObjectDeltaType.getPrimaryDelta());
            deltas.addProjectionChange(rsd, objectDelta);
        }
        return deltas;
    }

    public List<ObjectDelta<? extends ObjectType>> getDeltaList() {
        List<ObjectDelta<? extends ObjectType>> rv = new ArrayList<>();
        if (focusChange != null) {
            rv.add(focusChange);
        }
        rv.addAll(projectionChangeMap.values());
        return rv;
    }

    public boolean subtractFromFocusDelta(@NotNull ItemPath itemPath, @NotNull PrismValue value, boolean fromMinus,
            boolean dryRun) {
        return focusChange != null && focusChange.subtract(itemPath, value, fromMinus, dryRun);
    }

    @Override
    public String toString() {
        return "ObjectTreeDeltas{" +
                "focusChange=" + focusChange +
                ", projectionChangeMap=" + projectionChangeMap +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ObjectTreeDeltas:\n");
        DebugUtil.debugDumpWithLabel(sb, "Focus primary change", focusChange, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "Projections primary changes", indent+1);
        for (Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>> entry : projectionChangeMap.entrySet()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent+2);
            sb.append(entry.getKey());
            sb.append(" =>\n");
            sb.append(entry.getValue().debugDump(indent+3));
        }
        return sb.toString();
    }

    public void merge(ObjectTreeDeltas<T> deltasToMerge) throws SchemaException {
        if (deltasToMerge == null) {
            return;
        }
        if (focusChange != null) {
            focusChange.merge(deltasToMerge.focusChange);
        } else {
            focusChange = deltasToMerge.focusChange;
        }
        for (Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>> projEntry : deltasToMerge.getProjectionChangeMapEntries()) {
            ProjectionContextKey key = projEntry.getKey();
            ObjectDelta<ShadowType> existingDelta = projectionChangeMap.get(key);
            ObjectDelta<ShadowType> newDelta = projEntry.getValue();
            if (existingDelta != null) {
                existingDelta.merge(newDelta);
            } else {
                projectionChangeMap.put(key, newDelta);
            }
        }
    }

    public void mergeUnordered(ObjectTreeDeltas<T> deltasToMerge) throws SchemaException {
        if (deltasToMerge == null) {
            return;
        }
        focusChange = mergeInCorrectOrder(focusChange, deltasToMerge.focusChange);
        for (Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>> projEntry : deltasToMerge.getProjectionChangeMapEntries()) {
            ProjectionContextKey rsd = projEntry.getKey();
            ObjectDelta<ShadowType> existingDelta = projectionChangeMap.get(rsd);
            ObjectDelta<ShadowType> newDelta = projEntry.getValue();
            projectionChangeMap.put(rsd, mergeInCorrectOrder(existingDelta, newDelta));
        }
    }

    private <T1 extends ObjectType> ObjectDelta<T1> mergeInCorrectOrder(ObjectDelta<T1> first, ObjectDelta<T1> second) throws SchemaException {
        ObjectDelta<T1> rv;
        if (first == null) {
            rv = second;
        } else if (second == null) {
            rv = first;
        } else if (second.isAdd() || first.isDelete()) {
            rv = second.clone();
            rv.merge(first);
        } else {
            rv = first.clone();
            rv.merge(second);
        }
        return rv;
    }

    public static <T extends ObjectType> ObjectTreeDeltasType mergeDeltas(
            ObjectTreeDeltasType deltaTree, ObjectDeltaType deltaToMerge)
            throws SchemaException {
        if (deltaToMerge == null) {
            return deltaTree;
        }
        ObjectTreeDeltasType deltaTreeToMerge = new ObjectTreeDeltasType();
        deltaTreeToMerge.setFocusPrimaryDelta(deltaToMerge);
        if (deltaTree == null) {
            return deltaTreeToMerge;
        }
        ObjectTreeDeltas<T> tree = fromObjectTreeDeltasType(deltaTree);
        ObjectTreeDeltas<T> treeToMerge = fromObjectTreeDeltasType(deltaTreeToMerge);
        tree.merge(treeToMerge);
        return tree.toObjectTreeDeltasType();
    }
}
