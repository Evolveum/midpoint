package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2013 Evolveum
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


import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTrafo extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "trafo");

	public static final String NS_TRAFO_EXT = "http://midpoint.evolveum.com/xml/ns/story/trafo/ext";
	private static final File TRAFO_SCHEMA_EXTENSION_FILE = new File(TEST_DIR, "extension.xsd");
	private static final QName TRAFO_EXTENSION_HOMEDIR_QNAME = new QName(NS_TRAFO_EXT, "homedir");
	private static final QName TRAFO_EXTENSION_UID_QNAME = new QName(NS_TRAFO_EXT, "uid");
	private static final ItemPath TRAFO_EXTENSION_HOMEDIR_PATH = new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME);

	private static final String TRAFO_MAIL_DOMAIN = "trafo.xx";

	protected static final File USER_ANGELICA_FILE = new File(TEST_DIR, "user-angelica.xml");
	protected static final String USER_ANGELICA_OID = "c0c010c0-d34d-b33f-f00d-11111111aaaa";
	protected static final String USER_ANGELICA_USERNAME = "angelica";

	protected static final File USER_SMITH111_FILE = new File(TEST_DIR, "user-smith-111.xml");
	protected static final String USER_SMITH111_OID = "c0c010c0-d34d-b33f-f00d-555555551111";
	protected static final String USER_SMITH111_USERNAME = "smith111";

	protected static final File USER_SMITH222_FILE = new File(TEST_DIR, "user-smith-222.xml");
	protected static final String USER_SMITH222_OID = "c0c010c0-d34d-b33f-f00d-555555552222";
	protected static final String USER_SMITH222_USERNAME = "smith222";

	protected static final String ACCOUNT_JACK_AD_DN = "CN=Sparrow Jack,OU=People,O=Trafo";
	protected static final String ACCOUNT_JACK_AD_SAM_NAME = "jsparrow";

	protected static final String ACCOUNT_JACK_MAIL_USERNAME = "Jack Sparrow/TRAFO/XX";

	private static final String ACCOUNT_ANGELICA_AD_DN = "CN=Sparrow Jack2,OU=People,O=Trafo";
	protected static final String ACCOUNT_ANGELICA_AD_SAM_NAME = "jsparrow2";

	protected static final String ACCOUNT_ANGELICA_MAIL_USERNAME = "Jack Sparrow2/TRAFO/XX";

	private static final String ACCOUNT_SMITH111_AD_DN = "CN=Smith John,OU=People,O=Trafo";
	private static final Object ACCOUNT_SMITH111_AD_SAM_NAME = USER_SMITH111_USERNAME;
	private static final String ACCOUNT_SMITH111_AD_DN_AFTER_RENAME = "CN=Smither John,OU=People,O=Trafo";

	private static final String ACCOUNT_SMITH111_MAIL_USERNAME = "John Smith/111/TRAFO/XX";
	private static final String ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME = "John Smither/111/TRAFO/XX";

	private static final String ACCOUNT_SMITH222_AD_DN = "CN=Smith John2,OU=People,O=Trafo";
	private static final Object ACCOUNT_SMITH222_AD_SAM_NAME = USER_SMITH222_USERNAME;
	private static final String ACCOUNT_SMITH222_AD_DN_AFTER_RENAME = "CN=Smither John2,OU=People,O=Trafo";

	private static final String ACCOUNT_SMITH222_MAIL_USERNAME = "John Smith/222/TRAFO/XX";
	private static final String ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME = "John Smither/222/TRAFO/XX";

	protected static final File ROLE_EMPLOYEE_FILE = new File(TEST_DIR, "role-employee.xml");
	protected static final String ROLE_EMPLOYEE_OID = "6de5ff6a-5b61-11e3-adc5-001e8c717e5b";

	protected static final File RESOURCE_DUMMY_AD_FILE = new File(TEST_DIR, "resource-dummy-ad.xml");
	protected static final String RESOURCE_DUMMY_AD_ID = "AD";
	protected static final String RESOURCE_DUMMY_AD_OID = "14400000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_DUMMY_AD_NAMESPACE = MidPointConstants.NS_RI;

	protected static final File RESOURCE_DUMMY_MAIL_FILE = new File(TEST_DIR, "resource-dummy-mail.xml");
	protected static final String RESOURCE_DUMMY_MAIL_ID = "mail";
	protected static final String RESOURCE_DUMMY_MAIL_OID = "14400000-0000-0000-0000-000000000001";
	protected static final String RESOURCE_DUMMY_MAIL_NAMESPACE = MidPointConstants.NS_RI;

	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME = "FirstName";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME = "LastName";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME = "ShortName";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME = "idFile";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_TEMPLATE_NAME_NAME = "MailTemplateName";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME = "MailFile";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_SYSTEM_NAME = "MailSystem";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME = "MailDomain";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_SERVER_NAME = "MailServer";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME = "InternetAddress";

	protected static DummyResource dummyResourceAd;
	protected static DummyResourceContoller dummyResourceCtlAd;
	protected ResourceType resourceDummyAdType;
	protected PrismObject<ResourceType> resourceDummyAd;

	protected static DummyResource dummyResourceMail;
	protected static DummyResourceContoller dummyResourceCtlMail;
	protected ResourceType resourceDummyMailType;
	protected PrismObject<ResourceType> resourceDummyMail;

	private String jackAdIcfUid;
	private String jackMailIcfUid;

	private String angelicaAdIcfUid;

	private String smith111MailIcfUid;

	private String angelicaMailIcfUid;

	private String smith111AdIcfUid;

	private String smith222AdIcfUid;

	private String smith222MailIcfUid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// Roles
		repoAddObjectFromFile(ROLE_EMPLOYEE_FILE, initResult);

		// Resources
		dummyResourceCtlAd = DummyResourceContoller.create(RESOURCE_DUMMY_AD_ID, resourceDummyAd);
		dummyResourceCtlAd.extendSchemaAd();
		dummyResourceAd = dummyResourceCtlAd.getDummyResource();
		resourceDummyAd = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_AD_FILE, RESOURCE_DUMMY_AD_OID, initTask, initResult);
		resourceDummyAdType = resourceDummyAd.asObjectable();
		dummyResourceCtlAd.setResource(resourceDummyAd);

		dummyResourceCtlMail = DummyResourceContoller.create(RESOURCE_DUMMY_MAIL_ID, resourceDummyMail);
		dummyResourceCtlMail.populateWithDefaultSchema();
		DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtlMail.getDummyResource().getAccountObjectClass();
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, String.class, false, true);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_TEMPLATE_NAME_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_SYSTEM_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_SERVER_NAME, String.class, false, false);
		dummyResourceCtlMail.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, String.class, false, false);
		dummyResourceMail = dummyResourceCtlMail.getDummyResource();
		resourceDummyMail = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_MAIL_FILE, RESOURCE_DUMMY_MAIL_OID, initTask, initResult);
		resourceDummyMailType = resourceDummyMail.asObjectable();
		dummyResourceCtlMail.setResource(resourceDummyMail);

	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

        OperationResult testResultAd = modelService.testResource(RESOURCE_DUMMY_AD_OID, task);
        TestUtil.assertSuccess(testResultAd);

        OperationResult testResultMail = modelService.testResource(RESOURCE_DUMMY_MAIL_OID, task);
        TestUtil.assertSuccess(testResultMail);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
	}

	@Test
    public void test100JackAssignAccountAd() throws Exception {
		final String TEST_NAME = "test100JackAssignAccountAd";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_AD_OID, null, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkRef(userJack).getOid();

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("AD shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountModel);

        jackAdIcfUid = getIcfUid(accountModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, jackAdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_JACK_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_JACK_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Sparrow");
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME, "555-1234");

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
	 * Account stays. Should be disabled.
	 */
	@Test
    public void test105JackUnAssignAccountAd() throws Exception {
		final String TEST_NAME = "test105JackUnAssignAccountAd";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_AD_OID, null, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkRef(userJack).getOid();

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("AD shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusDisabled(accountModel);

        assertEquals("Jack AD ICF UID has changed", jackAdIcfUid, getIcfUid(accountModel));

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, jackAdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN, false);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_JACK_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_JACK_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Sparrow");
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME, "555-1234");

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
	 * "Wait" a bit and the account should be gone.
	 */
	@Test
    public void test109JackAccountAdGone() throws Exception {
		final String TEST_NAME = "test109JackAccountAdGone";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        clock.overrideDuration("P2D");
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 0);

        // Check account in dummy resource
        assertNoDummyAccountById(RESOURCE_DUMMY_AD_ID, jackAdIcfUid);
        assertNoDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test120AJackssignAccountMail() throws Exception {
		final String TEST_NAME = "test120JackAssignAccountMail";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_MAIL_OID, null, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkRef(userJack).getOid();

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Mail shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountModel);

        jackMailIcfUid = getIcfUid(accountModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, jackMailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Sparrow");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsparrow.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, USER_JACK_USERNAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\"+USER_JACK_USERNAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userJack, UserType.F_EMAIL_ADDRESS, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		PrismAsserts.assertPropertyValue(userJack,
 				new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "c:\\install\\test-id-folder");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        // Inbound
        dummyAuditService.assertExecutionDeltas(1,1);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test125JackUnAssignAccountMail() throws Exception {
		final String TEST_NAME = "test125JackUnAssignAccountMail";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_MAIL_OID, null, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkRef(userJack).getOid();

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Mail shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusDisabled(accountModel);

        assertEquals("Jack Mail ICF UID has changed", jackMailIcfUid, getIcfUid(accountModel));

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, jackMailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME, false);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Sparrow");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsparrow.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, USER_JACK_USERNAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\"+USER_JACK_USERNAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userJack, UserType.F_EMAIL_ADDRESS, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		PrismAsserts.assertPropertyValue(userJack,
 				new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "c:\\install\\test-id-folder");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * "Wait" a bit and the account should be gone.
	 */
	@Test
    public void test129JackAccountMailGone() throws Exception {
		final String TEST_NAME = "test129JackAccountMailGone";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        clock.overrideDuration("P2D");
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 0);

        // Check account in dummy resource
        assertNoDummyAccountById(RESOURCE_DUMMY_MAIL_ID, jackMailIcfUid);
        assertNoDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME);

        // Set by inbound mappings, this should stay
  		PrismAsserts.assertPropertyValue(userJack, UserType.F_EMAIL_ADDRESS, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
  		PrismAsserts.assertPropertyValue(userJack, TRAFO_EXTENSION_HOMEDIR_PATH, "c:\\install\\test-id-folder");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Clean up the data set by inbound. These could ruin next tests
        modifyUserReplace(USER_JACK_OID, UserType.F_EMAIL_ADDRESS, task, result);
        modifyUserReplace(USER_JACK_OID, TRAFO_EXTENSION_HOMEDIR_PATH, task, result);

        userJack = getUser(USER_JACK_OID);
		display("Clean jack", userJack);
	}

	@Test
    public void test150JackAssignRoleEmployee() throws Exception {
		final String TEST_NAME = "test150JackAssignRoleEmployee";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_OID, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 2);
		String accountAdOid = getLinkRefOid(userJack, RESOURCE_DUMMY_AD_OID);
		String accountMailOid = getLinkRefOid(userJack, RESOURCE_DUMMY_MAIL_OID);

		// AD ACCOUNT

		// Check shadow
        PrismObject<ShadowType> accountAdShadow = repositoryService.getObject(ShadowType.class, accountAdOid, null, result);
        assertAccountShadowRepo(accountAdShadow, accountAdOid, ACCOUNT_JACK_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountAdModel = modelService.getObject(ShadowType.class, accountAdOid, null, task, result);
        display("AD shadow", accountAdModel);
        assertAccountShadowModel(accountAdModel, accountAdOid, ACCOUNT_JACK_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountAdModel);

        jackAdIcfUid = getIcfUid(accountAdModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, jackAdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_JACK_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_JACK_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Sparrow");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_JACK_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME, "555-1234");


 		// MAIL ACCOUNT

 		// Check shadow
        PrismObject<ShadowType> accountMailShadow = repositoryService.getObject(ShadowType.class, accountMailOid, null, result);
        assertAccountShadowRepo(accountMailShadow, accountMailOid, ACCOUNT_JACK_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountMailModel = modelService.getObject(ShadowType.class, accountMailOid, null, task, result);
        display("Mail shadow", accountMailModel);
        assertAccountShadowModel(accountMailModel, accountMailOid, ACCOUNT_JACK_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountMailModel);

        jackMailIcfUid = getIcfUid(accountMailModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, jackMailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Sparrow");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsparrow.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, USER_JACK_USERNAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\"+USER_JACK_USERNAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_JACK_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userJack, UserType.F_EMAIL_ADDRESS, "Jack.Sparrow@" + TRAFO_MAIL_DOMAIN);
 		PrismAsserts.assertPropertyValue(userJack,
 				new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "c:\\install\\test-id-folder");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // primary, link (2 deltas)
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class); // AD account
        dummyAuditService.assertExecutionDeltas(1,3);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class); // Mail account
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class); // inbound
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class); // AD account
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Angelica pretends to be Jack Sparrow. She has the same first and last name. There is a naming conflict.
	 * The IDs and mail addresses should be correctly suffixed.
	 */
	@Test
    public void test160AngelicaAdd() throws Exception {
		final String TEST_NAME = "test160AngelicaAdd";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(USER_ANGELICA_FILE);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAngelica = getUser(USER_ANGELICA_OID);
		display("User angelica after change execution", userAngelica);
		assertUser(userAngelica, USER_ANGELICA_OID, USER_ANGELICA_USERNAME, "Jack Sparrow", "Jack", "Sparrow");
		assertLinks(userAngelica, 2);
		String accountAdOid = getLinkRefOid(userAngelica, RESOURCE_DUMMY_AD_OID);
		String accountMailOid = getLinkRefOid(userAngelica, RESOURCE_DUMMY_MAIL_OID);

		// AD ACCOUNT

		// Check shadow
        PrismObject<ShadowType> accountAdShadow = repositoryService.getObject(ShadowType.class, accountAdOid, null, result);
        assertAccountShadowRepo(accountAdShadow, accountAdOid, ACCOUNT_ANGELICA_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountAdModel = modelService.getObject(ShadowType.class, accountAdOid, null, task, result);
        display("AD shadow", accountAdModel);
        assertAccountShadowModel(accountAdModel, accountAdOid, ACCOUNT_ANGELICA_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountAdModel);

        angelicaAdIcfUid = getIcfUid(accountAdModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, angelicaAdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_ANGELICA_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_ANGELICA_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Sparrow");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, "Jack.Sparrow2@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_ANGELICA_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME);

 		// MAIL ACCOUNT

 		// Check shadow
        PrismObject<ShadowType> accountMailShadow = repositoryService.getObject(ShadowType.class, accountMailOid, null, result);
        assertAccountShadowRepo(accountMailShadow, accountMailOid, ACCOUNT_ANGELICA_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountMailModel = modelService.getObject(ShadowType.class, accountMailOid, null, task, result);
        display("Mail shadow", accountMailModel);
        assertAccountShadowModel(accountMailModel, accountMailOid, ACCOUNT_ANGELICA_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountMailModel);

        angelicaMailIcfUid = getIcfUid(accountMailModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, angelicaMailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "Jack.Sparrow2@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "Jack");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Sparrow2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsparrow2.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, USER_ANGELICA_USERNAME + "2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\"+USER_ANGELICA_USERNAME+ "2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_ANGELICA_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userAngelica, UserType.F_EMAIL_ADDRESS, "Jack.Sparrow2@" + TRAFO_MAIL_DOMAIN);
 		PrismAsserts.assertPropertyValue(userAngelica,
 				new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "c:\\install\\test-id-folder");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 3);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, UserType.class); // primary
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class); // AD account
        dummyAuditService.assertExecutionDeltas(1, 3);
        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class); // Mail account
        dummyAuditService.assertExecutionDeltas(2, 2);
        dummyAuditService.assertHasDelta(2, ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class); // AD account
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Attempt to add two employees that are boh "John Smith". This is the first user. Everything shouyld do as normal.
	 * Note: this is a different case than jack-angelica. Jack and Angelica are "externists". Smithes are employees (type "T")
	 */
	@Test
    public void test200Smith111Add() throws Exception {
		final String TEST_NAME = "test200Smith111Add";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(USER_SMITH111_FILE);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userSmith = getUser(USER_SMITH111_OID);
		display("User smith111 after change execution", userSmith);
		assertUser(userSmith, USER_SMITH111_OID, USER_SMITH111_USERNAME, "John Smith", "John", "Smith");
		assertLinks(userSmith, 2);
		String accountAdOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_AD_OID);
		String accountMailOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_MAIL_OID);

		// AD ACCOUNT

		// Check shadow
        PrismObject<ShadowType> accountAdShadow = repositoryService.getObject(ShadowType.class, accountAdOid, null, result);
        assertAccountShadowRepo(accountAdShadow, accountAdOid, ACCOUNT_SMITH111_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountAdModel = modelService.getObject(ShadowType.class, accountAdOid, null, task, result);
        display("AD shadow", accountAdModel);
        assertAccountShadowModel(accountAdModel, accountAdOid, ACCOUNT_SMITH111_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountAdModel);

        smith111AdIcfUid = getIcfUid(accountAdModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, smith111AdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_SMITH111_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_SMITH111_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Smith");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, "John.Smith@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME, "\\\\medusa\\User\\Smith_smith111");

 		// MAIL ACCOUNT

 		// Check shadow
        PrismObject<ShadowType> accountMailShadow = repositoryService.getObject(ShadowType.class, accountMailOid, null, result);
        assertAccountShadowRepo(accountMailShadow, accountMailOid, ACCOUNT_SMITH111_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountMailModel = modelService.getObject(ShadowType.class, accountMailOid, null, task, result);
        display("Mail shadow", accountMailModel);
        assertAccountShadowModel(accountMailModel, accountMailOid, ACCOUNT_SMITH111_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountMailModel);

        smith111MailIcfUid = getIcfUid(accountMailModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, smith111MailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "John.Smith@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Smith");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsmith.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, "PS111", "jsmith");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\js111");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userSmith, UserType.F_EMAIL_ADDRESS, "John.Smith@" + TRAFO_MAIL_DOMAIN);
 		PrismAsserts.assertPropertyValue(userSmith,
 				new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "\\\\medusa\\User\\Smith_smith111");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(5);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, UserType.class); // primary
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class); // AD account
        dummyAuditService.assertExecutionDeltas(1,3);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class); // link, inbound (2 deltas)
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class); // Mail account
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class); // inbound
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class); // AD account
        dummyAuditService.assertExecutionDeltas(3,1);
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, UserType.class); // inbound - SHOULD THIS BE HERE?? FIXME
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Attempt to add two employees that are boh "John Smith". This is the second user. There should be a naming conflict.
	 * Note: this is a different case than jack-angelica. Jack and Angelica are "externists". Smithes are employees (type "T")
	 */
	@Test
    public void test210Smith222Add() throws Exception {
		final String TEST_NAME = "test210Smith222Add";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(USER_SMITH222_FILE);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userSmith = getUser(USER_SMITH222_OID);
		display("User smith222 after change execution", userSmith);
		assertUser(userSmith, USER_SMITH222_OID, USER_SMITH222_USERNAME, "John Smith", "John", "Smith");
		assertLinks(userSmith, 2);
		String accountAdOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_AD_OID);
		String accountMailOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_MAIL_OID);

		// AD ACCOUNT

		// Check shadow
        PrismObject<ShadowType> accountAdShadow = repositoryService.getObject(ShadowType.class, accountAdOid, null, result);
        assertAccountShadowRepo(accountAdShadow, accountAdOid, ACCOUNT_SMITH222_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountAdModel = modelService.getObject(ShadowType.class, accountAdOid, null, task, result);
        display("AD shadow", accountAdModel);
        assertAccountShadowModel(accountAdModel, accountAdOid, ACCOUNT_SMITH222_AD_DN, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountAdModel);

        smith222AdIcfUid = getIcfUid(accountAdModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, smith222AdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_SMITH222_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_SMITH222_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Smith");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, "John.Smith2@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME, "\\\\medusa\\User\\Smith_smith222");

 		// MAIL ACCOUNT

 		// Check shadow
        PrismObject<ShadowType> accountMailShadow = repositoryService.getObject(ShadowType.class, accountMailOid, null, result);
        assertAccountShadowRepo(accountMailShadow, accountMailOid, ACCOUNT_SMITH222_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountMailModel = modelService.getObject(ShadowType.class, accountMailOid, null, task, result);
        display("Mail shadow", accountMailModel);
        assertAccountShadowModel(accountMailModel, accountMailOid, ACCOUNT_SMITH222_MAIL_USERNAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountMailModel);

        smith222MailIcfUid = getIcfUid(accountMailModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, smith222MailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "John.Smith2@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Smith2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsmith2.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, "PS222", "jsmith2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\js222");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userSmith, UserType.F_EMAIL_ADDRESS, "John.Smith2@" + TRAFO_MAIL_DOMAIN);
 		PrismAsserts.assertPropertyValue(userSmith,
 				new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "\\\\medusa\\User\\Smith_smith222");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(5);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, UserType.class); // primary
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class); // AD account
        dummyAuditService.assertExecutionDeltas(1,3);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class); // link, inbound (2 deltas)
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class); // Mail account
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class); // inbound
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class); // AD account
        dummyAuditService.assertExecutionDeltas(3,1);
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, UserType.class); // inbound - SHOULD THIS BE HERE?? FIXME
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Attempt to rename two employees that are boh "John Smith". This is the first user. Everything shouyld do as normal.
	 * Note: this is a different case than jack-angelica. Jack and Angelica are "externists". Smithes are employees (type "T")
	 */
	@Test
    public void test300Smith111Rename() throws Exception {
		final String TEST_NAME = "test300Smith111Rename";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
//        addObject(USER_SMITH111_FILE);


//        TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess(result);

		PrismObject<UserType> userSmith = getUser(USER_SMITH111_OID);
		display("User smith111 before change execution", userSmith);
		assertUser(userSmith, USER_SMITH111_OID, USER_SMITH111_USERNAME, "John Smith", "John", "Smith");
		assertLinks(userSmith, 2);
		String accountAdOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_AD_OID);
		String accountMailOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_MAIL_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

        Collection<? extends ItemDelta> fullNameModification = PropertyDelta.createModificationReplacePropertyCollection(UserType.F_FAMILY_NAME, userSmith.getDefinition(), new PolyString("Smither", "smither"));
        ObjectDelta.createModifyDelta(userSmith.getOid(), fullNameModification, UserType.class, prismContext);



        modifyUserReplace(userSmith.getOid(), UserType.F_FAMILY_NAME, task, result, new PolyString("Smither", "smither"));

		// AD ACCOUNT

		// Check shadow
        PrismObject<ShadowType> accountAdShadow = repositoryService.getObject(ShadowType.class, accountAdOid, null, result);
        display("AD shadow", accountAdShadow);
        assertAccountShadowRepo(accountAdShadow, accountAdOid, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountAdModel = modelService.getObject(ShadowType.class, accountAdOid, null, task, result);
        display("AD shadow", accountAdModel);
        assertAccountShadowModel(accountAdModel, accountAdOid, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountAdModel);

        smith111AdIcfUid = getIcfUid(accountAdModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, smith111AdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_SMITH111_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_SMITH111_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Smither");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, "John.Smither@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH111_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME, "\\\\medusa\\User\\Smither_smith111");

 		// MAIL ACCOUNT

 		// Check shadow
        PrismObject<ShadowType> accountMailShadow = repositoryService.getObject(ShadowType.class, accountMailOid, null, result);
        assertAccountShadowRepo(accountMailShadow, accountMailOid, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountMailModel = modelService.getObject(ShadowType.class, accountMailOid, null, task, result);
        display("Mail shadow", accountMailModel);
        assertAccountShadowModel(accountMailModel, accountMailOid, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountMailModel);

        smith111MailIcfUid = getIcfUid(accountMailModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, smith111MailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "John.Smither@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Smither");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsmith.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, "PS111", "jsmither");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\js111");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH111_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		PrismObject<UserType> userSmithAfter = getUser(USER_SMITH111_OID);
		display("User smith111 after change execution", userSmithAfter);
 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userSmithAfter, UserType.F_EMAIL_ADDRESS, "John.Smither@" + TRAFO_MAIL_DOMAIN);

		// [pmed] This is nondeterministic: extension/homedir is filled-in from both AD and Mail resources. (With different values.)
		// So it's hard to say which value will be there in the end.
 		// PrismAsserts.assertPropertyValue(userSmithAfter,
 		//		new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "\\\\medusa\\User\\Smither_smith111");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(5);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // primary
