package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestServiceAccountsClassifier extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "service-accounts-classifier");
	
	protected static final File RESOURCE_DUMMY_CLASSIFIER_FILE = new File(TEST_DIR, "resource-dummy-classifier.xml");
	protected static final String RESOURCE_DUMMY_CLASSIFIER_OID = "1169ac14-8377-11e8-b404-5b5a1a8af0db";
	private static final String RESOURCE_DUMMY_CLASSIFIER_NS = MidPointConstants.NS_RI;
	
	private static final File ROLE_EMPLOYEE_FILE = new File(TEST_DIR, "role-employee.xml");
	private static final String ROLE_EMPLOYEE_OID = "23d90f70-1924-419e-9beb-78a8bde6d261";
	
	private static final File SERVICE_JIRA_FILE = new File(TEST_DIR, "service-jira.xml");
	private static final String SERVICE_JIRA_OID = "c0c010c0-d34d-b33f-f00d-111111122222";
	
	private static final String ACCOUNT_DUMMY_JIRA_USERNAME = "jira";
	private static final String ACCOUNT_DUMMY_WIKI_USERNAME = "wiki";
	
	private static final File TASK_RECONCILE_DUMMY_CLASSIFIER_FILE = new File(TEST_DIR, "task-dummy-classifier-reconcile.xml");
	private static final String TASK_RECONCILE_DUMMY_CLASSIFIER_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";
	
	private static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/story/serviceAccountsClassifier/ext";
	private static final QName F_ACCOUNT_NAME = new QName(NS_EXT, "accountName");
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		assertServices(0);
		
		importObjectFromFile(ROLE_EMPLOYEE_FILE, initResult);
		
		initDummyResourcePirate(null, RESOURCE_DUMMY_CLASSIFIER_FILE, RESOURCE_DUMMY_CLASSIFIER_OID, initTask, initResult);
		getDummyResource().setSyncStyle(DummySyncStyle.SMART);
	}
	
	@Test
	public void test001assigneJackEmployeeRole() throws Exception {
		final String TEST_NAME = "test001assigneJackEmployeeRole";
		displayTestTitle(TEST_NAME);
		
		//WHEN
		displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_EMPLOYEE_OID);
		
		//THEN
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		assertAccount(userAfter, RESOURCE_DUMMY_CLASSIFIER_OID);
		
		//TODO: assert attributes?
	}
	
	@Test
	public void test100createService() throws Exception {
		final String TEST_NAME = "test100createService";
		displayTestTitle(TEST_NAME);
		
		//WHEN
		addObject(SERVICE_JIRA_FILE);
		
		//THEN
		displayThen(TEST_NAME);
		PrismObject<ServiceType> service = getObject(ServiceType.class, SERVICE_JIRA_OID);
		display("Service magazine after", service);
		assertNotNull("No magazine service", service);
		
		assertNoLinkedAccount(service);
	}
	
	@Test
	public void test101assignResourceNoneEnforcement() throws Exception {
		final String TEST_NAME = "test100createService";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		//GIVEN
		
		assumeResourceAssigmentPolicy(RESOURCE_DUMMY_CLASSIFIER_OID, AssignmentPolicyEnforcementType.NONE, false);
//		AssignmentType assignment = new AssignmentType(prismContext);
//		assignment.construction(new ConstructionType(prismContext).resourceRef(RESOURCE_DUMMY_CLASSIFIER_OID, ObjectTypes.RESOURCE.getTypeQName()));
//		
//		ObjectDelta<ServiceType> assignResourceDelta = (ObjectDelta<ServiceType>) DeltaBuilder.deltaFor(ServiceType.class, prismContext).item(ServiceType.F_ASSIGNMENT).add(assignment).asObjectDelta(SERVICE_JIRA_OID);
//	
		//WHEN
		displayWhen(TEST_NAME);
		assignAccount(ServiceType.class, SERVICE_JIRA_OID, RESOURCE_DUMMY_CLASSIFIER_OID, "service");
//		executeChanges(assignResourceDelta, null, task, result);
		
		//THEN
		displayThen(TEST_NAME);
		PrismObject<ServiceType> service = getObject(ServiceType.class, SERVICE_JIRA_OID);
		display("Service magazine after", service);
		assertNotNull("No magazine service", service);
		assertAssignedResource(ServiceType.class, SERVICE_JIRA_OID, RESOURCE_DUMMY_CLASSIFIER_OID, task, result);
		
		assertNoLinkedAccount(service);
		
		assumeResourceAssigmentPolicy(RESOURCE_DUMMY_CLASSIFIER_OID, AssignmentPolicyEnforcementType.RELATIVE, false);
	}
	
	@Test
	public void test150StartReconTask() throws Exception {
		final String TEST_NAME = "test150StartReconTask";
		displayTestTitle(TEST_NAME);
		
		assertUsers(getNumberOfUsers());
		assertServices(1);
		
		// WHEN
        displayWhen(TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_CLASSIFIER_FILE);

		// THEN
		displayThen(TEST_NAME);
		
		waitForTaskStart(TASK_RECONCILE_DUMMY_CLASSIFIER_OID, true);
		
		assertServices(1);
		assertUsers(getNumberOfUsers());
	}
	
	@Test
	public void test151LinkServiceAccountRecon() throws Exception {
		final String TEST_NAME = "test151LinkServiceAccountRecon";
		displayTestTitle(TEST_NAME);
		
		// Preconditions
		assertServices(1);

        DummyAccount account = new DummyAccount(ACCOUNT_DUMMY_JIRA_USERNAME);
		account.setEnabled(true);
		
		// WHEN
        displayWhen(TEST_NAME);

		getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_CLASSIFIER_OID, true);
		
		// THEN
		displayThen(TEST_NAME);
		
		assertServices(1);
		PrismObject<ServiceType> serviceJirafter = getObject(ServiceType.class, SERVICE_JIRA_OID);
		display("Service magazine after", serviceJirafter);
		assertNotNull("No magazine service", serviceJirafter);
		PrismAsserts.assertPropertyValue(serviceJirafter, new ItemPath(ServiceType.F_EXTENSION, F_ACCOUNT_NAME), ACCOUNT_DUMMY_JIRA_USERNAME);
		assertLinks(serviceJirafter, 1);
		
	}
	
	@Test
	public void test152InactivateUnmatchedAccountRecon() throws Exception {
		final String TEST_NAME = "test152InactivateUnmatchedAccountRecon";
		displayTestTitle(TEST_NAME);
		
		// Preconditions
		assertServices(1);

        DummyAccount account = new DummyAccount(ACCOUNT_DUMMY_WIKI_USERNAME);
		account.setEnabled(true);
		
		// WHEN
        displayWhen(TEST_NAME);

		getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_CLASSIFIER_OID, true);
		
		// THEN
		displayThen(TEST_NAME);
		
		assertServices(1);
		
		DummyAccount dummyAccount = getDummyAccount(getDummyResource().getInstanceName(), ACCOUNT_DUMMY_WIKI_USERNAME);
		assertFalse(dummyAccount.isEnabled(), "Dummy account should be disabled");
	}
	
	
	
}
