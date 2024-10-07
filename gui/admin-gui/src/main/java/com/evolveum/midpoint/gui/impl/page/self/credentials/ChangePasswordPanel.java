/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.credentials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordHintPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordLimitationsPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.progress.ProgressDto;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import java.io.Serial;

import org.jetbrains.annotations.NotNull;

public class ChangePasswordPanel<F extends FocusType> extends BasePanel<F> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordPanel.class);

    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_CURRENT_PASSWORD_FIELD = "currentPassword";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    private static final String ID_PASSWORD_HINT_PANEL = "passwordHintPanel";
    private static final String ID_CHANGE_PASSWORD = "changePassword";
    private static final String ID_PASSWORD_VALIDATION_PANEL = "passwordValidationPanel";
    private static final String CHANGE_PASSWORD_BUTTON_STYLE = "btn btn-success";

    private static final String DOT_CLASS = ChangePasswordPanel.class.getName() + ".";
    private static final String OPERATION_VALIDATE_PASSWORD = DOT_CLASS + "validatePassword";
    private static final String OPERATION_LOAD_CREDENTIALS_POLICY = DOT_CLASS + "loadCredentialsPolicy";
    protected static final String OPERATION_CHECK_PASSWORD = DOT_CLASS + "checkPassword";
    private static final String OPERATION_LOAD_PASSWORD_VALUE_POLICY = DOT_CLASS + "loadPasswordValuePolicy";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";

   protected String currentPasswordValue = null;
   protected ProtectedStringType newPasswordValue = new ProtectedStringType();
   protected LoadableDetachableModel<CredentialsPolicyType> credentialsPolicyModel;
    protected boolean savedPassword = false;
    protected ProgressDto progress = null;

    public ChangePasswordPanel(String id, IModel<F> objectModel) {
        super(id, objectModel);
    }

    protected void onInitialize() {
        super.onInitialize();
        initCredentialsPolicyModel();
        initLayout();
    }

    private void initCredentialsPolicyModel() {
        credentialsPolicyModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected CredentialsPolicyType load() {
                Task task = getParentPage().createSimpleTask(OPERATION_LOAD_CREDENTIALS_POLICY);
                return WebComponentUtil.getPasswordCredentialsPolicy(getModelObject().asPrismObject(), getParentPage(), task);
            }
        };
    }

    private void initLayout() {
        IModel<String> currentPasswordModel = new IModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return currentPasswordValue;
            }

            @Override
            public void setObject(String value) {
                currentPasswordValue = value;
            }
        };
        PasswordTextField currentPasswordField =
                new PasswordTextField(ID_CURRENT_PASSWORD_FIELD, currentPasswordModel);
        currentPasswordField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        currentPasswordField.add(new VisibleEnableBehaviour() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return shouldCheckOldPassword();
            }

            @Override
            public boolean isEnabled() {
                return !savedPassword;
            }
        });
        currentPasswordField.setRequired(false);
        currentPasswordField.setResetPassword(false);
        currentPasswordField.setOutputMarkupId(true);
        add(currentPasswordField);

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        LoadableDetachableModel<List<StringLimitationResult>> limitationsModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;
            @Override
            protected List<StringLimitationResult> load() {
                return getLimitationsForActualPassword(newPasswordValue);
            }
        };

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, Model.of(newPasswordValue), false, true, getModelObject().asPrismObject()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updatePasswordValidation(AjaxRequestTarget target) {
                limitationsModel.detach();
                updateNewPasswordValuePerformed(target);
            }

            @Override
            protected boolean isPasswordLimitationPopupVisible() {
                return ChangePasswordPanel.this.isPasswordLimitationPopupVisible();
            }

            @Override
            protected boolean canEditPassword() {
                return !savedPassword;
            }

            @Override
            protected boolean removePasswordValueAttribute() {
                return ChangePasswordPanel.this.removePasswordValueAttribute();
            }
        };
        passwordPanel.getBaseFormComponent().add(new AttributeModifier("autofocus", ""));
        add(passwordPanel);

        PasswordLimitationsPanel passwordLimitationsPanel = createLimitationPanel(ID_PASSWORD_VALIDATION_PANEL, limitationsModel);
        passwordLimitationsPanel.add(new VisibleBehaviour(() -> !isPasswordLimitationPopupVisible()));
        passwordLimitationsPanel.setOutputMarkupId(true);
        add(passwordLimitationsPanel);

        PasswordHintPanel hint = new PasswordHintPanel(ID_PASSWORD_HINT_PANEL,
                new PropertyModel<>(getModel(), FocusType.F_CREDENTIALS + "." + CredentialsType.F_PASSWORD + "." + PasswordType.F_HINT),
                Model.of(newPasswordValue), false) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean hideHintValue() {
                return true;
            }
        };
        hint.setOutputMarkupId(true);
        hint.add(new EnableBehaviour(() -> !savedPassword));
        hint.add(new VisibleBehaviour(this::isHintPanelVisible));
        add(hint);

        AjaxSubmitButton changePasswordButton = new AjaxSubmitButton(ID_CHANGE_PASSWORD) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onError(AjaxRequestTarget target) {
                List<FeedbackMessage> feedbackMessages = passwordPanel.collectPasswordFieldsFeedbackMessages();
                feedbackMessages.addAll(hint.collectHintFeedbackMessages());
                if (CollectionUtils.isNotEmpty(feedbackMessages)) {
                    StringBuilder sb = new StringBuilder();
                    feedbackMessages.forEach(m -> sb.append(m.getMessage()).append("\n"));
                    new Toast()
                            .error()
                            .autohide(false)
                            .title(getString("ChangePasswordPanel.savePassword"))
                            .body(sb.toString())
                            .show(target);
                }
            }

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                changePasswordPerformed(target);
            }
        };
        changePasswordButton.add(new VisibleBehaviour(() -> !savedPassword));
        changePasswordButton.add(AttributeAppender.append("class", getChangePasswordButtonStyle()));
        changePasswordButton.setOutputMarkupId(true);
        add(changePasswordButton);

    }

    protected boolean isHintPanelVisible() {
        return true;
    }

    protected PasswordLimitationsPanel createLimitationPanel(String id,
            LoadableDetachableModel<List<StringLimitationResult>> limitationsModel) {
        return new PasswordLimitationsPanel(id, limitationsModel);
    }

    protected String getChangePasswordButtonStyle() {
        return CHANGE_PASSWORD_BUTTON_STYLE;
    }

    protected void updateNewPasswordValuePerformed(AjaxRequestTarget target) {
        target.add(get(ID_PASSWORD_VALIDATION_PANEL));
    }

    private List<StringLimitationResult> getLimitationsForActualPassword(ProtectedStringType passwordValue) {
        ValuePolicyType valuePolicy = WebComponentUtil.getPasswordValuePolicy(credentialsPolicyModel.getObject(),
                OPERATION_LOAD_PASSWORD_VALUE_POLICY, getParentPage());
        if (valuePolicy != null) {
            Task task = getParentPage().createAnonymousTask(OPERATION_VALIDATE_PASSWORD);
            try {
                return getParentPage().getModelInteractionService().validateValue(passwordValue == null ? new ProtectedStringType() : passwordValue,
                        valuePolicy, getModelObject().asPrismObject(), task, task.getResult());
            } catch (Exception e) {
                LOGGER.error("Couldn't validate password security policy", e);
            }
        }
        return new ArrayList<>();
    }

    protected boolean shouldCheckOldPassword() {
        return (getPasswordChangeSecurity() == null) ||
                (getPasswordChangeSecurity().equals(PasswordChangeSecurityType.OLD_PASSWORD) ||
                        (getPasswordChangeSecurity().equals(PasswordChangeSecurityType.OLD_PASSWORD_IF_EXISTS) &&
                                getModelObject().asPrismObject()
                                .findProperty(ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)) != null));
    }

    private PasswordChangeSecurityType getPasswordChangeSecurity() {
        CredentialsPolicyType credentialsPolicy = credentialsPolicyModel.getObject();
        return credentialsPolicy != null && credentialsPolicy.getPassword() != null ?
                credentialsPolicy.getPassword().getPasswordChangeSecurity() : null;
    }


    private void changePasswordPerformed(AjaxRequestTarget target) {
        ProtectedStringType currentPassword;
        if (shouldCheckOldPassword()) {
            LOGGER.debug("Check old password");
            if (currentPasswordValue == null || currentPasswordValue.trim().isEmpty()) {
                new Toast()
                        .warning()
                        .autohide(false)
                        .title(getString("ChangePasswordPanel.savePassword"))
                        .body(getString("PageSelfCredentials.specifyOldPasswordMessage"))
                        .show(target);
                return;
            } else {
                OperationResult checkPasswordResult = new OperationResult(OPERATION_CHECK_PASSWORD);
                Task checkPasswordTask = getParentPage().createSimpleTask(OPERATION_CHECK_PASSWORD);
                try {
                    currentPassword = new ProtectedStringType();
                    currentPassword.setClearValue(currentPasswordValue);
                    boolean isCorrectPassword = getParentPage().getModelInteractionService().checkPassword(getModelObject().getOid(), currentPassword,
                            checkPasswordTask, checkPasswordResult);
                    if (!isCorrectPassword) {
                        new Toast()
                                .error()
                                .autohide(false)
                                .title(getString("ChangePasswordPanel.savePassword"))
                                .body(getString("PageSelfCredentials.incorrectOldPassword"))
                                .show(target);
                        return;
                    }
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check password", ex);
                    checkPasswordResult.recordFatalError(
                            getString("PageAbstractSelfCredentials.message.onSavePerformed.fatalError", ex.getMessage()), ex);
                    new Toast()
                            .error()
                            .autohide(false)
                            .title(getString("ChangePasswordPanel.savePassword"))
                            .body(getString("PageAbstractSelfCredentials.message.onSavePerformed.fatalError"))
                            .show(target);
                    return;
                } finally {
                    checkPasswordResult.computeStatus();
                }
            }
        }

        if (newPasswordValue == null || (!newPasswordValue.isEncrypted() && StringUtils.isEmpty(newPasswordValue.getClearValue()))) {
            new Toast()
                    .warning()
                    .autohide(false)
                    .title(getString("ChangePasswordPanel.savePassword"))
                    .body(getString("PageSelfCredentials.emptyPasswordFiled"))
                    .show(target);
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_PASSWORD);
        ProgressReporter reporter = new ProgressReporter(MidPointApplication.get());
        reporter.getProgress().clear();
        reporter.setWriteOpResultForProgressActivity(true);

        reporter.recordExecutionStart();
        boolean showFeedback = true;
        try {
            if (!newPasswordValue.isEncrypted()) {
                WebComponentUtil.encryptProtectedString(newPasswordValue, true, getParentPage().getMidpointApplication());
            }
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            ItemPath valuePath = ItemPath.create(SchemaConstantsGenerated.C_CREDENTIALS,
                    CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
            collectDeltas(deltas, newPasswordValue, valuePath);
            getParentPage().getModelService().executeChanges(
                    deltas, null, getParentPage().createSimpleTask(OPERATION_SAVE_PASSWORD, SchemaConstants.CHANNEL_SELF_SERVICE_URI),
                    Collections.singleton(reporter), result);
            result.computeStatus();
        } catch (Exception ex) {
            setNullEncryptedPasswordData();
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save password changes", ex);
            result.recordFatalError(getString("PageAbstractSelfCredentials.save.password.failed", ex.getMessage()), ex);
        } finally {
            reporter.recordExecutionStop();
            progress = reporter.getProgress();
            result.computeStatusIfUnknown();

            if (!result.isError()) {
                this.savedPassword = true;
                target.add(ChangePasswordPanel.this);
            }
        }

        finishChangePassword(result, target, showFeedback);
    }

    protected void collectDeltas(Collection<ObjectDelta<? extends ObjectType>> deltas, ProtectedStringType currentPassword, ItemPath valuePath) {
        SchemaRegistry registry = getPrismContext().getSchemaRegistry();

        PrismObjectDefinition<UserType> objDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
        List<PropertyDelta<?>> modifications = new ArrayList<>();

        PropertyDelta<ProtectedStringType> passwordDelta = getPrismContext().deltaFactory().property()
                .createModificationReplaceProperty(valuePath, objDef, newPasswordValue);
        modifications.add(passwordDelta);
        if (currentPassword != null) {
            passwordDelta.addEstimatedOldValue(getPrismContext().itemFactory().createPropertyValue(currentPassword));
        }

        String newHintValue = getHintValue();
        if (StringUtils.isNotEmpty(newHintValue)) {
            ItemPath hintPath = ItemPath.create(SchemaConstantsGenerated.C_CREDENTIALS,
                    CredentialsType.F_PASSWORD, PasswordType.F_HINT);
            PropertyDelta<String> hintDelta = getPrismContext().deltaFactory().property()
                    .createModificationReplaceProperty(hintPath, objDef, newHintValue);

            modifications.add(hintDelta);
        }

        deltas.add(getPrismContext().deltaFactory().object().createModifyDelta(getModelObject().getOid(), modifications, UserType.class));
    }

    private String getHintValue() {
        return getModelObject().getCredentials() != null && getModelObject().getCredentials().getPassword() != null ?
                getModelObject().getCredentials().getPassword().getHint() : null;
    }

    protected void setNullEncryptedPasswordData() {
        if (newPasswordValue != null) {
            newPasswordValue.setEncryptedData(null);
        }
    }

    protected void finishChangePassword(OperationResult result, AjaxRequestTarget target, boolean showFeedback) {
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            setNullEncryptedPasswordData();
            if (showFeedback) {
                new Toast()
                        .warning()
                        .autohide(false)
                        .title(getString("ChangePasswordPanel.savePassword"))
                        .body(getString(result.getMessage()))
                        .show(target);
            }
        } else {
            new Toast()
                    .success()
                    .autohide(false)
                    .title(getString("ChangePasswordPanel.savePassword"))
                    .body(getString(result.getStatus()))
                    .show(target);
        }
    }

    protected boolean isPasswordLimitationPopupVisible() {
        return false;
    }

    @NotNull
    protected PasswordHintConfigurabilityType getPasswordHintConfigurability() {
        CredentialsPolicyType credentialsPolicy = credentialsPolicyModel.getObject();

        if (credentialsPolicy != null
                && credentialsPolicy.getPassword() != null
                && credentialsPolicy.getPassword().getPasswordHintConfigurability() != null) {
            return credentialsPolicy.getPassword().getPasswordHintConfigurability();
        }
        return PasswordHintConfigurabilityType.ALWAYS_CONFIGURE;
    }

    protected boolean removePasswordValueAttribute() {
        return true;
    }
}
