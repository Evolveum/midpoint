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
package com.evolveum.midpoint.model.intest.orgstruct;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOrgStructCaribbean extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/orgstruct");
    
    protected static final File ORG_CARIBBEAN_FILE = new File(TEST_DIR, "org-caribbean.xml");
	protected static final String ORG_CARIBBEAN_TOP_OID = "00000000-8888-6666-0000-c00000000001";
	protected static final String ORG_CARIBBEAN_THE_CROWN_OID = "00000000-8888-6666-0000-c00000000002";
	protected static final String ORG_CARIBBEAN_JAMAICA_OID = "00000000-8888-6666-0000-c00000000003";
	protected static final String ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID = "00000000-8888-6666-0000-c00000000004";
	protected static final String ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID = "00000000-8888-6666-0000-c00000000005";
	protected static final String ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID = "00000000-8888-6666-0000-c00000000006";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        //DebugUtil.setDetailedDebugDump(true);
    }

    
    /**
     * MID-3448
     */
    @Test
    public void test100AddOrgCaribbean() throws Exception {
        final String TEST_NAME = "test100AddOrgCaribbean";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        repoAddObjectsFromFile(ORG_CARIBBEAN_FILE, OrgType.class, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Moneky Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasNoOrg(orgDoT, ORG_GOVERNOR_OFFICE_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test102RecomputeJamaica() throws Exception {
        final String TEST_NAME = "test102RecomputeJamaica";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);        
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test103ReconcileJamaica() throws Exception {
        final String TEST_NAME = "test103ReconcileJamaica";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileOrg(ORG_CARIBBEAN_JAMAICA_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test104RecomputeGovernor() throws Exception {
        final String TEST_NAME = "test104RecomputeGovernor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_GOVERNOR_OFFICE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test105ReconcileGovernor() throws Exception {
        final String TEST_NAME = "test105ReconcileGovernor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileOrg(ORG_GOVERNOR_OFFICE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }
    
    /**
     * Jamaica has an inducement to Monkey Island Governor Office.
     * Sub-orgs of Jamaica should appear under Governor office. 
     * 
     * MID-3448
     */
    @Test
    public void test106RecomputeDoT() throws Exception {
        final String TEST_NAME = "test106RecomputeDoT";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
        
        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID, ORG_GOVERNOR_OFFICE_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test107ReconcileDoT() throws Exception {
        final String TEST_NAME = "test107ReconcileDoT";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileOrg(ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
        
        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID, ORG_GOVERNOR_OFFICE_OID);
    }
    
    /**
     * Department of People (DoP) has in inducement to Monkey Island Scumm Bar.
     * But that inducement is limited to UserType. Therefore sub-orgs of
     * DoP should not not appear under Scumm Bar.
     * 
     * Related to MID-3448
     */
    @Test
    public void test110RecomputeDoP() throws Exception {
        final String TEST_NAME = "test110RecomputeDoP";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgEntertainmentSection = getObject(OrgType.class, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);
        display("Entertainment Section", orgEntertainmentSection);
        assertHasNoOrg(orgEntertainmentSection, ORG_SCUMM_BAR_OID);
        
        PrismObject<OrgType> orgScummBar = getObject(OrgType.class, ORG_SCUMM_BAR_OID);
        display("Scumm Bar", orgScummBar);
        assertHasNoOrg(orgScummBar, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);
        
    }
    
    /**
     * Department of People (DoP) has in inducement to Monkey Island Scumm Bar.
     * That inducement is limited to UserType. Therefore sub-orgs of
     * DoP should not not appear under Scumm Bar. But when Jack is assigned
     * to the DoP he should also appear under Scumm Bar.
     * 
     * Related to MID-3448
     */
    @Test
    public void test115AssignJackToDoP() throws Exception {
        final String TEST_NAME = "test115AssignJackToDoP";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignOrg(USER_JACK_OID, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, null);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgEntertainmentSection = getObject(OrgType.class, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);
        display("Entertainment Section", orgEntertainmentSection);
        assertHasNoOrg(orgEntertainmentSection, ORG_SCUMM_BAR_OID);
        
        PrismObject<OrgType> orgScummBar = getObject(OrgType.class, ORG_SCUMM_BAR_OID);
        display("Scumm Bar", orgScummBar);
        assertHasNoOrg(orgScummBar, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertHasOrgs(userJackAfter, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, ORG_SCUMM_BAR_OID);
        
    }
   

}
