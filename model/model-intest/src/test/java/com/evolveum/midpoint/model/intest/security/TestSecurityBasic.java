/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.test.TestObject;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.test.SearchAssertion;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.OperationResultRepoSearchAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityBasic extends AbstractInitializedSecurityTest {

    public static boolean unauthorizedScriptRun = false;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test200AutzJackNoRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertNoAccess(userJack);
        assertGlobalStateUntouched();
    }

    @Test
    public void test201AutzJackSuperuserRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SUPERUSER.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertSuperuserAccess(NUMBER_OF_ALL_USERS);

        Collection<SelectorOptions<GetOperationOptions>> withCases =
                SchemaService.get().getOperationOptionsBuilder()
                        .item(AccessCertificationCampaignType.F_CASE).retrieve().build();
        assertSearch(AccessCertificationCampaignType.class, null, withCases, new SearchAssertion<>() {

            public void assertObjects(String message, List<PrismObject<AccessCertificationCampaignType>> objects) {
                for (PrismObject<AccessCertificationCampaignType> obj : objects) {
                    assertFalse(obj.asObjectable().getCase().isEmpty());
                }
            }

            public void assertCount(int count) {
            }

        });
        assertGlobalStateUntouched();
    }

    @Test
    public void test202AutzJackReadonlyRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();
        assertReadDenyRaw();
        assertAddDeny();
        assertModifyDeny();
        assertEmptyDeltaExecutionNotAuthorized(UserType.class, USER_GUYBRUSH_OID);
        assertDeleteDeny();

        assertReadCertCasesAllow();
        assertReadCasesAllow();

        assertGlobalStateUntouched();

        assertAuditReadDeny();
    }

    /**
     * Authorized only for request but not execution. Everything should be denied.
     */
    @Test
    public void test202rAutzJackReadonlyReqRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_REQ.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();

        assertAuditReadDeny();
    }

    /**
     * Authorized only for execution but not request. Everything should be denied.
     */
    @Test
    public void test202eAutzJackReadonlyExecRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_EXEC.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();

        assertAuditReadDeny();
    }

    @Test
    public void test202reAutzJackReadonlyReqExecRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();
        assertReadDenyRaw();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();

        assertAuditReadDeny();
    }

    @Test
    public void test203AutzJackReadonlyDeepRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();
        assertReadDenyRaw();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();

        assertAuditReadDeny();
    }

    @Test
    public void test203eAutzJackReadonlyDeepExecRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();
        assertReadDenyRaw();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    @Test
    public void test204AutzJackSelfRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF.oid);
        assignRole(USER_JACK_OID, ROLE_CASES_REQUESTOR_SELF.oid);
        assignRole(USER_JACK_OID, ROLE_READ_JACKS_CAMPAIGNS.oid); // we cannot specify "own campaigns" yet

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertReadDenyRaw();

        assertVisibleUsers(1);
        assertSearch(OrgType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        // The search with ResourceObjectClass is important. It is a very different case
        // than searching just for UserType
        assertSearch(ObjectType.class, null, 3); // user + campaign + case1

        assertGetDeny(RoleType.class, ROLE_ORDINARY.oid);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDenyRaw();

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        assertGetAllow(CaseType.class, CASE1.oid);
        assertGetDeny(CaseType.class, CASE2.oid);
        assertGetDeny(CaseType.class, CASE3.oid);
        assertGetDeny(CaseType.class, CASE4.oid);
        assertSearchCases(CASE1.oid);

        // There are two visible search cases (because they reside in jack's campaigns).
        // We will try searching for them using various filters.
        assertSearchCertCases(2); // searching for all cases
        assertSearchCertCases(queryForCertCasesByCampaignOwner(USER_JACK_OID), 2);
        assertSearchCertCases(queryForCertCasesByStageNumber(1), 2);
        assertSearchCertCases(queryForCertCasesByWorkItemOutcome(MODEL_CERTIFICATION_OUTCOME_ACCEPT), 1);
        assertSearchCertWorkItems(
                queryFor(AccessCertificationWorkItemType.class)
                        .filter(
                                CertCampaignTypeUtil.createWorkItemsForCampaignQuery(CAMPAIGN2.oid).getFilter())
                        .and().item(AccessCertificationWorkItemType.F_ASSIGNEE_REF).ref(USER_ADMINISTRATOR_OID)
                        .and().item(AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                        .build(),
                2);

        assertGlobalStateUntouched();
    }

    @Test
    public void test204aAutzJackCaseObjectSelfRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF.oid);
        assignRole(USER_JACK_OID, ROLE_CASES_OBJECT_SELF.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertReadDenyRaw();

        assertVisibleUsers(1);
        assertSearch(OrgType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        // The search with ResourceObjectClass is important. It is a very different case
        // than searching just for UserType
        assertSearch(ObjectType.class, null, 2); // user + case2

        assertGetDeny(RoleType.class, ROLE_ORDINARY.oid);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDenyRaw();

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        assertGetDeny(CaseType.class, CASE1.oid);
        assertGetAllow(CaseType.class, CASE2.oid);
        assertGetDeny(CaseType.class, CASE3.oid);
        assertGetDeny(CaseType.class, CASE4.oid);
        assertReadCertCasesDeny();
        assertSearchCases(CASE2.oid);

        assertGlobalStateUntouched();
    }

    @Test
    public void test204bAutzJackCaseAssigneeSelfRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF.oid);
        assignRole(USER_JACK_OID, ROLE_CASES_ASSIGNEE_SELF.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertReadDenyRaw();

        assertVisibleUsers(1);
        assertSearch(OrgType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        // The search with ResourceObjectClass is important. It is a very different case
        // than searching just for UserType
        assertSearch(ObjectType.class, null, 3); // user + case3 + case4 (see below)

        assertGetDeny(RoleType.class, ROLE_ORDINARY.oid);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid);

        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
        assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDenyRaw();

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        assertGetDeny(CaseType.class, CASE1.oid);
        assertGetDeny(CaseType.class, CASE2.oid);
        assertGetAllow(CaseType.class, CASE3.oid); // jack is current assignee
        assertGetAllow(CaseType.class, CASE4.oid); // jack is assignee whose work item was completed
        assertReadCertCasesDeny();
        assertSearchCases(CASE3.oid, CASE4.oid);

        assertGlobalStateUntouched();
    }

    @Test
    public void test204cAutzJackCaseAssigneeSelfWithDelegatesRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_DELEGABLE.oid);
        assignRole(USER_JACK_OID, ROLE_CASES_ASSIGNEE_SELF.oid);

        USER_DEPUTY_1.init(this, task, result);
        try {
            login(USER_DEPUTY_1);

            // WHEN
            when();

            assertGetDeny(UserType.class, USER_JACK_OID);
            assertGetAllow(UserType.class, USER_DEPUTY_1.oid);
            assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
            assertReadDenyRaw();

            assertVisibleUsers(1);
            assertSearch(OrgType.class, null, 0);
            assertSearch(RoleType.class, null, 0);
            // The search with ResourceObjectClass is important. It is a very different case
            // than searching just for UserType
            assertSearch(ObjectType.class, null, 3); // user + case3 + case4

            assertGetDeny(RoleType.class, ROLE_ORDINARY.oid);
            assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid);

            assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
            assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

            assertAddDeny();

            assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX,
                    PolyString.fromOrig("Captain"));
            assertModifyAllow(UserType.class, USER_DEPUTY_1.oid, UserType.F_HONORIFIC_PREFIX,
                    PolyString.fromOrig("Captain"));
            assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX,
                    PolyString.fromOrig("Pirate"));
            assertModifyDenyRaw();

            assertDeleteDeny();
            assertDeleteDeny(UserType.class, USER_JACK_OID);

            assertGetDeny(CaseType.class, CASE1.oid);
            assertGetDeny(CaseType.class, CASE2.oid);
            assertGetAllow(CaseType.class, CASE3.oid);
            assertGetAllow(CaseType.class, CASE4.oid); // see previous test
            assertReadCertCasesDeny();
            assertSearchCases(CASE3.oid, CASE4.oid);

            assertGlobalStateUntouched();
        } finally {
            deleteObjectRepo(UserType.class, USER_DEPUTY_1.oid); // faster than attempting to do this in each cleanup; todo reconsider
        }
    }

    @Test
    public void test204dAutzJackCaseAssigneeSelfWithNonWorkItemsDelegatesRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_DELEGABLE.oid);
        assignRole(USER_JACK_OID, ROLE_CASES_ASSIGNEE_SELF.oid);

        addObject(USER_DEPUTY_2, task, result);
        try {
            login(USER_DEPUTY_2);

            // WHEN
            when();

            assertGetDeny(UserType.class, USER_JACK_OID);
            assertGetAllow(UserType.class, USER_DEPUTY_2.oid);
            assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
            assertReadDenyRaw();

            assertVisibleUsers(1);
            assertSearch(OrgType.class, null, 0);
            assertSearch(RoleType.class, null, 0);
            // The search with ResourceObjectClass is important. It is a very different case
            // than searching just for UserType
            assertSearch(ObjectType.class, null, 1);        // user

            assertGetDeny(RoleType.class, ROLE_ORDINARY.oid);
            assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN.oid);

            assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_ORDINARY.oid), 0);
            assertSearch(UserType.class, createMembersQuery(UserType.class, ROLE_APPROVER_UNASSIGN_ROLES.oid), 0);

            assertAddDeny();

            assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX,
                    PolyString.fromOrig("Captain"));
            assertModifyAllow(UserType.class, USER_DEPUTY_2.oid, UserType.F_HONORIFIC_PREFIX,
                    PolyString.fromOrig("Captain"));
            assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX,
                    PolyString.fromOrig("Pirate"));
            assertModifyDenyRaw();

            assertDeleteDeny();
            assertDeleteDeny(UserType.class, USER_JACK_OID);

            assertGetDeny(CaseType.class, CASE1.oid);
            assertGetDeny(CaseType.class, CASE2.oid);
            assertGetDeny(CaseType.class, CASE3.oid);
            assertGetDeny(CaseType.class, CASE4.oid);
            assertReadCertCasesDeny();
            assertSearchCases();

            assertGlobalStateUntouched();
        } finally {
            deleteObjectRepo(UserType.class, USER_DEPUTY_2.oid);        // faster than attempting to do this in each cleanup; todo reconsider
        }
    }

    @Test
    public void test205AutzJackObjectFilterModifyCaribbeanRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();
        assertReadDenyRaw();

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    @Test
    public void test207AutzJackObjectFilterCaribbeanRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_CARIBBEAN.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertReadDenyRaw();

        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 2);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));
        assertModifyDenyRaw();

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    @Test
    public void test207rAutzJackObjectFilterCaribbeanRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_CARIBBEAN_RAW.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearch(UserType.class, null, 2);
        assertSearchRaw(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 2);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, executeOptions().raw(), PolyString.fromOrig("Raw Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));
        assertModifyAllowOptions(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, executeOptions().raw(), PolyString.fromOrig("Raw Mutineer"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * MID-5245
     */
    @Test
    public void test208AutzJackReadSomeRoles() throws Exception {
        testAutzJackReadSomeRoles(ROLE_READ_SOME_ROLES.oid);

        assertNewRoleGetAllow(ARCHETYPE_BUSINESS_ROLE);
        assertNewRoleGetAllow(ARCHETYPE_APPLICATION_ROLE);
        assertNewRoleGetDeny(ARCHETYPE_PERSONA_ROLE);
    }

    /**
     * MID-3647
     */
    @Test
    public void test208sAutzJackReadSomeRoles() throws Exception {
        testAutzJackReadSomeRoles(ROLE_READ_SOME_ROLES_SUBTYPE.oid);
    }

    private void testAutzJackReadSomeRoles(String roleOid) throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, roleOid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadDeny();
        assertReadDenyRaw();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertSearch(UserType.class, null, 0);
        assertSearch(RoleType.class, null, 5);

        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertGetDeny(RoleType.class, ROLE_SUPERUSER.oid);
        assertGetDeny(RoleType.class, ROLE_SELF.oid);
        assertGetDeny(RoleType.class, ROLE_ASSIGN_APPLICATION_ROLES.oid);

        assertGetAllow(RoleType.class, ROLE_APPLICATION_1.oid);
        assertGetAllow(RoleType.class, ROLE_APPLICATION_2.oid);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_1.oid);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_2.oid);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_3.oid);

        assertGlobalStateUntouched();
    }

    private void assertNewRoleGetAllow(TestObject<ArchetypeType> archetype) throws CommonException {
        assertNewRoleGet(archetype, true);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertNewRoleGetDeny(TestObject<ArchetypeType> archetype) throws CommonException {
        assertNewRoleGet(archetype, false);
    }

    private void assertNewRoleGet(TestObject<ArchetypeType> archetype, boolean expected) throws CommonException {
        var role = new RoleType()
                .assignment(archetype.assignmentTo())
                .asPrismObject();
        PrismObjectDefinition<RoleType> def = getEditObjectDefinition(role);
        var canRead = def.findItemDefinition(RoleType.F_NAME).canRead();
        assertThat(canRead).as("canRead role name for " + archetype).isEqualTo(expected);
    }

    /**
     * MID-5002
     */
    @Test
    public void test209AutzJackSuperuserAndGuiAccessRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SUPERUSER.oid);
        assignRole(USER_JACK_OID, ROLE_APPROVER.oid);
        login(USER_JACK_USERNAME);

        // WHEN
        assertSuperuserAccess(NUMBER_OF_ALL_USERS);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3126
     */
    @Test
    public void test210AutzJackPropReadAllModifySome() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();
        assertReadDenyRaw();

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, ext("loot"), 888);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, ext("ship"), "Interceptor");
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, ext("weapon"), "sword");

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, ext("colors"), "red");

        assertModifyDenyRaw();

        assertDeleteDeny();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        assertJackEditSchemaReadAllModifySome(userJack);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3126
     */
    @Test
    public void test211AutzJackPropReadAllModifySomeUser() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_USER.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertReadDenyRaw();

        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyDenyRaw(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Raw Captain Jack Sparrow"));
        assertModifyDenyPartial(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Partial Captain Jack Sparrow"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyDenyRaw(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Raw Pirate wannabe");
        assertModifyDenyPartial(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Raw Pirate wannabe");

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertDeleteDeny();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        assertJackEditSchemaReadAllModifySome(userJack);

        assertGlobalStateUntouched();
    }

    /**
     * MID-4101
     */
    @Test
    public void test212AutzJackPropReadAllModifySomeUserPartial() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_USER_PARTIAL.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertReadDenyRaw();

        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyDenyRaw(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Raw Captain Jack Sparrow"));
        assertModifyAllowPartial(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Partial Captain Jack Sparrow"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyDenyRaw(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Raw Pirate wannabe");
        assertModifyDenyPartial(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Raw Pirate wannabe");

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertDeleteDeny();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        assertJackEditSchemaReadAllModifySome(userJack);

        assertGlobalStateUntouched();
    }

    private void assertJackEditSchemaReadAllModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), true, false, false);
    }

    @Test
    public void test215AutzJackPropReadSomeModifySome() throws Exception {
        testAutzJackPropReadSomeModifySome(ROLE_PROP_READ_SOME_MODIFY_SOME.oid);
    }

    @Test
    public void test215reAutzJackPropReadSomeModifySomeReqExec() throws Exception {
        testAutzJackPropReadSomeModifySome(ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC.oid);
    }

    /**
     * MID-3126
     */
    @Test
    public void test216AutzJackPropReadSomeModifySomeUser() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER.oid);
        login(USER_JACK_USERNAME);

        doReadSomeModifySomeUser();
    }

    /**
     * Same as test216AutzJackPropReadSomeModifySomeUser, but with get+search instead of read.
     */
    @Test
    public void test217AutzJackPropGetSearchSomeModifySomeUser() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_GET_SEARCH_SOME_MODIFY_SOME_USER.oid);
        login(USER_JACK_USERNAME);

        doReadSomeModifySomeUser();
    }

    private void doReadSomeModifySomeUser() throws Exception {
        // WHEN
        when();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        assertUserJackReadSomeModifySome(userJack, 1);
        assertJackEditSchemaReadSomeModifySome(userJack);

        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        assertNull("Unexpected Guybrush", userGuybrush);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                JACK_VALID_FROM_LONG_AGO);
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    private void testAutzJackPropReadSomeModifySome(String roleOid) throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, roleOid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadSomeModifySome(1);

        assertGlobalStateUntouched();
    }

    @Test
    public void test218AutzJackPropReadSomeModifySomeExecAll() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Captain"));

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PolyString.fromOrig(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FAMILY_NAME, PolyString.fromOrig(USER_JACK_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userJack, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userJack, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userJack, 1);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);

        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PolyString.fromOrig(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PolyString.fromOrig(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FAMILY_NAME, PolyString.fromOrig(USER_GUYBRUSH_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 1);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * FullName is computed in user template. It is not readable, therefore it should not be present in the preview deltas.
     * But it is modifiable (execution). Therefore the real modify operation should pass.
     * MID-5595
     */
    @Test
    public void test219AutzJackPropReadSomeModifySomeFullName() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_FULLNAME.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUser(userJack, "before modify (read by jack)")
                .assertName(USER_JACK_USERNAME)
                .assertNoFullName()
                .assertGivenName(USER_JACK_GIVEN_NAME)
                .assertNoFamilyName()
                .assertNoAdditionalName()
                .assertNoDescription()
                .activation()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertNoEffectiveStatus();

        ObjectDelta<UserType> jackGivenNameDelta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(PolyString.fromOrig("Jackie"))
                .asObjectDelta(USER_JACK_OID);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN: preview changes
        ModelContext<UserType> previewContext = previewChanges(jackGivenNameDelta, null, task, result);

        assertSuccess(result);
        assertPreviewContext(previewContext)
                .focusContext()
                .objectOld()
                .assertName(USER_JACK_USERNAME)
                .asUser()
                .assertNoFullName()
                .assertGivenName(USER_JACK_GIVEN_NAME)
                .assertNoFamilyName()
                .end()
                .end()
                .objectCurrent()
                .assertName(USER_JACK_USERNAME)
                .asUser()
                .assertNoFullName()
                .assertGivenName("Jackie") // simulations-based approach is closer to real execution than legacy one
                .assertNoFamilyName()
                .end()
                .end()
                .objectNew()
                .assertName(USER_JACK_USERNAME)
                .asUser()
                .assertNoFullName()
                .assertGivenName("Jackie")
                .assertNoFamilyName()
                .end()
                .end()
                .primaryDelta()
                .assertModify()
                .assertModifications(1)
                .property(UserType.F_GIVEN_NAME)
                .valuesToReplace()
                .single()
                .assertPolyStringValue("Jackie")
                .end()
                .end()
                .end()
                .end()
                .summarySecondaryDelta()
                // Secondary delta should be there. Because we are changing something.
                // But the user does not have authorization to read fullname.
                // Therefore the delta should be empty.
                .assertModify()
                .assertModifications(0)
                .end()
                .end()
                .projectionContexts()
                .single()
                .objectOld()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertObjectClass()
                .assertNoAttributes()
                .end()
                .objectCurrent()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertObjectClass()
                .assertNoAttributes()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .end()
                .objectNew()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertObjectClass()
                .assertNoAttributes()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .end()
                .assertNoPrimaryDelta()
                .summarySecondaryDelta()
                .assertModify()
                // Read of shadow attributes not allowed
                .assertModifications(0);

        // WHEN: real modification
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_GIVEN_NAME, PolyString.fromOrig("Jackie"));

        userJack = getUser(USER_JACK_OID);
        assertUser(userJack, "after modify (read by jack)")
                .assertName(USER_JACK_USERNAME)
                .assertNoFullName()
                .assertGivenName("Jackie")
                .assertNoFamilyName()
                .assertNoAdditionalName()
                .assertNoDescription()
                .activation()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertNoEffectiveStatus();

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);

        assertAddDeny();

        assertDeleteDeny();

        loginAdministrator();

        userJack = getUser(USER_JACK_OID);
        assertUser(userJack, "after modify (read by administrator)")
                .assertName(USER_JACK_USERNAME)
                .assertFullName("Jackie Sparrow")
                .assertGivenName("Jackie")
                .assertFamilyName(USER_JACK_FAMILY_NAME)
                .assertAdditionalName(USER_JACK_ADDITIONAL_NAME)
                .assertDescription(USER_JACK_DESCRIPTION)
                .activation()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertEffectiveStatus(ActivationStatusType.ENABLED);

        assertGlobalStateUntouched();
    }

    @Test
    public void test220AutzJackPropDenyModifySome() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_DENY_MODIFY_SOME.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PolyString.fromOrig(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PolyString.fromOrig(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FAMILY_NAME, PolyString.fromOrig(USER_JACK_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userJack, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignmentsWithTargets(userJack, 1);

        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, true, true);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, true, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, true, false);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, true, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, true, true);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, true, true);

        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PolyString.fromOrig(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PolyString.fromOrig(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_GIVEN_NAME, PolyString.fromOrig(USER_GUYBRUSH_GIVEN_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FAMILY_NAME, PolyString.fromOrig(USER_GUYBRUSH_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        assertAssignmentsWithTargets(userGuybrush, 1);

        assertAddAllow();
        assertAddAllowRaw();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Captain"));
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_GIVEN_NAME, PolyString.fromOrig("Jackie"));

        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_GIVEN_NAME, PolyString.fromOrig("Brushie"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_GIVEN_NAME, PolyString.fromOrig("Hectie"));

        assertDeleteAllow();

        assertGlobalStateUntouched();
    }

    @Test
    public void test221AutzJackObjectAddPreview() throws CommonException, IOException {
        given("Jack has authorization to add new users");
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ROLE_ADD_READ_SOME.oid);
        login(USER_JACK_USERNAME);

        when();
        ObjectDelta<RoleType> userToAddDelta = createObject(RoleType.class, "Jack's role").createAddDelta();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ModelContext<RoleType> previewContext = previewChanges(userToAddDelta, null, task, result);

        then();
        assertPreviewContext(previewContext)
                .focusContext().objectNew().assertName("Jack's role");
    }

    /**
     * Jack has no authorization to see the projections. The preview should not contain them, but it should not fail
     * with "Access denied" (as it did previously) either.
     *
     * MID-10397
     */
    @Test
    public void test225PreviewWithHiddenProjections() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SHOW_USERS_HIDE_SHADOWS.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow();

        ObjectDelta<UserType> jackGivenNameDelta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(PolyString.fromOrig("Jackie"))
                .asObjectDelta(USER_JACK_OID);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN: preview changes
        ModelContext<UserType> previewContext = previewChanges(jackGivenNameDelta, null, task, result);

        assertSuccess(result);
        assertPreviewContext(previewContext)
                .focusContext()
                .objectOld()
                .assertName(USER_JACK_USERNAME)
                .asUser()
                .assertGivenName(USER_JACK_GIVEN_NAME)
                .end()
                .end()
                .objectCurrent()
                .assertName(USER_JACK_USERNAME)
                .asUser()
                .assertGivenName("Jackie") // simulations-based approach is closer to real execution than legacy one
                .end()
                .end()
                .objectNew()
                .assertName(USER_JACK_USERNAME)
                .asUser()
                .assertGivenName("Jackie")
                .end()
                .end()
                .primaryDelta()
                .assertModify()
                .assertModifications(1)
                .property(UserType.F_GIVEN_NAME)
                .valuesToReplace()
                .single()
                .assertPolyStringValue("Jackie")
                .end()
                .end()
                .end()
                .end()
                .summarySecondaryDelta()
                .display()
                .assertModify()
                .assertModifications(1)
                .end()
                .end()
                .projectionContexts()
                .assertNone(); // projection context is not visible

        // WHEN: real modification
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_GIVEN_NAME, PolyString.fromOrig("Jackie"));

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUser(userJack, "after modify (read by jack)")
                .assertName(USER_JACK_USERNAME)
                .assertGivenName("Jackie");
    }

    @Test
    public void test230AutzJackMasterMinistryOfRum() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MASTER_MINISTRY_OF_RUM.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadDeny(3);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PolyString.fromOrig("drunk"));
        assertGetAllow(UserType.class, userCobbOid);
        assertAddDenyRaw(USER_MANCOMB);
        assertAddAllow(USER_MANCOMB);

        assertVisibleUsers(4);

        assertDeleteAllow(UserType.class, USER_ESTEVAN.oid);

        assertVisibleUsers(3);

        assertGlobalStateUntouched();
    }

    @Test
    public void test232AutzJackReadOrgMinistryOfRum() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertSearch(OrgType.class, null, 1);
        // The search wit ResourceObjectClass is important. It is a very different case
        // than searching just for UserType or OrgType
        assertSearch(ObjectType.class, null, 1);

        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PolyString.fromOrig("drunk"));
        assertAddDeny(USER_MANCOMB);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874
     */
    @Test
    public void test240AutzJackManagerFullControlNoOrg() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PolyString.fromOrig("drunk"));
        assertGetDeny(UserType.class, userCobbOid);
        assertAddDeny(USER_MANCOMB); // MID-3874

        assertVisibleUsers(0);

        assertGetDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 0);

        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");

        assertDeleteDeny(UserType.class, USER_ESTEVAN.oid);

        assertGetDeny(ShadowType.class, accountOid);
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        assertSearch(ShadowType.class,
                prismContext.queryFactory().createQuery(
                        ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS)),
                0);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874
     */
    @Test
    public void test241AutzJackManagerFullControlMemberMinistryOfRum() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertJack24xMember(accountOid);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874
     */
    @Test
    public void test242AutzJackManagerFullControlManagerMinistryOfRum() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertJack24xManager(true);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874, MID-3780
     */
    @Test
    public void test243AutzJackManagerFullControlManagerMinistryOfRumAndDefense() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        dumpOrgTreeAndUsers();

        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertJack24xManagerDefense(true);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874
     */
    @Test
    public void test245AutzJackManagerUserAdminMemberMinistryOfRum() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_USER_ADMIN.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertJack24xMember(accountOid);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874
     */
    @Test
    public void test246AutzJackManagerUserAdminManagerMinistryOfRum() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_USER_ADMIN.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertJack24xManager(false);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3874
     */
    @Test
    public void test247AutzJackManagerUserAdminManagerMinistryOfRumAndDefense() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_MANAGER_USER_ADMIN.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertJack24xManagerDefense(false);

        assertGlobalStateUntouched();
    }

    private void assertJack24xMember(String accountOid) throws Exception {
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PolyString.fromOrig("drunk"));
        assertGetDeny(UserType.class, userCobbOid);
        assertAddDeny(USER_MANCOMB); // MID-3874

        assertVisibleUsers(0);

        assertGetDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 0);

        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");

        assertDeleteDeny(UserType.class, USER_ESTEVAN.oid);

        assertGetDeny(ShadowType.class, accountOid);
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        assertSearch(ShadowType.class,
                prismContext.queryFactory().createQuery(
                        ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS)),
                0);
    }

    private void assertJack24xManager(boolean fullControl) throws Exception {
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearch(UserType.class, null, 4);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDenyRaw(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, PolyString.fromOrig("CSc"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();

        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PolyString.fromOrig("drunk"));
        assertGetAllow(UserType.class, userCobbOid); // Cobb is in Scumm Bar, transitive descendant of Ministry of Rum
        assertAddDenyRaw(USER_MANCOMB);
        assertAddAllow(USER_MANCOMB); // MID-3874

        Task task = createPlainTask();
        OperationResult result = task.getResult();
        try {
            addObject(ORG_CHEATERS, task, result); // MID-3874
            assertNotReached();
        } catch (PolicyViolationException e) {
            if (fullControl) {
                // Allowed by authorizations, disallowed by policy (parentOrgRef vs assignment)
                displayExpectedException(e);
                assertFailure(result);
            } else {
                fail("Unexpected exception: " + e.getMessage());
            }
        } catch (SecurityViolationException e) {
            if (!fullControl) {
                // Disallowed by authorizations
                displayExpectedException(e);
                assertFailure(result);
            } else {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        assertVisibleUsers(5);

        assertGetAllow(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 2);

        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        if (fullControl) {
            assertModifyAllow(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        } else {
            assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        }

        assignAccountToUser(USER_ESTEVAN.oid, RESOURCE_DUMMY_OID, null);

        PrismObject<UserType> userEstevan = getUser(USER_ESTEVAN.oid);
        String accountEstevanOid = getSingleLinkOid(userEstevan);
        assertGetAllow(ShadowType.class, accountEstevanOid);
        PrismObject<ShadowType> shadowEstevan = getObject(ShadowType.class, accountEstevanOid);
        display("Estevan shadow", shadowEstevan);

        // MID-2822

        task = createPlainTask();
        result = task.getResult();

        ObjectQuery query = prismContext.queryFactory().createQuery(
                ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS));

        // When finally fixed is should be like this:
//        assertSearch(ShadowType.class, query, 2);

        try {
            modelService.searchObjects(ShadowType.class, query, null, task, result);

            AssertJUnit.fail("unexpected success");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }
        result.computeStatus();
        TestUtil.assertFailure(result);

        assertDeleteAllow(UserType.class, USER_ESTEVAN.oid);

        assertVisibleUsers(4);
    }

    private void assertJack24xManagerDefense(boolean fullControl) throws Exception {
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearch(UserType.class, null, 4);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDenyRaw(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, PolyString.fromOrig("CSc"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();

        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PolyString.fromOrig("drunk"));
        assertGetAllow(UserType.class, userCobbOid); // Cobb is in Scumm Bar, transitive descendant of Ministry of Rum
        assertAddAllow(USER_MANCOMB); // MID-3874

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        assertVisibleUsers(5);

        assertGetAllow(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 3);

        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        if (fullControl) {
            assertModifyAllow(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        } else {
            assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        }

        assignAccountToUser(USER_ESTEVAN.oid, RESOURCE_DUMMY_OID, null);

        PrismObject<UserType> userEstevan = getUser(USER_ESTEVAN.oid);
        String accountEstevanOid = getSingleLinkOid(userEstevan);
        assertGetAllow(ShadowType.class, accountEstevanOid);
        PrismObject<ShadowType> shadowEstevan = getObject(ShadowType.class, accountEstevanOid);
        display("Estevan shadow", shadowEstevan);

        // MID-2822

        Task task = createPlainTask();
        OperationResult result = task.getResult();

        ObjectQuery query = prismContext.queryFactory().createQuery(
                ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS));

        // When finally fixed is should be like this:
//        assertSearch(ShadowType.class, query, 2);

        try {
            modelService.searchObjects(ShadowType.class, query, null, task, result);

            AssertJUnit.fail("unexpected success");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        assertFailure(result);

        assertSearch(UserType.class, null, 5);

        assertAddAllow(USER_CAPSIZE); // MID-3780

        assertSearch(UserType.class, null, 6);

        assertDeleteAllow(UserType.class, USER_ESTEVAN.oid);

        assertSearch(UserType.class, null, 5);

        assertDeleteAllow(UserType.class, USER_CAPSIZE.oid);

        assertSearch(UserType.class, null, 4);

        assertVisibleUsers(4);
    }

    @Test
    public void test250AutzJackSelfAccountsRead() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // enable after implementing MID-2789 and MID-2790
//        ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
//                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
//                .and().item(ShadowType.F_OBJECT_CLASS).eq(new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"))
//                .build();
//        assertSearch(ShadowType.class, query, null, 1);
//        assertSearch(ShadowType.class, query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY);

        // Linked to jack
        assertDeny("add jack's account to jack",
                (task, result) -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result));

        // Linked to other user
        assertDeny("add jack's account to gyubrush",
                (task, result) -> modifyUserAddAccount(USER_GUYBRUSH_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result));

        assertDeleteDeny(ShadowType.class, accountOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        getEditObjectDefinition(user);
        // TODO: assert items

        getEditObjectDefinition(shadow);
        // TODO: assert items

        assertGlobalStateUntouched();
    }

    /**
     * Among other things, checks the output of {@link ModelInteractionService#getEditObjectClassDefinition(PrismObject,
     * PrismObject, AuthorizationPhaseType, Task, OperationResult)}.
     *
     * See also `TestUnix.test020GetEditSchema` where this method is tested as well
     * (because of aux OC support that is missing here).
     */
    @Test
    public void test255AutzJackSelfAccountsReadWrite() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_WRITE.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        Task task = getTestTask();
        ResourceObjectDefinition rOcDef =
                modelInteractionService.getEditObjectClassDefinition(
                        shadow, getDummyResourceObject(), null, task, task.getResult());
        displayDumpable("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY);

        // Linked to jack
        assertAllow("add jack's account to jack",
                (t, result) -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, t, result));

        user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLiveLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);

        // Linked to other user
        assertDeny("add gyubrush's account",
                (t, result) -> modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, t, result));

        assertDeleteAllow(ShadowType.class, accountRedOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        assertGlobalStateUntouched();
    }

    @Test
    public void test256AutzJackSelfAccountsPartialControl() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PolyString.fromOrig("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        Task task = getTestTask();
        OperationResult result = task.getResult();
        ResourceObjectDefinition rOcDef =
                modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null, task, result);
        displayDumpable("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY);

        assertPasswordChangeDeny(UserType.class, USER_JACK_OID, "nbusr123");
        assertPasswordChangeDeny(UserType.class, USER_GUYBRUSH_OID, "nbusr123");

        PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, task, result);
        assertItemFlags(rDef, PASSWORD_PATH, true, false, false);

        assertGlobalStateUntouched();
    }

    @Test
    public void test258AutzJackSelfAccountsPartialControlPassword() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PolyString.fromOrig("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        Task task = getTestTask();
        OperationResult result = task.getResult();
        ResourceObjectDefinition rOcDef =
                modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null, task, result);
        displayDumpable("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY);

        assertPasswordChangeAllow(UserType.class, USER_JACK_OID, "nbusr123");
        assertPasswordChangeDeny(UserType.class, USER_GUYBRUSH_OID, "nbusr123");

        PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, task, result);
        assertItemFlags(rDef, PASSWORD_PATH, true, false, false);

        assertGlobalStateUntouched();
    }

    /**
     * Test getEditObjectDefinition for shadow.
     * It should also call and apply edited schema for attributes.
     */
    @Test
    public void test259AutzJackSelfAccountsPartialControl() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD.oid);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PolyString.fromOrig("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);

        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObjectDefinition<ShadowType> rOcDef = modelInteractionService.getEditObjectDefinition(shadow, null, task, result);
        shadow.applyDefinition(rOcDef);

        ShadowAttributesContainer shadowAttributesContainer = ShadowUtil.getAttributesContainer(shadow);
        ResourceObjectDefinition containerDef = shadowAttributesContainer.getDefinition().getResourceObjectDefinition();

        Item<?, ?> attr = shadowAttributesContainer.findItem(new ItemName("weapon"));
        ItemDefinition<?> attrDf = attr.getDefinition();
        assertTrue("Expected that attribute can be read", attrDf.canRead());
        assertFalse("Expected that attribute cannot be added", attrDf.canAdd());
        assertFalse("Expected that attribute cannot be modified", attrDf.canModify());

        displayDumpable("Refined objectclass def", containerDef);
        assertAttributeFlags(containerDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(containerDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(containerDef, new ItemName("location"), true, true, true);
        assertAttributeFlags(containerDef, new ItemName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY);

        assertPasswordChangeAllow(UserType.class, USER_JACK_OID, "nbusr123");
        assertPasswordChangeDeny(UserType.class, USER_GUYBRUSH_OID, "nbusr123");

        PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, task, result);
        assertItemFlags(rDef, PASSWORD_PATH, true, false, false);

        assertGlobalStateUntouched();
    }

    @Test
    public void test260AutzJackObjectFilterLocationShadowRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS.oid);
        login(USER_JACK_USERNAME);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 8);
        assertSearch(OrgType.class, null, 6);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PolyString.fromOrig("Mutineer"));

        assertDeleteDeny();

        // Linked to jack
        assertAllow("add jack's account to jack",
                (task, result) -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result));
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLiveLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        assertGetAllow(ShadowType.class, accountRedOid);

        assertGlobalStateUntouched();

        displayCleanup();
        login(USER_ADMINISTRATOR_USERNAME);

        Task task = getTestTask();
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_RED_FILE);
        account.setOid(accountRedOid);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
        userDelta.addModification(accountDelta);
        executeChanges(userDelta, null, task, task.getResult());

        user = getUser(USER_JACK_OID);
        assertLiveLinks(user, 0);
    }

    /**
     * Creates user and assigns role at the same time.
     */
    @Test
    public void test261AutzAngelicaObjectFilterLocationCreateUserShadowRole() throws Exception {
        given();

        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS.oid);
        login(USER_JACK_USERNAME);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        when();

        assertAllow("add user angelica",
                (task, result) -> addObject(USER_ANGELICA.get(), task, result));

        // THEN
        then();

        login(USER_ADMINISTRATOR_USERNAME);                 // user jack seemingly has no rights to search for angelika

        PrismObject<UserType> angelica = findUserByUsername(USER_ANGELICA.getNameOrig());
        display("angelica", angelica);
        assertUser(angelica, null, USER_ANGELICA.getNameOrig(), "angelika angelika", "angelika", "angelika");
        assertAssignedRole(angelica, ROLE_BASIC.oid);
        assertAccount(angelica, RESOURCE_DUMMY_OID);

        assertGlobalStateUntouched();
    }

    @Test
    public void test270AutzJackAssignApplicationRoles() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertEmptyDeltaExecutionAuthorized(UserType.class, USER_GUYBRUSH_OID);
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_APPLICATION_ROLES.oid);

        assertAllow("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_APPLICATION_1.oid);

        assertDeny("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        assertAllow("unassign application role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertAllowRequestAssignmentItems(USER_JACK_OID, ROLE_APPLICATION_1.oid,
                SchemaConstants.PATH_ASSIGNMENT_TARGET_REF,
                SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_FROM,
                SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_TO);

        assertGlobalStateUntouched();
    }

    @Test
    public void test272AutzJackAssignAnyRoles() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_ANY_ROLES.oid);

        assertAllow("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_APPLICATION_1.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        assertAllow("unassign application role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result)
        );

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertAllowRequestAssignmentItems(USER_JACK_OID, ROLE_APPLICATION_1.oid,
                SchemaConstants.PATH_ASSIGNMENT_DESCRIPTION,
                SchemaConstants.PATH_ASSIGNMENT_TARGET_REF,
                SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_FROM,
                SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_VALID_TO);

        assertGlobalStateUntouched();
    }

    /**
     * Check that the #assign authorization does not allow assignment that contains
     * policyException or policyRule.
     */
    @Test
    public void test273AutzJackRedyAssignmentExceptionRules() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_ANY_ROLES.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, null,
                        assignment -> {
                            PolicyExceptionType policyException = new PolicyExceptionType();
                            policyException.setRuleName("whatever");
                            assignment.getPolicyException().add(policyException);
                        },
                        task, result)
        );

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null,
                        assignment -> {
                            PolicyRuleType policyRule = new PolicyRuleType();
                            policyRule.setName("whatever");
                            assignment.setPolicyRule(policyRule);
                        },
                        task, result)
        );

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        assertGlobalStateUntouched();
    }

    @Test
    public void test274AutzJackAssignNonApplicationRoles() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_NON_APPLICATION_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_NON_APPLICATION_ROLES.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertGlobalStateUntouched();
    }

    @Test
    public void test275aAutzJackAssignRequestableRoles() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertAssignableRoleSpecification(getRole(ROLE_ASSIGN_REQUESTABLE_ROLES.oid), RoleType.class, 1)
                .assertNoAccess();

        assertGlobalStateUntouched();
    }

    /**
     * MID-3636 partially
     * MID-4399
     */
    @Test
    public void test275bAutzJackAssignRequestableOrgs() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_END_USER_REQUESTABLE_ABSTRACTROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_END_USER_REQUESTABLE_ABSTRACTROLES.oid);

        assertAllow("assign requestable org to jack",
                (task, result) -> assignOrg(USER_JACK_OID, ORG_REQUESTABLE.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, OrgType.class, 1);

        ObjectFilter jackAssignableRoleFilter =
                assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                        .relationDefault()
                        .filter()
                        .getFilter();

        ObjectQuery query = prismContext.queryFactory().createQuery();
        query.addFilter(jackAssignableRoleFilter);
        assertSearch(AbstractRoleType.class, query, 9);

        assertAllow("unassign business role from jack",
                (task, result) -> unassignOrg(USER_JACK_OID, ORG_REQUESTABLE.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, OrgType.class, 0);

        assertGlobalStateUntouched();
    }

    /**
     * MID-5005
     */
    @Test
    public void test275cAutzJackAssignRequestableRolesAndInduceAnyRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);
        assignRole(USER_JACK_OID, ROLE_INDUCE_ANY_ROLE.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertAssignableRoleSpecification(getRole(ROLE_ASSIGN_REQUESTABLE_ROLES.oid), RoleType.class, 1)
                .relationDefault()
                .filter()
                .assertNull();

        assertGlobalStateUntouched();
    }

    /**
     * MID-3136
     */
    @Test
    public void test276AutzJackAssignRequestableRolesWithOrgRef() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 1 assignments)", user);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(user)
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertGlobalStateUntouched();
    }

    /**
     * Assign a role with parameter while the user already has the same role without a parameter.
     * It seems that in this case the deltas are processed in a slightly different way.
     * MID-3136
     */
    @Test
    public void test277AutzJackAssignRequestableRolesWithOrgRefSecondTime() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assertAllow("assign business role to jack (no param)",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertAllow("assign business role to jack (org MoR)",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        display("user after (expected 3 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertAllow("assign business role to jack (org Scumm)",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_SCUMM_BAR_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        display("user after (expected 4 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertAllow("unassign business role from jack (org Scumm)",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_SCUMM_BAR_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        display("user after (expected 3 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack (no param)",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);

        assertAllow("unassign business role from jack (org MoR)",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 1 assignments)", user);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(user)
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3136
     */
    @Test
    public void test278AutzJackAssignRequestableRolesWithOrgRefTweakedDelta() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assertAllow("assign business role to jack",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> {
                    Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
                    ContainerDelta<AssignmentType> assignmentDelta1 = prismContext.deltaFactory().container().createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
                    PrismContainerValue<AssignmentType> cval = prismContext.itemFactory().createContainerValue();
                    assignmentDelta1.addValueToAdd(cval);
                    PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
                    targetRef.getValue().setOid(ROLE_BUSINESS_2.oid);
                    targetRef.getValue().setTargetType(RoleType.COMPLEX_TYPE);
                    targetRef.getValue().setRelation(null);
                    cval.setId(123L);
                    modifications.add(assignmentDelta1);
                    ObjectDelta<UserType> userDelta1 = prismContext.deltaFactory().object()
                            .createModifyDelta(USER_JACK_OID, modifications, UserType.class);
                    Collection<ObjectDelta<? extends ObjectType>> deltas =
                            MiscSchemaUtil.createCollection(userDelta1);
                    modelService.executeChanges(deltas, null, task, result);
                });

        assertAllow("unassign business role from jack",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, ORG_MINISTRY_OF_RUM_OID, null, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(user)
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertGlobalStateUntouched();
    }

    /**
     * MID-3136
     */
    @Test
    public void test279AutzJackAssignRequestableRolesWithTenantRef() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES.oid);

        assertAllow("assign business role to jack",
                (task, result) ->
                        assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack", (task, result) ->
                assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        assertAllow("unassign business role from jack",
                (task, result) ->
                        unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 1 assignments)", user);
        assertAssignments(user, 1);

        assertAssignableRoleSpecification(user)
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class);

        assertGlobalStateUntouched();
    }

    /**
     * MID-4183
     */
    @Test
    public void test280AutzJackEndUser() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_END_USER.oid);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);
        assertLiveLinks(user, 0);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertModifyMetadataDeny(UserType.class, USER_JACK_OID);
        assertModifyMetadataDeny(UserType.class, USER_GUYBRUSH_OID);

        assertPasswordChangeAllow(UserType.class, USER_JACK_OID, "nbusr123");
        assertPasswordChangeDeny(UserType.class, USER_GUYBRUSH_OID, "nbusr123");

        // MID-3136
        assertAllow("assign business role to jack",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        // End-user role has authorization to assign, but not to unassign
        assertDeny("unassign business role from jack",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 2);

        assertAllow("assign basic role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BASIC.oid, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);

        String accountOid = getSingleLinkOid(user);

        PrismObject<ShadowType> accountShadow = assertGetAllow(ShadowType.class, accountOid);
        display("account shadow", accountShadow);

        assertPasswordChangeAllow(UserType.class, USER_JACK_OID, "nbusr321");
        assertPasswordChangeDeny(UserType.class, USER_GUYBRUSH_OID, "nbusr321");

        assertPasswordChangeAllow(ShadowType.class, accountOid, "nbusr231");

        assertDeny("unassign basic role from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_BASIC.oid, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);

        assertGlobalStateUntouched();

        assertCredentialsPolicy(user);
    }

    @Test
    public void test281AutzJackEndUserSecondTime() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_END_USER.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 1);

        getUser(USER_JACK_OID);

        // MID-3136
        assertAllow("assign business role to jack (no param)",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, null, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        // MID-3136
        assertAllow("assign business role to jack (org governor)",
                (task, result) -> assignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1.oid);

        assertDeny("assign application role to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_BUSINESS_2.oid, task, result));

        // End-user role has authorization to assign, but not to unassign
        assertDeny("unassign business role from jack",
                (task, result) -> unassignParametricRole(USER_JACK_OID, ROLE_BUSINESS_1.oid, null, ORG_GOVERNOR_OFFICE_OID, task, result));

        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);

        assertGlobalStateUntouched();

        assertCredentialsPolicy(user);
    }

    private void assertCredentialsPolicy(PrismObject<UserType> user)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createOperationResult("assertCredentialsPolicy");
        CredentialsPolicyType credentialsPolicy = modelInteractionService.getCredentialsPolicy(user, getTestTask(), result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull("No credentials policy for " + user, credentialsPolicy);
        SecurityQuestionsCredentialsPolicyType securityQuestions = credentialsPolicy.getSecurityQuestions();
        assertEquals("Unexpected number of security questions for " + user, 2, securityQuestions.getQuestion().size());
    }

    /**
     * MID-5066
     */
    @Test
    public void test282AutzJackEndUserAndModify() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_END_USER.oid);
        assignRole(USER_JACK_OID, ROLE_USER_MODIFY.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyAllow();
        assertEmptyDeltaExecutionAuthorized(UserType.class, USER_GUYBRUSH_OID);
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        OperationResult result = assertAllowTracing("modify jack's familyName",
                (t, r) -> modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, UserType.F_FAMILY_NAME, t, r, PolyString.fromOrig("changed")));
        display("RESULT", result);
        // MID-5066, check for normal case. Search for personaRef is OK here.
        OperationResultRepoSearchAsserter.forResult(result)
                .display()
                .assertContainsQuerySubstring("personaRef");

        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, "Jack changed", "Jack", "changed");

        assertGlobalStateUntouched();
    }

    @Test
    public void test283AutzJackModifyAndEndUser() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_USER_MODIFY.oid);
        assignRole(USER_JACK_OID, ROLE_END_USER.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        assertAllow("modify jack's familyName",
                (task, result) -> modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, UserType.F_FAMILY_NAME, task, result, PolyString.fromOrig("changed")));

        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, "Jack changed", "Jack", "changed");

        assertGlobalStateUntouched();
    }

    @Test
    public void test285AutzJackEndUserAndAdd() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_END_USER.oid);
        assignRole(USER_JACK_OID, ROLE_USER_ADD.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);

        OperationResult result = assertAddAllowTracing(USER_NOOID_FILE);
        display("RESULT", result);
        // MID-5066; here the search for `personaRef` is safe: no search for "personaRef = null" will be done, because
        // the object being added has already an OID.
        OperationResultRepoSearchAsserter.forResult(result)
                .display()
                .assertContainsQuerySubstring("personaRef");

        assertModifyDeny();
        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    /**
     * Jack is not owner of application role 1 (yet), attempt to assign should fail.
     * #9985
     */
    @Test
    public void test290AutzJackNoRoleOwnerAssign() throws Exception {
		given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_ASSIGN.oid);
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assertUserBefore(USER_JACK_OID)
                .assertAssignments(1)
                .assertLinks(0,0);
        assertRoleBefore(ROLE_APPLICATION_1.oid);
        assertRoleBefore(ROLE_APPLICATION_2.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertUserAfter(USER_JACK_OID)
                .assertAssignments(1)
                .assignments()
                    .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid);

        assertDeny("assign application role 1 to jack",
        		(task,result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result));

        assertUser(USER_JACK_OID, "after2")
                .assertAssignments(1)
                .assignments()
                    .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid);

        assertDeny("assign application role 2 to jack",
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_2.oid, task, result));

        assertUser(USER_JACK_OID, "after3")
                .assertAssignments(1)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid);

        assertGlobalStateUntouched();
	}

    /**
     * Jack is owner of application role 1, attempt to assign should pass.
     * #9985
     */
    @Test
    public void test291AutzJackRoleOwnerAssign() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        // Make Jack owner of role Application 1
        assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());

        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_ASSIGN.oid);
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        assertUserBefore(USER_JACK_OID)
                .assertLinks(0,0)
                .assertAssignments(2)
                .assignments()
                    .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid)
                    .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());


        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertUserAfter(USER_JACK_OID)
                .assertAssignments(2)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid)
                .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());

        assertAllow("assign application role 1 to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result));

        assertUser(USER_JACK_OID, "after2")
                .assertAssignments(3)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid)
                .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.MEMBER.getRelation())
                .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());

        assertDeny("assign application role 2 to jack",
                (task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_2.oid, task, result));

        assertAllow("unassign application role 1 from jack",
                (task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, task, result));

        assertUser(USER_JACK_OID, "after3")
                .assertAssignments(2)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_ASSIGN.oid)
                .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());

        // TODO: change to assertAssignableRoleSpecification
