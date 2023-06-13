/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.MiningSet;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.details.DetailsPanel;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.visit.IVisitor;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TableJCF extends Panel {

    public List<MiningSet> getSelectedItems() {
        List<MiningSet> objects = new ArrayList<>();
        getDataTable().visitChildren(SelectableDataTable.SelectableRowItem.class,
                (IVisitor<SelectableDataTable.SelectableRowItem<SelectableBean<MiningSet>>, Void>) (row, visit) -> {
                    if (row.getModelObject().isSelected()) {
                        objects.add(row.getModel().getObject().getValue());
                    }
                });
        return objects;
    }

    HashSet<MiningSet> selectedMiningSet = new HashSet<>();

    private static final String ID_DATATABLE = "datatable_extra";

    public TableJCF(String id, List<MiningSet> miningSets, List<PrismObject<RoleType>> rolePrismObjectList, boolean sortable, boolean enablePopup) {
        super(id);

        AjaxButton ajaxButton = new AjaxButton("id", Model.of("Check")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };

        ajaxButton.setOutputMarkupId(true);
        add(ajaxButton);
        BoxedTablePanel<MiningSet> components = generateTableRM(ID_DATATABLE, miningSets, rolePrismObjectList, enablePopup, sortable);
        components.setOutputMarkupId(true);
        add(components);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    RoleMiningProvider<MiningSet> provider;

    public BoxedTablePanel<MiningSet> generateTableRM(String id, List<MiningSet> miningSets,
            List<PrismObject<RoleType>> rolePrismObjectList, boolean enablePopup, boolean sortable) {

        provider = new RoleMiningProvider<>(
                this, new ListModel<>(miningSets) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<MiningSet> object) {
                super.setObject(object);
            }

        }, sortable);

        if (sortable) {
            provider.setSort(MiningSet.F_RATION, SortOrder.DESCENDING);
        }

        provider.getModel().getObject().get(0).setSelected(true);
        miningSets.get(0).setSelected(true);
        BoxedTablePanel<MiningSet> table = new BoxedTablePanel<>(
                id, provider, initColumnsRM(rolePrismObjectList, miningSets, enablePopup),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public List<IColumn<MiningSet, String>> initColumnsRM(List<PrismObject<RoleType>> rolePrismObjectList, List<MiningSet> miningSets, boolean isPopup) {

        List<IColumn<MiningSet, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {

            @Override
            protected void processBehaviourOfCheckBox(IsolatedCheckBoxPanel check, IModel<MiningSet> rowModel) {
                super.processBehaviourOfCheckBox(check, rowModel);
            }

            @Override
            protected IModel<Boolean> getEnabled(IModel<MiningSet> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            protected void onUpdateRow(Item<ICellPopulator<MiningSet>> cellItem, AjaxRequestTarget target, DataTable table,
                    IModel<MiningSet> rowModel, IModel<Boolean> selected) {
                if (selected.getObject()) {
                    selectedMiningSet.add(rowModel.getObject());
                } else {
                    selectedMiningSet.remove(rowModel.getObject());
                }
                super.onUpdateRow(cellItem, target, table, rowModel, selected);

            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<MiningSet> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Id")) {

            @Override
            public String getSortProperty() {
                return MiningSet.F_ID;
            }

            @Override
            public IModel<?> getDataModel(IModel<MiningSet> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningSet>> item, String componentId,
                    IModel<MiningSet> rowModel) {

                item.add(AttributeAppender.replace("class", " d-flex justify-content-center"));
                item.add(new AttributeAppender("style", " width:150px; border: 1px solid rgb(244, 244, 244)"));

                item.add(new Label(componentId, rowModel.getObject().getId()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Id")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Group")) {

            @Override
            public String getSortProperty() {
                return MiningSet.F_GROUP;
            }

            @Override
            public IModel<?> getDataModel(IModel<MiningSet> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningSet>> item, String componentId,
                    IModel<MiningSet> rowModel) {

                item.add(AttributeAppender.replace("class", " d-flex justify-content-center"));
                item.add(new AttributeAppender("style", " width:150px; border: 1px solid rgb(244, 244, 244)"));

                item.add(new Label(componentId, rowModel.getObject().getGroupIdentifier()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Group")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Ration")) {

            @Override
            public String getSortProperty() {
                return MiningSet.F_RATION;
            }

            @Override
            public IModel<?> getDataModel(IModel<MiningSet> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningSet>> item, String componentId,
                    IModel<MiningSet> rowModel) {
                item.add(AttributeAppender.replace("class", " d-flex justify-content-center"));
                item.add(new AttributeAppender("style", " width:150px;border: 1px solid rgb(244, 244, 244)"));

                item.add(new Label(componentId, rowModel.getObject().getGroupOverlapRation()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Ration")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Members")) {

            @Override
            public String getSortProperty() {
                return MiningSet.F_USER_SIZE;
            }

            @Override
            public IModel<?> getDataModel(IModel<MiningSet> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningSet>> item, String componentId,
                    IModel<MiningSet> rowModel) {
                item.add(AttributeAppender.replace("class", " d-flex justify-content-center"));
                item.add(new AttributeAppender("style", " width:150px;border: 1px solid rgb(244, 244, 244)"));

                AjaxButton ajaxButton = new AjaxButton(componentId, Model.of(String.valueOf(rowModel.getObject().getUsers().size()))) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        DetailsPanel detailsPanel = new DetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Members"), rowModel.getObject().getUsers(), null) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);
                    }
                };

                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                ajaxButton.setOutputMarkupId(true);
                ajaxButton.setEnabled(isPopup);
                item.add(ajaxButton);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Members")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Threshold")) {

            @Override
            public String getSortProperty() {
                return MiningSet.F_ROLES_BY_TRESHOLD;
            }

            @Override
            public IModel<?> getDataModel(IModel<MiningSet> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningSet>> item, String componentId,
                    IModel<MiningSet> rowModel) {
                item.add(AttributeAppender.replace("class", " d-flex justify-content-center"));
                item.add(new AttributeAppender("style", " width:150px;border: 1px solid rgb(244, 244, 244)"));

                AjaxButton ajaxButton = new AjaxButton(componentId, Model.of(String.valueOf(rowModel.getObject().getRolesByThresholdId().size()))) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        List<Integer> rolesByThresholdId = rowModel.getObject().getRolesByThresholdId();
                        List<MiningSet> miningSetGroup = new ArrayList<>();
                        for (MiningSet miningSet : miningSets) {
                            for (Integer integer : rolesByThresholdId) {
                                if (miningSet.getGroupIdentifier() == integer) {
                                    miningSetGroup.add(miningSet);
                                }
                            }
                        }

                        DetailsPanel detailsPanel = new DetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Threshold"), rowModel.getObject().getUsers(), miningSetGroup) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);

                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);

                    }
                };
                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                ajaxButton.setOutputMarkupId(true);
                ajaxButton.setEnabled(isPopup);

                item.add(ajaxButton);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Threshold")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<MiningSet, String> column;

        for (int i = 0; i < rolePrismObjectList.size(); i++) {
            int finalI = i;

            column = new AbstractExportableColumn<>(
                    createStringResource(rolePrismObjectList.get(i).getName().toString())) {

                @Override
                public void populateItem(Item<ICellPopulator<MiningSet>> cellItem,
                        String componentId, IModel<MiningSet> model) {

                    tableStyle(cellItem);

                    List<String> roleMembers = model.getObject().getRoles();
                    if (roleMembers.contains(rolePrismObjectList.get(finalI).getOid())) {
                        filledCell(cellItem, componentId);
                    } else {
                        emptyCell(cellItem, componentId);
                    }
                }

                @Override
                public IModel<String> getDataModel(IModel<MiningSet> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    String headerName;
                    if (rolePrismObjectList.get(finalI).getName() == null) {
                        headerName = "unknown";
                    } else {
                        headerName = rolePrismObjectList.get(finalI).getName().toString();
                    }

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            RoleType object = rolePrismObjectList.get(finalI).asObjectable();
                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        return columns;
    }

    private void tableStyle(@NotNull Item<?> cellItem) {
        cellItem.getParent().getParent().add(AttributeAppender.replace("class", " d-flex"));
        cellItem.getParent().getParent().add(AttributeAppender.replace("style", " height:40px"));
        cellItem.add(new AttributeAppender("style", " width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    private void emptyCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new Label(componentId, " "));
    }

    private void filledCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-dark"));
        cellItem.add(new Label(componentId, " "));
    }

}
