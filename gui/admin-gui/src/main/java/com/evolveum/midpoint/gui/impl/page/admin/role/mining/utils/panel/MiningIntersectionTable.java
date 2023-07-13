/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class MiningIntersectionTable extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    public MiningIntersectionTable(String id, List<IntersectionObject> miningSets) {
        super(id);
        RoleMiningProvider<IntersectionObject> provider = new RoleMiningProvider<>(
                this, new ListModel<>(miningSets) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<IntersectionObject> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(IntersectionObject.F_METRIC, SortOrder.DESCENDING);

        BoxedTablePanel<IntersectionObject> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(),
                null, true, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }



    public List<IColumn<IntersectionObject, String>> initColumns() {

        List<IColumn<IntersectionObject, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<IntersectionObject> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("metric")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getMetric()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("metric"));
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("type")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_TYPE;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getType()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("type"));
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("intersection.count")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getPoints().size()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("intersection.count"));
            }

        });
        columns.add(new AbstractExportableColumn<>(getHeaderTitle("occupancy.current")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getCurrentElements()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("occupancy.current"));
            }

        });


        columns.add(new AbstractExportableColumn<>(getHeaderTitle("occupancy.total")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId,
                        createStringResource("RoleMining.button.title.compute")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        this.setDefaultModel(Model.of(String.valueOf("0")));
                        item.setOutputMarkupId(true);
                        ajaxRequestTarget.add(item);
                    }
                };
                ajaxButton.setOutputMarkupId(true);
                ajaxButton.setEnabled(false);
                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                        + "justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                item.add(ajaxButton);

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("occupancy.total"));
            }

        });


        columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

            @Override
            public String getSortProperty() {
                return IntersectionObject.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<IntersectionObject> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<IntersectionObject>> item, String componentId,
                    IModel<IntersectionObject> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId, createStringResource("RoleMining.button.title.load")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onLoad(ajaxRequestTarget, rowModel);
                    }
                };

                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                        + "justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                ajaxButton.setOutputMarkupId(true);
                item.add(ajaxButton);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("display"));
            }

        });

        return columns;
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

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<IntersectionObject> rowModel){

    }
}