//                    RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        //        assertRoleTypes(spec);
        //        assertFilter(spec.getFilter(), TypeFilter.class);
        //        assertEquals("Wrong type filter type", RoleType.COMPLEX_TYPE, ((TypeFilter)spec.getFilter()).getType());
        //        ObjectFilter subfilter = ((TypeFilter)spec.getFilter()).getFilter();
        //        assertFilter(subfilter, RefFilter.class);
        //        assertEquals(1, ((RefFilter)subfilter).getValues().size());
        //        assertEquals("Wrong OID in ref filter", USER_JACK_OID, ((RefFilter)subfilter).getValues().get(0).getOid());


        assertGlobalStateUntouched();
    }

    /**
     * Jack is not owner of application role 1 (yet), attempt to list, read and modify the role should fail.
     * #9985
     */
    @Test
    public void test292AutzJackNoRoleOwnerFullControl() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_FULL_CONTROL.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        assertUserBefore(USER_JACK_OID)
                .assertLinks(0, 0)
                .assertAssignments(1)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_FULL_CONTROL.oid);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        // Basic routine, given by "normal" authorizations
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertUserAfter(USER_JACK_OID)
                .assertAssignments(1)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_FULL_CONTROL.oid);

        // This is what we are really testing here
        assertGetDeny(RoleType.class, ROLE_APPLICATION_1.oid);
        assertGetDeny(RoleType.class, ROLE_APPLICATION_2.oid);
        assertSearch(RoleType.class, null, 0);
        assertSearch(RoleType.class, createNameQuery(ROLE_APPLICATION_1.getNameOrig()), 0);
        assertSearch(RoleType.class, createNameQuery(ROLE_APPLICATION_2.getNameOrig()), 0);


        assertDeny("modify app1 role description",
                (task, result) -> modifyObjectReplaceProperty(RoleType.class, ROLE_APPLICATION_1.oid, RoleType.F_DESCRIPTION, task, result,
                        "decs change denied"));

        assertDeny("modify app2 role description",
                (task, result) -> modifyObjectReplaceProperty(RoleType.class, ROLE_APPLICATION_2.oid, RoleType.F_DESCRIPTION, task, result,
                        "desc change denied"));

        assertGlobalStateUntouched();
    }


    /**
     * Jack is owner of application role 1, attempt to list, read and modify the role should pass.
     * #9985
     */
    @Test() // #9985
    public void test293AutzJackRoleOwnerFullControl() throws Exception {
        skipIfNotNativeRepository();
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        // Make Jack owner of role Application 1
        assignRole(USER_JACK_OID, ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());

        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_FULL_CONTROL.oid);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        assertUserBefore(USER_JACK_OID)
                .assertLinks(0,0)
                .assertAssignments(2)
                .assignments()
                    .assertRole(ROLE_ROLE_OWNER_FULL_CONTROL.oid)
                    .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());


        login(USER_JACK_USERNAME);

        // WHEN
        when();


        // Basic routine, given by "normal" authorizations
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertUserAfter(USER_JACK_OID)
                .assertAssignments(2)
                .assignments()
                .assertRole(ROLE_ROLE_OWNER_FULL_CONTROL.oid)
                .assertRole(ROLE_APPLICATION_1.oid, RelationTypes.OWNER.getRelation());

        // This is what we are really testing here
        // TODO
        assertGetAllow(RoleType.class, ROLE_APPLICATION_1.oid);
        assertGetDeny(RoleType.class, ROLE_APPLICATION_2.oid);
        assertSearch(RoleType.class, null, 1);
        assertSearch(RoleType.class, createNameQuery(ROLE_APPLICATION_1.getNameOrig()), 1);
        assertSearch(RoleType.class, createNameQuery(ROLE_APPLICATION_2.getNameOrig()), 0);

        assertAllow("modify app1 role description",
                (task, result) -> modifyObjectReplaceProperty(RoleType.class, ROLE_APPLICATION_1.oid, RoleType.F_DESCRIPTION, task, result,
                        "Jack was here"));

        assertDeny("modify app2 role description",
                (task, result) -> modifyObjectReplaceProperty(RoleType.class, ROLE_APPLICATION_2.oid, RoleType.F_DESCRIPTION, task, result,
                        "desc change denied"));

        // TODO: change to assertAssignableRoleSpecification
