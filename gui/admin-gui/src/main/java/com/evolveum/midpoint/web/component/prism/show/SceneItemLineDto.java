/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

import java.io.Serializable;

public class SceneItemLineDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_OLD_VALUE = "oldValue";
    public static final String F_NEW_VALUE = "newValue";
    public static final String F_NUMBER_OF_LINES = "numberOfLines";

    private final SceneItemDto sceneItemDto;
    private final VisualizationItemValue sceneItemOldValue;
    private final VisualizationItemValue sceneItemNewValue;
    private final boolean isDelta;

    public SceneItemLineDto(SceneItemDto sceneItemDto, VisualizationItemValue sceneItemOldValue, VisualizationItemValue sceneItemNewValue, boolean isDelta) {
        this.sceneItemDto = sceneItemDto;
        this.sceneItemOldValue = sceneItemOldValue;
        this.sceneItemNewValue = sceneItemNewValue;
        this.isDelta = isDelta;
    }

    public String getName() {
        return sceneItemDto.getName();
    }

    public VisualizationItemValue getOldValue() {
        return sceneItemOldValue;
    }

    public VisualizationItemValue getNewValue() {
        return sceneItemNewValue;
    }

    public Integer getNumberOfLines() {
        return sceneItemDto.getLines().size();
    }

    public boolean isFirst() {
        return sceneItemDto.getLines().indexOf(this) == 0;
    }

    public boolean isDelta() {
        return isDelta;
    }

    public boolean isDescriptive() {
        return sceneItemDto.isDescriptive();
    }

    public boolean isDeltaScene() {
        return sceneItemDto.isDeltaScene();
    }

    public boolean isNullEstimatedOldValues(){
        return sceneItemDto.isNullEstimatedOldValues();
    }

    public boolean isAdd(){
        return sceneItemDto.isAdd();
    }

    public boolean isDelete(){
        return sceneItemDto.isDelete();
    }

    public boolean isReplace(){
        return sceneItemDto.isReplace();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SceneItemLineDto that = (SceneItemLineDto) o;

        if (isDelta != that.isDelta) return false;
        if (sceneItemOldValue != null ? !sceneItemOldValue.equals(that.sceneItemOldValue) : that.sceneItemOldValue != null)
            return false;
        return !(sceneItemNewValue != null ? !sceneItemNewValue.equals(that.sceneItemNewValue) : that.sceneItemNewValue != null);

    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (sceneItemOldValue != null ? sceneItemOldValue.hashCode() : 0);
        result = 31 * result + (sceneItemNewValue != null ? sceneItemNewValue.hashCode() : 0);
        result = 31 * result + (isDelta ? 1 : 0);
        return result;
    }
}
