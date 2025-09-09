/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import java.io.File;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPolicyActions extends AbstractInitializedModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestPolicyActions.class);
    private static final File TEST_DIR = new File("./src/test/resources/rbac");
    private static final TestObject<SystemConfigurationType> GLOBAL_POLICY_RULE_RUN_PRIVILEGED_ACTION =
            TestObject.file(TEST_DIR, "global-policy-rule-run-privileged-action.xml", SystemObjectsType.SYSTEM_CONFIGURATION.value());

    public static final File USER_REBEKA_FILE = new File(COMMON_DIR, "user-rebeka-enduser.xml");
    public static final String USER_REBEKA_OID = "6e5c58e1-b9a0-4b2b-b8cf-476fa4ae0242";
    public static final String USER_REBEKA_USERNAME = "rebeka";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(USER_REBEKA_FILE, initResult);
        repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);
    }

    @Test
    public void test100RecomputePolicyActionWithRunPrivilegedOption() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject.FileBasedTestObjectSource file =
                (TestObject.FileBasedTestObjectSource) GLOBAL_POLICY_RULE_RUN_PRIVILEGED_ACTION.source;
        transplantGlobalPolicyRulesAdd(file.getFile(), task, result);

        login(USER_REBEKA_USERNAME);

        PrismObject<UserType> userBefore = getUser(USER_REBEKA_OID);
        display("User before", userBefore);

        when();
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_REBEKA_OID, PASSWORD_VALUE_PATH,
                new ProtectedStringType().clearValue("newPasswordValue1!"));
        executeChanges(userDelta, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

}
