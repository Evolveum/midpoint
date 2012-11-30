/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOrgStruct extends AbstractInitializedModelIntegrationTest {
	
	public TestOrgStruct() throws JAXBException {
		super();
	}
	
	@Test
    public void test001OrgStructSanity() throws Exception {
		final String TEST_NAME = "test001OrgStructSanity";
        displayTestTile(this, TEST_NAME);
        
        // WHEN
        assertMonkeyIslandOrgSanity();
	}
	
	@Test
    public void test101JackAssignScummBar() throws Exception {
		final String TEST_NAME = "test101JackAssignScummBar";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrg(userJack, ORG_SCUMM_BAR_OID);
        assertHasOrg(userJack, ORG_SCUMM_BAR_OID);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test102JackUnassignScummBar() throws Exception {
		final String TEST_NAME = "test102JackUnassignScummBar";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedNoOrg(userJack);
        assertHasNoOrg(userJack);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Assign jack to both functional and project orgstruct.
	 */
	@Test
    public void test201JackAssignScummBarAndSaveElaine() throws Exception {
		final String TEST_NAME = "test201JackAssignScummBarAndSaveElaine";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        assignOrg(USER_JACK_OID, ORG_SAVE_ELAINE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrg(userJack, ORG_SCUMM_BAR_OID);
        assertAssignedOrg(userJack, ORG_SAVE_ELAINE_OID);
        assertAssignments(userJack, 2);
        assertHasOrg(userJack, ORG_SCUMM_BAR_OID);
        assertHasOrg(userJack, ORG_SAVE_ELAINE_OID);
        assertHasOrgs(userJack, 2);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	/**
	 * Assign jack to functional orgstruct again.
	 */
	@Test
    public void test202JackAssignMinistryOfOffense() throws Exception {
		final String TEST_NAME = "test202JackAssignMinistryOfOffense";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrg(userJack, ORG_SCUMM_BAR_OID);
        assertAssignedOrg(userJack, ORG_SAVE_ELAINE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignments(userJack, 3);
        assertHasOrg(userJack, ORG_SCUMM_BAR_OID);
        assertHasOrg(userJack, ORG_SAVE_ELAINE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, 3);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}
	
	// MID-1065
	@Test(enabled=false)
    public void test207JackUnAssignScummBar() throws Exception {
		final String TEST_NAME = "test207JackUnAssignScummBar";
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestOrgStruct.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);
        
        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrg(userJack, ORG_SAVE_ELAINE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignments(userJack, 2);
        assertHasOrg(userJack, ORG_SAVE_ELAINE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, 2);
        
        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

}
