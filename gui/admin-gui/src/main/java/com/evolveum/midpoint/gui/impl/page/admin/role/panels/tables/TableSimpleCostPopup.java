/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CostResultSingle;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.MergeOperations;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TableSimpleCostPopup extends Panel {

    private static final String ID_DATATABLE = "datatable_extra_rbac";
    private static final String ID_MESSAGE_FORM = "form_message";
    private static final String ID_MESSAGE_PANEL = "warningFeedback";


    public TableSimpleCostPopup(String id, List<CostResultSingle> costResultList) {
        super(id);

        Form<?> messageForm = new Form<>(ID_MESSAGE_FORM);
        messageForm.setOutputMarkupId(true);
        add(messageForm);
        MessagePanel<?> warningMessage = new MessagePanel<>(ID_MESSAGE_PANEL, MessagePanel.MessagePanelType.INFO, getWarningMessageModel()) {
        };
        warningMessage.setOutputMarkupId(true);
        warningMessage.setOutputMarkupPlaceholderTag(true);
        warningMessage.setVisible(false);
        messageForm.add(warningMessage);

        messageForm.add(generateTableCR(costResultList));

    }

    protected MessagePanel<?> getMessagePanel() {
        return (MessagePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_MESSAGE_FORM, ID_MESSAGE_PANEL));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public BoxedTablePanel<CostResultSingle> generateTableCR(List<CostResultSingle> costResultList) {

        RoleMiningProvider<CostResultSingle> provider = new RoleMiningProvider<>(
                this, new ListModel<>(costResultList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<CostResultSingle> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(CostResultSingle.F_ROLE_COST, SortOrder.ASCENDING);

        BoxedTablePanel<CostResultSingle> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRC(costResultList),
                null, true, false);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<CostResultSingle, String>> initColumnsRC(List<CostResultSingle> costResultList) {

        List<IColumn<CostResultSingle, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<CostResultSingle> rowModel) {
                return () -> rowModel.getObject() != null;
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<CostResultSingle> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResultSingle.F_NAME_USER_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResultSingle> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResultSingle>> item, String componentId,
                    IModel<CostResultSingle> rowModel) {

                UserType userObjectType = rowModel.getObject().getUserObjectType();

                item.add(new AjaxLinkPanel(componentId, createStringResource(String.valueOf(userObjectType.getName()))) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserType object = rowModel.getObject().getUserObjectType();
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                        ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("User"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResultSingle.F_NAME_USER_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResultSingle> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResultSingle>> item, String componentId,
                    IModel<CostResultSingle> rowModel) {

                List<RoleType> userOriginalRoles = rowModel.getObject().getUserOriginalRoles();

                RepeatingView repeatingView = new RepeatingView(componentId);
                for (int i = 0; i < userOriginalRoles.size(); i++) {
                    int finalI = i;
                    repeatingView.add(new AjaxLinkPanel(repeatingView.newChildId(),
                            createStringResource(String.valueOf(userOriginalRoles.get(finalI).getName()))) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                            RoleType object = userOriginalRoles.get(finalI);
                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }
                    });
                }

                item.add(repeatingView);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("Original Roles"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResultSingle.F_NAME_USER_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResultSingle> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResultSingle>> item, String componentId,
                    IModel<CostResultSingle> rowModel) {

                List<String> userPossibleRoles = rowModel.getObject().getUserPossibleRoles();

                RepeatingView repeatingView = new RepeatingView(componentId);

                for (String userPossibleRole : userPossibleRoles) {
                    repeatingView.add(new Label(repeatingView.newChildId(),
                            createStringResource(String.valueOf(userPossibleRole))));
                }

                item.add(repeatingView);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("Possible Roles"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResultSingle.F_ROLE_COST;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResultSingle> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResultSingle>> item, String componentId,
                    IModel<CostResultSingle> rowModel) {

                double reduceValue = rowModel.getObject().getReduceValue();

                item.add(new Label(componentId, createStringResource((Math.round(reduceValue * 100.0) / 100.0) + "%")));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("Reduced value"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResultSingle.F_NAME_USER_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResultSingle> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResultSingle>> item, String componentId,
                    IModel<CostResultSingle> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId, Model.of("Merge")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        UserType manageUser = rowModel.getObject().getUserObjectType();
                        List<String> userPossibleRoles = rowModel.getObject().getUserPossibleRoles();
                        List<RoleType> userOriginalRoles = rowModel.getObject().getUserOriginalRoles();
                        List<PrismObject<RoleType>> roleForDelete = new ArrayList<>();
                        for (RoleType roleType : userOriginalRoles) {
                            if (!userPossibleRoles.contains(roleType.getName().toString())) {
                                roleForDelete.add(roleType.asPrismObject());
                            }
                        }
                        new MergeOperations(getPageBase()).mergeProcess(roleForDelete, manageUser.asPrismObject(),
                                rowModel.getObject().getCandidateRole());

                        //TODO The current functionality of replacing roles with candidate roles is used for basic processing
                        // of results. Another run of the prune algorithm is required to record and track changes. This process will need to be reviewed later.
                        //rowModel.getObject().setApplied(false);

                        ajaxRequestTarget.add(getMessagePanel().replaceWith(getMessagePanel().setVisible(true)));
                        this.setDefaultModel(Model.of("Applied"));
                        this.add(new AttributeAppender("class", " btn btn-primary btn-lg disabled"));
                        this.setEnabled(false);

                        ajaxRequestTarget.add(this);
                    }
                };

                if (rowModel.getObject().isApplied()) {
                    ajaxButton.setDefaultModel(Model.of("Applied"));
                    ajaxButton.add(new AttributeAppender("class", " btn btn-primary btn-lg disabled"));
                    ajaxButton.setEnabled(false);
                }
                item.add(ajaxButton.add(new AttributeAppender("class", " btn btn-primary btn-sm ")).setOutputMarkupId(true));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("Operation"));
            }

        });

        return columns;
    }

    protected IModel<String> getWarningMessageModel() {
        return new StringResourceModel("RoleMining.merge.role.mining.warning");
    }

}
