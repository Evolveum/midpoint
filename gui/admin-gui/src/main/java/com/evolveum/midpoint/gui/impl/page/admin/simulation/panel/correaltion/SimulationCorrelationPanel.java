/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObject;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.*;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.SimulationWebUtil.loadAvailableMarksModel;

public class SimulationCorrelationPanel extends BasePanel<SimulationResultType> {

    private static final String ID_FORM = "form";
    private static final String ID_DASHBOARD = "dashboard";
    private static final String ID_WIDGET = "widget";
    private static final String ID_TABLE = "table";

    IModel<CorrelationStatus> correlationStatusModel = Model.of();

    public SimulationCorrelationPanel(String id, IModel<SimulationResultType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        initDashboard(form);
        initTable(form);
    }

    private @NotNull Map<DashboardWidgetType, CorrelationStatus> loadWidgetsModel() {
        Map<DashboardWidgetType, CorrelationStatus> statusMap = new LinkedHashMap<>();

        int correlated = 0, uncertain = 0, notCorrelated = 0, total = 0;

        for (var processed : searchProcessedObjects(getPageBase(), getModelObject().getOid())) {
            ObjectDelta<Objectable> delta;
            try {
                delta = DeltaConvertor.createObjectDeltaNullable(processed.getDelta());
            } catch (SchemaException e) {
                throw new RuntimeException("Couldn't parse object delta: " + e.getMessage(), e);
            }

            var candidates = parseResourceObjectOwnerOptionsFromDelta(delta);

            if (candidates == null || candidates.isEmpty()) {notCorrelated++;} else if (candidates.size() == 1) {
                correlated++;
            } else {uncertain++;}

            total++;
        }

        statusMap.put(buildWidget("SimulationCorrelationPanel.correlated",
                "SimulationCorrelationPanel.correlated.help",
                "fa fa-link metric-icon success",
                correlated), CorrelationStatus.CORRELATED);

        statusMap.put(buildWidget("SimulationCorrelationPanel.uncertain",
                        "SimulationCorrelationPanel.uncertain.help",
                        "fa-solid fa-question metric-icon warning text-warning",
                        uncertain), CorrelationStatus.UNCERTAIN);

        statusMap.put(buildWidget("SimulationCorrelationPanel.notCorrelated",
                        "SimulationCorrelationPanel.notCorrelated.help",
                        "fa fa-link-slash metric-icon secondary",
                        notCorrelated), CorrelationStatus.NOT_CORRELATED);

        statusMap.put(buildWidget("SimulationCorrelationPanel.total",
                        "SimulationCorrelationPanel.total.help",
                        "fa fa-cube metric-icon info",
                        total), null);
        return statusMap;
    }

    private void initDashboard(@NotNull MidpointForm<?> form) {
        // TODO Auto-generated method stub
        Map<DashboardWidgetType, CorrelationStatus> statusMap = loadWidgetsModel();
        List<DashboardWidgetType> dashboardWidgetTypes = new ArrayList<>(statusMap.keySet());
        ListView<DashboardWidgetType> components = new ListView<>(ID_DASHBOARD, () -> new ArrayList<>(dashboardWidgetTypes)) {

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
                    protected void onMoreInfoPerformed(AjaxRequestTarget target) {
                        correlationStatusModel.setObject(statusMap.get(item.getModelObject()));
                        getTable().refreshTable(target);
                    }
                });
            }
        };

        components.setOutputMarkupId(true);
        form.add(components);
    }

    private void initTable(@NotNull MidpointForm<?> form) {
        IModel<List<MarkType>> availableMarksModel = loadAvailableMarksModel(getPageBase(), getModelObject());
        CorrelationProcessedObjectPanel table = new CorrelationProcessedObjectPanel(
                ID_TABLE,
                availableMarksModel) {
            @Override
            protected @Nullable CorrelationStatus filterByStatus() {
                return correlationStatusModel.getObject();
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
        form.add(table);
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
