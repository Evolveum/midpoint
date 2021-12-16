/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.test.TestResource;

import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Here we test the interface between midPoint and COmanage Match, an ID Match implementation.
 *
 * There are two source systems: `AIS` (Academic Information System) and `HR` (Human Resources).
 * They provide accounts using import, live sync, and reconciliation activities.
 *
 * REQUIREMENTS:
 *
 * The COmanage Match runs as an external system. Therefore, this tests runs manually.
 * (Later we may create a variant using automatically-run dummy ID Match implementation instead.)
 */
public class TestIdMatch extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch");

    private static final CsvResource RESOURCE_AIS = new CsvResource(TEST_DIR, "resource-ais.xml",
            "89d4fce0-f378-453a-a4f7-438efff10cfe", "resource-ais.csv",
            "identifier,givenName,familyName,dateOfBirth,emailAddress,referenceId");

    private static final TestTask TASK_IMPORT_AIS = new TestTask(TEST_DIR, "task-import-ais.xml",
            "95ebbf1e-9c71-4870-a1fb-dc47ce6856c9", 30000);

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_AIS.initialize(initTask, initResult);
        modelService.testResource(RESOURCE_AIS.oid, initTask);

        TASK_IMPORT_AIS.initialize(this, initTask, initResult); // importing in closed state
    }

    /**
     * First import: No ambiguities.
     *
     * Here we import two persons, with no ambiguities. Both should correlated to distinct new users.
     */
    @Test
    public void test100ImportNoAmbiguities() throws CommonException, IOException {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        RESOURCE_AIS.appendLine("1000000,Jan,Smith,2004-02-04,jan@evolveum.com,");
        RESOURCE_AIS.appendLine("1000001,Mary,Smith,2006-04-06,mary@evolveum.com,");

        when();
        TASK_IMPORT_AIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_AIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(2, 0, 0);
        // @formatter:on
    }
}
