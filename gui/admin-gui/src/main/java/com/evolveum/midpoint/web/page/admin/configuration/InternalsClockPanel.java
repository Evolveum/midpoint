/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.DatePanel;

public class InternalsClockPanel extends BasePanel<XMLGregorianCalendar>{

    private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_OFFSET = "offset";
    private static final String ID_BUTTON_SAVE = "save";
    private static final String ID_BUTTON_RESET = "reset";

    @SpringBean(name = "clock")
    private Clock clock;

    public InternalsClockPanel(String id, IModel<XMLGregorianCalendar> model) {
        super(id, model);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        Form form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        DatePanel offset = new DatePanel(ID_OFFSET, getModel());
        form.add(offset);

            AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageInternals.button.changeTime")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    savePerformed(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    target.add(getPageBase().getFeedbackPanel());
                }
            };
            form.add(saveButton);

            AjaxSubmitButton resetButton = new AjaxSubmitButton(ID_BUTTON_RESET, createStringResource("PageInternals.button.resetTimeChange")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    resetPerformed(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    target.add(getPageBase().getFeedbackPanel());
                }
            };
            form.add(resetButton);
    }

     private void savePerformed(AjaxRequestTarget target) {
            OperationResult result = new OperationResult(PageInternals.class.getName() + ".changeTime");
            XMLGregorianCalendar offset = getModelObject();
            if (offset != null) {
                clock.override(offset);
            }

            result.recordSuccess();
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel(), InternalsClockPanel.this);
        }

        private void resetPerformed(AjaxRequestTarget target) {
            OperationResult result = new OperationResult(PageInternals.class.getName() + ".changeTimeReset");
            clock.resetOverride();
//            getModel().reset();
            result.recordSuccess();
            getPageBase().showResult(result);
            target.add(InternalsClockPanel.this);
            target.add(getPageBase().getFeedbackPanel());
        }

}
