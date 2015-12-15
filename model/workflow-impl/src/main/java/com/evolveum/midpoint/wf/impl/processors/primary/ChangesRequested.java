/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.HashMap;
import java.util.Map;

/**
 * Structure that contains all primary changes requested: from focus as well as from projections.
 *
 * @author mederly
 */
public class ChangesRequested<F extends FocusType> {

    private ObjectDelta<F> focusChange;
    private Map<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> projectionChangeMap = new HashMap<>();    // values are non null here

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

    public static <T extends FocusType> ChangesRequested<T> extractFromModelContext(ModelContext<T> modelContext) {
        ChangesRequested changesRequested = new ChangesRequested();
        if (modelContext.getFocusContext() != null && modelContext.getFocusContext().getPrimaryDelta() != null) {
            changesRequested.setFocusChange(modelContext.getFocusContext().getPrimaryDelta().clone());
        }

        for (ModelProjectionContext projectionContext : modelContext.getProjectionContexts()) {
            if (projectionContext.getPrimaryDelta() != null) {
                changesRequested.addProjectionChange(projectionContext.getResourceShadowDiscriminator(), projectionContext.getPrimaryDelta());
            }
        }
        return changesRequested;
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

    public ChangesRequested<F> clone() {
        ChangesRequested<F> clone = new ChangesRequested<>();
        if (focusChange != null) {
            clone.setFocusChange(focusChange.clone());
        }
        for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : projectionChangeMap.entrySet()) {
            clone.addProjectionChange(entry.getKey(), entry.getValue());        // TODO clone RSD?
        }
        return clone;
    }

    @Override
    public String toString() {
        return "ChangesRequested{" +
                "focusChange=" + focusChange +
                ", projectionChangeMap=" + projectionChangeMap +
                '}';
    }

    public Iterable<? extends Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> getProjectionChangeMapEntries() {
        return projectionChangeMap.entrySet();
    }
}
