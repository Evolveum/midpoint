/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.panel.DateSearchItemPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

public class DateSearchItemWrapper extends PropertySearchItemWrapper<XMLGregorianCalendar> {

    private static final long serialVersionUID = 1L;

    public static final String F_FROM_DATE = "singleDate";
    public static final String F_TO_DATE = "intervalSecondDate";

    private XMLGregorianCalendar singleDate;
    private XMLGregorianCalendar intervalSecondDate;
    boolean isInterval = true;

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

}
