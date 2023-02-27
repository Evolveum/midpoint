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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.RuType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TableRU extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    public TableRU(String id, List<RuType> rpList, List<PrismObject<UserType>> userTypeList) {
        super(id);
        add(generateTableRU(rpList, userTypeList));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public BoxedTablePanel<RuType> generateTableRU(List<RuType> rpList, List<PrismObject<UserType>> userTypeList) {

        RoleMiningProvider<RuType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(rpList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RuType> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(RuType.F_NAME_ROLE_TYPE, SortOrder.ASCENDING);

        BoxedTablePanel<RuType> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRU(userTypeList),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public List<IColumn<RuType, String>> initColumnsRU(List<PrismObject<UserType>> userTypeList) {

        List<IColumn<RuType, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<RuType> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<RuType> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return RuType.F_NAME_ROLE_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<RuType> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RuType>> item, String componentId,
                    IModel<RuType> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));
                String header;
                if (rowModel.getObject().getRoleObjectType().getName() == null) {
                    header = "unknown";
                } else {
                    header = rowModel.getObject().getRoleObjectType().getName().toString();
                }

                item.add(new AjaxLinkPanel(componentId, createStringResource(header)) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleType object = rowModel.getObject().getRoleObjectType();
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                        ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
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

        IColumn<RuType, String> column;

        for (int i = 0; i < userTypeList.size(); i++) {
            int finalI = i;

            String header;
            if (userTypeList.get(i).getName() == null) {
                header = "unknown";
            } else {
                header = userTypeList.get(i).getName().toString();
            }

            column = new AbstractExportableColumn<>(
                    createStringResource(header)) {

                @Override
                public void populateItem(Item<ICellPopulator<RuType>> cellItem,
                        String componentId, IModel<RuType> model) {

                    tableStyle(cellItem);

                    List<PrismObject<UserType>> roleMembers = model.getObject().getRoleUserMembers();
                    if (roleMembers.contains(userTypeList.get(finalI))) {
                        filledCell(cellItem, componentId);
                    } else {
                        emptyCell(cellItem, componentId);
                    }
                }

                @Override
                public IModel<String> getDataModel(IModel<RuType> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    String headerName;
                    if (userTypeList.get(finalI).getName() == null) {
                        headerName = "unknown";
                    } else {
                        headerName = userTypeList.get(finalI).getName().toString();
                    }

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            UserType object = userTypeList.get(finalI).asObjectable();
                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                            ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
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

    private void tableStyle(Item<?> cellItem) {
        cellItem.getParent().getParent().add(AttributeAppender.replace("class", " d-flex"));
        cellItem.getParent().getParent().add(AttributeAppender.replace("style", " height:40px"));
        cellItem.add(new AttributeAppender("style", " width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    private void emptyCell(Item<?> cellItem, String componentId) {
        cellItem.add(new Label(componentId, " "));
    }

    private void filledCell(Item<?> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-dark"));
        cellItem.add(new Label(componentId, " "));
    }

}
