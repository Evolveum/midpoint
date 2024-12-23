/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.gui.impl.component.form.HoneypotBehaviour;
import com.evolveum.midpoint.gui.impl.component.form.HoneypotFormAjaxListener;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class PageAbstractFlow extends PageRegistrationBase {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractFlow.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SUBMIT_REGISTRATION = "submitRegistration";
    private static final String ID_SUBMIT_LABEL = "submitLabel";

    private static final String ID_DYNAMIC_FORM = "dynamicForm";
    protected static final String ID_CONTENT_AREA = "contentArea";

    private static final String DOT_CLASS = PageAbstractFlow.class.getName() + ".";

    protected static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";

    protected PageParameters pageParameters;
    protected boolean isSubmitted = false;

    public abstract void initializeModel();
    public abstract IModel<UserType> getUserModel();
    public abstract boolean isCustomFormDefined();
    protected abstract WebMarkupContainer initStaticLayout();
    protected abstract WebMarkupContainer initDynamicLayout();
    protected abstract void submitRegistration(AjaxRequestTarget target);
    protected abstract ObjectReferenceType getCustomFormRef();

    public PageAbstractFlow(PageParameters pageParameters) {
        this.pageParameters = pageParameters;
        initializeModel();
        initLayout();
    }

    private void initLayout() {
        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        mainForm.setMultiPart(true);
        add(mainForm);

        WebMarkupContainer content;
        Fragment fragment;
        if (!isCustomFormDefined()) {
            fragment = new Fragment(ID_CONTENT_AREA, "staticContent", this);
            content = initStaticLayout();

        } else {
            fragment = new Fragment(ID_CONTENT_AREA, "dynamicContent", this);
            content = initDynamicLayout();

        }

        fragment.setOutputMarkupId(true);
        fragment.add(new VisibleBehaviour(() -> !isSubmitted));
        content.setOutputMarkupId(true);
        addHoneypotBehaviour(mainForm);
        initButtons(mainForm);
        fragment.add(content);
        mainForm.add(fragment);

    }

    private void addHoneypotBehaviour(MidpointForm<?> mainForm) {
        if (!isSubmitted) {
            mainForm.add(new HoneypotBehaviour());
        }
    }

    private void initButtons(MidpointForm<?> mainForm) {
        AjaxSubmitButton register = new AjaxSubmitButton(ID_SUBMIT_REGISTRATION) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                showErrors(target);
            }

            protected void onSubmit(AjaxRequestTarget target) {
                submitRegistration(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new HoneypotFormAjaxListener(mainForm));
            }

            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                response.render(OnDomReadyHeaderItem.forScript("window.MidPointHoneypot.initHoneypotFields('" + mainForm.getMarkupId() + "')"));
            }
        };
        register.add(new VisibleBehaviour(() -> !isSubmitted));
        mainForm.add(register);

        Label submitLabel = new Label(ID_SUBMIT_LABEL, createStringResource(getSubmitLabelKey()));
        register.add(submitLabel);
    }

    protected String getSubmitLabelKey() {
        return "PageSelfRegistration.register";
    }

    protected MidpointForm<?> getMainForm() {
        return (MidpointForm<?>) get(ID_MAIN_FORM);
    }

    protected DynamicFormPanel<UserType> createDynamicPanel(MidpointForm<?> mainForm, Task task) {
        final ObjectReferenceType ort = getCustomFormRef();

        if (ort == null) {
            return null;
        }

        return new DynamicFormPanel<>(ID_DYNAMIC_FORM,
                getUserModel(), ort.getOid(), mainForm, task, PageAbstractFlow.this, true);
    }

    private void showErrors(AjaxRequestTarget target) {
        target.add(getFeedbackPanel());
    }

    protected DynamicFormPanel<UserType> getDynamicFormPanel() {
        //noinspection unchecked
        return (DynamicFormPanel<UserType>) get(
                createComponentPath(ID_MAIN_FORM, ID_CONTENT_AREA, ID_DYNAMIC_FORM));
    }

}
