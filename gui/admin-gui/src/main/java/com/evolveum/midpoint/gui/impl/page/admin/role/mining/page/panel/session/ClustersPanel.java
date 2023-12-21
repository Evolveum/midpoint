/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChannelMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@PanelType(name = "clusters")
@PanelInstance(
        identifier = "clusters",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.roleAnalysisClusterRef",
                icon = GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON,
                order = 20
        )
)
public class ClustersPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";
    private static final String DOT_CLASS = ClustersPanel.class.getName() + ".";
    private static final String OP_DELETE_CLUSTER = DOT_CLASS + "deleteCluster";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";

    public ClustersPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        form.add(clusterTable());
    }

    private ObjectQuery getCustomizeContentQuery() {
        return getPrismContext().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(getObjectDetailsModels().getObjectWrapper().getOid(), RoleAnalysisSessionType.COMPLEX_TYPE)
                .build();
    }

    protected MainObjectListPanel<RoleAnalysisClusterType> clusterTable() {
        MainObjectListPanel<RoleAnalysisClusterType> basicTable = new MainObjectListPanel<>(ID_DATATABLE, RoleAnalysisClusterType.class) {

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisClusterType>> createProvider() {
                SelectableBeanObjectDataProvider<RoleAnalysisClusterType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(ClustersPanel.this.createDeleteInlineMenu());
                menuItems.add(ClustersPanel.this.createPreviewInlineMenu());
                return menuItems;
            }

            @Override
            protected IColumn<SelectableBean<RoleAnalysisClusterType>, String> createIconColumn() {
                return new CompositedIconColumn<>(Model.of("")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem, String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {
                        super.populateItem(cellItem, componentId, rowModel);
                    }

                    @Override
                    protected CompositedIcon getCompositedIcon(IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {
                        String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(RoleAnalysisClusterType.COMPLEX_TYPE);
                        CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                                LayeredIconCssStyle.IN_ROW_STYLE);

                        SelectableBean<RoleAnalysisClusterType> object = rowModel.getObject();
                        if (object != null) {
                            RoleAnalysisClusterType value = object.getValue();
                            if (value != null) {

                                PolyStringType name = value.getName();
                                if (name != null && name.getOrig().contains("outlier")) {
                                    compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(
                                            defaultBlackIcon + " " + GuiStyleConstants.RED_COLOR,
                                            LayeredIconCssStyle.IN_ROW_STYLE);
                                }

                                Task task = getPageBase().createSimpleTask(OP_UPDATE_STATUS);
                                OperationResult result = task.getResult();

                                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                                roleAnalysisService.recomputeAndResolveClusterOpStatus(
                                        value.asPrismObject(), RoleAnalysisChannelMode.DEFAULT
                                        , result, task);

                                boolean isUnderActivity = getPageBase().getRoleAnalysisService()
                                        .isUnderActivity(value.asPrismObject(), RoleAnalysisChannelMode.DEFAULT, task, result);

                                IconType icon = new IconType();
                                if (isUnderActivity) {
                                    icon.setCssClass("fas fa-sync-alt fa-spin"
                                            + " " + GuiStyleConstants.BLUE_COLOR);
                                    compositedIconBuilder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
                                }

                            }
                        }
                        return compositedIconBuilder.build();
                    }
                };
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("Users")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractUserObjectCount(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_USERS_COUNT);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        IModel<?> userObjectCount = extractUserObjectCount(model);
                        cellItem.add(new Label(componentId, userObjectCount));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("Roles")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractRoleObjectCount(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_ROLES_COUNT);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        cellItem.add(new Label(componentId, extractRoleObjectCount(model)));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("AnalysisClusterStatisticType.membershipRange")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractMembershipRange(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_MEMBERSHIP_RANGE);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        IModel<?> membershipRange = extractMembershipRange(model);
                        cellItem.add(new Label(componentId, membershipRange));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("AnalysisClusterStatisticType.membershipMean")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractMembershipMean(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_MEMBERSHIP_MEAN);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        cellItem.add(new Label(componentId, extractMembershipMean(model)));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("AnalysisClusterStatisticType.detectedReductionMetric")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractReductionMetric(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        cellItem.add(new Label(componentId, extractReductionMetric(model)));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC.getLocalPart();
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("AnalysisClusterStatisticType.membershipDensity")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractMembershipDensity(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_MEMBERSHIP_DENSITY);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        RoleAnalysisClusterType value = model.getObject().getValue();
                        if (value != null
                                && value.getClusterStatistics() != null
                                && value.getClusterStatistics().getMembershipDensity() != null) {
                            AnalysisClusterStatisticType clusterStatistics = model.getObject().getValue().getClusterStatistics();
                            Double density = clusterStatistics.getMembershipDensity();
                            String pointsDensity = String.format("%.3f",
                                    density);

                            String colorClass = densityBasedColor(density);

                            Label label = new Label(componentId, pointsDensity + " (%)");
                            label.setOutputMarkupId(true);
                            label.add(new AttributeModifier("class", colorClass));
                            label.add(AttributeModifier.append("style", "width: 100px;"));
                            cellItem.add(label);
                        } else {

                            cellItem.add(new EmptyPanel(componentId));
                        }

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_CLUSTER;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }
        };
        basicTable.setOutputMarkupId(true);

        return basicTable;
    }

    private MainObjectListPanel<RoleAnalysisClusterType> getTable() {
        return (MainObjectListPanel<RoleAnalysisClusterType>) get(ID_FORM + ":" + ID_DATATABLE);
    }

    private InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisClusterType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisClusterType>> selectedObjects = getTable().getSelectedObjects();
                        PageBase page = (PageBase) getPage();
                        RoleAnalysisService roleAnalysisService = page.getRoleAnalysisService();
                        Task task = page.createSimpleTask(OP_DELETE_CLUSTER);
                        OperationResult result = task.getResult();
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisClusterType> roleAnalysisSessionTypeSelectableBean = selectedObjects.get(0);
                                roleAnalysisService
                                        .deleteCluster(
                                                roleAnalysisSessionTypeSelectableBean.getValue(), task, result);
                            } catch (Exception e) {
                                throw new RuntimeException("Couldn't delete selected cluster", e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisClusterType>> rowModel = getRowModel();
                                roleAnalysisService
                                        .deleteCluster(
                                                rowModel.getObject().getValue(), task, result);
                            } catch (Exception e) {
                                throw new RuntimeException("Couldn't delete selected cluster", e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisClusterType> selectedObject : selectedObjects) {
                                try {
                                    RoleAnalysisClusterType roleAnalysisClusterType = selectedObject.getValue();
                                    roleAnalysisService
                                            .deleteCluster(
                                                    roleAnalysisClusterType, task, result);
                                } catch (Exception e) {
                                    throw new RuntimeException("Couldn't delete selected cluster", e);
                                }
                            }
                        }

                        getTable().refreshTable(target);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return getTable().getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        };
    }

    private InlineMenuItem createPreviewInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_PREVIEW);
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisClusterType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Image"),
                                getRowModel().getObject().getValue().asPrismObject().getOid()) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }
                };
            }
        };
    }

    private static IModel<?> extractUserObjectCount(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getUsersCount() != null) {
            return Model.of(value.getClusterStatistics().getUsersCount());
        } else {
            return Model.of("");
        }
    }

    private static IModel<?> extractRoleObjectCount(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getRolesCount() != null) {
            Integer rolesCount = value.getClusterStatistics().getRolesCount();
            return Model.of(rolesCount);
        } else {
            return Model.of("");
        }
    }

    private static IModel<?> extractMembershipRange(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        AnalysisClusterStatisticType clusterStatistics = null;
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null) {
            clusterStatistics = value.getClusterStatistics();
        }

        if (clusterStatistics != null
                && clusterStatistics.getMembershipRange() != null
                && clusterStatistics.getMembershipRange().getMin() != null
                && clusterStatistics.getMembershipRange().getMax() != null) {
            return Model.of(clusterStatistics.getMembershipRange().getMin()
                    + " - " + clusterStatistics.getMembershipRange().getMax());

        } else {
            return Model.of("");
        }
    }

    private static IModel<?> extractMembershipMean(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getMembershipMean() != null) {
            return Model.of(value.getClusterStatistics().getMembershipMean());
        } else {
            return Model.of("");
        }
    }

    private static IModel<?> extractReductionMetric(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getDetectedReductionMetric() != null) {
            Double detectedReductionMetric = value.getClusterStatistics().getDetectedReductionMetric();
            return Model.of(detectedReductionMetric);
        } else {
            return Model.of("");
        }
    }

    private static IModel<?> extractMembershipDensity(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getMembershipDensity() != null) {
            AnalysisClusterStatisticType clusterStatistics = model.getObject().getValue().getClusterStatistics();
            String pointsDensity = String.format("%.3f",
                    clusterStatistics.getMembershipDensity());

            return Model.of(pointsDensity + " (%)");
        } else {

            return Model.of("");
        }
    }

    private <C extends Containerable> PrismPropertyHeaderPanel<?> createModeBasedColumnHeader(String componentId,
            ItemPath itemPath) {
        LoadableModel<PrismContainerDefinition<AnalysisClusterStatisticType>> definitionModel
                = WebComponentUtil.getContainerDefinitionModel(AnalysisClusterStatisticType.class);

        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel<>(
                definitionModel,
                itemPath,
                (PageBase) getPage())) {

            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }
        };

    }

}
