/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.component;

import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;

import org.apache.wicket.util.visit.IVisitor;

/**
 * @author Kate Honchar
 */
public class ChangePasswordPanel extends BasePanel<MyPasswordsDto> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordPanel.class);

    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_OLD_PASSWORD_CONTAINER = "oldPasswordContainer";
    private static final String ID_OLD_PASSWORD_FIELD = "oldPassword";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    public static final String ID_ACCOUNTS_TABLE = "accounts";
    public static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";

    private static final String DOT_CLASS = PageSelfCredentials.class.getName() + ".";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";

    private IModel<Boolean> midpointAccountSelected;
    private Map<String, List<StringLimitationResult>> limitationsByPolicyOid = new HashMap<>();

    public ChangePasswordPanel(String id, IModel<MyPasswordsDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPasswordModel();
        initMidpointAccountSelected();
        initLayout();
    }

    private void initPasswordModel() {
        if (getModelObject() == null) {
            getModel().setObject(loadPageModel());
        }
    }

    private void initMidpointAccountSelected() {
        MyPasswordsDto dto = getModelObject();
        PasswordAccountDto midpointAccount = null;
        for (PasswordAccountDto account : dto.getAccounts()) {
            if (account.isMidpoint()) {
                midpointAccount = account;
            }
        }
        midpointAccountSelected = new PropertyModel<>(midpointAccount, Selectable.F_SELECTED);
    }

    private void initLayout() {
        WebMarkupContainer oldPasswordContainer = new WebMarkupContainer(ID_OLD_PASSWORD_CONTAINER);
        oldPasswordContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return canEditPassword();
            }

            @Override
            public boolean isVisible() {
                return isCheckOldPassword();
            }
        });
        add(oldPasswordContainer);

        PasswordTextField oldPasswordField =
                new PasswordTextField(ID_OLD_PASSWORD_FIELD, new PropertyModel<>(getModel(), MyPasswordsDto.F_OLD_PASSWORD));
        oldPasswordField.setRequired(false);
        oldPasswordField.setResetPassword(false);
        oldPasswordField.setOutputMarkupId(true);
        oldPasswordContainer.add(oldPasswordField);

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new PropertyModel<>(getModel(), MyPasswordsDto.F_PASSWORD),
                getModelObject().getFocus()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected <F extends FocusType> ValuePolicyType getValuePolicy(PrismObject<F> object) {
                return getModelObject().getFocusPolicy();
            }

            @Override
            protected void updatePasswordValidation(AjaxRequestTarget target) {
                super.updatePasswordValidation(target);
                limitationsByPolicyOid.clear();
                getTable().visitChildren(PasswordPolicyValidationPanel.class,
                        (IVisitor<PasswordPolicyValidationPanel, PasswordPolicyValidationPanel>) (panel, iVisit) -> {
                            panel.refreshValidationPopup(target);
                        });
            }

            @Override
            protected boolean canEditPassword() {
                return ChangePasswordPanel.this.canEditPassword();
            }

            @Override
            protected boolean isRemovePasswordVisible() {
                return false;
            }
        };
        passwordPanel.getBaseFormComponent().add(new AttributeModifier("autofocus", ""));
        add(passwordPanel);

        WebMarkupContainer accountContainer = new WebMarkupContainer(ID_ACCOUNTS_CONTAINER);

        List<IColumn<PasswordAccountDto, String>> columns = initColumns();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<>(this,
                new PropertyModel<>(getModel(), MyPasswordsDto.F_ACCOUNTS));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS_TABLE, provider, columns);
        accounts.setItemsPerPage(30);
        accounts.setShowPaging(false);
        accountContainer.add(accounts);

        accountContainer.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return shouldShowPasswordPropagation();
            }
        });

        add(accountContainer);
    }

    protected boolean shouldShowPasswordPropagation() {
        return true;
    }

    private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(Model.of(""), Selectable.F_SELECTED) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<PasswordAccountDto> rowModel) {
                return () -> {
                    PasswordAccountDto passwordAccountDto = rowModel.getObject();
                    if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordCapabilityEnabled()) {
                        passwordAccountDto.setSelected(false);
                        return false;
                    }
                    if (CredentialsPropagationUserControlType.ONLY_MAPPING.equals(getModelObject().getPropagation())) {
                        if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordOutbound()) {
                            passwordAccountDto.setSelected(false);
                        }
                        return false;
                    }
                    if (passwordAccountDto.isMidpoint() && CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(getModelObject().getPropagation())) {
                        return false;
                    }
                    if (!passwordAccountDto.isMidpoint() && midpointAccountSelected.getObject() && passwordAccountDto.isPasswordOutbound()) {
                        passwordAccountDto.setSelected(true);
                        return false;
                    }
                    return true;
                };
            }

            @Override
            protected void processBehaviourOfCheckBox(IsolatedCheckBoxPanel check, IModel<PasswordAccountDto> rowModel) {
                super.processBehaviourOfCheckBox(check, rowModel);
                IModel<String> titleModel = () -> {
                    PasswordAccountDto passwordAccountDto = rowModel.getObject();
                    if (!getEnabled(rowModel).getObject()) {
                        String key;
                        if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordCapabilityEnabled()) {
                            key = "ChangePasswordPanel.legendMessage.no.password.capability";
                        } else {
                            key = "ChangePasswordPanel.legendMessage.policy";
                        }
                        return createStringResource(key).getString();
                    }
                    return "";
                };
                check.add(AttributeAppender.append("title", titleModel));
            }

            @Override
            protected void onUpdateRow(Item<ICellPopulator<PasswordAccountDto>> cellItem, AjaxRequestTarget target, DataTable table, IModel<PasswordAccountDto> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(cellItem, target, table, rowModel, selected);
                if (rowModel.getObject().isMidpoint()) {
                    table.visitChildren(IsolatedCheckBoxPanel.class,
                            (IVisitor<IsolatedCheckBoxPanel, IsolatedCheckBoxPanel>) (panel, iVisit) -> {
                                target.add(panel);
                            });
                }
            }
        });

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.name")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                    final IModel<PasswordAccountDto> rowModel) {
                item.add(new Label(componentId, new IModel<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        PasswordAccountDto dto = rowModel.getObject();
                        return dto.getDisplayName();
                    }
                }));
            }
        });

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.resourceName")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                    final IModel<PasswordAccountDto> rowModel) {
                IModel<String> helpModel = () -> {
                    String title = "";
                    if (!rowModel.getObject().isMidpoint() && !rowModel.getObject().isPasswordCapabilityEnabled()) {
                        title = createStringResource("ChangePasswordPanel.legendMessage.no.password.capability").getString();
                    }
                    if (rowModel.getObject().isMaintenanceState()) {
                        title = title
                                + (StringUtils.isEmpty(title) ? "" : " ")
                                + createStringResource("ChangePasswordPanel.legendMessage.maintenance").getString();
                    }
                    return title;
                };

                item.add(new LabelWithHelpPanel(componentId, new IModel<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        PasswordAccountDto dto = rowModel.getObject();
                        return dto.getResourceName();
                    }
                }) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return helpModel;
                    }
                });
            }
        });

        IconColumn enabled = new IconColumn<PasswordAccountDto>(createStringResource("ChangePasswordPanel.enabled")) {

            @Override
            protected DisplayType getIconDisplayType(IModel<PasswordAccountDto> rowModel) {
                String cssClass = "fa fa-question text-info";
                String tooltip = "ActivationStatusType.null";
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().isEnabled() != null) {
                    if (rowModel.getObject().isEnabled()) {
                        cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED;
                        tooltip = "ActivationStatusType.ENABLED";
                    } else {
                        cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED;
                        tooltip = "ActivationStatusType.DISABLED";
                    }
                }
                return GuiDisplayTypeUtil.createDisplayType(cssClass + " fa-fw fa-lg", "", createStringResource(tooltip).getString());
            }

            @Override
            public String getCssClass() {
                return "mp-w-lg-1";
            }
        };
        columns.add(enabled);

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.passwordValidation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> cellItem, String componentId, IModel<PasswordAccountDto> rowModel) {
                IModel<List<StringLimitationResult>> limitationsModel = () -> {
                    String policyOid = rowModel.getObject().getPasswordValuePolicyOid();
                    if (StringUtils.isEmpty(policyOid) || !getModelObject().getPasswordPolicies().containsKey(policyOid)) {
                        return new ArrayList<>();
                    }
                    if (limitationsByPolicyOid.containsKey(policyOid)) {
                        return limitationsByPolicyOid.get(policyOid);
                    }

                    ValuePolicyType policyType = getModelObject().getPasswordPolicies().get(policyOid);
                    PrismObject<? extends ObjectType> object = rowModel.getObject().getObject();
                    List<StringLimitationResult> limitations = getPasswordPanel().getLimitationsForActualPassword(policyType, object);
                    limitationsByPolicyOid.put(policyOid, limitations);
                    return limitations;
                };
                PasswordPolicyValidationPanel validationPanel = new PasswordPolicyValidationPanel(componentId, limitationsModel);
                validationPanel.add(new VisibleEnableBehaviour() {
                    @Override
                    public boolean isVisible() {
                        return !limitationsModel.getObject().isEmpty();
                    }
                });
                cellItem.add(validationPanel);
            }

            @Override
            public String getCssClass() {
                return "mp-w-lg-2";
            }
        });

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.propagationResult")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> cellItem, String componentId, IModel<PasswordAccountDto> rowModel) {
                LoadableModel<OperationResult> resultStatusModel = new LoadableModel<OperationResult>() {
                    @Override
                    protected OperationResult load() {
                        if (getModelObject().getProgress() == null
                                || getModelObject().getProgress().getProgressReportActivities().isEmpty()) {
                            return null;
                        }

                        for (ProgressReportActivityDto progressActivity : getModelObject().getProgress().getProgressReportActivities()) {
                            if (rowModel.getObject().isSelected() && progressActivity.getStatus() != null && rowModel.getObject().isMidpoint()
                                    && (ProgressInformation.ActivityType.FOCUS_OPERATION.equals(progressActivity.getActivityType())
                                    || (ProgressInformation.ActivityType.PROJECTOR.equals(progressActivity.getActivityType())
                                    && !OperationResultStatusType.SUCCESS.equals(progressActivity.getStatus())))) {
                                return progressActivity.getOperationResult();
                            } else if (progressActivity.getStatus() != null && !rowModel.getObject().isMidpoint()
                                    && ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION.equals(progressActivity.getActivityType())
                                    && progressActivity.getResourceOperationResultList() != null
                                    && !progressActivity.getResourceOperationResultList().isEmpty()) {
                                String resourceOid = rowModel.getObject().getResourceOid();
                                if (StringUtils.isNotEmpty(resourceOid) && progressActivity.getProjectionContextKey() != null
                                        && resourceOid.equals(progressActivity.getProjectionContextKey().getResourceOid())) {
                                    return progressActivity.getOperationResult();
                                }

                            }
                        }
                        return new OperationResult("Empty result");
                    }
                };
                ColumnResultPanel resultPanel = new ColumnResultPanel(componentId, resultStatusModel) {
                    @Override
                    protected boolean isProjectionResult() {
                        return !rowModel.getObject().isMidpoint();
                    }

                    @Override
                    protected DisplayType getDisplayForEmptyResult() {
                        String policyOid = rowModel.getObject().getPasswordValuePolicyOid();
                        if (StringUtils.isNotEmpty(policyOid) && ChangePasswordPanel.this.getModelObject().getPasswordPolicies().containsKey(policyOid)) {
                            if (limitationsByPolicyOid.get(policyOid) != null) {
                                var ref = new Object() {
                                    boolean result = true;
                                };
                                limitationsByPolicyOid.get(policyOid).forEach((limit) -> {
                                    if (ref.result && !limit.isSuccess()) {
                                        ref.result = false;
                                    }
                                });
                                if (!ref.result && rowModel.getObject().isSelected()) {
                                    return GuiDisplayTypeUtil.createDisplayType("fa-fw fa fa-times-circle text-muted fa-lg", "",
                                            createStringResource("ChangePasswordPanel.result.validationError").getString());
                                }
                            }
                        }
                        return null;
                    }
                };
                resultPanel.setOutputMarkupId(true);
                cellItem.add(resultPanel);
            }

            @Override
            public String getCssClass() {
                return "mp-w-lg-2";
            }
        });

        return columns;
    }

    public void updateResultColumnOfTable(AjaxRequestTarget target) {
        getTable().visitChildren(ColumnResultPanel.class,
                (IVisitor<ColumnResultPanel, ColumnResultPanel>) (panel, iVisit) -> {
                    if (panel.getModel() instanceof LoadableModel) {
                        ((LoadableModel) panel.getModel()).reset();
                    }
                    target.add(panel);
                });
    }

    private MyPasswordsDto loadPageModel() {
        LOGGER.debug("Loading user and accounts.");

        MyPasswordsDto passwordsDto = new MyPasswordsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER_WITH_ACCOUNTS);
        String focusOid = AuthUtil.getPrincipalUser().getOid();
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_USER);
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);
        PrismObject<? extends FocusType> focus = null;
        try {
            focus = getPageBase().getModelService().getObject(FocusType.class, focusOid, null, task, subResult);
        } catch (CommonException e) {
            if (shouldShowPasswordPropagation()) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user: " + e.getMessage(), e);
                result.recordFatalError(getString("ChangePasswordPanel.message.couldntLoadUser.fatalError", e.getMessage()), e);
            } else {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user: " + e.getMessage(), e);
                result.recordFatalError(getString("web.security.provider.access.denied"), e);
            }
            result.recomputeStatus();
        }
        if (focus != null) {
            passwordsDto = createMyPasswordsDto(focus);
            subResult.recordSuccessIfUnknown();

            getModel().setObject(passwordsDto);
            if (!shouldShowPasswordPropagation()) {
                LOGGER.debug("Skip loading account, because policy said so (enabled {} propagation).", passwordsDto.getPropagation());
                return passwordsDto;
            }

            PrismReference reference = focus.findReference(FocusType.F_LINK_REF);
            if (reference == null || CollectionUtils.isEmpty(reference.getValues())) {
                LOGGER.debug("No accounts found for user {}.", focusOid);
                return passwordsDto;
            }

            try {
                addAccountsToMyPasswordsDto(passwordsDto, reference.getValues(), task, result);
                result.recordSuccessIfUnknown();
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load accounts", ex);
                result.recordFatalError(getString("PageAbstractSelfCredentials.message.couldntLoadAccounts.fatalError"), ex);
            } finally {
                result.recomputeStatus();
            }
        }

        Collections.sort(passwordsDto.getAccounts());

        if (!result.isSuccess() && !result.isHandledError()) {
            getPageBase().showResult(result);
        }
        return passwordsDto;
    }

    private MyPasswordsDto createMyPasswordsDto(PrismObject<? extends FocusType> focus) {
        MyPasswordsDto dto = new MyPasswordsDto();
        dto.setFocus(focus);
        Task task = getPageBase().createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY);
        CredentialsPolicyType credentialsPolicyType = WebComponentUtil.getPasswordCredentialsPolicy(focus, getPageBase(), task);
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
                if (valuePolicyRef != null && valuePolicyRef.getOid() != null) {
                    task = getPageBase().createSimpleTask("load value policy");
                    PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                            valuePolicyRef, getPageBase(), task, task.getResult());
                    if (valuePolicy != null) {
                        dto.addPasswordPolicy(valuePolicy.asObjectable());
                    }
                }

            }
        }
        return dto;
    }

    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<? extends FocusType> focus, String passwordPolicyOid) {
        String customSystemName = WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midpoint.default.system.name");
        PasswordAccountDto accountDto = new PasswordAccountDto(focus, focus.getName().getOrig(),
                getString("PageSelfCredentials.resourceMidpoint", customSystemName),
                WebComponentUtil.isActivationEnabled(focus, ActivationType.F_EFFECTIVE_STATUS), true);
        accountDto.setPasswordValuePolicyOid(passwordPolicyOid);
        return accountDto;
    }

    private CredentialsPolicyType getPasswordCredentialsPolicy(ResourceObjectDefinition rOCDef) {
        LOGGER.debug("Getting credentials policy");
        Task task = getPageBase().createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY);
        OperationResult result = new OperationResult(OPERATION_GET_CREDENTIALS_POLICY);
        CredentialsPolicyType credentialsPolicyType = null;
        try {
            SecurityPolicyType securityPolicy = getPageBase().getModelInteractionService().getSecurityPolicy(rOCDef, task, result);
            if (securityPolicy != null) {
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

    private String getPasswordPolicyOid(CredentialsPolicyType credentialsPolicyType) {
        if (credentialsPolicyType != null && credentialsPolicyType.getPassword() != null
                && credentialsPolicyType.getPassword().getValuePolicyRef() != null) {
            return credentialsPolicyType.getPassword().getValuePolicyRef().getOid();
        }
        return null;
    }

    private void addAccountsToMyPasswordsDto(MyPasswordsDto dto, List<PrismReferenceValue> linkReferences, Task task, OperationResult result) {
        final Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_RESOURCE_REF).resolve()
                .item(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_SECURITY_POLICY_REF)).resolve()
                .build();
        for (PrismReferenceValue value : linkReferences) {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
            try {
                String accountOid = value.getOid();
                PrismObject<ShadowType> account = getPageBase().getModelService().getObject(ShadowType.class,
                        accountOid, options, task, subResult);

                dto.getAccounts().add(createPasswordAccountDto(dto, account, task, subResult));
                subResult.recordSuccessIfUnknown();
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load account", ex);
                subResult.recordFatalError(getString("PageAbstractSelfCredentials.message.couldntLoadAccount.fatalError"), ex);
            }
        }
    }

    private PasswordAccountDto createPasswordAccountDto(MyPasswordsDto passwordDto, PrismObject<ShadowType> account, Task task, OperationResult result) {
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
                ResourceObjectDefinition rOCDef = getPageBase().getModelInteractionService().getEditObjectClassDefinition(account,
                        resource.asPrismObject(), AuthorizationPhaseType.REQUEST, task, result);

                if (rOCDef != null) {
                    passwordAccountDto.setPasswordOutbound(getPasswordOutbound(account, resource, rOCDef));
                    CredentialsPolicyType credentialsPolicy = getPasswordCredentialsPolicy(rOCDef);
                    if (credentialsPolicy != null && credentialsPolicy.getPassword() != null
                            && credentialsPolicy.getPassword().getValuePolicyRef() != null) {
                        PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                                credentialsPolicy.getPassword().getValuePolicyRef(), getPageBase(), task, task.getResult());
                        if (valuePolicy != null) {
                            passwordAccountDto.setPasswordValuePolicyOid(valuePolicy.getOid());
                            passwordDto.addPasswordPolicy(valuePolicy.asObjectable());
                        }
                    }
                } else {
                    passwordAccountDto.setPasswordOutbound(false);
                }

            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Fail to get ResourceObjectTypeDefinition for {} ", e, account);
                result.recordFatalError("Fail to get ResourceObjectTypeDefinition for " + account, e);
                getPageBase().showResult(result);
                passwordAccountDto.setPasswordOutbound(false);
            }

        } else {
            passwordAccountDto.setPasswordCapabilityEnabled(false);
            passwordAccountDto.setPasswordOutbound(false);
        }

        return passwordAccountDto;
    }

    private boolean getPasswordOutbound(PrismObject<ShadowType> shadow, ResourceType resource, ResourceObjectDefinition rOCDef) {
        for (MappingType mapping : rOCDef.getPasswordOutbound()) {
            if (MappingStrengthType.WEAK == mapping.getStrength()) {
                CredentialsCapabilityType capability = ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
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

    private PasswordPanel getPasswordPanel() {
        return (PasswordPanel) get(ID_PASSWORD_PANEL);
    }

    private TablePanel getTable() {
        return (TablePanel) get(getPageBase().createComponentPath(ID_ACCOUNTS_CONTAINER, ID_ACCOUNTS_TABLE));
    }

    protected boolean isCheckOldPassword() {
        return false;
    }

    protected boolean canEditPassword() {
        return true;
    }
}
