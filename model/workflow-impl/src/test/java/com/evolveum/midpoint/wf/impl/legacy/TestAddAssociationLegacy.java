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
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAddAssociationLegacy extends AbstractWfTestLegacy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestAddAssociationLegacy.class);

    private static final File REQ_SHADOW_MODIFY_ADD_ENTITLEMENT_TESTERS = new File(TEST_RESOURCE_DIR,
            "shadow-modify-add-entitlement-testers.xml");
    private static final File REQ_SHADOW_MODIFY_ADD_ENTITLEMENT_GUESTS = new File(TEST_RESOURCE_DIR,
            "shadow-modify-add-entitlement-guests.xml");

    public TestAddAssociationLegacy() throws JAXBException {
		super();
	}

    private String jackAccountShadowOid;
    private String elisabethAccountShadowOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        modifyUserAddAccount(USER_JACK_OID, ACCOUNT_SHADOW_JACK_DUMMY_FILE, initTask, initResult);

        importObjectFromFile(USER_ELISABETH_FILE, initResult);
        modifyUserAddAccount(USER_ELISABETH_OID, ACCOUNT_SHADOW_ELISABETH_DUMMY_FILE, initTask, initResult);
    }

    /**
     * Add entitlement to user jack
     */
	@Test
    public void test010AddJackToTesters() throws Exception {
        TestUtil.displayTestTitle(this, "test010AddJackToTesters");
        executeTest("test010AddJackToTesters", USER_JACK_OID, new TestDetails() {
            @Override
            int subtaskCount() {
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
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);

                UserType jack = context.getFocusContext().getObjectCurrent().asObjectable();
                AssertJUnit.assertEquals("Jack has wrong number of accounts", 1, jack.getLinkRef().size());
                jackAccountShadowOid = jack.getLinkRef().get(0).getOid();

                LensProjectionContext accountContext = fillContextWithAccount(context, jackAccountShadowOid, task, result);

                ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_SHADOW_MODIFY_ADD_ENTITLEMENT_TESTERS, ObjectModificationType.COMPLEX_TYPE);
                ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, ShadowType.class, prismContext);
                shadowDelta.setOid(jackAccountShadowOid);
                accountContext.setPrimaryDelta(shadowDelta);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
                IntegrationTestTools.display("model context from the root task", taskModelContext);
                assertEquals("Wrong # of projection contexts in root task", 1, taskModelContext.getProjectionContexts().size());
                assertTrue("There are modifications in primary focus delta", ObjectDelta.isNullOrEmpty(taskModelContext.getFocusContext().getPrimaryDelta()));
                assertTrue("There are modifications left in primary projection delta",
                        ObjectDelta.isNullOrEmpty(
                                ((LensProjectionContext) (taskModelContext.getProjectionContexts().iterator().next()))
                                        .getPrimaryDelta()));
                ShadowType account = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
                IntegrationTestTools.display("jack dummy account after first clockwork run", account);
                assertEquals("Unexpected associations present", 0, account.getAssociation().size());
            }

            @Override
            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
                ShadowType account = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
                IntegrationTestTools.display("jack dummy account", account);
                assertHasAssociation(account, new QName("group"), GROUP_TESTERS_OID);

                checkWorkItemAuditRecords(createResultMap(GROUP_TESTERS_OID, WorkflowResult.APPROVED));
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return true;
            }
        });
	}

    /**
     * Add entitlement to user elisabeth (rejected)
     */
    @Test
    public void test020AddElisabethToTestersRejected() throws Exception {
        TestUtil.displayTestTitle(this, "test020AddElisabethToTestersRejected");
        executeTest("test020AddElisabethToTestersRejected", USER_ELISABETH_OID, new TestDetails() {
            @Override
            int subtaskCount() {
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
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_ELISABETH_OID, result);

                UserType elisabeth = context.getFocusContext().getObjectCurrent().asObjectable();
                AssertJUnit.assertEquals("Elisabeth has wrong number of accounts", 1, elisabeth.getLinkRef().size());
                elisabethAccountShadowOid = elisabeth.getLinkRef().get(0).getOid();

                LensProjectionContext accountContext = fillContextWithAccount(context, elisabethAccountShadowOid, task, result);

                ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_SHADOW_MODIFY_ADD_ENTITLEMENT_TESTERS, ObjectModificationType.COMPLEX_TYPE);
                ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, ShadowType.class, prismContext);
                shadowDelta.setOid(elisabethAccountShadowOid);
                accountContext.setPrimaryDelta(shadowDelta);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
                IntegrationTestTools.display("model context from the root task", taskModelContext);
                assertEquals("Wrong # of projection contexts in root task", 1, taskModelContext.getProjectionContexts().size());
                assertTrue("There are modifications in primary focus delta", ObjectDelta.isNullOrEmpty(taskModelContext.getFocusContext().getPrimaryDelta()));
                assertTrue("There are modifications left in primary projection delta",
                        ObjectDelta.isNullOrEmpty(
                                ((LensProjectionContext) (taskModelContext.getProjectionContexts().iterator().next()))
                                        .getPrimaryDelta()));
                ShadowType account = getObject(ShadowType.class, elisabethAccountShadowOid).asObjectable();
                IntegrationTestTools.display("elisabeth dummy account after first clockwork run", account);
                assertEquals("Unexpected associations present", 0, account.getAssociation().size());
            }

            @Override
            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
                ShadowType account = getObject(ShadowType.class, elisabethAccountShadowOid).asObjectable();
                IntegrationTestTools.display("elisabeth dummy account", account);
                assertEquals("Unexpected associations present", 0, account.getAssociation().size());

                checkWorkItemAuditRecords(createResultMap(GROUP_TESTERS_OID, WorkflowResult.REJECTED));
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return false;
            }
        });
    }

    /**
     * Add entitlement for 'guests' to user jack - should be created without starting wf process
     */
    @Test
    public void test100AddJackToGuests() throws Exception {
        final String TEST_NAME = "test100AddJackToGuests";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task modelTask = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = new OperationResult(TEST_NAME);
        modelTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);

        UserType jack = context.getFocusContext().getObjectCurrent().asObjectable();
        AssertJUnit.assertEquals("Jack has wrong number of accounts", 1, jack.getLinkRef().size());
        jackAccountShadowOid = jack.getLinkRef().get(0).getOid();

        ShadowType accountBefore = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
        assertEquals("Wrong # of jack's account associations", 1, accountBefore.getAssociation().size());
        assertHasAssociation(accountBefore, new QName("group"), GROUP_TESTERS_OID);

        LensProjectionContext accountContext = fillContextWithAccount(context, jackAccountShadowOid, modelTask, result);

        ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_SHADOW_MODIFY_ADD_ENTITLEMENT_GUESTS, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, ShadowType.class, prismContext);
        shadowDelta.setOid(jackAccountShadowOid);
        accountContext.setPrimaryDelta(shadowDelta);

        HookOperationMode mode = clockwork.run(context, modelTask, result);

        assertEquals("Unexpected state of the context - workflow was started even if it should not", ModelState.FINAL, context.getState());
        assertEquals("Wrong mode after clockwork.run in " + context.getState(), HookOperationMode.FOREGROUND, mode);

        ShadowType accountAfter = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
        assertEquals("Wrong # of jack's account associations", 2, accountAfter.getAssociation().size());
        assertHasAssociation(accountAfter, new QName("group"), GROUP_TESTERS_OID);
        assertHasAssociation(accountAfter, new QName("group"), GROUP_GUESTS_OID);
    }


    public void assertHasAssociation(ShadowType shadow, QName associationName, String entitlementOid) {
        for (ShadowAssociationType association : shadow.getAssociation()) {
            if (QNameUtil.match(association.getName(), associationName) &&
                    entitlementOid.equals(association.getShadowRef().getOid())) {
                return;
            }
        }
        AssertJUnit.fail("No association of type " + associationName + " of " + entitlementOid + " in " + ObjectTypeUtil.toShortString(shadow));
    }

    @Test
    public void zzzMarkAsNotInitialized() {
        display("Setting class as not initialized");
        unsetSystemInitialized();
    }
}