//        dummyAuditService.asserHasDelta(0,ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class); // AD account (outbound)
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class); // link, inbound (2 deltas)
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class); // Mail account (outboud)
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class); // inbound
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class); // AD account (mail change compued from outboud)
        dummyAuditService.assertExecutionDeltas(3,1);
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, UserType.class); // inbound - SHOULD THIS BE HERE?? FIXME
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * Attempt to rename two employees that are boh "John Smith". This is the second user. There should be a naming conflict.
	 * Note: this is a different case than jack-angelica. Jack and Angelica are "externists". Smithes are employees (type "T")
	 */
	@Test
    public void test310Smith222Rename() throws Exception {
		final String TEST_NAME = "test310Smith222Rename";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObject<UserType> userSmith = getUser(USER_SMITH222_OID);
		display("User smith222 before change execution", userSmith);
		assertUser(userSmith, USER_SMITH222_OID, USER_SMITH222_USERNAME, "John Smith", "John", "Smith");
		assertLinks(userSmith, 2);
		String accountAdOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_AD_OID);
		String accountMailOid = getLinkRefOid(userSmith, RESOURCE_DUMMY_MAIL_OID);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

        modifyUserReplace(userSmith.getOid(), UserType.F_FAMILY_NAME, task, result, new PolyString("Smither", "smither"));

		// AD ACCOUNT

		// Check shadow
        PrismObject<ShadowType> accountAdShadow = repositoryService.getObject(ShadowType.class, accountAdOid, null, result);
        display("AD shadow", accountAdShadow);
        assertAccountShadowRepo(accountAdShadow, accountAdOid, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME, resourceDummyAdType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountAdModel = modelService.getObject(ShadowType.class, accountAdOid, null, task, result);
        display("AD shadow", accountAdModel);
        assertAccountShadowModel(accountAdModel, accountAdOid, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME, resourceDummyAdType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountAdModel);

        smith222AdIcfUid = getIcfUid(accountAdModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_AD_ID, smith222AdIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, ACCOUNT_SMITH222_AD_SAM_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, ACCOUNT_SMITH222_AD_SAM_NAME + "@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, "Smither");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, "John.Smither2@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountNoAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_AD_ID, ACCOUNT_SMITH222_AD_DN_AFTER_RENAME,
 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME, "\\\\medusa\\User\\Smither_smith222");

 		// MAIL ACCOUNT

 		// Check shadow
        PrismObject<ShadowType> accountMailShadow = repositoryService.getObject(ShadowType.class, accountMailOid, null, result);
        assertAccountShadowRepo(accountMailShadow, accountMailOid, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME, resourceDummyMailType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountMailModel = modelService.getObject(ShadowType.class, accountMailOid, null, task, result);
        display("Mail shadow", accountMailModel);
        assertAccountShadowModel(accountMailModel, accountMailOid, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME, resourceDummyMailType, caseIgnoreMatchingRule);
        assertAdministrativeStatusEnabled(accountMailModel);

        smith222MailIcfUid = getIcfUid(accountMailModel);

        // Check account in dummy resource
        assertDummyAccountById(RESOURCE_DUMMY_MAIL_ID, smith222MailIcfUid);
        assertDummyAccount(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME, true);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_INTERNET_ADDRESS_NAME, "John.Smither2@" + TRAFO_MAIL_DOMAIN);
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_FIRST_NAME_NAME, "John");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_LAST_NAME_NAME, "Smither2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_ID_FILE_NAME, "c:\\install\\test-id-folder\\jsmith2.id");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_SHORT_NAME_NAME, "PS222", "jsmither2");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_FILE_NAME_NAME, "mail\\js222");
 		assertDummyAccountAttribute(RESOURCE_DUMMY_MAIL_ID, ACCOUNT_SMITH222_MAIL_USERNAME_AFTER_RENAME,
 				DUMMY_ACCOUNT_ATTRIBUTE_MAIL_MAIL_DOMAIN_NAME, "TRAFO");

 		PrismObject<UserType> userSmithAfter = getUser(USER_SMITH222_OID);
		display("User smith222 after change execution", userSmithAfter);
 		// Set by inbound mappings
 		PrismAsserts.assertPropertyValue(userSmithAfter, UserType.F_EMAIL_ADDRESS, "John.Smither2@" + TRAFO_MAIL_DOMAIN);
		// [pmed] This is nondeterministic: extension/homedir is filled-in from both AD and Mail resources. (With different values.)
		// So it's hard to say which value will be there in the end.
 		// PrismAsserts.assertPropertyValue(userSmithAfter,
 		//		new ItemPath(UserType.F_EXTENSION, TRAFO_EXTENSION_HOMEDIR_QNAME), "\\\\medusa\\User\\Smither_smith222");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(5);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // primary
//        dummyAuditService.asserHasDelta(0,ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class); // AD account (outbound)
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class); // link, inbound (2 deltas)
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class); // Mail account (outboud)
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class); // inbound
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class); // AD account (mail change compued from outboud)
        dummyAuditService.assertExecutionDeltas(3,1);
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, UserType.class); // inbound - SHOULD THIS BE HERE?? FIXME
        dummyAuditService.assertExecutionSuccess();
	}

}
