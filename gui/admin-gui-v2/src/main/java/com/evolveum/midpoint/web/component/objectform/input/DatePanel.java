/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.objectform.input;

import com.evolveum.midpoint.web.component.objectform.InputPanel;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import java.util.Date;

/**
 * @author lazyman
 */
public class DatePanel extends InputPanel {

    public DatePanel(String id, IModel<Date> model) {
        super(id);

        DateTextField date = DateTextField.forDatePattern("input", model, "dd/MMM/yyyy");
        date.add(new DatePicker());
        add(date);
    }


    @Override
    public FormComponent getComponent() {
        return (FormComponent) get("input");
    }
}
