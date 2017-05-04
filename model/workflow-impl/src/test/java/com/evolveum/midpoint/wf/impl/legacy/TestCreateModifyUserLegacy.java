/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.legacy;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCreateModifyUserLegacy extends AbstractWfTestLegacy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestCreateModifyUserLegacy.class);

    private static final File REQ_USER_ELISABETH_MODIFY_ADD_ASSIGNMENT_ROLE1 = new File(TEST_RESOURCE_DIR,
            "user-elisabeth-modify-add-assignment-role3.xml");

    public TestCreateModifyUserLegacy() throws JAXBException {
		super();
	}

    /**
     * Create user elisabeth (with sensitive role)
     */
	@Test(enabled = true)
    public void test010CreateElisabeth() throws Exception {
        TestUtil.displayTestTile(this, "test010CreateElisabeth");
       	executeTest("test010CreateElisabeth", USER_ELISABETH_OID, new TestDetails() {
            @Override int subtaskCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }
            @Override boolean removeAssignmentsBeforeTest() { return false; }

            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                addFocusDeltaToContext(context, (ObjectDelta) ObjectDelta.createAddDelta(PrismTestUtil.parseObject(USER_ELISABETH_FILE)));
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                //assertNoObject(UserType.class, USER_ELISABETH_OID, task, result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
                assertAssignedRole(USER_ELISABETH_OID, ROLE_R1_OID, task, result);
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                checkUserApproversForCreate(USER_ELISABETH_OID, Arrays.asList(R1BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }
        });
	}

    /**
     * Add another assignment to user elisabeth (with sensitive role)
     */
    @Test(enabled = true)
    public void test020ModifyElisabethAssignRole3() throws Exception {
        TestUtil.displayTestTile(this, "test020ModifyElisabethAssignRole3");
        executeTest("test020ModifyElisabethAssignRole3", USER_ELISABETH_OID, new TestDetails() {
            @Override int subtaskCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }
            @Override boolean removeAssignmentsBeforeTest() { return false; }

            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_ELISABETH_OID, result);
                addFocusModificationToContext(context, REQ_USER_ELISABETH_MODIFY_ADD_ASSIGNMENT_ROLE1);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                //assertNotAssignedRole(USER_ELISABETH_OID, ROLE_R3_OID, task, result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
                //assertAssignedRole(USER_ELISABETH_OID, ROLE_R3_OID, task, result);
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_ELISABETH_OID, Arrays.asList(R1BOSS_OID), result);
                checkUserApproversForCreate(USER_ELISABETH_OID, Arrays.asList(R1BOSS_OID), result);   // this one should remain from test010
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }
        });
    }

    @Test
    public void zzzMarkAsNotInitialized() {
        display("Setting class as not initialized");
        unsetSystemInitialized();
    }
}
