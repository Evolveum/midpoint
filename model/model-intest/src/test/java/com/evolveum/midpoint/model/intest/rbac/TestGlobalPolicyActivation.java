/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.rbac;

import java.io.File;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.test.util.OperationResultAssert;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestGlobalPolicyActivation extends AbstractInitializedModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestGlobalPolicyActivation.class);

    private static final File TEST_DIR = new File("./src/test/resources/rbac");

    private static final TestObject<SystemConfigurationType> GLOBAL_POLICY_RULE =
            TestObject.file(TEST_DIR, "global-policy-rule-activation.xml", SystemObjectsType.SYSTEM_CONFIGURATION.value());

    /**
     * Test for MID-10743
     */
    @Test
    public void testPolicyActivationOnUserDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        TestObject.FileBasedTestObjectSource file = (TestObject.FileBasedTestObjectSource) GLOBAL_POLICY_RULE.source;

        given();

        // create empty role
        RoleType sampleRole = new RoleType();
        sampleRole.setName(PrismTestUtil.createPolyStringType("remove_policy_test"));
        final String sampleRoleOid = addObject(sampleRole, task, result);

        UserType user = new UserType();
        user.setName(PrismTestUtil.createPolyStringType("first"));
        user.beginAssignment()
                .targetRef(sampleRoleOid, RoleType.COMPLEX_TYPE)
                .end();

        String userOid = addObject(user, task, result);

        when();

        modifyObjectReplaceContainer(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SystemConfigurationType.F_GLOBAL_POLICY_RULE, task, result);
        transplantGlobalPolicyRulesAdd(file.getFile(), task, result);

        deleteObject(UserType.class, userOid, task, result);

        then();

        new OperationResultAssert(result)
                .isSuccess();
    }
}
