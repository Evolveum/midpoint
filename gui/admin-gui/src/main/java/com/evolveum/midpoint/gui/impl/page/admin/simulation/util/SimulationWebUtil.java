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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
}
