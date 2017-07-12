/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTreeDeltasType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionObjectDeltaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Structure that contains all primary changes requested: from focus as well as from projections.
 *
 * @author mederly
 */
public class ObjectTreeDeltas<F extends FocusType> implements DebugDumpable {

    private ObjectDelta<F> focusChange;
    private Map<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> projectionChangeMap = new HashMap<>();    // values are non null here
    private PrismContext prismContext;

    public ObjectTreeDeltas(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public ObjectTreeDeltas(ObjectDelta<F> focusChange, PrismContext prismContext) {
        this.focusChange = focusChange;
        this.prismContext = prismContext;
    }

    public ObjectDelta<F> getFocusChange() {
        return focusChange;
    }

    public ObjectDelta<ShadowType> getProjectionChange(ResourceShadowDiscriminator discriminator) {
        return projectionChangeMap.get(discriminator);
    }

    public Map<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> getProjectionChangeMap() {
        return projectionChangeMap;
    }

    public void setFocusChange(ObjectDelta<F> focusChange) {
        this.focusChange = focusChange;
    }

    public void addProjectionChange(ResourceShadowDiscriminator resourceShadowDiscriminator, ObjectDelta<ShadowType> primaryDelta) {
        if (projectionChangeMap.containsKey(resourceShadowDiscriminator)) {
            throw new IllegalStateException("Duplicate contexts for " + resourceShadowDiscriminator);
        }
        projectionChangeMap.put(resourceShadowDiscriminator, primaryDelta.clone());
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
			if (!ObjectDelta.isEmpty(deltas.getFocusPrimaryDelta())) {
				return false;
			}
		}
		for (ProjectionObjectDeltaType projDelta: deltas.getProjectionPrimaryDelta()) {
			if (!ObjectDelta.isEmpty(projDelta.getPrimaryDelta())) {
				return false;
			}
		}
		return true;
	}

	public ObjectTreeDeltas<F> clone() {
        ObjectTreeDeltas<F> clone = new ObjectTreeDeltas<>(prismContext);
        if (focusChange != null) {
            clone.setFocusChange(focusChange.clone());
        }
        for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : projectionChangeMap.entrySet()) {
            clone.addProjectionChange(entry.getKey(), entry.getValue());        // TODO clone RSD?
        }
        return clone;
    }

    public Set<? extends Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> getProjectionChangeMapEntries() {
        return projectionChangeMap.entrySet();
    }

    public String toObjectTreeDeltasTypeXml() throws SchemaException {
        ObjectTreeDeltasType jaxb = toObjectTreeDeltasType();
        return prismContext.xmlSerializer().serializeRealValue(jaxb, SchemaConstantsGenerated.C_OBJECT_TREE_DELTAS);
    }

    public ObjectTreeDeltasType toObjectTreeDeltasType() throws SchemaException {
        ObjectTreeDeltasType jaxb = new ObjectTreeDeltasType();
        if (getFocusChange() != null) {
            jaxb.setFocusPrimaryDelta(DeltaConvertor.toObjectDeltaType(getFocusChange()));
        }
        Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> entries =
                (Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>>) getProjectionChangeMapEntries();
        for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : entries) {
            ProjectionObjectDeltaType projChange = new ProjectionObjectDeltaType();
            projChange.setResourceShadowDiscriminator(entry.getKey().toResourceShadowDiscriminatorType());
            projChange.setPrimaryDelta(DeltaConvertor.toObjectDeltaType(entry.getValue()));
            jaxb.getProjectionPrimaryDelta().add(projChange);
        }
        return jaxb;
    }

    public static String toObjectTreeDeltasTypeXml(ObjectTreeDeltas objectTreeDeltas) throws SchemaException {
        return objectTreeDeltas != null ? objectTreeDeltas.toObjectTreeDeltasTypeXml() : null;
    }

    public static String toObjectTreeDeltasTypeXml(ObjectTreeDeltasType objectTreeDeltasType, PrismContext prismContext) throws SchemaException {
        if (objectTreeDeltasType != null) {
            return prismContext.xmlSerializer().serializeRealValue(objectTreeDeltasType, SchemaConstantsGenerated.C_OBJECT_TREE_DELTAS);
        } else {
            return null;
        }
    }

    public static ObjectTreeDeltasType toObjectTreeDeltasType(ObjectTreeDeltas objectTreeDeltas) throws SchemaException {
        return objectTreeDeltas != null ? objectTreeDeltas.toObjectTreeDeltasType() : null;
    }

    public static ObjectTreeDeltas fromObjectTreeDeltasType(ObjectTreeDeltasType deltasType, PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext, "prismContext");
        if (deltasType == null) {
            return null;
        }
        ObjectTreeDeltas deltas = new ObjectTreeDeltas(prismContext);
        if (deltasType.getFocusPrimaryDelta() != null) {
            deltas.setFocusChange(DeltaConvertor.createObjectDelta(deltasType.getFocusPrimaryDelta(), prismContext));
        }
        for (ProjectionObjectDeltaType projectionObjectDeltaType : deltasType.getProjectionPrimaryDelta()) {
            ResourceShadowDiscriminator rsd = ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(
                    projectionObjectDeltaType.getResourceShadowDiscriminator());
            ObjectDelta objectDelta = DeltaConvertor.createObjectDelta(projectionObjectDeltaType.getPrimaryDelta(), prismContext);
            deltas.addProjectionChange(rsd, objectDelta);
        }
        return deltas;
    }

    public List<ObjectDelta<?>> getDeltaList() {
        List<ObjectDelta<?>> rv = new ArrayList<>();
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
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ObjectTreeDeltas:\n");
        DebugUtil.debugDumpWithLabel(sb, "Focus primary change", focusChange, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "Projections primary changes", indent+1);
        for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : projectionChangeMap.entrySet()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent+2);
            sb.append(entry.getKey());
            sb.append(" =>\n");
            sb.append(entry.getValue().debugDump(indent+3));
        }
        return sb.toString();
    }

	public void merge(ObjectTreeDeltas<F> deltasToMerge) throws SchemaException {
		if (deltasToMerge == null) {
			return;
		}
		if (focusChange != null) {
			focusChange.merge(deltasToMerge.focusChange);
		} else {
			focusChange = deltasToMerge.focusChange;
		}
		for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> projEntry : deltasToMerge.getProjectionChangeMapEntries()) {
			ResourceShadowDiscriminator rsd = projEntry.getKey();
			ObjectDelta<ShadowType> existingDelta = projectionChangeMap.get(rsd);
			ObjectDelta<ShadowType> newDelta = projEntry.getValue();
			if (existingDelta != null) {
				existingDelta.merge(newDelta);
			} else {
				projectionChangeMap.put(rsd, newDelta);
			}
		}
	}

	public static ObjectTreeDeltasType mergeDeltas(ObjectTreeDeltasType deltaTree, ObjectDeltaType deltaToMerge,
			PrismContext prismContext) throws SchemaException {
    	if (deltaToMerge == null) {
    		return deltaTree;
		}
		ObjectTreeDeltasType deltaTreeToMerge = new ObjectTreeDeltasType();
		deltaTreeToMerge.setFocusPrimaryDelta(deltaToMerge);
		if (deltaTree == null) {
			return deltaTreeToMerge;
		}
		ObjectTreeDeltas tree = fromObjectTreeDeltasType(deltaTree, prismContext);
		ObjectTreeDeltas treeToMerge = fromObjectTreeDeltasType(deltaTreeToMerge, prismContext);
		tree.merge(treeToMerge);
		return tree.toObjectTreeDeltasType();
	}
}
