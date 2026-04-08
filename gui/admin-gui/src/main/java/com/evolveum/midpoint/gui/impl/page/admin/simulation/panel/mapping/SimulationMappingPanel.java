/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.MappingUtil.extractMappingInfo;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.SimulationWebUtil.loadAvailableMarksModel;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.SimulationWebUtil.processedObjectsCountWidget;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion.SimulationCorrelationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.util.MappingUtil;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObject;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Main panel for displaying mapping simulation results.
 *
 * <p>Shows mapping header information, summary metric widgets,
 * and a table of processed objects related to the simulated mapping.</p>
 */
public class SimulationMappingPanel extends BasePanel<SimulationResultType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationMappingPanel.class);

    private static final String ID_FORM = "form";
    private static final String ID_HEADER = "header";
    private static final String ID_DASHBOARD = "dashboard";
    private static final String ID_WIDGET = "widget";
    private static final String ID_TABLE = "table";

    private IModel<List<DashboardWidgetType>> metricsModel;
    IModel<String> selectedMarkOidModel = Model.of(MARK_SHADOW_CORRELATION_OWNER_FOUND.value());

    List<String> mappingMarksOids = List.of(
            MARK_ITEM_VALUE_ADDED.value(),
            MARK_ITEM_VALUE_REMOVED.value(),
            MARK_ITEM_VALUE_MODIFIED.value(),
            MARK_ITEM_VALUE_NOT_CHANGED.value(),
            MARK_ITEM_VALUE_CHANGE_NOT_APPLIED.value());

    public SimulationMappingPanel(String id, IModel<SimulationResultType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        loadMetricModel();

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        initHeader(form);
        initDashboard(form);
        initTable(form);
    }

    private void initHeader(@NotNull MidpointForm<?> form) {
        MappingUtil.MappingInfo mappingInfo = extractMappingInfo(getPageBase(), getModelObject());

        SimulationMappingHeaderPanel header = new SimulationMappingHeaderPanel(ID_HEADER, () -> mappingInfo);
        header.setOutputMarkupId(true);
        form.add(header);
    }
    private void loadMetricModel() {
        metricsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<DashboardWidgetType> load() {
                List<SimulationMetricValuesType> metrics = getModelObject().getMetric();

                List<DashboardWidgetType> widgets = metrics.stream()
                        .map(m -> {
                            SimulationMetricReferenceType ref = m.getRef();
                            ObjectReferenceType eventMarkRef = ref.getEventMarkRef();
                            if (eventMarkRef == null
                                    || eventMarkRef.getOid() == null
                                    || !mappingMarksOids.contains(eventMarkRef.getOid())) {
                                return null;
                            }

                            BigDecimal value = SimulationMetricValuesTypeUtil.getValue(m);
                            String storedData = MetricWidgetPanel.formatValue(value, LocalizationUtil.findLocale());

                            DashboardWidgetType widget = new DashboardWidgetType();
                            widget.beginData()
                                    .sourceType(DashboardWidgetSourceTypeType.METRIC)
                                    .metricRef(m.getRef())
                                    .storedData(storedData)
                                    .end();
                            return widget;
                        })
                        .filter(Objects::nonNull)
                        .sorted((left, right) -> Integer.compare(
                                getMarkOrder(left),
                                getMarkOrder(right)))
                        .collect(Collectors.toList());

                DashboardWidgetType totalProcessedWidget =
                        processedObjectsCountWidget(getPageBase(), getModelObject(), LOGGER);
                if (totalProcessedWidget != null) {
                    widgets.add(totalProcessedWidget);
                }

                return widgets;
            }
        };
    }

    private int getMarkOrder(@NotNull DashboardWidgetType widget) {
        SimulationMetricReferenceType metricRef = widget.getData() != null
                ? widget.getData().getMetricRef()
                : null;
        ObjectReferenceType eventMarkRef = metricRef != null ? metricRef.getEventMarkRef() : null;
        String oid = eventMarkRef != null ? eventMarkRef.getOid() : null;

        int index = mappingMarksOids.indexOf(oid);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private void initDashboard(@NotNull MidpointForm<?> form) {
        ListView<DashboardWidgetType> components = new ListView<>(ID_DASHBOARD, () -> new ArrayList<>(metricsModel.getObject())) {

            @Override
            protected void populateItem(@NotNull ListItem<DashboardWidgetType> item) {
                item.add(new MetricWidgetPanel(ID_WIDGET, item.getModel()) {

                    @Override
                    protected @NotNull CompositedIconBuilder createCompositeIcon(DisplayType d) {
                        CompositedIconBuilder builder = new CompositedIconBuilder();
                        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(d), IconCssStyle.CENTER_METRIC_STYLE)
                                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(d));
                        return builder;
                    }

                    @Override
                    protected String getDisplayContainerAdditionalCssClass() {
                        return "align-items-start px-3";
                    }

                    @Override
                    protected String getAdditionalLayerIconCssClass() {
                        return "up-30";
                    }

                    @Override
                    protected boolean isMoreInfoVisible() {
                        return true;
                    }

                    @Override
                    protected IModel<String> getActionLinkLabelModel() {
                        return createStringResource("SimulationMappingPanel.viewProcessedObjects");
                    }

                    @Override
                    protected IModel<String> getActionLinkIconModel() {
                        return Model.of("");
                    }

                    @Override
                    protected void onMoreInfoPerformed(AjaxRequestTarget target) {
                        DashboardWidgetType widget = getModelObject();
                        if (widget == null || widget.getData() == null) {
                            selectedMarkOidModel.setObject(null);
                            return;
                        } else {
                            SimulationMetricReferenceType metricRef = widget.getData().getMetricRef();
                            ObjectReferenceType markRef = metricRef != null ? metricRef.getEventMarkRef() : null;
                            selectedMarkOidModel.setObject(markRef == null ? null : markRef.getOid());
                        }
                        target.add(getTable());
                    }
                });
            }
        };

        components.setOutputMarkupId(true);
        form.add(components);
    }

    private void initTable(@NotNull MidpointForm<?> form) {
        MappingProcessedObjectPanel table = buildTableComponent();
        form.add(table);
    }

    private @NotNull MappingProcessedObjectPanel buildTableComponent() {
        IModel<List<MarkType>> availableMarksModel = loadAvailableMarksModel(getPageBase(), getModelObject());
        availableMarksModel.getObject().removeIf(mark ->
                !mappingMarksOids.contains(mark.getOid())
        );

        MappingProcessedObjectPanel table = new MappingProcessedObjectPanel(
                ID_TABLE,
                availableMarksModel) {
            @Override
            protected String getDefaultMarkOidForSearch() {
                return selectedMarkOidModel.getObject();
            }

            @Override
            protected @NotNull IModel<SimulationResultType> getSimulationResultModel() {
                return SimulationMappingPanel.this.getModel();
            }

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                SimulationMappingPanel.this.navigateToSimulationResultObject(
                        simulationResultOid, markOid, object, target);
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    private MappingProcessedObjectPanel getTable() {
        return (MappingProcessedObjectPanel) get(createComponentPath(ID_FORM, ID_TABLE));
    }

    protected void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target) {
        PageParameters params = new PageParameters();
        params.set(PageSimulationResultObject.PAGE_PARAMETER_RESULT_OID, simulationResultOid);
        if (markOid != null) {
            params.set(PageSimulationResultObject.PAGE_PARAMETER_MARK_OID, markOid);
        }
        params.set(PageSimulationResultObject.PAGE_PARAMETER_CONTAINER_ID, object.getId());

        getPageBase().navigateToNext(PageSimulationResultObject.class, params);
    }
}
