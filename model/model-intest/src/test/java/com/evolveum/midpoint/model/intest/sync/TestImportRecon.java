/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskResultListener;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImportRecon extends AbstractInitializedModelIntegrationTest {
	
	private static final File TEST_DIR = new File("src/test/resources/sync");
	
	private static final String ACCOUNT_OTIS_NAME = "otis";
	private static final String ACCOUNT_OTIS_FULLNAME = "Otis";
	
	private static final File ACCOUNT_STAN_FILE = new File(TEST_DIR, "account-stan-dummy.xml");
	private static final String ACCOUNT_STAN_OID = "22220000-2200-0000-0000-444400004455";
	private static final String ACCOUNT_STAN_NAME = "stan";
	private static final String ACCOUNT_STAN_FULLNAME = "Stan the Salesman";
	
	private static final String ACCOUNT_RUM_NAME = "rum";
	
	private static final String ACCOUNT_MURRAY_NAME = "murray";
	
	private static final String ACCOUNT_CAPSIZE_NAME = "capsize";
	private static final String ACCOUNT_CAPSIZE_FULLNAME = "Kata Capsize";
	
	private static final String USER_AUGUSTUS_NAME = "augustus";

	private static final File ACCOUNT_AUGUSTUS_FILE = new File(TEST_DIR, "account-augustus-dummy.xml");
	private static final String ACCOUNT_AUGUSTUS_OID = "22220000-2200-0000-0000-444400004457";
	private static final String ACCOUNT_AUGUSTUS_NAME = "augustus";
	private static final String ACCOUNT_AUGUSTUS_FULLNAME = "Augustus DeWaat";
	
	private static final File ACCOUNT_TAUGUSTUS_FILE = new File(TEST_DIR, "account-taugustus-dummy.xml");
	private static final String ACCOUNT_TAUGUSTUS_OID = "22220000-2200-0000-0000-444400004456";
	private static final String ACCOUNT_TAUGUSTUS_NAME = "Taugustus";
	private static final String ACCOUNT_TAUGUSTUS_FULLNAME = "Augustus DeWaat";
	
	private static final File ACCOUNT_KENNY_FILE = new File(TEST_DIR, "account-kenny-dummy.xml");
	private static final String ACCOUNT_KENNY_OID = "22220000-2200-0000-0000-444400004461";
	private static final String ACCOUNT_KENNY_NAME = "kenny";
	private static final String ACCOUNT_KENNY_FULLNAME = "Kenny Falmouth";

	private static final String USER_PALIDO_NAME = "palido";
	private static final File ACCOUNT_TPALIDO_FILE = new File(TEST_DIR, "account-tpalido-dummy.xml");
	private static final String ACCOUNT_TPALIDO_OID = "22220000-2200-0000-0000-444400004462";
	private static final String ACCOUNT_TPALIDO_NAME = "Tpalido";
	private static final String ACCOUNT_TPALIDO_FULLNAME = "Palido Domingo";
	
	private static final File ACCOUNT_LECHIMP_FILE = new File(TEST_DIR, "account-lechimp-dummy.xml");
	private static final String ACCOUNT_LECHIMP_OID = "22220000-2200-0000-0000-444400004463";
	private static final String ACCOUNT_LECHIMP_NAME = "lechimp";
	private static final String ACCOUNT_LECHIMP_FULLNAME = "Captain LeChimp";

	private static final File ACCOUNT_TLECHIMP_FILE = new File(TEST_DIR, "account-tlechimp-dummy.xml");
	private static final String ACCOUNT_TLECHIMP_OID = "22220000-2200-0000-0000-444400004464";
	private static final String ACCOUNT_TLECHIMP_NAME = "Tlechimp";
	private static final String ACCOUNT_TLECHIMP_FULLNAME = "Captain LeChimp";
	
	private static final File ACCOUNT_ANDRE_FILE = new File(TEST_DIR, "account-andre-dummy.xml");
	private static final String ACCOUNT_ANDRE_OID = "22220000-2200-0000-0000-444400004465";
	private static final String ACCOUNT_ANDRE_NAME = "andre";
	private static final String ACCOUNT_ANDRE_FULLNAME = "King Andre";
	
	private static final File ACCOUNT_TANDRE_FILE = new File(TEST_DIR, "account-tandre-dummy.xml");
	private static final String ACCOUNT_TANDRE_OID = "22220000-2200-0000-0000-444400004466";
	private static final String ACCOUNT_TANDRE_NAME = "Tandre";
	private static final String ACCOUNT_TANDRE_FULLNAME = "King Andre";
	
	private static final String USER_LAFOOT_NAME = "lafoot";
	private static final File ACCOUNT_TLAFOOT_FILE = new File(TEST_DIR, "account-tlafoot-dummy.xml");
	private static final String ACCOUNT_TLAFOOT_OID = "22220000-2200-0000-0000-444400004467";
	private static final String ACCOUNT_TLAFOOT_NAME = "Tlafoot";
	private static final String ACCOUNT_TLAFOOT_FULLNAME = "Effete LaFoot";
	
	private static final File ACCOUNT_CRUFF_FILE = new File(TEST_DIR, "account-cruff-dummy.xml");
	private static final String ACCOUNT_CRUFF_OID = "22220000-2200-0000-0000-444400004468";
	private static final String ACCOUNT_CRUFF_NAME = "cruff";
	private static final String ACCOUNT_CRUFF_FULLNAME = "Cruff";

	protected static final File RESOURCE_DUMMY_AZURE_FILE = new File(TEST_DIR, "resource-dummy-azure.xml");
	protected static final File RESOURCE_DUMMY_AZURE_DEPRECATED_FILE = new File(TEST_DIR, "resource-dummy-azure-deprecated.xml");
	protected static final String RESOURCE_DUMMY_AZURE_OID = "10000000-0000-0000-0000-00000000a204";
	protected static final String RESOURCE_DUMMY_AZURE_NAME = "azure";
	protected static final String RESOURCE_DUMMY_AZURE_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_LIME_FILE = new File(TEST_DIR, "resource-dummy-lime.xml");
	protected static final File RESOURCE_DUMMY_LIME_DEPRECATED_FILE = new File(TEST_DIR, "resource-dummy-lime-deprecated.xml");
	protected static final String RESOURCE_DUMMY_LIME_OID = "10000000-0000-0000-0000-000000131404";
	protected static final String RESOURCE_DUMMY_LIME_NAME = "lime";
	protected static final String RESOURCE_DUMMY_LIME_NAMESPACE = MidPointConstants.NS_RI;

    protected static final File TASK_RECONCILE_DUMMY_SINGLE_FILE = new File(TEST_DIR, "task-reconcile-dummy-single.xml");
    protected static final String TASK_RECONCILE_DUMMY_SINGLE_OID = "10000000-0000-0000-5656-565600000004";

	protected static final File TASK_RECONCILE_DUMMY_AZURE_FILE = new File(TEST_DIR, "task-reconcile-dummy-azure.xml");
	protected static final String TASK_RECONCILE_DUMMY_AZURE_OID = "10000000-0000-0000-5656-56560000a204";

	protected static final File TASK_RECONCILE_DUMMY_LIME_FILE = new File(TEST_DIR, "task-reconcile-dummy-lime.xml");
	protected static final String TASK_RECONCILE_DUMMY_LIME_OID = "10000000-0000-0000-5656-565600131204";
	
	protected DummyResource dummyResourceAzure;
	protected DummyResourceContoller dummyResourceCtlAzure;
	protected ResourceType resourceDummyAzureType;
	protected PrismObject<ResourceType> resourceDummyAzure;
	
	protected DummyResource dummyResourceLime;
	protected DummyResourceContoller dummyResourceCtlLime;
	protected ResourceType resourceDummyLimeType;
	protected PrismObject<ResourceType> resourceDummyLime;

	@Autowired(required=true)
	private ReconciliationTaskHandler reconciliationTaskHandler;
	
	private DebugReconciliationTaskResultListener reconciliationTaskResultListener;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		reconciliationTaskResultListener = new DebugReconciliationTaskResultListener();
		reconciliationTaskHandler.setReconciliationTaskResultListener(reconciliationTaskResultListener);
		
		dummyResourceCtlAzure = DummyResourceContoller.create(RESOURCE_DUMMY_AZURE_NAME, resourceDummyAzure);
		dummyResourceCtlAzure.extendSchemaPirate();
		dummyResourceAzure = dummyResourceCtlAzure.getDummyResource();
		resourceDummyAzure = importAndGetObjectFromFile(ResourceType.class, getDummyResourceAzureFile(), RESOURCE_DUMMY_AZURE_OID, initTask, initResult); 
		resourceDummyAzureType = resourceDummyAzure.asObjectable();
		dummyResourceCtlAzure.setResource(resourceDummyAzure);	
		
		dummyResourceCtlLime = DummyResourceContoller.create(RESOURCE_DUMMY_LIME_NAME, resourceDummyLime);
		dummyResourceCtlLime.extendSchemaPirate();
		dummyResourceLime = dummyResourceCtlLime.getDummyResource();
		resourceDummyLime = importAndGetObjectFromFile(ResourceType.class, getDummyResourceLimeFile(), RESOURCE_DUMMY_LIME_OID, initTask, initResult); 
		resourceDummyLimeType = resourceDummyLime.asObjectable();
		dummyResourceCtlLime.setResource(resourceDummyLime);
		
		// Create an account that midPoint does not know about yet
		dummyResourceCtl.addAccount(USER_RAPP_USERNAME, "Rapp Scallion", "Scabb Island");
		
		dummyResourceCtlLime.addAccount(USER_RAPP_USERNAME, "Rapp Scallion", "Scabb Island");
		dummyResourceCtlLime.addAccount(ACCOUNT_RUM_NAME, "Rum Rogers");
		dummyResourceCtlLime.addAccount(ACCOUNT_MURRAY_NAME, "Murray");
		
		// And a user that will be correlated to that account
		repoAddObjectFromFile(USER_RAPP_FILE, UserType.class, initResult);
		
		// 
		PrismObject<ShadowType> accountStan = PrismTestUtil.parseObject(ACCOUNT_STAN_FILE);
		provisioningService.addObject(accountStan, null, null, initTask, initResult);
		
		addObject(SHADOW_GROUP_DUMMY_TESTERS_FILE, initTask, initResult);
		
		InternalMonitor.reset();
		InternalMonitor.setTraceShadowFetchOperation(true);
		
//		DebugUtil.setDetailedDebugDump(true);
	}
	
	protected File getDummyResourceLimeFile() {
		return RESOURCE_DUMMY_LIME_FILE;
	}

	protected File getDummyResourceAzureFile() {
		return RESOURCE_DUMMY_AZURE_FILE;
	}

	@Test
    public void test100ImportStanFromResourceDummy() throws Exception {
		final String TEST_NAME = "test100ImportStanFromResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Preconditions
        assertUsers(6);
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(ACCOUNT_STAN_OID, task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
        
        // First fetch: import handler reading the account
        // Second fetch: fetchback to correctly process inbound (import changes the account).
//        assertShadowFetchOperationCountIncrement(2);
        
        // WHY???
        assertShadowFetchOperationCountIncrement(1);
                
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertUsers(7);
        
        // Check audit
        assertImportAuditModifications(1);

	}

	@Test
    public void test150ImportFromResourceDummy() throws Exception {
		final String TEST_NAME = "test150ImportFromResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Preconditions
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users before import", users);
        assertEquals("Unexpected number of users", 7, users.size());
        
        PrismObject<UserType> rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp has dummy account but it is not linked
        assertLinks(rapp, 0);
        
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_OID, new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        TestUtil.assertSuccess(task.getResult());
        
        // First fetch: search in import handler
        // 6 fetches: fetchback to correctly process inbound (import changes the account).
        // The accounts are modified during import as there are also outbound mappings in
        // ther dummy resource. As the import is in fact just a recon the "fetchbacks" happens.
        // One is because of counting resource objects before importing them.
//        assertShadowFetchOperationCountIncrement(8);
        
        // WHY????
        assertShadowFetchOperationCountIncrement(4);
        
        users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertEquals("Unexpected number of users", 8, users.size());
        
        // Check audit
        assertImportAuditModifications(4);
	}
	
	@Test
    public void test155ImportFromResourceDummyAgain() throws Exception {
		final String TEST_NAME = "test155ImportFromResourceDummyAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_OID, new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        TestUtil.assertSuccess(task.getResult());
        
        // First fetch: search in import handler
        // Even though there are outbound mappings these were already processes
        // by previous import run. There are no account modifications this time.
        // Therefore there should be no "fetchbacks".
        // Second fetch is because of counting resource objects before importing them.
        assertShadowFetchOperationCountIncrement(2);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertEquals("Unexpected number of users", 8, users.size());
        
        // Check audit
        assertImportAuditModifications(0);
	}
	
	@Test
    public void test160ImportFromResourceDummyLime() throws Exception {
		final String TEST_NAME = "test160ImportFromResourceDummyLime";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Preconditions
        assertUsers(8);
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_LIME_OID, new QName(RESOURCE_DUMMY_LIME_NAMESPACE, "AccountObjectClass"), task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        TestUtil.assertSuccess(task.getResult());
        
        // One fetch: search in import handler
        // There are no outbound mappings in lime resource, therefore there are no
        // modifications of accounts during import, therefore there are no "fetchbacks".
        // Second is because of counting resource objects before importing them.
        assertShadowFetchOperationCountIncrement(2);
                
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_RUM_NAME, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_MURRAY_NAME, RESOURCE_DUMMY_LIME_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertUsers(10);
        
        // Check audit
        assertImportAuditModifications(3);
	}

	@Test
    public void test200ReconcileDummy() throws Exception {
		final String TEST_NAME = "test200ReconcileDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Lets do some local changes on dummy resource
        DummyAccount guybrushDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        // fullname has a normal outbound mapping, this change should NOT be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Dubrish Freepweed");
        
        // location has strong outbound mapping, this change should be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "The Forbidded Dodecahedron");
        
        // Weapon has a weak mapping, this change should be left as it is
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Feather duster");
        
        // Drink is not tolerant. The extra values should be removed
        guybrushDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

        // Quote is tolerant. The extra values should stay as it is
        guybrushDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "I want to be a pirate!");

        
        // Calypso is protected, this should not reconcile
        DummyAccount calypsoDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        calypsoDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");
        
        dummyResource.purgeScriptHistory();
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_RECONCILE_DUMMY_SINGLE_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, false);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        // First fetch: searchIterative
        // Second fetch: "fetchback" of modified account (guybrush)
        assertShadowFetchOperationCountIncrement(2);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        // Guybrushes fullname should NOT be corrected back to real fullname
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Dubrish Freepweed");
        // Guybrushes location should be corrected back to real value
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, 
        		"Melee Island");
        // Guybrushes weapon should be left untouched
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, 
        		"Feather duster");
        // Guybrushes drink should be corrected
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"rum");
        // Guybrushes quotes should be left untouched
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, 
        		"Arr!", "I want to be a pirate!");
        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Calypso");
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource", dummyResource.debugDump());
        
        display("Script history", dummyResource.getScriptHistory());
        
        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<ProvisioningScriptSpec>();
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        addReconScripts(scripts, USER_RAPP_USERNAME, "Rapp Scallion", false);
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));
        
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
	}

	@Test
    public void test210ReconcileDummyBroken() throws Exception {
		final String TEST_NAME = "test210ReconcileDummyBroken";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Lets do some local changes on dummy resource ... 
        DummyAccount guybrushDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        // location has strong outbound mapping, this change should be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Phatt Island");
        
        // BREAK it!
        dummyResource.setBreakMode(BreakMode.NETWORK);

        dummyResource.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_OID);
        waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconciliation (broken resource)", users);
        
        // Total error in the recon process. No reasonable result here.
