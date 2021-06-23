/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;

/**
 * @author honchar
 */
public class DateSearchPanel extends DateIntervalSearchPanel {

    private static final long serialVersionUID = 1L;

    public DateSearchPanel(String id, IModel<XMLGregorianCalendar> fromDateModel) {
        super(id, fromDateModel, (IModel<XMLGregorianCalendar>) () -> null);
    }

    @Override
    protected boolean isInterval() {
        return false;
    }
}
