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

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditReportDto;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.web.util.WebMiscUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.Date;

/**
 * @author lazyman
 */
public class AuditPopupPanel extends SimplePanel<AuditReportDto> {

    private static final String ID_FORM = "form";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_AUDITEVENTTYPE = "auditEventType";
    private static final String ID_RUN = "run";

    public AuditPopupPanel(String id, IModel<AuditReportDto> model) {
        super(id, model);

        initLayout(this);
    }

    @SuppressWarnings("serial")
    protected void initLayout(final Component component) {
        Form form = new Form(ID_FORM);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        IChoiceRenderer<AuditEventType> renderer = new IChoiceRenderer<AuditEventType>() {

            @Override
            public Object getDisplayValue(AuditEventType object) {
                return WebMiscUtil.createLocalizedModelForEnum(object, component).getObject();
            }

            @Override
            public String getIdValue(AuditEventType object, int index) {
                return Integer.toString(index);
            }
        };

        DropDownChoice dropDown = new DropDownChoice(ID_AUDITEVENTTYPE, new PropertyModel<AuditEventType>(getModel(),
                AuditReportDto.F_AUDITEVENTTYPE), WebMiscUtil.createReadonlyModelFromEnum(AuditEventType.class),
                renderer) {

            @Override
            protected String getNullValidDisplayValue() {
                return AuditPopupPanel.this.getString("AuditEventType.null");
            }
        };
        dropDown.setNullValid(true);


        form.add(dropDown);

        DateInput dateFrom = new DateInput(ID_DATE_FROM, new PropertyModel<Date>(getModel(), AuditReportDto.F_FROM));
        dateFrom.setRequired(true);
        form.add(dateFrom);

        DateInput dateTo = new DateInput(ID_DATE_TO, new PropertyModel<Date>(getModel(), AuditReportDto.F_TO));
        dateTo.setRequired(true);
        form.add(dateTo);

        form.add(new DateValidator(dateFrom, dateTo));

        AjaxSubmitButton run = new AjaxSubmitButton(ID_RUN, createStringResource("PageBase.button.run")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onRunPerformed(target);
            }
        };
        form.add(run);
    }


    protected void onRunPerformed(AjaxRequestTarget target) {

    }
}
