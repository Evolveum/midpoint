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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIteration extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/iteration");
	
	protected static final File USER_XAVIER_FILE = new File(TEST_DIR, "user-xavier.xml");
	protected static final String USER_XAVIER_OID = "c0c010c0-d34d-b33f-f00d-11111111aaa1";
	
	// Plain iteration, no iteration expressions
	protected static final File RESOURCE_DUMMY_PINK_FILE = new File(TEST_DIR, "resource-dummy-pink.xml");
	protected static final String RESOURCE_DUMMY_PINK_OID = "10000000-0000-0000-0000-00000000a104";
	protected static final String RESOURCE_DUMMY_PINK_NAME = "pink";
	protected static final String RESOURCE_DUMMY_PINK_NAMESPACE = MidPointConstants.NS_RI;
	
	// Iteration with token expression, pre-iteration condition and post-iteration condition
	protected static final File RESOURCE_DUMMY_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-violet.xml");
	protected static final String RESOURCE_DUMMY_VIOLET_OID = "10000000-0000-0000-0000-00000000a204";
	protected static final String RESOURCE_DUMMY_VIOLET_NAME = "violet";
	protected static final String RESOURCE_DUMMY_VIOLET_NAMESPACE = MidPointConstants.NS_RI;
	
	// similar to violet but it works in the inbound direction
	protected static final File RESOURCE_DUMMY_DARK_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-dark-violet.xml");
	protected static final String RESOURCE_DUMMY_DARK_VIOLET_OID = "10000000-0000-0000-0000-0000000da204";
	protected static final String RESOURCE_DUMMY_DARK_VIOLET_NAME = "darkViolet";
	protected static final String RESOURCE_DUMMY_DARK_VIOLET_NAMESPACE = MidPointConstants.NS_RI;
	
	// iteration, token expression, post-iteration condition that invokes isUniquAccountValue()
	protected static final File RESOURCE_DUMMY_MAGENTA_FILE = new File(TEST_DIR, "resource-dummy-magenta.xml");
	protected static final String RESOURCE_DUMMY_MAGENTA_OID = "10000000-0000-0000-0000-00000000a304";
	protected static final String RESOURCE_DUMMY_MAGENTA_NAME = "magenta";
	protected static final String RESOURCE_DUMMY_MAGENTA_NAMESPACE = MidPointConstants.NS_RI;

	// Plain iteration (no expressions). Has synchronization block.
	protected static final File RESOURCE_DUMMY_FUCHSIA_FILE = new File(TEST_DIR, "resource-dummy-fuchsia.xml");
	protected static final String RESOURCE_DUMMY_FUCHSIA_OID = "10000000-0000-0000-0000-0000000dd204";
	protected static final String RESOURCE_DUMMY_FUCHSIA_NAME = "fuchsia";
	protected static final String RESOURCE_DUMMY_FUCHSIA_NAMESPACE = MidPointConstants.NS_RI;

	protected static final File TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_FILE = new File(TEST_DIR, "task-dumy-dark-violet-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID = "10000000-0000-0000-5555-555500da0204";
	
	// Iteration with token expression and pre- and post-condition. Sequential suffix.
	protected static final File USER_TEMPLATE_ITERATION_FILE = new File(TEST_DIR, "user-template-iteration.xml");
	protected static final String USER_TEMPLATE_ITERATION_OID = "10000000-0000-0000-0000-0000000d0002";

	// Iteration that generates random suffix. Token expression and post- and pre-conditions.
	protected static final File USER_TEMPLATE_ITERATION_RANDOM_FILE = new File(TEST_DIR, "user-template-iteration-random.xml");
	protected static final String USER_TEMPLATE_ITERATION_RANDOM_OID = "10000000-0000-0000-0000-0000000d0002"; // SAME OID as USER_TEMPLATE_ITERATION
	
	// Iteration with token expression (sequential) and post-condition that checks for e-mail uniquness.
	protected static final File USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_FILE = new File(TEST_DIR, "user-template-iteration-unique-email.xml");
	protected static final String USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_OID = "10000000-0000-0000-0000-0000000d0004";
	
	private static final String USER_ANGELICA_NAME = "angelica";
	private static final String ACCOUNT_SPARROW_NAME = "sparrow";
	
	private static final String USER_DEWATT_NAME = "dewatt";
	private static final String ACCOUNT_DEWATT_NAME = "DeWatt";

	private static final String USER_LARGO_NAME = "largo";
	private static final String ACCOUNT_LARGO_NAME = "largo";
	public static final String ACCOUNT_LARGO_DUMMY_USERNAME = "largo";

	private static final String DESCRIPTION_RUM = "Where's the rum?";

	private static final String USER_JACK_RENAMED_NAME = "cptjack";

	private static final String ACCOUNT_LECHUCK_USERNAME = "lechuck";
	private static final String LECHUCK_FULLNAME = "LeChuck";
	private static final String ACCOUNT_CHARLES_USERNAME = "charles";
	private static final String ACCOUNT_SHINETOP_USERNAME = "shinetop";
	private static final String CHUCKIE_FULLNAME = "Chuckie";
	
	private static final String ACCOUNT_MATUSALEM_USERNAME = "matusalem";
	private static final String ACCOUNT_DIPLOMATICO_USERNAME = "diplomatico";
	private static final String ACCOUNT_MILLONARIO_USERNAME = "millonario";
	private static final String RUM_FULLNAME = "Rum";
	private static final String RON_FULLNAME = "Ron";

	private static final File USER_JUPITER_FILE = new File(TEST_DIR, "user-jupiter.xml");
	private static final String USER_JUPITER_NAME = "jupiter";
	private static final String ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME = "Jupiter Jones";

	private static final File USER_ALFRED_FILE = new File(TEST_DIR, "user-alfred.xml");
	private static final String USER_ALFRED_NAME = "alfred";

	private static final String USER_BOB_NAME = "bob";

	private static final String USER_ALFREDO_FETTUCINI_USERNAME = "afettucini";
	private static final String USER_ALFREDO_FETTUCINI_GIVEN_NAME = "Alfredo";
	private static final String USER_ALFREDO_FETTUCINI_FAMILY_NAME = "Fettucini";

	private static final String USER_BILL_FETTUCINI_USERNAME = "bfettucini";
	private static final String USER_BILL_FETTUCINI_GIVEN_NAME = "Bill";
	private static final String USER_BILL_FETTUCINI_FAMILY_NAME = "Fettucini";

	private static final String USER_FETTUCINI_NICKNAME = "fetty";

	private static final String EMAIL_SUFFIX = "@example.com";

	protected String jupiterUserOid;

	protected static DummyResource dummyResourcePink;
	protected static DummyResourceContoller dummyResourceCtlPink;
	protected ResourceType resourceDummyPinkType;
	protected PrismObject<ResourceType> resourceDummyPink;
	
	protected static DummyResource dummyResourceViolet;
	protected static DummyResourceContoller dummyResourceCtlViolet;
	protected ResourceType resourceDummyVioletType;
	protected PrismObject<ResourceType> resourceDummyViolet;
	
	protected static DummyResource dummyResourceDarkViolet;
	protected static DummyResourceContoller dummyResourceCtlDarkViolet;
	protected ResourceType resourceDummyDarkVioletType;
	protected PrismObject<ResourceType> resourceDummyDarkViolet;
	
	protected static DummyResource dummyResourceMagenta;
	protected static DummyResourceContoller dummyResourceCtlMagenta;
	protected ResourceType resourceDummyMagentaType;
	protected PrismObject<ResourceType> resourceDummyMagenta;

	protected static DummyResource dummyResourceFuchsia;
	protected static DummyResourceContoller dummyResourceCtlFuchsia;
	protected ResourceType resourceDummyFuchsiaType;
	protected PrismObject<ResourceType> resourceDummyFuchsia;

	String iterationTokenDiplomatico;
	String iterationTokenMillonario;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlPink = DummyResourceContoller.create(RESOURCE_DUMMY_PINK_NAME, resourceDummyPink);
		dummyResourceCtlPink.extendSchemaPirate();
		dummyResourcePink = dummyResourceCtlPink.getDummyResource();
		resourceDummyPink = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_PINK_FILE, RESOURCE_DUMMY_PINK_OID, initTask, initResult); 
		resourceDummyPinkType = resourceDummyPink.asObjectable();
		dummyResourceCtlPink.setResource(resourceDummyPink);
		
		dummyResourceCtlViolet = DummyResourceContoller.create(RESOURCE_DUMMY_VIOLET_NAME, resourceDummyViolet);
		dummyResourceCtlViolet.extendSchemaPirate();
		dummyResourceViolet = dummyResourceCtlViolet.getDummyResource();
		resourceDummyViolet = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_VIOLET_FILE, RESOURCE_DUMMY_VIOLET_OID, initTask, initResult); 
		resourceDummyVioletType = resourceDummyViolet.asObjectable();
		dummyResourceCtlViolet.setResource(resourceDummyViolet);
		
		dummyResourceCtlDarkViolet = DummyResourceContoller.create(RESOURCE_DUMMY_DARK_VIOLET_NAME, resourceDummyViolet);
		dummyResourceCtlDarkViolet.extendSchemaPirate();
		dummyResourceDarkViolet = dummyResourceCtlDarkViolet.getDummyResource();
		resourceDummyDarkViolet = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_DARK_VIOLET_FILE, RESOURCE_DUMMY_DARK_VIOLET_OID, initTask, initResult); 
		resourceDummyDarkVioletType = resourceDummyDarkViolet.asObjectable();
		dummyResourceCtlDarkViolet.setResource(resourceDummyDarkViolet);
		dummyResourceDarkViolet.setSyncStyle(DummySyncStyle.SMART);
		
		dummyResourceCtlMagenta = DummyResourceContoller.create(RESOURCE_DUMMY_MAGENTA_NAME, resourceDummyMagenta);
		dummyResourceCtlMagenta.extendSchemaPirate();
		dummyResourceMagenta = dummyResourceCtlMagenta.getDummyResource();
		resourceDummyMagenta = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_MAGENTA_FILE, RESOURCE_DUMMY_MAGENTA_OID, initTask, initResult); 
		resourceDummyMagentaType = resourceDummyMagenta.asObjectable();
		dummyResourceCtlMagenta.setResource(resourceDummyMagenta);

		dummyResourceCtlFuchsia = DummyResourceContoller.create(RESOURCE_DUMMY_FUCHSIA_NAME, resourceDummyFuchsia);
		dummyResourceCtlFuchsia.extendSchemaAd();
		dummyResourceFuchsia = dummyResourceCtlFuchsia.getDummyResource();
		resourceDummyFuchsia = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FUCHSIA_FILE, RESOURCE_DUMMY_FUCHSIA_OID, initTask, initResult);
		resourceDummyFuchsiaType = resourceDummyFuchsia.asObjectable();
		dummyResourceCtlFuchsia.setResource(resourceDummyFuchsia);

		addObject(USER_TEMPLATE_ITERATION_FILE);
		addObject(USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_FILE);

		addObject(USER_LARGO_FILE);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
	}

	/**
	 * The default dummy instance will not iterate. It has correlation rule which will link the account instead.
	 */
	@Test
    public void test100JackAssignAccountDummyConflicting() throws Exception {
		final String TEST_NAME = "test100JackAssignAccountDummyConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Tortuga");
		getDummyResource().addAccount(account);
		repoAddObject(createShadow(getDummyResourceObject(), ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");
        
        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
		
	@Test
    public void test200JackAssignAccountDummyPinkConflicting() throws Exception {
		final String TEST_NAME = "test200JackAssignAccountDummyPinkConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Pinky");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Red Sea");
		dummyResourcePink.addAccount(account);
		repoAddObject(createShadow(resourceDummyPink, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

		// assignment with weapon := 'pistol' (test for
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		AssignmentType assignmentType = createConstructionAssignment(RESOURCE_DUMMY_PINK_OID, ShadowKindType.ACCOUNT, null);
		ConstructionType constructionType = assignmentType.getConstruction();
		ResourceAttributeDefinitionType attributeDefinitionType = new ResourceAttributeDefinitionType();
		attributeDefinitionType.setRef(new ItemPathType(new ItemPath(dummyResourceCtlPink.getAttributeWeaponQName())));
		MappingType mappingType = new MappingType();
		mappingType.setStrength(MappingStrengthType.STRONG);
		ExpressionType expressionType = new ExpressionType();
		expressionType.getExpressionEvaluator().add(new ObjectFactory().createValue(RawType.create("pistol", prismContext)));
		mappingType.setExpression(expressionType);
		attributeDefinitionType.setOutbound(mappingType);
		constructionType.getAttribute().add(attributeDefinitionType);
		modifications.add(createAssignmentModification(assignmentType, true));
		ObjectDelta<UserType> accountAssignmentUserDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);

        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 2);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		
		String accountPinkOid = getLinkRefOid(userJack, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountPinkShadow = repositoryService.getObject(ShadowType.class, accountPinkOid, null, result);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, "jack1", resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, "jack1", resourceDummyPinkType);
		display("accountPinkModel", accountPinkModel);
		PrismAsserts.assertPropertyValue(accountPinkModel, dummyResourceCtlPink.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
				"pistol");

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Pinky", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "jack1", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Test the normal case. Just to be sure the default iteration algorithm works well.
	 */
	@Test
    public void test210GuybrushAssignAccountDummyPink() throws Exception {
		final String TEST_NAME = "test210GuybrushAssignAccountDummyPink";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
                        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_PINK_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 2);
		// Guybrush had dummy account before
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		
		String accountPinkOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountPinkShadow = repositoryService.getObject(ShadowType.class, accountPinkOid, null, result);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyPinkType);
        
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(4);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test220DeWattAssignAccountDummyPinkCaseIgnore() throws Exception {
		final String TEST_NAME = "test220DeWattAssignAccountDummyPinkCaseIgnore";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userDeWatt = createUser(USER_DEWATT_NAME, "Augustus DeWatt", true);
        addObject(userDeWatt);
        String userDeWattkOid = userDeWatt.getOid();
        
        PrismObject<ShadowType> accountDeWatt = createAccount(resourceDummyPink, ACCOUNT_DEWATT_NAME, true);
        addAttributeToShadow(accountDeWatt, resourceDummyPink, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,  "Augustus DeWatt");
        addObject(accountDeWatt);

        // precondition
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_DEWATT_NAME, "Augustus DeWatt", true);
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(userDeWattkOid,
        		RESOURCE_DUMMY_PINK_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        dummyAuditService.clear();
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userDeWattAfter = getUser(userDeWattkOid);
		display("User after change execution", userDeWattAfter);
		assertUser(userDeWattAfter, userDeWattkOid, USER_DEWATT_NAME, "Augustus DeWatt", null, null);
		assertLinks(userDeWattAfter, 1);
		assertAccount(userDeWattAfter, RESOURCE_DUMMY_PINK_OID);
		
		String accountPinkOid = getLinkRefOid(userDeWattAfter, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountPinkShadow = repositoryService.getObject(ShadowType.class, accountPinkOid, null, result);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, USER_DEWATT_NAME+"1", resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, USER_DEWATT_NAME + "1", resourceDummyPinkType);
        
        // Old account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_DEWATT_NAME, "Augustus DeWatt", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_DEWATT_NAME+"1", "Augustus DeWatt", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
	public void test230ScroogeAddAccountDummyConflictingNoShadow() throws Exception {
		final String TEST_NAME = "test230ScroogeAddAccountDummyConflictingNoShadow";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount("scrooge");
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Scrooge Pinky");
		dummyResourcePink.addAccount(account);

		PrismObject<UserType> userScrooge = createUser("scrooge", "Scrooge McDuck", true);
		PrismObject<ShadowType> newPinkyShadow = createShadow(resourceDummyPinkType.asPrismObject(), null, null);
		userScrooge.asObjectable().getLink().add(newPinkyShadow.asObjectable());

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(ObjectDelta.createAddDelta(userScrooge));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userScroogeAfter = findUserByUsername("scrooge");
		display("User after change execution", userScroogeAfter);
		assertUser(userScroogeAfter, null, "scrooge", "Scrooge McDuck", null, null, null);
		String accountOid = getSingleLinkOid(userScroogeAfter);

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		assertAccountShadowRepo(accountShadow, accountOid, "scrooge1", resourceDummyPinkType);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		assertAccountShadowModel(accountModel, accountOid, "scrooge1", resourceDummyPinkType);

		// Check account in dummy resource
		assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "scrooge1", "Scrooge McDuck", true);
	}

	/**
	 * This tests a situation where the ObjectAlreadyExists conflict occurs because of some misconfiguration.
	 * For example, the reason of the conflict is not the naming attribute itself.
	 * @throws Exception
	 */
	@Test
	public void test235HackerAddAccountDummyEternalConflict() throws Exception {
		final String TEST_NAME = "test235HackerAddAccountDummyEternalConflict";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		PrismObject<UserType> userJoeHacker = createUser("hacker", "Joe Hacker", true);
		PrismObject<ShadowType> newPinkyShadow = createShadow(resourceDummyPinkType.asPrismObject(), null, null);
		userJoeHacker.asObjectable().getLink().add(newPinkyShadow.asObjectable());

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(ObjectDelta.createAddDelta(userJoeHacker));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		// wrong behavior is throwing "java.lang.IllegalStateException: Model operation took too many clicks (limit is 30). Is there a cycle?"
		// good behavior is reporting ObjectAlreadyExistsException here
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("Result", result);
		TestUtil.assertPartialError(result);
		String exp = "hacker is forbidden to use as an object name";
		String msg = result.getMessage();
		if (msg == null) {
			msg = "(null)";
		}
		assertTrue("result message is does not contain expected '" + exp + "', instead it is: '" + msg + "'", msg.contains(exp));

		PrismObject<UserType> userHackerAfter = findUserByUsername("hacker");
		display("User after change execution", userHackerAfter);
		assertUser(userHackerAfter, null, "hacker", "Joe Hacker", null, null, null);
		assertNoLinkedAccount(userHackerAfter);

		assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, "hacker");		// just in case ;)
	}

	@Test
	public void test240LargoAssignAccountDummyConflictingNoShadow() throws Exception {
		final String TEST_NAME = "test240LargoAssignAccountDummyConflictingNoShadow";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount(ACCOUNT_LARGO_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Largo Pinky");
		dummyResourcePink.addAccount(account);

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_LARGO_OID, RESOURCE_DUMMY_PINK_OID, null, true);
		deltas.add(accountAssignmentUserDelta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
		assertUserLargo(userLargo);
		String accountOid = getSingleLinkOid(userLargo);

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		assertAccountShadowRepo(accountShadow, accountOid, "largo1", resourceDummyPinkType);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		assertAccountShadowModel(accountModel, accountOid, "largo1", resourceDummyPinkType);

		// Check account in dummy resource
		assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "largo1", null, true);

		// Check audit
		// USER Largo MODIFY(add-assignment):   request + execution (focus(assignment) + account/failed) + execution (focus(linkRef) / account/OK)
		display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(3);
		dummyAuditService.assertSimpleRecordSanity();
		dummyAuditService.assertAnyRequestDeltas();
		dummyAuditService.assertExecutionDeltas(2);
		dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
		dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
		dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Doing the same as test240 (conflict without pre-existing shadow -> surprising the model with AlreadyExists,
	 * causing re-running given wave in model. But this time doing this on resource that has synchronization
	 * defined (fuchsia).
	 *
	 * test260: first case - existing account corresponds to user being created
	 *
	 * 1) manually create account Jupiter Jones (no shadow!), description = "jupiter"
	 * 2) create user Jupiter Jones (name = jupiter)
	 *
	 * Create account operation should fail, account should be synchronized back to repo (creating the user!), and
	 * model should clean it up somehow...
	 */
	@Test
	public void test260JupiterConflictNoShadowSyncBack() throws Exception {
		final String TEST_NAME = "test260JupiterConflictNoShadowSyncBack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount(ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME);		// Jupiter Jones
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, USER_JUPITER_NAME);				// jupiter
		dummyResourceFuchsia.addAccount(account);

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		PrismObject<UserType> userJupiter = PrismTestUtil.parseObject(USER_JUPITER_FILE);
		deltas.add(ObjectDelta.createAddDelta(userJupiter));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);		// jupiter
		display("User after change execution", userJupiterAfter);
		assertUserJupiter(userJupiterAfter);
		jupiterUserOid = userJupiterAfter.getOid();
		String accountOid = getSingleLinkOid(userJupiterAfter);

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		display("Account shadow from repo", accountShadow);
		assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, resourceDummyFuchsiaType);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, resourceDummyFuchsiaType);

		// Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, null, true);

		// TODO Check audit
		display("Audit", dummyAuditService);
