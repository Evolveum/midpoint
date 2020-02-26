/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.self.component.ChangePasswordPanel;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Viliam Repan (lazyman)
 */

public abstract class PageAbstractSelfCredentials extends PageSelf {
    private static final long serialVersionUID = 1L;

    protected static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractSelfCredentials.class);
    private static final String DOT_CLASS = PageSelfCredentials.class.getName() + ".";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";
    private static final String OPERATION_CHECK_PASSWORD = DOT_CLASS + "checkPassword";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";


    private LoadableModel<MyPasswordsDto> model;
    private PrismObject<? extends FocusType> focus;

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

    public PageAbstractSelfCredentials(final MyPasswordsDto myPasswordsDto) {
        model = new LoadableModel<MyPasswordsDto>(myPasswordsDto, false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MyPasswordsDto load() {
                return myPasswordsDto;
            }
        };

        initLayout();
    }

    private MyPasswordsDto loadPageModel() {
        LOGGER.debug("Loading user and accounts.");
        MyPasswordsDto dto = new MyPasswordsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER_WITH_ACCOUNTS);
        try {
            String focusOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);
            focus = getModelService().getObject(FocusType.class, focusOid, null, task, subResult);
            subResult.recordSuccessIfUnknown();

            dto.getAccounts().add(createDefaultPasswordAccountDto(focus));

            CredentialsPolicyType credentialsPolicyType = getPasswordCredentialsPolicy();
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

                }

            }

            if (dto.getPropagation() == null || dto.getPropagation().equals(CredentialsPropagationUserControlType.USER_CHOICE)) {
                PrismReference reference = focus.findReference(FocusType.F_LINK_REF);
                if (reference == null || reference.getValues() == null) {
                    LOGGER.debug("No accounts found for user {}.", new Object[]{focusOid});
                    return dto;
                }

                final Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                        .item(ShadowType.F_RESOURCE_REF).resolve()
                        .build();
                List<PrismReferenceValue> values = reference.getValues();
                for (PrismReferenceValue value : values) {
                    subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
                    try {
                        String accountOid = value.getOid();
                        task = createSimpleTask(OPERATION_LOAD_ACCOUNT);

                        PrismObject<ShadowType> account = getModelService().getObject(ShadowType.class,
                                accountOid, options, task, subResult);


                        dto.getAccounts().add(createPasswordAccountDto(account, task, subResult));
                        subResult.recordSuccessIfUnknown();
                    } catch (Exception ex) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load account", ex);
                        subResult.recordFatalError(getString("PageAbstractSelfCredentials.message.couldntLoadAccount.fatalError"), ex);
                    }
                }
            }
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


    private void initLayout() {
        Form<?> mainForm = new com.evolveum.midpoint.web.component.form.Form<>(ID_MAIN_FORM);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageSelfCredentials.tabs.password")) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ChangePasswordPanel(panelId, isCheckOldPassword(), model, model.getObject());
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
                onCancelPerformed(target);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onCancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<? extends FocusType> focus) {
        String customSystemName = WebComponentUtil.getMidpointCustomSystemName(PageAbstractSelfCredentials.this, "midpoint.default.system.name");
        return new PasswordAccountDto(focus.getOid(), focus.getName().getOrig(),
                getString("PageSelfCredentials.resourceMidpoint", customSystemName),
                WebComponentUtil.isActivationEnabled(focus), true);
    }

    private PasswordAccountDto createPasswordAccountDto(PrismObject<ShadowType> account, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
        String resourceName;
        if (resourceRef == null || resourceRef.getValue() == null || resourceRef.getValue().getObject() == null) {
            resourceName = getString("PageSelfCredentials.couldntResolve");
        } else {
            resourceName = WebComponentUtil.getName(resourceRef.getValue().getObject());
        }

        PasswordAccountDto passwordAccountDto = new PasswordAccountDto(account.getOid(), WebComponentUtil.getName(account),
                resourceName, WebComponentUtil.isActivationEnabled(account));

        passwordAccountDto.setPasswordOutbound(getPasswordOutbound(account, task, result));
        passwordAccountDto.setPasswordCapabilityEnabled(hasPasswordCapability(account));
        return passwordAccountDto;
    }

    protected void onSavePerformed(AjaxRequestTarget target) {
        List<PasswordAccountDto> selectedAccounts = getSelectedAccountsList();

        ProtectedStringType oldPassword = null;
        if (isCheckOldPassword()) {
            LOGGER.debug("Check old password");
            if (model.getObject().getOldPassword() == null
                    || model.getObject().getOldPassword().trim().equals("")){
                warn(getString("PageSelfCredentials.specifyOldPasswordMessage"));
                target.add(getFeedbackPanel());
                return;
            } else {
                OperationResult checkPasswordResult = new OperationResult(OPERATION_CHECK_PASSWORD);
                Task checkPasswordTask = createSimpleTask(OPERATION_CHECK_PASSWORD);
                try {
                    oldPassword = new ProtectedStringType();
                    oldPassword.setClearValue(model.getObject().getOldPassword());
                    boolean isCorrectPassword = getModelInteractionService().checkPassword(focus.getOid(), oldPassword,
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
        if (selectedAccounts.isEmpty()) {
            warn(getString("PageSelfCredentials.noAccountSelected"));
            target.add(getFeedbackPanel());
            return;
        }
        if (getModelObject().getPassword() == null ) {
            warn(getString("PageSelfCredentials.emptyPasswordFiled"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_PASSWORD);
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
            getModelService().executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE_PASSWORD, SchemaConstants.CHANNEL_GUI_SELF_SERVICE_URI), result);

            result.computeStatus();
        } catch (Exception ex) {
            setEncryptedPasswordData(null);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save password changes", ex);
            result.recordFatalError(getString("PageAbstractSelfCredentials.save.password.failed", ex.getMessage()), ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        finishChangePassword(result, target);
    }

    protected void setEncryptedPasswordData(EncryptedDataType data) {
        MyPasswordsDto dto = model.getObject();
        ProtectedStringType password = dto.getPassword();
        if (password != null){
            password.setEncryptedData(data);
        }
    }

    protected abstract boolean isCheckOldPassword();

    protected abstract void finishChangePassword(OperationResult result, AjaxRequestTarget target);

    private List<PasswordAccountDto> getSelectedAccountsList(){
        List<PasswordAccountDto> passwordAccountDtos = model.getObject().getAccounts();
        List<PasswordAccountDto> selectedAccountList = new ArrayList<>();
        if (model.getObject().getPropagation() != null
                && model.getObject().getPropagation().equals(CredentialsPropagationUserControlType.MAPPING)){
            selectedAccountList.addAll(passwordAccountDtos);
        } else {
            for (PasswordAccountDto passwordAccountDto : passwordAccountDtos) {
                if (passwordAccountDto.getCssClass().equals(ChangePasswordPanel.SELECTED_ACCOUNT_ICON_CSS)) {
                    selectedAccountList.add(passwordAccountDto);
                }
            }
        }
        return selectedAccountList;
    }
    private void onCancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    private boolean getPasswordOutbound(PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        try {
            RefinedObjectClassDefinition rOCDef = getModelInteractionService().getEditObjectClassDefinition(shadow,
                    shadow.asObjectable().getResourceRef().asReferenceValue().getObject(),
                    AuthorizationPhaseType.REQUEST, task, result);

            if (rOCDef != null && !CollectionUtils.isEmpty(rOCDef.getPasswordOutbound())){
                return true;
            }
        } catch (SchemaException ex) {

        }
        return false;
    }


    private boolean hasPasswordCapability(PrismObject<ShadowType> shadow) {

        ShadowType shadowType = shadow.asObjectable();
        ResourceType resource = (ResourceType) shadowType.getResourceRef().asReferenceValue().getObject().asObjectable();
        ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType = ResourceTypeUtil.findObjectTypeDefinition(resource.asPrismObject(), shadowType.getKind(), shadowType.getIntent());

         return ResourceTypeUtil.isPasswordCapabilityEnabled(resource, resourceObjectTypeDefinitionType);

    }

    public PrismObject<? extends FocusType> getFocus() {
        return focus;
    }

    private CredentialsPolicyType getPasswordCredentialsPolicy (){
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

    protected MyPasswordsDto getModelObject() {
        return model.getObject();
    }
}
