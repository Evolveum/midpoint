/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
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

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.ChartType;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.file.File;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.RoleAnalysisAggregateChartModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

import javax.xml.namespace.QName;

public class RoleAnalysisInfoAccessPanel extends BasePanel<String> implements Popupable {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisInfoAccessPanel.class);
    private static final String ID_TOOL_FORM = "toolForm";
    private static final String ID_CONTAINER_CHART = "container";
    private static final String ID_CHART = "chart";
    private static final String ID_CARD_TITLE = "cardTitle";
    private static final String DOT_CLASS = RoleAnalysisInfoAccessPanel.class.getName() + ".";
    private static final String OP_LOAD_STATISTICS = DOT_CLASS + "loadRoleAnalysisStatistics";
    private static final String ID_EXPORT_BUTTON = "exportButton";

    private static final String ID_SETTING_PANEL = "settingPanel";

    private boolean isSortByGroup = false;
    ChartType chartType = ChartType.BAR;
    private boolean isScalable = false;
    private boolean isUserMode = false;

    public RoleAnalysisInfoAccessPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initChartPart();
    }

    WebMarkupContainer chartContainer;

    private void initChartPart() {
        chartContainer = new WebMarkupContainer(ID_CONTAINER_CHART);
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);


        IconWithLabel cardTitle = new IconWithLabel(ID_CARD_TITLE,
                createStringResource("PageRoleAnalysis.chart.access.distribution.title")){
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-chart-pie";
            }
        };
        cardTitle.setOutputMarkupId(true);
        cardTitle.add(AttributeModifier.replace("title", createStringResource("PageRoleAnalysis.chart.access.distribution.title")));
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

        IModel<List<DetailsTableItem>> listIModel = loadDetailsModel(roleAnalysisChart);

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON,
                LayeredIconCssStyle.IN_ROW_STYLE);

        AjaxCompositedIconSubmitButton settingPanel = new AjaxCompositedIconSubmitButton(ID_SETTING_PANEL, iconBuilder.build(),
                createStringResource("PageRoleAnalysis.chart.setting.button.title")) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisChartSettingPanel roleAnalysisChartSettingPanel = new RoleAnalysisChartSettingPanel(
                        ((PageBase) getPage()).getMainPopupBodyId(), null, listIModel);
                ((PageBase) getPage()).showMainPopup(roleAnalysisChartSettingPanel, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        settingPanel.titleAsLabel(true);
        settingPanel.setOutputMarkupId(true);
        settingPanel.add(AttributeAppender.append("class", "btn btn-tool"));
        settingPanel.add(AttributeModifier.replace("title", createStringResource("PageRoleAnalysis.export.button.title")));
        settingPanel.add(new TooltipBehavior());
        toolForm.add(settingPanel);

        initExportButton(toolForm);

    }


    private void initExportButton(Form<?> toolForm) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_IMPORT_MENU_ITEM,
                LayeredIconCssStyle.IN_ROW_STYLE);

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
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        exportCsv.titleAsLabel(true);
        exportCsv.setOutputMarkupId(true);
        exportCsv.add(AttributeAppender.append("class", "btn btn-tool"));
        exportCsv.add(AttributeModifier.replace("title", createStringResource("PageRoleAnalysis.export.button.title")));
        exportCsv.add(new TooltipBehavior());
        toolForm.add(exportCsv);
    }

    private AjaxCompositedIconSubmitButton buildSortButton(String id, ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton sortMode = new AjaxCompositedIconSubmitButton(id, iconBuilder.build(),
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
                        LayeredIconCssStyle.IN_ROW_STYLE).build();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (isScalable) {
                    target.appendJavaScript(applyChartScaleScript());
                }
                target.add(chartContainer);
                isSortByGroup = !isSortByGroup;
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        sortMode.titleAsLabel(true);
        sortMode.setOutputMarkupId(true);
        sortMode.add(AttributeAppender.append("class", "btn btn-tool"));
        sortMode.add(AttributeModifier.replace("title", getSortButtonTitle()));
        sortMode.add(new TooltipBehavior());
        return sortMode;
    }

    private @NotNull AjaxCompositedIconSubmitButton buildChartTypeButton(String id, ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_LINE_CHART_ICON,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton chartTypeButton = new AjaxCompositedIconSubmitButton(id,
                iconBuilder.build(),
                getChartTypeButtonTitle()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIcon getIcon() {
                String scaleIcon = chartType.getChartIcon();
                return new CompositedIconBuilder().setBasicIcon(scaleIcon,
                        LayeredIconCssStyle.IN_ROW_STYLE).build();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (isScalable) {
                    target.appendJavaScript(applyChartScaleScript());
                }
                target.add(chartContainer);
                chartType = getNextChartType(chartType);
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        chartTypeButton.titleAsLabel(true);
        chartTypeButton.setOutputMarkupId(true);
        chartTypeButton.add(AttributeAppender.append("class", "btn btn-tool"));
        chartTypeButton.add(AttributeModifier.replace("title", getChartTypeButtonTitle()));
        chartTypeButton.add(new TooltipBehavior());
        return chartTypeButton;
    }

    private AjaxCompositedIconSubmitButton buildModeButton(String id, ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fe fe-role object-role-color",
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton modeButton = new AjaxCompositedIconSubmitButton(id, iconBuilder.build(),
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
                        LayeredIconCssStyle.IN_ROW_STYLE).build();
            }

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                isUserMode = !isUserMode;
                target.add(chartContainer);
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        modeButton.titleAsLabel(true);
        modeButton.setOutputMarkupId(true);
        modeButton.setVisible(true);
        modeButton.add(AttributeAppender.append("class", "btn btn-tool"));
        modeButton.add(AttributeModifier.replace("title", getModeButtonTitle()));
        modeButton.add(new TooltipBehavior());
        return modeButton;
    }

    private AjaxCompositedIconSubmitButton buildScaleButton(String id, ChartJsPanel<ChartConfiguration> roleAnalysisChart) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-refresh",
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton scaleButton = new AjaxCompositedIconSubmitButton(id, iconBuilder.build(),
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
                        LayeredIconCssStyle.IN_ROW_STYLE).build();
            }

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                isScalable = !isScalable;
                if (isScalable) {
                    target.appendJavaScript(applyChartScaleScript());
                }
                target.add(chartContainer);
                target.add(roleAnalysisChart);
                target.add(this);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        scaleButton.titleAsLabel(true);
        scaleButton.setOutputMarkupId(true);
        scaleButton.setVisible(true);
        scaleButton.add(AttributeAppender.append("class", "btn btn-tool"));
        scaleButton.add(AttributeModifier.replace("title", getScaleButtonTitle()));
        scaleButton.add(new TooltipBehavior());
        return scaleButton;
    }

    @NotNull
    private PageDebugDownloadBehaviour<?> initDownloadBehaviour(Form<?> toolForm) {
        PageDebugDownloadBehaviour<?> downloadBehaviour = new PageDebugDownloadBehaviour<>() {
            @Override
            protected File initFile() {
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
            loadUserModeMapStatistics().forEach((key, value) -> roleAnalysisModels.add(new RoleAnalysisModel(key, value)));
        } else {
            ListMultimap<Integer, ObjectReferenceType> mapView = getIntegerCollectionMap();

            for (Integer key : mapView.keySet()) {
                List<ObjectReferenceType> objectReferenceTypes = mapView.get(key);
                roleAnalysisModels.add(new RoleAnalysisModel(objectReferenceTypes.size(), key));
            }
        }

        if (isSortByGroup) {
            roleAnalysisModels.sort((model1, model2) -> Integer.compare(model2.getRolesCount(), model1.getRolesCount()));
        } else {
            roleAnalysisModels.sort((model1, model2) -> Integer.compare(model2.getUsersCount(), model1.getUsersCount()));
        }
        return roleAnalysisModels;
    }

    private ListMultimap<Integer, ObjectReferenceType> getIntegerCollectionMap() {
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
        RepositoryService repositoryService = getPageBase().getRepositoryService();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        OperationResult result = new OperationResult(OP_LOAD_STATISTICS);

        Collection<QName> memberRelations = getPageBase().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.MEMBER);

        S_FilterExit filter = roleAnalysisService.buildStatisticsAssignmentSearchFilter(memberRelations);

        SearchResultList<PrismContainerValue<?>> aggregateResult = new SearchResultList<>();

        var spec = AggregateQuery.forType(AssignmentType.class);
        try {
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

    @NotNull
    private Map<Integer, Integer> loadUserModeMapStatistics() {
        RepositoryService repositoryService = getPageBase().getRepositoryService();
        OperationResult result = new OperationResult(OP_LOAD_STATISTICS);

        Map<Integer, Integer> aggregateResult = new HashMap<>();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                List<String> properties = getRolesOidAssignment(object.asObjectable());
                int propertiesCount = properties.size();
                if (aggregateResult.containsKey(propertiesCount)) {
                    aggregateResult.put(propertiesCount, aggregateResult.get(propertiesCount) + 1);
                } else {
                    aggregateResult.put(propertiesCount, 1);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        try {
            repositoryService.searchObjectsIterative(UserType.class, null, resultHandler,
                    null, false, result);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't count user mode statistics ", e);
        }
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

    public static String applyChartScaleScript() {
        return "MidPointTheme.initScaleResize('#chartScaleContainer');";
    }

    public void addPatternItems(RepeatingView repeatingView) {

    }

    public void addOutliersItems(RepeatingView repeatingView) {

    }

    private @NotNull IModel<List<DetailsTableItem>> loadDetailsModel(ChartJsPanel<ChartConfiguration> roleAnalysisChart) {

        List<DetailsTableItem> detailsModel = List.of(
                new DetailsTableItem(createStringResource("PageRoleAnalysis.chart.setting.object.mode"),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        return buildModeButton(id, roleAnalysisChart);
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, getLabel()) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("PageRoleAnalysis.chart.setting.object.mode.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource("PageRoleAnalysis.chart.setting.scale.mode"),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        return buildScaleButton(id, roleAnalysisChart);
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, getLabel()) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("PageRoleAnalysis.chart.setting.scale.mode.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource("PageRoleAnalysis.chart.setting.sort.mode"),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        return buildSortButton(id, roleAnalysisChart);
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, getLabel()) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("PageRoleAnalysis.chart.setting.sort.mode.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource("PageRoleAnalysis.chart.setting.chart.type"),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        return buildChartTypeButton(id, roleAnalysisChart);
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, getLabel()) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("PageRoleAnalysis.chart.setting.chart.type.help");
                            }
                        };
                    }
                }
        );

        return Model.ofList(detailsModel);
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
}