//        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 1, 0);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource", dummyResource.debugDump());
        
        display("Script history", dummyResource.getScriptHistory());
        
        // no scripts
        IntegrationTestTools.assertScripts(dummyResource.getScriptHistory());
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertFailure(reconTaskResult);
        
        // Check audit
        display("Audit", dummyAuditService);
        
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
        dummyAuditService.assertExecutionMessage();
	}
	
	/**
	 * Simply re-run recon after the resource is fixed. This should correct the data.
	 */
	@Test
    public void test219ReconcileDummyFixed() throws Exception {
		final String TEST_NAME = "test219ReconcileDummyFixed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Fix it!
        dummyResource.setBreakMode(BreakMode.NONE);

        dummyResource.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_OID);
        waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        // First fetch: searchIterative
        // Second fetch: "fetchback" of modified account (guybrush)
        assertShadowFetchOperationCountIncrement(2);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Dubrish Freepweed");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, 
        		"Melee Island");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, 
        		"Feather duster");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"rum");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, 
        		"Arr!", "I want to be a pirate!");
        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Calypso");
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource", dummyResource.debugDump());
        
        display("Script history", dummyResource.getScriptHistory());
        
        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<ProvisioningScriptSpec>();
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        addReconScripts(scripts, USER_RAPP_USERNAME, "Rapp Scallion", false);
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));
        
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
	}
	
	/**
	 * The resource itself works, just the guybrush account is broken.
	 */
	@Test
    public void test220ReconcileDummyBrokenGuybrush() throws Exception {
		final String TEST_NAME = "test220ReconcileDummyBrokenGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Lets do some local changes on dummy resource ... 
        DummyAccount guybrushDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        // location has strong outbound mapping, this change should be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Forbidden Dodecahedron");
        
        // BREAK it!
        dummyResource.setBreakMode(BreakMode.NONE);
        guybrushDummyAccount.setModifyBreakMode(BreakMode.NETWORK);

        dummyResource.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_OID);
        waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconciliation (broken resource)", users);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 1, 0);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource", dummyResource.debugDump());
        
        display("Script history", dummyResource.getScriptHistory());
        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<ProvisioningScriptSpec>();
        // Guybrush is broken.
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true, false);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        addReconScripts(scripts, USER_RAPP_USERNAME, "Rapp Scallion", false);
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
		TestUtil.assertStatus(reconTaskResult, OperationResultStatusType.PARTIAL_ERROR);
		assertTrue("Errors not mentioned in the task message", reconTaskResult.getMessage().contains("got 1 error"));
        
        // Check audit
        display("Audit", dummyAuditService);
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);
	}

	/**
	 * Simply re-run recon after the resource is fixed. This should correct the data.
	 */
	@Test
    public void test229ReconcileDummyFixed() throws Exception {
		final String TEST_NAME = "test229ReconcileDummyFixed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Fix it!
        dummyResource.setBreakMode(BreakMode.NONE);
        dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME).setModifyBreakMode(BreakMode.NONE);

        dummyResource.purgeScriptHistory();
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_OID);
        waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        // First fetch: searchIterative
        // Second fetch: "fetchback" of modified account (guybrush)
        assertShadowFetchOperationCountIncrement(2);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Dubrish Freepweed");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, 
        		"Melee Island");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, 
        		"Feather duster");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"rum");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, 
        		"Arr!", "I want to be a pirate!");
        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Calypso");
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource", dummyResource.debugDump());
        
        display("Script history", dummyResource.getScriptHistory());
        
        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<ProvisioningScriptSpec>();
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        addReconScripts(scripts, USER_RAPP_USERNAME, "Rapp Scallion", false);
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));
        
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
	}
	
	private void addReconScripts(Collection<ProvisioningScriptSpec> scripts, String username, String fullName, boolean modified) {
		addReconScripts(scripts, username, fullName, modified, true);
	}
	
	private void addReconScripts(Collection<ProvisioningScriptSpec> scripts, String username, String fullName, 
			boolean modified, boolean afterRecon) {
		// before recon
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("The vorpal blade went snicker-snack!");
		script.addArgSingle("who", username);
		scripts.add(script);
		
		if (modified) {
			script = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
			script.addArgSingle("howMuch", null);
			script.addArgSingle("howLong", "from here to there");
			script.addArgSingle("who", username);
			script.addArgSingle("whatchacallit", fullName);
			scripts.add(script);
		}
		
		if (afterRecon) {
			// after recon
			script = new ProvisioningScriptSpec("He left it dead, and with its head");
			script.addArgSingle("how", "enabled");
			scripts.add(script);
		}
	}
	
	@Test
    public void test300ReconcileDummyAzure() throws Exception {
		final String TEST_NAME = "test300ReconcileDummyAzure";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        dummyResource.setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);
        
        // Create some illegal account
        dummyResourceCtlAzure.addAccount(ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME);
        
        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_RECONCILE_DUMMY_AZURE_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_AZURE_OID, false);
        
        TestUtil.displayThen(TEST_NAME);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 0);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        
        // Otis
        assertNoImporterUserByUsername(ACCOUNT_OTIS_NAME);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME, false);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Calypso");
        
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource (azure)", dummyResourceAzure.debugDump());
        
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_AZURE_OID);
	}
	
	@Test
    public void test310ReconcileDummyAzureAgain() throws Exception {
		final String TEST_NAME = "test310ReconcileDummyAzureAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        dummyResource.setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);
        
        PrismObject<TaskType> reconTask = getTask(TASK_RECONCILE_DUMMY_AZURE_OID);
        display("Recon task", reconTask);
        
        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_AZURE_OID);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_AZURE_OID, false);
        
        TestUtil.displayThen(TEST_NAME);
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        
        // Otis
        assertNoImporterUserByUsername(ACCOUNT_OTIS_NAME);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME, false);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Calypso");
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource (azure)", dummyResourceAzure.debugDump());
        
        assertReconAuditModifications(0, TASK_RECONCILE_DUMMY_AZURE_OID);
	}
	
	@Test
    public void test400ReconcileDummyLimeAddAccount() throws Exception {
		final String TEST_NAME = "test400ReconcileDummyLimeAddAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Create some illegal account
        dummyResourceCtlLime.addAccount(ACCOUNT_CAPSIZE_NAME, ACCOUNT_CAPSIZE_FULLNAME);
        
        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_RECONCILE_DUMMY_LIME_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_LIME_OID, false);
        
        TestUtil.displayThen(TEST_NAME);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_LIME_OID, 0, 4, 0, 0);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImporterUserByUsername(ACCOUNT_OTIS_NAME);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME, false);
        
        // Kate Capsize: user should be created
        assertImportedUserByUsername(ACCOUNT_CAPSIZE_NAME, RESOURCE_DUMMY_LIME_OID);
        
        assertEquals("Unexpected number of users", 11, users.size());
        
        display("Dummy resource (lime)", dummyResourceLime.debugDump());
        
        // Audit record structure is somehow complex here.
