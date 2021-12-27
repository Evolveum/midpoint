/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import javax.xml.datatype.XMLGregorianCalendar;

public class DateSearchItemWrapper extends PropertySearchItemWrapper {

    private static final long serialVersionUID = 1L;

    public static final String F_FROM_DATE = "fromDate";
    public static final String F_TO_DATE = "toDate";

    private XMLGregorianCalendar fromDate;
    private XMLGregorianCalendar toDate;

    public DateSearchItemWrapper(SearchItemType searchItem) {
        super(searchItem);
    }

    public XMLGregorianCalendar getFromDate() {
        return fromDate;
    }

    public void setFromDate(XMLGregorianCalendar fromDate) {
        this.fromDate = fromDate;
    }

    public XMLGregorianCalendar getToDate() {
        return toDate;
    }

    public void setToDate(XMLGregorianCalendar toDate) {
        this.toDate = toDate;
    }

    @Override
    public Class<DateSearchItemPanel> getSearchItemPanelClass() {
        return DateSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<XMLGregorianCalendar> getDefaultValue() {
        return null;
    }

}
