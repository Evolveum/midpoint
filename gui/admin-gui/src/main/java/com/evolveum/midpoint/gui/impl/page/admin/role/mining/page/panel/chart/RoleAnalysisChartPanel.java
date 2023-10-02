/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.file.File;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisAggregateChartModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisModel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public class RoleAnalysisChartPanel extends BasePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisChartPanel.class);

    private static final String ID_TOOL_FORM = "toolForm";
    private static final String ID_SCALE_BUTTON = "scaleButton";
    private static final String ID_EXPORT_BUTTON = "exportButton";
    private static final String ID_SORT_BUTTON = "sortButton";
    private static final String ID_CONTAINER_CHART = "container";
    private static final String ID_CHART = "chart";
    private static final String ID_CARD_TITLE = "cardTitle";
    private static final String DOT_CLASS = RoleAnalysisChartPanel.class.getName() + ".";
    private static final String OP_LOAD_STATISTICS = DOT_CLASS + "loadRoleAnalysisStatistics";

    private boolean isSortByGroup = false;
    private boolean isScalable = false;

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

        Label cardTitle = new Label(ID_CARD_TITLE, createStringResource("PageRoleAnalysis.chart.title"));
        cardTitle.setOutputMarkupId(true);
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

        initScaleButton(chartContainer, roleAnalysisChart, toolForm);

        initSortButton(chartContainer, roleAnalysisChart, toolForm);

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
        toolForm.add(exportCsv);
    }

    private void initSortButton(WebMarkupContainer chartContainer, ChartJsPanel<ChartConfiguration> roleAnalysisChart, Form<?> toolForm) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton sortMode = new AjaxCompositedIconSubmitButton(ID_SORT_BUTTON, iconBuilder.build(),
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getSortButtonTitle().getString();
                    }
                }) {
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
        toolForm.add(sortMode);
    }

    private void initScaleButton(WebMarkupContainer chartContainer, ChartJsPanel<ChartConfiguration> roleAnalysisChart,
            Form<?> toolForm) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-refresh",
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton reset = new AjaxCompositedIconSubmitButton(ID_SCALE_BUTTON, iconBuilder.build(),
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
        reset.titleAsLabel(true);
        reset.setOutputMarkupId(true);
        reset.add(AttributeAppender.append("class", "btn btn-tool"));
        toolForm.add(reset);
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
        });
    }

    private List<RoleAnalysisModel> prepareRoleAnalysisData() {
        List<RoleAnalysisModel> roleAnalysisModels = new ArrayList<>();
        ListMultimap<Integer, ObjectReferenceType> mapView = getIntegerCollectionMap();

        for (Integer key : mapView.keySet()) {
            List<ObjectReferenceType> objectReferenceTypes = mapView.get(key);
            roleAnalysisModels.add(new RoleAnalysisModel(objectReferenceTypes.size(), key));
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
        OperationResult result = new OperationResult(OP_LOAD_STATISTICS);

        SearchResultList<PrismContainerValue<?>> aggregateResult = new SearchResultList<>();

        var spec = AggregateQuery.forType(AssignmentType.class);
        try {
            spec.retrieve(F_NAME, ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), F_NAME))
                    .retrieve(AssignmentType.F_TARGET_REF)
                    .filter(PrismContext.get().queryFor(AssignmentType.class).ownedBy(UserType.class, UserType.F_ASSIGNMENT)
                            .and().ref(AssignmentType.F_TARGET_REF).type(RoleType.class).buildFilter())
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

    private StringResourceModel getScaleButtonTitle() {
        if (isScalable) {
            return createStringResource("PageRoleAnalysis.chart.scale.on.button.title");
        }
        return createStringResource("PageRoleAnalysis.chart.scale.off.button.title");
    }

    private StringResourceModel getSortButtonTitle() {
        return createStringResource("PageRoleAnalysis.chart.sort.button.title");
    }

    public static String applyChartScaleScript() {
        return "MidPointTheme.initScaleResize('#chartScaleContainer');";
    }

}
