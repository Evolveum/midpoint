/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.misc;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ShadowIntegrityCheckerTest extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");

    private static final File TASK_SHADOW_INTEGRITY_CHECK_FILE = new File(TEST_DIR, "task-shadow-integrity-check.xml");
    private static final String TASK_SHADOW_INTEGRITY_CHECK_OID = "b5a8b51d-d834-4803-a7d0-c81bcc58113e";

    private static final File SHADOW_1_FILE = new File(TEST_DIR, "shadow-1.xml");
    private static final File SHADOW_2_FILE = new File(TEST_DIR, "shadow-2.xml");
    private static final File SHADOW_2_DUPLICATE_FILE = new File(TEST_DIR, "shadow-2-duplicate.xml");

    private static final File RESOURCE_DUMMY_FOR_CHECKER_FILE = new File(TEST_DIR, "resource-dummy-for-checker.xml");
    private static final String RESOURCE_DUMMY_FOR_CHECKER_OID = "8fdb9db5-429a-4bcc-94f4-043dbd7f2eb2";
    private static final String DUMMY_FOR_CHECKER = "for-checker";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(DUMMY_FOR_CHECKER, RESOURCE_DUMMY_FOR_CHECKER_FILE, RESOURCE_DUMMY_FOR_CHECKER_OID, initTask, initResult);

        List<PrismObject<ShadowType>> shadows = repositoryService
                .searchObjects(ShadowType.class, null, null, initResult);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), initResult);
        }
        repoAddObjectFromFile(SHADOW_1_FILE, initResult);
        repoAddObjectFromFile(SHADOW_2_FILE, initResult);
        repoAddObjectFromFile(SHADOW_2_DUPLICATE_FILE, initResult);
    }

    @Test
    public void test100FixDuplicatesWithDifferentObjectClasses() throws Exception {
        final String TEST_NAME = "test100FixDuplicatesWithDifferentObjectClasses";

        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN
        SearchResultList<PrismObject<ShadowType>> shadowsBefore = repositoryService
                .searchObjects(ShadowType.class, null, null, result);
        display("shadows before", shadowsBefore);

        assertEquals("Wrong # of shadows before", 3, shadowsBefore.size());

        repoAddObjectFromFile(TASK_SHADOW_INTEGRITY_CHECK_FILE, result);

        // WHEN
        when();
        waitForTaskCloseOrSuspend(TASK_SHADOW_INTEGRITY_CHECK_OID);

        // THEN
        then();
        PrismObject<TaskType> taskAfter = getTask(TASK_SHADOW_INTEGRITY_CHECK_OID);
        display("task after", taskAfter);

        SearchResultList<PrismObject<ShadowType>> shadowsAfter = repositoryService
                .searchObjects(ShadowType.class, null, null, result);
        display("shadows after", shadowsAfter);

        assertEquals("Wrong # of shadows after", 2, shadowsAfter.size());
        PrismObject<ShadowType> intent1 = shadowsAfter.stream()
                .filter(o -> "intent1".equals(o.asObjectable().getIntent())).findFirst().orElse(null);
        assertNotNull("intent1 shadow was removed", intent1);
    }


}
