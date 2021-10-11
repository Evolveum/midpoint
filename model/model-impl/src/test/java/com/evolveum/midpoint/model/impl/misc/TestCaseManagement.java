/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.misc;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCaseManagement extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");
    protected static final File USER1_FILE = new File(TEST_DIR, "user1.xml");
    protected static final File USER2_FILE = new File(TEST_DIR, "user2.xml");
    protected static final File CASE1_FILE = new File(TEST_DIR, "case1.xml");
    protected static final File CASE2_FILE = new File(TEST_DIR, "case2.xml");
    protected static final File CASE3_FILE = new File(TEST_DIR, "case3.xml");

    @Autowired protected ModelController controller;
    @Autowired protected Projector projector;
    @Autowired protected Clockwork clockwork;
    @Autowired protected TaskManager taskManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        repoAddObjectFromFile(USER1_FILE, initResult);
        repoAddObjectFromFile(USER2_FILE, initResult);
        repoAddObjectFromFile(CASE1_FILE, initResult);
        repoAddObjectFromFile(CASE2_FILE, initResult);
        repoAddObjectFromFile(CASE3_FILE, initResult);
    }

    @Test
    public void test100SearchCases() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        login(userAdministrator);

        SearchResultList<PrismObject<CaseType>> cases = controller.searchObjects(CaseType.class, null, null, task, result);
        assertEquals(3, cases.size());
        SearchResultList<CaseWorkItemType> workItems = controller.searchContainers(CaseWorkItemType.class, null, null, task, result);
        assertEquals(4, workItems.size());
    }
}
