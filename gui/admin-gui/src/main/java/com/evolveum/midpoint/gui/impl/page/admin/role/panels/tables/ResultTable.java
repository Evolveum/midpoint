/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.ResultData;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.UserCostDetailsPopup;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.RoleMiningBoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ResultTable extends Panel {

    private static final String ID_DATATABLE_RBAM_UA = "datatable_extra_rbac";

    public ResultTable(String id, List<ResultData> resultDatumTypes, boolean modePermission, boolean modeRole) {
        super(id);

        add(generateTableRBAM(resultDatumTypes, modePermission, modeRole).setOutputMarkupId(true));
    }

    public RoleMiningBoxedTablePanel<ResultData> generateTableRBAM(
            List<ResultData> resultDatumTypes, boolean modePermission, boolean modeRole) {

        RoleMiningProvider<ResultData> provider = new RoleMiningProvider<>(
                this, new ListModel<>(resultDatumTypes) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<ResultData> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(ResultData.F_ROLE_COST, SortOrder.DESCENDING);

        RoleMiningBoxedTablePanel<ResultData> table = new RoleMiningBoxedTablePanel<>(
                ID_DATATABLE_RBAM_UA, provider, initColumnsRBAM(modePermission, modeRole),
                null, false, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();
        return table;
    }

    public List<IColumn<ResultData, String>> initColumnsRBAM(boolean modePermission, boolean modeRole) {

        List<IColumn<ResultData, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<ResultData> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public IModel<?> getDataModel(IModel<ResultData> iModel) {
                return null;
            }

            @Override
            public String getSortProperty() {
                return ResultData.F_NAME_USER_TYPE;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ResultData>> item, String componentId,
                    IModel<ResultData> rowModel) {

                item.add(new AjaxLinkPanel(componentId,
                        createStringResource(String.valueOf(rowModel.getObject().getUserObjectType().getName()))) {
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
                return new Label(componentId, createStringResource("RoleMining.name.column"));
            }

        });

        IColumn<ResultData, String> column;

        column = new AbstractExportableColumn<>(
                createStringResource("Permission")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                    String componentId, IModel<ResultData> model) {

                List<AuthorizationType> userAuthorizations = model.getObject().getUserAuthorizations();

                RepeatingView repeatingView = new RepeatingView(componentId);

                for (AuthorizationType userAuthorization : userAuthorizations) {
                    repeatingView.add(new Label(repeatingView.newChildId(), userAuthorization.getName()));
                }
                cellItem.add(repeatingView);
            }

            @Override
            public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("Originally roles")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                    String componentId, IModel<ResultData> model) {

                List<RoleType> userRoles = model.getObject().getRoleAssign();

                RepeatingView repeatingView = new RepeatingView(componentId);

                int bound = userRoles.size();
                for (int i = 0; i < bound; i++) {
                    int finalI = i;
                    repeatingView.add(new AjaxLinkPanel(repeatingView.newChildId(),
                            createStringResource(userRoles.get(finalI).getName().toString())) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                            RoleType object = userRoles.get(finalI);
                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }
                    });
                }

                cellItem.add(repeatingView);
            }

            @Override
            public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        if (modePermission && !modeRole) {
            column = new AbstractExportableColumn<>(
                    createStringResource("Candidate role(s)")) {

                @Override
                public boolean isSortable() {
                    return false;
                }

                @Override
                public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                        String componentId, IModel<ResultData> model) {

                    List<List<AuthorizationType>> candidateRoles = model.getObject().getCandidatePermissionRole();
                    if (candidateRoles == null) {
                        return;
                    }
                    List<Integer> indices = model.getObject().getIndices();

                    RepeatingView repeatingView = new RepeatingView(componentId);

                    for (int i = 0; i < candidateRoles.size(); i++) {

                        List<AuthorizationType> authorizationTypes = candidateRoles.get(i);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("[");
                        for (AuthorizationType authorizationType : authorizationTypes) {
                            stringBuilder.append(authorizationType.getName()).append(", ");
                        }
                        stringBuilder.append("]");

                        if (indices.contains(i)) {
                            repeatingView.add(new Label(repeatingView.newChildId(),
                                    String.valueOf(stringBuilder).replaceAll(", ]", "]"))
                                    .add(new AttributeAppender("class", " text-success")));
                        } else {
                            repeatingView.add(new Label(repeatingView.newChildId(),
                                    String.valueOf(stringBuilder).replaceAll(", ]", "]"))
                                    .add(new AttributeAppender("class", " text-danger")));
                        }
                    }

                    cellItem.add(repeatingView);
                }

                @Override
                public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                    return Model.of(" ");
                }

            };
            columns.add(column);
        } else if (modeRole && !modePermission) {
            column = new AbstractExportableColumn<>(
                    createStringResource("Candidate roles permission")) {

                @Override
                public boolean isSortable() {
                    return false;
                }

                @Override
                public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                        String componentId, IModel<ResultData> model) {

                    List<List<RoleType>> candidateRoles = model.getObject().getCandidateRoles();
                    List<Integer> indices = model.getObject().getIndices();

                    RepeatingView repeatingView = new RepeatingView(componentId);

                    if (candidateRoles == null || candidateRoles.size() == 0) {
                        cellItem.add(new Label(componentId, " "));
                    }

                    if (candidateRoles != null) {
                        for (int i = 0; i < candidateRoles.size(); i++) {

                            List<RoleType> authorizationTypes = candidateRoles.get(i);
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("[");
                            for (RoleType authorizationType : authorizationTypes) {
                                stringBuilder.append(authorizationType.getName()).append(", ");
                            }
                            stringBuilder.append("]");

                            if (indices.contains(i)) {
                                repeatingView.add(new Label(repeatingView.newChildId(),
                                        String.valueOf(stringBuilder).replaceAll(", ]", "]"))
                                        .add(new AttributeAppender("class", " text-success")));
                            } else {
                                repeatingView.add(new Label(repeatingView.newChildId(),
                                        String.valueOf(stringBuilder).replaceAll(", ]", "]"))
                                        .add(new AttributeAppender("class", " text-danger")));
                            }
                        }
                    }

                    cellItem.add(repeatingView);
                }

                @Override
                public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                    return Model.of(" ");
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Possible reduce value")) {

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public String getSortProperty() {
                return ResultData.F_ROLE_COST;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                    String componentId, IModel<ResultData> model) {

                cellItem.add(new Label(componentId, (Math.round(model.getObject().getReduceValue() * 100.0) / 100.0) + "%"));

            }

            @Override
            public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("Status")) {

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public String getSortProperty() {
                return ResultData.F_STATUS;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                    String componentId, IModel<ResultData> model) {

                if (!model.getObject().isCorrelateStatus()) {
                    cellItem.add(new Label(componentId, "UNPROCESSED"));
                } else {
                    cellItem.add(new Label(componentId, "PROCESSED"));
                }
            }

            @Override
            public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("Operation")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ResultData>> cellItem,
                    String componentId, IModel<ResultData> model) {

                AjaxButton ajaxButton = new AjaxButton(componentId, Model.of("Manage")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserCostDetailsPopup candidateRoleDetailsPopupable = new UserCostDetailsPopup(
                                getPageBase().getMainPopupBodyId(),
                                createStringResource("Cost reduce preview"),
                                Collections.singletonList(model.getObject().getUserObjectType()), modePermission, modeRole,
                                true,getPageBase());
                        getPageBase().showMainPopup(candidateRoleDetailsPopupable, ajaxRequestTarget);
                    }
                };

                cellItem.add(ajaxButton.add(new AttributeAppender("class", " btn btn-primary btn-sm ")));
            }

            @Override
            public IModel<String> getDataModel(IModel<ResultData> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }


    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
