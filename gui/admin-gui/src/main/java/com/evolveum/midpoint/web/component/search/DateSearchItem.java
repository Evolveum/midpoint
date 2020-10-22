/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author honchar
 */
public class DateSearchItem extends PropertySearchItem {

    private static final long serialVersionUID = 1L;

    private XMLGregorianCalendar fromDate;
    private XMLGregorianCalendar toDate;

    public DateSearchItem(Search search, ItemPath path, ItemDefinition definition, PolyStringType displayName) {
        super(search, path, definition, null, displayName);
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
    public Type getType() {
        return Type.DATE;
    }
}
