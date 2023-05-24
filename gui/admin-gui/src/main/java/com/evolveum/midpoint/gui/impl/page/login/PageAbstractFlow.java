/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.component.captcha.CaptchaPanel;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class PageAbstractFlow extends PageRegistrationBase {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractFlow.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SUBMIT_REGISTRATION = "submitRegistration";

    private static final String ID_DYNAMIC_FORM = "dynamicForm";
    protected static final String ID_CONTENT_AREA = "contentArea";

    private static final String ID_CAPTCHA = "captcha";

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
        content.setOutputMarkupId(true);
        addOrReplaceCaptcha(fragment);
        initButtons(mainForm);
        fragment.add(content);
        mainForm.add(fragment);

    }

    private void addOrReplaceCaptcha(WebMarkupContainer content) {
        CaptchaPanel captcha = new CaptchaPanel(ID_CAPTCHA, this);
        captcha.setOutputMarkupId(true);
        captcha.add(new VisibleBehaviour(() -> !isSubmitted));
        content.addOrReplace(captcha);
    }

    private void initButtons(MidpointForm<?> mainForm) {
        AjaxSubmitButton register = new AjaxSubmitButton(ID_SUBMIT_REGISTRATION) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                showErrors(target);
            }

            protected void onSubmit(AjaxRequestTarget target) {
                doRegistration(target);

            }
        };
        register.add(new VisibleBehaviour(() -> !isSubmitted));
        mainForm.add(register);
    }

    private void doRegistration(AjaxRequestTarget target) {
        if (!validateCaptcha(target)) {
            return;
        }

        submitRegistration(target);
        isSubmitted = true;
    }

    private boolean validateCaptcha(AjaxRequestTarget target) {
        String value = System.getProperty(MidpointConfiguration.MIDPOINT_SCHRODINGER_PROPERTY);
        boolean isSchrodingerTesting = Boolean.parseBoolean(value);
        if (isSchrodingerTesting) {
            LOGGER.trace("Skipping CAPTCHA Validation, because system variable (midpoint.schrodinget) for schrodinger testing is TRUE");
            return true;
        }

        CaptchaPanel captcha = getCaptcha();
        if (captcha.getRandomText() == null) {
            String message = createStringResource("PageSelfRegistration.captcha.validation.failed")
                    .getString();
            LOGGER.error(message);
            getSession().error(message);
            target.add(getFeedbackPanel());
            updateCaptcha(target);
            return false;
        }

        if (captcha.getCaptchaText() != null && captcha.getRandomText() != null) {
            if (!captcha.getCaptchaText().equals(captcha.getRandomText())) {
                String message = createStringResource("PageSelfRegistration.captcha.validation.failed")
                        .getString();
                LOGGER.error(message);
                getSession().error(message);
                updateCaptcha(target);
                target.add(getFeedbackPanel());
                return false;
            }
        }
        LOGGER.trace("CAPTCHA Validation OK");
        return true;
    }

    protected void updateCaptcha(AjaxRequestTarget target) {
        Fragment fragment = (Fragment) get(createComponentPath(ID_MAIN_FORM, ID_CONTENT_AREA));
        addOrReplaceCaptcha(fragment);
        target.add(fragment);
    }

    private CaptchaPanel getCaptcha() {
        return (CaptchaPanel) get(createComponentPath(ID_MAIN_FORM, ID_CONTENT_AREA, ID_CAPTCHA));
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
