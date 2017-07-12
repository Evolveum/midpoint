/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.model.XmlGregorianCalendarModel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author lazyman
 */
public class DateFormGroup extends BasePanel<XMLGregorianCalendar> {

    private static final String ID_DATE = "date";
    private static final String ID_DATE_WRAPPER = "dateWrapper";
    private static final String ID_LABEL = "label";
    private static final String ID_FEEDBACK = "feedback";

    public DateFormGroup(String id, IModel<XMLGregorianCalendar> value, IModel<String> label, String labelSize, String textSize,
                         boolean required) {
        super(id, value);

        initLayout(label, labelSize, textSize, required);
    }

    private void initLayout(IModel<String> label, String labelSize, String textSize, boolean required) {
        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelSize)) {
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

        WebMarkupContainer dateWrapper = new WebMarkupContainer(ID_DATE_WRAPPER);
        if (StringUtils.isNotEmpty(textSize)) {
            dateWrapper.add(AttributeAppender.prepend("class", textSize));
        }
        add(dateWrapper);

        DateInput date = new DateInput(ID_DATE, new XmlGregorianCalendarModel(getModel()));
        date.setRequired(required);
        date.setLabel(label);
        dateWrapper.add(date);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(date));
        dateWrapper.add(feedback);
    }
}
