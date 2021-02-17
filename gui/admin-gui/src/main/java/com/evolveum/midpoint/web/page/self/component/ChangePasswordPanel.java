/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
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
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.visit.IVisitor;

/**
 * @author Kate Honchar
 */
public class ChangePasswordPanel extends BasePanel<MyPasswordsDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_OLD_PASSWORD_CONTAINER = "oldPasswordContainer";
    private static final String ID_OLD_PASSWORD_FIELD = "oldPassword";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    public static final String ID_ACCOUNTS_TABLE = "accounts";
    public static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";

    private LoadableModel<MyPasswordsDto> model;
    private IModel<Boolean> midpointAccountSelected;
    private boolean oldPasswordVisible = false;
    private Map<String, List<StringLimitationResult>> limitationsByPolicyOid = new HashMap<>();

    public ChangePasswordPanel(String id, boolean oldPasswordVisible) {
        super(id);
        this.oldPasswordVisible = oldPasswordVisible;
    }

    public ChangePasswordPanel(String id, boolean oldPasswordVisible, LoadableModel<MyPasswordsDto> model, MyPasswordsDto myPasswordsDto) {
        super(id, model);
        this.oldPasswordVisible = oldPasswordVisible;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initMidpointAccountSelected();
        initLayout();
    }

    private void initMidpointAccountSelected() {
        MyPasswordsDto dto = getModelObject();
        PasswordAccountDto midpointAccount = null;
        for(PasswordAccountDto account : dto.getAccounts()){
            if (account.isMidpoint()) {
                midpointAccount = account;
            }
        }
        midpointAccountSelected = new PropertyModel<>(midpointAccount, Selectable.F_SELECTED);
    }

    private void initLayout() {
        model = (LoadableModel<MyPasswordsDto>) getModel();

        WebMarkupContainer oldPasswordContainer = new WebMarkupContainer(ID_OLD_PASSWORD_CONTAINER);
        oldPasswordContainer.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return oldPasswordVisible;
            }
        });
        add(oldPasswordContainer);

        PasswordTextField oldPasswordField =
                new PasswordTextField(ID_OLD_PASSWORD_FIELD, new PropertyModel<>(model, MyPasswordsDto.F_OLD_PASSWORD));
        oldPasswordField.setRequired(false);
        oldPasswordContainer.add(oldPasswordField);

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new PropertyModel<>(model, MyPasswordsDto.F_PASSWORD),
                model.getObject().getFocus(), getPageBase()){
            @Override
            protected <F extends FocusType> ValuePolicyType getValuePolicy(PrismObject<F> object) {
                return model.getObject().getFocusPolicy();
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
        };
        passwordPanel.getBaseFormComponent().add(new AttributeModifier("autofocus", ""));
        add(passwordPanel);

        WebMarkupContainer accountContainer = new WebMarkupContainer(ID_ACCOUNTS_CONTAINER);

        List<IColumn<PasswordAccountDto, String>> columns = initColumns();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<>(this,
            new PropertyModel<>(model, MyPasswordsDto.F_ACCOUNTS));
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

        columns.add(new CheckBoxColumn<>(Model.of(""), Selectable.F_SELECTED){
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
                    if (!rowModel.getObject().isMidpoint() && !rowModel.getObject().isPasswordCapabilityEnabled()){
                        title = createStringResource("ChangePasswordPanel.legendMessage.no.password.capability").getString();
                    }
                    if (rowModel.getObject().isMaintenanceState()){
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
                }){
                    @Override
                    protected IModel<String> getHelpModel() {
                        return helpModel;
                    }
                });
            }
        });

        IconColumn enabled = new IconColumn<PasswordAccountDto>(createStringResource("ChangePasswordPanel.enabled")){

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
                return WebComponentUtil.createDisplayType(cssClass + " fa-fw fa-lg", "", createStringResource(tooltip).getString());
            }

            @Override
            public String getCssClass() {
                return "col-lg-1";
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
                    return getPasswordPanel().getLimitationsForActualPassword(policyType, object);
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
                return "col-lg-2";
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
                            if (progressActivity.getStatus() != null && rowModel.getObject().isMidpoint()
                                    && (ProgressInformation.ActivityType.FOCUS_OPERATION.equals(progressActivity.getActivityType())
                                    || (ProgressInformation.ActivityType.PROJECTOR.equals(progressActivity.getActivityType())
                                    && !OperationResultStatusType.SUCCESS.equals(progressActivity.getStatus())))) {
                                return progressActivity.getOperationResult();
                            } else if (progressActivity.getStatus() != null && !rowModel.getObject().isMidpoint()
                                    && ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION.equals(progressActivity.getActivityType())
                                            && progressActivity.getResourceOperationResultList() != null
                                            && !progressActivity.getResourceOperationResultList().isEmpty()) {
                                String resourceOid = rowModel.getObject().getResourceOid();
                                if (StringUtils.isNotEmpty(resourceOid) && progressActivity.getResourceShadowDiscriminator() != null
                                        && resourceOid.equals(progressActivity.getResourceShadowDiscriminator().getResourceOid())) {
                                    return progressActivity.getOperationResult();
                                }

                            }
                        }
                        return new OperationResult("Empty result");
                    }
                };
                ColumnResultPanel resultPanel = new ColumnResultPanel(componentId, resultStatusModel){
                    @Override
                    protected boolean isProjectionResult() {
                        return !rowModel.getObject().isMidpoint();
                    }

                    @Override
                    protected DisplayType getDisplayForEmptyResult(){
                        String policyOid = rowModel.getObject().getPasswordValuePolicyOid();
                        if (StringUtils.isNotEmpty(policyOid) && ChangePasswordPanel.this.getModelObject().getPasswordPolicies().containsKey(policyOid)) {
                            return WebComponentUtil.createDisplayType("fa-fw fa fa-times-circle text-muted fa-lg", "", createStringResource("ChangePasswordPanel.result.validationError").getString());
                        }
                        return null;
                    }
                };
                resultPanel.setOutputMarkupId(true);
                cellItem.add(resultPanel);
            }

            @Override
            public String getCssClass() {
                return "col-lg-2";
            }
        });

        return columns;
    }

    public void updateResultColumnOfTable(AjaxRequestTarget target) {
        getTable().visitChildren(ColumnResultPanel.class,
                (IVisitor<ColumnResultPanel, ColumnResultPanel>) (panel, iVisit) -> {
                    if (panel.getModel() instanceof LoadableModel) {
                        ((LoadableModel)panel.getModel()).reset();
                    }
                    target.add(panel);
                });
    }

    private PasswordPanel getPasswordPanel(){
        return (PasswordPanel) get(ID_PASSWORD_PANEL);
    }

    private TablePanel getTable() {
        return (TablePanel) get(getPageBase().createComponentPath(ID_ACCOUNTS_CONTAINER, ID_ACCOUNTS_TABLE));
    }
}
