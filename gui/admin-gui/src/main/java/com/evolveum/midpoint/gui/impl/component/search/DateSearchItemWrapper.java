/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

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
        return new SearchValue();
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        PrismContext ctx = PrismContext.get();
        ItemPath path = getSearchItem().getPath().getItemPath();
        if (fromDate != null && toDate != null) {
            return ctx.queryFor(ObjectType.class)
                    .item(path)
                    .gt(fromDate)
                    .and()
                    .item(path)
                    .lt(toDate)
                    .buildFilter();
        } else if (fromDate != null) {
            return ctx.queryFor(ObjectType.class)
                    .item(path)
                    .gt(fromDate)
                    .buildFilter();
        } else if (toDate != null) {
            return ctx.queryFor(ObjectType.class)
                    .item(path)
                    .lt(toDate)
                    .buildFilter();
        }
        return null;
    }

}
