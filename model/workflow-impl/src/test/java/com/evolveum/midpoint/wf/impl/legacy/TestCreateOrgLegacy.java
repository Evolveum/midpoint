/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCreateOrgLegacy extends AbstractWfTestLegacy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestCreateOrgLegacy.class);

    private static final File TEST1_FILE = new File(TEST_RESOURCE_DIR, "org-test1.xml");
    private static final String ORG_TEST1_OID = "00000000-1345-3213-4321-432435432034";

    public TestCreateOrgLegacy() throws JAXBException {
		super();
	}

    /**
     * Create org test1 - rejected
     */
    @Test(enabled = false)              // this is not enabled by default
    public void test010CreateTest1Rejected() throws Exception {
        TestUtil.displayTestTitle(this, "test010CreateTest1Rejected");
        executeTest("test010CreateTest1Rejected", ORG_TEST1_OID, new TestDetails() {
            @Override
            int subcasesCount() {
                return 1;
            }

            @Override
            boolean immediate() {
                return false;
            }

            @Override
            boolean checkObjectOnSubtasks() {
                return true;
            }

            @Override
            boolean removeAssignmentsBeforeTest() {
                return false;
            }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<OrgType> context = createLensContext(OrgType.class);
                addFocusDeltaToContext(context, (ObjectDelta) DeltaFactory.Object.createAddDelta(PrismTestUtil.parseObject(TEST1_FILE)));
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertTrue("Primary focus delta is not empty", taskModelContext.getFocusContext().getPrimaryDelta().isEmpty());
                assertNoObject(OrgType.class, ORG_TEST1_OID, opTask, result);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                assertNoObject(OrgType.class, ORG_TEST1_OID, opTask, result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
		            WfContextType wfContext) throws Exception {
                return false;
            }
        });
    }

    /**
     * Create org test1 - this time approved
     */
	@Test(enabled = false)                  // this is not enabled by default
    public void test020CreateTest1Approved() throws Exception {
        TestUtil.displayTestTitle(this, "test020CreateTest1Approved");
       	executeTest("test020CreateTest1Approved", ORG_TEST1_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }
            @Override boolean removeAssignmentsBeforeTest() { return false; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<OrgType> context = createLensContext(OrgType.class);
                addFocusDeltaToContext(context, (ObjectDelta) DeltaFactory.Object.createAddDelta(PrismTestUtil.parseObject(TEST1_FILE)));
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertTrue("Primary focus delta is not empty", taskModelContext.getFocusContext().getPrimaryDelta().isEmpty());
                assertNoObject(OrgType.class, ORG_TEST1_OID, opTask, result);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                checkApproversForCreate(OrgType.class, ORG_TEST1_OID, Arrays.asList(USER_ADMINISTRATOR_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
		            WfContextType wfContext) throws Exception {
                return true;
            }
        });
	}

    @Test
    public void zzzMarkAsNotInitialized() {
        display("Setting class as not initialized");
        unsetSystemInitialized();
    }
}
