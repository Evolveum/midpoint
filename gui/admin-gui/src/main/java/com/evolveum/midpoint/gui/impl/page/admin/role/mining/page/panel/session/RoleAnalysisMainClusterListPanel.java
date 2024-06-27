/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectState;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisClusterOccupationPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.data.mining.CollapsableContainerPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
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
                label = "RoleAnalysisSessionType.roleAnalysisClusterRef",
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

    public RoleAnalysisMainClusterListPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
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
            protected boolean isCollapsableTable() {
                return true;
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisClusterType>> createProvider() {
                SelectableBeanObjectDataProvider<RoleAnalysisClusterType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                provider.setSort(RoleAnalysisClusterType.F_NAME.getLocalPart(), SortOrder.DESCENDING);
                return provider;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(RoleAnalysisMainClusterListPanel.this.createDeleteInlineMenu());
                menuItems.add(RoleAnalysisMainClusterListPanel.this.createPreviewInlineMenu());
                return menuItems;
            }

            @Override
            protected IColumn<SelectableBean<RoleAnalysisClusterType>, String> createNameColumn(IModel<String> displayModel,
                    GuiObjectColumnType customColumn, ExpressionType expression) {
                return new ObjectNameColumn<>(displayModel == null ? createStringResource("ObjectType.name") : displayModel,
                        customColumn, expression, getPageBase()) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {
                        super.onClick(rowModel);
                    }

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
                        linkPanel.add(AttributeAppender.append("class", "text-truncate"));
                        linkPanel.add(AttributeAppender.append("title", clusterName.getOrig()));
                        linkPanel.add(new TooltipBehavior());
                        cellItem.add(linkPanel);
                    }
                };
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
                                RoleAnalysisClusterCategory category = value.getCategory();
                                if (category != null && category.equals(RoleAnalysisClusterCategory.OUTLIERS)) {
                                    compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon
                                                    + " " + GuiStyleConstants.RED_COLOR,
                                            LayeredIconCssStyle.IN_ROW_STYLE);
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
            protected List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> createDefaultColumns() {

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
                            label.add(AttributeAppender.append("class", labelClass));
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
                                createStringResource("RoleAnalysisCluster.table.header.cluster.occupation")) {
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
                        IModel<String> userObjectCount = extractUserObjectCount(model);

                        RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(componentId) {
                            @Override
                            public Component createFirstPanel(String idFirstPanel) {
                                return new IconWithLabel(idFirstPanel, userObjectCount) {
                                    @Override
                                    public String getIconCssClass() {
                                        return "fa fa-user object-user-color";
                                    }
                                };
                            }

                            @Override
                            public Component createSecondPanel(String idSecondPanel) {
                                return new IconWithLabel(idSecondPanel, roleObjectCount) {
                                    @Override
                                    public String getIconCssClass() {
                                        return "fe fe-role object-role-color";
                                    }
                                };
                            }

                            @Override
                            public Component createSeparatorPanel(String idSeparatorPanel) {
                                Label separator = new Label(idSeparatorPanel, "");
                                separator.add(AttributeModifier.replace("class",
                                        "d-flex align-items-center gap-3 fa-solid fa-grip-lines-vertical"));
                                separator.setOutputMarkupId(true);
                                add(separator);
                                return separator;
                            }
                        };

                        occupationPanel.setOutputMarkupId(true);
                        cellItem.add(occupationPanel);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                if (!RoleAnalysisCategoryType.OUTLIERS.equals(analysisOption.getAnalysisCategory())) {
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

                            IconWithLabel icon = new IconWithLabel(componentId, extractReductionMetric(model)) {
                                @Override
                                public String getIconCssClass() {
                                    return "fa fa-leaf";
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
                    columns.add(new AbstractExportableColumn<>(
                            createStringResource("AnalysisClusterStatisticType.attribute.confidence")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                            return null;
                        }

                        @Override
                        public String getSortProperty() {
                            return DetectedPattern.F_METRIC;
                        }

                        @Override
                        public boolean isSortable() {
                            return true;
                        }

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {
                            if (rowModel.getObject() != null) {
                                SelectableBean<RoleAnalysisClusterType> object = rowModel.getObject();
                                RoleAnalysisClusterType cluster = object.getValue();
                                AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

                                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                                RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
                                String confidence = roleAnalysisService.calculateAttributeConfidence(processMode, clusterStatistics);
                                cellItem.add(new Label(componentId, confidence + "%"));
                            }
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new Label(
                                    componentId,
                                    createStringResource("RoleMining.cluster.table.column.header.items.confidence"));

                        }

                    });

                    column = new AbstractExportableColumn<>(Model.of("Outliers count")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisClusterType>> iModel) {
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new Label(componentId, "Outliers count");
                        }

                        int outlierCount = 0;

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                            outlierCount = 0;
                            RoleAnalysisClusterType cluster = model.getObject().getValue();
                            List<ObjectReferenceType> member = cluster.getMember();

                            Set<String> membersOid = new HashSet<>();
                            for (ObjectReferenceType objectReferenceType : member) {
                                membersOid.add(objectReferenceType.getOid());
                            }

                            ObjectQuery query = getPrismContext().queryFor(RoleAnalysisOutlierType.class)
                                    .item(RoleAnalysisOutlierType.F_TARGET_OBJECT_REF).ref(membersOid.toArray(new String[0]))
                                    .build();

                            ModelService modelService = getPageBase().getModelService();
                            Task task = getPageBase().createSimpleTask("countObjects");
                            OperationResult result = task.getResult();

//                            int count = 0;
                            //TODO restore after db schema change
//                            try {
//                                count = modelService.countObjects(RoleAnalysisOutlierType.class, query, null, task, result);
//                            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException |
//                                    ConfigurationException | CommunicationException |
//                                    ExpressionEvaluationException e) {
//                                throw new RuntimeException("Couldn't count outliers", e);
//                            }

                            //TODO remove hack after db schema change
                            ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

                                RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
                                ObjectReferenceType targetObjectRef = outlierObject.getTargetObjectRef();
                                String oid = targetObjectRef.getOid();
                                if (membersOid.contains(oid)) {
                                    outlierCount++;
                                }
                                return true;
                            };

                            try {
                                modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                                        null, task, result);
                            } catch (Exception ex) {
                                throw new RuntimeException("Couldn't count outliers", ex);
                            }

                            cellItem.add(new Label(componentId, Model.of(outlierCount)));
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
                            confidence = roleAnalysisService.calculateAttributeConfidence(processMode, clusterStatistics) + "%";
                        } else {
                            cellItem.add(new EmptyPanel(componentId));
                        }

                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                                .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);

                        AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                                Model.of(confidence)) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
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
                                        RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_COLLAPSABLE_CONTENT,
                                                Model.of("Role analysis attribute panel"), roleAttributeAnalysisResult, userAttributeAnalysisResult);
                                        roleAnalysisAttributePanel.setOutputMarkupId(true);
                                        webMarkupContainerUser.add(roleAnalysisAttributePanel);
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

                        };
                        objectButton.titleAsLabel(true);
                        objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm rounded-pill"));
                        objectButton.add(AttributeAppender.append("style", "width:120px"));

                        cellItem.add(objectButton);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
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

            @Override
            public String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeAppender.append("style", "width: 170px"));
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

    private static IModel<String> extractUserObjectCount(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getUsersCount() != null) {
            return Model.of(value.getClusterStatistics().getUsersCount().toString());
        } else {
            return Model.of("");
        }
    }

    private static IModel<String> extractRoleObjectCount(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
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

    private static IModel<String> extractReductionMetric(IModel<SelectableBean<RoleAnalysisClusterType>> model) {
        RoleAnalysisClusterType value = model.getObject().getValue();
        if (value != null
                && value.getClusterStatistics() != null
                && value.getClusterStatistics().getDetectedReductionMetric() != null) {
            Double detectedReductionMetric = value.getClusterStatistics().getDetectedReductionMetric();
            return Model.of(String.valueOf(detectedReductionMetric));
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
