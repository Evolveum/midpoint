/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.model.api.DataModelVisualizer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

@UnusedTestElement("not in suite - failing")
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelVisualization extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/modelVisualization");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test100VisualizeOneResource() throws Exception {
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();

        // WHEN
        String output = modelDiagnosticService.exportDataModel(Collections.singleton(RESOURCE_DUMMY_OID),
                DataModelVisualizer.Target.DOT, task, result);

        // THEN
        displayValue("Visualization output", output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test110VisualizeTwoResources() throws Exception {
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();

        // WHEN
        String output = modelDiagnosticService.exportDataModel(Arrays.asList(RESOURCE_DUMMY_OID, RESOURCE_DUMMY_BLACK_OID),
                DataModelVisualizer.Target.DOT, task, result);

        // THEN
        displayValue("Visualization output", output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }
}
