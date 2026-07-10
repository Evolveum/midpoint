/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.function.Consumer;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Simple (single-node, single-thread) flavor of {@link TestFocusPolicyHrScenario}.
 */
public class TestFocusPolicyHrScenarioSimple extends TestFocusPolicyHrScenario {

    @Override
    protected Consumer<PrismObject<TaskType>> topology() {
        return t -> { }; // no distribution
    }
}
