/*
 * Copyright (c) 2010-2018 Evolveum
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

/**
 *
 */
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * MID-4347
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualGrouping extends AbstractGroupingManualResourceTest {

	@Override
	protected BackingStore createBackingStore() {
		return new CsvBackingStore();
	}
	
	@Override
	protected String getResourceOid() {
		return RESOURCE_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_GROUPING_FILE;
	}

	@Override
	protected String getRoleOneOid() {
		return ROLE_ONE_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getRoleOneFile() {
		return ROLE_ONE_SEMI_MANUAL_GROUPING_FILE;
	}

	@Override
	protected String getRoleTwoOid() {
		return ROLE_TWO_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getRoleTwoFile() {
		return ROLE_TWO_SEMI_MANUAL_GROUPING_FILE;
	}
	
	@Override
	protected String getPropagationTaskOid() {
		return TASK_PROPAGATION_MULTI_OID;
	}

	@Override
	protected File getPropagationTaskFile() {
		return TASK_PROPAGATION_MULTI_FILE;
	}

	@Override
	protected boolean hasMultivalueInterests() {
		return false;
	}
	
	@Override
	protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
		AssertJUnit.assertNull("Resource schema sneaked in before test connection", resourceXsdSchemaElementBefore);
	}

	@Override
	protected int getNumberOfAccountAttributeDefinitions() {
		return 5;
	}
	
	@Override
	protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
		// CSV password is readable
		PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNotNull("No password value property in "+shadow+": "+passValProp, passValProp);
	}

	/**
	 * Create phantom account in the backing store. MidPoint does not know anything about it.
	 * At the same time, there is phantom user that has the account assigned. But it is not yet
	 * provisioned. MidPoint won't figure out that there is already an account, as the propagation
	 * is not execute immediatelly. The conflict will be discovered only later, when propagation
	 * task is run.
	 * MID-4614
	 */
	@Test
	@Override
	public void test400PhantomAccount() throws Exception {
		final String TEST_NAME = "test400PhantomAccount";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		setupPhantom(TEST_NAME);

		// WHEN (mid1)
		displayWhen(TEST_NAME, "mid1");
		recomputeUser(USER_PHANTOM_OID, task, result);

		// THEN (mid1)
		displayThen(TEST_NAME, "mid1");
		String caseOid1 = assertInProgress(result);
		display("Case 1", caseOid1);
		// No case OID yet. The case would be created after propagation is run.
		assertNull("Unexpected case 1 OID", caseOid1);
		
		PrismObject<UserType> userMid1 = getUser(USER_PHANTOM_OID);
		display("User mid1", userMid1);
		String shadowOid = getSingleLinkOid(userMid1);
		PrismObject<ShadowType> shadowMid1NoFetch = getShadowModelNoFetch(shadowOid);
		display("Shadow mid1 (model, noFetch)", shadowMid1NoFetch);

		assertAttribute(shadowMid1NoFetch, ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowMid1NoFetch, 
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null);
		
		clockForward("PT3M");
		
		// WHEN (mid2)
		displayWhen(TEST_NAME, "mid2");
		// Existing account is detected now. Hence partial error.		
		runPropagation();
		
		// Synchronization service kicks in, reconciliation detects wrong full name.
		// It tries to fix it. But as we are in propagation mode, the data are not
		// fixed immediatelly. Instead there is a pending delta to fix the problem.

		// THEN (mid2)
		displayThen(TEST_NAME, "mid2");
		String caseOid2 = assertInProgress(result);
		display("Case 2", caseOid2);
		// No case OID yet. The case will be created after propagation is run.
		assertNull("Unexpected case 2 OID", caseOid2);
		
		PrismObject<UserType> userMid2 = getUser(USER_PHANTOM_OID);
		display("User mid2", userMid2);
		String shadowOidMid2 = getSingleLinkOid(userMid2);
		PrismObject<ShadowType> shadowMid2NoFetch = getShadowModelNoFetch(shadowOidMid2);
		display("Shadow mid2 (model, noFetch)", shadowMid2NoFetch);

		assertAttribute(shadowMid2NoFetch, ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME);

		assertPendingOperationDeltas(shadowMid2NoFetch, 2);
		PendingOperationType fizzledAddOperation = findPendingOperation(shadowMid2NoFetch, OperationResultStatusType.HANDLED_ERROR, ChangeTypeType.ADD);
		assertPendingOperation(shadowMid2NoFetch, fizzledAddOperation,
						PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.HANDLED_ERROR);
		assertNotNull("Null completion timestamp", fizzledAddOperation.getCompletionTimestamp());
		PendingOperationType reconOperation = findPendingOperation(shadowMid2NoFetch, null, ChangeTypeType.MODIFY);
		assertPendingOperation(shadowMid2NoFetch, reconOperation,
				PendingOperationExecutionStatusType.EXECUTION_PENDING, null);
		assertHasModification(reconOperation.getDelta(), new ItemPath(ShadowType.F_ATTRIBUTES, new QName(MidPointConstants.NS_RI, "fullname")));
		
		clockForward("PT20M");

		// WHEN (final)
		displayWhen(TEST_NAME, "final");
		runPropagation();
		
		// THEN
		displayThen(TEST_NAME, "final");
		
		PrismObject<UserType> userAfter = getUser(USER_PHANTOM_OID);
		display("User after", userAfter);
		shadowOid = getSingleLinkOid(userAfter);
		PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
		display("Shadow after (model)", shadowModel);

		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME_WRONG);
		assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHANTOM_DESCRIPTION_MANUAL);
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
//		fizzledAddOperation = findPendingOperation(shadowModel, OperationResultStatusType.HANDLED_ERROR, ChangeTypeType.ADD);
//		assertPendingOperation(shadowModel, fizzledAddOperation,
//						PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.HANDLED_ERROR);
		reconOperation = findPendingOperation(shadowModel, null, ChangeTypeType.MODIFY);
		assertPendingOperation(shadowModel, reconOperation,
				PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);

		// TODO: assert the case
		
		assertSteadyResources();
	}
}