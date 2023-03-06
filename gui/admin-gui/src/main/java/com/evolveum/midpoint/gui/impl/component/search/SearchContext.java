/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionReportEngineConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;

public class SearchContext {

    private ResourceObjectDefinition resourceObjectDefinition;
    private ItemDefinition<?> definitionOverride;

    private CollectionPanelType collectionPanelType;

    private QName assignmentTargetType;
    private ObjectCollectionReportEngineConfigurationType reportCollection;

    private List<DisplayableValue<String>> availableEventMarks;

    private String selectedEventMark;

    private ObjectProcessingStateType objectProcessingState;

    public ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public void setResourceObjectDefinition(ResourceObjectDefinition resourceObjectDefinition) {
        this.resourceObjectDefinition = resourceObjectDefinition;
    }

    public CollectionPanelType getPanelType() {
        return collectionPanelType;
    }

    public void setPanelType(CollectionPanelType collectionPanelType) {
        this.collectionPanelType = collectionPanelType;
    }

    public QName getAssignmentTargetType() {
        return assignmentTargetType;
    }

    public void setAssignmentTargetType(QName assignmentTargetType) {
        this.assignmentTargetType = assignmentTargetType;
    }

    public ObjectCollectionReportEngineConfigurationType getReportCollection() {
        return reportCollection;
    }

    public void setReportCollection(ObjectCollectionReportEngineConfigurationType reportCollection) {
        this.reportCollection = reportCollection;
    }

    public ItemDefinition<?> getDefinitionOverride() {
        return definitionOverride;
    }

    public void setDefinitionOverride(ItemDefinition<?> definitionOverride) {
        this.definitionOverride = definitionOverride;
    }

    public List<DisplayableValue<String>> getAvailableEventMarks() {
        if (availableEventMarks == null) {
            availableEventMarks = new ArrayList<>();
        }
        return availableEventMarks;
    }

    public void setAvailableEventMarks(List<DisplayableValue<String>> availableEventMarks) {
        this.availableEventMarks = availableEventMarks;
    }

    public String getSelectedEventMark() {
        return selectedEventMark;
    }

    public void setSelectedEventMark(String selectedEventMark) {
        this.selectedEventMark = selectedEventMark;
    }

    public ObjectProcessingStateType getObjectProcessingState() {
        return objectProcessingState;
    }

    public void setObjectProcessingState(ObjectProcessingStateType objectProcessingState) {
        this.objectProcessingState = objectProcessingState;
    }
}
