/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ConfigurationSpecificationTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_RESULT_URL,
                        label = "PageSimulationResults.auth.simulationResult.label",
                        description = "PageSimulationResults.auth.simulationResult.description")
        }
)
public class PageSimulationResult extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DETAILS = "details";
    private static final String ID_MARKS_CONTAINER = "marksContainer";
    private static final String ID_MARKS = "marks";
    private static final String ID_MARK = "mark";
    private static final String ID_METRICS_CONTAINER = "metricsContainer";
    private static final String ID_METRICS = "metrics";
    private static final String ID_METRIC = "metric";

    private IModel<SimulationResultType> resultModel;

    private IModel<TaskType> rootTaskModel;

    private IModel<List<DetailsTableItem>> detailsModel;

    private IModel<List<DashboardWidgetType>> metricsModel;

    public PageSimulationResult() {
        this(new PageParameters());
    }

    public PageSimulationResult(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {
            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResult.this);
            }
        };

        rootTaskModel = new LoadableDetachableModel<>() {
            @Override
            protected TaskType load() {
                SimulationResultType result = resultModel.getObject();
                if (result == null || result.getRootTaskRef() == null) {
                    return null;
                }

                PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), PageSimulationResult.this);
                return task != null ? task.asObjectable() : null;
            }
        };

        detailsModel = new LoadableModel<>(false) {

            @Override
            protected List<DetailsTableItem> load() {
                List<DetailsTableItem> list = new ArrayList<>();
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.startTimestamp"),
                        () -> LocalizationUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl(resultModel.getObject().getStartTimestamp())))));
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.endTimestamp"),
                        () -> LocalizationUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl(resultModel.getObject().getEndTimestamp())))));
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.finishedIn"),
                        () -> createResultDurationText(resultModel.getObject(), PageSimulationResult.this)));
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.rootTask"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        AjaxButton link = new AjaxButton(id, () -> getTaskName()) {

                            @Override
                            protected void onComponentTag(ComponentTag tag) {
                                tag.setName("a");   // to override default <span> element

                                super.onComponentTag(tag);
                            }

                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                redirectToTaskDetails();
                            }
                        };
                        link.add(new VisibleBehaviour(() -> rootTaskModel.getObject() != null));

                        return link;
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.status"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        return createTaskStateLabel(id, resultModel, rootTaskModel, PageSimulationResult.this);
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.productionConfiguration"), () -> {

                    ConfigurationSpecificationType specification = resultModel.getObject().getConfigurationUsed();
                    if (ConfigurationSpecificationTypeUtil.isProductionConfiguration(specification)) {
                        return getString("PageSimulationResult.production");
                    }

                    return getString("PageSimulationResult.development");
                }));

                addBuiltInMetrics(list);

                return list;
            }
        };

        metricsModel = new LoadableDetachableModel<>() {

            @Override
            protected List<DashboardWidgetType> load() {
                List<SimulationMetricValuesType> metrics = resultModel.getObject().getMetric();
                return metrics.stream().map(m -> {

                    BigDecimal value = SimulationMetricValuesTypeUtil.getValue(m);
                    String storedData = MetricWidgetPanel.formatValue(value, getPrincipal().getLocale());

                    DashboardWidgetType dw = new DashboardWidgetType();
                    dw.beginData()
                            .sourceType(DashboardWidgetSourceTypeType.METRIC)
                            .metricRef(m.getRef())
                            .storedData(storedData)
                            .end();

                    SimulationMetricReferenceType metricRef = m.getRef();
                    if (metricRef.getEventMarkRef() != null) {
                        PrismObject<MarkType> markObject =
                                WebModelServiceUtils.loadObject(metricRef.getEventMarkRef(), PageSimulationResult.this);
                        if (markObject != null) {
                            MarkType mark = markObject.asObjectable();
                            DisplayType display = mark.getDisplay();
                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(mark.getName());
                            }
                            dw.setDisplay(display);
                            dw.setDisplayOrder(mark.getDisplayOrder());
                        }
                    } else if (metricRef.getIdentifier() != null) {
                        SimulationMetricDefinitionType def =
                                getSimulationResultManager().getMetricDefinition(metricRef.getIdentifier());
                        if (def != null) {
                            DisplayType display = def.getDisplay();
                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(new PolyStringType(def.getIdentifier()));
                            }
                            dw.setDisplay(display);
                            dw.setDisplayOrder(def.getDisplayOrder());
                        }
                    } else {
                        // built-in -> ignored
                    }

                    return dw;
                }).collect(Collectors.toList());
            }
        };
    }

    private void addBuiltInMetrics(List<DetailsTableItem> result) {
        List<SimulationMetricValuesType> metrics = resultModel.getObject().getMetric();
        List<SimulationMetricValuesType> builtIn = metrics.stream().filter(m -> m.getRef() != null && m.getRef().getBuiltIn() != null)
                .collect(Collectors.toList());

        List<DetailsTableItem> items = new ArrayList<>();

        for (SimulationMetricValuesType metric : builtIn) {
            BuiltInSimulationMetricType identifier = metric.getRef().getBuiltIn();

            BigDecimal value = SimulationMetricValuesTypeUtil.getValue(metric);
            items.add(new DetailsTableItem(
                    createStringResource("PageSimulationResultObject." + WebComponentUtil.createEnumResourceKey(identifier)),
                    () -> MetricWidgetPanel.formatValue(value, getPrincipal().getLocale())) {

                @Override
                public Component createValueComponent(String id) {
                    AjaxButton link = new AjaxButton(id, getValue()) {

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            tag.setName("a");   // to override default <span> element

                            super.onComponentTag(tag);
                        }

                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                            redirectToProcessedObjects(identifier);
                        }
                    };

                    return link;
                }
            });
        }

        items.sort(Comparator.comparing(d -> d.getLabel().getObject(), Comparator.naturalOrder()));

        result.addAll(items);
    }

    private void redirectToProcessedObjects(BuiltInSimulationMetricType identifier) {
        ObjectProcessingStateType state = null;
        switch (identifier) {
            case ADDED:
                state = ObjectProcessingStateType.ADDED;
                break;
            case MODIFIED:
                state = ObjectProcessingStateType.MODIFIED;
                break;
            case DELETED:
                state = ObjectProcessingStateType.DELETED;
                break;
        }

        ObjectFilter filter = null;
        if (state != null) {
            filter = getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                    .item(SimulationResultProcessedObjectType.F_STATE).eq(state)
                    .buildFilter();
        }

        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());

        navigateToNext(new PageSimulationResultObjects(params, filter));
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected IModel<String> createTitleModel() {
                return PageSimulationResult.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResult.this.onBackPerformed(target);
            }

            @Override
            protected AjaxLink createNextButton(String id, IModel<String> nextTitle) {
                AjaxIconButton next = new AjaxIconButton(id, () -> "fa-solid fa-magnifying-glass mr-2", () -> "View results") {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onViewAllPerformed();
                    }
                };
                next.showTitleAsLabel(true);
                next.add(AttributeAppender.append("class", "btn btn-primary"));

                return next;
            }
        };
        add(navigation);

        DetailsTablePanel details = new DetailsTablePanel(ID_DETAILS,
                () -> "fa-solid fa-circle-question",
                createStringResource("PageSimulationResult.details"),
                detailsModel);
        add(details);

        SimpleContainerPanel marksContainer = new SimpleContainerPanel(ID_MARKS_CONTAINER, createStringResource("PageSimulationResult.eventMarks")) {

            @Override
            protected Component createContent(String id) {
                Component content = super.createContent(id);
                content.add(AttributeModifier.replace("class", "row"));
                return content;
            }
        };
        add(marksContainer);

        ListView<DashboardWidgetType> marks = createWidgetList(ID_MARKS, ID_MARK, true);
        marksContainer.add(new VisibleBehaviour(() -> !marks.getModelObject().isEmpty()));
        marksContainer.add(marks);

        SimpleContainerPanel metricsContainer = new SimpleContainerPanel(ID_METRICS_CONTAINER, createStringResource("PageSimulationResult.metrics")) {

            @Override
            protected Component createContent(String id) {
                Component content = super.createContent(id);
                content.add(AttributeModifier.replace("class", "row"));
                return content;
            }
        };
        metricsContainer.add(new VisibleBehaviour(() -> !createWidgetListModel(true).getObject().isEmpty()));
        add(metricsContainer);

        ListView<DashboardWidgetType> metrics = createWidgetList(ID_METRICS, ID_METRIC, false);
        metricsContainer.add(new VisibleBehaviour(() -> !metrics.getModelObject().isEmpty()));
        metricsContainer.add(metrics);
    }

    private void onViewAllPerformed() {
        PageParameters params = new PageParameters();
        params.add(SimulationPage.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());

        navigateToNext(PageSimulationResultObjects.class, params);
    }

    private ListView<DashboardWidgetType> createWidgetList(String id, String widgetId, boolean eventMarks) {
        return new ListView<>(id, createWidgetListModel(eventMarks)) {

            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
                item.add(new MetricWidgetPanel(widgetId, item.getModel()) {

                    @Override
                    protected void onMoreInfoPerformed(AjaxRequestTarget target) {
                        openMarkMetricPerformed(target, item.getModelObject());
                    }
                });
            }
        };
    }

    private IModel<List<DashboardWidgetType>> createWidgetListModel(boolean eventMarkWidgets) {

        return new LoadableDetachableModel<>() {

            @Override
            protected List<DashboardWidgetType> load() {
                return metricsModel.getObject().stream()
                        .filter(d -> d.getData().getMetricRef().getEventMarkRef() != null ? eventMarkWidgets : !eventMarkWidgets)
                        .sorted(
                                Comparator.comparing(DashboardWidgetType::getDisplayOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
                                        .thenComparing(d -> getTranslatedDashboardWidgetLabel(d), Comparator.nullsFirst(Comparator.naturalOrder()))
                        )
                        .collect(Collectors.toList());
            }
        };
    }

    private String getTranslatedDashboardWidgetLabel(DashboardWidgetType widget) {
        PolyStringType label = widget.getDisplay() != null ? widget.getDisplay().getLabel() : null;
        return LocalizationUtil.translatePolyString(label);
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    private void openMarkMetricPerformed(AjaxRequestTarget target, DashboardWidgetType widget) {
        if (widget == null || widget.getData() == null) {
            return;
        }

        DashboardWidgetDataType data = widget.getData();
        if (data.getMetricRef() == null) {
            return;
        }

        ObjectReferenceType ref = data.getMetricRef().getEventMarkRef();
        if (ref == null || StringUtils.isEmpty(ref.getOid())) {
            return;
        }

        PageParameters params = new PageParameters();
        params.add(SimulationPage.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());
        params.add(SimulationPage.PAGE_PARAMETER_MARK_OID, ref.getOid());

        navigateToNext(PageSimulationResultObjects.class, params);
    }

    public static String createResultDurationText(SimulationResultType result, Component panel) {
        XMLGregorianCalendar start = result.getStartTimestamp();
        if (start == null) {
            return panel.getString("SimulationResultsPanel.notStartedYet");
        }

        XMLGregorianCalendar end = result.getEndTimestamp();
        if (end == null) {
            end = MiscUtil.asXMLGregorianCalendar(new Date());
        }

        long duration = end.toGregorianCalendar().getTimeInMillis() - start.toGregorianCalendar().getTimeInMillis();
        if (duration < 0) {
            return null;
        }

        return DurationFormatUtils.formatDurationWords(duration, true, true);
    }

    public static Component createTaskStateLabel(String
            id, IModel<SimulationResultType> model, IModel<TaskType> taskModel, PageBase page) {
        IModel<TaskExecutionStateType> stateModel = () -> {
            TaskType task;
            if (taskModel != null) {
                task = taskModel.getObject();
            } else {
                SimulationResultType result = model.getObject();
                if (result == null || result.getRootTaskRef() == null) {
                    return null;
                }

                PrismObject<TaskType> obj = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
                task = obj != null ? obj.asObjectable() : null;
            }

            return task != null ? task.getExecutionState() : null;
        };

        Label label = new Label(id, () -> {
            if (model.getObject().getEndTimestamp() != null) {
                return page.getString("PageSimulationResult.finished");
            }

            TaskExecutionStateType state = stateModel.getObject();
            if (state == null) {
                return null;
            }

            return page.getString(state);
        });
        label.add(AttributeAppender.replace("class", () -> {
            TaskExecutionStateType state = stateModel.getObject();
            if (state == TaskExecutionStateType.RUNNABLE || state == TaskExecutionStateType.RUNNING) {
                return Badge.State.SUCCESS.getCss();
            }

            return Badge.State.SECONDARY.getCss();
        }));

        return label;
    }

    private String getTaskName() {
        TaskType task = rootTaskModel.getObject();
        if (task == null) {
            return null;
        }

        return WebComponentUtil.getDisplayNameOrName(task.asPrismObject());
    }

    private void redirectToTaskDetails() {
        TaskType task = rootTaskModel.getObject();
        if (task == null) {
            return;
        }

        PageParameters params = new PageParameters();
        params.set(OnePageParameterEncoder.PARAMETER, task.getOid());
        navigateToNext(PageTask.class, params);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private IModel<String> createTitleModel() {
        return () -> WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject());
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(createTitleModel(), this.getClass(), getPageParameters()));
    }
}
