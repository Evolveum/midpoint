/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.input;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DateField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * @author lazyman
 */
public class DatePanel extends InputPanel {

    private static final String ID_INPUT = "input";

    public DatePanel(String id, IModel<XMLGregorianCalendar> model) {
        super(id);

//        DateField date = DateTextField.forDatePattern("input", createDateModel(model), "dd/MMM/yyyy");
        DateField date = new DateField(ID_INPUT, createDateModel(model));
//        date.add(new DatePicker());
        add(date);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }

    private IModel<Date> createDateModel(final IModel<XMLGregorianCalendar> model) {
        return new Model<Date>() {

            @Override
            public Date getObject() {
                XMLGregorianCalendar calendar = model.getObject();
                if (calendar == null) {
                    return null;
                }
                return MiscUtil.asDate(calendar);
            }

            @Override
            public void setObject(Date object) {
                if (object == null) {
                    model.setObject(null);
                } else {
                    model.setObject(MiscUtil.asXMLGregorianCalendar(object));
                }
            }
        };
    }
}
