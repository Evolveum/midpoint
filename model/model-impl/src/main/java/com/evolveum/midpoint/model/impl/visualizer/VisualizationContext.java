/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VisualizationContext {

    private boolean separateSinglevaluedContainers = true;
    private boolean separateMultivaluedContainers = true;
    private boolean separateSinglevaluedContainersInDeltas = true;
    private boolean separateMultivaluedContainersInDeltas = true;
    private boolean removeExtraDescriptiveItems = true;
    private boolean includeOperationalItems = false;
    private boolean includeMetadata = false;
    private Map<String,PrismObject<? extends ObjectType>> oldObjects;
    private Map<String,PrismObject<? extends ObjectType>> currentObjects;
    private Collection<ItemPath> pathsToHide = new ArrayList<>();
    private Collection<ItemPath> pathsToShow = new ArrayList<>();

    public boolean isSeparateSinglevaluedContainers() {
        return separateSinglevaluedContainers;
    }

    public void setSeparateSinglevaluedContainers(boolean separateSinglevaluedContainers) {
        this.separateSinglevaluedContainers = separateSinglevaluedContainers;
    }

    public boolean isSeparateMultivaluedContainers() {
        return separateMultivaluedContainers;
    }

    public void setSeparateMultivaluedContainers(boolean separateMultivaluedContainers) {
        this.separateMultivaluedContainers = separateMultivaluedContainers;
    }

    public boolean isSeparateSinglevaluedContainersInDeltas() {
        return separateSinglevaluedContainersInDeltas;
    }

    public void setSeparateSinglevaluedContainersInDeltas(boolean separateSinglevaluedContainersInDeltas) {
        this.separateSinglevaluedContainersInDeltas = separateSinglevaluedContainersInDeltas;
    }

    public boolean isSeparateMultivaluedContainersInDeltas() {
        return separateMultivaluedContainersInDeltas;
    }

    public void setSeparateMultivaluedContainersInDeltas(boolean separateMultivaluedContainersInDeltas) {
        this.separateMultivaluedContainersInDeltas = separateMultivaluedContainersInDeltas;
    }

    public boolean isRemoveExtraDescriptiveItems() {
        return removeExtraDescriptiveItems;
    }

    public void setRemoveExtraDescriptiveItems(boolean removeExtraDescriptiveItems) {
        this.removeExtraDescriptiveItems = removeExtraDescriptiveItems;
    }

    public boolean isIncludeOperationalItems() {
        return includeOperationalItems;
    }

    public void setIncludeOperationalItems(boolean includeOperationalItems) {
        this.includeOperationalItems = includeOperationalItems;
    }

    public boolean isIncludeMetadata() {
        return this.includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    public Map<String, PrismObject<? extends ObjectType>> getOldObjects() {
        if (oldObjects == null) {
            oldObjects = new HashMap<>();
        }
        return oldObjects;
    }

    public void setOldObjects(Map<String, PrismObject<? extends ObjectType>> oldObjects) {
        this.oldObjects = oldObjects;
    }

    public PrismObject<? extends ObjectType> getOldObject(String oid) {
        return getOldObjects().get(oid);
    }

    public Map<String, PrismObject<? extends ObjectType>> getCurrentObjects() {
        if (currentObjects == null) {
            currentObjects = new HashMap<>();
        }
        return currentObjects;
    }

    public void setCurrentObjects(
            Map<String, PrismObject<? extends ObjectType>> currentObjects) {
        this.currentObjects = currentObjects;
    }

    public PrismObject<? extends ObjectType> getCurrentObject(String oid) {
        return getCurrentObjects().get(oid);
    }

    public void putObject(PrismObject<? extends ObjectType> object) {
        getCurrentObjects().put(object.getOid(), object);
    }

    public void setPathsToHide(Collection<ItemPath> pathsToHide) {
        this.pathsToHide = pathsToHide;
    }

    public void setPathsToShow(Collection<ItemPath> pathsToShow) {
        this.pathsToShow = pathsToShow;
    }

    public boolean isPathToBeShown(ItemPath path) {
        return CollectionUtils.isEmpty(pathsToShow) || ItemPathCollectionsUtil.containsSubpathOrEquivalent(this.pathsToShow, path);
    }

    public boolean isHidden(ItemPath path) {
        return ItemPathCollectionsUtil.containsSubpathOrEquivalent(this.pathsToHide, path);
    }
}