//                    RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        //        assertRoleTypes(spec);
        //        assertFilter(spec.getFilter(), TypeFilter.class);
        //        assertEquals("Wrong type filter type", RoleType.COMPLEX_TYPE, ((TypeFilter)spec.getFilter()).getType());
        //        ObjectFilter subfilter = ((TypeFilter)spec.getFilter()).getFilter();
        //        assertFilter(subfilter, RefFilter.class);
        //        assertEquals(1, ((RefFilter)subfilter).getValues().size());
        //        assertEquals("Wrong OID in ref filter", USER_JACK_OID, ((RefFilter)subfilter).getValues().get(0).getOid());

        assertGlobalStateUntouched();
	}


    @Test
    public void test295AutzJackAssignOrgRelation() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_ASSIGN_ORGRELATION.oid);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null);

        when();
        login(USER_JACK_USERNAME);

        then();
        ObjectFilter jackAssignableRoleFilter = assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertNotNull()
                .getFilter();

        Task task = createPlainTask();
        SearchResultList<PrismObject<AbstractRoleType>> assignableRolesJack =
                modelService.searchObjects(AbstractRoleType.class, prismContext.queryFactory().createQuery(jackAssignableRoleFilter), null, task, task.getResult());
        display("Assignable roles", assignableRolesJack);
        assertObjectOids("Wrong assignable roles (jack)", assignableRolesJack, ROLE_BUSINESS_3.oid);

        ObjectFilter rumAssignableRoleFilter = assertAssignableRoleSpecification(getUser(userRumRogersOid))
                .relationDefault()
                .filter()
                .assertClass(TypeFilter.class)
                .getFilter();

        SearchResultList<PrismObject<AbstractRoleType>> assignableRolesRum =
                modelService.searchObjects(AbstractRoleType.class, prismContext.queryFactory().createQuery(rumAssignableRoleFilter), null, task, task.getResult());
        display("Assignable roles", assignableRolesRum);
        assertObjectOids("Wrong assignable roles (rum)", assignableRolesRum, ROLE_BUSINESS_3.oid);

        assertGlobalStateUntouched();
    }

    @Test
    public void test300AutzAnonymous() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);

        when();
        loginAnonymous();

        then();
        assertNoAccess(userJack);
        assertGlobalStateUntouched();
    }

    @Test
    public void test310AutzJackNoRolePrivileged() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        login(USER_JACK_USERNAME);

        expect();
        assertNoAccess(userJack);

        // WHEN (security context elevated)
        runPrivileged(() -> {
            try {

                assertSuperuserAccess(NUMBER_OF_ALL_USERS + 1);

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            return null;
        });

        // WHEN (security context back to normal)
        assertNoAccess(userJack);

        assertGlobalStateUntouched();
    }

    @Test
    public void test312AutzAnonymousPrivileged() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();

        // precondition
        assertNoAccess(userJack);

        // WHEN (security context elevated)
        runPrivileged(() -> {
            try {

                assertSuperuserAccess(NUMBER_OF_ALL_USERS + 1);

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            return null;
        });

        // WHEN (security context back to normal)
        assertNoAccess(userJack);

        assertGlobalStateUntouched();
    }

    @Test
    public void test313AutzAnonymousPrivilegedRestore() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();

        // WHEN (security context elevated)
        runPrivileged(() -> {

            // do nothing.

            return null;
        });

        // WHEN (security context back to normal)
        assertNoAccess(userJack);

        assertGlobalStateUntouched();
    }

    /** Searching for "roles of teammate" with denied access to `assignment` item. MID-9638. */
    @Test
    public void test320AutzDenyReadAssignmentAndRoleMembershipRef() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY.oid);
        assignRole(USER_JACK_OID, ROLE_DENY_READ_ASSIGNMENT_AND_ROLE_MEMBERSHIP_REF.oid);
        login(USER_JACK_USERNAME);

        when("searching for users based on assignment/targetRef (forbidden)");
        assertSearch(
                UserType.class,
                queryFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(ROLE_BASIC.oid) // at least alex has this role
                        .build(),
                0); // access to assignment/targetRef is explicitly denied

        when("searching for roles of teammate alex (forbidden)");
        var alexRolesQuery = createRolesOfTeammateQuery(USER_ALEX.oid);
        assertSearch(RoleType.class, alexRolesQuery, 0);
    }

    private ObjectQuery createRolesOfTeammateQuery(String userOid) {
        return queryFor(RoleType.class)
                .referencedBy(UserType.class, UserType.F_ASSIGNMENT.append(AssignmentType.F_TARGET_REF))
                .id(userOid)
                .and().not().type(ArchetypeType.class)
                .build();
    }

    @Test
    public void test360AutzJackAuditorRole() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_AUDITOR.oid);
        login(USER_JACK_USERNAME);

        // WHEN
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertReadCertCasesAllow();
        assertReadCasesAllow();

        assertGlobalStateUntouched();

        assertAuditReadAllow();
    }

    /**
     * MID-3826
     */
    @Test
    public void test370AutzJackLimitedUserAdmin() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_USER_ADMIN.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);

        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS + 1);
        assertSearch(ObjectType.class, null, NUMBER_OF_ALL_USERS + 1);
        assertSearch(OrgType.class, null, 0);

        assertAddAllow(USER_HERMAN_FILE);

        assertModifyDeny();
        assertEmptyDeltaExecutionAuthorized(UserType.class, USER_GUYBRUSH_OID);

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    @Test
    public void test380AutzJackSelfTaskOwner() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_TASK_OWNER.oid);

        when();
        login(USER_JACK_USERNAME);

        then();
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertGetDeny(TaskType.class, TASK_USELESS_ADMINISTRATOR.oid);
        var task = assertGetAllow(TaskType.class, TASK_USELESS_JACK.oid).asObjectable();
        assertThat(task.getActivity()).as("activity in the task").isNotNull();
        assertThat(task.getActivityState()).as("activity state in the task, denied by the autz").isNull();

        assertOwnTaskEditSchema(task.asPrismObject());

        assertSearch(UserType.class, null, 0);
        assertSearch(ObjectType.class, null, 1); // the task
        assertSearch(OrgType.class, null, 0);
        assertSearch(TaskType.class, null, 1);

        assertTaskAddAllow(TASK_T1_OID, "t1", USER_JACK_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T2_OID, "t2", USER_JACK_OID, "nonsense");
        assertTaskAddDeny(TASK_T3_OID, "t3", USER_ADMINISTRATOR_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T4_OID, "t4", USER_LECHUCK.oid, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T5_OID, "t5", null, TASK_USELESS_HANDLER_URI);

        assertAddDeny();

        assertModifyDeny();

        assertDeleteDeny();

        assertGlobalStateUntouched();
    }

    private void assertOwnTaskEditSchema(PrismObject<TaskType> task) throws Exception {
        PrismObjectDefinition<TaskType> def = getEditObjectDefinition(task);
        displayDumpable("task edit def", def);
        // All items are readable, except for `activityState` (see `read-self-task-owner` autz)
        // All items are "addable" (see `add-self-task-owner` autz)
        // No items are modifiable, as there's no autz to modify the task.
        assertItemFlags(def, TaskType.F_NAME, true, true, false);
        assertItemFlags(def, TaskType.F_DESCRIPTION, true, true, false);
        assertItemFlags(def, TaskType.F_ACTIVITY, true, true, false);
        assertItemFlags(def, TaskType.F_ACTIVITY_STATE, false, true, false);
    }

    /**
     * Searches for users with given assignment/targetRef (both directly and using EXISTS clause). See MID-7931.
     */
    @Test
    public void test400AutzJackSearchByAssignmentTargetRef() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SEARCH_USER_ASSIGNMENT_TARGET_REF.oid);

        when();
        login(USER_JACK_USERNAME);

        then("searching using the direct query yields guybrush");
        ObjectQuery directQuery = queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                .ref(ORG_SWASHBUCKLER_SECTION_OID, OrgType.COMPLEX_TYPE)
                .build();
        assertSearch(UserType.class, directQuery, 1); // guybrush

        and("searching using the 'exists' query yields guybrush as well");
        ObjectQuery existsQuery = queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(ORG_SWASHBUCKLER_SECTION_OID, OrgType.COMPLEX_TYPE)
                .endBlock()
                .build();
        assertSearch(UserType.class, existsQuery, 1); // guybrush
    }

    /**
     * Checks whether item configuration from object template is applicated for child item of multivalue container
     *
     * MID-8347
     */
    @Test
    public void test410ItemAccessMultivalueAttrChild() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanupAutzTest(USER_JACK_OID);

        assignRole(USER_JACK_OID, ROLE_SUPERUSER.oid);

        login(USER_JACK_USERNAME);

        when();

        PrismObject<UserType> user = modelService.getObject(
                UserType.class,
                USER_CHARLES.oid,
                GetOperationOptionsBuilder.create()
                        .definitionUpdate(DefinitionUpdateOption.ROOT_ONLY)
                        .build(),
                task,
                result);

        then();

        assertObjectDefinition(user.getDefinition())
                .container(UserType.F_OPERATION_EXECUTION)
                .property(OperationExecutionType.F_MESSAGE)
                .assertDenyAdd()
                .assertDenyModify()
                .assertAllowRead();
    }

    /**
     * Checks that no expressions are evaluated before the [first round of] authorization is checked,
     * as well as that `#assign` does not allow to create assignments with `targetRef` expressions.
     */
    @Test
    public void test420TargetRefExpression() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES.oid);
        dummyAuditService.setEnabled(true);
        dummyAuditService.clear();

        when();
        login(USER_JACK_USERNAME);

        then("targetRef expression is not evaluated during 'assignment add' operation");
        try {
            executeChanges(
                    deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT)
                            .add(createAssignmentWithExpression())
                            .asObjectDelta(USER_JACK_OID),
                    null, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
        assertThat(unauthorizedScriptRun).withFailMessage("Unauthorized script was run").isFalse();
        assertAssignOperationFailureAudited();

        and("targetRef expression is not evaluated during 'object add' operation");
        dummyAuditService.clear();
        try {
            UserType newUser = new UserType()
                    .name("new")
                            .assignment(createAssignmentWithExpression());
            executeChanges(
                    newUser.asPrismObject().createAddDelta(),
                    null, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
        assertThat(unauthorizedScriptRun).withFailMessage("Unauthorized script was run").isFalse();
        assertAssignOperationFailureAudited();
    }

    private AssignmentType createAssignmentWithExpression() throws SchemaException {
        var filterXml = """
                <q:inOid xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                         xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3">
                    <expression>
                        <script>
                            <code>
                                import com.evolveum.midpoint.model.intest.security.TestSecurityBasic
                                TestSecurityBasic.unauthorizedScriptRun = true
                                null
                            </code>
                        </script>
                    </expression>
                </q:inOid>
                """;

        var searchFilter = new SearchFilterType();
        searchFilter.setFilterClauseXNode(
                prismContext.parserFor(filterXml).parseToXNode());

        return new AssignmentType()
                .targetRef(new ObjectReferenceType()
                        .type(RoleType.COMPLEX_TYPE)
                        .filter(searchFilter));
    }

    private void assertAssignOperationFailureAudited() {
        displayDumpable("audit", dummyAuditService);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
        assertThat(dummyAuditService.getExecutionRecord(0).getMessage())
                .contains("User ''jack'' not authorized for operation ASSIGN");
    }

    /** Checks that the clockwork is not started if there's no relevant authorization. */
    @Test
    public void test430UnauthorizedClockworkStart() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY.oid);
        dummyAuditService.setEnabled(true);
        dummyAuditService.clear();

        when();
        login(USER_JACK_USERNAME);

        then("clockwork will not be started");
        try {
            executeChanges(
                    deltaFor(UserType.class) // empty delta
                            .asObjectDelta(USER_JACK_OID),
                    null, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }

        displayDumpable("audit", dummyAuditService);
    }

    /** Checks the authorization is enforced on selected model-level operations. */
    @Test
    public void test440UnauthorizedModelOperations() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given();
        cleanupAutzTest(USER_JACK_OID);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);

        when();
        login(USER_JACK_USERNAME);

        then("notifyChange cannot be called");
        try {
            modelService.notifyChange(
                    new ResourceObjectShadowChangeDescriptionType(),
                    task, result);
            fail("Unexpected success");
        } catch (AuthorizationException e) {
            displayExpectedException(e);
        }

        and("testResource cannot be called");
        try {
            modelService.testResource(RESOURCE_DUMMY_OID, task, result);
            fail("Unexpected success");
        } catch (AuthorizationException e) {
            displayExpectedException(e);
        }

        and("importFromResource (the whole) cannot be called");
        try {
            modelService.importFromResource(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS, task, result);
            fail("Unexpected success");
        } catch (AuthorizationException e) {
            displayExpectedException(e);
        }

        and("importFromResource (for single shadow) cannot be called");
        try {
            modelService.importFromResource(accountOid, task, result);
            fail("Unexpected success");
        } catch (AuthorizationException e) {
            displayExpectedException(e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void assertTaskAddAllow(String oid, String name, String ownerOid, String handlerUri) throws Exception {
        assertAllow("add task " + name,
                (task, result) -> addTask(oid, name, ownerOid, handlerUri, task, result));
    }

    private void assertTaskAddDeny(String oid, String name, String ownerOid, String handlerUri) throws Exception {
        assertDeny("add task " + name,
                (task, result) -> addTask(oid, name, ownerOid, handlerUri, task, result));
    }

    private void addTask(String oid, String name, String ownerOid, String handlerUri, Task execTask, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        PrismObject<TaskType> task = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class).instantiate();
        task.setOid(oid);
        TaskType taskType = task.asObjectable();
        taskType.setName(createPolyStringType(name));
        if (ownerOid != null) {
            ObjectReferenceType ownerRef = new ObjectReferenceType();
            ownerRef.setOid(ownerOid);
            taskType.setOwnerRef(ownerRef);
        }
        taskType.setHandlerUri(handlerUri);
        modelService.executeChanges(MiscSchemaUtil.createCollection(task.createAddDelta()), null, execTask, result);
    }

    private ItemPath ext(Object segment) {
        return ItemPath.create(ObjectType.F_EXTENSION, segment);
    }
}
