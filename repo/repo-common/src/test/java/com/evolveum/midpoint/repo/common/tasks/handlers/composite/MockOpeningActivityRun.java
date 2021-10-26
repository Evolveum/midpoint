/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;

import org.jetbrains.annotations.NotNull;

final class MockOpeningActivityRun extends MockComponentActivityRun {

    MockOpeningActivityRun(@NotNull ActivityRunInstantiationContext<CompositeMockWorkDefinition, CompositeMockActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    @Override
    String getMockSubActivity() {
        return "opening";
    }
}
