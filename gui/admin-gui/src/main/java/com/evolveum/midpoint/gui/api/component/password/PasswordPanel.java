/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import java.io.Serial;
import java.time.Duration;
import java.util.*;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PasswordPanel.class);
    private static final String DOT_CLASS = PasswordPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CREDENTIALS_POLICY = DOT_CLASS + "loadCredentialsPolicy";
    private static final String OPERATION_LOAD_PASSWORD_VALUE_POLICY = DOT_CLASS + "loadPasswordValuePolicy";

    private static final String ID_INPUT_CONTAINER = "inputContainer";
    private static final String ID_PASSWORD_ONE = "password1";
    private static final String ID_VALIDATION_PROGRESS_BAR = "validationProgressBar";
    private static final String ID_PASSWORD_TWO = "password2";
    private static final String ID_PASSWORD_TWO_VALIDATION_MESSAGE = "password2ValidationMessage";
    private static final String ID_VALIDATION_PANEL = "validationPanel";

    protected boolean passwordInputVisible;
    private final PrismObject<? extends FocusType> prismObject;
    private final IModel<ProtectedStringType> passwordModel;
    protected boolean isReadOnly;
    private boolean shouldTrimInput = false;
    private final boolean showOneLinePasswordPanel;

    public <F extends FocusType> PasswordPanel(
            String id,
            IModel<ProtectedStringType> passwordModel,
            boolean isReadOnly,
            boolean isInputVisible,
            PrismObject<F> prismObject) {
        this(id, passwordModel, isReadOnly, isInputVisible, false, prismObject);
    }

    public <F extends FocusType> PasswordPanel(
            String id,
            IModel<ProtectedStringType> passwordModel,
            boolean isReadOnly,
            boolean isInputVisible,
            boolean showOneLinePasswordPanel,
            PrismObject<F> prismObject) {
        super(id);
        this.passwordInputVisible = isInputVisible;
        this.passwordModel = passwordModel;
        this.isReadOnly = isReadOnly;
        this.showOneLinePasswordPanel = showOneLinePasswordPanel;
        this.prismObject = prismObject;
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(new UrlResourceReference(Url.parse("static/vendors-passwords.js")).setContextRelative(true)));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected  <F extends FocusType> void initLayout() {
        setOutputMarkupId(true);

        final WebMarkupContainer inputContainer = new WebMarkupContainer(ID_INPUT_CONTAINER);
        inputContainer.add(new VisibleBehaviour(this::isPasswordInputVisible));
        inputContainer.setOutputMarkupId(true);
        add(inputContainer);

        LoadableDetachableModel<List<StringLimitationResult>> limitationsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<StringLimitationResult> load() {
                return getLimitationsForActualPassword();
            }
        };

        final PasswordLimitationsPanel validationPanel = new PasswordLimitationsPanel(ID_VALIDATION_PANEL, limitationsModel);
        validationPanel.add(new VisibleBehaviour(this::isPasswordLimitationPopupVisible));
        validationPanel.setOutputMarkupId(true);
        inputContainer.add(validationPanel);

        final PasswordTextField password1 = new SecureModelPasswordTextField(ID_PASSWORD_ONE, new ProtectedStringClearPasswordModel(passwordModel)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (removePasswordValueAttribute()) {
                    tag.remove("value");
                }
            }

            @Override
            protected boolean shouldTrimInput() {
                return shouldTrimInput;
            }


        };
        if (isPasswordStrengthBarVisible()) {
            password1.add(AttributeAppender.append("onfocus", initPasswordValidation()));
        }
        password1.setRequired(false);
        password1.add(new EnableBehaviour(this::canEditPassword));
        password1.setOutputMarkupId(true);
        password1.add(AttributeAppender.append("aria-describedby", validationPanel.getMarkupId()));
        inputContainer.add(password1);

        WebMarkupContainer validationProgressBar = new WebMarkupContainer(ID_VALIDATION_PROGRESS_BAR);
        validationProgressBar.setOutputMarkupId(true);
        validationProgressBar.add(new VisibleBehaviour(() -> !showOneLinePasswordPanel));
        inputContainer.add(validationProgressBar);

        final PasswordTextField password2 = new SecureModelPasswordTextField(ID_PASSWORD_TWO,
                new ProtectedStringClearPasswordModel(Model.of(new ProtectedStringType()))) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean shouldTrimInput() {
                return shouldTrimInput;
            }

        };
        password2.setRequired(false);
        password2.setOutputMarkupId(true);
        password2.add(new VisibleEnableBehaviour(() -> !showOneLinePasswordPanel, this::canEditPassword));
        inputContainer.add(password2);

        password1.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean required = !StringUtils.isEmpty(password1.getModelObject());
                password2.setRequired(required);

                changePasswordPerformed();
            }
        });

        IModel<String> password2ValidationModel = () -> getPasswordMatched(password1.getModelObject(), password2.getValue());
        Label password2ValidationMessage = new Label(ID_PASSWORD_TWO_VALIDATION_MESSAGE, password2ValidationModel);
        password2ValidationMessage.setOutputMarkupId(true);
        password2ValidationMessage.add(new VisibleBehaviour(() -> !showOneLinePasswordPanel));
        inputContainer.add(password2ValidationMessage);
        password2.add(AttributeAppender.append("aria-describedby", password2ValidationMessage.getMarkupId()));

        password1.add(new AjaxFormComponentUpdatingBehavior("keyup input") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                validationPanel.refreshItems(target);
                updatePasswordValidation(target);
                target.add(password2ValidationMessage);
                target.appendJavaScript(String.format("""
                        window.MidPointTheme.updatePasswordErrorState('%s', '%s');
                 """, password2ValidationMessage.getMarkupId(), password2.getMarkupId()));
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(500), true));
                attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
            }
        });

        if (!showOneLinePasswordPanel) {
            PasswordValidator pass2Validator = new PasswordValidator(password1);
            password2.add(pass2Validator);
        }
        password2.add(new AjaxFormComponentUpdatingBehavior("keyup input") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(password2ValidationMessage);
                target.appendJavaScript(String.format("""
                        window.MidPointTheme.updatePasswordErrorState('%s', '%s');
                 """, password2ValidationMessage.getMarkupId(), password2.getMarkupId()));
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(500), true));
                attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
            }
        });

        WebComponentUtil.addAjaxOnUpdateBehavior(inputContainer);

    }

    protected boolean isPasswordInputVisible() {
        return passwordInputVisible || getParentPage().getPrincipalFocus() == null;
    }

    private String initPasswordValidation() {
        return "initPasswordValidation({\n"
                + "container: $('#progress-bar-container'),\n"
                + "hierarchy: {\n"
                + "    '0': ['progress-bar-danger', '" + PageBase.createStringResourceStatic("PasswordPanel.strength.veryWeak").getString() + "'],\n"
                + "    '25': ['progress-bar-danger', '" + PageBase.createStringResourceStatic("PasswordPanel.strength.weak").getString() + "'],\n"
                + "    '50': ['progress-bar-warning', '" + PageBase.createStringResourceStatic("PasswordPanel.strength.good").getString() + "'],\n"
                + "    '75': ['progress-bar-success', '" + PageBase.createStringResourceStatic("PasswordPanel.strength.strong").getString() + "'],\n"
                + "    '100': ['progress-bar-success', '" + PageBase.createStringResourceStatic("PasswordPanel.strength.veryStrong").getString() + "']\n"
                + "}\n"
                + "})";
    }

    private String getPasswordMatched(String password1, String password2) {
        if (StringUtils.isEmpty(password1) || StringUtils.isEmpty(password2)) {
            return "";
        }

        if (!Objects.equals(password1, password2)) {
            return PageBase.createStringResourceStatic("passwordPanel.error").getString();
        }
        return "";
    }

    protected boolean canEditPassword() {
        return true;
    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<>();
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE));
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_TWO));
        return list;
    }

    public List<FeedbackMessage> collectPasswordFieldsFeedbackMessages() {
        List<FeedbackMessage> feedbackMessages = new ArrayList<>();
        getFormComponents().forEach(c -> {
            if (c.getFeedbackMessages() != null && c.getFeedbackMessages().hasMessage(0)) {
                feedbackMessages.addAll(c.getFeedbackMessages().messages(null));
            }
        });
        return feedbackMessages;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE);
    }

    public List<StringLimitationResult> getLimitationsForActualPassword() {
        ValuePolicyType valuePolicy = getValuePolicy();
        if (valuePolicy != null) {
            Task task = getParentPage().createAnonymousTask("validation of password");
            try {
                ProtectedStringType newValue = passwordModel == null ? new ProtectedStringType() : passwordModel.getObject();
                return getParentPage().getModelInteractionService().validateValue(
                        newValue, valuePolicy, prismObject, task, task.getResult());
            } catch (Exception e) {
                LOGGER.error("Couldn't validate password security policy", e);
            }
        }
        return new ArrayList<>();
    }

    protected <F extends FocusType> ValuePolicyType getValuePolicy() {
        ValuePolicyType valuePolicy = null;
        if (prismObject == null || !prismObject.canRepresent(ResourceType.class)) {
            //we skip getting value policy for ResourceType because it is some protected string from connector configuration
            Task task = createTask(OPERATION_LOAD_CREDENTIALS_POLICY);
            CredentialsPolicyType credentials = null;
            try {
                credentials = getParentPage().getModelInteractionService().getCredentialsPolicy(prismObject, task, task.getResult());
            } catch (Exception e) {
                LOGGER.warn("Couldn't load credentials policy for focus " + prismObject, e);
            }
            valuePolicy = WebComponentUtil.getPasswordValuePolicy(credentials, OPERATION_LOAD_PASSWORD_VALUE_POLICY, getParentPage());
        }
        return valuePolicy;
    }

    protected Task createTask(String operation) {
        MidPointPrincipal user = AuthUtil.getPrincipalUser();
        Task task;
        if (user != null) {
            task = getParentPage().createSimpleTask(operation);
        } else {
            task = getParentPage().createAnonymousTask(operation);
        }
        return task;
    }

    private static class PasswordValidator implements IValidator<String> {

        private final PasswordTextField p1;

        private PasswordValidator(@NotNull PasswordTextField p1) {
            this.p1 = p1;
        }

        @Override
        public void validate(IValidatable<String> validatable) {
            String s1 = p1.getModelObject();
            String s2 = validatable.getValue();

            if (StringUtils.isEmpty(s1) && StringUtils.isEmpty(s2)) {
                return;
            }

            if (!Objects.equals(s1, s2)) {
                validatable = p1.newValidatable();
                ValidationError err = new ValidationError();
                err.addKey("passwordPanel.error");
                validatable.error(err);
            }
        }
    }

    protected void changePasswordPerformed() {
    }

    protected boolean isPasswordLimitationPopupVisible() {
        return !showOneLinePasswordPanel;
    }

    protected boolean isPasswordStrengthBarVisible() {
        return !showOneLinePasswordPanel;
    }

    protected void updatePasswordValidation(AjaxRequestTarget target) {
    }

    public IModel<ProtectedStringType> getPasswordModel() {
        return passwordModel;
    }

    public PrismObject<? extends FocusType> getPrismObject() {
        return prismObject;
    }

    protected boolean removePasswordValueAttribute() {
        return true;
    }
}
