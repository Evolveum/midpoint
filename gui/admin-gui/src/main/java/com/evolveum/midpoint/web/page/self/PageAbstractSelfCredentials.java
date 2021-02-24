/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;

import com.evolveum.midpoint.web.page.admin.home.dto.MyCredentialsDto;
import com.evolveum.midpoint.web.page.self.component.SecurityQuestionsPanel;
import com.evolveum.midpoint.web.security.MidPointApplication;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.self.component.ChangePasswordPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.model.PropertyModel;

/**
 * @author Viliam Repan (lazyman)
 */

public abstract class PageAbstractSelfCredentials extends PageSelf {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractSelfCredentials.class);

    protected static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";

    private static final String DOT_CLASS = PageSelfCredentials.class.getName() + ".";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";
    private static final String OPERATION_CHECK_PASSWORD = DOT_CLASS + "checkPassword";
    private static final String OPERATION_SAVE_QUESTIONS = DOT_CLASS + "saveSecurityQuestions";

    private final IModel<MyCredentialsDto> model;

    public PageAbstractSelfCredentials() {
        model = new LoadableModel<MyCredentialsDto>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MyCredentialsDto load() {
                return new MyCredentialsDto();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model<>("fa fa-shield"));
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);

        List<ITab> tabs = new ArrayList<>();
        tabs.addAll(createDefaultTabs());
        tabs.addAll(createSpecificTabs());

        TabbedPanel<ITab> credentialsTabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, this, tabs, null);
        credentialsTabPanel.setOutputMarkupId(true);

        mainForm.add(credentialsTabPanel);
        initButtons(mainForm);

        add(mainForm);

    }

    protected Collection<? extends ITab> createSpecificTabs(){
        return new ArrayList<>();
    };

    private Collection<? extends ITab> createDefaultTabs(){
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageSelfCredentials.tabs.password")) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ChangePasswordPanel(panelId, new PropertyModel<MyPasswordsDto>(model, MyCredentialsDto.F_MY_PASSOWRDS_DTO)) {
                    @Override
                    protected boolean shouldShowPasswordPropagation() {
                        return shouldLoadAccounts();
                    }

                    @Override
                    protected boolean isCheckOldPassword() {
                        return PageAbstractSelfCredentials.this.isCheckOldPassword();
                    }
                };
            }
        });
        return tabs;
    }

    private void initButtons(Form<?> mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSavePerformed(target);
            }
        };
        mainForm.setDefaultButton(save);
        mainForm.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_CANCEL_BUTTON, createStringResource("PageBase.button.back")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                onCancelPerformed();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onCancelPerformed();
            }
        };
        mainForm.add(cancel);
    }

    protected void onSavePerformed(AjaxRequestTarget target) {
        Component actualTab = getActualTabPanel();
        if (actualTab instanceof ChangePasswordPanel) {
            ProtectedStringType oldPassword = null;
            if (isCheckOldPassword()) {
                LOGGER.debug("Check old password");
                MyPasswordsDto modelObject = getPasswordDto();
                if (modelObject.getOldPassword() == null
                        || modelObject.getOldPassword().trim().equals("")) {
                    warn(getString("PageSelfCredentials.specifyOldPasswordMessage"));
                    target.add(getFeedbackPanel());
                    return;
                } else {
                    OperationResult checkPasswordResult = new OperationResult(OPERATION_CHECK_PASSWORD);
                    Task checkPasswordTask = createSimpleTask(OPERATION_CHECK_PASSWORD);
                    try {
                        oldPassword = new ProtectedStringType();
                        oldPassword.setClearValue(modelObject.getOldPassword());
                        boolean isCorrectPassword = getModelInteractionService().checkPassword(modelObject.getFocusOid(), oldPassword,
                                checkPasswordTask, checkPasswordResult);
                        if (!isCorrectPassword) {
                            error(getString("PageSelfCredentials.incorrectOldPassword"));
                            target.add(getFeedbackPanel());
                            return;
                        }
                    } catch (Exception ex) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check password", ex);
                        checkPasswordResult.recordFatalError(
                                getString("PageAbstractSelfCredentials.message.onSavePerformed.fatalError", ex.getMessage()), ex);
                        target.add(getFeedbackPanel());
                        return;
                    } finally {
                        checkPasswordResult.computeStatus();
                    }
                }
            }

            if (getPasswordDto().getPassword() == null) {
                warn(getString("PageSelfCredentials.emptyPasswordFiled"));
                target.add(getFeedbackPanel());
                return;
            }

            List<PasswordAccountDto> selectedAccounts = getSelectedAccountsList();
            if (selectedAccounts.isEmpty()) {
                warn(getString("PageSelfCredentials.noAccountSelected"));
                target.add(getFeedbackPanel());
                return;
            }

            OperationResult result = new OperationResult(OPERATION_SAVE_PASSWORD);
            ProgressReporter reporter = new ProgressReporter(MidPointApplication.get());
            reporter.getProgress().clear();
            reporter.setWriteOpResultForProgressActivity(true);

            reporter.recordExecutionStart();
            boolean showFeedback = true;
            try {
                MyPasswordsDto dto = getPasswordDto();
                ProtectedStringType password = dto.getPassword();
                if (!password.isEncrypted()) {
                    WebComponentUtil.encryptProtectedString(password, true, getMidpointApplication());
                }
                final ItemPath valuePath = ItemPath.create(SchemaConstantsGenerated.C_CREDENTIALS,
                        CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
                SchemaRegistry registry = getPrismContext().getSchemaRegistry();
                Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

                for (PasswordAccountDto accDto : selectedAccounts) {
                    PrismObjectDefinition objDef = accDto.isMidpoint() ?
                            registry.findObjectDefinitionByCompileTimeClass(UserType.class) :
                            registry.findObjectDefinitionByCompileTimeClass(ShadowType.class);

                    PropertyDelta<ProtectedStringType> delta = getPrismContext().deltaFactory().property()
                            .createModificationReplaceProperty(valuePath, objDef, password);
                    if (oldPassword != null) {
                        delta.addEstimatedOldValue(getPrismContext().itemFactory().createPropertyValue(oldPassword));
                    }

                    Class<? extends ObjectType> type = accDto.isMidpoint() ? UserType.class : ShadowType.class;

                    deltas.add(getPrismContext().deltaFactory().object().createModifyDelta(accDto.getOid(), delta, type
                    ));
                }
                getModelService().executeChanges(
                        deltas, null, createSimpleTask(OPERATION_SAVE_PASSWORD, SchemaConstants.CHANNEL_SELF_SERVICE_URI), Collections.singleton(reporter), result);
                result.computeStatus();
            } catch (Exception ex) {
                setNullEncryptedPasswordData();
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save password changes", ex);
                result.recordFatalError(getString("PageAbstractSelfCredentials.save.password.failed", ex.getMessage()), ex);
            } finally {
                reporter.recordExecutionStop();
                getPasswordDto().setProgress(reporter.getProgress());
                if (getActualTabPanel() != null) {
                    ((ChangePasswordPanel)getActualTabPanel()).updateResultColumnOfTable(target);
                }
                result.computeStatusIfUnknown();
                if (shouldLoadAccounts()) {
                    showFeedback = false;
                    if (result.isError()) {
                        error(createStringResource("PageAbstractSelfCredentials.message.resultInTable.error").getString());
                    } else {
                        info(createStringResource("PageAbstractSelfCredentials.message.resultInTable").getString());
                    }
                }
            }

            finishChangePassword(result, target, showFeedback);
        } else if (actualTab instanceof SecurityQuestionsPanel) {
            ((SecurityQuestionsPanel) actualTab).onSavePerformed(target);
        }

    }

    protected Component getActualTabPanel(){
        return get(createComponentPath(ID_MAIN_FORM, ID_TAB_PANEL, TabbedPanel.TAB_PANEL_ID));
    }

    protected void setNullEncryptedPasswordData() {
        MyPasswordsDto dto = getPasswordDto();
        ProtectedStringType password = dto.getPassword();
        if (password != null) {
            password.setEncryptedData(null);
        }
    }

    protected abstract boolean isCheckOldPassword();

    protected abstract void finishChangePassword(OperationResult result, AjaxRequestTarget target, boolean showFeedback);

    private void onCancelPerformed() {
        redirectBack();
    }

    protected MyPasswordsDto getPasswordDto() {
        return model.getObject() == null ? null : model.getObject().getMyPasswordsDto();
    }

    protected IModel<MyCredentialsDto> getModel() {
        return model;
    }

    protected boolean shouldLoadAccounts() {
        return getPasswordDto().getPropagation() == null || CredentialsPropagationUserControlType.USER_CHOICE.equals(getPasswordDto().getPropagation())
                || CredentialsPropagationUserControlType.ONLY_MAPPING.equals(getPasswordDto().getPropagation())
                || CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(getPasswordDto().getPropagation());
    }

    protected List<PasswordAccountDto> getSelectedAccountsList() {
        List<PasswordAccountDto> passwordAccountDtos = getPasswordDto().getAccounts();
        List<PasswordAccountDto> selectedAccountList = new ArrayList<>();
        if (getPasswordDto().getPropagation() != null
                && getPasswordDto().getPropagation().equals(CredentialsPropagationUserControlType.MAPPING)) {
            selectedAccountList.addAll(passwordAccountDtos);
        } else {
            boolean midpointAccountSelected = false;
            List<PasswordAccountDto> selectedWithOutbound = new ArrayList<>();
            for (PasswordAccountDto passwordAccountDto : passwordAccountDtos) {
                if (passwordAccountDto.isMidpoint()){
                    midpointAccountSelected = passwordAccountDto.isSelected();
                }
                if (passwordAccountDto.isSelected()) {
                    if(!passwordAccountDto.isPasswordOutbound()) {
                        selectedAccountList.add(passwordAccountDto);
                    } else {
                        selectedWithOutbound.add(passwordAccountDto);
                    }
                }
            }
            if (!midpointAccountSelected) {
                selectedAccountList.addAll(selectedWithOutbound);
            }
        }
        return selectedAccountList;
    }
}
