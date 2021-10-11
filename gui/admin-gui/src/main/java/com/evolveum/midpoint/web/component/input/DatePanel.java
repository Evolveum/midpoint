/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.model.XmlGregorianCalendarModel;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author lazyman
 */
public class DatePanel extends InputPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_INPUT = "input";

    public DatePanel(String id, IModel<XMLGregorianCalendar> model) {
        super(id);

        DateInput date = new DateInput(ID_INPUT, new XmlGregorianCalendarModel(model));
        date.setOutputMarkupId(true);
        add(date);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }
}
