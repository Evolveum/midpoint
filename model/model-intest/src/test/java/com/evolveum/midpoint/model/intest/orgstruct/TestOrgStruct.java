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
package com.evolveum.midpoint.model.intest.orgstruct;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOrgStruct extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/orgstruct");

    public static final File ROLE_DEFENDER_FILE = new File(TEST_DIR, "role-defender.xml");
    public static final String ROLE_DEFENDER_OID = "12345111-1111-2222-1111-121212111567";

    public static final File ROLE_META_DEFENDER_FILE = new File(TEST_DIR, "role-meta-defender.xml");
    public static final String ROLE_META_DEFENDER_OID = "12345111-1111-2222-1111-121212111568";

    public static final File ROLE_OFFENDER_FILE = new File(TEST_DIR, "role-offender.xml");
    public static final String ROLE_OFFENDER_OID = "12345111-1111-2222-1111-121212111569";

    public static final File ROLE_OFFENDER_ADMIN_FILE = new File(TEST_DIR, "role-offender-admin.xml");
    public static final String ROLE_OFFENDER_ADMIN_OID = "12345111-1111-2222-1111-121212111566";

    public static final File ROLE_META_DEFENDER_ADMIN_FILE = new File(TEST_DIR, "role-meta-defender-admin.xml");
    public static final String ROLE_META_DEFENDER_ADMIN_OID = "12345111-1111-2222-1111-121212111565";

    public static final File ORG_TEMP_FILE = new File(TEST_DIR, "org-temp.xml");
    public static final String ORG_TEMP_OID = "43214321-4311-0952-4762-854392584320";

    public static final File ORG_FICTIONAL_FILE = new File(TEST_DIR, "org-fictional.xml");
    public static final String ORG_FICTIONAL_OID = "b5b179cc-03c7-11e5-9839-001e8c717e5b";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(ROLE_DEFENDER_FILE);
        addObject(ROLE_META_DEFENDER_FILE);
        addObject(ROLE_META_DEFENDER_ADMIN_FILE);
        addObject(ROLE_OFFENDER_FILE);
        addObject(ROLE_OFFENDER_ADMIN_FILE);
        addObject(USER_HERMAN_FILE);
        setDefaultUserTemplate(USER_TEMPLATE_ORG_ASSIGNMENT_OID);       // used for tests 4xx
        //DebugUtil.setDetailedDebugDump(true);
    }

    @Test
    public void test010AddOrgStruct() throws Exception {
		final String TEST_NAME = "test010AddOrgStruct";
        displayTestTitle(TEST_NAME);

		// Dummy, just to be overridden in subclasses
		addOrgStruct();
	}

	protected void addOrgStruct() throws Exception {
		// Dummy, just to be overridden in subclasses
	}

	@Test
    public void test051OrgStructSanity() throws Exception {
		final String TEST_NAME = "test051OrgStructSanity";
        displayTestTitle(TEST_NAME);

        // WHEN
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test052RootOrgQuery() throws Exception {
		final String TEST_NAME = "test052RootOrgQuery";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(prismContext);

        // WHEN
        List<PrismObject<OrgType>> rootOrgs = modelService.searchObjects(OrgType.class, query, null, task, result);

        // THEN
        assertEquals("Unexpected number of root orgs", 2, rootOrgs.size());

        // Post-condition
        assertMonkeyIslandOrgSanity();
	}

	/**
	 * Scumm bar org also acts as a role, assigning account on dummy resource.
	 */
	@Test
    public void test101JackAssignScummBar() throws Exception {
		final String TEST_NAME = "test101JackAssignScummBar";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // Precondition
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        assignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SCUMM_BAR_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test102JackUnassignScummBar() throws Exception {
		final String TEST_NAME = "test102JackUnassignScummBar";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserNoOrg(userJack);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	/**
	 * Assign jack to both functional and project orgstruct.
	 * Assign both orgs at the same time.
	 */
	@Test
    public void test201JackAssignScummBarAndSaveElaine() throws Exception {
		final String TEST_NAME = "test201JackAssignScummBarAndSaveElaine";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ORG_SCUMM_BAR_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        modifications.add(createAssignmentModification(ORG_SAVE_ELAINE_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
		modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	/**
	 * Assign jack to functional orgstruct again.
	 */
	@Test
    public void test202JackAssignMinistryOfOffense() throws Exception {
		final String TEST_NAME = "test202JackAssignMinistryOfOffense";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_OFFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test207JackUnAssignScummBar() throws Exception {
		final String TEST_NAME = "test207JackUnAssignScummBar";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignOrg(USER_JACK_OID, ORG_SCUMM_BAR_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_OFFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test208JackUnassignAll() throws Exception {
		final String TEST_NAME = "test208JackUnassignAll";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignAllReplace(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserNoOrg(userJack);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

    // besides Offense org assignment, we create also Defender role assignment (which indirectly creates Defense org assignment)
	@Test
    public void test210JackAssignMinistryOfOffenseMember() throws Exception {
		final String TEST_NAME = "test210JackAssignMinistryOfOffenseMember";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        modifications.add(createAssignmentModification(ROLE_DEFENDER_OID, RoleType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test211JackAssignMinistryOfOffenseMinister() throws Exception {
		final String TEST_NAME = "test211JackAssignMinistryOfOffenseMinister";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test212JackUnassignMinistryOfOffenseMember() throws Exception {
		final String TEST_NAME = "test212JackUnassignMinistryOfOffenseMember";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test213JackUnassignMinistryOfOffenseManager() throws Exception {
		final String TEST_NAME = "test213JackUnassignMinistryOfOffenseManager";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedNoOrg(userJack);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_DEFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

    @Test
    public void test220JackAssignMinistryOfOffenseMemberAgain() throws Exception {
        final String TEST_NAME = "test220JackAssignMinistryOfOffenseMemberAgain";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     * Assign jack to both functional and project orgstruct.
     * Implemented to check org struct reconciliation in test223.
     */
    @Test
    public void test221JackAssignScummBarAndSaveElaine() throws Exception {
        final String TEST_NAME = "test221JackAssignScummBarAndSaveElaine";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ORG_SCUMM_BAR_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        modifications.add(createAssignmentModification(ORG_SAVE_ELAINE_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_DEFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }


    @Test
    public void test223JackChangeMinistryOfOffenseMemberToManager() throws Exception {
        final String TEST_NAME = "test221JackChangeMinistryOfOffenseMemberToManager";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> jack = getUser(USER_JACK_OID);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();

        // this is now forbidden
//        Long id = findAssignmentIdForTarget(jack, ORG_MINISTRY_OF_OFFENSE_OID);
//        PrismReferenceDefinition referenceDefinition = getUserDefinition()
//                .findItemDefinition(
//                        new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), PrismReferenceDefinition.class);
//        ReferenceDelta referenceDelta = new ReferenceDelta(
//                new ItemPath(
//                        new NameItemPathSegment(UserType.F_ASSIGNMENT),
//                        new IdItemPathSegment(id),
//                        new NameItemPathSegment(AssignmentType.F_TARGET_REF)), referenceDefinition, prismContext);
//        PrismReferenceValue oldValue = new PrismReferenceValue(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE);
//        PrismReferenceValue newValue = new PrismReferenceValue(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE);
//        newValue.setRelation(SchemaConstants.ORG_MANAGER);
//
//        referenceDelta.addValueToDelete(oldValue);
//        referenceDelta.addValueToAdd(newValue);
//        modifications.add(referenceDelta);

        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, null, null, null, false));
        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER, null, null, true));

        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userJack, ORG_SCUMM_BAR_OID, null);
        assertHasOrg(userJack, ORG_SCUMM_BAR_OID, null);
        assertAssignedOrg(userJack, ORG_SAVE_ELAINE_OID, null);
        assertHasOrg(userJack, ORG_SAVE_ELAINE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }


    /**
     * Recompute jack. Make sure nothing is changed.
     * MID-3384
     */
    @Test
    public void test230JackRecompute() throws Exception {
        final String TEST_NAME = "test230JackRecompute";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertRefs23x();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
    }

    /**
     * Destroy parentOrgRef and roleMembershipRef in the repo. Then recompute.
     * Make sure that the refs are fixed.
     * MID-3384
     */
    @Test
    public void test232JackDestroyRefsAndRecompute() throws Exception {
        final String TEST_NAME = "test232JackDestroyRefsAndRecompute";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        clearUserOrgAndRoleRefs(USER_JACK_OID);

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertRefs23x();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 4);
    }

    /**
     * Destroy parentOrgRef and roleMembershipRef in the repo. Then light recompute.
     * Make sure that the refs are fixed and that the resources were not touched.
     * MID-3384
     */
    @Test
    public void test234JackDestroyRefsAndLightRecompute() throws Exception {
        final String TEST_NAME = "test234JackDestroyRefsAndLightRecompute";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        clearUserOrgAndRoleRefs(USER_JACK_OID);

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

        PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
        partialProcessing.setInbound(PartialProcessingTypeType.SKIP);
        partialProcessing.setObjectTemplateBeforeAssignments(PartialProcessingTypeType.SKIP);
        partialProcessing.setObjectTemplateAfterAssignments(PartialProcessingTypeType.SKIP);
        partialProcessing.setProjection(PartialProcessingTypeType.SKIP);
        partialProcessing.setApprovals(PartialProcessingTypeType.SKIP);
		ModelExecuteOptions options = ModelExecuteOptions.createPartialProcessing(partialProcessing);
		options.setReconcileFocus(true);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(UserType.class, USER_JACK_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertRefs23x();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 0);
    }



    private void assertRefs23x() throws Exception {
    	PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedOrgs(userAfter, ORG_MINISTRY_OF_OFFENSE_OID, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID);
        assertHasOrgs(userAfter, ORG_MINISTRY_OF_OFFENSE_OID, ORG_SCUMM_BAR_OID, ORG_SAVE_ELAINE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userAfter, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userAfter, ORG_SCUMM_BAR_OID, null);
        assertHasOrg(userAfter, ORG_SCUMM_BAR_OID, null);
        assertAssignedOrg(userAfter, ORG_SAVE_ELAINE_OID, null);
        assertHasOrg(userAfter, ORG_SAVE_ELAINE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

	private Long findAssignmentIdForTarget(PrismObject<UserType> user, String targetOid) {
        for (AssignmentType assignmentType : user.asObjectable().getAssignment()) {
            if (assignmentType.getTargetRef() != null && targetOid.equals(assignmentType.getTargetRef().getOid())) {
                return assignmentType.getId();
            }
        }
        throw new IllegalStateException("No assignment pointing to " + targetOid + " found");
    }

    @Test
    public void test300JackUnassignAllOrgs() throws Exception {
        final String TEST_NAME = "test300JackUnassignAllOrgs";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER, null, null, false)));
        modifications.add((createAssignmentModification(ORG_SCUMM_BAR_OID, OrgType.COMPLEX_TYPE, null, null, null, false)));
        modifications.add((createAssignmentModification(ORG_SAVE_ELAINE_OID, OrgType.COMPLEX_TYPE, null, null, null, false)));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);

        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);

        // THEN
        displayThen(TEST_NAME);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedNoOrg(userJack);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
	 * Assign jack to functional orgstruct again. Make him both minister and member (for Defense org i.e. for that which he already has indirect assignment)
	 */
	@Test
    public void test301JackAssignMinistryOfOffense() throws Exception {
		final String TEST_NAME = "test301JackAssignMinistryOfOffense";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

    /**
     * Conflict: removing the role assignment (that should remove org assignment), while keeping explicit org assignment present
     */
    @Test
    public void test305JackConflictZeroAndMinus() throws Exception {
        final String TEST_NAME = "test305JackConflictZeroAndMinus";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(ROLE_DEFENDER_OID, RoleType.COMPLEX_TYPE, null, null, null, false)));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertUserOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     * Another conflict: adding the role assignment (that should add org assignment), while deleting explicit org assignment
     */
    @Test
    public void test307JackConflictPlusAndMinus() throws Exception {
        final String TEST_NAME = "test307JackConflictPlusAndMinus";
        displayTestTitle(TEST_NAME);

        executeConflictPlusAndMinus(TEST_NAME);
    }

    protected void executeConflictPlusAndMinus(String TEST_NAME) throws Exception {
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(ROLE_DEFENDER_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
        modifications.add((createAssignmentModification(ORG_MINISTRY_OF_DEFENSE_OID, OrgType.COMPLEX_TYPE, null, null, null, false)));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertNotAssignedOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, null);

        assertHasOrgs(userJack, ORG_MINISTRY_OF_DEFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    // preparation for test309
    // also tests that when removing indirectly assigned org, it disappears from parentOrgRef
    @Test
    public void test308JackUnassignRoleDefender() throws Exception {
        final String TEST_NAME = "test308JackUnassignRoleDefender";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(ROLE_DEFENDER_OID, RoleType.COMPLEX_TYPE, null, null, null, false)));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_DEFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertNotAssignedOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, null);
        assertNotAssignedRole(userJack, ROLE_DEFENDER_OID);

        assertHasOrgs(userJack, ORG_MINISTRY_OF_DEFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     * Retrying last kind of conflict: adding the role assignment (that should add org assignment),
     * while deleting explicit org assignment - even it is not there!
     *
     * So this time there is originally NO parentOrgRef to Ministry of Defense/null.
     *
     * This situation is a kind of abnormal, but deleting non-present value is considered to be legal.
     * So we should treat a situation like this well.
     */
    @Test
    public void test309JackConflictPlusAndMinusAgain() throws Exception {
        final String TEST_NAME = "test309JackConflictPlusAndMinusAgain";
        displayTestTitle(TEST_NAME);

        executeConflictPlusAndMinus(TEST_NAME);
    }

    /**
     * MID-3874
     */
    @Test
    public void test310JackConflictParentOrgRefAndAssignmentsAddOrg() throws Exception {
        final String TEST_NAME = "test310JackConflictParentOrgRefAndAssignmentsAddOrg";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createObject(OrgType.class, "Cheaters");
        orgBefore.asObjectable().parentOrgRef(ORG_SCUMM_BAR_OID, OrgType.COMPLEX_TYPE);
        display("Org before");

        try {
	        // WHEN
	        displayWhen(TEST_NAME);
	        addObject(orgBefore, task, result);

	        assertNotReached();
        } catch (PolicyViolationException e) {
        	// THEN
        	displayThen(TEST_NAME);
        	display("Expected exception", e);
        	assertFailure(result);
        }

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    // TODO: modify org: add parentOrgRef, removeParentOrgRef

    /**
	 * Delete jack while he is still assigned.
	 */
	@Test
    public void test349DeleteJack() throws Exception {
		final String TEST_NAME = "test349DeleteJack";
        displayTestTitle(TEST_NAME);

        executeDeleteJack(TEST_NAME);
	}

    protected void executeDeleteJack(String TEST_NAME) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        try {
        	PrismObject<UserType> user = getUser(USER_JACK_OID);
        	AssertJUnit.fail("Jack survived!");
        } catch (ObjectNotFoundException e) {
        	// This is expected
        }
    }

    /**
	 * Add new user Jack with an assignments as an manager and also a member of ministry of offense.
	 */
	@Test
    public void test350AddJackAsMinistryOfOffenseManager() throws Exception {
		final String TEST_NAME = "test350AddJackAsMinistryOfOffenseManager";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);

        AssignmentType assignmentType = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(ORG_MINISTRY_OF_OFFENSE_OID);
        targetRef.setType(OrgType.COMPLEX_TYPE);
		assignmentType.setTargetRef(targetRef);
		userJack.asObjectable().getAssignment().add(assignmentType);

        assignmentType = new AssignmentType();
        targetRef = new ObjectReferenceType();
        targetRef.setOid(ORG_MINISTRY_OF_OFFENSE_OID);
        targetRef.setType(OrgType.COMPLEX_TYPE);
        targetRef.setRelation(SchemaConstants.ORG_MANAGER);
		assignmentType.setTargetRef(targetRef);
		userJack.asObjectable().getAssignment().add(assignmentType);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userJack.createAddDelta());
		// WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);

        assertManager(USER_JACK_OID, null, null, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test360ElaineAssignGovernor() throws Exception {
		final String TEST_NAME = "test360ElaineAssignGovernor";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_ELAINE_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER, task, result);

        // THEN
        PrismObject<UserType> userElaine = getUser(USER_ELAINE_OID);
        display("User jack after", userElaine);
        assertAssignedOrgs(userElaine, ORG_GOVERNOR_OFFICE_OID);
        assertHasOrgs(userElaine, ORG_GOVERNOR_OFFICE_OID);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);

        assertManager(USER_JACK_OID, USER_ELAINE_OID, null, false, result);
        assertManager(USER_JACK_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_ELAINE_OID, null, null, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_ELAINE_OID, null, null, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test362ElaineAssignGovernmentMember() throws Exception {
		final String TEST_NAME = "test362ElaineAssignGovernmentMember";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_ELAINE_OID, ORG_GOVERNOR_OFFICE_OID, null, task, result);

        // THEN
        PrismObject<UserType> userElaine = getUser(USER_ELAINE_OID);
        display("User jack after", userElaine);
        assertAssignedOrgs(userElaine, ORG_GOVERNOR_OFFICE_OID, ORG_GOVERNOR_OFFICE_OID);
        assertHasOrgs(userElaine, ORG_GOVERNOR_OFFICE_OID, ORG_GOVERNOR_OFFICE_OID);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID);
        assertHasOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);

        assertManager(USER_JACK_OID, USER_ELAINE_OID, null, false, result);
        assertManager(USER_JACK_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_ELAINE_OID, null, null, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, null, true, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test365GuybrushAssignSwashbucklerMember() throws Exception {
		final String TEST_NAME = "test365GuybrushAssignSwashbucklerMember";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_GUYBRUSH_OID, ORG_SWASHBUCKLER_SECTION_OID, null, task, result);

        // THEN
        PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
        display("User jack after", userGuybrush);
        assertAssignedOrgs(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID);
        assertHasOrgs(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID);
        assertAssignedOrg(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID);
        assertHasOrg(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID);

        assertManager(USER_JACK_OID, USER_ELAINE_OID, null, false, result);
        assertManager(USER_JACK_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_ELAINE_OID, null, null, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, null, true, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, null, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test368GuybrushAssignSwashbucklerManager() throws Exception {
		final String TEST_NAME = "test368GuybrushAssignSwashbucklerManager";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_GUYBRUSH_OID, ORG_SWASHBUCKLER_SECTION_OID, SchemaConstants.ORG_MANAGER, task, result);

        // THEN
        PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
        display("User jack after", userGuybrush);
        assertAssignedOrgs(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID, ORG_SWASHBUCKLER_SECTION_OID);
        assertHasOrgs(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID, ORG_SWASHBUCKLER_SECTION_OID);
        assertAssignedOrg(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID);
        assertAssignedOrg(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID);
        assertHasOrg(userGuybrush, ORG_SWASHBUCKLER_SECTION_OID, SchemaConstants.ORG_MANAGER);

        assertManager(USER_JACK_OID, USER_ELAINE_OID, null, false, result);
        assertManager(USER_JACK_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_ELAINE_OID, null, null, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, null, true, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, null, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_GUYBRUSH_OID, null, true, result);
        assertManager(USER_GUYBRUSH_OID, USER_GUYBRUSH_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test370BarbossaAssignOffenseMember() throws Exception {
		final String TEST_NAME = "test370BarbossaAssignOffenseMember";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignOrg(USER_BARBOSSA_OID, ORG_MINISTRY_OF_OFFENSE_OID, null, task, result);

        // THEN
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        display("User jack after", userBarbossa);
        assertAssignedOrgs(userBarbossa, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userBarbossa, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userBarbossa, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrg(userBarbossa, ORG_MINISTRY_OF_OFFENSE_OID);

        assertManager(USER_JACK_OID, USER_ELAINE_OID, null, false, result);
        assertManager(USER_JACK_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_ELAINE_OID, null, null, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, null, true, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, null, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_GUYBRUSH_OID, null, true, result);
        assertManager(USER_GUYBRUSH_OID, USER_GUYBRUSH_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, null, false, result);
        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_BARBOSSA_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_BARBOSSA_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

	@Test
    public void test372HermanAssignSwashbucklerMember() throws Exception {
		final String TEST_NAME = "test365GuybrushAssignSwashbucklerMember";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        PrismObject<UserType> userHerman = getUser(USER_HERMAN_OID);
        assertHasNoOrg(userHerman);

        // WHEN
        assignOrg(USER_HERMAN_OID, ORG_SWASHBUCKLER_SECTION_OID, null, task, result);

        // THEN
        userHerman = getUser(USER_HERMAN_OID);
        display("User jack after", userHerman);
        assertAssignedOrgs(userHerman, ORG_SWASHBUCKLER_SECTION_OID);
        assertHasOrgs(userHerman, ORG_SWASHBUCKLER_SECTION_OID);
        assertAssignedOrg(userHerman, ORG_SWASHBUCKLER_SECTION_OID);
        assertHasOrg(userHerman, ORG_SWASHBUCKLER_SECTION_OID);

        assertManager(USER_JACK_OID, USER_ELAINE_OID, null, false, result);
        assertManager(USER_JACK_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_JACK_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_JACK_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_ELAINE_OID, null, null, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, null, true, result);
        assertManager(USER_ELAINE_OID, USER_ELAINE_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_ELAINE_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, null, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_GUYBRUSH_OID, USER_GUYBRUSH_OID, null, true, result);
        assertManager(USER_GUYBRUSH_OID, USER_GUYBRUSH_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_GUYBRUSH_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, null, false, result);
        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_BARBOSSA_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, null, true, result);
        assertManager(USER_BARBOSSA_OID, USER_JACK_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_BARBOSSA_OID, null, ORG_TYPE_PROJECT, true, result);

        assertManager(USER_HERMAN_OID, USER_GUYBRUSH_OID, null, false, result);
        assertManager(USER_HERMAN_OID, USER_GUYBRUSH_OID, ORG_TYPE_FUNCTIONAL, false, result);
        assertManager(USER_HERMAN_OID, null, ORG_TYPE_PROJECT, false, result);
        assertManager(USER_HERMAN_OID, USER_GUYBRUSH_OID, null, true, result);
        assertManager(USER_HERMAN_OID, USER_GUYBRUSH_OID, ORG_TYPE_FUNCTIONAL, true, result);
        assertManager(USER_HERMAN_OID, null, ORG_TYPE_PROJECT, true, result);

        // Postcondition
        assertMonkeyIslandOrgSanity();
	}

    @Test
    public void test399DeleteJack() throws Exception {
        final String TEST_NAME = "test399DeleteJack";
        displayTestTitle(TEST_NAME);

        executeDeleteJack(TEST_NAME);
    }

    /**
     * Add new user Jack with an assignments as an manager and also a member of ministry of offense.
     */
    @Test
    public void test400AddJackWithOrgUnit() throws Exception {
        final String TEST_NAME = "test400AddJackWithOrgUnit";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        OrgType minOffense = getObject(OrgType.class, ORG_MINISTRY_OF_OFFENSE_OID).asObjectable();

        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().getOrganizationalUnit().add(minOffense.getName());

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userJack.createAddDelta());
        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test402JackChangeMinistryOfOffenseMemberToManagerByAddingRemovingAssignment() throws Exception {
        final String TEST_NAME = "test402JackChangeMinistryOfOffenseMemberToManagerByAddingRemovingAssignment";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> jack = getUser(USER_JACK_OID);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();

        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, null, null, null, false));
        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER, null, null, true));

        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        // No assignment from object template. The object template mapping is normal. It will NOT be applied
        // because there is primary delta.
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);  // because of the modification
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, SchemaConstants.ORG_MANAGER);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test404JackChangeMinistryOfOffenseManagerToMemberByAddingRemovingAssignment() throws Exception {
        final String TEST_NAME = "test404JackChangeMinistryOfOffenseManagerToMemberByAddingRemovingAssignment";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> jack = getUser(USER_JACK_OID);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();

        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER, null, null, false));
        modifications.add(createAssignmentModification(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE, null, null, null, true));

        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);                         // because of object template and the modification
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test409DeleteJack() throws Exception {
        final String TEST_NAME = "test409DeleteJack";
        displayTestTitle(TEST_NAME);

        executeDeleteJack(TEST_NAME);
    }

    /**
     * Add new user Jack with an assignments as an manager and also a member of ministry of offense.
     * (copied from test400)
     */
    @Test
    public void test410AddJackWithOrgUnit() throws Exception {
        final String TEST_NAME = "test400AddJackWithOrgUnit";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        OrgType minOffense = getObject(OrgType.class, ORG_MINISTRY_OF_OFFENSE_OID).asObjectable();

        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().getOrganizationalUnit().add(minOffense.getName());

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userJack.createAddDelta());
        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID, null);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    // this test should generate a SchemaException (modifying targetRef in assignment should be prohibited)
    @Test
    public void test412JackChangeMinistryOfOffenseMemberToManagerByModifyingAssignment() throws Exception {
        final String TEST_NAME = "test412JackChangeMinistryOfOffenseMemberToManagerByModifyingAssignment";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        Long id = findAssignmentIdForTarget(jack, ORG_MINISTRY_OF_OFFENSE_OID);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();

        PrismReferenceDefinition referenceDefinition = getUserDefinition()
                .findItemDefinition(
                        new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), PrismReferenceDefinition.class);
        ReferenceDelta referenceDelta = new ReferenceDelta(
                new ItemPath(
                        new NameItemPathSegment(UserType.F_ASSIGNMENT),
                        new IdItemPathSegment(id),
                        new NameItemPathSegment(AssignmentType.F_TARGET_REF)), referenceDefinition, prismContext);
        PrismReferenceValue oldValue = new PrismReferenceValue(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE);
        PrismReferenceValue newValue = new PrismReferenceValue(ORG_MINISTRY_OF_OFFENSE_OID, OrgType.COMPLEX_TYPE);
        newValue.setRelation(SchemaConstants.ORG_MANAGER);

        referenceDelta.addValueToDelete(oldValue);
        referenceDelta.addValueToAdd(newValue);
        modifications.add(referenceDelta);

        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        try {
            modelService.executeChanges(deltas, null, task, result);
            AssertJUnit.fail("executeChanges should fail but it did not.");
        } catch (SchemaException e) {
            // ok!
        } catch (Exception e) {
            AssertJUnit.fail("executeChanges failed in the wrong way (expected SchemaException): " + e);
        }
    }

    // import temp org + assign to jack (preparation for the next test)
    @Test
    public void test420JackAssignTempOrg() throws Exception {
        final String TEST_NAME = "test420JackAssignTempOrg";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        addObject(ORG_TEMP_FILE);

        // WHEN
        assignOrg(USER_JACK_OID, ORG_TEMP_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_TEMP_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_TEMP_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    // delete the org and then unassign it
    @Test
    public void test425JackUnassignDeletedOrg() throws Exception {
        final String TEST_NAME = "test425JackUnassignDeletedOrg";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        deleteObject(OrgType.class, ORG_TEMP_OID, task, result);

        // WHEN
        displayWhen(TEST_NAME);
        unassignOrg(USER_JACK_OID, ORG_TEMP_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("result", result);
        result.computeStatus();
        TestUtil.assertSuccess(result, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test430JackAssignMetaroleOffender() throws Exception {
    	final String TEST_NAME = "test430JackAssignMetaroleOffender";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("User jack before", userJackBefore);
        assertAssignedOrgs(userJackBefore, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJackBefore, ORG_MINISTRY_OF_OFFENSE_OID);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ROLE_OFFENDER_OID, RoleType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);


        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test431JackAssignMetaroleOffenderAdmin() throws Exception {
    	final String TEST_NAME = "test431JackAssignMetaroleOffenderAdmin";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ROLE_OFFENDER_ADMIN_OID, RoleType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);


        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        MidPointAsserts.assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test437JackUnassignOffender() throws Exception {
        final String TEST_NAME = "test437JackUnassignOffender";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("user before", userBefore);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_OFFENDER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID, ORG_MINISTRY_OF_DEFENSE_OID);
        MidPointAsserts.assertHasOrg(userJack, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test438JackUnassignOffenderAdmin() throws Exception {
        final String TEST_NAME = "test438JackUnassignOffenderAdmin";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("user before", userBefore);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_OFFENDER_ADMIN_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertAssignedOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userJack, ORG_MINISTRY_OF_OFFENSE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    @Test
    public void test439JackCleanup() throws Exception {
        final String TEST_NAME = "test439JackCleanup";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User jack after", userJack);
        assertNoAssignments(userJack);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     * MID-3545
     */
    @Test
    public void test440JackModifyEmployeeTypeRolePirate() throws Exception {
        final String TEST_NAME = "test440JackModifyEmployeeTypeRolePirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("user before", userBefore);
        assertNoAssignments(userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, task, result, "ROLE:Pirate");

        // THEN
        displayThen(TEST_NAME);
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     * MID-3545
     */
    @Test
    public void test441JackModifyEmployeeTypeRoleCaptain() throws Exception {
        final String TEST_NAME = "test441JackModifyEmployeeTypeRoleCaptain";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("user before", userBefore);

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, task, result, "ROLE:Captain");

        // THEN
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     * MID-3545
     */
    @Test
    public void test443JackModifyEmployeeTypeRoleNotExist() throws Exception {
        final String TEST_NAME = "test443JackModifyEmployeeTypeRoleNotExist";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("user before", userBefore);

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, task, result, "ROLE:TheRoleThatDoesNotExist");

        // THEN
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertAssignedRole(userAfter, ROLE_EMPTY_OID);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }


    /**
     * MID-3545
     */
    @Test
    public void test449JackModifyEmployeeTypeNull() throws Exception {
        final String TEST_NAME = "test449JackModifyEmployeeTypeNull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("user before", userBefore);

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, task, result);

        // THEN
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertNoAssignments(userAfter);

        // Postcondition
        assertMonkeyIslandOrgSanity();
    }

    /**
     *  Now let's test working with assignments when there is an object template that prescribes an org assignment
     *  based on organizationalUnit property.
     *
     */

    @Test
    public void test799DeleteJack() throws Exception {
        final String TEST_NAME = "test799DeleteJack";
        displayTestTitle(TEST_NAME);

        executeDeleteJack(TEST_NAME);
    }


    // BEWARE, tests 800+ are executed in TestOrgStructMeta, so this class has to end with test799 and no jack present
    // ---------------------------------------------------------------------------------------------------------------

    protected void assertUserOrg(PrismObject<UserType> user, String... orgOids) throws Exception {
		for (String orgOid: orgOids) {
			assertAssignedOrg(user, orgOid);
	        assertHasOrg(user, orgOid);
		}
		assertHasOrgs(user, orgOids.length);
	}

	protected void assertUserNoOrg(PrismObject<UserType> user) throws Exception {
		assertAssignedNoOrg(user);
        assertHasNoOrg(user);
        assertAssignments(user, 0);
        assertHasOrgs(user, 0);

	}

	private void assertManager(String userOid, String managerOid, String orgType, boolean allowSelf, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<UserType> user = getUser(userOid);
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(null, result));
		Collection<UserType> managers = libraryMidpointFunctions.getManagers(user.asObjectable(), orgType, allowSelf);
		ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		if (managerOid == null) {
			if (managers == null || managers.isEmpty()) {
				return;
			} else {
				AssertJUnit.fail("Expected no manager for "+user+", but got "+managers);
			}
		} else {
			if (managers == null) {
				AssertJUnit.fail("Expected manager for "+user+", but got no manager");
			} if (managers.size() != 1) {
				AssertJUnit.fail("Expected one manager for "+user+", but got: "+managers);
			} else {
				UserType manager = managers.iterator().next();
				if (manager.getOid().equals(managerOid)) {
					return;
				} else {
					AssertJUnit.fail("Expected manager with OID "+managerOid+" for "+user+", but got "+manager);
				}
			}
		}
	}

}
