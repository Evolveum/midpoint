/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.web.util.DateValidator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;

public class DateIntervalSearchPopupPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_DATE_FORM = "dateForm";
    private static final String ID_DATE_FROM_VALUE = "dateFromValue";
    private static final String ID_DATE_TO_VALUE = "dateToValue";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";

    private IModel<XMLGregorianCalendar> fromDateModel;
    private IModel<XMLGregorianCalendar> toDateModel;

    public DateIntervalSearchPopupPanel(String id, IModel<XMLGregorianCalendar> fromDateModel, IModel<XMLGregorianCalendar> toDateModel) {
        super(id);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        Form dateForm = new Form(ID_DATE_FORM);
        dateForm.setOutputMarkupId(true);
        add(dateForm);

        DateValidator validator = WebComponentUtil.getRangeValidator(dateForm, SchemaConstants.PATH_ACTIVATION);

        DatePanel fromDatePanel = new DatePanel(ID_DATE_FROM_VALUE, fromDateModel);
        fromDatePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        dateForm.add(fromDatePanel);
        validator.setDateFrom((DateTimeField) fromDatePanel.getBaseFormComponent());

        DatePanel toDatePanel = new DatePanel(ID_DATE_TO_VALUE, toDateModel);
        toDatePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        dateForm.add(toDatePanel);
        validator.setDateFrom((DateTimeField) toDatePanel.getBaseFormComponent());

        AjaxButton confirm = new AjaxButton(ID_CONFIRM_BUTTON, createStringResource("Button.ok")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                confirmPerformed(target);
            }
        };
        dateForm.add(confirm);

    }

    protected void confirmPerformed(AjaxRequestTarget target){
    }

}
