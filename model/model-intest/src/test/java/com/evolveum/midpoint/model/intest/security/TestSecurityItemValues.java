/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Tests the security functions related to sub-object structures, like:
 *
 * . operations on individual items (e.g., case work items),
 * . visibility of individual items.
 *
 * For example, there may be a case with three work items with different operations and visibility because of different
 * relations to the current principal.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityItemValues extends AbstractSecurityTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test100CaseWorkItemsAssigneeSelfRead() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ.oid);

        when();
        login(USER_JACK_USERNAME);

        then("can see all cases (because of 'all cases' object selector)");
        assertReadCases(CASE1.oid, CASE2.oid, CASE3.oid, CASE4.oid);

        and("but not all their work items");
        assertGetWorkItems(CASE1.oid).assertSize(0); // none assigned to jack
        assertGetWorkItems(CASE2.oid).assertSize(0); // none assigned to jack
        assertGetWorkItems(CASE3.oid)
                .single()
                .assertAssignees(USER_JACK_OID);
        assertGetWorkItems(CASE4.oid)
                .single()
                .assertAssignees(USER_JACK_OID);

        and("only allowed items are there");
        assertCaseAfter(CASE1.oid)
                .assertItems(CaseType.F_NAME);
        assertCaseAfter(CASE2.oid)
                .assertItems(CaseType.F_NAME);
        assertCaseAfter(CASE3.oid)
                .assertItems(CaseType.F_NAME, CaseType.F_WORK_ITEM);
        assertCaseAfter(CASE4.oid)
                .assertItems(CaseType.F_NAME, CaseType.F_WORK_ITEM);

        // TODO continue
    }
}
