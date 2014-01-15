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

import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.UserReportDto;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.Date;

/**
 *  @author shood
 * */
public class UserReportConfigPanel extends SimplePanel<UserReportDto>{

    private static final String ID_FORM = "form";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPORT_TYPE = "exportType";
    private static final String ID_BUTTON_RUN = "runSave";
    private static final String ID_BUTTON_SAVE = "save";
    private static final String ID_BUTTON_CANCEL = "cancel";

    public UserReportConfigPanel(String componentId, IModel<UserReportDto> model){
        super(componentId, model);

        initLayout(this);
    }

    protected void initLayout(final Component component){
        Form form = new Form(ID_FORM);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        TextField<String> description = new TextField<String>(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), UserReportDto.F_DESCRIPTION));
        description.setRequired(true);
        form.add(description);

        IChoiceRenderer<ExportType> renderer = new IChoiceRenderer<ExportType>() {

            @Override
            public Object getDisplayValue(ExportType object) {
                return WebMiscUtil.createLocalizedModelForEnum(object, component).getObject();
            }

            @Override
            public String getIdValue(ExportType object, int index) {
                return Integer.toString(index);
            }
        };

        DropDownChoice dropDown = new DropDownChoice(ID_EXPORT_TYPE, new PropertyModel<ExportType>(getModel(),
                UserReportDto.F_EXPORT_TYPE), WebMiscUtil.createReadonlyModelFromEnum(ExportType.class), renderer){

            @Override
            protected CharSequence getDefaultChoice(final String selectedValue){
                return ExportType.PDF.toString();
            }
        };
        form.add(dropDown);

        DateInput dateFrom = new DateInput(ID_DATE_FROM, new PropertyModel<Date>(getModel(), UserReportDto.F_FROM));
        dateFrom.setRequired(true);
        form.add(dateFrom);

        DateInput dateTo = new DateInput(ID_DATE_TO, new PropertyModel<Date>(getModel(), UserReportDto.F_TO));
        dateFrom.setRequired(true);
        form.add(dateTo);

        form.add(new DateValidator(dateFrom, dateTo));

        AjaxSubmitButton saveAndRun = new AjaxSubmitButton(ID_BUTTON_RUN, createStringResource("PageBase.button.saveAndRun")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSaveAndRunPerformed(target);
            }
        };
        form.add(saveAndRun);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSavePerformed(target);
            }
        };
        form.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL, createStringResource("PageBase.button.cancel")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onCancelPerformed(target);
            }
        };
        form.add(cancel);
    }

    protected void onSaveAndRunPerformed(AjaxRequestTarget target) {}
    protected void onSavePerformed(AjaxRequestTarget target) {}
    protected void onCancelPerformed(AjaxRequestTarget target) {}

}
