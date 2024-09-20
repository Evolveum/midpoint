/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component.NumberFormatSelectorPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

public class RoleAnalysisClusteringAttributeTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";

    boolean isSimplePanel;

    public RoleAnalysisClusteringAttributeTable(
            @NotNull String id,
            ListModel<ClusteringAttributeRuleType> selectedObject,
            boolean isSimplePanel) {
        super(id);

        this.isSimplePanel = isSimplePanel;

        RoleMiningProvider<ClusteringAttributeRuleType> provider = new RoleMiningProvider<>(
                this, selectedObject, false);

        BoxedTablePanel<ClusteringAttributeRuleType> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns()) {

            @Override
            protected boolean isPagingVisible() {
                return !isSimplePanel;
            }

            @Override
            protected @NotNull WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public List<IColumn<ClusteringAttributeRuleType, String>> initColumns() {

        List<IColumn<ClusteringAttributeRuleType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<ClusteringAttributeRuleType> rowModel) {
                return createDisplayType(GuiStyleConstants.CLASS_TASK_ACTIVITY_ICON);
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ClusteringAttributeRuleType>> item, String componentId,
                    IModel<ClusteringAttributeRuleType> rowModel) {
                String attributeIdentifier = rowModel.getObject().getAttributeIdentifier();
                item.add(new Label(componentId, attributeIdentifier));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("Identifier"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ClusteringAttributeRuleType>> item, String componentId,
                    IModel<ClusteringAttributeRuleType> rowModel) {
                if (rowModel.getObject() != null) {
                    NumberFormatSelectorPanel field = new NumberFormatSelectorPanel(componentId,
                            Model.of(rowModel.getObject().getWeight())) {
                        @Override
                        public DisplayType getImage() {
                            DisplayType displayType = new DisplayType();
                            IconType iconType = new IconType();
                            iconType.setCssClass("fa fa-cube");
                            displayType.setIcon(iconType);
                            return displayType;
                        }

                        @Override
                        public void onChangePerform(Double newValue) {
                            rowModel.getObject().setWeight(newValue);
                        }
                    };

                    field.setEnabled(true);
                    item.add(field);
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("Weight"));

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<ClusteringAttributeRuleType>> item, String componentId,
                    IModel<ClusteringAttributeRuleType> rowModel) {
                if (rowModel.getObject() != null) {
                    NumberFormatSelectorPanel field = new NumberFormatSelectorPanel(componentId,
                            Model.of(rowModel.getObject().getSimilarity())) {
                        @Override
                        public RangeValidator<Double> getRangeValidator() {
                            return RangeValidator.range(0.0, 100.0);
                        }

                        @Override
                        public DisplayType getImage() {
                            DisplayType displayType = new DisplayType();
                            IconType iconType = new IconType();
                            iconType.setCssClass("fa fa-percent");
                            displayType.setIcon(iconType);
                            return displayType;
                        }

                        @Override
                        public void onChangePerform(Double newValue) {
                            rowModel.getObject().setWeight(newValue);
                        }
                    };
                    field.setEnabled(rowModel.getObject().isIsMultiValue());
                    item.add(field);
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("Similarity"));
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

    protected void onRefresh(AjaxRequestTarget target) {

    }

}
