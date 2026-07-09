/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.function.Consumer;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Multithreaded (single-node, worker threads) flavor of {@link TestFocusPolicyHrScenario}: each
 * reconciliation child runs with worker threads. The shared delete counter may momentarily overshoot the
 * threshold, which the base assertion's bound already tolerates.
 */
public class TestFocusPolicyHrScenarioMultithreaded extends TestFocusPolicyHrScenario {

    private static final int THREADS = 3;

    @Override
    protected Consumer<PrismObject<TaskType>> topology() {
        return taskObj -> {
            for (ActivityDefinitionType child : taskObj.asObjectable().getActivity().getComposition().getActivity()) {
                ActivityDefinitionUtil.findOrCreateDistribution(child).setWorkerThreads(THREADS);
            }
        };
    }
}
