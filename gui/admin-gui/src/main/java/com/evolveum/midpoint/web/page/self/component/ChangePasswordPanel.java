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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
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
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;

import org.apache.wicket.util.visit.IVisitor;

/**
 * @author Kate Honchar
 */
public class ChangePasswordPanel extends BasePanel<MyPasswordsDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_OLD_PASSWORD_FIELD = "oldPassword";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    private static final String ID_OLD_PASSWORD_LABEL = "oldPasswordLabel";
    public static final String ID_ACCOUNTS_TABLE = "accounts";
    public static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";
    private static final String ID_BUTTON_HELP = "help";
    public static final String SELECTED_ACCOUNT_ICON_CSS = "fa fa-check-square-o";
    public static final String DESELECTED_ACCOUNT_ICON_CSS = "fa fa-square-o";
    public static final String PROPAGATED_ACCOUNT_ICON_CSS = "fa fa-sign-out";
    public static final String NO_CAPABILITY_ICON_CSS = "fa fa-square";
    private static final int HELP_MODAL_WIDTH = 400;
    private static final int HELP_MODAL_HEIGH = 600;

    private LoadableModel<MyPasswordsDto> model;
    private boolean midpointAccountSelected = true;
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
        initLayout();
    }

    private void initLayout() {
        model = (LoadableModel<MyPasswordsDto>) getModel();

        Label oldPasswordLabel = new Label(ID_OLD_PASSWORD_LABEL, createStringResource("PageSelfCredentials.oldPasswordLabel"));
        add(oldPasswordLabel);
        oldPasswordLabel.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return oldPasswordVisible;
            }
        });

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        PasswordTextField oldPasswordField =
                new PasswordTextField(ID_OLD_PASSWORD_FIELD, new PropertyModel<>(model, MyPasswordsDto.F_OLD_PASSWORD));
        oldPasswordField.setRequired(false);
        add(oldPasswordField);
        oldPasswordField.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return oldPasswordVisible;
            }
        });

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

//        AjaxLink<Void> help = new AjaxLink<Void>(ID_BUTTON_HELP) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                showHelpPerformed(target);
//            }
//
//
//        };
//
//        accountContainer.add(help);
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

        IColumn column = new IconColumn<PasswordAccountDto>(new Model<>()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PasswordAccountDto> rowModel) {
                PasswordAccountDto item = rowModel.getObject();
                if (item.getCssClass() == null || item.getCssClass().trim().equals("")) {
                    if (item.isMidpoint()) {
                        item.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                    } else if (!item.isPasswordCapabilityEnabled()){
                        item.setCssClass(NO_CAPABILITY_ICON_CSS);
                    } else if (item.isPasswordOutbound()) {
                        item.setCssClass(PROPAGATED_ACCOUNT_ICON_CSS);
                    } else {
                        item.setCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                    }
                }
                return WebComponentUtil.createDisplayType(item.getCssClass());

            }

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                                     final IModel<PasswordAccountDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                final ImagePanel imagePanel = (ImagePanel) item.get(componentId);

                final PasswordAccountDto passwordAccountDto = rowModel.getObject();

                imagePanel.add(new AjaxEventBehavior("click") {
                    private static final long serialVersionUID = 1L;

                                   protected void onEvent(final AjaxRequestTarget target) {
                                       if (!passwordAccountDto.isMidpoint()) {
                                           if (passwordAccountDto.getCssClass().equals(PROPAGATED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                                           } else if (passwordAccountDto.getCssClass().equals(SELECTED_ACCOUNT_ICON_CSS)
                                                   && passwordAccountDto.isPasswordOutbound() &&
                                                   midpointAccountSelected) {
                                               passwordAccountDto.setCssClass(PROPAGATED_ACCOUNT_ICON_CSS);
                                           }  else if (passwordAccountDto.getCssClass().equals(SELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                                           } else if (passwordAccountDto.getCssClass().equals(DESELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                                           }
                                           target.add(imagePanel.findParent(SelectableDataTable.class));
                                       } else {
                                           midpointAccountSelected = !midpointAccountSelected;
                                           if (passwordAccountDto.getCssClass().equals(SELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                                               updatePropagatedAccountIconsCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                                               target.add(imagePanel.findParent(SelectableDataTable.class));
                                           } else if (passwordAccountDto.getCssClass().equals(DESELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                                               updatePropagatedAccountIconsCssClass(PROPAGATED_ACCOUNT_ICON_CSS);
                                               target.add(imagePanel.findParent(SelectableDataTable.class));
                                           }
                                       }
                                   }
                               }
                );

                imagePanel.add(new VisibleEnableBehaviour() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return !passwordAccountDto.getCssClass().equals(NO_CAPABILITY_ICON_CSS);
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                Label help = new Label(componentId);
                IModel<String> helpModel = createStringResource("ChangePasswordPanel.helpInfo",
                        WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midpoint.default.system.name"));
                help.add(AttributeModifier.replace("title", helpModel));
                help.add(new InfoTooltipBehavior());
                return help;
            }
        };
        columns.add(column);

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.name")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                                     final IModel<PasswordAccountDto> rowModel) {
                item.add(new Label(componentId, new IModel<Object>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getObject() {
                        PasswordAccountDto dto = rowModel.getObject();
                        return dto.getDisplayName();
                    }
                }));
            }
        });

        column = new PropertyColumn(createStringResource("ChangePasswordPanel.resourceName"),
                PasswordAccountDto.F_RESOURCE_NAME);
        columns.add(column);

        IconColumn enabled = new IconColumn<PasswordAccountDto>(createStringResource("ChangePasswordPanel.enabled")){

            @Override
            protected DisplayType getIconDisplayType(IModel<PasswordAccountDto> rowModel) {
                String cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_UNKNOWN_COLORED;
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().isEnabled() != null) {
                    if (rowModel.getObject().isEnabled()) {
                        cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED;
                    } else {
                        cssClass = GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED;
                    }
                }
                return WebComponentUtil.createDisplayType(cssClass);
            }

            @Override
            public String getCssClass() {
                return "col-lg-3";
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
                return "col-lg-3";
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
                        return null;
                    }
                };
                ColumnResultPanel resultPanel = new ColumnResultPanel(componentId, resultStatusModel){
                    @Override
                    protected boolean isProjectionResult() {
                        return !rowModel.getObject().isMidpoint();
                    }
                };
                resultPanel.setOutputMarkupId(true);
                cellItem.add(resultPanel);
            }

            @Override
            public String getCssClass() {
                return "col-lg-3";
            }
        });

        return columns;
    }

    private void updatePropagatedAccountIconsCssClass(String cssClassName) {
        MyPasswordsDto dto = model.getObject();
        for (PasswordAccountDto passwordAccountDto : dto.getAccounts()) {
            if (passwordAccountDto.isPasswordOutbound()) {
                passwordAccountDto.setCssClass(cssClassName);
            }
        }
    }

    private void showHelpPerformed(AjaxRequestTarget target){
        getPageBase().showMainPopup(new HelpInfoPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("ChangePasswordPanel.helpInfo",
                        WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midpoint.default.system.name"))) {
            @Override
            protected void closePerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);

            }
        }, target);
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
