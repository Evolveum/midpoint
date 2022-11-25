/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.util.LocalizationMessageComparator;

public class SceneItemDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_LINES  = "lines";

    @NotNull private final VisualizationItem sceneItem;
    @NotNull private final SceneDto sceneDto;
    @NotNull private List<SceneItemLineDto> lines;

    public SceneItemDto(@NotNull SceneDto sceneDto, @NotNull VisualizationItem sceneItem) {
        Validate.notNull(sceneDto);
        Validate.notNull(sceneItem);
        this.sceneDto = sceneDto;
        this.sceneItem = sceneItem;
        this.lines = computeLines();
    }

    public String getName() {
        Name n = sceneItem.getName();
        if (n == null) {
            return "";
        } else if (n.getDisplayName() != null) {
            return n.getDisplayName();
        } else {
            return n.getSimpleName();
        }
    }

    public String getNewValue() {
        return String.valueOf(sceneItem.getNewValues());
    }

    public List<SceneItemLineDto> computeLines() {
        List<SceneItemLineDto> rv = new ArrayList<>();

        if (!isDelta()) {
            for (VisualizationItemValue itemValue : sceneItem.getNewValues()) {
                rv.add(new SceneItemLineDto(this, null, itemValue, false));
            }
        } else {
            VisualizationDeltaItem deltaItem = (VisualizationDeltaItem) sceneItem;
            for (VisualizationItemValue itemValue : deltaItem.getUnchangedValues()) {
                rv.add(new SceneItemLineDto(this, null, itemValue, false));
            }
            List<? extends VisualizationItemValue> deletedValues = deltaItem.getDeletedValues();
            List<? extends VisualizationItemValue> addedValues = deltaItem.getAddedValues();
            Comparator<? super VisualizationItemValue> comparator =
                    (s1, s2) -> {
                        LocalizableMessage value1 = s1 == null ? null : s1.getText();
                        LocalizableMessage value2 = s2 == null ? null : s2.getText();

                        return LocalizationMessageComparator.COMPARE_MESSAGE_TRANSLATED.compare(value1, value2);
                    };
            deletedValues.sort(comparator);
            addedValues.sort(comparator);

            Iterator<? extends VisualizationItemValue> deletedValuesIter = deletedValues.iterator();
            Iterator<? extends VisualizationItemValue> addedValuesIter = addedValues.iterator();
            while (deletedValuesIter.hasNext() || addedValuesIter.hasNext()) {
                VisualizationItemValue deletedValue = deletedValuesIter.hasNext() ? deletedValuesIter.next() : null;
                VisualizationItemValue addedValue = addedValuesIter.hasNext() ? addedValuesIter.next() : null;
                rv.add(new SceneItemLineDto(this, deletedValue, addedValue, true));
            }
        }
        Comparator<? super SceneItemLineDto> comparator =
                (s1, s2) -> {
                    if (s1.isDelta()){
                        return 1;
                    } else if (s2.isDelta()){
                        return -1;
                    }

                    LocalizableMessage value1 = s1.getNewValue() == null ? null : s1.getNewValue().getText();
                    LocalizableMessage value2 = s2.getNewValue() == null ? null : s2.getNewValue().getText();

                    return LocalizationMessageComparator.COMPARE_MESSAGE_TRANSLATED.compare(value1, value2);
                };
        rv.sort(comparator);
        return rv;
    }

    @NotNull
    public List<SceneItemLineDto> getLines() {
        return lines;
    }

    public boolean isDelta() {
        return sceneItem instanceof VisualizationDeltaItem;
    }

    public boolean isNullEstimatedOldValues(){
        return isDelta() && ((VisualizationDeltaItem)sceneItem).getSourceDelta() != null && ((VisualizationDeltaItem)sceneItem).getSourceDelta().getEstimatedOldValues() == null;
    }

    public boolean isAdd(){
        return isDelta() && ((VisualizationDeltaItem)sceneItem).getSourceDelta() != null && ((VisualizationDeltaItem)sceneItem).getSourceDelta().isAdd();
    }

    public boolean isDelete(){
        return isDelta() && ((VisualizationDeltaItem)sceneItem).getSourceDelta() != null && ((VisualizationDeltaItem)sceneItem).getSourceDelta().isDelete();
    }

    public boolean isReplace(){
        return isDelta() && ((VisualizationDeltaItem)sceneItem).getSourceDelta() != null && ((VisualizationDeltaItem)sceneItem).getSourceDelta().isReplace();
    }

    public boolean isDeltaScene() {
        return sceneDto.containsDeltaItems();
    }

    public boolean isOperational(){
        return sceneItem.isOperational();
    }

    public boolean isDescriptive() {
        return sceneItem.isDescriptive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SceneItemDto that = (SceneItemDto) o;

        if (!sceneItem.equals(that.sceneItem)) return false;
        return lines.equals(that.lines);

    }

    @Override
    public int hashCode() {
        int result = sceneItem.hashCode();
        result = 31 * result + lines.hashCode();
        return result;
    }
}
