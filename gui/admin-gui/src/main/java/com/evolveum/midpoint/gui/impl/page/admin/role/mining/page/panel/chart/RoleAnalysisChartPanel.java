/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.ChartType.SCATTER;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.ChartType.getNextChartType;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.file.File;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.ChartType;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.RoleAnalysisAggregateChartModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

import javax.xml.namespace.QName;

public class RoleAnalysisChartPanel extends BasePanel<String> implements Popupable {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisChartPanel.class);
    private static final String ID_TOOL_FORM = "toolForm";
    private static final String ID_CONTAINER_CHART = "container";
    private static final String ID_CHART = "chart";
    private static final String ID_CARD_TITLE = "cardTitle";
    private static final String DOT_CLASS = RoleAnalysisChartPanel.class.getName() + ".";
    private static final String OP_LOAD_STATISTICS = DOT_CLASS + "loadRoleAnalysisStatistics";
    private static final String ID_EXPORT_BUTTON = "exportButton";

    private static final String BTN_TOOL_CSS = "btn btn-tool";

    private boolean isSortByGroup = false;
    private boolean isScalable = false;
    private boolean isUserMode = false;

    boolean isAxisExpanded = false;

    ChartType chartType = SCATTER;

    public RoleAnalysisChartPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initChartPart();
    }

    private void initChartPart() {
        WebMarkupContainer chartContainer = new WebMarkupContainer(ID_CONTAINER_CHART);
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);

        IconWithLabel cardTitle = new IconWithLabel(ID_CARD_TITLE,
                createStringResource("PageRoleAnalysis.chart.access.distribution.title")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-chart-pie";
            }
        };
        cardTitle.setOutputMarkupId(true);
        cardTitle.add(AttributeModifier.replace("title",
                createStringResource("PageRoleAnalysis.chart.access.distribution.title")));
        cardTitle.add(new TooltipBehavior());
        add(cardTitle);

        ChartJsPanel<ChartConfiguration> roleAnalysisChart =
                new ChartJsPanel<>(ID_CHART, new LoadableModel<>() {
                    @Override
                    protected ChartConfiguration load() {
                        return getRoleAnalysisStatistics().getObject();
                    }
                });

        roleAnalysisChart.setOutputMarkupId(true);
        roleAnalysisChart.setOutputMarkupPlaceholderTag(true);
        chartContainer.add(roleAnalysisChart);

        Form<?> toolForm = new MidpointForm<>(ID_TOOL_FORM);
        toolForm.setOutputMarkupId(true);
        add(toolForm);

        AjaxCompositedIconSubmitButton components1 = buildModeButton(roleAnalysisChart);
        toolForm.add(components1);

        AjaxCompositedIconSubmitButton components2 = buildScaleButton(roleAnalysisChart);
        toolForm.add(components2);

        AjaxCompositedIconSubmitButton components3 = buildSortButton(roleAnalysisChart);
        components3.add(new VisibleBehaviour(() -> !chartType.equals(SCATTER)));
        components3.setOutputMarkupId(true);
        toolForm.add(components3);

        AjaxCompositedIconSubmitButton components4 = buildChartTypeButton(roleAnalysisChart);
        toolForm.add(components4);

        AjaxCompositedIconSubmitButton components5 = buildExpandAxisButton(roleAnalysisChart);
        components5.add(new VisibleBehaviour(() -> !chartType.equals(SCATTER)));
        toolForm.add(components5);

        initExportButton(toolForm);
    }

    private void initExportButton(Form<?> toolForm) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_IMPORT_MENU_ITEM,
                IconCssStyle.IN_ROW_STYLE);

        PageDebugDownloadBehaviour<?> downloadBehaviour = initDownloadBehaviour(toolForm);

        AjaxCompositedIconSubmitButton exportCsv = new AjaxCompositedIconSubmitButton(ID_EXPORT_BUTTON, iconBuilder.build(),
                createStringResource("PageRoleAnalysis.export.button.title")) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                downloadBehaviour.initiate(target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        exportCsv.titleAsLabel(true);
        exportCsv.setOutputMarkupId(true);
        exportCsv.add(AttributeModifier.append(CLASS_CSS, BTN_TOOL_CSS));
        exportCsv.add(AttributeModifier.replace(TITLE_CSS, createStringResource("PageRoleAnalysis.export.button.title")));
        exportCsv.add(new TooltipBehavior());
        toolForm.add(exportCsv);
    }

    private @NotNull AjaxCompositedIconSubmitButton buildSortButton(ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton sortMode = new AjaxCompositedIconSubmitButton("settingPanel3", iconBuilder.build(),
                getSortButtonTitle()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIcon getIcon() {
                String scaleIcon;
                if (isSortByGroup) {
                    scaleIcon = GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC;
                } else {
                    scaleIcon = GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_DSC;
                }
                return new CompositedIconBuilder().setBasicIcon(scaleIcon,
                        IconCssStyle.IN_ROW_STYLE).build();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (isScalable) {
                    target.appendJavaScript(applyChartScaleScript());
                }
                target.add(getChartContainer());
                isSortByGroup = !isSortByGroup;
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        sortMode.titleAsLabel(true);
        sortMode.setOutputMarkupId(true);
        sortMode.add(AttributeModifier.append(CLASS_CSS, BTN_TOOL_CSS));
        sortMode.add(AttributeModifier.replace(TITLE_CSS, getSortButtonTitle()));
        sortMode.add(new TooltipBehavior());
        return sortMode;
    }

    private @NotNull AjaxCompositedIconSubmitButton buildChartTypeButton(ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_LINE_CHART_ICON,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton chartTypeButton = new AjaxCompositedIconSubmitButton("settingPanel4",
                iconBuilder.build(),
                getChartTypeButtonTitle()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIcon getIcon() {
                String scaleIcon = chartType.getChartIcon();
                return new CompositedIconBuilder().setBasicIcon(scaleIcon,
                        IconCssStyle.IN_ROW_STYLE).build();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (isScalable) {
                    target.appendJavaScript(applyChartScaleScript());
                }
                target.add(getChartContainer());
                chartType = getNextChartType(chartType);
                if(chartType.equals(SCATTER)){
                    isAxisExpanded = false;
                }
                target.add(roleAnalysisChart);
                target.add(this);
                //tool form
                target.add(this.getParent());
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        chartTypeButton.titleAsLabel(true);
        chartTypeButton.setOutputMarkupId(true);
        chartTypeButton.add(AttributeModifier.append(CLASS_CSS, BTN_TOOL_CSS));
        chartTypeButton.add(AttributeModifier.replace(TITLE_CSS, getChartTypeButtonTitle()));
        chartTypeButton.add(new TooltipBehavior());
        return chartTypeButton;
    }

    private @NotNull AjaxCompositedIconSubmitButton buildExpandAxisButton(ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        LoadableModel<String> expandButtonTitle = new LoadableModel<>() {
            @Override
            protected String load() {
                if (isAxisExpanded) {
                    return "Collapse";
                }
                return "Expand";
            }
        };
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_LINE_CHART_ICON,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton chartTypeButton = new AjaxCompositedIconSubmitButton("settingPanel5",
                iconBuilder.build(),
                expandButtonTitle) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIcon getIcon() {
                String scaleIcon = "fa fa-expand";
                return new CompositedIconBuilder().setBasicIcon(scaleIcon,
                        IconCssStyle.IN_ROW_STYLE).build();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                isAxisExpanded = !isAxisExpanded;
                target.add(getChartContainer());
                target.add(roleAnalysisChart);
                target.add(this);
                //tool form
                target.add(this.getParent());
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        chartTypeButton.titleAsLabel(true);
        chartTypeButton.setOutputMarkupId(true);
        chartTypeButton.add(AttributeModifier.append(CLASS_CSS, BTN_TOOL_CSS));
        chartTypeButton.add(AttributeModifier.replace(TITLE_CSS, getChartTypeButtonTitle()));
        chartTypeButton.add(new TooltipBehavior());
        return chartTypeButton;
    }


    private @NotNull AjaxCompositedIconSubmitButton buildModeButton(ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fe fe-role object-role-color",
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton modeButton = new AjaxCompositedIconSubmitButton("settingPanel1", iconBuilder.build(),
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getModeButtonTitle().getString();
                    }
                }) {
            @Override
            public CompositedIcon getIcon() {
                String scaleIcon;
                if (isUserMode) {
                    scaleIcon = "fa fa-user object-user-color";
                } else {
                    scaleIcon = "fe fe-role object-role-color";
                }
                return new CompositedIconBuilder().setBasicIcon(scaleIcon,
                        IconCssStyle.IN_ROW_STYLE).build();
            }

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(@NotNull AjaxRequestTarget target) {
                isUserMode = !isUserMode;
                target.add(getChartContainer());
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        modeButton.titleAsLabel(true);
        modeButton.setOutputMarkupId(true);
        modeButton.setVisible(true);
        modeButton.add(AttributeModifier.append(CLASS_CSS, BTN_TOOL_CSS));
        modeButton.add(AttributeModifier.replace(TITLE_CSS, getModeButtonTitle()));
        modeButton.add(new TooltipBehavior());
        return modeButton;
    }

    private @NotNull AjaxCompositedIconSubmitButton buildScaleButton(ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-refresh",
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton scaleButton = new AjaxCompositedIconSubmitButton("settingPanel2", iconBuilder.build(),
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getScaleButtonTitle().getString();
                    }
                }) {
            @Override
            public CompositedIcon getIcon() {
                String scaleIcon;
                if (isScalable) {
                    scaleIcon = "fa fa-eye";
                } else {
                    scaleIcon = "fa fa-refresh";
                }
                return new CompositedIconBuilder().setBasicIcon(scaleIcon,
                        IconCssStyle.IN_ROW_STYLE).build();
            }

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                isScalable = !isScalable;
                if (isScalable) {
                    target.appendJavaScript(applyChartScaleScript());
                }
                target.add(getChartContainer());
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        scaleButton.titleAsLabel(true);
        scaleButton.setOutputMarkupId(true);
        scaleButton.setVisible(true);
        scaleButton.add(AttributeModifier.append(CLASS_CSS, BTN_TOOL_CSS));
        scaleButton.add(AttributeModifier.replace(TITLE_CSS, getScaleButtonTitle()));
        scaleButton.add(new TooltipBehavior());
        return scaleButton;
    }

    @NotNull
    private PageDebugDownloadBehaviour<?> initDownloadBehaviour(@NotNull Form<?> toolForm) {
        PageDebugDownloadBehaviour<?> downloadBehaviour = new PageDebugDownloadBehaviour<>() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            protected @NotNull File initFile() {
                MidPointApplication application = getPageBase().getMidpointApplication();
                WebApplicationConfiguration config = application.getWebApplicationConfiguration();
                File folder = new File(config.getExportFolder());
                if (!folder.exists() || !folder.isDirectory()) {
                    folder.mkdir();
                }

                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_m_s"));
                String fileName = "Exported_RoleAnalysisData_" + "_" + currentTime + ".csv";
                File file = new File(folder, fileName);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("RolesCount,UsersCount");
                    writer.newLine();

                    for (RoleAnalysisModel model : prepareRoleAnalysisData()) {
                        writer.write(model.getRolesCount() + "," + model.getUsersCount());
                        writer.newLine();
                    }
                } catch (IOException e) {
                    LOGGER.error("Couldn't export data", e);
                }

                return file;
            }
        };

        toolForm.add(downloadBehaviour);
        return downloadBehaviour;
    }

    public RoleAnalysisAggregateChartModel getRoleAnalysisStatistics() {
        return new RoleAnalysisAggregateChartModel(new LoadableDetachableModel<>() {
            @Override
            protected List<RoleAnalysisModel> load() {
                return prepareRoleAnalysisData();
            }
        }, chartType) {
            @Override
            public String getXAxisTitle() {
                if (chartType.equals(SCATTER)) {
                    return getPageBase().createStringResource("PageRoleAnalysis.chart.yAxis.title").getString();
                }
                return getPageBase().createStringResource("PageRoleAnalysis.chart.xAxis.title").getString();
            }

            @Override
            public String getYAxisTitle() {
                if (chartType.equals(SCATTER)) {
                    return getPageBase().createStringResource("PageRoleAnalysis.chart.xAxis.title").getString();
                }
                return getPageBase().createStringResource("PageRoleAnalysis.chart.yAxis.title").getString();
            }

            @Override
            public String getDatasetUserLabel() {
                if (isUserMode) {
                    return getPageBase().createStringResource("PageRoleAnalysis.chart.dataset.user.userMode.label").getString();
                }
                return getPageBase().createStringResource("PageRoleAnalysis.chart.dataset.user.roleMode.label").getString();
            }

            @Override
            public String getDatasetRoleLabel() {
                if (isUserMode) {
                    return getPageBase().createStringResource("PageRoleAnalysis.chart.dataset.role.userMode.label").getString();
                }
                return getPageBase().createStringResource("PageRoleAnalysis.chart.dataset.role.roleMode.label").getString();
            }
        };
    }

    private @NotNull List<RoleAnalysisModel> prepareRoleAnalysisData() {
        List<RoleAnalysisModel> roleAnalysisModels = new ArrayList<>();

        if (isUserMode) {
            if (!isAxisExpanded) {
                loadUserModeMapStatistics().forEach((key, value) -> roleAnalysisModels.add(new RoleAnalysisModel(key, value)));
            } else {
                loadUserModeMapStatistics().forEach((key, value) -> {
                    for (int i = 0; i < key; i++) {
                        roleAnalysisModels.add(new RoleAnalysisModel(1, value));
                    }
                });
            }

        } else {
            if (!isAxisExpanded) {
                ListMultimap<Integer, ObjectReferenceType> mapView = getIntegerCollectionMap();

                for (Integer key : mapView.keySet()) {
                    List<ObjectReferenceType> objectReferenceTypes = mapView.get(key);
                    roleAnalysisModels.add(new RoleAnalysisModel(objectReferenceTypes.size(), key));
                }
            } else {
                ListMultimap<Integer, ObjectReferenceType> mapView = getIntegerCollectionMap();

                for (Integer key : mapView.keySet()) {
                    List<ObjectReferenceType> objectReferenceTypes = mapView.get(key);
                    int size = objectReferenceTypes.size();
                    for (int i = 0; i < size; i++) {
                        roleAnalysisModels.add(new RoleAnalysisModel(1, key));
                    }
                }
            }
        }

        if (isSortByGroup) {
            roleAnalysisModels.sort((model1, model2) -> Integer.compare(model2.getRolesCount(), model1.getRolesCount()));
        } else {
            roleAnalysisModels.sort((model1, model2) -> Integer.compare(model2.getUsersCount(), model1.getUsersCount()));
        }
        return roleAnalysisModels;
    }

    private @NotNull ListMultimap<Integer, ObjectReferenceType> getIntegerCollectionMap() {
        SearchResultList<PrismContainerValue<?>> aggregateResult = loadAggregateStatistics();
        ListMultimap<Integer, ObjectReferenceType> roleToUserMap = ArrayListMultimap.create();
        for (PrismContainerValue<?> value : aggregateResult) {
            Object objectName = value.findItem(F_NAME).getRealValue();
            String roleName = "Unknown";
            if (objectName != null) {
                roleName = objectName.toString();
            }
            Object assignmentCount = value.findItem(F_ASSIGNMENT).getRealValue();
            String oid = value.findItem(AssignmentType.F_TARGET_REF).getValue().find(ObjectReferenceType.F_OID).toString();
            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(RoleType.COMPLEX_TYPE);
            objectReferenceType.setOid(oid);
            objectReferenceType.setTargetName(PolyStringType.fromOrig(roleName));
            if (assignmentCount != null) {
                roleToUserMap.put(Integer.parseInt(assignmentCount.toString()), objectReferenceType);
            } else {
                roleToUserMap.put(0, objectReferenceType);
            }
        }

        return roleToUserMap;
    }

    @NotNull
    private SearchResultList<PrismContainerValue<?>> loadAggregateStatistics() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        RepositoryService repositoryService = getPageBase().getRepositoryService();
        OperationResult result = new OperationResult(OP_LOAD_STATISTICS);

        Collection<QName> memberRelations = getPageBase().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.MEMBER);

        S_FilterExit filter = roleAnalysisService.buildStatisticsAssignmentSearchFilter(memberRelations);

        SearchResultList<PrismContainerValue<?>> aggregateResult = new SearchResultList<>();

        var spec = AggregateQuery.forType(AssignmentType.class);
        try {
            //TODO check
            spec.retrieve(F_NAME, ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), F_NAME))
                    .retrieve(AssignmentType.F_TARGET_REF)
                    .filter(filter.buildFilter())
                    .count(F_ASSIGNMENT, ItemPath.SELF_PATH)
                    .groupBy(ItemPath.create(new ParentPathSegment(), F_ASSIGNMENT));

            AggregateQuery.ResultItem resultItem = spec.getResultItem(F_ASSIGNMENT);
            spec.orderBy(resultItem, OrderDirection.DESCENDING);
            aggregateResult = repositoryService.searchAggregate(spec, result);

        } catch (SchemaException e) {
            LOGGER.error("Cloud aggregate execute search", e);
        }
        return aggregateResult;
    }

    //TODO move it (it has been changed due to poc needs (activation))
    @NotNull
    private Map<Integer, Integer> loadUserModeMapStatistics() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        ModelService modelService = getPageBase().getModelService();
        OperationResult result = new OperationResult(OP_LOAD_STATISTICS);
        Task task = getPageBase().createSimpleTask(OP_LOAD_STATISTICS);

        Collection<QName> memberRelations = getPageBase().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.MEMBER);

        GetOperationOptionsBuilder optionsBuilder = getPageBase().getSchemaService().getOperationOptionsBuilder();
        optionsBuilder.iterationPageSize(10000);
        Collection<SelectorOptions<GetOperationOptions>> options = optionsBuilder.build();

        ObjectQuery assignmentQuery = roleAnalysisService.buildStatisticsAssignmentSearchFilter(memberRelations).build();

        ListMultimap<String, String> userRoleMap = ArrayListMultimap.create();

        ContainerableResultHandler<AssignmentType> handler;
        handler = (object, parentResult) -> {
            PrismContainerValue<?> prismContainerValue = object.asPrismContainerValue();

            if (prismContainerValue == null) {
                LOGGER.error("Couldn't get prism container value during assignment search");
                return true;
            }

            Map<String, Object> userData = prismContainerValue.getUserData();

            if (userData == null) {
                LOGGER.error("Couldn't get user data during assignment search");
                return true;
            }

            Object ownerOid = userData.get("ownerOid");
            ObjectReferenceType targetRef = object.getTargetRef();
            if (targetRef == null) {
                LOGGER.error("Couldn't get target reference during assignment search");
                return true;
            }

            String roleOid = targetRef.getOid();
            if (ownerOid == null) {
                LOGGER.error("Owner oid retrieved null value during assignment search");
                return true;
            }

            if (roleOid == null) {
                LOGGER.error("Target role oid retrieved null value during search");
                return true;
            }

            userRoleMap.put(ownerOid.toString(), roleOid);

            return true;
        };

        try {
            modelService.searchContainersIterative(AssignmentType.class, assignmentQuery, handler,
                    options, task, result);
        } catch (SchemaException | SecurityViolationException | ConfigurationException | ObjectNotFoundException |
                ExpressionEvaluationException | CommunicationException e) {
            throw new SystemException("Couldn't search assignments", e);
        }

        //group of user has this number of roles
        Map<Integer, Integer> aggregateResult = new HashMap<>();

        userRoleMap.asMap().forEach((key, value) -> {
            int propertiesCount = value.size();
            if (aggregateResult.containsKey(propertiesCount)) {
                aggregateResult.put(propertiesCount, aggregateResult.get(propertiesCount) + 1);
            } else {
                aggregateResult.put(propertiesCount, 1);
            }
        });

        return aggregateResult;
    }

    private StringResourceModel getScaleButtonTitle() {
        if (isScalable) {
            return createStringResource("PageRoleAnalysis.chart.scale.on.button.title");
        }
        return createStringResource("PageRoleAnalysis.chart.scale.off.button.title");
    }

    private StringResourceModel getModeButtonTitle() {
        if (isUserMode) {
            return createStringResource("PageRoleAnalysis.chart.mode.user.button.title");
        }
        return createStringResource("PageRoleAnalysis.chart.mode.role.button.title");
    }

    private StringResourceModel getSortButtonTitle() {
        return createStringResource("PageRoleAnalysis.chart.sort.button.title");
    }

    private StringResourceModel getChartTypeButtonTitle() {
        return createStringResource("PageRoleAnalysis.chart.type.button.title");
    }

    @Contract(pure = true)
    public static @NotNull String applyChartScaleScript() {
        return "MidPointTheme.initScaleResize('#chartScaleContainer');";
    }

    @Override
    public int getWidth() {
        return 80;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }

    private WebMarkupContainer getChartContainer() {
        return (WebMarkupContainer) get(ID_CONTAINER_CHART);
    }

}
