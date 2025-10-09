/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.simulation;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPreviewSequences extends AbstractConfiguredModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestPreviewChangesCoD.class);

    private static final File TEST_DIR = new File("src/test/resources/simulation/sequence");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<SequenceType> SEQUENCE_EXAMPLE = TestObject.file(TEST_DIR, "sequence.xml", "5e5731aa-476c-477b-b02e-372252004813");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_SIMULATION = TestObject.file(TEST_DIR, "object-template-simulation.xml", "10a8c24d-7031-4342-b20c-805e174b93a5");

    private static final TestObject<UserType> USER_CHAD = TestObject.file(TEST_DIR, "user-chad.xml");

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(OBJECT_TEMPLATE_SIMULATION, initTask, initResult);
        addObject(SEQUENCE_EXAMPLE, initTask, initResult);

        TestObject.getAll(USER_CHAD);
    }

    @Test
    public void test100Simple() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        SequenceType expected = getObject(SequenceType.class, SEQUENCE_EXAMPLE.oid).asObjectable();

        when("preview for add user chad, one sequence, counter shouldn't change after preview");

        PrismObject<UserType> orgChild = USER_CHAD.get().clone();
        ObjectDelta<UserType> delta = orgChild.createAddDelta();

        ModelContext<UserType> context = modelInteractionService.previewChanges(List.of(delta), null, task, result);

        then();

        SequenceType real = getObject(SequenceType.class, SEQUENCE_EXAMPLE.oid).asObjectable();

        AssertJUnit.assertNotNull(context);
        AssertJUnit.assertEquals("Sequence changed", expected, real);
    }
}
