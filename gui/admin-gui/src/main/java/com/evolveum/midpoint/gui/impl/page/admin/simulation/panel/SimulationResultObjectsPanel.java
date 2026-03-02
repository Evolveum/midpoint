/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.ProcessedObjectsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.SimulationWebUtil.loadAvailableMarksModel;

public abstract class SimulationResultObjectsPanel extends BasePanel<SimulationResultType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    private IModel<List<MarkType>> availableMarksModel;

    public SimulationResultObjectsPanel(String id, IModel<SimulationResultType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        availableMarksModel = loadAvailableMarksModel(getPageBase(), getModelObject());
    }

    private void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        initTablePanel(form);
    }

    private void initTablePanel(@NotNull MidpointForm<?> form) {
        IModel<List<MarkType>> nonEmptyMarksModel = createNonEmptyMarksModel();
        ProcessedObjectsPanel table = new ProcessedObjectsPanel(ID_TABLE, nonEmptyMarksModel) {

            @Override
            protected void navigateToSimulationResultObject(
                    @NotNull String simulationResultOid,
                    @Nullable String markOid,
                    @NotNull SimulationResultProcessedObjectType object,
                    @NotNull AjaxRequestTarget target) {
                SimulationResultObjectsPanel.this.navigateToSimulationResultObject(simulationResultOid, markOid, object, target);
            }

            @Override
            protected ObjectProcessingStateType getPredefinedProcessingState() {
                return getStateQueryParameter();
            }

            @Override
            protected @NotNull SearchContext createAdditionalSearchContext() {
                SearchContext ctx = super.createAdditionalSearchContext();
                ctx.setObjectProcessingState(getStateQueryParameter());

                return ctx;
            }

            @Override
            protected @NotNull String getSimulationResultOid() {
                String oid = getResultOid();
                if (!Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }

            @Override
            protected String getPredefinedMarkOid() {
                String oid = SimulationResultObjectsPanel.this.getPredefinedMarkOid();
                if (oid != null && !Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }
        };
        form.add(table);
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull IModel<List<MarkType>> createNonEmptyMarksModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<MarkType> load() {
                List<MarkType> all = availableMarksModel.getObject();

                Set<String> nonEmptyMarkOids = getModelObject().getMetric().stream()
                        .filter(m -> m.getRef() != null && m.getRef().getEventMarkRef() != null)
                        .filter(m -> !Objects.equals(BigDecimal.ZERO, SimulationMetricValuesTypeUtil.getValue(m)))
                        .map(m -> m.getRef().getEventMarkRef().getOid())
                        .collect(Collectors.toUnmodifiableSet());

                // filter only marks that occur in simulation result (their respective metric count > 0)
                return all.stream()
                        .filter(m -> nonEmptyMarkOids.contains(m.getOid()))
                        .toList();
            }
        };
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

    private String getResultOid() {
        return getModelObject().getOid();
    }

    protected @Nullable String getPredefinedMarkOid() {
        PageParameters pageParameters = getPageBase().getPageParameters();
        StringValue parameterMarkOid = pageParameters.get(PageSimulationResultObject.PAGE_PARAMETER_MARK_OID);
        return parameterMarkOid != null && !parameterMarkOid.isEmpty() ? parameterMarkOid.toString() : null;
    }

    protected abstract ObjectProcessingStateType getStateQueryParameter();
}
