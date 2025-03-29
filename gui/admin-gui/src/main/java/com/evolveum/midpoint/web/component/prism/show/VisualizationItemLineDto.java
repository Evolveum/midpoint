/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.io.Serializable;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

public class VisualizationItemLineDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_OLD_VALUE = "oldValue";
    public static final String F_NEW_VALUE = "newValue";
    public static final String F_NUMBER_OF_LINES = "numberOfLines";

    private final VisualizationItemDto visualizationItemDto;
    private final VisualizationItemValue visualizationItemOldValue;
    private final VisualizationItemValue visualizationItemNewValue;
    private final boolean isDelta;

    public VisualizationItemLineDto(VisualizationItemDto visualizationItemDto, VisualizationItemValue visualizationItemOldValue, VisualizationItemValue visualizationItemNewValue, boolean isDelta) {
        this.visualizationItemDto = visualizationItemDto;
        this.visualizationItemOldValue = visualizationItemOldValue;
        this.visualizationItemNewValue = visualizationItemNewValue;
        this.isDelta = isDelta;
    }

    public String getName() {
        return visualizationItemDto.getName();
    }

    public VisualizationItemValue getOldValue() {
        return visualizationItemOldValue;
    }

    public VisualizationItemValue getNewValue() {
        return visualizationItemNewValue;
    }

    public Integer getNumberOfLines() {
        return visualizationItemDto.getLines().size();
    }

    public boolean isFirst() {
        return visualizationItemDto.getLines().indexOf(this) == 0;
    }

    public boolean isDelta() {
        return isDelta;
    }

    public boolean isDescriptive() {
        return visualizationItemDto.isDescriptive();
    }

    public boolean isDeltaVisualization() {
        return visualizationItemDto.isDeltaVisualization();
    }

    public boolean oldValueIsUnknown() {
        return visualizationItemDto.oldValueIsUnknown();
    }

    public boolean isAdd() {
        return visualizationItemDto.isAdd();
    }

    public boolean isDelete() {
        return visualizationItemDto.isDelete();
    }

    public boolean isReplace() {
        return visualizationItemDto.isReplace();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        VisualizationItemLineDto that = (VisualizationItemLineDto) o;

        if (isDelta != that.isDelta) {return false;}
        if (visualizationItemOldValue != null ? !visualizationItemOldValue.equals(that.visualizationItemOldValue) : that.visualizationItemOldValue != null) {
            return false;
        }
        return !(visualizationItemNewValue != null ? !visualizationItemNewValue.equals(that.visualizationItemNewValue) : that.visualizationItemNewValue != null);

    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (visualizationItemOldValue != null ? visualizationItemOldValue.hashCode() : 0);
        result = 31 * result + (visualizationItemNewValue != null ? visualizationItemNewValue.hashCode() : 0);
        result = 31 * result + (isDelta ? 1 : 0);
        return result;
    }
}
