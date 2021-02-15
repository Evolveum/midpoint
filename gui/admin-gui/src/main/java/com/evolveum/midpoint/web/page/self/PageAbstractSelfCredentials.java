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

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;

import com.evolveum.midpoint.web.security.MidPointApplication;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
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
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";
    private static final String OPERATION_CHECK_PASSWORD = DOT_CLASS + "checkPassword";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";

    private final LoadableModel<MyPasswordsDto> model;

    public PageAbstractSelfCredentials() {
        model = new LoadableModel<MyPasswordsDto>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MyPasswordsDto load() {
                return loadPageModel();
            }
        };

        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model<>("fa fa-shield"));
    }

    private MyPasswordsDto loadPageModel() {
        LOGGER.debug("Loading user and accounts.");

        MyPasswordsDto dto = new MyPasswordsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER_WITH_ACCOUNTS);
        try {
            String focusOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);
            PrismObject<? extends FocusType> focus = getModelService().getObject(FocusType.class, focusOid, null, task, subResult);
            dto = createMyPasswordsDto(focus);
            subResult.recordSuccessIfUnknown();

            if (!shouldLoadAccounts(dto)) {
                LOGGER.debug("Skip loading account, because policy said so (enabled {} propagation).", dto.getPropagation());
                return dto;
            }

            PrismReference reference = focus.findReference(FocusType.F_LINK_REF);
            if (reference == null || CollectionUtils.isEmpty(reference.getValues())) {
                LOGGER.debug("No accounts found for user {}.", focusOid);
                return dto;
            }

            addAccountsToMyPasswordsDto(dto, reference.getValues(), task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load accounts", ex);
            result.recordFatalError(getString("PageAbstractSelfCredentials.message.couldntLoadAccounts.fatalError"), ex);
        } finally {
            result.recomputeStatus();
        }

        Collections.sort(dto.getAccounts());

        if (!result.isSuccess() && !result.isHandledError()) {
            showResult(result);
        }

        return dto;
    }

    private MyPasswordsDto createMyPasswordsDto(PrismObject<? extends FocusType> focus) {
        MyPasswordsDto dto = new MyPasswordsDto();
        dto.setFocus(focus);
        CredentialsPolicyType credentialsPolicyType = getPasswordCredentialsPolicy(focus);
        dto.getAccounts().add(createDefaultPasswordAccountDto(focus, getPasswordPolicyOid(credentialsPolicyType)));


        if (credentialsPolicyType != null) {
            PasswordCredentialsPolicyType passwordCredentialsPolicy = credentialsPolicyType.getPassword();
            if (passwordCredentialsPolicy != null) {
                CredentialsPropagationUserControlType propagationUserControl = passwordCredentialsPolicy.getPropagationUserControl();
                if (propagationUserControl != null) {
                    dto.setPropagation(propagationUserControl);
                }
                PasswordChangeSecurityType passwordChangeSecurity = passwordCredentialsPolicy.getPasswordChangeSecurity();
                if (passwordChangeSecurity != null) {
                    dto.setPasswordChangeSecurity(passwordChangeSecurity);
                }
                ObjectReferenceType valuePolicyRef = passwordCredentialsPolicy.getValuePolicyRef();
                if (valuePolicyRef != null && valuePolicyRef.getOid() != null){
                    Task task = createSimpleTask("load value policy");
                    PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                            valuePolicyRef, this, task, task.getResult());
                    if (valuePolicy != null) {
                        dto.addPasswordPolicy(valuePolicy.asObjectable());
                    }
                }

            }
        }
        return dto;
    }

    private String getPasswordPolicyOid(CredentialsPolicyType credentialsPolicyType) {
        if (credentialsPolicyType != null && credentialsPolicyType.getPassword() != null
                && credentialsPolicyType.getPassword().getValuePolicyRef() != null) {
            return credentialsPolicyType.getPassword().getValuePolicyRef().getOid();
        }
        return null;
    }

    protected boolean shouldLoadAccounts(MyPasswordsDto dto) {
        return dto.getPropagation() == null || CredentialsPropagationUserControlType.USER_CHOICE.equals(dto.getPropagation())
                || CredentialsPropagationUserControlType.ONLY_MAPPING.equals(dto.getPropagation())
                || CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(dto.getPropagation());
    }

    private void addAccountsToMyPasswordsDto(MyPasswordsDto dto, List<PrismReferenceValue> linkReferences, Task task, OperationResult result) {
        final Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(ShadowType.F_RESOURCE_REF).resolve()
                .item(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_SECURITY_POLICY_REF)).resolve()
                .build();
        for (PrismReferenceValue value : linkReferences) {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
            try {
                String accountOid = value.getOid();
                PrismObject<ShadowType> account = getModelService().getObject(ShadowType.class,
                        accountOid, options, task, subResult);

                dto.getAccounts().add(createPasswordAccountDto(dto, account, task, subResult));
                subResult.recordSuccessIfUnknown();
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load account", ex);
                subResult.recordFatalError(getString("PageAbstractSelfCredentials.message.couldntLoadAccount.fatalError"), ex);
            }
        }
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageSelfCredentials.tabs.password")) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ChangePasswordPanel(panelId, isCheckOldPassword(), model, model.getObject()) {
                    @Override
                    protected boolean shouldShowPasswordPropagation() {
                        return shouldLoadAccounts(getModelObject());
                    }
                };
            }
        });

        TabbedPanel<ITab> credentialsTabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, this, tabs, null);
        credentialsTabPanel.setOutputMarkupId(true);

        mainForm.add(credentialsTabPanel);
        initButtons(mainForm);

        add(mainForm);

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

    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<? extends FocusType> focus, String passwordPolicyOid) {
        String customSystemName = WebComponentUtil.getMidpointCustomSystemName(PageAbstractSelfCredentials.this, "midpoint.default.system.name");
        PasswordAccountDto accountDto = new PasswordAccountDto(focus, focus.getName().getOrig(),
                getString("PageSelfCredentials.resourceMidpoint", customSystemName),
                WebComponentUtil.isActivationEnabled(focus, ActivationType.F_EFFECTIVE_STATUS), true);
        accountDto.setPasswordValuePolicyOid(passwordPolicyOid);
        return accountDto;
    }

    private PasswordAccountDto createPasswordAccountDto(MyPasswordsDto passwordDto, PrismObject<ShadowType> account, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
        String resourceName;
        if (resourceRef == null || resourceRef.getValue() == null || resourceRef.getValue().getObject() == null) {
            resourceName = getString("PageSelfCredentials.couldntResolve");
        } else {
            resourceName = WebComponentUtil.getName(resourceRef.getValue().getObject());
        }

        PasswordAccountDto passwordAccountDto = new PasswordAccountDto(account, resourceName, resourceRef.getOid());

        ShadowType shadowType = account.asObjectable();
        ResourceType resource = (ResourceType) shadowType.getResourceRef().asReferenceValue().getObject().asObjectable();
        if (resource != null) {
            ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType = ResourceTypeUtil.findObjectTypeDefinition(resource.asPrismObject(), shadowType.getKind(), shadowType.getIntent());
            passwordAccountDto.setPasswordCapabilityEnabled(ResourceTypeUtil.isPasswordCapabilityEnabled(resource, resourceObjectTypeDefinitionType));
            passwordAccountDto.setMaintenanceState(ResourceTypeUtil.isInMaintenance(resource));
            try {
                RefinedObjectClassDefinition rOCDef = getModelInteractionService().getEditObjectClassDefinition(account,
                        resource.asPrismObject(), AuthorizationPhaseType.REQUEST, task, result);

                if (rOCDef != null) {
                    passwordAccountDto.setPasswordOutbound(getPasswordOutbound(account, resource, rOCDef));
                    CredentialsPolicyType credentialsPolicy = getPasswordCredentialsPolicy(rOCDef);
                    if (credentialsPolicy != null && credentialsPolicy.getPassword() != null
                            && credentialsPolicy.getPassword().getValuePolicyRef() != null) {
                        PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                                credentialsPolicy.getPassword().getValuePolicyRef(), this, task, task.getResult());
                        if (valuePolicy != null) {
                            passwordAccountDto.setPasswordValuePolicyOid(valuePolicy.getOid());
                            passwordDto.addPasswordPolicy(valuePolicy.asObjectable());
                        }
                    }
                } else {
                    passwordAccountDto.setPasswordOutbound(false);
                }

            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Fail to get RefinedObjectClassDefinition for {} ", e, account);
                result.recordFatalError("Fail to get RefinedObjectClassDefinition for " + account, e);
                showResult(result);
                passwordAccountDto.setPasswordOutbound(false);
            }

        } else {
            passwordAccountDto.setPasswordCapabilityEnabled(false);
            passwordAccountDto.setPasswordOutbound(false);
        }

        return passwordAccountDto;
    }

    protected void onSavePerformed(AjaxRequestTarget target) {
        ProtectedStringType oldPassword = null;
        if (isCheckOldPassword()) {
            LOGGER.debug("Check old password");
            MyPasswordsDto modelObject = getModelObject();
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

        if (getModelObject().getPassword() == null) {
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
            MyPasswordsDto dto = model.getObject();
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
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = getModelService().executeChanges(
                    deltas, null, createSimpleTask(OPERATION_SAVE_PASSWORD, SchemaConstants.CHANNEL_SELF_SERVICE_URI), Collections.singleton(reporter), result);
            result.computeStatus();
        } catch (Exception ex) {
            setNullEncryptedPasswordData();
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save password changes", ex);
            result.recordFatalError(getString("PageAbstractSelfCredentials.save.password.failed", ex.getMessage()), ex);
        } finally {
            reporter.recordExecutionStop();
            model.getObject().setProgress(reporter.getProgress());
            if (getChangePasswordPanel() != null) {
                getChangePasswordPanel().updateResultColumnOfTable(target);
            }
            result.computeStatusIfUnknown();
            if (shouldLoadAccounts(model.getObject())) {
                showFeedback = false;
                info(createStringResource("PageAbstractSelfCredentials.message.resultInTable").getString());
            }
        }

        finishChangePassword(result, target, showFeedback);
    }

    protected ChangePasswordPanel getChangePasswordPanel(){
        Component component = get(createComponentPath(ID_MAIN_FORM, ID_TAB_PANEL, TabbedPanel.TAB_PANEL_ID));
        if (component instanceof ChangePasswordPanel) {
            return (ChangePasswordPanel) component;
        }
        return null;
    }

    protected void setNullEncryptedPasswordData() {
        MyPasswordsDto dto = model.getObject();
        ProtectedStringType password = dto.getPassword();
        if (password != null) {
            password.setEncryptedData(null);
        }
    }

    protected abstract boolean isCheckOldPassword();

    protected abstract void finishChangePassword(OperationResult result, AjaxRequestTarget target, boolean showFeedback);

    protected List<PasswordAccountDto> getSelectedAccountsList() {
        List<PasswordAccountDto> passwordAccountDtos = model.getObject().getAccounts();
        List<PasswordAccountDto> selectedAccountList = new ArrayList<>();
        if (model.getObject().getPropagation() != null
                && model.getObject().getPropagation().equals(CredentialsPropagationUserControlType.MAPPING)) {
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

    private void onCancelPerformed() {
        redirectBack();
    }

    private boolean getPasswordOutbound(PrismObject<ShadowType> shadow, ResourceType resource, RefinedObjectClassDefinition rOCDef) {

            List<MappingType> passwordOutbound = rOCDef.getPasswordOutbound();
            if (passwordOutbound == null) {
                return false;
            }
            for (MappingType mapping : passwordOutbound) {
                if (MappingStrengthType.WEAK == mapping.getStrength()) {
                    CredentialsCapabilityType capability = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
                    if (CapabilityUtil.isPasswordReadable(capability)) {
                        return true;
                    }
                    continue;
                }
                // at least one mapping which is not WEAK
                return true;
            }

        return false;

    }

    private CredentialsPolicyType getPasswordCredentialsPolicy(PrismObject<? extends FocusType> focus) {
        LOGGER.debug("Getting credentials policy");
        Task task = createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY);
        OperationResult result = new OperationResult(OPERATION_GET_CREDENTIALS_POLICY);
        CredentialsPolicyType credentialsPolicyType = null;
        try {
            credentialsPolicyType = getModelInteractionService().getCredentialsPolicy(focus, task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load credentials policy", ex);
            result.recordFatalError(
                    getString("PageAbstractSelfCredentials.message.getPasswordCredentialsPolicy.fatalError", ex.getMessage()), ex);
        } finally {
            result.computeStatus();
        }
        return credentialsPolicyType;
    }

    private CredentialsPolicyType getPasswordCredentialsPolicy(RefinedObjectClassDefinition rOCDef) {
        LOGGER.debug("Getting credentials policy");
        Task task = createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY);
        OperationResult result = new OperationResult(OPERATION_GET_CREDENTIALS_POLICY);
        CredentialsPolicyType credentialsPolicyType = null;
        try {
            SecurityPolicyType securityPolicy = getModelInteractionService().getSecurityPolicy(rOCDef, task, result);
            if (securityPolicy != null){
                credentialsPolicyType = securityPolicy.getCredentials();
            }
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load security policy", ex);
            result.recordFatalError(
                    getString("PageAbstractSelfCredentials.message.getPasswordSecurityPolicy.fatalError", ex.getMessage()), ex);
        } finally {
            result.computeStatus();
        }
        return credentialsPolicyType;
    }

    protected MyPasswordsDto getModelObject() {
        return model.getObject();
    }
}
