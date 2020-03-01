/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.assignments;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Shouldn't be used, as global policy rules for assignments are not implemented yet.
 *
 * @author mederly
 */
public class TestAssignmentApprovalGlobal extends AbstractTestAssignmentApproval {

    private static final File SYSTEM_CONFIGURATION_GLOBAL_FILE = new File(TEST_RESOURCE_DIR, "system-configuration-global.xml");

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_GLOBAL_FILE;
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected String getRoleOid(int number) {
        switch (number) {
            case 1: return roleRole1Oid;
            case 2: return roleRole2Oid;
            case 3: return roleRole3Oid;
            case 4: return roleRole4Oid;
            case 10: return roleRole10Oid;
            default: throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected String getRoleName(int number) {
        switch (number) {
            case 1: return "Role1";
            case 2: return "Role2";
            case 3: return "Role3";
            case 4: return "Role4";
            case 10: return "Role10";
            default: throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }

    /**
     * MID-3836
     */
    public void test300ApprovalAndEnforce() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        task.setOwner(userAdministrator);
        OperationResult result = getTestResult();

        try {
            assignRole(userJackOid, roleRole15Oid, task, result);
        } catch (PolicyViolationException e) {
            // ok
            System.out.println("Got expected exception: " + e);
        }
        List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, getOpenItemsQuery(), null, task, result);
        display("current work items", currentWorkItems);
        assertEquals("Wrong # of current work items", 0, currentWorkItems.size());
    }

}
