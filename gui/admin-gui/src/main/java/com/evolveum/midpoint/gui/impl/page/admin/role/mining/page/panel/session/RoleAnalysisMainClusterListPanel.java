/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.LinkIconLabelIconPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;
import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
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

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectState;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.data.mining.CollapsableContainerPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@PanelType(name = "clusters")
@PanelInstance(
        identifier = "clusters",
        applicableForType = RoleAnalysisSessionType.class,
//        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.roleAnalysisCluster.result",
                icon = GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON,
                order = 20
        )
)
public class RoleAnalysisMainClusterListPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";
    private static final String DOT_CLASS = RoleAnalysisMainClusterListPanel.class.getName() + ".";
    private static final String OP_DELETE_CLUSTER = DOT_CLASS + "deleteCluster";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";

    LoadableModel<ListMultimap<String, String>> mappedClusterOutliers;

    public RoleAnalysisMainClusterListPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
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
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_DATATABLE, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Contract("_, _ -> new")
            @Override
            protected @NotNull WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        assert target != null;
                        target.add(getPageBase().getFeedbackPanel());
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
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
            protected @NotNull ISelectableDataProvider<SelectableBean<RoleAnalysisClusterType>> createProvider() {
                //                SelectableBeanObjectDataProvider<RoleAnalysisClusterType> provider = createSelectableBeanObjectDataProvider(() ->
//                        getCustomizeContentQuery(), null);
//                provider.setEmptyListOnNullQuery(true);
//                provider.setSort(null);
//                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
//                provider.setSort(RoleAnalysisClusterType.F_NAME.getLocalPart(), SortOrder.DESCENDING);
                return RoleAnalysisMainClusterListPanel.this
                        .createProvider(category, RoleAnalysisMainClusterListPanel.this);
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected @NotNull List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(RoleAnalysisMainClusterListPanel.this.createDeleteInlineMenu());
                menuItems.add(RoleAnalysisMainClusterListPanel.this.createPreviewInlineMenu());
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
                        createStringResource("")) {

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
                        createStringResource("")) {

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
                            initDensityProgressPanel(cellItem, componentId, density);
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
                            return false;
                        }

                        @Override
                        public String getSortProperty() {
                            return AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC.getLocalPart();
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
                            return false;
                        }

                        @Override
                        public String getSortProperty() {
                            return AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC.getLocalPart();
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
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = null;
            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = null;
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

    private static void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
            @NotNull String componentId,
            @NotNull Double density) {

        BigDecimal bd = new BigDecimal(Double.toString(density));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        String colorClass = densityBasedColor(pointsDensity);

        ProgressBar progressBar = new ProgressBar(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return pointsDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeModifier.append(STYLE_CSS, "width: 170px"));
        cellItem.add(progressBar);
    }

    @SuppressWarnings("unchecked")
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
                                Model.of("Image"),
                                getRowModel().getObject().getValue().asPrismObject().getOid());
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
            outlierObject.getOutlierPartitions().forEach(partition -> outliersMap.put(
                    partition.getTargetClusterRef().getOid(), outlierObject.getOid()));
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

    private @NotNull SelectableBeanObjectDataProvider<RoleAnalysisClusterType> createProvider(
            RoleAnalysisClusterCategory category,
            Component component) {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("loadClusters");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<RoleAnalysisClusterType> sessionClustersByType = roleAnalysisService.getSessionClustersByType(
                getObjectWrapperObject().getOid(), category, task, result);
        RoleAnalysisSessionType session = getObjectDetailsModels().getObjectType();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcedureType analysisProcedureType = analysisOption.getAnalysisProcedureType();
        //sort by outlier count
        if (analysisProcedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
            ListMultimap<String, String> clusterMappedClusterOutliers = getMappedClusterOutliers().getObject();
            sessionClustersByType.sort((o1, o2) -> Integer.compare(
                    clusterMappedClusterOutliers.get(o2.getOid()).size(), clusterMappedClusterOutliers.get(o1.getOid()).size()));
        } else {
            sessionClustersByType.sort((o1, o2) -> {
                AnalysisClusterStatisticType o1ClusterStatistics = o1.getClusterStatistics();
                AnalysisClusterStatisticType o2ClusterStatistics = o2.getClusterStatistics();
                if (o1ClusterStatistics == null
                        || o2ClusterStatistics == null) {
                    return 0;
                }

                if (o1ClusterStatistics.getDetectedReductionMetric() == null
                        || o2ClusterStatistics.getDetectedReductionMetric() == null) {
                    return 0;
                }

                return Double.compare(o2.getClusterStatistics().getDetectedReductionMetric(),
                        o1.getClusterStatistics().getDetectedReductionMetric());
            });
        }
        return new SelectableBeanObjectDataProvider<>(
                component, Set.of()) {

            @SuppressWarnings("rawtypes")
            @Override
            protected List<RoleAnalysisClusterType> searchObjects(Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                return sessionClustersByType.subList(offset, offset + maxSize);
            }

            @Override
            protected Integer countObjects(Class<RoleAnalysisClusterType> type,
                    ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                    Task task,
                    OperationResult result) {
                return sessionClustersByType.size();
            }
        };
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
