/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.panel.DateSearchItemPanel;
import com.evolveum.midpoint.gui.impl.component.search.panel.NamedIntervalPreset;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;

import java.util.List;

public class DateSearchItemWrapper extends PropertySearchItemWrapper<XMLGregorianCalendar> {

    private static final long serialVersionUID = 1L;

    public static final String F_FROM_DATE = "singleDate";
    public static final String F_TO_DATE = "intervalSecondDate";
    public static final String F_INTERVAL_PRESETS = "intervalPresets";
    public static final String F_SELECTED_INTERVAL_PRESET = "selectedIntervalPreset";

    private XMLGregorianCalendar singleDate;
    private XMLGregorianCalendar intervalSecondDate;
    boolean isInterval = true;

    private List<NamedIntervalPreset> intervalPresets = NamedIntervalPreset.DEFAULT_PRESETS;
    private NamedIntervalPreset selectedIntervalPreset;

    public DateSearchItemWrapper(ItemPath path) {
        super(path);
    }

    public XMLGregorianCalendar getSingleDate() {
        return singleDate;
    }

    public void setSingleDate(XMLGregorianCalendar singleDate) {
        this.singleDate = singleDate;
    }

    public XMLGregorianCalendar getIntervalSecondDate() {
        return intervalSecondDate;
    }

    public void setIntervalSecondDate(XMLGregorianCalendar intervalSecondDate) {
        this.intervalSecondDate = intervalSecondDate;
    }

    @Override
    public Class<DateSearchItemPanel> getSearchItemPanelClass() {
        return DateSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<XMLGregorianCalendar> getDefaultValue() {
        return new SearchValue();
    }

    public boolean isInterval() {
        return isInterval;
    }

    public void setInterval(boolean interval) {
        isInterval = interval;
    }

    @Override
    public DisplayableValue<XMLGregorianCalendar> getValue() {
        if (!isInterval && singleDate != null) {
            return new SearchValue<>(singleDate);
        }
        return super.getValue();
    }

    @Override
    public void setValue(DisplayableValue<XMLGregorianCalendar> value) {
        if (!isInterval && singleDate != null) {
            singleDate = null;
            return;
        }

        super.setValue(value);
    }

    @Override
    public void clearValue() {
        singleDate = getDefaultValue() != null ? getDefaultValue().getValue() : null;
        intervalSecondDate = null;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        PrismContext ctx = PrismContext.get();
        ItemPath path = getPath();
        if (singleDate != null && intervalSecondDate != null) {
            return ctx.queryFor(type)
                    .item(path)
                    .ge(singleDate)
                    .and()
                    .item(path)
                    .le(intervalSecondDate)
                    .buildFilter();
        } else if (singleDate != null) {
            return ctx.queryFor(type)
                    .item(path)
                    .ge(singleDate)
                    .buildFilter();
        } else if (intervalSecondDate != null) {
            return ctx.queryFor(type)
                    .item(path)
                    .le(intervalSecondDate)
                    .buildFilter();
        }
        return null;
    }

    public List<NamedIntervalPreset> getIntervalPresets() {
        return intervalPresets;
    }

    public void setIntervalPresets(List<NamedIntervalPreset> intervalPresets) {
        this.intervalPresets = intervalPresets;
    }

    public NamedIntervalPreset getSelectedIntervalPreset() {
        return selectedIntervalPreset;
    }

    public void setSelectedIntervalPreset(NamedIntervalPreset selectedIntervalPreset) {
        this.selectedIntervalPreset = selectedIntervalPreset;
    }
}
