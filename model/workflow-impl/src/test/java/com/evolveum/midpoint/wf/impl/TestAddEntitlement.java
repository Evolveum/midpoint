/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.python.antlr.base.mod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.Arrays;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAddEntitlement extends AbstractWfTest {

    protected static final Trace LOGGER = TraceManager.getTrace(TestAddEntitlement.class);

    private static final File ELISABETH_FILE = new File(TEST_RESOURCE_DIR, "user-elisabeth.xml");
    private static final String USER_ELISABETH_OID = "c0c010c0-d34d-b33f-f00d-111111112222";

    private static final File REQ_USER_JACK_MODIFY_ADD_ENTITLEMENT_TESTERS = new File(TEST_RESOURCE_DIR, "user-jack-modify-add-entitlement-testers.xml");

    public TestAddEntitlement() throws JAXBException {
		super();
	}

    private String jackAccountShadowOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        modifyUserAddAccount(USER_JACK_OID, ACCOUNT_SHADOW_JACK_DUMMY_FILE, initTask, initResult);
    }

    /**
     * Add entitlement to user jack
     */
	@Test
    public void test010AddJackToTesters() throws Exception {
        TestUtil.displayTestTile(this, "test010AddJackToTesters");
        executeTest("test010AddJackToTesters", USER_JACK_OID, new TestDetails() {
            @Override int subtaskCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }
            @Override boolean removeAssignmentsBeforeTest() { return false; }

            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);

                UserType jack = context.getFocusContext().getObjectCurrent().asObjectable();
                AssertJUnit.assertEquals("Jack has wrong number of accounts", 1, jack.getLinkRef().size());
                jackAccountShadowOid = jack.getLinkRef().get(0).getOid();

                LensProjectionContext accountContext = fillContextWithAccount(context, jackAccountShadowOid, result);

                ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_USER_JACK_MODIFY_ADD_ENTITLEMENT_TESTERS, ObjectModificationType.COMPLEX_TYPE);
                ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, context.getFocusClass(), prismContext);
                shadowDelta.setOid(jackAccountShadowOid);
                accountContext.setPrimaryDelta(shadowDelta);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                //assertNotAssignedRole(USER_ELISABETH_OID, ROLE_R3_OID, task, result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                //assertAssignedRole(USER_ELISABETH_OID, ROLE_R3_OID, task, result);
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R3_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_ELISABETH_OID, Arrays.asList(R1BOSS_OID), result);
//                checkUserApproversForCreate(USER_ELISABETH_OID, Arrays.asList(R1BOSS_OID), result);   // this one should remain from test010
                ShadowType account = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
                IntegrationTestTools.display("jack dummy account", account);
                assertHasAssociation(account, new QName("group"), GROUP_TESTERS_OID);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return true;
            }
        });
	}

    public void assertHasAssociation(ShadowType shadow, QName associationName, String entitlementOid) {
        for (ShadowAssociationType association : shadow.getAssociation()) {
            if (QNameUtil.match(association.getName(), associationName) &&
                    entitlementOid.equals(association.getShadowRef().getOid() != null)) {
                return;
            }
        }
        AssertJUnit.fail("No association of type " + associationName + " of " + entitlementOid + " in " + ObjectTypeUtil.toShortString(shadow));
    }


}
