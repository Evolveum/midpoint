/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.util;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.buildWidget;

public class SimulationWebUtil {

    /**
     * Loads all MarkType objects that are referenced from the given simulation result.
     */
    public static @NotNull IModel<List<MarkType>> loadAvailableMarksModel(
            @NotNull PageBase pageBase,
            @NotNull SimulationResultType simulationResult) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<MarkType> load() {
                String[] markOids = simulationResult.getMetric().stream()
                        .map(m -> m.getRef() != null ? m.getRef().getEventMarkRef() : null)
                        .filter(Objects::nonNull)
                        .map(AbstractReferencable::getOid)
                        .filter(Utils::isPrismObjectOidValid)
                        .distinct().toArray(String[]::new);

                ObjectQuery query = pageBase.getPrismContext()
                        .queryFor(MarkType.class)
                        .id(markOids).build();

                Task pageTask = pageBase.getPageTask();
                List<PrismObject<MarkType>> marks = WebModelServiceUtils.searchObjects(
                        MarkType.class, query, pageTask.getResult(), pageBase);

                return marks.stream()
                        .map(o -> o.asObjectable())
                        .collect(Collectors.toList());

            }
        };
    }

    public static @Nullable DashboardWidgetType processedObjectsCountWidget(
            @NotNull PageBase pageBase,
            @NotNull SimulationResultType simulationResult,
            Trace logger) {
        final Task countTask = pageBase.createSimpleTask("Count processed objects.");
        final OperationResult result = countTask.getResult();
        try {
            final int processedObjectsCount = pageBase.getModelService().countContainers(
                    SimulationResultProcessedObjectType.class, pageBase.getPrismContext()
                            .queryFor(SimulationResultProcessedObjectType.class)
                            .ownedBy(SimulationResultType.class).id(simulationResult.getOid())
                            .build(),
                    null, countTask, result);
            return buildWidget(pageBase.createStringResource("SimulationPanel.total").getString(),
                    "SimulationPanel.total.help", "fa fa-cube metric-icon info", processedObjectsCount);
        } catch (CommonException e) {
            result.recordFatalError("Can't count processed objects.");
            LoggingUtils.logUnexpectedException(logger, "Unable to count processed objects", e);
        }
        return null;
    }
}
