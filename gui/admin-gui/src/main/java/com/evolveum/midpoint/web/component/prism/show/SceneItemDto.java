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
import com.evolveum.midpoint.model.api.visualizer.SceneDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.util.PolyStringComparator;

public class SceneItemDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_LINES  = "lines";

    @NotNull private final SceneItem sceneItem;
    @NotNull private final SceneDto sceneDto;
    @NotNull private List<SceneItemLineDto> lines;

    public SceneItemDto(@NotNull SceneDto sceneDto, @NotNull SceneItem sceneItem) {
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
            for (SceneItemValue itemValue : sceneItem.getNewValues()) {
                rv.add(new SceneItemLineDto(this, null, itemValue, false));
            }
        } else {
            SceneDeltaItem deltaItem = (SceneDeltaItem) sceneItem;
            for (SceneItemValue itemValue : deltaItem.getUnchangedValues()) {
                rv.add(new SceneItemLineDto(this, null, itemValue, false));
            }
            List<? extends SceneItemValue> deletedValues = deltaItem.getDeletedValues();
            List<? extends SceneItemValue> addedValues = deltaItem.getAddedValues();
            Comparator<? super SceneItemValue> comparator =
                    (s1, s2) -> {
                        PolyString value1 = s1 == null ? null : s1.getText();
                        PolyString value2 = s2 == null ? null : s2.getText();

                        return PolyStringComparator.COMPARE_TRANSLATED.compare(value1, value2);
                    };
            deletedValues.sort(comparator);
            addedValues.sort(comparator);

            Iterator<? extends SceneItemValue> deletedValuesIter = deletedValues.iterator();
            Iterator<? extends SceneItemValue> addedValuesIter = addedValues.iterator();
            while (deletedValuesIter.hasNext() || addedValuesIter.hasNext()) {
                SceneItemValue deletedValue = deletedValuesIter.hasNext() ? deletedValuesIter.next() : null;
                SceneItemValue addedValue = addedValuesIter.hasNext() ? addedValuesIter.next() : null;
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

                    PolyString value1 = s1.getNewValue() == null ? null : s1.getNewValue().getText();
                    PolyString value2 = s2.getNewValue() == null ? null : s2.getNewValue().getText();

                    return PolyStringComparator.COMPARE_TRANSLATED.compare(value1, value2);
                };
        rv.sort(comparator);
        return rv;
    }

    @NotNull
    public List<SceneItemLineDto> getLines() {
        return lines;
    }

    public boolean isDelta() {
        return sceneItem instanceof SceneDeltaItem;
    }

    public boolean isNullEstimatedOldValues(){
        return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().getEstimatedOldValues() == null;
    }

    public boolean isAdd(){
        return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().isAdd();
    }

    public boolean isDelete(){
        return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().isDelete();
    }

    public boolean isReplace(){
        return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().isReplace();
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
