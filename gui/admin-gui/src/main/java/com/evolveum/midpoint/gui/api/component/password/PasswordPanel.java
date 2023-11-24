/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PasswordPanel.class);

    private static final String ID_LINK_CONTAINER = "linkContainer";
    private static final String ID_PASSWORD_SET = "passwordSet";
    private static final String ID_PASSWORD_REMOVE = "passwordRemove";
    private static final String ID_CHANGE_PASSWORD_LINK = "changePasswordLink";
    private static final String ID_REMOVE_PASSWORD_LINK = "removePasswordLink";

    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_INPUT_CONTAINER = "inputContainer";
    private static final String ID_PASSWORD_ONE = "password1";
    private static final String ID_PASSWORD_TWO = "password2";
    private static final String ID_PASSWORD_TWO_VALIDATION_MESSAGE = "password2ValidationMessage";
    private static final String ID_VALIDATION_PANEL = "validationPanel";

    private boolean passwordInputVisible;
    private static boolean clearPasswordInput = false;
    private static boolean setPasswordInput = false;
    private final IModel<ProtectedStringType> model;

    private boolean isReadOnly;
    private boolean shouldTrimInput = false;


    public PasswordPanel(String id, IModel<ProtectedStringType> model) {
        this(id, model, false, model == null || model.getObject() == null);
    }

    public <F extends FocusType> PasswordPanel(String id, IModel<ProtectedStringType> model, PrismObject<F> object) {
        this(id, model, false, model == null || model.getObject() == null, object);
    }

    public PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly, boolean isInputVisible) {
        this(id, model, isReadOnly, isInputVisible, null);
    }

    public <F extends FocusType> PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly, boolean isInputVisible,
            PrismObject<F> object) {
        super(id);
        this.passwordInputVisible = isInputVisible;
        this.model = model;
        this.isReadOnly = isReadOnly;
        initLayout(object);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private <F extends FocusType> void initLayout(PrismObject<F> object) {
        setOutputMarkupId(true);

        final WebMarkupContainer inputContainer = new WebMarkupContainer(ID_INPUT_CONTAINER);
        inputContainer.add(new VisibleBehaviour(() -> passwordInputVisible));
        inputContainer.setOutputMarkupId(true);
        add(inputContainer);

        LoadableDetachableModel<List<StringLimitationResult>> limitationsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<StringLimitationResult> load() {
                ValuePolicyType valuePolicy = null;
                if (object == null || !object.canRepresent(ResourceType.class)) {
                    //we skip getting value policy for ResourceType because it is some protected string from connector configuration
                    valuePolicy = getValuePolicy(object);
                }
                return getLimitationsForActualPassword(valuePolicy, object);
            }
        };

        final PasswordLimitationsPanel validationPanel = new PasswordLimitationsPanel(ID_VALIDATION_PANEL, limitationsModel);
        validationPanel.setOutputMarkupId(true);
        inputContainer.add(validationPanel);

        final PasswordTextField password1 = new SecureModelPasswordTextField(ID_PASSWORD_ONE, new PasswordModel(model)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (clearPasswordInput) {
                    tag.remove("value");
                }
            }

            @Override
            protected boolean shouldTrimInput() {
                return shouldTrimInput;
            }
        };
        password1.add(AttributeAppender.append("onfocus", initPasswordValidation()));
        password1.setRequired(false);
        password1.setOutputMarkupId(true);
        password1.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        inputContainer.add(password1);

        final PasswordTextField password2 = new SecureModelPasswordTextField(ID_PASSWORD_TWO,
                new PasswordModel(Model.of(new ProtectedStringType()))) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean shouldTrimInput() {
                return shouldTrimInput;
            }
        };
        password2.setRequired(false);
        password2.setOutputMarkupId(true);
        inputContainer.add(password2);

        password1.add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

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
        inputContainer.add(password2ValidationMessage);

        password1.add(new AjaxFormComponentUpdatingBehavior("keyup input") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
//                limitationsModel.reset();
                validationPanel.refreshItems(target);
                updatePasswordValidation(target);
                target.add(password2ValidationMessage);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(500), true));
                attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
            }
        });

        PasswordValidator pass2Validator = new PasswordValidator(password1);
        password2.add(pass2Validator);
        password2.add(new AjaxFormComponentUpdatingBehavior("keyup input") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(password2ValidationMessage);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(500), true));
                attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
            }
        });

        final WebMarkupContainer linkContainer = new WebMarkupContainer(ID_LINK_CONTAINER);
        linkContainer.add(new VisibleBehaviour(() -> !passwordInputVisible));
        linkContainer.setOutputMarkupId(true);
        add(linkContainer);

        final Label passwordSetLabel = new Label(ID_PASSWORD_SET, new ResourceModel("passwordPanel.passwordSet"));
        linkContainer.add(passwordSetLabel);

        final Label passwordRemoveLabel = new Label(ID_PASSWORD_REMOVE, new ResourceModel("passwordPanel.passwordRemoveLabel"));
        passwordRemoveLabel.setVisible(false);
        linkContainer.add(passwordRemoveLabel);

        WebMarkupContainer buttonBar = new WebMarkupContainer(ID_BUTTON_BAR);
        buttonBar.add(new VisibleBehaviour(() -> isChangePasswordVisible() || isRemovePasswordVisible()));
        add(buttonBar);

        AjaxLink<Void> changePassword = new AjaxLink<>(ID_CHANGE_PASSWORD_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearPasswordInput = true;
                setPasswordInput = false;
                onLinkClick(target);
            }
        };
        changePassword.add(new VisibleBehaviour(() ->  isChangePasswordVisible()));
        changePassword.setBody(new ResourceModel("passwordPanel.passwordChange"));
        changePassword.setOutputMarkupId(true);
        buttonBar.add(changePassword);

        AjaxLink<Void> removePassword = new AjaxLink<>(ID_REMOVE_PASSWORD_LINK) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemovePassword(model, target);
            }
        };
        removePassword.add(new VisibleBehaviour(() -> isRemovePasswordVisible()));
        removePassword.setBody(new ResourceModel("passwordPanel.passwordRemove"));
        removePassword.setOutputMarkupId(true);
        buttonBar.add(removePassword);
    }

    private boolean isChangePasswordVisible() {
        return !isReadOnly && !passwordInputVisible && model != null && model.getObject() != null;
    }

    protected boolean isRemovePasswordVisible() {
        // todo wrong code, panel should be stupid, it must not know about different pages and subpages...
//        PageBase pageBase = getPageBase();
//        if (pageBase == null) {
//            return false;
//        }
//        if (pageBase instanceof PageUserSelfProfile || pageBase instanceof PageOrgSelfProfile
//                || pageBase instanceof PageRoleSelfProfile || pageBase instanceof PageServiceSelfProfile) {
//            return false;
//        }
//        return pageBase instanceof PageAdminFocus && !((PageAdminFocus) pageBase).isLoggedInFocusPage()
//                && model.getObject() != null;

        return !isReadOnly && model.getObject() != null && SecurityUtils.getPrincipalUser() != null;
    }

    private String initPasswordValidation() {
        return  "initPasswordValidation({\n"
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

    protected <F extends FocusType> ValuePolicyType getValuePolicy(PrismObject<F> object) {
        ValuePolicyType valuePolicyType = null;
        try {
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (getPageBase() != null) {
                if (user != null) {
                    Task task = getPageBase().createSimpleTask("load value policy");
                    valuePolicyType = searchValuePolicy(object, task);
                } else {
                    valuePolicyType = getPageBase().getSecurityContextManager().runPrivileged((Producer<ValuePolicyType>) () -> {
                        Task task = getPageBase().createAnonymousTask("load value policy");
                        return searchValuePolicy(object, task);
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't load security policy for focus " + object, e);
        }
        return valuePolicyType;
    }

    private <F extends FocusType> ValuePolicyType searchValuePolicy(PrismObject<F> object, Task task) {
        try {
            CredentialsPolicyType credentials = getPageBase().getModelInteractionService().getCredentialsPolicy(object, task, task.getResult());
            if (credentials != null && credentials.getPassword() != null
                    && credentials.getPassword().getValuePolicyRef() != null) {
                PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                        credentials.getPassword().getValuePolicyRef(), getPageBase(), task, task.getResult());
                if (valuePolicy != null) {
                    return valuePolicy.asObjectable();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't load security policy for focus " + object, e);
        }
        return null;
    }

    private void onLinkClick(AjaxRequestTarget target) {
        passwordInputVisible = true;
        target.add(this);
    }

    private void onRemovePassword(IModel<ProtectedStringType> model, AjaxRequestTarget target) {
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_SET).setVisible(false);
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_REMOVE).setVisible(true);
        passwordInputVisible = false;
        model.setObject(null);
        target.add(this);
    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<>();
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE));
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_TWO));
        return list;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE);
    }

    public List<StringLimitationResult> getLimitationsForActualPassword(ValuePolicyType valuePolicy, PrismObject<? extends ObjectType> object) {
        if (valuePolicy != null) {
            Task task = getPageBase().createAnonymousTask("validation of password");
            try {
                ProtectedStringType newValue = !setPasswordInput ? new ProtectedStringType() : model.getObject();
                return getPageBase().getModelInteractionService().validateValue(
                        newValue, valuePolicy, object, task, task.getResult());
            } catch (Exception e) {
                LOGGER.error("Couldn't validate password security policy", e);
            }
        }
        return new ArrayList<>();
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

    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("blur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }

    private static class PasswordModel implements IModel<String> {
        private static final long serialVersionUID = 1L;

        IModel<ProtectedStringType> psModel;

        PasswordModel(IModel<ProtectedStringType> psModel) {
            this.psModel = psModel;
        }

        @Override
        public void detach() {
            // Nothing to do
        }

        private Protector getProtector() {
            return ((MidPointApplication) Application.get()).getProtector();
        }

        @Override
        public String getObject() {
            ProtectedStringType ps = psModel.getObject();
            if (ps == null) {
                return null;
            } else {
                try {
                    return getProtector().decryptString(ps);
                } catch (EncryptionException e) {
                    throw new SystemException(e.getMessage(), e);   // todo handle somewhat better
                }
            }
        }

        @Override
        public void setObject(String object) {
            if (clearPasswordInput) {
                clearPasswordInput = false;
                setPasswordInput = false;
                return;
            }
            setPasswordInput = true;
            if (object == null) {
                psModel.setObject(null);
            } else {
                if (psModel.getObject() == null) {
                    psModel.setObject(new ProtectedStringType());
                } else {
                    psModel.getObject().clear();
                }
                psModel.getObject().setClearValue(object);
                try {
                    getProtector().encrypt(psModel.getObject());
                } catch (EncryptionException e) {
                    throw new SystemException(e.getMessage(), e);   // todo handle somewhat better
                }
            }
        }
    }

    protected void changePasswordPerformed() {
    }

    protected void updatePasswordValidation(AjaxRequestTarget target) {
    }
}