//		dummyAuditService.assertRecords(3);
//		dummyAuditService.assertSimpleRecordSanity();
//		dummyAuditService.assertAnyRequestDeltas();
//		dummyAuditService.assertExecutionDeltas(2);
//		dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//		dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//		dummyAuditService.assertExecutionSuccess();
	}

	// remove the assignment, shadow and account to prepare for following tests
	@Test
	public void test262JupiterCleanup() throws Exception {
		final String TEST_NAME = "test262JupiterCleanup";
		cleanUpJupiter(TEST_NAME);
	}

	protected void cleanUpJupiter(String TEST_NAME) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, SchemaViolationException, ConflictException {
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(createAccountAssignmentUserDelta(jupiterUserOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", false));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);		// jupiter
		display("User after change execution", userJupiterAfter);
		assertUserJupiter(userJupiterAfter);

		assertEquals("Unexpected number of linkRefs", 0, userJupiterAfter.asObjectable().getLinkRef().size());
		assertNull("Unexpected account for jupiter", getDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME));

		// TODO Check audit
		display("Audit", dummyAuditService);
//		dummyAuditService.assertRecords(3);
//		dummyAuditService.assertSimpleRecordSanity();
//		dummyAuditService.assertAnyRequestDeltas();
//		dummyAuditService.assertExecutionDeltas(2);
//		dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//		dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//		dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Doing the same as test260. But this time assigns the account in separate step.
	 */
	@Test
	public void test264JupiterConflictNoShadowSyncBackSeparate() throws Exception {
		final String TEST_NAME = "test264JupiterConflictNoShadowSyncBackSeparate";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount(ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME);		// Jupiter Jones
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, USER_JUPITER_NAME);				// jupiter
		dummyResourceFuchsia.addAccount(account);

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(createAccountAssignmentUserDelta(jupiterUserOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", true));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);		// jupiter
		display("User after change execution", userJupiterAfter);
		assertUserJupiter(userJupiterAfter);
		jupiterUserOid = userJupiterAfter.getOid();
		String accountOid = getSingleLinkOid(userJupiterAfter);

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		display("Account shadow from repo", accountShadow);
		assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, resourceDummyFuchsiaType);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, resourceDummyFuchsiaType);

		// Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, null, true);

		// TODO Check audit
		display("Audit", dummyAuditService);
//		dummyAuditService.assertRecords(3);
//		dummyAuditService.assertSimpleRecordSanity();
//		dummyAuditService.assertAnyRequestDeltas();
//		dummyAuditService.assertExecutionDeltas(2);
//		dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//		dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//		dummyAuditService.assertExecutionSuccess();
	}

	// remove the assignment, shadow and account to prepare for following tests
	@Test
	public void test266JupiterCleanupAgain() throws Exception {
		final String TEST_NAME = "test266JupiterCleanupAgain";
		cleanUpJupiter(TEST_NAME);
	}

	/**
	 * Doing the same as test264, but the conflicting account does not belong to the user being created
	 * (and causes another user to be added).
	 */
	@Test
	public void test270JupiterConflictOtherNoShadowSyncBack() throws Exception {
		final String TEST_NAME = "test270JupiterConflictOtherNoShadowSyncBack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount(ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME);		// Jupiter Jones
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "jupiter0");		// different from our jupiter
		dummyResourceFuchsia.addAccount(account);

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(createAccountAssignmentUserDelta(jupiterUserOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", true));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);		// jupiter
		display("User after change execution", userJupiterAfter);
		assertUserJupiter(userJupiterAfter);
		jupiterUserOid = userJupiterAfter.getOid();
		String accountOid = getSingleLinkOid(userJupiterAfter);

		// Check shadow & account
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		display("Account shadow from repo", accountShadow);
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		display("Account shadow from model", accountModel);

		assertAccountShadowRepo(accountShadow, accountOid, "Jupiter Jones1", resourceDummyFuchsiaType);
		assertAccountShadowModel(accountModel, accountOid, "Jupiter Jones1", resourceDummyFuchsiaType);

		// Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones1", null, true);
		assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones1", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "jupiter");

		// Now check the other (newly created) user, jupiter0
		PrismObject<UserType> userJupiter0 = findUserByUsername("jupiter0");
		display("Newly created jupiter0 user", userJupiter0);
		assertUser(userJupiter0, null, "jupiter0", "Jupiter Jones", null, null, null);
		String accountOidJ0 = getSingleLinkOid(userJupiter0);

		// Check shadow
		PrismObject<ShadowType> accountShadowJ0 = repositoryService.getObject(ShadowType.class, accountOidJ0, null, result);
		display("Account shadow from repo (jupiter0)", accountShadowJ0);
		assertAccountShadowRepo(accountShadowJ0, accountOidJ0, "Jupiter Jones", resourceDummyFuchsiaType);

		// Check account
		PrismObject<ShadowType> accountModelJ0 = modelService.getObject(ShadowType.class, accountOidJ0, null, task, result);
		assertAccountShadowModel(accountModelJ0, accountOidJ0, "Jupiter Jones", resourceDummyFuchsiaType);

		// Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones", null, true);
		assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "jupiter0");

		// TODO Check audit
		display("Audit", dummyAuditService);
//		dummyAuditService.assertRecords(3);
//		dummyAuditService.assertSimpleRecordSanity();
//		dummyAuditService.assertAnyRequestDeltas();
//		dummyAuditService.assertExecutionDeltas(2);
//		dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//		dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//		dummyAuditService.assertExecutionSuccess();
	}

	private void assertUserJupiter(PrismObject<UserType> user) {
		assertUser(user, null, USER_JUPITER_NAME, "Jupiter Jones", "Jupiter", "Jones", null);
	}

	/**
	 * Same as test240 (conflict with no shadow), but including rename operation.
	 */
	@Test
	public void test280RenameBobNoShadow() throws Exception {
		final String TEST_NAME = "test280RenameBobNoShadow";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBob = createUser(USER_BOB_NAME, "Bob Andrews", true);
		addObject(userBob);
		String userBobOid = userBob.getOid();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount("bobby");
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Bobby Pinky");
		dummyResourcePink.addAccount(account);

		// preconditions
		assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_BOB_NAME);
		assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bobby", "Bobby Pinky", true);

		// prepare change

		ObjectDelta<UserType> objectDelta = createAccountAssignmentUserDelta(userBobOid, RESOURCE_DUMMY_PINK_OID, "default", true);
		objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_NAME, new PolyString("bobby")));	// will conflict with Bobby Pinky
		objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_FULL_NAME, new PolyString("Bobby Andrews")));

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(objectDelta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismObject<UserType> userBobAfter = getUser(userBobOid);
		display("User after change execution", userBobAfter);
		assertUser(userBobAfter, userBobOid, "bobby", "Bobby Andrews", null, null, null);
		String accountOid = getSingleLinkOid(userBobAfter);

		// Check shadow & account
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		display("Account shadow from repo", accountShadow);
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		display("Account shadow from model", accountModel);

		assertAccountShadowRepo(accountShadow, accountOid, "bobby1", resourceDummyPinkType);
		assertAccountShadowModel(accountModel, accountOid, "bobby1", resourceDummyPinkType);

		// THEN
		assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bobby", "Bobby Pinky", true);
		assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bobby1", "Bobby Andrews", true);
		assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bob");
	}

	/**
	 * Same as test280 (conflict with no shadow with rename), but including synchronization.
	 */
	@Test
	public void test282RenamePeterNoShadowSync() throws Exception {
		final String TEST_NAME = "test282RenamePeterNoShadowSync";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userPeter = createUser("peter", "Peter Crenshaw", true);
		addObject(userPeter);
		String userPeterOid = userPeter.getOid();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount("Pete Crenshaw");				// will conflict after rename
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "pete0");
		dummyResourceFuchsia.addAccount(account);

		// preconditions
		assertNoDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Peter Crenshaw");
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw", null, true);		// conflicting account (pete0)

		// prepare change

		ObjectDelta<UserType> objectDelta = createAccountAssignmentUserDelta(userPeterOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", true);
		objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_NAME, new PolyString("pete")));
		objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_FULL_NAME, new PolyString("Pete Crenshaw")));

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(objectDelta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismObject<UserType> userPeteAfter = getUser(userPeterOid);
		display("User after change execution", userPeteAfter);
		assertUser(userPeteAfter, userPeterOid, "pete", "Pete Crenshaw", null, null, null);
		String accountOid = getSingleLinkOid(userPeteAfter);

		// Check shadow & account
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		display("Account shadow from repo", accountShadow);
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		display("Account shadow from model", accountModel);

		assertAccountShadowRepo(accountShadow, accountOid, "Pete Crenshaw1", resourceDummyFuchsiaType);
		assertAccountShadowModel(accountModel, accountOid, "Pete Crenshaw1", resourceDummyFuchsiaType);

		// Now check the other (newly created) user, pete0
		PrismObject<UserType> userPete0 = findUserByUsername("pete0");
		display("Newly created pete0 user", userPete0);
		assertUser(userPete0, null, "pete0", "Pete Crenshaw", null, null, null);
		String accountOidP0 = getSingleLinkOid(userPete0);

		// Check shadow
		PrismObject<ShadowType> accountShadowP0 = repositoryService.getObject(ShadowType.class, accountOidP0, null, result);
		display("Account shadow from repo (pete0)", accountShadowP0);
		assertAccountShadowRepo(accountShadowP0, accountOidP0, "Pete Crenshaw", resourceDummyFuchsiaType);

		// Check account
		PrismObject<ShadowType> accountModelP0 = modelService.getObject(ShadowType.class, accountOidP0, null, task, result);
		display("Account shadow from model (pete0)", accountModelP0);
		assertAccountShadowModel(accountModelP0, accountOidP0, "Pete Crenshaw", resourceDummyFuchsiaType);

		// accounts on the resource
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw", null, true);			// pete0
		assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "pete0");
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw1", null, true);			// pete
		assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw1", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "pete");
		assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, "peter");
	}

	// as with jupiter, but ADD instead of ASSIGN account
	@Test
	public void test290AlfredConflictNoShadowSyncBackAdd() throws Exception {
		final String TEST_NAME = "test290AlfredConflictNoShadowSyncBackAdd";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// Make sure there is a conflicting account and NO shadow for it
		DummyAccount account = new DummyAccount("Alfred Hitchcock");
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "alfred");
		dummyResourceFuchsia.addAccount(account);

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		PrismObject<UserType> userJupiter = PrismTestUtil.parseObject(USER_ALFRED_FILE);			// there is a linked account
		deltas.add(ObjectDelta.createAddDelta(userJupiter));

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAlfredAfter = findUserByUsername(USER_ALFRED_NAME);		// alfred
		display("User after change execution", userAlfredAfter);
		assertUser(userAlfredAfter, null, USER_ALFRED_NAME, "Alfred Hitchcock", "Alfred", "Hitchcock", null);
		String accountOid = getSingleLinkOid(userAlfredAfter);

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		display("Account shadow from repo", accountShadow);
		assertAccountShadowRepo(accountShadow, accountOid, "Alfred Hitchcock", resourceDummyFuchsiaType);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
		assertAccountShadowModel(accountModel, accountOid, "Alfred Hitchcock", resourceDummyFuchsiaType);

		// Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
		assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Alfred Hitchcock", null, true);
	}



	@Test
    public void test300JackAssignAccountDummyVioletConflicting() throws Exception {
		final String TEST_NAME = "test300JackAssignAccountDummyVioletConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Violet");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Sea of Lavender");
		dummyResourceViolet.addAccount(account);
		repoAddObject(createShadow(resourceDummyViolet, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 3);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
		
		String accountVioletOid = getLinkRefOid(userJack, RESOURCE_DUMMY_VIOLET_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountVioletShadow = repositoryService.getObject(ShadowType.class, accountVioletOid, null, result);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "jack.1", resourceDummyVioletType);
        
        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "jack.1", resourceDummyVioletType);
        
        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "jack.1", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test350GuybrushAssignAccountDummyViolet() throws Exception {
		final String TEST_NAME = "test350GuybrushAssignAccountDummyViolet";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 3);
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
		
		String accountVioletOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountVioletShadow = repositoryService.getObject(ShadowType.class, accountVioletOid, null, result);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "guybrush.3", resourceDummyVioletType);
        
        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "guybrush.3", resourceDummyVioletType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test360HermanAssignAccountDummyViolet() throws Exception {
		final String TEST_NAME = "test360HermanAssignAccountDummyViolet";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        addObject(USER_HERMAN_FILE);
        
        dummyAuditService.clear();
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_HERMAN_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userHerman = getUser(USER_HERMAN_OID);
		display("User after change execution", userHerman);
		assertUser(userHerman, USER_HERMAN_OID, "herman", "Herman Toothrot", "Herman", "Toothrot");
		assertLinks(userHerman, 1);
		assertAccount(userHerman, RESOURCE_DUMMY_VIOLET_OID);
		
		String accountVioletOid = getLinkRefOid(userHerman, RESOURCE_DUMMY_VIOLET_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountVioletShadow = repositoryService.getObject(ShadowType.class, accountVioletOid, null, result);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "herman.1", resourceDummyVioletType);
        
        assertIteration(accountVioletShadow, 1, ".1");
        
        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "herman.1", resourceDummyVioletType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "herman");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "herman.1", "Herman Toothrot", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test400RenameAngelicaConflicting() throws Exception {
		final String TEST_NAME = "test400RenameAngelicaConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userLechuck = createUser(USER_ANGELICA_NAME, "Angelica", true);
        userLechuck.asObjectable().getAssignment().add(createAccountAssignment(RESOURCE_DUMMY_PINK_OID, null));
        addObject(userLechuck);
        String userLechuckOid = userLechuck.getOid();
        
        PrismObject<ShadowType> accountCharles = createAccount(resourceDummyPink, ACCOUNT_SPARROW_NAME, true);
        addObject(accountCharles);
        
        // preconditions
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_ANGELICA_NAME, "Angelica", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME, null, true);
        
        // WHEN
        modifyUserReplace(userLechuckOid, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString(ACCOUNT_SPARROW_NAME));
        
        // THEN
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME, null, true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME+"1", "Angelica", true);
        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_ANGELICA_NAME);
	}
	
	/**
	 * No conflict. Just make sure the iteration condition is not triggered.
	 */
	@Test
    public void test500JackAssignAccountDummyMagenta() throws Exception {
		final String TEST_NAME = "test500JackAssignAccountDummyMagenta";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 4);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userJack, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userJack, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "jack", resourceDummyMagentaType);
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "jack", resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 0, "");
        
        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "jack.1", "Jack Sparrow", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "jack", "Jack Sparrow", true);
        
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Conflict on quote attribute
	 */
	@Test
    public void test510DrakeAssignAccountDummyMagenta() throws Exception {
		final String TEST_NAME = "test510DrakeAssignAccountDummyMagenta";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userDrake = PrismTestUtil.parseObject(USER_DRAKE_FILE);
        userDrake.asObjectable().setDescription(DESCRIPTION_RUM);
        userDrake.asObjectable().setLocality(PrismTestUtil.createPolyStringType("Jamaica"));
        addObject(userDrake);
        
        dummyAuditService.clear();
        
        // Make sure there are some dummy accounts without quote. So if the code tries to search for null
        // it will get something and the test fails
        dummyResourceCtlMagenta.addAccount("afettucini", "Alfredo Fettucini");
        dummyResourceCtlMagenta.addAccount("bfettucini", "Bill Fettucini");
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_DRAKE_OID, 
        		RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
		display("User after change execution", userDrakeAfter);
		assertUser(userDrakeAfter, USER_DRAKE_OID, "drake", "Francis Drake", "Fancis", "Drake");
		assertLinks(userDrakeAfter, 1);
		assertAccount(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 1, "001");
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", "Francis Drake", true);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Francis Drake");
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Jamaica");
        
        PrismAsserts.assertPropertyValue(userDrakeAfter, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM + " -- Francis Drake"));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/*
	 * Modify a property that has nothing to do with iteration
	 */
	@Test
    public void test520DrakeModifyLocality() throws Exception {
		final String TEST_NAME = "test520DrakeModifyLocality";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();        
        dummyAuditService.clear();
                                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_DRAKE_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("London"));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
		display("User after change execution", userDrakeAfter);
		assertUser(userDrakeAfter, USER_DRAKE_OID, "drake", "Francis Drake", "Fancis", "Drake");
		assertLinks(userDrakeAfter, 1);
		assertAccount(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 1, "001");
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", "Francis Drake", true);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "London");
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Francis Drake");
        
        PrismAsserts.assertPropertyValue(userDrakeAfter, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM + " -- Francis Drake"));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Nothing special in this test. Just plain assignment. No conflicts. It just prepares the ground for the next
	 * test and also tests the normal case.
	 */
	@Test
    public void test530GuybrushAssignAccountDummyMagenta() throws Exception {
		final String TEST_NAME = "test530GuybrushAssignAccountDummyMagenta";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 4);
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 0, "");
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // old account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
                
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Change Guybrushe's description so it conflicts with Jack's description in magenta resource.
	 * As the iterator is also bound to the account identifier (ICF NAME) the guybrushe's account will
	 * also be renamed.
	 */
	@Test
    public void test532GuybrushModifyDescription() throws Exception {
		final String TEST_NAME = "test532GuybrushModifyDescription";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, task, result, DESCRIPTION_RUM);
                  		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 4);
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", resourceDummyMagentaType);
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // Renamed
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME+ "001", "Guybrush Threepwood", true);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME+ "001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Guybrush Threepwood");
        
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM + " -- Guybrush Threepwood"));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test600JackRename() throws Exception {
		final String TEST_NAME = "test600JackRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result, 
        		PrismTestUtil.createPolyString(USER_JACK_RENAMED_NAME));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, USER_JACK_RENAMED_NAME, "Jack Sparrow", "Jack", "Sparrow", "Caribbean");
		assertLinks(userJack, 4);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userJack, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userJack, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, USER_JACK_RENAMED_NAME, resourceDummyMagentaType);
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, USER_JACK_RENAMED_NAME, resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 0, "");
        
        assertDefaultDummyAccount(USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(5);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test700DarkVioletSyncTask() throws Exception {
		final String TEST_NAME = "test700DarkVioletSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_FILE);
        
        // THEN
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, false);
	}

	/*
	 * Create account with fullname LeChuck. User with name LeChuck should be created (no conflict yet).
	 */
	@Test
    public void test710DarkVioletAddLeChuck() throws Exception {
		final String TEST_NAME = "test710DarkVioletAddLeChuck";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_LECHUCK_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
	}
	
	/*
	 * Create account with fullname LeChuck. User with name LeChuck.1 should be created (conflict).
	 */
	@Test
    public void test712DarkVioletAddCharles() throws Exception {
		final String TEST_NAME = "test712DarkVioletAddCharles";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_CHARLES_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
	}
	
	/*
	 * Create account with fullname LeChuck. User with name LeChuck.2 should be created (second conflict).
	 */
	@Test
    public void test714DarkVioletAddShinetop() throws Exception {
		final String TEST_NAME = "test714DarkVioletAddShinetop";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_SHINETOP_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2", "Melee Island");
	}

	@Test
    public void test716DarkVioletDeleteCharles() throws Exception {
		final String TEST_NAME = "test716DarkVioletDeleteCharles";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		dummyResourceDarkViolet.deleteAccountByName(ACCOUNT_CHARLES_USERNAME);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2", "Melee Island");
	}
	
	@Test
    public void test720DarkVioletModifyShinetopLocation() throws Exception {
		final String TEST_NAME = "test720DarkVioletModifyShinetopLocation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_SHINETOP_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2", "Monkey Island");
	}
	
	@Test
    public void test722DarkVioletModifyShinetopFullName() throws Exception {
		final String TEST_NAME = "test722DarkVioletModifyShinetopFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_SHINETOP_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, CHUCKIE_FULLNAME);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, CHUCKIE_FULLNAME, CHUCKIE_FULLNAME, "Monkey Island");
		assertNoUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2");
	}
	
	/*
	 * Create account with fullname barbossa. But user barbossa already exists.
	 *  User with name barbossa.1 should be created (conflict).
	 */
	@Test
    public void test730DarkVioletAddBarbossa() throws Exception {
		final String TEST_NAME = "test730DarkVioletAddBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(USER_BARBOSSA_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_BARBOSSA_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME+".1");
	}
	
	/*
	 * Create yet another account with fullname barbossa. We already have two barbossa users,
	 * so the next one is barbossa.2. But there is a post-iteration condition that refuses that
	 * name. It also refuses barbossa.3. So the result should be barbossa.4.
	 * MID-3338
	 */
	@Test
    public void test732DarkVioletAddBarbossa() throws Exception {
		final String TEST_NAME = "test732DarkVioletAddBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount("YA" + USER_BARBOSSA_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_BARBOSSA_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME+".1");
		assertUserNick("YA" + USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME+".4");
	}
	
	@Test
    public void test750DarkVioletAddMatusalem() throws Exception {
		final String TEST_NAME = "test750DarkVioletAddMatusalem";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // IMPORTANT! Change of user template!
        deleteObject(ObjectTemplateType.class, USER_TEMPLATE_ITERATION_OID, task, result);
        addObject(USER_TEMPLATE_ITERATION_RANDOM_FILE);
        
        DummyAccount account = new DummyAccount(ACCOUNT_MATUSALEM_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
	}
	
	/*
	 * Create account with fullname Rum. User with name Rum.xxx should be created (conflict).
	 */
	@Test
    public void test752DarkVioletAddDiplomatico() throws Exception {
		final String TEST_NAME = "test752DarkVioletAddDiplomatico";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_DIPLOMATICO_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		
		iterationTokenDiplomatico = lookupIterationTokenByAdditionalName(ACCOUNT_DIPLOMATICO_USERNAME);
		assertUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
	}

	/*
	 * Create account with fullname Rum. User with name Rum.yyy should be created (second conflict).
	 */
	@Test
    public void test754DarkVioletAddMilionario() throws Exception {
		final String TEST_NAME = "test754DarkVioletAddMilionario";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_MILLONARIO_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Peru");
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		
		iterationTokenMillonario = lookupIterationTokenByAdditionalName(ACCOUNT_MILLONARIO_USERNAME);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario, "Peru");
	}
	
	@Test
    public void test756DarkVioletDeleteDiplomatico() throws Exception {
		final String TEST_NAME = "test756DarkVioletDeleteDiplomatico";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		dummyResourceDarkViolet.deleteAccountByName(ACCOUNT_DIPLOMATICO_USERNAME);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario, "Peru");
	}
	
	@Test
    public void test760DarkVioletModifyMillonarioLocation() throws Exception {
		final String TEST_NAME = "test760DarkVioletModifyMillonarioLocation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_MILLONARIO_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Northern Peru");
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario, "Northern Peru");
	}
	
	/**
	 * Rename to an identifier that is free. Empty iterationToken is expected.
	 */
	@Test
    public void test762DarkVioletModifyMillonarioFullName() throws Exception {
		final String TEST_NAME = "test762DarkVioletModifyMillonarioFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_MILLONARIO_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RON_FULLNAME);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RON_FULLNAME, RON_FULLNAME, "Northern Peru");
		assertNoUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario);
	}
	
	/**
	 * Rename to an identifier that is taken. New random iterationToken is expected.
	 */	
	@Test
    public void test764DarkVioletModifyMatusalemFullName() throws Exception {
		final String TEST_NAME = "test764DarkVioletModifyMatusalemFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_MATUSALEM_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RON_FULLNAME);
		
		waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertNoUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		String iterationTokenMatusalem = lookupIterationTokenByAdditionalName(ACCOUNT_MATUSALEM_USERNAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RON_FULLNAME, RON_FULLNAME+iterationTokenMatusalem);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RON_FULLNAME, RON_FULLNAME, "Northern Peru");
		assertNoUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario);
	}
	
	
	/**
	 * MID-2887
	 */
	@Test
    public void test800UniqeEmailAddUserAlfredoFettucini() throws Exception {
		final String TEST_NAME = "test800UniqeEmailAddUserAlfredoFettucini";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_OID);
        
        PrismObject<UserType> user = createUser(USER_ALFREDO_FETTUCINI_USERNAME, USER_ALFREDO_FETTUCINI_GIVEN_NAME, USER_ALFREDO_FETTUCINI_FAMILY_NAME,
        		USER_FETTUCINI_NICKNAME, true);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(user, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(user.getOid());
		display("User after change execution", userAfter);
		assertUser(userAfter, user.getOid(), USER_ALFREDO_FETTUCINI_USERNAME, 
				USER_ALFREDO_FETTUCINI_GIVEN_NAME + " " + USER_ALFREDO_FETTUCINI_FAMILY_NAME,
				USER_ALFREDO_FETTUCINI_GIVEN_NAME, USER_ALFREDO_FETTUCINI_FAMILY_NAME);
	
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" nickname", USER_FETTUCINI_NICKNAME, userAfter.asObjectable().getNickName());
		
		assertEquals("Wrong "+user+" emailAddress", USER_FETTUCINI_NICKNAME + EMAIL_SUFFIX, userAfter.asObjectable().getEmailAddress());
	}
	
	/**
	 * MID-2887
	 */
	@Test
    public void test802UniqeEmailAddUserBillFettucini() throws Exception {
		final String TEST_NAME = "test802UniqeEmailAddUserBillFettucini";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_OID);
        
        PrismObject<UserType> user = createUser(USER_BILL_FETTUCINI_USERNAME, USER_BILL_FETTUCINI_GIVEN_NAME, 
        		USER_BILL_FETTUCINI_FAMILY_NAME, USER_FETTUCINI_NICKNAME, true);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(user, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(user.getOid());
		display("User after change execution", userAfter);
		assertUser(userAfter, user.getOid(), USER_BILL_FETTUCINI_USERNAME, 
				USER_BILL_FETTUCINI_GIVEN_NAME + " " + USER_BILL_FETTUCINI_FAMILY_NAME,
				USER_BILL_FETTUCINI_GIVEN_NAME, USER_BILL_FETTUCINI_FAMILY_NAME);
	
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" nickname", USER_FETTUCINI_NICKNAME, userAfter.asObjectable().getNickName());
		
		assertEquals("Wrong "+user+" emailAddress", USER_FETTUCINI_NICKNAME + ".1" + EMAIL_SUFFIX, userAfter.asObjectable().getEmailAddress());
	}
	

	private PrismObject<UserType> createUser(String username, String givenName,
			String familyName, String nickname, boolean enabled) throws SchemaException {
		PrismObject<UserType> user = createUser(username, givenName, familyName, enabled);
		user.asObjectable().setNickName(PrismTestUtil.createPolyStringType(nickname));
		return user;
	}

	private void assertUserLargo(PrismObject<UserType> userLargo) {
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_NAME, null, "Largo", "LaGrande", null);
	}

	private void assertUserNick(String accountName, String accountFullName, String expectedUserName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		assertUserNick(accountName, accountFullName, expectedUserName, null);
	}
	
	private void assertUserNick(String accountName, String accountFullName, String expectedUserName, String expectedLocality) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<UserType> user = findUserByUsername(expectedUserName);
		assertNotNull("No user for "+accountName+" ("+expectedUserName+")", user);
		display("Created user for "+accountName, user);
		assertEquals("Wrong nickname in user created for "+accountName, accountFullName, user.asObjectable().getNickName().getOrig());
		assertEquals("Wrong additionalName in user created for "+accountName, accountName, user.asObjectable().getAdditionalName().getOrig());
		PolyStringType locality = user.asObjectable().getLocality();
		if (locality == null) {
			assertEquals("Wrong locality in user created for "+accountName, expectedLocality, null);
		} else {
			assertEquals("Wrong locality in user created for "+accountName, expectedLocality, locality.getOrig());
		}
	}
	
	private void assertNoUserNick(String accountName, String accountFullName, String expectedUserName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<UserType> user = findUserByUsername(expectedUserName);
		display("User for "+accountName, user);
		assertNull("User for "+accountName+" ("+expectedUserName+") exists but it should be gone", user);
	}
	
	private String lookupIterationTokenByAdditionalName(String additionalName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + ".lookupIterationTokenByAdditionalName");
        OperationResult result = task.getResult();
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_ADDITIONAL_NAME).eq(PrismTestUtil.createPolyString(additionalName))
				.build();
		List<PrismObject<UserType>> objects = modelService.searchObjects(UserType.class, query, null, task, result);
		if (objects.isEmpty()) {
			return null;
		}
		assert objects.size() == 1 : "Too many objects found for additional name "+additionalName+": "+objects;
		PrismObject<UserType> user = objects.iterator().next();
		return user.asObjectable().getIterationToken();
	}
}
