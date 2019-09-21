/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.assignments;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.ApprovalInstruction;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.NO_ASSIGNEES_FOUND;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.PROCESS;
import static java.util.Collections.*;
import static org.testng.AssertJUnit.*;

/**
 * A special test dealing with assigning roles that have different metarole-induced approval policies.
 *
 * Role21 - uses default approval (org:approver)
 * Role22 - uses metarole 1 'default' induced approval (org:special-approver)
 * Role23 - uses both metarole 'default' and 'security' induced approval (org:special-approver and org:security-approver)
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentsAdvanced extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentsAdvanced.class);

	private static final File TEST_RESOURCE_DIR = new File("src/test/resources/assignments-advanced");

	private static final File METAROLE_DEFAULT_FILE = new File(TEST_RESOURCE_DIR, "metarole-default.xml");
	private static final File METAROLE_SECURITY_FILE = new File(TEST_RESOURCE_DIR, "metarole-security.xml");

	private static final File ROLE_ROLE21_FILE = new File(TEST_RESOURCE_DIR, "role-role21-standard.xml");
    private static final File ROLE_ROLE22_FILE = new File(TEST_RESOURCE_DIR, "role-role22-special.xml");
    private static final File ROLE_ROLE23_FILE = new File(TEST_RESOURCE_DIR, "role-role23-special-and-security.xml");
    private static final File ROLE_ROLE24_FILE = new File(TEST_RESOURCE_DIR, "role-role24-approval-and-enforce.xml");
    private static final File ROLE_ROLE25_FILE = new File(TEST_RESOURCE_DIR, "role-role25-very-complex-approval.xml");
    private static final File ROLE_ROLE26_FILE = new File(TEST_RESOURCE_DIR, "role-role26-no-approvers.xml");
    private static final File ROLE_ROLE27_FILE = new File(TEST_RESOURCE_DIR, "role-role27-modifications-and.xml");
    private static final File ROLE_ROLE28_FILE = new File(TEST_RESOURCE_DIR, "role-role28-modifications-or.xml");
    private static final File ROLE_ROLE29_FILE = new File(TEST_RESOURCE_DIR, "role-role29-modifications-no-items.xml");
    private static final File ORG_LEADS2122_FILE = new File(TEST_RESOURCE_DIR, "org-leads2122.xml");

    private static final File USER_LEAD21_FILE = new File(TEST_RESOURCE_DIR, "user-lead21.xml");
    private static final File USER_LEAD22_FILE = new File(TEST_RESOURCE_DIR, "user-lead22.xml");
    private static final File USER_LEAD23_FILE = new File(TEST_RESOURCE_DIR, "user-lead23.xml");
    private static final File USER_LEAD24_FILE = new File(TEST_RESOURCE_DIR, "user-lead24.xml");

    private static final File USER_SECURITY_APPROVER_FILE = new File(TEST_RESOURCE_DIR, "user-security-approver.xml");
    private static final File USER_SECURITY_APPROVER_DEPUTY_FILE = new File(TEST_RESOURCE_DIR, "user-security-approver-deputy.xml");
    private static final File USER_SECURITY_APPROVER_DEPUTY_LIMITED_FILE = new File(TEST_RESOURCE_DIR, "user-security-approver-deputy-limited.xml");

    private String userSecurityApproverOid;
    private String userSecurityApproverDeputyOid;
    private String userSecurityApproverDeputyLimitedOid;

    private String roleRole21Oid;
    private String roleRole22Oid;
    private String roleRole23Oid;
    private String roleRole24Oid;
    private String roleRole25Oid;
    private String roleRole26Oid;
    private String roleRole27Oid;
    private String roleRole28Oid;
    private String roleRole29Oid;
    private String orgLeads2122Oid;

    private String userLead21Oid;
    private String userLead22Oid;
    private String userLead23Oid;
    private String userLead24Oid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(METAROLE_DEFAULT_FILE, initResult);
		repoAddObjectFromFile(METAROLE_SECURITY_FILE, initResult);

		roleRole21Oid = repoAddObjectFromFile(ROLE_ROLE21_FILE, initResult).getOid();
		roleRole22Oid = repoAddObjectFromFile(ROLE_ROLE22_FILE, initResult).getOid();
		roleRole23Oid = repoAddObjectFromFile(ROLE_ROLE23_FILE, initResult).getOid();
		roleRole24Oid = repoAddObjectFromFile(ROLE_ROLE24_FILE, initResult).getOid();
		roleRole25Oid = repoAddObjectFromFile(ROLE_ROLE25_FILE, initResult).getOid();
		roleRole26Oid = repoAddObjectFromFile(ROLE_ROLE26_FILE, initResult).getOid();
		roleRole27Oid = repoAddObjectFromFile(ROLE_ROLE27_FILE, initResult).getOid();
		roleRole28Oid = repoAddObjectFromFile(ROLE_ROLE28_FILE, initResult).getOid();
		roleRole29Oid = repoAddObjectFromFile(ROLE_ROLE29_FILE, initResult).getOid();
		orgLeads2122Oid = repoAddObjectFromFile(ORG_LEADS2122_FILE, initResult).getOid();

		userLead21Oid = addAndRecomputeUser(USER_LEAD21_FILE, initTask, initResult);
		userLead22Oid = addAndRecomputeUser(USER_LEAD22_FILE, initTask, initResult);
		userLead23Oid = addAndRecomputeUser(USER_LEAD23_FILE, initTask, initResult);
		userLead24Oid = addAndRecomputeUser(USER_LEAD24_FILE, initTask, initResult);

		userSecurityApproverOid = addAndRecomputeUser(USER_SECURITY_APPROVER_FILE, initTask, initResult);
		userSecurityApproverDeputyOid = addAndRecomputeUser(USER_SECURITY_APPROVER_DEPUTY_FILE, initTask, initResult);
		userSecurityApproverDeputyLimitedOid = addAndRecomputeUser(USER_SECURITY_APPROVER_DEPUTY_LIMITED_FILE, initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_JSON);
	}

	@Test
	public void test102AddRoles123AssignmentYYYYDeputy() throws Exception {
		final String TEST_NAME = "test102AddRoles123AssignmentYYYYDeputy";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, true);
	}

	@Test
	public void test105AddRoles123AssignmentYYYYImmediate() throws Exception {
		final String TEST_NAME = "test105AddRoles123AssignmentYYYYImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true, true, false);
	}

	@Test
	public void test110AddRoles123AssignmentNNNN() throws Exception {
		final String TEST_NAME = "test110AddRoles123AssignmentNNNN";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false, false, false);
	}

	@Test
	public void test115AddRoles123AssignmentNNNNImmediate() throws Exception {
		final String TEST_NAME = "test115AddRoles123AssignmentNNNNImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false, false, false);
	}

	@Test
	public void test120AddRoles123AssignmentYNNN() throws Exception {
		final String TEST_NAME = "test120AddRoles123AssignmentYNNN";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false, false, false);
	}

	@Test
	public void test125AddRoles123AssignmentYNNNImmediate() throws Exception {
		final String TEST_NAME = "test125AddRoles123AssignmentYNNNImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false, false, false);
	}

	@Test
	public void test130AddRoles123AssignmentYYYN() throws Exception {
		final String TEST_NAME = "test130AddRoles123AssignmentYYYN";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, false, false);
	}

	@Test
	public void test132AddRoles123AssignmentYYYNDeputy() throws Exception {
		final String TEST_NAME = "test132AddRoles123AssignmentYYYNDeputy";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, false, true);
	}

	@Test
	public void test135AddRoles123AssignmentYYYNImmediate() throws Exception {
		final String TEST_NAME = "test135AddRoles123AssignmentYYYNImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true, false, false);
	}

	/**
	 * Attempt to assign roles 21-23 along with changing description.
	 */
	@Test
	public void test200AddRoles123AssignmentYYYY() throws Exception {
		final String TEST_NAME = "test200AddRoles012AssignmentYYYY";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, false);
	}

	@Test
	public void test210DeleteRoles123AssignmentN() throws Exception {
		final String TEST_NAME = "test210DeleteRoles123AssignmentN";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, false, false, true);
	}

	@Test
	public void test212DeleteRoles123AssignmentNById() throws Exception {
		final String TEST_NAME = "test212DeleteRoles123AssignmentNById";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, false, true, false);
	}

	@Test
	public void test218DeleteRoles123AssignmentY() throws Exception {
		final String TEST_NAME = "test218DeleteRoles123AssignmentY";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, true, false, false);
	}

	@Test
	public void test220AddRoles123AssignmentYYYY() throws Exception {
		final String TEST_NAME = "test220AddRoles012AssignmentYYYY";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, false);
	}

	@Test
	public void test230DeleteRoles123AssignmentYById() throws Exception {
		final String TEST_NAME = "test230DeleteRoles123AssignmentYById";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, true, true, true);
	}

	/**
	 * MID-3836
	 */
	@Test
	public void test300ApprovalAndEnforce() throws Exception {
		final String TEST_NAME = "test300ApprovalAndEnforce";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		try {
			assignRole(userJackOid, roleRole24Oid, task, result);
		} catch (PolicyViolationException e) {
			// ok
			System.out.println("Got expected exception: " + e);
		}
		List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, getOpenItemsQuery(), null, task, result);
		display("current work items", currentWorkItems);
		assertEquals("Wrong # of current work items", 0, currentWorkItems.size());
	}

	// preview-related tests

	@Test
	public void test400AddRoles123AssignmentPreview() throws Exception {
		final String TEST_NAME = "test400AddRoles123AssignmentPreview";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		previewAssignRolesToJack(TEST_NAME, false, false);
	}

	@Test
	public void test410AddRoles1234AssignmentPreview() throws Exception {
		final String TEST_NAME = "test410AddRoles1234AssignmentPreview";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		previewAssignRolesToJack(TEST_NAME, false, true);
	}

	@Test
	public void test420AddRoles123AssignmentPreviewImmediate() throws Exception {
		final String TEST_NAME = "test420AddRoles123AssignmentPreviewImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		previewAssignRolesToJack(TEST_NAME, true, false);
	}

	/**
	 * MID-4121
	 */
	public static Exception exception;      // see system-configuration.xml

	@Test
	public void test500NoApprovers() throws Exception {
		final String TEST_NAME = "test500NoApprovers";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userJackOid, roleRole26Oid, task, result);
		String ref = result.findAsynchronousOperationReference();       // TODO use recompute + getAsync... when fixed
		assertNotNull("No asynchronous operation reference", ref);
		String caseOid = OperationResult.referenceToCaseOid(ref);

		List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, getOpenItemsQuery(), null, task, result);
		display("current work items", currentWorkItems);
		assertEquals("Wrong # of current work items", 0, currentWorkItems.size());

		assertNotNull("Missing task OID", caseOid);
		waitForCaseClose(getCase(caseOid));

		if (exception != null) {
			System.err.println("Got unexpected exception");
			exception.printStackTrace();
			fail("Got unexpected exception: " + exception);
		}
	}

	// "assignment modification" (no specific items)
	@Test
	public void test600AssignRole29() throws Exception {
		final String TEST_NAME = "test600AssignRole29";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(userJackOid, roleRole29Oid, task, result);           // should proceed without approvals

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jack = getUser(userJackOid);
		display("jack", jack);
		assertAssignedRoles(jack, roleRole29Oid);
	}

	@Test
	public void test610ModifyAssignmentOfRole29() throws Exception {
		final String TEST_NAME = "test610ModifyAssignmentOfRole29";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<UserType> jackBefore = getUser(userJackOid);
		AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, roleRole29Oid);
		ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_DESCRIPTION)
						.replace("new description")
				.asObjectDeltaCast(userJackOid);
		ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
				.item(UserType.F_FULL_NAME)
						.replace(PolyString.fromOrig("new full name"))
				.asObjectDeltaCast(userJackOid);

		// +THEN
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jackBefore;
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws Exception {
				return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return singletonList(deltaToApprove);
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0;
			}

			@Override
			protected String getObjectOid() {
				return userJackOid;
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return singletonList(new ExpectedTask(roleRole29Oid, "Modifying assignment of role \"Role29\" on user \"jack\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				ExpectedTask etask = getExpectedTasks().get(0);
				return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, roleRole29Oid, etask));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
				// todo
				// e.g. check metadata
				if (number == 0) {
					PrismObject<UserType> jack = getUser(userJackOid);
					assertEquals("wrong new full name", yes ? "new full name" : "Jack Sparrow", jack.asObjectable().getFullName().getOrig());
				} else {
					PrismObject<UserType> jack = getUser(userJackOid);
					AssignmentType assignment1 = findAssignmentByTargetRequired(jack, roleRole29Oid);
					assertEquals("wrong new assignment description", yes ? "new description" : null, assignment1.getDescription());
				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return null;
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment1"));
			}

			@Override
			protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
					OperationResult result) {
				// todo
			}

		}, 1, false);

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jackAfter = getUser(userJackOid);
		display("jack after", jackAfter);
		assertAssignedRoles(jackAfter, roleRole29Oid);
		assertAssignmentMetadata(jackAfter, roleRole29Oid, emptySet(), emptySet(), singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment1"));
	}

	private void assertAssignmentMetadata(PrismObject<? extends FocusType> object, String targetOid, Set<String> createApproverOids,
			Set<String> createApprovalComments, Set<String> modifyApproverOids, Set<String> modifyApprovalComments) {
		AssignmentType assignment = findAssignmentByTargetRequired(object, targetOid);
		MetadataType metadata = assignment.getMetadata();
		PrismAsserts.assertReferenceOids("Wrong create approvers", createApproverOids, metadata.getCreateApproverRef());
		PrismAsserts.assertEqualsCollectionUnordered("Wrong create comments", createApprovalComments, metadata.getCreateApprovalComment());
		PrismAsserts.assertReferenceOids("Wrong modify approvers", modifyApproverOids, metadata.getModifyApproverRef());
		PrismAsserts.assertEqualsCollectionUnordered("Wrong modify comments", modifyApprovalComments, metadata.getModifyApprovalComment());
	}

	@Test
	public void test620ModifyAssignmentOfRole29Immediate() throws Exception {
		final String TEST_NAME = "test620ModifyAssignmentOfRole29Immediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<UserType> jackBefore = getUser(userJackOid);
		AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, roleRole29Oid);
		ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_DESCRIPTION)
						.replace("new description 2")
				.asObjectDeltaCast(userJackOid);
		ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
				.item(UserType.F_FULL_NAME)
						.replace(PolyString.fromOrig("new full name 2"))
				.asObjectDeltaCast(userJackOid);

		// +THEN
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jackBefore;
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws Exception {
				return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return singletonList(deltaToApprove);
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0;
			}

			@Override
			protected String getObjectOid() {
				return userJackOid;
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return singletonList(new ExpectedTask(roleRole29Oid, "Modifying assignment of role \"Role29\" on user \"jack\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				ExpectedTask etask = getExpectedTasks().get(0);
				return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, roleRole29Oid, etask));
			}

			@SuppressWarnings("Duplicates")
			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
				// todo
				// e.g. check metadata
				if (number == 0) {
					PrismObject<UserType> jack = getUser(userJackOid);
					assertEquals("wrong new full name", yes ? "new full name 2" : "new full name", jack.asObjectable().getFullName().getOrig());
				} else {
					PrismObject<UserType> jack = getUser(userJackOid);
					AssignmentType assignment1 = findAssignmentByTargetRequired(jack, roleRole29Oid);
					assertEquals("wrong new assignment description", yes ? "new description 2" : "new description", assignment1.getDescription());
				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return null;
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment2"));
			}

			@Override
			protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
					OperationResult result) {
				// todo
			}

		}, 1, true);

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jackAfter = getUser(userJackOid);
		display("jack after", jackAfter);
		assertAssignedRoles(jackAfter, roleRole29Oid);
		assertAssignmentMetadata(jackAfter, roleRole29Oid, emptySet(), emptySet(), singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment2"));
	}

	@Test
	public void test630UnassignRole29() throws Exception {
		final String TEST_NAME = "test630UnassignRole29";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		unassignRoleByAssignmentValue(getUser(userJackOid), roleRole29Oid, task, result);           // should proceed without approvals

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jack = getUser(userJackOid);
		display("jack", jack);
		assertNotAssignedRole(jack, roleRole29Oid);
	}

	// assignment modification ("or" of 2 items)
	@Test
	public void test700AssignRole28() throws Exception {
		final String TEST_NAME = "test700AssignRole28";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		// WHEN/THEN
		ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT)
						.add(ObjectTypeUtil.createAssignmentTo(roleRole28Oid, ObjectTypes.ROLE, prismContext)
									.description("description"))
				.asObjectDeltaCast(userJackOid);
		ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
				.item(UserType.F_FULL_NAME)
				.replace(PolyString.fromOrig("new full name 3"))
				.asObjectDeltaCast(userJackOid);

		PrismObject<UserType> jackBefore = getUser(userJackOid);

		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jackBefore;
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws Exception {
				return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return singletonList(deltaToApprove);
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0;
			}

			@Override
			protected String getObjectOid() {
				return userJackOid;
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return singletonList(new ExpectedTask(roleRole28Oid, "Assigning role \"Role28\" to user \"jack\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				ExpectedTask etask = getExpectedTasks().get(0);
				return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, roleRole28Oid, etask));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
				// todo
				// e.g. check metadata
				if (number == 0) {
					PrismObject<UserType> jack = getUser(userJackOid);
					assertEquals("wrong new full name", yes ? "new full name 3" : "new full name 2", jack.asObjectable().getFullName().getOrig());
				} else {
					PrismObject<UserType> jack = getUser(userJackOid);
					if (yes) {
						assertAssignedRole(jack, roleRole28Oid);
					} else {
						assertNotAssignedRole(jack, roleRole28Oid);
					}
				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return null;
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment3"));
			}

			@Override
			protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
					OperationResult result) {
				// todo
			}

		}, 1, false);

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jack = getUser(userJackOid);
		display("jack", jack);
		assertAssignedRoles(jack, roleRole28Oid);
		assertAssignmentMetadata(jack, roleRole28Oid, singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment3"), emptySet(), emptySet());
	}

	@Test
	public void test710ModifyAssignmentOfRole28() throws Exception {
		final String TEST_NAME = "test710ModifyAssignmentOfRole28";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<UserType> jackBefore = getUser(userJackOid);
		AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, roleRole28Oid);
		ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_DESCRIPTION)
						.replace("new description")
				.item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_LIFECYCLE_STATE)      // this will be part of the delta to approve
						.replace("active")
				.asObjectDeltaCast(userJackOid);
		ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
				.item(UserType.F_FULL_NAME)
						.replace(PolyString.fromOrig("new full name 4"))
				.asObjectDeltaCast(userJackOid);

		// +THEN
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jackBefore;
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws Exception {
				return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return singletonList(deltaToApprove);
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0;
			}

			@Override
			protected String getObjectOid() {
				return userJackOid;
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return singletonList(new ExpectedTask(roleRole28Oid, "Modifying assignment of role \"Role28\" on user \"jack\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				ExpectedTask etask = getExpectedTasks().get(0);
				return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, roleRole28Oid, etask));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
				// todo
				// e.g. check metadata
				if (number == 0) {
					PrismObject<UserType> jack = getUser(userJackOid);
					assertEquals("wrong new full name", yes ? "new full name 4" : "new full name 3", jack.asObjectable().getFullName().getOrig());
				} else {
					PrismObject<UserType> jack = getUser(userJackOid);
					AssignmentType assignment1 = findAssignmentByTargetRequired(jack, roleRole28Oid);
					assertEquals("wrong new assignment description", yes ? "new description" : "description", assignment1.getDescription());
				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return null;
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment4"));
			}

			@Override
			protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
					OperationResult result) {
				// todo
			}

		}, 1, false);

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jackAfter = getUser(userJackOid);
		display("jack after", jackAfter);
		assertAssignedRoles(jackAfter, roleRole28Oid);
		assertAssignmentMetadata(jackAfter, roleRole28Oid, singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment3"), singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment4"));
	}

	@Test
	public void test720UnassignRole28() throws Exception {
		final String TEST_NAME = "test720UnassignRole28";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<UserType> jackBefore = getUser(userJackOid);
		AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, roleRole28Oid);
		ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT)
						.delete(new AssignmentType().id(assignment.getId()))        // id-only, to test the constraint
				.asObjectDeltaCast(userJackOid);
		ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
				.item(UserType.F_FULL_NAME)
				.replace(PolyString.fromOrig("new full name 5"))
				.asObjectDeltaCast(userJackOid);

		// +THEN
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jackBefore;
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws Exception {
				return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return singletonList(deltaToApprove);
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0;
			}

			@Override
			protected String getObjectOid() {
				return userJackOid;
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return singletonList(new ExpectedTask(roleRole28Oid, "Unassigning role \"Role28\" from user \"jack\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				ExpectedTask etask = getExpectedTasks().get(0);
				return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, roleRole28Oid, etask));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
				// todo
				// e.g. check metadata
				if (number == 0) {
					PrismObject<UserType> jack = getUser(userJackOid);
					assertEquals("wrong new full name", yes ? "new full name 5" : "new full name 4", jack.asObjectable().getFullName().getOrig());
				} else {
					PrismObject<UserType> jack = getUser(userJackOid);
					if (yes) {
						assertNotAssignedRole(jack, roleRole28Oid);
					} else {
						assertAssignedRole(jack, roleRole28Oid);
					}
				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return true;
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				return null;
			}

			@Override
			protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
					OperationResult result) {
				// todo
			}

		}, 1, false);

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jackAfter = getUser(userJackOid);
		display("jack after", jackAfter);
		assertNotAssignedRole(jackAfter, roleRole28Oid);
	}

	@Test
	public void test800AssignRole27() throws Exception {
		final String TEST_NAME = "test800AssignRole27";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT)
				.add(ObjectTypeUtil.createAssignmentTo(roleRole27Oid, ObjectTypes.ROLE, prismContext)
						.description("description"))
				.asObjectDeltaCast(userJackOid);
		executeChanges(delta, null, task, result); // should proceed without approvals (only 1 of the items is present)

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jack = getUser(userJackOid);
		display("jack", jack);
		assertAssignedRoles(jack, roleRole27Oid);
	}

	@Test
	public void test810ModifyAssignmentOfRole27() throws Exception {
		final String TEST_NAME = "test810ModifyAssignmentOfRole27";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<UserType> jackBefore = getUser(userJackOid);
		AssignmentType assignmentBefore = findAssignmentByTargetRequired(jackBefore, roleRole27Oid);
		ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT, assignmentBefore.getId(), AssignmentType.F_DESCRIPTION)
				.replace("new description")
				.asObjectDeltaCast(userJackOid);

		executeChanges(delta, null, task, result); // should proceed without approvals (only 1 of the items is present)

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jack = getUser(userJackOid);
		display("jack", jack);
		AssignmentType assignment = findAssignmentByTargetRequired(jack, roleRole27Oid);
		assertEquals("Wrong description", "new description", assignment.getDescription());
	}

	@Test
	public void test820UnassignRole27() throws Exception {
		final String TEST_NAME = "test820UnassignRole27";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		unassignRoleByAssignmentValue(getUser(userJackOid), roleRole27Oid, task, result);           // should proceed without approvals

		// THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> jack = getUser(userJackOid);
		display("jack", jack);
		assertNotAssignedRole(jack, roleRole27Oid);
	}

	private void executeAssignRoles123ToJack(String TEST_NAME, boolean immediate,
			boolean approve1, boolean approve2, boolean approve3a, boolean approve3b, boolean securityDeputy) throws Exception {
		Task task = createTask("executeAssignRoles123ToJack");
		PrismObject<UserType> jack = getUser(userJackOid);
		ObjectDelta<UserType> addRole1Delta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRole21Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> addRole2Delta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRole22Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> addRole3Delta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRole23Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> changeDescriptionDelta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
				.summarize(addRole1Delta, addRole2Delta, addRole3Delta, changeDescriptionDelta);
		ObjectDelta<UserType> delta0 = changeDescriptionDelta.clone();
		String originalDescription = getUser(userJackOid).asObjectable().getDescription();
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() {
				return primaryDelta.clone();
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 3;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return Arrays.asList(approve1, approve2, approve3a && approve3b);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return Arrays.asList(addRole1Delta.clone(), addRole2Delta.clone(), addRole3Delta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0.clone();
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return Arrays.asList(
						new ExpectedTask(roleRole21Oid, "Assigning role \"Role21\" to user \"jack\""),
						new ExpectedTask(roleRole22Oid, "Assigning role \"Role22\" to user \"jack\""),
						new ExpectedTask(roleRole23Oid, "Assigning role \"Role23\" to user \"jack\""));
			}

			// after first step
			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> tasks = getExpectedTasks();
				return Arrays.asList(
						new ExpectedWorkItem(userLead21Oid, roleRole21Oid, tasks.get(0)),
						new ExpectedWorkItem(userLead22Oid, roleRole22Oid, tasks.get(1)),
						new ExpectedWorkItem(userLead23Oid, roleRole23Oid, tasks.get(2))
				);
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(userJackOid, UserType.F_DESCRIPTION, TEST_NAME);
						} else {
							if (originalDescription != null) {
								assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
							}
						}
						break;
					case 1:
					case 2:
					case 3:
						String[] oids = { roleRole21Oid, roleRole22Oid, roleRole23Oid };
					if (yes) {
						assertAssignedRole(userJackOid, oids[number-1], opTask, result);
					} else {
						assertNotAssignedRole(userJackOid, oids[number-1], opTask, result);
					}
					break;

				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return null;            // ignore this way of approving
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				List<ExpectedTask> tasks = getExpectedTasks();
				List<ApprovalInstruction> instructions = new ArrayList<>();
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead21Oid, roleRole21Oid, tasks.get(0)), approve1, userLead21Oid, null));
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead22Oid, roleRole22Oid, tasks.get(1)), approve2, userLead22Oid, null));
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead23Oid, roleRole23Oid, tasks.get(2)), approve3a, userLead23Oid, null));
				if (approve3a) {
					ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(2));
					ApprovalInstruction.CheckedRunnable before = () -> {
						login(getUserFromRepo(userSecurityApproverOid));
						checkVisibleWorkItem(expectedWorkItem, 1, task, task.getResult());
						login(getUserFromRepo(userSecurityApproverDeputyOid));
						checkVisibleWorkItem(expectedWorkItem, 1, task, task.getResult());
						login(getUserFromRepo(userSecurityApproverDeputyLimitedOid));
						checkVisibleWorkItem(null, 0, task, task.getResult());
					};
					instructions.add(new ApprovalInstruction(expectedWorkItem, approve3b,
							securityDeputy ? userSecurityApproverDeputyOid : userSecurityApproverOid, null, before, null));
				}
				return instructions;
			}
		}, 3, immediate);
	}

	private void previewAssignRolesToJack(String TEST_NAME, boolean immediate, boolean also24) throws Exception {
		Task task = createTask("previewAssignRolesToJack");
		OperationResult result = task.getResult();
		result.tracingProfile(tracer.compileProfile(addWorkflowLogging(createModelLoggingTracingProfile()), result));

		List<AssignmentType> assignmentsToAdd = new ArrayList<>();
		assignmentsToAdd.add(createAssignmentTo(roleRole21Oid, ObjectTypes.ROLE, prismContext));
		assignmentsToAdd.add(createAssignmentTo(roleRole22Oid, ObjectTypes.ROLE, prismContext));
		assignmentsToAdd.add(createAssignmentTo(roleRole23Oid, ObjectTypes.ROLE, prismContext));
		assignmentsToAdd.add(createAssignmentTo(roleRole25Oid, ObjectTypes.ROLE, prismContext));
		if (also24) {
			assignmentsToAdd.add(createAssignmentTo(roleRole24Oid, ObjectTypes.ROLE, prismContext));
		}
		ObjectDelta<UserType> primaryDelta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).addRealValues(assignmentsToAdd)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(userJackOid);

		ModelExecuteOptions options = immediate ? ModelExecuteOptions.createExecuteImmediatelyAfterApproval() : new ModelExecuteOptions();
		options.setPartialProcessing(new PartialProcessingOptionsType().approvals(PROCESS));
		ModelContext<ObjectType> modelContext = modelInteractionService
				.previewChanges(singleton(primaryDelta), options, task, result);

		List<ApprovalSchemaExecutionInformationType> approvalInfo = modelContext.getHookPreviewResults(ApprovalSchemaExecutionInformationType.class);
		List<PolicyRuleEnforcerHookPreviewOutputType> enforceInfo = modelContext.getHookPreviewResults(PolicyRuleEnforcerHookPreviewOutputType.class);
		displayContainerablesCollection("Approval infos", approvalInfo);
		displayContainerablesCollection("Enforce infos", enforceInfo);
		result.computeStatus();
		tracer.storeTrace(task, result);

		// we do not assert success here, because there are (intentional) exceptions in some of the expressions

		assertEquals("Wrong # of schema execution information pieces", also24 ? 5 : 4, approvalInfo.size());
		assertEquals("Wrong # of enforcement hook preview output items", 1, enforceInfo.size());
		List<EvaluatedPolicyRuleType> enforcementRules = enforceInfo.get(0).getRule();
		if (also24) {
			assertEquals("Wrong # of enforcement rules", 1, enforcementRules.size());
		} else {
			assertEquals("Wrong # of enforcement rules", 0, enforcementRules.size());
		}

		// shortcuts
		final String l1 = userLead21Oid, l2 = userLead22Oid, l3 = userLead23Oid, l4 = userLead24Oid;

		assertApprovalInfo(approvalInfo, roleRole21Oid,
                new ExpectedStagePreview(1, set(l1), set(l1)));
		assertApprovalInfo(approvalInfo, roleRole22Oid,
                new ExpectedStagePreview(1, set(l2), set(l2)));
		assertApprovalInfo(approvalInfo, roleRole23Oid,
                new ExpectedStagePreview(1, set(l3), set(l3)),
                new ExpectedStagePreview(2, set(userSecurityApproverOid), set(userSecurityApproverOid)));
		if (also24) {
			assertApprovalInfo(approvalInfo, roleRole24Oid,
                    new ExpectedStagePreview(1, set(l4), set(l4)));
		}
		assertApprovalInfo(approvalInfo, roleRole25Oid,
                new ExpectedStagePreview(1, set(l1, l2, l3, l4), set(l1, l2, l3, l4)),
                new ExpectedStagePreview(2, set(), set(l3)),
                new ExpectedStagePreview(3, set(orgLeads2122Oid), set(orgLeads2122Oid)),
                new ExpectedStagePreview(4, set(orgLeads2122Oid), set(l1, l2)),
                new ExpectedStagePreview(5, set(l1, l2, l3, l4), set(), APPROVE, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(6, set(l1, l2, l3, l4), set(), APPROVE, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(7, set(l1, l2, l3, l4), set(), SKIP, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(8, set(l1, l2, l3, l4), set(), REJECT, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(9, set(l1, l2, l3, l4), set(l1, l2, l3, l4), true),
                new ExpectedStagePreview(10, set(), set(), REJECT, NO_ASSIGNEES_FOUND));

	}

	private Set<String> set(String... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	private void assertApprovalInfo(List<ApprovalSchemaExecutionInformationType> infos, String targetOid,
			ExpectedStagePreview... expectedStagePreviews) {
		ApprovalSchemaExecutionInformationType found = null;
		for (ApprovalSchemaExecutionInformationType info : infos) {
			assertNotNull("No taskRef", info.getCaseRef());
			PrismObject object = info.getCaseRef().asReferenceValue().getObject();
			assertNotNull("No case in caseRef", object);
			CaseType aCase = (CaseType) object.asObjectable();
			ApprovalContextType wfc = aCase.getApprovalContext();
			assertNotNull("No wf context in caseRef", wfc);
			assertNotNull("No targetRef in caseRef", aCase.getTargetRef());
			if (targetOid.equals(aCase.getTargetRef().getOid())) {
				found = info;
				break;
			}
		}
		assertNotNull("No approval info for target '" + targetOid + "' found", found);
		String taskName = getOrig(found.getCaseRef().getTargetName());
		assertEquals("Wrong # of stage info in " + taskName, expectedStagePreviews.length, found.getStage().size());
		for (int i = 0; i < expectedStagePreviews.length; i++) {
			ExpectedStagePreview expectedStagePreview = expectedStagePreviews[i];
			ApprovalStageExecutionInformationType stagePreview = found.getStage().get(i);
			String pos = taskName + "/" + (i + 1);
			assertNotNull("no stage definition at " + pos, stagePreview.getDefinition());
			assertNotNull("no execution preview at " + pos, stagePreview.getExecutionPreview());
			assertNull("execution record present at " + pos, stagePreview.getExecutionRecord());

			assertEquals("Wrong preview stage number at " + pos, (Integer) (i+1), stagePreview.getNumber());
			assertEquals("Wrong definition stage number at " + pos, (Integer) (i+1), stagePreview.getDefinition().getNumber());

			assertEquals("Stage definition approver ref info differs at " + pos, expectedStagePreview.definitionApproverOids, getOids(stagePreview.getDefinition().getApproverRef()));
			assertEquals("Stage expected approver ref info differs at " + pos, expectedStagePreview.expectedApproverOids, getOids(stagePreview.getExecutionPreview().getExpectedApproverRef()));
			assertEquals("Unexpected outcome at " + pos, expectedStagePreview.outcome, stagePreview.getExecutionPreview().getExpectedAutomatedOutcome());
			assertEquals("Unexpected completion reason at " + pos, expectedStagePreview.reason, stagePreview.getExecutionPreview().getExpectedAutomatedCompletionReason());
			if (expectedStagePreview.hasError) {
				assertNotNull("Error should be present at "+ pos, stagePreview.getExecutionPreview().getErrorMessage());
			} else {
                //noinspection SimplifiedTestNGAssertion
                assertEquals("Error message differs at " + pos, null, stagePreview.getExecutionPreview().getErrorMessage());
			}
		}
	}

	private Set<String> getOids(List<ObjectReferenceType> refs) {
		return new HashSet<>(ObjectTypeUtil.objectReferenceListToOids(refs));
	}

	@SuppressWarnings("SameParameterValue")
    private void executeUnassignRoles123ToJack(String TEST_NAME, boolean immediate, boolean approve, boolean byId, boolean has1and2) throws Exception {
		PrismObject<UserType> jack = getUser(userJackOid);
		AssignmentType a1 = has1and2 ? findAssignmentByTargetRequired(jack, roleRole21Oid) : null;
		AssignmentType a2 = has1and2 ? findAssignmentByTargetRequired(jack, roleRole22Oid) : null;
		AssignmentType a3 = findAssignmentByTargetRequired(jack, roleRole23Oid);
		AssignmentType del1 = toDelete(a1, byId);
		AssignmentType del2 = toDelete(a2, byId);
		AssignmentType del3 = toDelete(a3, byId);
		ObjectDelta<UserType> deleteRole1Delta = has1and2 ? prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).delete(del1)
				.asObjectDelta(userJackOid) : null;
		ObjectDelta<UserType> deleteRole2Delta = has1and2 ? prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).delete(del2)
				.asObjectDelta(userJackOid) : null;
		ObjectDelta<UserType> deleteRole3Delta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_ASSIGNMENT).delete(del3)
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> changeDescriptionDelta = prismContext
				.deltaFor(UserType.class)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
				.summarize(changeDescriptionDelta, deleteRole1Delta, deleteRole2Delta, deleteRole3Delta);
		ObjectDelta<UserType> delta0 = ObjectDeltaCollectionsUtil
				.summarize(changeDescriptionDelta, deleteRole1Delta, deleteRole2Delta);
		String originalDescription = getUser(userJackOid).asObjectable().getDescription();
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() {
				return primaryDelta.clone();
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return singletonList(approve);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return singletonList(deleteRole3Delta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0.clone();
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return singletonList(
                        new ExpectedTask(roleRole23Oid, "Unassigning role \"Role23\" from user \"jack\""));
			}

			// after first step
			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> tasks = getExpectedTasks();
				return singletonList(
                        new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(0))
                );
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(userJackOid, UserType.F_DESCRIPTION, TEST_NAME);
						} else {
							if (originalDescription != null) {
								assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
							}
						}
						if (yes || !has1and2) {
							assertNotAssignedRole(userJackOid, roleRole21Oid, opTask, result);
							assertNotAssignedRole(userJackOid, roleRole22Oid, opTask, result);
						} else {
							assertAssignedRole(userJackOid, roleRole21Oid, opTask, result);
							assertAssignedRole(userJackOid, roleRole22Oid, opTask, result);
						}
						break;
					case 1:
						if (yes) {
							assertNotAssignedRole(userJackOid, roleRole23Oid, opTask, result);
						} else {
							assertAssignedRole(userJackOid, roleRole23Oid, opTask, result);
						}
						break;
					default:
						throw new IllegalArgumentException("Unexpected delta number: " + number);
				}
			}

			@Override
			protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
				return null;            // ignore this way of approving
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				List<ExpectedTask> tasks = getExpectedTasks();
				List<ApprovalInstruction> instructions = new ArrayList<>();
				instructions.add(new ApprovalInstruction(
						new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(0)), approve,
						userSecurityApproverOid, null));
				return instructions;
			}
		}, 1, immediate);
	}

	private AssignmentType toDelete(AssignmentType assignment, boolean byId) {
		if (assignment == null) {
			return null;
		}
		if (!byId) {
			return assignment.clone();
		} else {
			AssignmentType rv = new AssignmentType(prismContext);
			rv.setId(assignment.getId());
			return rv;
		}
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

	private static class ExpectedStagePreview {
		@SuppressWarnings({ "FieldCanBeLocal", "unused" }) private int number;
		private final Set<String> definitionApproverOids;
		private final Set<String> expectedApproverOids;
		private final ApprovalLevelOutcomeType outcome;
		private final AutomatedCompletionReasonType reason;
		private final boolean hasError;

		private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids) {
			this(number, definitionApproverOids, expectedApproverOids, null, null, false);
		}
		private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids,
                ApprovalLevelOutcomeType outcome, AutomatedCompletionReasonType reason) {
			this(number, definitionApproverOids, expectedApproverOids, outcome, reason, false);
		}
		private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids,
                boolean hasError) {
			this(number, definitionApproverOids, expectedApproverOids, null, null, hasError);
		}
		private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids,
                ApprovalLevelOutcomeType outcome, AutomatedCompletionReasonType reason, boolean hasError) {
			this.number = number;
			this.definitionApproverOids = definitionApproverOids;
			this.expectedApproverOids = expectedApproverOids;
			this.outcome = outcome;
			this.reason = reason;
			this.hasError = hasError;
		}
	}
}
