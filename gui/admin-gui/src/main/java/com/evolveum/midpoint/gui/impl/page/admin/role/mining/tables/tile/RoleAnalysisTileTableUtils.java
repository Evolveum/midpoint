/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;

import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

public class RoleAnalysisTileTableUtils {

    public static @NotNull IModel<List<Toggle<ViewToggle>>> initToggleItems(TileTablePanel<?, ?> table) {
        return new LoadableModel<>(false) {

            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

                ViewToggle object = table.getViewToggleModel().getObject();

                asList.setValue(ViewToggle.TABLE);
                asList.setActive(object == ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setValue(ViewToggle.TILE);
                asTile.setActive(object == ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
    }

    public static void initMemberCountColumn(@NotNull List<IColumn<RoleType, String>> columns, @NotNull PageBase pageBase) {
        columns.add(new AbstractColumn<>(
                pageBase.createStringResource("RoleAnalysisCandidateRoleTileTable.column.title.members")) {

            @Override
            public String getSortProperty() {
                return AssignmentHolderType.F_ASSIGNMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                Task task = pageBase.createSimpleTask("countRoleMembers");

                Integer membersCount = pageBase.getRoleAnalysisService()
                        .countUserTypeMembers(null, rowModel.getObject().getOid(),
                                task, task.getResult());

                item.add(new Label(componentId, membersCount));
            }

        });
    }

    public static void initNameColumn(@NotNull List<IColumn<RoleType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("ObjectType.name")) {

            @Override
            public String getSortProperty() {
                return ObjectType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {

                String name = rowModel.getObject().getName().toString();
                String oid = rowModel.getObject().getOid();

                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(name)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

                        ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                item.add(ajaxLinkPanel);
            }

        });
    }

    public static void initCreateTimeStampColumn(@NotNull List<IColumn<RoleType, String>> columns, @NotNull PageBase pageBase) {
        columns.add(new AbstractColumn<>(pageBase.createStringResource(
                "RoleAnalysisCandidateRoleTileTable.column.title.create.timestamp")) {

            @Override
            public String getSortProperty() {
                return AbstractRoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                RoleType object = rowModel.getObject();
                if (object == null || object.getMetadata() == null || object.getMetadata().getCreateTimestamp() == null) {
                    item.add(new Label(componentId, ""));
                } else {
                    item.add(new Label(componentId, resolveDateAndTime(object.getMetadata().getCreateTimestamp())));
                }
            }

        });
    }

    public static void initInducementCountColumn(@NotNull List<IColumn<RoleType, String>> columns, @NotNull PageBase pageBase) {
        columns.add(new AbstractColumn<>(pageBase.createStringResource(
                "RoleAnalysisCandidateRoleTileTable.column.title.inducements")) {

            @Override
            public String getSortProperty() {
                return AbstractRoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getInducement().size()));
            }

        });
    }

    public static void initIconColumn(@NotNull List<IColumn<RoleType, String>> columns, PageBase pageBase) {
        columns.add(new CompositedIconColumn<>(createStringResource("")) {

            @Override
            protected CompositedIcon getCompositedIcon(IModel<RoleType> rowModel) {
                if (rowModel == null || rowModel.getObject() == null || rowModel.getObject() == null) {
                    return new CompositedIconBuilder().build();
                }
                OperationResult result = new OperationResult("getIconDisplayType");

                return WebComponentUtil.createCompositeIconForObject(rowModel.getObject(),
                        result, pageBase);
            }
        });
    }

    public static @NotNull AjaxIconButton buildRefreshToggleTablePanel(String id, Consumer<AjaxRequestTarget> refreshHandler) {
        AjaxIconButton refreshTable = new AjaxIconButton(id,
                Model.of("fa fa-refresh"),
                Model.of()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                refreshHandler.accept(ajaxRequestTarget);
            }
        };

        refreshTable.setOutputMarkupId(true);
        refreshTable.add(AttributeModifier.replace("title",
                createStringResource("Refresh table")));
        refreshTable.add(new TooltipBehavior());

        return refreshTable;
    }

    public static @NotNull TogglePanel<ViewToggle> buildViewToggleTablePanel(String id,
            IModel<List<Toggle<ViewToggle>>> items,
            IModel<ViewToggle> viewToggleModel,
            Component component) {
        TogglePanel<ViewToggle> viewToggle = new TogglePanel<>(id, items) {

            @Override
            protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                viewToggleModel.setObject(item.getObject().getValue());
                items.getObject().forEach(toggle -> toggle.setActive(toggle.equals(item.getObject())));
                target.add(this);
                target.add(component);
            }
        };

        viewToggle.add(AttributeModifier.replace(TITLE_CSS, createStringResource("RoleAnalysisTable.change.view")));
        viewToggle.add(new TooltipBehavior());
        return viewToggle;
    }


}