//        assertReconAuditModifications(4, TASK_RECONCILE_DUMMY_LIME_OID);
	}
	
	@Test
    public void test410ReconcileDummyLimeDeleteLinkedAccount() throws Exception {
		final String TEST_NAME = "test410ReconcileDummyLimeDeleteLinkedAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Create some illegal account
        dummyResourceLime.deleteAccountByName(ACCOUNT_CAPSIZE_NAME);
        
        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_LIME_OID);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_LIME_OID, false);
        
        TestUtil.displayThen(TEST_NAME);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);
        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_LIME_OID, 0, 3, 0, 1);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);        
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImporterUserByUsername(ACCOUNT_OTIS_NAME);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME, false);
        
        // Kate Capsize: user should be gone
        assertNoImporterUserByUsername(ACCOUNT_CAPSIZE_NAME);
        
        assertEquals("Unexpected number of users", 10, users.size());
        
        display("Dummy resource (lime)", dummyResourceLime.debugDump());
        
        // Audit record structure is somehow complex here.
//        assertReconAuditModifications(4, TASK_RECONCILE_DUMMY_LIME_OID);
	}
	
	/**
	 * Imports a testing account (Taugustus)
	 */
	@Test
    public void test500ImportTAugustusFromResourceDummy() throws Exception {
		final String TEST_NAME = "test500ImportTAugustusFromResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> accountStan = PrismTestUtil.parseObject(ACCOUNT_TAUGUSTUS_FILE);
		provisioningService.addObject(accountStan, null, null, task, result);
        
        // Preconditions
        assertUsers(10);
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(ACCOUNT_TAUGUSTUS_OID, task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
        
        // First fetch: import handler reading the account
        // Second fetch: fetchback to correctly process inbound (import changes the account).
//        assertShadowFetchOperationCountIncrement(2);
        
        // WHY???
        assertShadowFetchOperationCountIncrement(1);
                
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertUsers(11);
        
        assertShadowKindIntent(ACCOUNT_TAUGUSTUS_OID, ShadowKindType.ACCOUNT, INTENT_TEST);
        
        // Check audit
        assertImportAuditModifications(1);

	}
	
	/**
	 * Imports a default account (augustus), it should be linked
	 */
	@Test
    public void test502ImportAugustusFromResourceDummy() throws Exception {
		final String TEST_NAME = "test502ImportAugustusFromResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_AUGUSTUS_FILE);
		provisioningService.addObject(account, null, null, task, result);
        
        // Preconditions
        assertUsers(11);
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(ACCOUNT_AUGUSTUS_OID, task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
        
        // First fetch: import handler reading the account
        // Second fetch: fetchback to correctly process inbound (import changes the account).
//        assertShadowFetchOperationCountIncrement(2);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertUsers(11);
        
        assertShadowKindIntent(ACCOUNT_AUGUSTUS_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        assertShadowKindIntent(ACCOUNT_TAUGUSTUS_OID, ShadowKindType.ACCOUNT, INTENT_TEST);
        
        // Check audit
        assertImportAuditModifications(1);

	}
	
	/**
	 * This should import all the intents in the object class
	 */
	@Test
    public void test510ImportFromResourceDummy() throws Exception {
		final String TEST_NAME = "test510ImportFromResourceDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_KENNY_FILE);
        provisioningService.addObject(account, null, null, task, result);
        
        account = PrismTestUtil.parseObject(ACCOUNT_TPALIDO_FILE);
		provisioningService.addObject(account, null, null, task, result);

        account = PrismTestUtil.parseObject(ACCOUNT_LECHIMP_FILE);
		provisioningService.addObject(account, null, null, task, result);

        account = PrismTestUtil.parseObject(ACCOUNT_TLECHIMP_FILE);
		provisioningService.addObject(account, null, null, task, result);
		
		account = PrismTestUtil.parseObject(ACCOUNT_ANDRE_FILE);
		provisioningService.addObject(account, null, null, task, result);

		account = PrismTestUtil.parseObject(ACCOUNT_TANDRE_FILE);
		provisioningService.addObject(account, null, null, task, result);

		account = PrismTestUtil.parseObject(ACCOUNT_TLAFOOT_FILE);
		provisioningService.addObject(account, null, null, task, result);

		account = PrismTestUtil.parseObject(ACCOUNT_CRUFF_FILE);
		provisioningService.addObject(account, null, null, task, result);
		
        // Preconditions
		assertUsers(11);
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_OID, new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        TestUtil.assertSuccess(task.getResult());
        
        // First fetch: search in import handler
        // 6 fetches: fetchback to correctly process inbound (import changes the account).
        // The accounts are modified during import as there are also outbound mappings in
        // ther dummy resource. As the import is in fact just a recon the "fetchbacks" happens.
        // One is because of counting resource objects before importing them.
//        assertShadowFetchOperationCountIncrement(8);
        
        // WHY????
//        assertShadowFetchOperationCountIncrement(4);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_KENNY_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_PALIDO_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_LECHIMP_NAME, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_CRUFF_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_LAFOOT_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_ANDRE_NAME, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        assertNoImporterUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImporterUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        
        assertUsers(17);
        
        assertShadowKindIntent(ACCOUNT_AUGUSTUS_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        assertShadowKindIntent(ACCOUNT_TAUGUSTUS_OID, ShadowKindType.ACCOUNT, INTENT_TEST);
	}

	private void assertImportAuditModifications(int expectedModifications) {
		display("Audit", dummyAuditService);
        
        List<AuditEventRecord> auditRecords = dummyAuditService.getRecords();
        
    	int i=0;
    	int modifications = 0;
    	for (; i < (auditRecords.size() - 1); i+=2) {
        	AuditEventRecord requestRecord = auditRecords.get(i);
        	assertNotNull("No request audit record ("+i+")", requestRecord);
        	assertEquals("Got this instead of request audit record ("+i+"): "+requestRecord, AuditEventStage.REQUEST, requestRecord.getEventStage());
        	Collection<ObjectDeltaOperation<? extends ObjectType>> requestDeltas = requestRecord.getDeltas();
        	assertTrue("Unexpected delta in request audit record "+requestRecord, requestDeltas == null || 
        			requestDeltas.isEmpty() || (requestDeltas.size() == 1 && requestDeltas.iterator().next().getObjectDelta().isAdd()));

        	AuditEventRecord executionRecord = auditRecords.get(i+1);
        	assertNotNull("No execution audit record ("+i+")", executionRecord);
        	assertEquals("Got this instead of execution audit record ("+i+"): "+executionRecord, AuditEventStage.EXECUTION, executionRecord.getEventStage());
        	
        	assertTrue("Empty deltas in execution audit record "+executionRecord, executionRecord.getDeltas() != null && ! executionRecord.getDeltas().isEmpty());
        	modifications++;
        	
        	// check next records
        	while (i < (auditRecords.size() - 2)) {
        		AuditEventRecord nextRecord = auditRecords.get(i+2);
        		if (nextRecord.getEventStage() == AuditEventStage.EXECUTION) {
        			// more than one execution record is OK
        			i++;
        		} else {
        			break;
        		}
        	}

        }
        assertEquals("Unexpected number of audit modifications", expectedModifications, modifications);
	}

	private void assertReconAuditModifications(int expectedModifications, String taskOid) {
		// Check audit
        display("Audit", dummyAuditService);
        
        List<AuditEventRecord> auditRecords = dummyAuditService.getRecords();

        Iterator<AuditEventRecord> iterator = auditRecords.iterator();
        while (iterator.hasNext()) {
        	AuditEventRecord record = iterator.next();
	    	if (record.getTaskOID() != null && !record.getTaskOID().equals(taskOid)) {
	    		// Record from some other task, skip it
	    		iterator.remove();
	    	}
        }

    	int i=0;
    	while (i < (auditRecords.size() - 1)) {
    		AuditEventRecord reconStartRecord = auditRecords.get(i);
    		if (reconStartRecord.getEventType() == AuditEventType.EXECUTE_CHANGES_RAW) {
    			i++;
    			continue;
    		}
            assertNotNull("No reconStartRecord audit record", reconStartRecord);
        	assertEquals("Wrong stage in reconStartRecord audit record: "+reconStartRecord, AuditEventStage.REQUEST, reconStartRecord.getEventStage());
        	assertEquals("Wrong type in reconStartRecord audit record: "+reconStartRecord, AuditEventType.RECONCILIATION, reconStartRecord.getEventType());
        	assertTrue("Unexpected delta in reconStartRecord audit record "+reconStartRecord, reconStartRecord.getDeltas() == null || reconStartRecord.getDeltas().isEmpty());
        	i++;
        	break;
    	}

    	int modifications = 0;
    	for (; i < (auditRecords.size() - 1); i+=2) {
        	AuditEventRecord requestRecord = auditRecords.get(i);
        	assertNotNull("No request audit record ("+i+")", requestRecord);

            if (requestRecord.getEventStage() == AuditEventStage.EXECUTION && requestRecord.getEventType() == AuditEventType.RECONCILIATION) {
                // end of audit records;
                break;
            }
        	        	
        	assertEquals("Got this instead of request audit record ("+i+"): "+requestRecord, AuditEventStage.REQUEST, requestRecord.getEventStage());
        	// Request audit may or may not have a delta. Usual records will not have a delta. But e.g. disableAccount reactions will have.

        	AuditEventRecord executionRecord = auditRecords.get(i+1);
        	assertNotNull("No execution audit record (" + i + ")", executionRecord);
        	assertEquals("Got this instead of execution audit record (" + i + "): " + executionRecord, AuditEventStage.EXECUTION, executionRecord.getEventStage());
        	
        	assertTrue("Empty deltas in execution audit record "+executionRecord, executionRecord.getDeltas() != null && ! executionRecord.getDeltas().isEmpty());
        	modifications++;

        }
        assertEquals("Unexpected number of audit modifications", expectedModifications, modifications);

        AuditEventRecord reconStopRecord = auditRecords.get(i);
        assertNotNull("No reconStopRecord audit record", reconStopRecord);
    	assertEquals("Wrong stage in reconStopRecord audit record: "+reconStopRecord, AuditEventStage.EXECUTION, reconStopRecord.getEventStage());
    	assertEquals("Wrong type in reconStopRecord audit record: "+reconStopRecord, AuditEventType.RECONCILIATION, reconStopRecord.getEventType());
    	assertTrue("Unexpected delta in reconStopRecord audit record "+reconStopRecord, reconStopRecord.getDeltas() == null || reconStopRecord.getDeltas().isEmpty());
	}

	private void assertNoImporterUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = findUserByUsername(username);
        assertNull("User "+username+" sneaked in", user);
	}

	private void assertImportedUserByOid(String userOid, String... resourceOids) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = getUser(userOid);
		assertNotNull("No user "+userOid, user);
		assertImportedUser(user, resourceOids);
	}
		
	private void assertImportedUserByUsername(String username, String... resourceOids) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = findUserByUsername(username);
		assertNotNull("No user "+username, user);
		assertImportedUser(user, resourceOids);
	}
		
	private void assertImportedUser(PrismObject<UserType> user, String... resourceOids) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        assertLinks(user, resourceOids.length);
        for (String resourceOid: resourceOids) {
        	assertAccount(user, resourceOid);
        }
        assertAdministrativeStatusEnabled(user);
	}

}
