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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.util.LocalizationMessageComparator;

public class VisualizationItemDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_LINES = "lines";

    @NotNull private final VisualizationItem visualizationItem;
    @NotNull private final VisualizationDto visualizationDto;
    @NotNull private List<VisualizationItemLineDto> lines;

    public VisualizationItemDto(@NotNull VisualizationDto visualizationDto, @NotNull VisualizationItem visualizationItem) {
        Validate.notNull(visualizationDto);
        Validate.notNull(visualizationItem);
        this.visualizationDto = visualizationDto;
        this.visualizationItem = visualizationItem;
        this.lines = computeLines();
    }

    public String getName() {
        Name n = visualizationItem.getName();
        if (n == null) {
            return "";
        }

        if (n.getDisplayName() != null) {
            return LocalizationUtil.translateMessage(n.getDisplayName());
        } else {
            return LocalizationUtil.translateMessage(n.getSimpleName());
        }
    }

    public String getNewValue() {
        return String.valueOf(visualizationItem.getNewValues());
    }

    public List<VisualizationItemLineDto> computeLines() {
        List<VisualizationItemLineDto> rv = new ArrayList<>();

        if (!isDelta()) {
            for (VisualizationItemValue itemValue : visualizationItem.getNewValues()) {
                rv.add(new VisualizationItemLineDto(this, null, itemValue, false));
            }
        } else {
            VisualizationDeltaItem deltaItem = (VisualizationDeltaItem) visualizationItem;

            if (deltaItem.getSourceDelta() != null
                    && deltaItem.getSourceDelta().isReplace()
                    && oldValueIsUnknown()
                    && CollectionUtils.isEmpty(deltaItem.getNewValues())
                    && CollectionUtils.isEmpty(deltaItem.getUnchangedValues())) {
                // MID-10666 this is a special case when we have a REPLACE delta that has no old estimated value
                // and no new values -> clearing item in this case we need to show empty line in visualization
                rv.add(new VisualizationItemLineDto(this, null, null, false));
            }

            for (VisualizationItemValue itemValue : deltaItem.getUnchangedValues()) {
                rv.add(new VisualizationItemLineDto(this, null, itemValue, false));
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
                rv.add(new VisualizationItemLineDto(this, deletedValue, addedValue, true));
            }
        }
        Comparator<? super VisualizationItemLineDto> comparator =
                (s1, s2) -> {
                    if (s1.isDelta()) {
                        return 1;
                    } else if (s2.isDelta()) {
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
    public List<VisualizationItemLineDto> getLines() {
        return lines;
    }

    public boolean isDelta() {
        return visualizationItem instanceof VisualizationDeltaItem;
    }

    public boolean oldValueIsUnknown() {
        return isDelta() && ((VisualizationDeltaItem) visualizationItem).getSourceDelta() != null && ((VisualizationDeltaItem) visualizationItem).getSourceDelta().getEstimatedOldValues() == null;
    }

    public boolean isAdd() {
        return isDelta() && ((VisualizationDeltaItem) visualizationItem).getSourceDelta() != null && ((VisualizationDeltaItem) visualizationItem).getSourceDelta().isAdd();
    }

    public boolean isDelete() {
        return isDelta() && ((VisualizationDeltaItem) visualizationItem).getSourceDelta() != null && ((VisualizationDeltaItem) visualizationItem).getSourceDelta().isDelete();
    }

    public boolean isReplace() {
        return isDelta() && ((VisualizationDeltaItem) visualizationItem).getSourceDelta() != null && ((VisualizationDeltaItem) visualizationItem).getSourceDelta().isReplace();
    }

    public boolean isDeltaVisualization() {
        return visualizationDto.containsDeltaItems();
    }

    public boolean isOperational() {
        return visualizationItem.isOperational();
    }

    public boolean isDescriptive() {
        return visualizationItem.isDescriptive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        VisualizationItemDto that = (VisualizationItemDto) o;

        if (!visualizationItem.equals(that.visualizationItem)) {return false;}
        return lines.equals(that.lines);

    }

    @Override
    public int hashCode() {
        int result = visualizationItem.hashCode();
        result = 31 * result + lines.hashCode();
        return result;
    }
}
