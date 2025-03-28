/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.search.panel.NamedIntervalPreset;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionReportEngineConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

public class SearchContext {

    private ResourceObjectDefinition resourceObjectDefinition;
    private ItemDefinition<?> definitionOverride;

    private CollectionPanelType collectionPanelType;

    private QName assignmentTargetType;
    private ObjectCollectionReportEngineConfigurationType reportCollection;

    private List<DisplayableValue<String>> availableEventMarks;

    private String selectedEventMark;

    private ObjectProcessingStateType objectProcessingState;

    private List<SearchBoxModeType> availableSearchBoxModes;

    private boolean history;

    private Map<ItemPath, List<NamedIntervalPreset>> intervalPresets = new PathKeyedMap<>();

    private Map<ItemPath, NamedIntervalPreset> selectedIntervalPresets = new PathKeyedMap<>();

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

    public List<SearchBoxModeType> getAvailableSearchBoxModes() {
        return availableSearchBoxModes;
    }

    public void setAvailableSearchBoxModes(List<SearchBoxModeType> availableSearchBoxModes) {
        this.availableSearchBoxModes = availableSearchBoxModes;
    }

    public void setHistory(boolean objectHistoryPanel) {
        this.history = objectHistoryPanel;
    }

    public boolean isHistory() {
        return history;
    }

    public boolean isReportCollectionSearch() {
        return reportCollection != null;
    }

    public void setIntervalPresets(@NotNull ItemPath path, @NotNull List<NamedIntervalPreset> presets) {
        intervalPresets.put(path, presets);
    }

    public void setSelectedIntervalPreset(@NotNull ItemPath path, NamedIntervalPreset preset) {
        if (preset == null) {
            selectedIntervalPresets.remove(path);
            return;
        }

        selectedIntervalPresets.put(path, preset);
    }

    public List<NamedIntervalPreset> getIntervalPresets(ItemPath path) {
        return intervalPresets.get(path);
    }

    public NamedIntervalPreset getSelectedIntervalPresets(ItemPath path) {
        return selectedIntervalPresets.get(path);
    }
}
