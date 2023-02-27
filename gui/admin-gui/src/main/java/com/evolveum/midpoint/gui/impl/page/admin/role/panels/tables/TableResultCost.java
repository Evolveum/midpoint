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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CostResult;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.CandidateRoleDetailsPopup;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TableResultCost extends Panel {

    private static final String ID_DATATABLE = "datatable_extra_rbac";

    boolean candidateDetailsManage;

    public TableResultCost(String id, List<CostResult> costResultList, boolean modePermission, boolean modeRole,
            boolean candidateDetailsManage) {
        super(id);
        this.candidateDetailsManage = candidateDetailsManage;
        add(generateTableCR(costResultList, modePermission, modeRole));

    }

    public BoxedTablePanel<CostResult> generateTableCR(List<CostResult> costResultList, boolean modePermission,
            boolean modeRole) {

        RoleMiningProvider<CostResult> provider = new RoleMiningProvider<>(
                this, new ListModel<>(costResultList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<CostResult> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(CostResult.F_ROLE_COST, SortOrder.DESCENDING);

        BoxedTablePanel<CostResult> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRC(modePermission, modeRole),
                null, true, false);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<CostResult, String>> initColumnsRC(boolean modePermission, boolean modeRole) {

        List<IColumn<CostResult, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<CostResult> rowModel) {
                return () -> rowModel.getObject() != null;
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<CostResult> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResult.F_ROLE_KEY;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

                CostResult costResult = rowModel.getObject();
                int roleKey = costResult.getRoleKey();

                if (isCandidateDetailsManage()) {
                    item.add(new AjaxLinkPanel(componentId, createStringResource(String.valueOf(roleKey))) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                            CandidateRoleDetailsPopup candidateRoleDetailsPopup = new CandidateRoleDetailsPopup(
                                    getPageBase().getMainPopupBodyId(),
                                    createStringResource("Cost reduce preview"), costResult, modePermission, modeRole);
                            TableResultCost.this.getPageBase().showMainPopup(candidateRoleDetailsPopup, ajaxRequestTarget);

                        }
                    });
                } else {
                    item.add(new Label(componentId, createStringResource(String.valueOf(roleKey))));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("Candidate Key"));
            }

        });

        if (modeRole) {
            columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

                @Override
                public String getSortProperty() {
                    return CostResult.F_ROLE_DEGREE;
                }

                @Override
                public IModel<?> getDataModel(IModel<CostResult> iModel) {
                    return null;
                }

                @Override
                public boolean isSortable() {
                    return true;
                }

                @Override
                public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                        IModel<CostResult> rowModel) {

                    List<RoleType> candidateRoles = rowModel.getObject().getCandidateRole().getCandidateRoles();
                    RepeatingView repeatingView = new RepeatingView(componentId);

                    for (int i = 0; i < candidateRoles.size(); i++) {
                        int finalI = i;
                        repeatingView.add(new AjaxLinkPanel(repeatingView.newChildId(),
                                createStringResource(candidateRoles.get(finalI).getName() + ", ")) {
                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                RoleType object = candidateRoles.get(finalI);
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
                    return new Label(componentId,
                            createStringResource("Group"));
                }

                @Override
                public String getCssClass() {
                    return " overflow-auto ";
                }

            });

        }

        if (modePermission) {
            columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

                @Override
                public String getSortProperty() {
                    return CostResult.F_ROLE_DEGREE;
                }

                @Override
                public IModel<?> getDataModel(IModel<CostResult> iModel) {
                    return null;
                }

                @Override
                public boolean isSortable() {
                    return true;
                }

                @Override
                public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                        IModel<CostResult> rowModel) {

                    List<AuthorizationType> candidateRoles = rowModel.getObject().getCandidateRole().getCandidatePermissions();
                    RepeatingView repeatingView = new RepeatingView(componentId);

                    for (AuthorizationType candidateRole : candidateRoles) {
                        repeatingView.add(new Label(repeatingView.newChildId(),
                                createStringResource(candidateRole.getName() + ", ")));
                    }

                    item.add(repeatingView);
                }

                @Override
                public Component getHeader(String componentId) {
                    return new Label(componentId,
                            createStringResource("Group"));
                }

                @Override
                public String getCssClass() {
                    return " overflow-auto ";
                }

            });

        }

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResult.F_ROLE_DEGREE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

                List<RoleType> equivalentRoles = rowModel.getObject().getEquivalentRoles();

                if (equivalentRoles.size() == 0) {
                    item.add(new Label(componentId, Model.of("UNIQUE")));
                } else {
                    RepeatingView repeatingView = new RepeatingView(componentId);

                    for (int i = 0; i < equivalentRoles.size(); i++) {
                        int finalI = i;
                        repeatingView.add(new AjaxLinkPanel(repeatingView.newChildId(),
                                createStringResource(equivalentRoles.get(finalI).getName() + ", ")) {
                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                RoleType object = equivalentRoles.get(finalI);
                                PageParameters parameters = new PageParameters();
                                parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                                ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                            }
                        });
                    }

                    item.add(repeatingView);
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("Equivalent"));
            }

            @Override
            public String getCssClass() {
                return " overflow-auto ";
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResult.F_ROLE_KEY;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

                List<UrType> candidateUserRole = rowModel.getObject().getCandidateUserRole();
                RepeatingView repeatingView2 = new RepeatingView(componentId);

                for (int i = 0; i < candidateUserRole.size(); i++) {
                    int finalI = i;
                    repeatingView2.add(new AjaxLinkPanel(repeatingView2.newChildId(),
                            createStringResource(
                                    candidateUserRole.get(finalI).getUserObjectType().getName() + ", ")) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                            UserType object = candidateUserRole.get(finalI).getUserObjectType();
                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                            ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                        }
                    });
                }

                item.add(repeatingView2).add(new AttributeAppender("class", " row"));

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("Affect"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResult.F_ROLE_DEGREE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

                int roleDegree = rowModel.getObject().getRoleDegree();

                item.add(new Label(componentId, createStringResource(String.valueOf(roleDegree))));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("Candidate Degree"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResult.F_ROLE_COST;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

                double roleCost = rowModel.getObject().getRoleCost();

                item.add(new Label(componentId, createStringResource(String.valueOf((Math.round(roleCost * 100.0) / 100.0)))));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("Candidate support"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return CostResult.F_REDUCED_VALUE;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

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
                return CostResult.F_ROLE_KEY;
            }

            @Override
            public IModel<?> getDataModel(IModel<CostResult> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<CostResult>> item, String componentId,
                    IModel<CostResult> rowModel) {

                CostResult costResult = rowModel.getObject();


                    AjaxButton ajaxButton = new AjaxButton(componentId, Model.of("Manage")) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                            if(isCandidateDetailsManage()) {
                                CandidateRoleDetailsPopup candidateRoleDetailsPopup = new CandidateRoleDetailsPopup(
                                        getPageBase().getMainPopupBodyId(),
                                        createStringResource("Cost reduce preview"), costResult,
                                        modePermission, modeRole);
                                TableResultCost.this.getPageBase().showMainPopup(candidateRoleDetailsPopup, ajaxRequestTarget);
                            }
                        }
                    };

                    item.add(ajaxButton.add(new AttributeAppender("class", " btn btn-primary btn-sm ")));
                }


            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("Operation"));
            }

        });

        return columns;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public boolean isCandidateDetailsManage() {
        return candidateDetailsManage;
    }

}
