/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.RoleMiningBoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TableUR extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    public TableUR(String id, List<UrType> userRoleList, List<PrismObject<RoleType>> rolePrismObjectList,boolean sortable) {
        super(id);
        add(generateTableRM(userRoleList, rolePrismObjectList,sortable));

    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public RoleMiningBoxedTablePanel<UrType> generateTableRM(List<UrType> userRoleList, List<PrismObject<RoleType>> rolePrismObjectList, boolean sortable) {

        RoleMiningProvider<UrType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(userRoleList) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<UrType> object) {
                super.setObject(object);
            }

        }, sortable);

        if(sortable) {
            provider.setSort(UrType.F_NAME_USER_TYPE, SortOrder.ASCENDING);
        }
        RoleMiningBoxedTablePanel<UrType> table = new RoleMiningBoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRM(rolePrismObjectList),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public List<IColumn<UrType, String>> initColumnsRM(List<PrismObject<RoleType>> rolePrismObjectList) {

        List<IColumn<UrType, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<UrType> rowModel) {
                return () -> rowModel.getObject() != null;
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
            protected DisplayType getIconDisplayType(IModel<UrType> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return UrType.F_NAME_USER_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<UrType> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UrType>> item, String componentId,
                    IModel<UrType> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                String header = rowModel.getObject().getUserObjectType().getName().toString();
                if (header == null) {
                    header = "unknown";
                }

                item.add(new AjaxLinkPanel(componentId, createStringResource(header)) {
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
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<UrType, String> column;

        for (int i = 0; i < rolePrismObjectList.size(); i++) {
            int finalI = i;

            column = new AbstractExportableColumn<>(
                    createStringResource(rolePrismObjectList.get(i).getName().toString())) {

                @Override
                public void populateItem(Item<ICellPopulator<UrType>> cellItem,
                        String componentId, IModel<UrType> model) {

                    tableStyle(cellItem);

                    List<RoleType> roleMembers = model.getObject().getRoleMembers();
                    if (roleMembers.contains(rolePrismObjectList.get(finalI).asObjectable())) {
                        filledCell(cellItem, componentId);
                    } else {
                        emptyCell(cellItem, componentId);
                    }
                }

                @Override
                public IModel<String> getDataModel(IModel<UrType> rowModel) {
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
