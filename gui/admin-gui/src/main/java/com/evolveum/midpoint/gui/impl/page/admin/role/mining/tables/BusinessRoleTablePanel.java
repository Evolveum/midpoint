/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class BusinessRoleTablePanel extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    public BusinessRoleTablePanel(String id,
            List<RoleType> roles) {
        super(id);


        RoleMiningProvider<RoleType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roles) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleType> object) {
                super.setObject(object);
            }
        }, false);

//        provider.setSort(RoleType.F_NAME.toString(), SortOrder.ASCENDING);

        BoxedTablePanel<RoleType> table = generateTable(provider);
        add(table);
    }

    public BoxedTablePanel<RoleType> generateTable(RoleMiningProvider<RoleType> provider) {

        BoxedTablePanel<RoleType> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns());
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<RoleType, String>> initColumns() {

        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleType> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });






        columns.add(new AbstractColumn<>(createStringResource("ObjectType.name")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {

                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(rowModel.getObject().getName())){
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getOid());

                        ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                item.add(ajaxLinkPanel);
            }

        });


        columns.add(new AbstractColumn<>(createStringResource("Activation")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getActivation()));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Relation")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getIdentifier()));
            }

        });

        return columns;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

}
