/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.simulation;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.TaskExecutionMode;

/**
 * Runs the basic simulations in {@link TaskExecutionMode#SIMULATED_DEVELOPMENT}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDevelopmentSimulations extends AbstractBasicSimulationExecutionTest {

    @Override
    TaskExecutionMode getExecutionMode(boolean shadowSimulation) {
        return shadowSimulation ? TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT : TaskExecutionMode.SIMULATED_DEVELOPMENT;
    }
}
