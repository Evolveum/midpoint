/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test for deputy (delegation) mechanism.
 * 
 * MID-3472
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDeputy extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/deputy");
		
	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User Jack", userJack);
        assertNoAssignments(userJack);
        assertLinks(userJack, 0);
        
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        display("User Barbossa", userBarbossa);
        assertNoAssignments(userBarbossa);
        assertLinks(userBarbossa, 0);
	}

	/**
	 * Jack and Barbossa does not have any accounts or roles.
	 * Assign Barbossa as Jack's deputy. Not much should happen.
	 */
    @Test
    public void test100AssignDeputyNoBigDeal() throws Exception {
		final String TEST_NAME = "test100AssignDeputyNoBigDeal";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLinks(userBarbossaAfter, 0);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLinks(userBarbossaAfter, 0);
        
    }
    
    /**
	 * Jack and Barbossa does not have any accounts or roles.
	 * Unassign Barbossa as Jack's deputy. Not much should happen.
	 */
    @Test
    public void test109UnassignDeputyNoBigDeal() throws Exception {
		final String TEST_NAME = "test109UnassignDeputyNoBigDeal";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);        
        assertAssignments(userBarbossaAfter, 0);
        assertLinks(userBarbossaAfter, 0);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLinks(userBarbossaAfter, 0);
        
    }

	private void assignDeputy(String userDeputyOid, String userTargetOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userDeputyOid, userTargetOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY, task, null, null, true, result);
	}

	private void unassignDeputy(String userDeputyOid, String userTargetOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userDeputyOid, userTargetOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY, task, null, null, false, result);
	}

	private <F extends FocusType> void assertAssignedDeputy(PrismObject<F> focus, String targetUserOid) {
		MidPointAsserts.assertAssigned(focus, targetUserOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
	}
    
}
