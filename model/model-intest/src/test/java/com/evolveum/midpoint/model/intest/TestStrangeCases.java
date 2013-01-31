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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestStrangeCases extends AbstractInitializedModelIntegrationTest {
	
	private static final File TEST_DIR = new File("src/test/resources/strange");
	
	private static final File USER_DEGHOULASH_FILE = new File(TEST_DIR, "user-deghoulash.xml");
	private static final String USER_DEGHOULASH_OID = "c0c010c0-d34d-b33f-f00d-1d11dd11dd11";

	private static final String NON_EXISTENT_ACCOUNT_OID = "f000f000-f000-f000-f000-f000f000f000";
	
	private String accountGuybrushDummyRedOid;
	
	public TestStrangeCases() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlRed.addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Monkey Island");
		
		PrismObject<AccountShadowType> accountGuybrushDummyRed = addObjectFromFile(ACCOUNT_GUYBRUSH_DUMMY_RED_FILENAME, AccountShadowType.class, initResult);
		accountGuybrushDummyRedOid = accountGuybrushDummyRed.getOid();
	}

	@Test
    public void test100ModifyUserGuybrushAddAccountDummyRedNoAttributesConflict() throws Exception {
		final String TEST_NAME = "test100ModifyUserGuybrushAddAccountDummyRedNoAttributesConflict";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_GUYBRUSH_DUMMY_RED_FILENAME));
        // Remove the attributes. This will allow outbound mapping to take place instead.
        account.removeContainer(AccountShadowType.F_ATTRIBUTES);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		dummyAuditService.clear();
        
		try {
			
			// WHEN
			modelService.executeChanges(deltas, null, task, result);
			
			AssertJUnit.fail("Unexpected executeChanges success");
		} catch (ObjectAlreadyExistsException e) {
			// This is expected
			display("Expected exception", e);
		}
				
		// Check accountRef
		PrismObject<UserType> userGuybrush = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        UserType userGuybrushType = userGuybrush.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userGuybrushType.getAccountRef().size());
        ObjectReferenceType accountRefType = userGuybrushType.getAccountRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        
		// Check shadow
        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        // Check account
        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood");
        
        // Check account in dummy resource
        assertDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        
        result.computeStatus();
        display("executeChanges result", result);
        IntegrationTestTools.assertFailure("executeChanges result", result);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDelta<? extends ObjectType>> auditExecution0Deltas = dummyAuditService.getExecutionDeltas(0);
        assertEquals("Wrong number of execution deltas", 0, auditExecution0Deltas.size());
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
        
	}

	@Test
    public void test180DeleteHalfAssignmentFromUser() throws Exception {
		String TEST_NAME = "test180DeleteHalfAssignmentFromUser";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestStrangeCases.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        PrismObject<UserType> userOtis = createUser("otis", "Otis");
        fillinUserAssignmentAccountConstruction(userOtis, RESOURCE_DUMMY_OID);
		
		display("Half-assigned user", userOtis);
		
		// Remember the assignment so we know what to remove
		AssignmentType assignmentType = userOtis.asObjectable().getAssignment().iterator().next();
		
		// Add to repo to avoid processing of the assignment
		String userOtisOid = repositoryService.addObject(userOtis, result);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, 
        		userOtisOid, UserType.F_ASSIGNMENT, prismContext, assignmentType.asPrismContainerValue().clone());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userOtisAfter = getUser(userOtisOid);
		assertNotNull("Otis is gone!", userOtisAfter);
		// Check accountRef
        assertUserNoAccountRefs(userOtisAfter);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("otis");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDelta<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
        assertEquals("Wrong number of execution deltas", 1, auditExecutionDeltas.size());
        PrismAsserts.asserHasDelta("Audit execution deltas", auditExecutionDeltas, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test190DeleteHalfAssignedUser() throws Exception {
		String TEST_NAME = "test190DeleteHalfAssignedUser";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestStrangeCases.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        PrismObject<UserType> userNavigator = createUser("navigator", "Head of the Navigator");
        fillinUserAssignmentAccountConstruction(userNavigator, RESOURCE_DUMMY_OID);
		
		display("Half-assigned user", userNavigator);
		
		// Add to repo to avoid processing of the assignment
		String userNavigatorOid = repositoryService.addObject(userNavigator, result);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, userNavigatorOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		try {
			getUser(userNavigatorOid);
			AssertJUnit.fail("navigator is still alive!");
		} catch (ObjectNotFoundException ex) {
			// This is OK
		}
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("navigator");
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDelta<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
        assertEquals("Wrong number of execution deltas", 1, auditExecutionDeltas.size());
        PrismAsserts.asserHasDelta("Audit execution deltas", auditExecutionDeltas, ChangeType.DELETE, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test200ModifyUserJackBrokenAccountRef() throws Exception {
		final String TEST_NAME = "test200ModifyUserJackBrokenAccountRef";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        addBrokenAccountRef(USER_JACK_OID);
                        
		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, 
        		PrismTestUtil.createPolyString("Magnificent Captain Jack Sparrow"));
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
        assertAccounts(USER_JACK_OID, 0);
                
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDelta<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
        assertEquals("Wrong number of execution deltas", 2, auditExecutionDeltas.size());
        PrismAsserts.asserHasDelta("Audit execution deltas", auditExecutionDeltas, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	// Lets test various extension magic and border cases now. This is maybe quite hight in the architecture for
	// this test, but we want to make sure that none of the underlying components will screw the things up.
	
	@Test
    public void test300ExtensionSanity() throws Exception {
		final String TEST_NAME = "test300ExtensionSanity";
        displayTestTile(this, TEST_NAME);
        
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<Containerable> extensionContainerDef = userDef.findContainerDefinition(UserType.F_EXTENSION);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_SHIP, DOMUtil.XSD_STRING, 1, 1);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_TALES, DOMUtil.XSD_STRING, 0, 1);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_WEAPON, DOMUtil.XSD_STRING, 0, -1);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_LOOT, DOMUtil.XSD_INT, 0, 1);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_BAD_LUCK, DOMUtil.XSD_LONG, 0, -1);
	}
	
	@Test
    public void test301AddUserDeGhoulash() throws Exception {
		final String TEST_NAME = "test301AddUserDeGhoulash";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_DEGHOULASH_FILE);
        ObjectDelta<UserType> userAddDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userAddDelta);
                        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
		display("User after change execution", userDeGhoulash);
		assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAccounts(USER_JACK_OID, 0);
                
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDelta<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
        assertEquals("Wrong number of execution deltas", 1, auditExecutionDeltas.size());
        PrismAsserts.asserHasDelta("Audit execution deltas", auditExecutionDeltas, ChangeType.ADD, UserType.class);
        dummyAuditService.assertExecutionSuccess();
        
        assertBasicGeGhoulashExtension(userDeGhoulash);
	}
	
	@Test
    public void test310SearchDeGhoulashByShip() throws Exception {
		final String TEST_NAME = "test310SearchDeGhoulashByShip";
        searchDeGhoulash(TEST_NAME, PIRACY_SHIP, "The Undead Pot");
	}
	
	@Test
    public void test311SearchDeGhoulashByTales() throws Exception {
		final String TEST_NAME = "test311SearchDeGhoulashByTales";
        searchDeGhoulash(TEST_NAME, PIRACY_TALES, "Only a dead meat is a good meat");
	}

	@Test
    public void test312SearchDeGhoulashByWeaponSpoon() throws Exception {
		final String TEST_NAME = "test312SearchDeGhoulashByWeaponSpoon";
        searchDeGhoulash(TEST_NAME, PIRACY_WEAPON, "spoon");
	}

	@Test
    public void test313SearchDeGhoulashByWeaponFork() throws Exception {
		final String TEST_NAME = "test313SearchDeGhoulashByWeaponFork";
        searchDeGhoulash(TEST_NAME, PIRACY_WEAPON, "fork");
	}

	@Test
    public void test314SearchDeGhoulashByLoot() throws Exception {
		final String TEST_NAME = "test314SearchDeGhoulashByLoot";
        searchDeGhoulash(TEST_NAME, PIRACY_LOOT, 424242);
	}

	@Test
    public void test315SearchDeGhoulashByBadLuck13() throws Exception {
		final String TEST_NAME = "test315SearchDeGhoulashByBadLuck13";
        searchDeGhoulash(TEST_NAME, PIRACY_BAD_LUCK, 13L);
	}

	@Test
    public void test316SearchDeGhoulashByBadLuck28561() throws Exception {
		final String TEST_NAME = "test316SearchDeGhoulashByBadLuck28561";
        searchDeGhoulash(TEST_NAME, PIRACY_BAD_LUCK, 28561L);
	}

	
    private <T> void searchDeGhoulash(String testName, QName propName, T propValue) throws Exception {
        displayTestTile(this, testName);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + testName);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
                                
        EqualsFilter filter = EqualsFilter.createEqual(UserType.class, prismContext, 
        		new ItemPath(UserType.F_EXTENSION, propName), propValue);
        ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		// WHEN
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        assertFalse("No user found", users.isEmpty());
        assertEquals("Wrong number of users found", 1, users.size());
        PrismObject<UserType> userDeGhoulash = users.iterator().next();
		display("Found user", userDeGhoulash);
		assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
                        
		assertBasicGeGhoulashExtension(userDeGhoulash);
	}
    
    private void assertBasicGeGhoulashExtension(PrismObject<UserType> userDeGhoulash) {
    	assertExtension(userDeGhoulash, PIRACY_SHIP, "The Undead Pot");
        assertExtension(userDeGhoulash, PIRACY_TALES, "Only a dead meat is a good meat");
        assertExtension(userDeGhoulash, PIRACY_WEAPON, "fork", "spoon");
        assertExtension(userDeGhoulash, PIRACY_LOOT, 424242);
        assertExtension(userDeGhoulash, PIRACY_BAD_LUCK, 13L, 169L, 2197L, 28561L, 371293L, 131313131313131313L);
    }

	private <O extends ObjectType, T> void assertExtension(PrismObject<O> object, QName propName, T... expectedValues) {
		PrismContainer<Containerable> extensionContainer = object.findContainer(ObjectType.F_EXTENSION);
		assertNotNull("No extension container in "+object, extensionContainer);
		PrismProperty<T> extensionProperty = extensionContainer.findProperty(propName);
		assertNotNull("No extension property "+propName+" in "+object, extensionProperty);
		PrismAsserts.assertPropertyValues("Values of extension property "+propName, extensionProperty.getValues(), expectedValues);
	}

	/** 
	 * Break the user in the repo by inserting accountRef that points nowhere. 
	 */
	private void addBrokenAccountRef(String userOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		OperationResult result = new OperationResult(TestModelServiceContract.class.getName() + ".addBrokenAccountRef");
		
		Collection<? extends ItemDelta> modifications = ReferenceDelta.createModificationAddCollection(UserType.class, 
				UserType.F_ACCOUNT_REF, prismContext, NON_EXISTENT_ACCOUNT_OID);
		repositoryService.modifyObject(UserType.class, userOid, modifications , result);
		
		result.computeStatus();
		assertSuccess(result);
	}

}
