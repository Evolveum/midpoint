/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Basic scenarios of simulations.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSimpleSimulations extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/simulation/simple");

    private final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class);

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test100CreateUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name("test100");

        when("user is created in simulation");
        SimulationResult simResult =
                traced(() -> executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        task, result));

        then("everything is OK");
        assertSuccess(result);

        and("no new object should be created");
        objectsCounter.assertNoNewObjects(result);

        and("there are simulation deltas");
        simResult.assertNoExecutedDeltas();
        List<ObjectDelta<?>> simulatedDeltas = simResult.getSimulatedDeltas();
        displayCollection("simulated deltas", simulatedDeltas);

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
    }
}
