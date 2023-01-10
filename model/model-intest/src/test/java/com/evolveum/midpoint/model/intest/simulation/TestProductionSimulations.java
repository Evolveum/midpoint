/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import com.evolveum.midpoint.schema.TaskExecutionMode;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

/**
 * Runs the basic simulations in {@link TaskExecutionMode#SIMULATED_PRODUCTION}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProductionSimulations extends AbstractBasicSimulationExecutionTest {

    @Override
    TaskExecutionMode getExecutionMode() {
        return TaskExecutionMode.SIMULATED_PRODUCTION;
    }
}
