/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.deleteSingleRoleAnalysisCluster;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getClusterStatisticsType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.getColorClass;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.details.work.ImageDetailsPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "clusters")
@PanelInstance(
        identifier = "clusters",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.roleAnalysisClusterRef",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        )
)
public class ClustersPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";

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

        RoleAnalysisProcessModeType processMode = getObjectDetailsModels().getObjectType().getProcessMode();

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
                return menuItems;
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;

                LoadableModel<PrismContainerDefinition<RoleAnalysisClusterType>> containerDefinitionModel
                        = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisClusterType.class);

                column = new AbstractColumn<>(
                        createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AbstractAnalysisClusterStatistic.F_MEMBER_COUNT, processMode);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        cellItem.add(new Label(componentId, getClusterStatisticsType(model.getObject().getValue())
                                .getMemberCount()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AbstractAnalysisClusterStatistic.F_PROPERTIES_COUNT, processMode);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        if (model.getObject().getValue() != null && getClusterStatisticsType(model.getObject().getValue()).getPropertiesCount() != null) {
                            cellItem.add(new Label(componentId, getClusterStatisticsType(model.getObject().getValue()).getPropertiesCount()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.range.properties")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AbstractAnalysisClusterStatistic.F_PROPERTIES_RANGE, processMode);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (getClusterStatisticsType(model.getObject().getValue()) != null
                                && getClusterStatisticsType(model.getObject().getValue()).getPropertiesRange() != null
                                && getClusterStatisticsType(model.getObject().getValue()).getPropertiesRange().getMin() != null) {
                            cellItem.add(new Label(componentId, getClusterStatisticsType(model.getObject().getValue())
                                    .getPropertiesRange().getMin() + " - " + getClusterStatisticsType(model.getObject().getValue())
                                    .getPropertiesRange().getMax()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.mean")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AbstractAnalysisClusterStatistic.F_PROPERTIES_MEAN, processMode);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && getClusterStatisticsType(model.getObject().getValue()).getPropertiesMean() != null) {
                            cellItem.add(new Label(componentId, getClusterStatisticsType(model.getObject().getValue()).getPropertiesMean()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.reduction")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisClusterType.F_DETECTION_PATTERN,
                                RoleAnalysisDetectionPatternType.F_CLUSTER_METRIC));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        List<RoleAnalysisDetectionPatternType> detection = model.getObject().getValue().getDetectionPattern();
                        double maxMetric = 0;
                        if (detection != null) {
                            for (RoleAnalysisDetectionPatternType roleAnalysisDetectionType : detection) {
                                if (roleAnalysisDetectionType.getClusterMetric() != null) {
                                    maxMetric = Math.max(maxMetric, roleAnalysisDetectionType.getClusterMetric());
                                }
                            }
                        }

                        cellItem.add(new Label(componentId,
                                maxMetric));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.density")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createModeBasedColumnHeader(componentId, AbstractAnalysisClusterStatistic.F_PROPERTIES_DENSITY, processMode);

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        String pointsDensity = String.format("%.3f",
                                getClusterStatisticsType(model.getObject().getValue()).getPropertiesDensity());

                        String colorClass = getColorClass(pointsDensity);

                        Label label = new Label(componentId, pointsDensity + " (%)");
                        label.setOutputMarkupId(true);
                        label.add(new AttributeModifier("class", colorClass));
                        label.add(AttributeModifier.append("style", "width: 100px;"));

                        cellItem.add(label);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.image.popup")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && getClusterStatisticsType(model.getObject().getValue()).getMemberCount() != null) {

                            AjaxIconButton ajaxButton = new AjaxIconButton(componentId, Model.of("fa fa-image"),
                                    createStringResource("RoleMining.cluster.table.similar.image.popup")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Image"),
                                            model.getObject().getValue().asPrismObject().getOid()) {
                                        @Override
                                        public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                            super.onClose(ajaxRequestTarget);
                                        }
                                    };
                                    ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);
                                }
                            };

                            ajaxButton.add(AttributeAppender.replace("class", " btn btn-default btn-sm d-flex "
                                    + "justify-content-center align-items-center"));
                            ajaxButton.add(new AttributeAppender("style", " width:40px; "));
                            ajaxButton.setOutputMarkupId(true);
                            cellItem.add(ajaxButton);

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
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

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisClusterType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisClusterType>> selectedObjects = getTable().getSelectedObjects();
                        OperationResult result = new OperationResult("Delete cluster object");
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisClusterType> roleAnalysisSessionTypeSelectableBean = selectedObjects.get(0);
                                deleteSingleRoleAnalysisCluster(result, roleAnalysisSessionTypeSelectableBean.getValue(),
                                        (PageBase) getPage());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisClusterType>> rowModel = getRowModel();
                                deleteSingleRoleAnalysisCluster(result, rowModel.getObject().getValue(), (PageBase) getPage());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisClusterType> selectedObject : selectedObjects) {
                                try {
                                    RoleAnalysisClusterType roleAnalysisClusterType = selectedObject.getValue();
                                    deleteSingleRoleAnalysisCluster(result, roleAnalysisClusterType, (PageBase) getPage());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
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

    private <C extends Containerable> PrismPropertyHeaderPanel<?> createModeBasedColumnHeader(String componentId,
            ItemPath itemPath, RoleAnalysisProcessModeType processMode) {
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            LoadableModel<PrismContainerDefinition<RoleAnalysisClusterStatistic>> definitionModel
                    = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisClusterStatistic.class);

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
        } else {
            LoadableModel<PrismContainerDefinition<UserAnalysisClusterStatistic>> definitionModel
                    = WebComponentUtil.getContainerDefinitionModel(UserAnalysisClusterStatistic.class);
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

    private PrismPropertyHeaderPanel<?> createColumnHeader(String componentId,
            LoadableModel<PrismContainerDefinition<RoleAnalysisClusterType>> containerDefinitionModel,
            ItemPath itemPath) {
        return createPrismPropertyHeaderPanel(componentId, containerDefinitionModel, itemPath);
    }

    private PrismPropertyHeaderPanel<?> createPrismPropertyHeaderPanel(String componentId,
            LoadableModel<? extends PrismContainerDefinition<?>> definitionModel,
            ItemPath itemPath) {
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
