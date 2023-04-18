/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * TODO: Unfinished. Not included in standard tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestEvents extends AbstractWfTestPolicy {

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    protected static final File TEST_EVENTS_RESOURCE_DIR = new File("src/test/resources/events");
    protected static final File ROLE_NO_APPROVERS_FILE = new File(TEST_EVENTS_RESOURCE_DIR, "role-no-approvers.xml");

    protected String roleNoApproversOid;
    private WorkItemId workItemId;
    private String approvalCaseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        roleNoApproversOid = repoAddObjectFromFile(ROLE_NO_APPROVERS_FILE, initResult).getOid();
    }

    @Test
    public void test100CreateTask() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assignRole(USER_JACK.oid, roleNoApproversOid, task, result);                // should start approval process
        assertNotAssignedRole(USER_JACK.oid, roleNoApproversOid, result);

        CaseWorkItemType workItem = getWorkItem(task, result);
        workItemId = WorkItemId.of(workItem);

        display("work item", workItem);
        display("Case", CaseTypeUtil.getCase(workItem));

        // TODO check events
    }

}
