/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObject;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.*;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.SimulationWebUtil.loadAvailableMarksModel;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.*;

public class SimulationCorrelationPanel extends BasePanel<SimulationResultType> {

    private static final String ID_FORM = "form";
    private static final String ID_DASHBOARD = "dashboard";
    private static final String ID_WIDGET = "widget";
    private static final String ID_TABLE = "table";

    private IModel<List<DashboardWidgetType>> metricsModel;
    IModel<String> selectedMarkOidModel = Model.of(MARK_SHADOW_CORRELATION_OWNER_FOUND.value());

    List<String> correlationMarksOids = List.of(
            MARK_SHADOW_CORRELATION_OWNER_FOUND.value(),
            MARK_SHADOW_CORRELATION_OWNER_NOT_FOUND.value(),
            MARK_SHADOW_CORRELATION_OWNER_NOT_CERTAIN.value());

    public SimulationCorrelationPanel(String id, IModel<SimulationResultType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        loadMetricModel();

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        initDashboard(form);
        initTable(form);
    }

    private void loadMetricModel() {
        metricsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<DashboardWidgetType> load() {
                List<SimulationMetricValuesType> metrics = getModelObject().getMetric();

                List<DashboardWidgetType> collect = metrics.stream().map(m -> {

                            SimulationMetricReferenceType ref = m.getRef();
                            ObjectReferenceType eventMarkRef = ref.getEventMarkRef();
                            if (eventMarkRef == null
                                    || eventMarkRef.getOid() == null
                                    || !correlationMarksOids.contains(eventMarkRef.getOid())) {
                                return null;
                            }

                            BigDecimal value = SimulationMetricValuesTypeUtil.getValue(m);
                            String storedData = MetricWidgetPanel.formatValue(value, LocalizationUtil.findLocale());

                            DashboardWidgetType dw = new DashboardWidgetType();
                            dw.beginData()
                                    .sourceType(DashboardWidgetSourceTypeType.METRIC)
                                    .metricRef(m.getRef())
                                    .storedData(storedData)
                                    .end();
                            return dw;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                int totalProcessed = collect.stream()
                        .map(DashboardWidgetType::getData)
                        .map(DashboardWidgetDataType::getStoredData)
                        .mapToInt(s -> {
                            try {
                                return Integer.parseInt(s);
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        })
                        .reduce(0, Integer::sum);

                DashboardWidgetType totalProcessedWidget = buildWidget(
                        createStringResource("SimulationCorrelationPanel.total").getString(),
                        "SimulationCorrelationPanel.total.help",
                        "fa fa-cube metric-icon info",
                        totalProcessed);
                collect.add(totalProcessedWidget);
                return collect;
            }
        };
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
                        return createStringResource("SimulationCorrelationPanel.viewProcessedObjects");
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
        CorrelationProcessedObjectPanel table = buildTableComponent();
        form.add(table);
    }

    private @NotNull CorrelationProcessedObjectPanel buildTableComponent() {
        IModel<List<MarkType>> availableMarksModel = loadAvailableMarksModel(getPageBase(), getModelObject());
        availableMarksModel.getObject().removeIf(mark ->
                !correlationMarksOids.contains(mark.getOid())
        );

        CorrelationProcessedObjectPanel table = new CorrelationProcessedObjectPanel(
                ID_TABLE,
                availableMarksModel) {
            @Override
            protected String getMarkOidForSearch() {
                return selectedMarkOidModel.getObject();
            }

            @Override
            protected @NotNull IModel<SimulationResultType> getSimulationResultModel() {
                return SimulationCorrelationPanel.this.getModel();
            }

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                SimulationCorrelationPanel.this.navigateToSimulationResultObject(
                        simulationResultOid, markOid, object, target);
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    private CorrelationProcessedObjectPanel getTable() {
        return (CorrelationProcessedObjectPanel) get(ID_FORM).get(ID_TABLE);
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
