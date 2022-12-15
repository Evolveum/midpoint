/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.security;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collections;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for privacy-enhancing setup. E.g. broad get authorizations, but limited search.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRoleMembers extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security/role-members");

    protected static final File USERS_ROLES_FILE = new File(TEST_DIR, "users-roles.xml");

    protected static final String USER_GUYBRUSH_OID = "df294d8e-cafc-11e8-bb75-6b3aee37f782";
    protected static final String USER_GUYBRUSH_USERNAME = "guybrush";

    protected static final String USER_ELAINE_OID = "e7b4bc54-cafc-11e8-a0e8-cf8010206061";
    protected static final String USER_ELAINE_USERNAME = "elaine";

    protected static final String USER_MANCOMB_OID = "f034084e-cafc-11e8-a31f-f7b3274f7480";
    protected static final String USER_MANCOMB_USERNAME = "mancomb";
    protected static final String USER_MANCOMB_GIVEN_NAME = "Mancomb";
    protected static final String USER_MANCOMB_FULL_NAME = "Mancomb Seepgood";

    protected static final String ROLE_END_USER_OID = "c057bbd0-cafb-11e8-9525-cbcb025548f7";

    protected static final String ROLE_GOVERNOR_OID = "78a76270-cafd-11e8-ba0c-4f7b8e8b4e57";

    protected static final String ROLE_PIRATE_OID = "31d5bdce-cafd-11e8-b41d-b373e6c564cb";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importObjectsFromFileNotRaw(USERS_ROLES_FILE, initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        // WHEN
        when();

        assertUserBefore(USER_GUYBRUSH_OID)
                .assertName(USER_GUYBRUSH_USERNAME)
                .roleMembershipRefs()
                .assertRole(ROLE_END_USER_OID, SchemaConstants.ORG_DEFAULT)
                .assertRoleMemberhipRefs(1);

        assertUserBefore(USER_ELAINE_OID)
                .assertName(USER_ELAINE_USERNAME)
                .roleMembershipRefs()
                .assertRole(ROLE_PIRATE_OID, SchemaConstants.ORG_OWNER)
                .assertRole(ROLE_GOVERNOR_OID, SchemaConstants.ORG_DEFAULT)
                .assertRole(ROLE_END_USER_OID, SchemaConstants.ORG_DEFAULT)
                .assertRoleMemberhipRefs(3);

        assertUserBefore(USER_MANCOMB_OID)
                .assertName(USER_MANCOMB_USERNAME)
                .assertFullName(USER_MANCOMB_FULL_NAME)
                .assertGivenName(USER_MANCOMB_GIVEN_NAME)
                .assignments()
                .assertAssignments(1)
                .assertRole(ROLE_PIRATE_OID)
                .end()
                .roleMembershipRefs()
                .assertRole(ROLE_PIRATE_OID, SchemaConstants.ORG_DEFAULT)
                .assertRoleMemberhipRefs(1);

        assertCanSearchPirateMembers(true);

        ModelContext<UserType> previewContext = previewUser(USER_MANCOMB_OID);
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = previewContext.getEvaluatedAssignmentTriple();
        assertNotNull("Preview evaluated assignment triple is null", evaluatedAssignmentTriple);
        assertFalse("Preview evaluated assignment triple is empty", evaluatedAssignmentTriple.isEmpty());

        // THEN
        then();

    }

    /**
     * MID-4893, MID-4947
     */
    @Test
    public void test100AutzGuybrushNoMembers() throws Exception {
        login(USER_GUYBRUSH_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userMancomb = assertGetAllow(UserType.class, USER_MANCOMB_OID);
        assertUser(userMancomb, "mancomb")
                .assertName(USER_MANCOMB_USERNAME)
                .assertAssignments(0)
                .assertRoleMemberhipRefs(0);

        assertCanSearchPirateMembers(false);

        // Even though canSearch returns false, we can still try the search.
        // The authorization is enforcementStrategy=maySkipOnSearch. And it
        // really gets skipped on search. Therefore we will see mancomb as role
        // member. But we cannot read roleMembershipRef, therefore it won't be
        // in the object.
        SearchResultList<PrismObject<UserType>> members = searchPirateMembers(1);
        assertUser(members.get(0), "pirate role member")
                .assertName(USER_MANCOMB_USERNAME)
                .assertFullName(USER_MANCOMB_FULL_NAME)
                .assertNoGivenName()
                .assertRoleMemberhipRefs(0);

        ModelContext<UserType> previewContext = previewUser(USER_MANCOMB_OID);

        assertUser(previewContext.getFocusContext().getObjectOld(), "preview user old")
                .assertName(USER_MANCOMB_USERNAME)
                .assertFullName(USER_MANCOMB_FULL_NAME)
                .assertNoGivenName();
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = previewContext.getEvaluatedAssignmentTriple();
        assertNull("Preview evaluated assignment triple sneaked in", evaluatedAssignmentTriple);

        // THEN
        then();

    }

    private ModelContext<UserType> previewUser(String userOid) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ObjectDelta<UserType> emptyMancombDelta = deltaFor(UserType.class).asObjectDelta(userOid);
        ModelContext<UserType> previewContext = modelInteractionService.previewChanges(Collections.singleton(emptyMancombDelta), null, task, result);
        displayDumpable("Preview context", previewContext);
        result.computeStatus();
        if (!result.isSuccess() && !result.isHandledError() && !result.isWarning()) {
            display("Unexpected preview result", result);
            fail("Unexpected preview result: " + result.getStatus());
        }
        return previewContext;
    }

    /**
     * MID-4893
     */
    @Test
    public void test105AutzElaineMembers() throws Exception {
        login(USER_ELAINE_USERNAME);

        // WHEN
        when();

        PrismObject<UserType> userMancomb = assertGetAllow(UserType.class, USER_MANCOMB_OID);
        assertUser(userMancomb, "mancomb")
                .assertName(USER_MANCOMB_USERNAME)
                .assertAssignments(0)
                .roleMembershipRefs()
                .assertRole(ROLE_PIRATE_OID)
                .assertRoleMemberhipRefs(1);

        assertCanSearchPirateMembers(true);

        SearchResultList<PrismObject<UserType>> members = searchPirateMembers(1);
        assertUser(members.get(0), "pirate role member")
                .assertName(USER_MANCOMB_USERNAME)
                .roleMembershipRefs()
                .assertRole(ROLE_PIRATE_OID, SchemaConstants.ORG_DEFAULT)
                .assertRoleMemberhipRefs(1);

        // THEN
        then();

    }

    private void assertCanSearchPirateMembers(boolean expected) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertEquals("Wrong canSearch on pirate members", expected, canSearchPirateMembers());
    }

    private boolean canSearchPirateMembers() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(TestRoleMembers.class.getName() + ".canSearchPirateMembers");
        OperationResult result = task.getResult();
        ObjectQuery query = createMembersQuery(ROLE_PIRATE_OID);
        // Object is null here by purpose. Maybe the object does not really makes any sense in canSearch() ?
        boolean canSearch = modelInteractionService.canSearch(UserType.class, null, null, false, query, task, result);
        assertSuccess(result);
        return canSearch;
    }

    private SearchResultList<PrismObject<UserType>> searchPirateMembers(int expectedResults) throws Exception {
        return assertSearch(UserType.class, createMembersQuery(ROLE_PIRATE_OID), expectedResults);
    }

    private ObjectQuery createMembersQuery(String roleOid) {
        return queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(roleOid).build();
    }

}
