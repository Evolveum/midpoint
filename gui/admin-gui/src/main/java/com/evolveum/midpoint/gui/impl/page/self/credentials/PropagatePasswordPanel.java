/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.credentials;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.self.PageAbstractSelfCredentials;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PropagatePasswordPanel<F extends FocusType> extends ChangePasswordPanel<F> {

    private static final String DOT_CLASS = PropagatePasswordPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadUserAccounts";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";
    private static final String OPERATION_LOAD_VALUE_POLICY = DOT_CLASS + "loadValuePolicy";
    private static final Trace LOGGER = TraceManager.getTrace(PropagatePasswordPanel.class);
    private static final String ID_PROPAGATE_PASSWORD_CHECKBOX = "propagatePasswordCheckbox";
    private static final String ID_INDIVIDUAL_SYSTEMS_TABLE = "individualSystemsTable";

    private boolean propagatePassword = false;

    public PropagatePasswordPanel(String id, IModel<F> focusModel) {
        super(id, focusModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CheckBoxPanel propagatePasswordCheckbox = new CheckBoxPanel(ID_PROPAGATE_PASSWORD_CHECKBOX, Model.of(propagatePassword),
                createStringResource("ChangePasswordPanel.changePasswordOnIndividualSystems")) {

            private static final long serialVersionUID = 1L;
            @Override
            public void onUpdate(AjaxRequestTarget target) {
                target.add(PropagatePasswordPanel.this);
            }

        };
        propagatePasswordCheckbox.setOutputMarkupId(true);
        add(propagatePasswordCheckbox);

        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<>(PropagatePasswordPanel.this, getShadowModel());
        BoxedTablePanel<PasswordAccountDto> provisioningTable = new BoxedTablePanel<>(ID_INDIVIDUAL_SYSTEMS_TABLE,
                provider, initColumns()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };
        provisioningTable.add(new VisibleBehaviour(() -> propagatePasswordCheckbox.getCheckboxModel().getObject() != null
                && propagatePasswordCheckbox.getCheckboxModel().getObject()));
        add(provisioningTable);
    }

    private IModel<List<PasswordAccountDto>> getShadowModel() {
        return () -> {
            List<PasswordAccountDto> accountDtoList = new ArrayList<>();
            accountDtoList.add(createDefaultPasswordAccountDto());
            PrismReference linkReferences = getModelObject().asPrismObject().findReference(FocusType.F_LINK_REF);
            if (linkReferences == null) {
                return accountDtoList;
            }
            final Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                    .item(ShadowType.F_RESOURCE_REF).resolve()
                    .item(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE,
                            ResourceObjectTypeDefinitionType.F_SECURITY_POLICY_REF)).resolve()
                    .build();
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ACCOUNTS);
            OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
            for (PrismReferenceValue value : linkReferences.getValues()) {
                try {
                    String accountOid = value.getOid();
                    PrismObject<ShadowType> account = getPageBase().getModelService().getObject(ShadowType.class,
                            accountOid, options, task, result);

                    accountDtoList.add(createPasswordAccountDto(account, task, result));
                    result.recordSuccessIfUnknown();
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load account", ex);
                    result.recordFatalError(getString("PageAbstractSelfCredentials.message.couldntLoadAccount.fatalError"), ex);
                }
            }

            return accountDtoList;
        };
    }

    private PasswordAccountDto createPasswordAccountDto(PrismObject<ShadowType> account, Task task, OperationResult result) {
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
                    passwordAccountDto.setPasswordOutbound(getPasswordOutbound(resource, rOCDef));
                    CredentialsPolicyType credentialsPolicy = getPasswordCredentialsPolicy(rOCDef);
                    if (credentialsPolicy != null && credentialsPolicy.getPassword() != null
                            && credentialsPolicy.getPassword().getValuePolicyRef() != null) {
                        PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                                credentialsPolicy.getPassword().getValuePolicyRef(), getPageBase(), task, task.getResult());
                        if (valuePolicy != null) {
                            passwordAccountDto.setPasswordValuePolicy(valuePolicy.asObjectable());
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

    private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>() {
            @Override
            protected IModel<Boolean> getEnabled(IModel<PasswordAccountDto> rowModel) {
                return Model.of(true);
//                return () -> {
//                    PasswordAccountDto passwordAccountDto = rowModel.getObject();
//                    if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordCapabilityEnabled()) {
//                        passwordAccountDto.setSelected(false);
//                        return false;
//                    }
//                    if (CredentialsPropagationUserControlType.ONLY_MAPPING.equals(getModelObject().getPropagation())) {
//                        if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordOutbound()) {
//                            passwordAccountDto.setSelected(false);
//                        }
//                        return false;
//                    }
//                    if (passwordAccountDto.isMidpoint() && CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(getModelObject().getPropagation())) {
//                        return false;
//                    }
//                    if (!passwordAccountDto.isMidpoint() && midpointAccountSelected.getObject() && passwordAccountDto.isPasswordOutbound()) {
//                        passwordAccountDto.setSelected(true);
//                        return false;
//                    }
//                    return true;
//                };
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
            protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<PasswordAccountDto> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(target, table, rowModel, selected);
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
                    if (rowModel.getObject().getPasswordValuePolicy() == null) {
                        return new ArrayList<>();
                    }

                    return getLimitationsForActualPassword(rowModel.getObject().getPasswordValuePolicy(),
                            rowModel.getObject().getObject());
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

//        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.propagationResult")) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> cellItem, String componentId, IModel<PasswordAccountDto> rowModel) {
//                LoadableModel<OperationResult> resultStatusModel = new LoadableModel<OperationResult>() {
//                    @Override
//                    protected OperationResult load() {
//                        if (getModelObject().getProgress() == null
//                                || getModelObject().getProgress().getProgressReportActivities().isEmpty()) {
//                            return null;
//                        }
//
//                        for (ProgressReportActivityDto progressActivity : getModelObject().getProgress().getProgressReportActivities()) {
//                            if (rowModel.getObject().isSelected() && progressActivity.getStatus() != null && rowModel.getObject().isMidpoint()
//                                    && (ProgressInformation.ActivityType.FOCUS_OPERATION.equals(progressActivity.getActivityType())
//                                    || (ProgressInformation.ActivityType.PROJECTOR.equals(progressActivity.getActivityType())
//                                    && !OperationResultStatusType.SUCCESS.equals(progressActivity.getStatus())))) {
//                                return progressActivity.getOperationResult();
//                            } else if (progressActivity.getStatus() != null && !rowModel.getObject().isMidpoint()
//                                    && ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION.equals(progressActivity.getActivityType())
//                                    && progressActivity.getResourceOperationResultList() != null
//                                    && !progressActivity.getResourceOperationResultList().isEmpty()) {
//                                String resourceOid = rowModel.getObject().getResourceOid();
//                                if (StringUtils.isNotEmpty(resourceOid) && progressActivity.getResourceShadowDiscriminator() != null
//                                        && resourceOid.equals(progressActivity.getResourceShadowDiscriminator().getResourceOid())) {
//                                    return progressActivity.getOperationResult();
//                                }
//
//                            }
//                        }
//                        return new OperationResult("Empty result");
//                    }
//                };
//                ColumnResultPanel resultPanel = new ColumnResultPanel(componentId, resultStatusModel) {
//                    @Override
//                    protected boolean isProjectionResult() {
//                        return !rowModel.getObject().isMidpoint();
//                    }
//
//                    @Override
//                    protected DisplayType getDisplayForEmptyResult() {
//                        String policyOid = rowModel.getObject().getPasswordValuePolicyOid();
//                        if (StringUtils.isNotEmpty(policyOid) && com.evolveum.midpoint.web.page.self.component.ChangePasswordPanel.this.getModelObject().getPasswordPolicies().containsKey(policyOid)) {
//                            if (limitationsByPolicyOid.get(policyOid) != null) {
//                                var ref = new Object() {
//                                    boolean result = true;
//                                };
//                                limitationsByPolicyOid.get(policyOid).forEach((limit) -> {
//                                    if (ref.result && !limit.isSuccess()) {
//                                        ref.result = false;
//                                    }
//                                });
//                                if (!ref.result && rowModel.getObject().isSelected()) {
//                                    return GuiDisplayTypeUtil.createDisplayType("fa-fw fa fa-times-circle text-muted fa-lg", "",
//                                            createStringResource("ChangePasswordPanel.result.validationError").getString());
//                                }
//                            }
//                        }
//                        return null;
//                    }
//                };
//                resultPanel.setOutputMarkupId(true);
//                cellItem.add(resultPanel);
//            }
//
//            @Override
//            public String getCssClass() {
//                return "mp-w-lg-2";
//            }
//        });

        return columns;
    }

    private PasswordAccountDto createDefaultPasswordAccountDto() {
        String customSystemName = WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midpoint.default.system.name");
        PasswordAccountDto accountDto = new PasswordAccountDto(getModelObject().asPrismObject(), getModelObject().asPrismObject().getName().getOrig(),
                getString("PageSelfCredentials.resourceMidpoint", customSystemName),
                WebComponentUtil.isActivationEnabled(getModelObject().asPrismObject(), ActivationType.F_EFFECTIVE_STATUS), true);
        CredentialsPolicyType credentialsPolicyType = WebComponentUtil.getPasswordCredentialsPolicy(getModelObject().asPrismObject(), getPageBase(),
                getPageBase().createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY));
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_VALUE_POLICY);
        ObjectReferenceType passwordPolicyRef = credentialsPolicyType != null && credentialsPolicyType.getPassword() != null ?
                credentialsPolicyType.getPassword().getValuePolicyRef() : null;
        PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                passwordPolicyRef, getPageBase(), task, task.getResult());
            accountDto.setPasswordValuePolicy(valuePolicy.asObjectable());
        return accountDto;
    }

    @Override
    protected void updateNewPasswordValuePerformed(AjaxRequestTarget target) {
        super.updateNewPasswordValuePerformed(target);
    }

    private boolean getPasswordOutbound(ResourceType resource, ResourceObjectDefinition rOCDef) {
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

    private List<StringLimitationResult> getLimitationsForActualPassword(ValuePolicyType valuePolicy, PrismObject<? extends ObjectType> object) {
        if (valuePolicy != null) {
            Task task = getPageBase().createAnonymousTask("validation of password");
            try {
                return getPageBase().getModelInteractionService().validateValue(newPasswordValue, valuePolicy, object, task, task.getResult());
            } catch (Exception e) {
                LOGGER.error("Couldn't validate password security policy", e);
            }
        }
        return new ArrayList<>();
    }

//    @Override
//    protected void changePasswordPerformed(AjaxRequestTarget target) {
//        ProtectedStringType currentPassword = null;
//        if (isCheckOldPassword()) {
//            LOGGER.debug("Check old password");
//            if (currentPasswordValue == null || currentPasswordValue.trim().equals("")) {
//                warn(getString("PageSelfCredentials.specifyOldPasswordMessage"));
//                target.add(getPageBase().getFeedbackPanel());
//                return;
//            } else {
//                OperationResult checkPasswordResult = new OperationResult(OPERATION_CHECK_PASSWORD);
//                Task checkPasswordTask = getPageBase().createSimpleTask(OPERATION_CHECK_PASSWORD);
//                try {
//                    currentPassword = new ProtectedStringType();
//                    currentPassword.setClearValue(currentPasswordValue);
//                    boolean isCorrectPassword = getPageBase().getModelInteractionService().checkPassword(getModelObject().getOid(), currentPassword,
//                            checkPasswordTask, checkPasswordResult);
//                    if (!isCorrectPassword) {
//                        error(getString("PageSelfCredentials.incorrectOldPassword"));
//                        target.add(getPageBase().getFeedbackPanel());
//                        return;
//                    }
//                } catch (Exception ex) {
//                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check password", ex);
//                    checkPasswordResult.recordFatalError(
//                            getString("PageAbstractSelfCredentials.message.onSavePerformed.fatalError", ex.getMessage()), ex);
//                    target.add(getPageBase().getFeedbackPanel());
//                    return;
//                } finally {
//                    checkPasswordResult.computeStatus();
//                }
//            }
//        }
//
//        if (newPasswordValueModel.getObject() == null) {
//            warn(getString("PageSelfCredentials.emptyPasswordFiled"));
//            target.add(getPageBase().getFeedbackPanel());
//            return;
//        }
//
//
//
//        List<com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto> selectedAccounts = getSelectedAccountsList();
//        if (selectedAccounts.isEmpty()) {
//            warn(getString("PageSelfCredentials.noAccountSelected"));
//            target.add(getFeedbackPanel());
//            return;
//        }
//
//        OperationResult result = new OperationResult(OPERATION_SAVE_PASSWORD);
//        ProgressReporter reporter = new ProgressReporter(MidPointApplication.get());
//        reporter.getProgress().clear();
//        reporter.setWriteOpResultForProgressActivity(true);
//
//        reporter.recordExecutionStart();
//        boolean showFeedback = true;
//        try {
//            MyPasswordsDto dto = getPasswordDto();
//            ProtectedStringType password = dto.getPassword();
//            if (!password.isEncrypted()) {
//                WebComponentUtil.encryptProtectedString(password, true, getMidpointApplication());
//            }
//            final ItemPath valuePath = ItemPath.create(SchemaConstantsGenerated.C_CREDENTIALS,
//                    CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
//            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
//            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
//
//            for (com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto accDto : selectedAccounts) {
//                PrismObjectDefinition objDef = accDto.isMidpoint() ?
//                        registry.findObjectDefinitionByCompileTimeClass(UserType.class) :
//                        registry.findObjectDefinitionByCompileTimeClass(ShadowType.class);
//
//                PropertyDelta<ProtectedStringType> delta = getPrismContext().deltaFactory().property()
//                        .createModificationReplaceProperty(valuePath, objDef, password);
//                if (currentPassword != null) {
//                    delta.addEstimatedOldValue(getPrismContext().itemFactory().createPropertyValue(currentPassword));
//                }
//
//                Class<? extends ObjectType> type = accDto.isMidpoint() ? UserType.class : ShadowType.class;
//
//                deltas.add(getPrismContext().deltaFactory().object().createModifyDelta(accDto.getOid(), delta, type
//                ));
//            }
//            getModelService().executeChanges(
//                    deltas, null, createSimpleTask(OPERATION_SAVE_PASSWORD, SchemaConstants.CHANNEL_SELF_SERVICE_URI), Collections.singleton(reporter), result);
//            result.computeStatus();
//        } catch (Exception ex) {
//            setNullEncryptedPasswordData();
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save password changes", ex);
//            result.recordFatalError(getString("PageAbstractSelfCredentials.save.password.failed", ex.getMessage()), ex);
//        } finally {
//            reporter.recordExecutionStop();
//            getPasswordDto().setProgress(reporter.getProgress());
//            if (getActualTabPanel() != null) {
//                ((com.evolveum.midpoint.web.page.self.component.ChangePasswordPanel) getActualTabPanel()).updateResultColumnOfTable(target);
//            }
//            result.computeStatusIfUnknown();
//            if (shouldLoadAccounts()) {
//                showFeedback = false;
//                if (result.isError()) {
//                    error(createStringResource("PageAbstractSelfCredentials.message.resultInTable.error").getString());
//                } else {
//                    success(createStringResource("PageAbstractSelfCredentials.message.resultInTable").getString());
//                }
//            }
//            if (!result.isError()) {
//                this.savedPassword = true;
//                target.add(PageAbstractSelfCredentials.this.get(ID_MAIN_FORM));
//            }
//        }
//
//        finishChangePassword(result, target, showFeedback);
//    }
//
//    protected void updateResultColumnOfTable(AjaxRequestTarget target) {
//        getTable().visitChildren(ColumnResultPanel.class,
//                (IVisitor<ColumnResultPanel, ColumnResultPanel>) (panel, iVisit) -> {
//                    if (panel.getModel() instanceof LoadableModel) {
//                        ((LoadableModel) panel.getModel()).reset();
//                    }
//                    target.add(panel);
//                });
//    }

    private Component getTableComponent() {
        return get(ID_INDIVIDUAL_SYSTEMS_TABLE);
    }
}
