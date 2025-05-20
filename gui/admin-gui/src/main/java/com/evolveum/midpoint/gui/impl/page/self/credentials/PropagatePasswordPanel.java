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
import com.evolveum.midpoint.gui.api.component.password.PasswordLimitationsPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.model.api.ProgressInformation;
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
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisitor;

import java.io.Serial;
import java.util.*;

public class PropagatePasswordPanel<F extends FocusType> extends ChangePasswordPanel<F> {

    private static final String DOT_CLASS = PropagatePasswordPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadUserAccounts";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";
    private static final String OPERATION_LOAD_VALUE_POLICY = DOT_CLASS + "loadValuePolicy";
    private static final Trace LOGGER = TraceManager.getTrace(PropagatePasswordPanel.class);
    private static final String ID_PROPAGATE_PASSWORD_CHECKBOX = "propagatePasswordCheckbox";
    private static final String ID_INDIVIDUAL_SYSTEMS_CONTAINER = "individualSystemsContainer";
    private static final String ID_INDIVIDUAL_SYSTEMS_TABLE = "individualSystemsTable";

    private Boolean propagatePassword;
    private boolean showResultInTable = false;
    ListDataProvider<PasswordAccountDto> provider = null;

    public PropagatePasswordPanel(String id, IModel<F> focusModel) {
        super(id, focusModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPropagatePasswordDefault();
        initLayout();
    }

    private void initPropagatePasswordDefault() {
        if (propagatePassword == null) {
            CredentialsPropagationUserControlType propagationUserControl = getCredentialsPropagationUserControl();
            propagatePassword = propagationUserControl == CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY
                    || propagationUserControl == CredentialsPropagationUserControlType.USER_CHOICE;
        }
    }

    private void initLayout() {
        CheckBoxPanel propagatePasswordCheckbox = new CheckBoxPanel(ID_PROPAGATE_PASSWORD_CHECKBOX, Model.of(propagatePassword),
                createStringResource("ChangePasswordPanel.changePasswordOnIndividualSystems")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                target.add(PropagatePasswordPanel.this);
            }

        };
        propagatePasswordCheckbox.add(new VisibleBehaviour(this::shouldLoadAccounts));
        propagatePasswordCheckbox.setOutputMarkupId(true);
        add(propagatePasswordCheckbox);

        WebMarkupContainer individualSystemsContainer = new WebMarkupContainer(ID_INDIVIDUAL_SYSTEMS_CONTAINER);
        individualSystemsContainer.setOutputMarkupId(true);
        individualSystemsContainer.add(new VisibleBehaviour(this::isIndividualSystemsContainerVisible));
        add(individualSystemsContainer);

        provider = new ListDataProvider<>(PropagatePasswordPanel.this, getShadowListModel());
        BoxedTablePanel<PasswordAccountDto> provisioningTable = new BoxedTablePanel<>(ID_INDIVIDUAL_SYSTEMS_TABLE,
                provider, initColumns()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };
        individualSystemsContainer.add(provisioningTable);
    }

    private boolean isIndividualSystemsContainerVisible() {
        CheckBoxPanel propagateCheckBox = getPropagatePasswordCheckbox();
        return propagateCheckBox.getCheckboxModel().getObject() != null
                        && propagateCheckBox.getCheckboxModel().getObject() || showResultInTable;
    }

    private CheckBoxPanel getPropagatePasswordCheckbox() {
        return (CheckBoxPanel) get(ID_PROPAGATE_PASSWORD_CHECKBOX);
    }

    private IModel<List<PasswordAccountDto>> getShadowListModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PasswordAccountDto> load() {
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
            }
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

        PasswordAccountDto passwordAccountDto = new PasswordAccountDto(account, resourceName,
                resourceRef != null ? resourceRef.getOid() : "");

