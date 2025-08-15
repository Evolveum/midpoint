/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.clusters;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectState;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.ImageDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider.ClusterSelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.LinkIconLabelIconPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.image.CustomImageResource;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.data.mining.CollapsableContainerPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider.ClusterSelectableBeanObjectDataProvider.*;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

public abstract class RoleAnalysisAbstractClusteringResultPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_TABS_PANEL = "datatable";
    private static final String ID_FORM = "form";
    private static final String DOT_CLASS = RoleAnalysisAbstractClusteringResultPanel.class.getName() + ".";
    private static final String OP_DELETE_CLUSTER = DOT_CLASS + "deleteCluster";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";
    private static final String OP_PREPARE_CLUSTER_IMAGE = DOT_CLASS + "prepareClusterImage";

    LoadableModel<ListMultimap<String, String>> mappedClusterOutliers;

    protected RoleAnalysisAbstractClusteringResultPanel(
            String id,
            ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
        initModels();
    }

    public void initModels() {
        RoleAnalysisSessionType session = getObjectDetailsModels().getObjectType();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption != null && analysisOption.getAnalysisProcedureType() == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
            mappedClusterOutliers = new LoadableModel<>() {
                @Override
                protected ListMultimap<String, String> load() {
                    return tmpMapClusterOutliers();
                }
            };
        }
    }

    @Override
    protected void initLayout() {
        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        List<ITab> tabs = createTabs();
        RoleAnalysisTabbedPanel<ITab> tabPanel = createRoleAnalysisTabPanel(getPageBase(), ID_TABS_PANEL, tabs);
        tabPanel.add(AttributeModifier.append(CLASS_CSS, "p-0 m-0"));
        form.add(tabPanel);

//        form.add(clusterTable());
    }

    protected List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(getPageBase().createStringResource("Clusters"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                MainObjectListPanel<RoleAnalysisClusterType> components = clusterTable(panelId, RoleAnalysisClusterCategory.INLIERS);
                components.setOutputMarkupId(true);
                return components;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("Noise"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                MainObjectListPanel<RoleAnalysisClusterType> components = clusterTable(panelId, RoleAnalysisClusterCategory.OUTLIERS);
                components.setOutputMarkupId(true);
                return components;
            }
        });
        return tabs;
    }

    //TODO - structure
    protected MainObjectListPanel<RoleAnalysisClusterType> clusterTable(String panelId, RoleAnalysisClusterCategory category) {
        MainObjectListPanel<RoleAnalysisClusterType> basicTable = new MainObjectListPanel<>(panelId, RoleAnalysisClusterType.class) {

            @Override
            protected boolean isCollapsableTable() {
                return true;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected boolean showTableAsCard() {
                return false;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected @NotNull ISelectableDataProvider<SelectableBean<RoleAnalysisClusterType>> createProvider() {
                return new ClusterSelectableBeanObjectDataProvider(
                        RoleAnalysisAbstractClusteringResultPanel.this,
                        Model.of(),
                        Set.of(),
                        category,
                        mappedClusterOutliers,
                        getObjectWrapperObject().asObjectable());
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected IColumn<SelectableBean<RoleAnalysisClusterType>, String> createCheckboxColumn() {
                return new CheckBoxHeaderColumn<>() {
                    @Override
                    public Component getHeader(String componentId) {
                        Component header = super.getHeader(componentId);
                        header.add(new VisibleBehaviour(() -> false));
                        return header;
                    }
                };
            }

            @Override
            protected @NotNull List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(RoleAnalysisAbstractClusteringResultPanel.this.createDeleteInlineMenu(panelId));
                menuItems.add(RoleAnalysisAbstractClusteringResultPanel.this.createPreviewInlineMenu());
                return menuItems;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getInlineMenuItemCssClass() {
                return "btn btn-default btn-sm";
            }

            @Override
            @Contract(pure = true)
            protected @NotNull String getInlineMenuCssClass() {
                return "inline-menu-column";
            }

            @Contract("_, _, _ -> new")
            @Override
            protected @NotNull IColumn<SelectableBean<RoleAnalysisClusterType>, String> createNameColumn(IModel<String> displayModel,
                    GuiObjectColumnType customColumn, ExpressionType expression) {
                return new ObjectNameColumn<>(displayModel == null ? createStringResource("ObjectType.name") : displayModel,
                        customColumn, expression, getPageBase()) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {

                        RoleAnalysisClusterType cluster = rowModel.getObject().getValue();
                        PolyStringType clusterName = cluster.getName();
                        LinkPanel linkPanel = new LinkPanel(componentId, Model.of(clusterName)) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick() {
                                objectDetailsPerformed(cluster);
                            }
                        };

                        linkPanel.setOutputMarkupId(true);
                        linkPanel.add(AttributeModifier.append(CLASS_CSS, TEXT_TRUNCATE));
                        linkPanel.add(AttributeModifier.append(TITLE_CSS, clusterName.getOrig()));
                        linkPanel.add(new TooltipBehavior());
                        cellItem.add(linkPanel);
                    }
                };
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IColumn<SelectableBean<RoleAnalysisClusterType>, String> createIconColumn() {
                return new CompositedIconColumn<>(Model.of("")) {

                    @Override
                    protected CompositedIcon getCompositedIcon(IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {
                        String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(RoleAnalysisClusterType.COMPLEX_TYPE);
                        CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                                IconCssStyle.IN_ROW_STYLE);

                        SelectableBean<RoleAnalysisClusterType> object = rowModel.getObject();
                        if (object != null) {
                            RoleAnalysisClusterType value = object.getValue();
                            if (value != null) {
                                RoleAnalysisClusterCategory category = value.getCategory();
                                if (category != null && category.equals(RoleAnalysisClusterCategory.OUTLIERS)) {
                                    compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon
                                                    + " " + GuiStyleConstants.RED_COLOR,
                                            IconCssStyle.IN_ROW_STYLE);
                                }

                                Task task = getPageBase().createSimpleTask(OP_UPDATE_STATUS);
                                OperationResult result = task.getResult();

                                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                                @NotNull String stateString = roleAnalysisService.recomputeAndResolveClusterOpStatus(
                                        value.asPrismObject().getOid(),
                                        result, task, true, getPageBase().getModelInteractionService());

                                IconType icon = new IconType();
                                if (stateString.equals(RoleAnalysisObjectState.PROCESSING.getDisplayString())) {
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
            protected @NotNull List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> createDefaultColumns() {

                RoleAnalysisSessionType session = getObjectWrapperObject().asObjectable();
                RoleAnalysisOptionType analysisOption = session.getAnalysisOption();

                List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("AnalysisClusterStatisticType.status")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractMembershipDensity(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisCluster.table.header.cluster.state")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisCluster.table.header.cluster.state.help");
                            }
                        };

                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        RoleAnalysisClusterType value = model.getObject().getValue();
                        if (value != null) {
                            List<RoleAnalysisCandidateRoleType> candidateRoles = value.getCandidateRoles();
                            List<ObjectReferenceType> resolvedPattern = value.getResolvedPattern();

                            boolean candidateExist = candidateRoles != null && !candidateRoles.isEmpty();
                            boolean resolvedPatternExist = resolvedPattern != null && !resolvedPattern.isEmpty();

                            String status;
                            String labelClass;
                            if (resolvedPatternExist) {
                                status = "Rebuild recommended";
                                labelClass = "badge badge-warning text-center";
                            } else if (candidateExist) {
                                status = "In progress";
                                labelClass = "badge badge-info text-center";
                            } else {
                                status = "New";
                                labelClass = "badge badge-primary text-center";
                            }

                            Label label = new Label(componentId, status);
                            label.setOutputMarkupId(true);
                            label.add(AttributeModifier.append(CLASS_CSS, labelClass));
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

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysis.tile.panel.users")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractRoleObjectCount(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysis.tile.panel.users")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisCluster.table.header.cluster.occupation.help");
                            }
                        };
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        IModel<String> userObjectCount = extractUserObjectCount(model);

                        IconWithLabel components = new IconWithLabel(componentId, userObjectCount) {
                            @Contract(pure = true)
                            @Override
                            public @NotNull String getIconCssClass() {
                                return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
                            }

                            @Override
                            protected @NotNull String getComponentCssClass() {
                                return super.getComponentCssClass() + TEXT_MUTED;
                            }
                        };

                        components.setOutputMarkupId(true);
                        cellItem.add(components);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysis.tile.panel.roles")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                        return extractRoleObjectCount(iModel);
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysis.tile.panel.roles")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisCluster.table.header.cluster.occupation.help");
                            }
                        };
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        IModel<String> roleObjectCount = extractRoleObjectCount(model);

                        IconWithLabel components = new IconWithLabel(componentId, roleObjectCount) {
                            @Contract(pure = true)
                            @Override
                            public @NotNull String getIconCssClass() {
                                return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " align-middle";
                            }

                            @Override
                            protected String getIconComponentCssStyle() {
                                return "font-size:18px!important;line-height:1;margin-bottom:0.25rem;";
                            }

                            @Override
                            protected @NotNull String getComponentCssClass() {
                                return super.getComponentCssClass() + TEXT_MUTED;
                            }
                        };

                        components.setOutputMarkupId(true);
                        cellItem.add(components);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                initSpecificColumn(analysisOption, columns);

                initRoleMiningColumns(analysisOption, columns);
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
                            initClusterDensityProgressPanel(cellItem, componentId, density);
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

            private void initSpecificColumn(RoleAnalysisOptionType analysisOption, List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns) {
                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;
                if (!RoleAnalysisProcedureType.OUTLIER_DETECTION.equals(analysisOption.getAnalysisProcedureType())) {
                    RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                    OperationResult result = new OperationResult("countUserOwnedRoleAssignment");
                    int allUserOwnedRoleAssignments = roleAnalysisService.countUserOwnedRoleAssignment(result);

                    column = new AbstractExportableColumn<>(
                            createStringResource("AnalysisClusterStatisticType.detectedReductionMetric")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                            return extractSystemReductionMetric(iModel, allUserOwnedRoleAssignments);
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return createModeBasedColumnHeader(componentId, AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC);
                        }

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                            IconWithLabel icon = new IconWithLabel(componentId, extractSystemReductionMetric(model, allUserOwnedRoleAssignments)) {
                                @Contract(pure = true)
                                @Override
                                public @NotNull String getIconCssClass() {
                                    return "fa fa-arrow-down text-success";
                                }

                                @Contract(pure = true)
                                @Override
                                protected @NotNull String getLabelComponentCssClass() {
                                    return "ml-1 text-success";
                                }

                                @Override
                                protected @NotNull Component getSubComponent(String id) {
                                    Label label = new Label(id, extractReductionMetric(model));
                                    label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                                    label.add(AttributeModifier.append(STYLE_CSS, "font-size: 14px"));
                                    return label;
                                }

                                @Override
                                protected String getIconComponentCssStyle() {
                                    return "vertical-align: inherit";
                                }
                            };

                            cellItem.add(icon);
                        }

                        @Override
                        public boolean isSortable() {
                            return true;
                        }

                        @Override
                        public String getSortProperty() {
                            return SORT_REDUCTION_PROPERTY;
                        }
                    };
                    columns.add(column);
                } else {
                    ListMultimap<String, String> clusterMappedClusterOutliers = getMappedClusterOutliers().getObject();
                    column = new AbstractExportableColumn<>(createStringResource("")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new Label(componentId, createStringResource("RoleAnalysis.outliers.count"));
                        }

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                            RoleAnalysisClusterType cluster = model.getObject().getValue();
                            String clusterOid = cluster.getOid();

                            List<String> outliers = clusterMappedClusterOutliers.get(clusterOid);
                            int outlierCount = outliers.size();
                            Label badgeLabel = new Label(componentId, String.valueOf(outlierCount));
                            if (outlierCount > 0) {
                                badgeLabel.add(AttributeModifier.append(CLASS_CSS, "badge badge-danger"));
                            } else {
                                badgeLabel.add(AttributeModifier.append(CLASS_CSS, "badge badge-info"));
                            }
                            badgeLabel.add(AttributeModifier.append(TITLE_CSS, "Outliers count"));
                            badgeLabel.add(new TooltipBehavior());
                            cellItem.add(badgeLabel);
                        }

                        @Override
                        public boolean isSortable() {
                            return true;
                        }

                        @Override
                        public String getSortProperty() {
                            return SORT_OUTLIER_COUNT_PROPERTY;
                        }
                    };
                    columns.add(column);
                }
            }

            private void initRoleMiningColumns(RoleAnalysisOptionType analysisOption,
                    List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns) {
                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;
                if (category != RoleAnalysisClusterCategory.OUTLIERS) {
                    column = new AbstractExportableColumn<>(
                            createStringResource("")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                            return extractUserObjectCount(iModel);
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new LabelWithHelpPanel(componentId,
                                    createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic")) {
                                @Override
                                protected IModel<String> getHelpModel() {
                                    return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                                }
                            };
                        }

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                            String confidence = "";

                            if (model.getObject() != null) {
                                SelectableBean<RoleAnalysisClusterType> object = model.getObject();
                                RoleAnalysisClusterType cluster = object.getValue();
                                AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

                                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                                RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
                                confidence = roleAnalysisService.calculateAttributeConfidence(processMode, clusterStatistics) + "% ";
                            } else {
                                cellItem.add(new EmptyPanel(componentId));
                            }

                            LinkIconLabelIconPanel components = new LinkIconLabelIconPanel(componentId, Model.of(confidence)) {
                                @Contract(pure = true)
                                @Override
                                public @NotNull String getIconCssClass() {
                                    return "fa fa-chart-bar";
                                }

                                @Override
                                protected String getSubComponentCssStyle() {
                                    return "margin-top:2px;";
                                }

                                @Override
                                protected void onClickPerform(AjaxRequestTarget target) {
                                    onAttributeAnalysisClickPerform(target, cellItem, model);
                                }
                            };

                            cellItem.add(components);
                        }

                        @Override
                        public boolean isSortable() {
                            return false;
                        }

                    };
                    columns.add(column);

                }
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_CLUSTER;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }
        };
        basicTable.setOutputMarkupId(true);

        return basicTable;
    }

    private void onAttributeAnalysisClickPerform(
            @NotNull AjaxRequestTarget target,
            @NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
            @NotNull IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        CollapsableContainerPanel collapseContainerUser = (CollapsableContainerPanel) cellItem
                .findParent(Item.class).get(ID_FIRST_COLLAPSABLE_CONTAINER);
        CollapsableContainerPanel collapseContainerRole = (CollapsableContainerPanel) cellItem
                .findParent(Item.class).get(ID_SECOND_COLLAPSABLE_CONTAINER);

        if (!collapseContainerUser.isExpanded()) {
            RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = null;
            RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult = null;
            if (model.getObject() != null) {
                RoleAnalysisClusterType value = model.getObject().getValue();
                AnalysisClusterStatisticType clusterStatistics = value.getClusterStatistics();
                if (clusterStatistics != null) {
                    userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
                    roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
                }
            }

            CollapsableContainerPanel webMarkupContainerUser = new CollapsableContainerPanel(
                    ID_FIRST_COLLAPSABLE_CONTAINER);
            webMarkupContainerUser.setOutputMarkupId(true);
            webMarkupContainerUser.add(AttributeModifier.replace("class", "collapse"));
            webMarkupContainerUser.add(AttributeModifier.replace("style", "display: none;"));
            webMarkupContainerUser.setExpanded(true);

            if (userAttributeAnalysisResult != null || roleAttributeAnalysisResult != null) {

                LoadableModel<RoleAnalysisAttributesDto> attributesModel = new LoadableModel<>(false) {
                    @Override
                    protected RoleAnalysisAttributesDto load() {
                        return RoleAnalysisAttributesDto.loadFromCluster(
                                createStringResource("RoleAnalysis.aspect.overview.page.title.clustering.attribute.analysis").getString(),
                                model.getObject().getValue());
                    }
                };

                RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_COLLAPSABLE_CONTENT,
                        attributesModel) {

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getCssClassForCardContainer() {
                        return "m-3 elevation-1 card";
                    }
                };
                roleAnalysisAttributePanel.setOutputMarkupId(true);
                webMarkupContainerUser.add(roleAnalysisAttributePanel);
                webMarkupContainerUser.add(AttributeModifier.append(CLASS_CSS, "bg-light"));
            } else {
                Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
                label.setOutputMarkupId(true);
                webMarkupContainerUser.add(label);
            }

            collapseContainerUser.replaceWith(webMarkupContainerUser);
            target.add(webMarkupContainerUser);
        }
        target.appendJavaScript(getCollapseScript(collapseContainerUser, collapseContainerRole));
    }

    private InlineMenuItem createDeleteInlineMenu(String tableId) {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisClusterType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisClusterType>> selectedObjects = getTable(tableId).getSelectedObjects();
                        PageBase page = (PageBase) getPage();
                        RoleAnalysisService roleAnalysisService = page.getRoleAnalysisService();
                        Task task = page.createSimpleTask(OP_DELETE_CLUSTER);
                        OperationResult result = task.getResult();
                        try {
                            if (selectedObjects.size() == 1 && getRowModel() == null) {

                                SelectableBean<RoleAnalysisClusterType> roleAnalysisSessionTypeSelectableBean = selectedObjects.get(0);
                                roleAnalysisService
                                        .deleteCluster(
                                                roleAnalysisSessionTypeSelectableBean.getValue(), task, result, true);

                            } else if (getRowModel() != null) {

                                IModel<SelectableBean<RoleAnalysisClusterType>> rowModel = getRowModel();
                                roleAnalysisService
                                        .deleteCluster(
                                                rowModel.getObject().getValue(), task, result, true);

                            } else {
                                for (SelectableBean<RoleAnalysisClusterType> selectedObject : selectedObjects) {

                                    RoleAnalysisClusterType roleAnalysisClusterType = selectedObject.getValue();
                                    roleAnalysisService
                                            .deleteCluster(
                                                    roleAnalysisClusterType, task, result, true);
                                }
                            }
                        } catch (Exception e) {
                            throw new SystemException("Couldn't delete cluster", e);
                        }

                        getTable(tableId).refreshTable(target);
                        target.add(RoleAnalysisAbstractClusteringResultPanel.this);
                    }
                };
            }

            @SuppressWarnings("rawtypes")
            @Override
            public IModel<String> getConfirmationMessageModel() {
                ColumnMenuAction action = (ColumnMenuAction) getAction();
                return createConfirmationMessage(action, tableId);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleAnalysisClusterType> getTable(String tableId) {
        return (MainObjectListPanel<RoleAnalysisClusterType>) get(getPageBase().createComponentPath(ID_FORM, ID_TABS_PANEL, tableId));
    }

    @Contract(pure = true)
    private @NotNull IModel<String> createConfirmationMessage(
            ColumnMenuAction<SelectableBean<RoleAnalysisClusterType>> action,
            String tableId) {
        return () -> {
            IModel<SelectableBean<RoleAnalysisClusterType>> result = action.getRowModel();
            if (result != null) {
                return getString("RoleAnalysisAbstractClusterResultPanel.delete.single", WebComponentUtil.getName(result.getObject().getValue()));
            }

            List<SelectableBean<RoleAnalysisClusterType>> selectedObjects = getTable(tableId).getSelectedObjects();

            if (selectedObjects.size() == 1) {
                RoleAnalysisClusterType object = selectedObjects.get(0).getValue();
                return getString("RoleAnalysisAbstractClusterResultPanel.delete.single", WebComponentUtil.getName(object));
            }

            return getString("RoleAnalysisAbstractClusterResultPanel.delete.multiple", selectedObjects.size());
        };
    }

    private InlineMenuItem createPreviewInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.cluster.preview")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-qrcode");
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
                                () -> {
                                    PageBase pageBase = RoleAnalysisAbstractClusteringResultPanel.this.getPageBase();
                                    RoleAnalysisClusterType cluster = getRowModel().getObject().getValue();
                                    Task task = pageBase.createSimpleTask(OP_PREPARE_CLUSTER_IMAGE);
                                    OperationResult result = task.getResult();
                                    return new CustomImageResource(pageBase, cluster, task, result);
                                });
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }
                };
            }
        };
    }

    private static @NotNull IModel<String> extractUserObjectCount(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getUsersCount() != null) {
            return Model.of(value.getClusterStatistics().getUsersCount().toString());
        } else {
            return Model.of("");
        }
    }

    private static @NotNull IModel<String> extractRoleObjectCount(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getRolesCount() != null) {
            Integer rolesCount = value.getClusterStatistics().getRolesCount();
            return Model.of(rolesCount.toString());
        } else {
            return Model.of("");
        }
    }

    private static @NotNull IModel<String> extractReductionMetric(
            @NotNull IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getDetectedReductionMetric() != null) {
            Double detectedReductionMetric = value.getClusterStatistics().getDetectedReductionMetric();
            if (detectedReductionMetric == null) {
                detectedReductionMetric = 0.0;
            }
            return Model.of("(" + detectedReductionMetric + ")");
        } else {
            return Model.of("(0)");
        }
    }

    private static @NotNull IModel<String> extractSystemReductionMetric(
            @NotNull IModel<SelectableBean<RoleAnalysisClusterType>> model,
            int allUserOwnedRoleAssignments) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getDetectedReductionMetric() != null) {
            Double detectedReductionMetric = value.getClusterStatistics().getDetectedReductionMetric();
            double percentagePart = 0;
            if (detectedReductionMetric != null && detectedReductionMetric != 0 && allUserOwnedRoleAssignments != 0) {
                percentagePart = (detectedReductionMetric / allUserOwnedRoleAssignments) * 100;
            }
            String formattedReductionFactorConfidence = String.format("%.2f", percentagePart);
            return Model.of(formattedReductionFactorConfidence + "%");
        } else {
            return Model.of("00.00%");
        }
    }

    private static @NotNull IModel<?> extractMembershipDensity(@NotNull IModel<SelectableBean<RoleAnalysisClusterType>> model) {
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

    @Contract("_, _ -> new")
    private @NotNull PrismPropertyHeaderPanel<?> createModeBasedColumnHeader(String componentId,
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

    //TODO this is a hack, remove after db schema change
    public ListMultimap<String, String> tmpMapClusterOutliers() {
        ListMultimap<String, String> outliersMap = ArrayListMultimap.create();
        ModelService modelService = getPageBase().getModelService();
        Task task = getPageBase().createSimpleTask("countObjects");
        OperationResult result = task.getResult();
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {
            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            outlierObject.getPartition().forEach(partition -> outliersMap.put(
                    partition.getClusterRef().getOid(), outlierObject.getOid()));
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                    null, task, result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't count outliers", ex);
        }
        return outliersMap;
    }

    //TODO we need decide how we can specify db for query provider
    private ObjectQuery getCustomizeContentQuery() {
        return getPrismContext().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(getObjectDetailsModels().getObjectWrapper().getOid(), RoleAnalysisSessionType.COMPLEX_TYPE)
                .build();
    }

    public LoadableModel<ListMultimap<String, String>> getMappedClusterOutliers() {
        return mappedClusterOutliers;
    }

}
