/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.*;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObjects;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.schema.util.ConfigurationSpecificationTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;

import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.createResultDurationText;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.createTaskStateLabel;

public class SimulationResultPanel extends BasePanel<SimulationResultType> {

    private static final String ID_DETAILS = "details";
    private static final String ID_MARKS_CONTAINER = "marksContainer";
    private static final String ID_MARKS = "marks";
    private static final String ID_MARK = "mark";
    private static final String ID_METRICS_CONTAINER = "metricsContainer";
    private static final String ID_METRICS = "metrics";
    private static final String ID_METRIC = "metric";

    private IModel<TaskType> rootTaskModel;
    private IModel<List<DetailsTableItem>> detailsModel;
    private IModel<List<DashboardWidgetType>> metricsModel;

    public SimulationResultPanel(String id, IModel<SimulationResultType> resultModel) {
        super(id, resultModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initModels();
        initLayout();
    }

    private void initLayout() {
        DisplayType displayType = new DisplayType()
                .label(createStringResource("PageSimulationResult.details").getString())
                .icon(new IconType().cssClass("nav-icon fa-solid fa-flask"));
        DetailsTablePanel details = new DetailsTablePanel(ID_DETAILS,
                Model.of(displayType),
                detailsModel);
        add(details);

        SimpleContainerPanel marksContainer = new SimpleContainerPanel(ID_MARKS_CONTAINER,
                createStringResource("PageSimulationResult.eventMarks")) {

            @Override
            protected @NotNull Component createContent(String id) {
                Component content = super.createContent(id);
                content.add(AttributeModifier.replace("class", "row"));
                return content;
            }
        };
        add(marksContainer);

        ListView<DashboardWidgetType> marks = createWidgetList(ID_MARKS, ID_MARK, true);
        marksContainer.add(new VisibleBehaviour(() -> !marks.getModelObject().isEmpty()));
        marksContainer.add(marks);

        SimpleContainerPanel metricsContainer = new SimpleContainerPanel(ID_METRICS_CONTAINER,
                createStringResource("PageSimulationResult.metrics")) {

            @Override
            protected @NotNull Component createContent(String id) {
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

    private void initModels() {

        rootTaskModel = new LoadableDetachableModel<>() {
            @Override
            protected TaskType load() {
                SimulationResultType result = getModelObject();
                if (result == null || result.getRootTaskRef() == null) {
                    return null;
                }

                PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), getPageBase());
                return task != null ? task.asObjectable() : null;
            }
        };

        detailsModel = new LoadableModel<>(false) {

            @Override
            protected List<DetailsTableItem> load() {
                List<DetailsTableItem> list = new ArrayList<>();
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.startTimestamp"),
                        () -> LocalizationUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl<>(getModelObject().getStartTimestamp())))));
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.endTimestamp"),
                        () -> LocalizationUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl<>(getModelObject().getEndTimestamp())))));
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.finishedIn"),
                        () -> createResultDurationText(getModelObject(), SimulationResultPanel.this)));
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.rootTask"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        AjaxButton link = new AjaxButton(id, SimulationResultPanel.this::getTaskName) {

                            @Override
                            protected void onComponentTag(@NotNull ComponentTag tag) {
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
                        return createTaskStateLabel(id, getModel(), rootTaskModel, getPageBase());
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageSimulationResult.productionConfiguration"), () -> {

                    ConfigurationSpecificationType specification = getModelObject().getConfigurationUsed();
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
                List<SimulationMetricValuesType> metrics = getModelObject().getMetric();
                return metrics.stream().map(m -> {

                    BigDecimal value = SimulationMetricValuesTypeUtil.getValue(m);
                    String storedData = MetricWidgetPanel.formatValue(value, LocalizationUtil.findLocale());

                    DashboardWidgetType dw = new DashboardWidgetType();
                    dw.beginData()
                            .sourceType(DashboardWidgetSourceTypeType.METRIC)
                            .metricRef(m.getRef())
                            .storedData(storedData)
                            .end();

                    SimulationMetricReferenceType metricRef = m.getRef();
                    ObjectReferenceType eventMarkRef = metricRef.getEventMarkRef();
                    String metricIdentifier = metricRef.getIdentifier();
                    if (eventMarkRef != null) {
                        PrismObject<MarkType> markObject =
                                WebModelServiceUtils.loadObject(eventMarkRef, getPageBase());
                        DisplayType display;
                        if (markObject != null) {
                            MarkType mark = markObject.asObjectable();
                            display = mark.getDisplay();
                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(mark.getName());
                            }
                            dw.setDisplayOrder(mark.getDisplayOrder());
                        } else {
                            display = new DisplayType();
                            display.setLabel(new PolyStringType(WebComponentUtil.getName(eventMarkRef)));
                        }

                        dw.setDisplay(display);
                    } else if (metricIdentifier != null) {
                        SimulationMetricDefinitionType def =
                                getSimulationResultManager().getMetricDefinition(metricIdentifier);
                        DisplayType display;
                        if (def != null) {
                            display = def.getDisplay();
                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(new PolyStringType(def.getIdentifier()));
                            }
                            dw.setDisplayOrder(def.getDisplayOrder());
                        } else {
                            display = new DisplayType();
                            display.setLabel(new PolyStringType(metricIdentifier));
                        }

                        dw.setDisplay(display);
                    } else if (metricRef.getBuiltIn() != null) {
                        DisplayType display = new DisplayType();
                        display.setLabel(new PolyStringType(LocalizationUtil.createKeyForEnum(metricRef.getBuiltIn())));
                        dw.setDisplay(display);
                    }

                    return dw;
                }).collect(Collectors.toList());
            }
        };
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull IModel<List<DashboardWidgetType>> createWidgetListModel(boolean eventMarkWidgets) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<DashboardWidgetType> load() {
                return metricsModel.getObject().stream()
                        .filter(d -> {
                            SimulationMetricReferenceType ref = d.getData().getMetricRef();
                            if (ref.getBuiltIn() != null) {
                                return false;
                            }

                            return (ref.getEventMarkRef() != null) == eventMarkWidgets;
                        })
                        .sorted(
                                Comparator.comparing(DashboardWidgetType::getDisplayOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
                                        .thenComparing(d -> getTranslatedDashboardWidgetLabel(d), Comparator.nullsFirst(Comparator.naturalOrder()))
                        )
                        .collect(Collectors.toList());
            }
        };
    }

    private String getTranslatedDashboardWidgetLabel(@NotNull DashboardWidgetType widget) {
        PolyStringType label = widget.getDisplay() != null ? widget.getDisplay().getLabel() : null;
        return LocalizationUtil.translatePolyString(label);
    }

    @Contract("_, _, _ -> new")
    private @NotNull ListView<DashboardWidgetType> createWidgetList(String id, String widgetId, boolean eventMarks) {
        return new ListView<>(id, createWidgetListModel(eventMarks)) {

            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
                item.add(new MetricWidgetPanel(widgetId, item.getModel()) {

                    @Override
                    protected boolean isMoreInfoVisible() {
                        DashboardWidgetDataType data = getWidgetData();
                        SimulationMetricReferenceType ref = data.getMetricRef();
                        if (ref == null || ref.getEventMarkRef() == null) {
                            return false;
                        }

                        return (StringUtils.isNotEmpty(data.getStoredData()) && !"0".equals(data.getStoredData()))
                                || !metricValues.getObject().isEmpty();
                    }

                    @Override
                    protected void onMoreInfoPerformed(AjaxRequestTarget target) {
                        openMarkMetricPerformed(getResultOid(), item.getModelObject(), target);
                    }
                });
            }
        };
    }

    private void addBuiltInMetrics(List<DetailsTableItem> result) {
        Map<BuiltInSimulationMetricType, Integer> builtIn = SimulationsGuiUtil.getBuiltInMetrics(getModelObject());

        List<DetailsTableItem> items = new ArrayList<>();

        int totalCount = SimulationResultTypeUtil.getObjectsProcessed(getModelObject());
        for (Map.Entry<BuiltInSimulationMetricType, Integer> entry : builtIn.entrySet()) {
            BuiltInSimulationMetricType identifier = entry.getKey();
            if (identifier == BuiltInSimulationMetricType.ERRORS) {
                // handled later (as last detail item)
                continue;
            }

            int value = entry.getValue();

            items.add(createDetailsItemForBuiltInMetric(identifier, value));
        }

        int unmodifiedCount = SimulationsGuiUtil.getUnmodifiedProcessedObjectCount(getModelObject(), builtIn);

        items.sort(Comparator.comparing(d -> d.getLabel().getObject(), Comparator.naturalOrder()));

        items.add(createDetailsItemForBuiltInMetric(
                createStringResource("PageSimulationResultObject.UnmodifiedObjects"),
                () -> Integer.toString(unmodifiedCount),
                target -> redirectToProcessedObjects(getResultOid(), (BuiltInSimulationMetricType) null, target))
        );

        items.add(createDetailsItemForBuiltInMetric(
                createStringResource("PageSimulationResultObject.AllProcessedObjects"),
                () -> Integer.toString(totalCount),
                target -> redirectToProcessedObjects(getResultOid(), (ObjectProcessingStateType) null, target))
        );

        Integer value = builtIn.get(BuiltInSimulationMetricType.ERRORS);
        if (value != null) {
            items.add(createDetailsItemForBuiltInMetric(BuiltInSimulationMetricType.ERRORS, value));
        }

        result.addAll(items);
    }

    private void redirectToProcessedObjects(String resultOid, BuiltInSimulationMetricType identifier, AjaxRequestTarget target) {
        if (identifier == null) {
            redirectToProcessedObjects(resultOid, ObjectProcessingStateType.UNMODIFIED, target);
            return;
        }

        ObjectProcessingStateType state = SimulationsGuiUtil.builtInMetricToProcessingState(identifier);
        redirectToProcessedObjects(resultOid, state, target);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    private @NotNull DetailsTableItem createDetailsItemForBuiltInMetric(
            IModel<String> name, IModel<String> value, SerializableConsumer<AjaxRequestTarget> onClickHandler) {
        return new DetailsTableItem(name, value) {

            @Override
            public Component createValueComponent(String id) {
                return new AjaxButton(id, getValue()) {

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.setName("a");   // to override default <span> element

                        super.onComponentTag(tag);
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onClickHandler.accept(target);
                    }
                };
            }
        };
    }

    private @NotNull DetailsTableItem createDetailsItemForBuiltInMetric(BuiltInSimulationMetricType identifier, Number value) {
        IModel<String> nameModel = createStringResource("PageSimulationResultObject." + LocalizationUtil.createKeyForEnum(identifier));
        IModel<String> valueModel = () -> MetricWidgetPanel.formatValue(value, LocalizationUtil.findLocale());

        return createDetailsItemForBuiltInMetric(nameModel, valueModel, target -> redirectToProcessedObjects(getResultOid(), identifier, target));
    }

    private @Nullable String getTaskName() {
        TaskType task = rootTaskModel.getObject();
        if (task == null) {
            return null;
        }

        return WebComponentUtil.getDisplayNameOrName(task.asPrismObject());
    }

    private String getResultOid() {
        return getModelObject().getOid();
    }

    private SimulationResultManager getSimulationResultManager() {
        return getPageBase().getSimulationResultManager();
    }

    // Navigation action handlers

    private void redirectToProcessedObjects(String resultOid, ObjectProcessingStateType state, AjaxRequestTarget target) {
        navigateToSimulationResultObjects(resultOid, null, state, target);
    }

    private void openMarkMetricPerformed(String resultOid, DashboardWidgetType widget, AjaxRequestTarget target) {
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

        navigateToSimulationResultObjects(resultOid, ref, null, target);
    }

    protected void navigateToSimulationResultObjects(
            @NotNull String resultOid,
            @Nullable ObjectReferenceType ref,
            @Nullable ObjectProcessingStateType state, AjaxRequestTarget target) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, resultOid);
        if (state != null) {
            params.set(PageSimulationResultObjects.PAGE_QUERY_PARAMETER, state.value());
        }
        if (ref != null && !StringUtils.isEmpty(ref.getOid())) {
            params.set(SimulationPage.PAGE_PARAMETER_MARK_OID, ref.getOid());
        }
        getPageBase().navigateToNext(PageSimulationResultObjects.class, params);
    }

    protected void redirectToTaskDetails() {
        TaskType task = rootTaskModel.getObject();
        if (task == null) {
            return;
        }

        PageParameters params = new PageParameters();
        params.set(OnePageParameterEncoder.PARAMETER, task.getOid());
        getPageBase().navigateToNext(PageTask.class, params);
    }
}