        ShadowType shadowType = account.asObjectable();
        ResourceType resource = (ResourceType) shadowType.getResourceRef().asReferenceValue().getObject().asObjectable();
        if (resource != null) {
            ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType = ResourceTypeUtil.findObjectTypeDefinition(resource.asPrismObject(),
                    shadowType.getKind(), shadowType.getIntent());
            passwordAccountDto.setPasswordCapabilityEnabled(ResourceTypeUtil.isPasswordCapabilityEnabled(resource, resourceObjectTypeDefinitionType));
            passwordAccountDto.setMaintenanceState(ResourceTypeUtil.isInMaintenance(resource));
            try {
                ResourceObjectDefinition rOCDef = getPageBase().getModelInteractionService().getEditObjectClassDefinition(account,
                        resource.asPrismObject(), AuthorizationPhaseType.REQUEST, task, result);

                if (rOCDef != null) {
                    passwordAccountDto.setPasswordOutbound(getPasswordOutbound(resource, rOCDef));
                    CredentialsPolicyType credentialsPolicy = getPasswordCredentialsPolicy(rOCDef);
                    if (credentialsPolicy != null
                            && credentialsPolicy.getPassword() != null
                            && credentialsPolicy.getPassword().getValuePolicyRef() != null) {
                        // Actually, the value policy should be already resolved, so the following call is redundant.
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

    private boolean isPasswordPropagationEnabled(PasswordAccountDto passwordAccountDto) {
        if (passwordAccountDto.isMidpoint()) {
            return true;
        }
        return passwordAccountDto.isPasswordCapabilityEnabled() || passwordAccountDto.isPasswordOutbound();
    }

    private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<Boolean> getEnabled(IModel<PasswordAccountDto> rowModel) {
                return () -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return true;
                    }
                    CredentialsPropagationUserControlType propagationUserControl = getCredentialsPropagationUserControl();
                    PasswordAccountDto passwordAccountDto = rowModel.getObject();
                    if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordCapabilityEnabled()) {
                        passwordAccountDto.setSelected(false);
                        return false;
                    }
                    if (CredentialsPropagationUserControlType.ONLY_MAPPING.equals(propagationUserControl)) {
                        if (!passwordAccountDto.isMidpoint() && !passwordAccountDto.isPasswordOutbound()) {
                            passwordAccountDto.setSelected(false);
                        }
                        return false;
                    }
                    if (passwordAccountDto.isMidpoint()
                            && CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(propagationUserControl)) {
                        return false;
                    }
                    if (!passwordAccountDto.isMidpoint() && isMidpointAccountSelected() && passwordAccountDto.isPasswordOutbound()) {
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

            @Override
            protected boolean shouldBeUnchangeable(PasswordAccountDto obj) {
                if (obj == null) {
                    return super.shouldBeUnchangeable(obj);
                }
                return isMandatoryPropagation(obj);
            }
        });

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.name")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                    final IModel<PasswordAccountDto> rowModel) {
                item.add(new Label(componentId, new IModel<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        PasswordAccountDto dto = rowModel.getObject();
                        return dto.getDisplayName();
                    }
                }));
            }
        });

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.resourceName")) {
            @Serial private static final long serialVersionUID = 1L;

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
                    @Serial private static final long serialVersionUID = 1L;

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

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PasswordAccountDto> rowModel) {
                String cssClass = "fa fa-question text-info";
                String tooltip = "ChangePasswordPanel.undefined";
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().isEnabled() != null) {
                    if (rowModel.getObject().isEnabled()) {
                        cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED;
                        tooltip = "ChangePasswordPanel.enabled";
                    } else {
                        cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED;
                        tooltip = "ChangePasswordPanel.disabled";
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
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> cellItem, String componentId, IModel<PasswordAccountDto> rowModel) {
                LoadableDetachableModel<List<StringLimitationResult>> limitationsModel = new LoadableDetachableModel<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected List<StringLimitationResult> load() {
                        if (rowModel.getObject().getPasswordValuePolicy() == null) {
                            return new ArrayList<>();
                        }

                        return getLimitationsForActualPassword(rowModel.getObject().getPasswordValuePolicy(),
                                rowModel.getObject().getObject());
                    }
                };

                PasswordPolicyValidationPanel validationPanel = new PasswordPolicyValidationPanel(componentId, limitationsModel);
                validationPanel.add(new VisibleEnableBehaviour(() -> !limitationsModel.getObject().isEmpty()));
                validationPanel.setOutputMarkupId(true);
                cellItem.add(validationPanel);
            }

            @Override
            public String getCssClass() {
                return "mp-w-lg-2";
            }
        });

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.propagationResult")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> cellItem, String componentId, IModel<PasswordAccountDto> rowModel) {
                LoadableModel<OperationResult> resultStatusModel = new LoadableModel<OperationResult>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected OperationResult load() {
                        if (progress == null
                                || progress.getProgressReportActivities().isEmpty()) {
                            return null;
                        }

                        for (ProgressReportActivityDto progressActivity : progress.getProgressReportActivities()) {
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
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isProjectionResult() {
                        return !rowModel.getObject().isMidpoint();
                    }

                    @Override
                    protected DisplayType getDisplayForEmptyResult() {
                        List<StringLimitationResult> limitations = getLimitationsForActualPassword(rowModel.getObject().getPasswordValuePolicy(),
                                rowModel.getObject().getObject());
                        for (StringLimitationResult limit : limitations) {
                            if (!limit.isSuccess() && rowModel.getObject().isSelected()) {
                                return GuiDisplayTypeUtil.createDisplayType("fa-fw fa fa-times-circle text-muted fa-lg", "",
                                        createStringResource("ChangePasswordPanel.result.validationError").getString());
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

    private boolean isMidpointAccountSelected() {
        Iterator<PasswordAccountDto> accounts = (Iterator<PasswordAccountDto>) provider.internalIterator(0, provider.size());
        while (accounts.hasNext()) {
            PasswordAccountDto account = accounts.next();
            if (account.isMidpoint()) {
                return account.isSelected();
            }
        }
        return false;
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


    private boolean getPasswordOutbound(ResourceType resource, ResourceObjectDefinition rOCDef) {
        for (MappingType mapping : rOCDef.getPasswordOutboundMappings()) {
            if (MappingStrengthType.WEAK == mapping.getStrength()) {
                var credentialsCapability = ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
                if (CapabilityUtil.isPasswordReadable(credentialsCapability)) {
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

    protected void collectDeltas(Collection<ObjectDelta<? extends ObjectType>> deltas, ProtectedStringType currentPassword, ItemPath valuePath) {
        if (isMidpointAccountSelected()) {
            //let's use the unified code of changing password for current user
            super.collectDeltas(deltas, currentPassword, valuePath);
        }
        List<PasswordAccountDto> selectedAccounts = getAccountsListToChangePassword();

        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        selectedAccounts.forEach(account -> {
            PrismObjectDefinition objDef = account.isMidpoint() ?
                    registry.findObjectDefinitionByCompileTimeClass(UserType.class) :
                    registry.findObjectDefinitionByCompileTimeClass(ShadowType.class);

            PropertyDelta<ProtectedStringType> delta = getPrismContext().deltaFactory().property()
                    .createModificationReplaceProperty(valuePath, objDef, currentPassword);
            if (currentPassword != null) {
                delta.addEstimatedOldValue(getPrismContext().itemFactory().createPropertyValue(currentPassword));
            }

            Class<? extends ObjectType> type = account.isMidpoint() ? UserType.class : ShadowType.class;

            deltas.add(getPrismContext().deltaFactory().object().createModifyDelta(account.getOid(), delta, type));
        });
    }

    protected void finishChangePassword(OperationResult result, AjaxRequestTarget target, boolean showFeedback) {
        updateResultColumnOfTable(target);
        showResultInTable = true;
        if (shouldLoadAccounts()) {
            showFeedback = false;
            String msg;
            String cssClass;
            if (result.isError()) {
                msg = createStringResource("PageAbstractSelfCredentials.message.resultInTable.error").getString();
                cssClass = "bg-danger m-3";
            } else {
                msg = createStringResource("PageAbstractSelfCredentials.message.resultInTable").getString();
                cssClass = "bg-success m-3";
            }
            new Toast()
                    .cssClass(cssClass)
                    .autohide(true)
                    .delay(10_000)
                    .title(msg)
                    .show(target);
        }
        super.finishChangePassword(result, target, showFeedback);
    }

    protected boolean shouldLoadAccounts() {
        return getCredentialsPropagationUserControl() == null
                || CredentialsPropagationUserControlType.USER_CHOICE.equals(getCredentialsPropagationUserControl())
                || CredentialsPropagationUserControlType.ONLY_MAPPING.equals(getCredentialsPropagationUserControl())
                || CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(getCredentialsPropagationUserControl());
    }

    private void updateResultColumnOfTable(AjaxRequestTarget target) {
        getTableComponent().getDataTable().visitChildren(ColumnResultPanel.class,
                (IVisitor<ColumnResultPanel, ColumnResultPanel>) (panel, iVisit) -> {
                    if (panel.getModel() instanceof LoadableModel) {
                        ((LoadableModel) panel.getModel()).reset();
                    }
                    target.add(panel);
                });
    }

    //fix for open project ticket 9608
    private void updatePasswordValidationColumnOfTable(AjaxRequestTarget target) {
        getTableComponent().getDataTable().visitChildren(PasswordPolicyValidationPanel.class,
                (IVisitor<PasswordPolicyValidationPanel, PasswordPolicyValidationPanel>) (panel, iVisit) -> {
                    if (panel.getModel() instanceof LoadableDetachableModel panelModel) {
                        panelModel.detach();
                    }
                    target.add(panel);
                });

        target.appendJavaScript(PasswordPolicyValidationPanel.JAVA_SCRIPT_CODE);
    }

    private CredentialsPropagationUserControlType getCredentialsPropagationUserControl() {
        CredentialsPolicyType credentialsPolicy = credentialsPolicyModel.getObject();
        return credentialsPolicy != null && credentialsPolicy.getPassword() != null ?
                credentialsPolicy.getPassword().getPropagationUserControl() : null;
    }

    private BoxedTablePanel<PasswordAccountDto> getTableComponent() {
        return (BoxedTablePanel<PasswordAccountDto>) get(createComponentPath(ID_INDIVIDUAL_SYSTEMS_CONTAINER, ID_INDIVIDUAL_SYSTEMS_TABLE));
    }

    @Override
    protected PasswordLimitationsPanel createLimitationPanel(String id,
            LoadableDetachableModel<List<StringLimitationResult>> limitationsModel) {
        return new PasswordLimitationsPanel(id, limitationsModel) {
            @Override
            protected boolean showInTwoColumns() {
                if (getModelObject().size() > 5) {
                    return true;
                }
                return super.showInTwoColumns();
            }
        };
    }

    @Override
    protected boolean isHintPanelVisible() {
        return getPasswordHintConfigurability() == PasswordHintConfigurabilityType.ALWAYS_CONFIGURE;
    }

    private boolean isMandatoryPropagation(PasswordAccountDto passwordAccountDto) {
        CredentialsPropagationUserControlType propagationUserControl = getCredentialsPropagationUserControl();
        return passwordAccountDto.isMidpoint()
                && CredentialsPropagationUserControlType.IDENTITY_MANAGER_MANDATORY.equals(propagationUserControl);
    }

    protected void updateNewPasswordValuePerformed(AjaxRequestTarget target) {
        super.updateNewPasswordValuePerformed(target);
        updatePasswordValidationColumnOfTable(target);
    }

    protected boolean removePasswordValueAttribute() {
        return false;
    }

    private List<PasswordAccountDto> getAccountsListToChangePassword() {
        List<PasswordAccountDto> result = Lists.newArrayList(provider.internalIterator(0, provider.size()));
        result.removeIf(account -> !account.isSelected());
        result.removeIf(account -> !isPasswordPropagationEnabled(account));
        result.removeIf(PasswordAccountDto::isMidpoint);    //midpoint account is handled in super class
        if (isMidpointAccountSelected()) {
            //fix for 9571: outbound mapping was already processed during midpoint account change execution
            result.removeIf(PasswordAccountDto::isPasswordOutbound);
        }
        return result;
    }
}
