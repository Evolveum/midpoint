/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyExceptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Security tests for multitenant environment.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityMultitenant extends AbstractSecurityTest {

    public static final File TEST_DIR = new File("src/test/resources/security/multitenant");

    protected static final File ORG_MULTITENANT_FILE = new File(TEST_DIR, "org-multitenant.xml");

    protected static final String ORG_ROOT_OID = "00000000-8888-6666-a000-000000000000";
    protected static final String ROLE_TENANT_ADMIN_OID = "00000000-8888-6666-a000-100000000000";


    // ===[ Spacing Guild ]===

    protected static final String ORG_GUILD_OID = "00000000-8888-6666-a001-000000000000";

    protected static final TestObject<OrgType> ORG_JUNCTION = TestObject.file(
            TEST_DIR, "org-junction.xml", "00000000-8888-6666-a001-000000000001");
    protected static final TestObject<OrgType> ORG_GUILD_SUBTENANT = TestObject.file(
            TEST_DIR, "org-guild-subtenant.xml", "00000000-8888-6666-a001-000000000fff");

    protected static final String ROLE_GUILD_BROKEN_ADMIN_OID = "00000000-8888-6666-a001-100000000001";

    protected static final String ROLE_GUILD_NAVIGATOR_OID = "00000000-8888-6666-a001-100000000002";

    protected static final String USER_EDRIC_OID = "00000000-8888-6666-a001-200000000000";
    protected static final String USER_EDRIC_NAME = "edric";
    protected static final String USER_EDRIC_FULL_NAME = "Navigator Edric";

    protected static final TestObject<UserType> USER_DMURR = TestObject.file(
            TEST_DIR, "user-dmurr.xml", "00000000-8888-6666-a001-200000000001");

    private static final File RESOURCE_DUMMY_JUNCTION_FILE = new File(TEST_DIR, "resource-dummy-junction.xml");
    private static final String RESOURCE_DUMMY_JUNCTION_OID = "00000000-8888-6666-a001-300000000000";


    // ===[ House Corrino ]===

    protected static final String ORG_CORRINO_OID = "00000000-8888-6666-a100-000000000000";

    protected static final String ORG_KAITAIN_OID = "00000000-8888-6666-a100-000000000001";

    protected static final String ORG_IMPERIAL_PALACE_OID = "00000000-8888-6666-a100-000000000002";

    protected static final String ROLE_CORRINO_ADMIN_OID = "00000000-8888-6666-a100-100000000000";

    protected static final String ROLE_CORRINO_EMPEROR_OID = "00000000-8888-6666-a100-100000000001";

    protected static final String USER_SHADDAM_CORRINO_OID = "00000000-8888-6666-a100-200000000000";
    protected static final String USER_SHADDAM_CORRINO_NAME = "shaddam";
    protected static final String USER_SHADDAM_CORRINO_FULL_NAME = "Padishah Emperor Shaddam IV";


    // ===[ House Atreides ]===

    protected static final String ORG_ATREIDES_OID = "00000000-8888-6666-a200-000000000000";

    protected static final String ORG_CALADAN_OID = "00000000-8888-6666-a200-000000000001";

    protected static final File ORG_ARRAKIS_FILE = new File(TEST_DIR, "org-arrakis.xml");
    protected static final String ORG_ARRAKIS_OID = "00000000-8888-6666-a200-000000000002";
    protected static final String ORG_ARRAKIS_NAME = "Arrakis";
    protected static final String ORG_ARRAKIS_DISPLAY_NAME = "Planet Arrakis";

    protected static final File ORG_CASTLE_CALADAN_FILE = new File(TEST_DIR, "org-castle-caladan.xml");
    protected static final String ORG_CASTLE_CALADAN_OID = "00000000-8888-6666-a200-000000000003";
    protected static final String ORG_CASTLE_CALADAN_NAME = "Castle Caladan";
    protected static final String ORG_CASTLE_CALADAN_DISPLAY_NAME = "Castle Caladan";

    protected static final TestObject<OrgType> ORG_ATREIDES_SUBTENANT = TestObject.file(TEST_DIR, "org-atreides-subtenant.xml", "00000000-8888-6666-a200-000000000fff");

    protected static final String ROLE_ATREIDES_ADMIN_OID = "00000000-8888-6666-a200-100000000000";

    protected static final String ROLE_ATREIDES_END_USER_OID = "00000000-8888-6666-a200-100000000006";

    protected static final String ROLE_ATREIDES_ROLE_MANAGER_OID = "00000000-8888-6666-a200-100000000007";

    protected static final String ROLE_ATREIDES_GUARD_OID = "00000000-8888-6666-a200-100000000002";
    protected static final File ROLE_ATREIDES_GUARD_FILE = new File(TEST_DIR, "role-atreides-guard.xml");

    protected static final TestObject<RoleType> ROLE_ATREIDES_HACKER = TestObject.file(TEST_DIR, "role-atreides-hacker.xml", "00000000-8888-6666-a200-100000000003");

    protected static final String ROLE_ATREIDES_SOLDIER_OID = "00000000-8888-6666-a200-100000000004";

    protected static final String ROLE_ATREIDES_SWORDMASTER_OID = "00000000-8888-6666-a200-100000000005";
    protected static final File ROLE_ATREIDES_SWORDMASTER_FILE = new File(TEST_DIR, "role-atreides-swordmaster.xml");

    protected static final String USER_LETO_ATREIDES_OID = "00000000-8888-6666-a200-200000000000";
    protected static final String USER_LETO_ATREIDES_NAME = "leto";
    protected static final String USER_LETO_ATREIDES_FULL_NAME = "Leto Atreides";

    protected static final String USER_PAUL_ATREIDES_OID = "00000000-8888-6666-a200-200000000001";
    protected static final String USER_PAUL_ATREIDES_NAME = "paul";
    protected static final String USER_PAUL_ATREIDES_FULL_NAME = "Paul Atreides";

    protected static final File USER_DUNCAN_FILE = new File(TEST_DIR, "user-duncan.xml");
    protected static final String USER_DUNCAN_OID = "00000000-8888-6666-a200-200000000002";
    protected static final String USER_DUNCAN_NAME = "duncan";
    protected static final String USER_DUNCAN_FULL_NAME = "Duncan Idaho";

    private static final File RESOURCE_DUMMY_CASTLE_CALADAN_FILE = new File(TEST_DIR, "resource-dummy-castle-caladan.xml");
    private static final String RESOURCE_DUMMY_CASTLE_CALADAN_OID = "00000000-8888-6666-a200-300000000000";

    // ===[ House Harkonnen ]===

    protected static final String ORG_HARKONNEN_OID = "00000000-8888-6666-a300-000000000000";

    protected static final TestObject<OrgType> ORG_GIEDI_PRIME = TestObject.file(
            TEST_DIR, "org-giedi-prime.xml", "00000000-8888-6666-a300-000000000001");
    protected static final TestObject<OrgType> ORG_HARKONNEN_SUBTENANT = TestObject.file(
            TEST_DIR, "org-harkonnen-subtenant.xml", "00000000-8888-6666-a300-000000000fff");

    protected static final String ROLE_HARKONNEN_ADMIN_OID = "00000000-8888-6666-a300-100000000000";

    protected static final String USER_VLADIMIR_HARKONNEN_OID = "00000000-8888-6666-a300-200000000000";

    protected static final TestObject<UserType> USER_PITER = TestObject.file(
            TEST_DIR, "user-piter.xml", "00000000-8888-6666-a300-200000000001");

    private static final File RESOURCE_DUMMY_BARONY_FILE = new File(TEST_DIR, "resource-dummy-barony.xml");
    private static final String RESOURCE_DUMMY_BARONY_OID = "00000000-8888-6666-a300-300000000000";

    protected PrismObject<ConnectorType> dummyConnector;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        dummyConnector = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, initResult);
    }

    @Override
    protected boolean doAddOrgstruct() {
        return false;
    }

    @Override
    protected String getTopOrgOid() {
        return ORG_ROOT_OID;
    }

    protected static final int NUMBER_OF_IMPORTED_ROLES = 0;


    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
    }

    /**
     * Stay logged in as administrator. Make sure that our assumptions about
     * the users and roles are correct.
     */
    @Test
    public void test000Sanity() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        // WHEN
        when();
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(RoleType.class, null, getNumberOfRoles());

        assertReadAllow(NUMBER_OF_ALL_USERS);
        assertReadAllowRaw(NUMBER_OF_ALL_USERS);
        assertAddAllow();
        assertAddAllowRaw();
        assertModifyAllow();
        assertDeleteAllow();

        assertGlobalStateUntouched();
    }

    /**
     * Stay logged in as administrator.
     * Import orgstruct with tenant and roles and everything.
     * Make sure that tenantRefs are properly set (they are NOT part of imported file)
     *
     * MID-4882
     */
    @Test
    public void test010ImportOrgstruct() throws Exception {
        // GIVEN

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        importObjectsFromFileNotRaw(ORG_MULTITENANT_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();

        // House Atreides

        assertOrgAfter(ORG_ATREIDES_OID)
            .assertIsTenant()
            .assertTenantRef(ORG_ATREIDES_OID)
            .assignments()
                .single()
                    .assertTargetOid(ORG_ROOT_OID)
                    .end()
                .end()
            .assertLiveLinks(0)
            .assertParentOrgRefs(ORG_ROOT_OID);

        assertOrgAfter(ORG_CALADAN_OID)
            .assertTenant(null)
            .assertTenantRef(ORG_ATREIDES_OID)
            .assignments()
                .single()
                    .assertTargetOid(ORG_ATREIDES_OID)
                    .end()
                .end()
            .assertLiveLinks(0)
            .assertParentOrgRefs(ORG_ATREIDES_OID);

        assertRoleAfter(ROLE_ATREIDES_ADMIN_OID)
            .assertTenantRef(ORG_ATREIDES_OID)
            .assertParentOrgRefs(ORG_ATREIDES_OID);

        assertUserAfter(USER_LETO_ATREIDES_OID)
            .assertName(USER_LETO_ATREIDES_NAME)
            .assertFullName(USER_LETO_ATREIDES_FULL_NAME)
            .assignments()
                .assertOrg(ORG_ATREIDES_OID)
                .assertRole(ROLE_ATREIDES_ADMIN_OID)
                .end()
            .assertTenantRef(ORG_ATREIDES_OID)
            .assertParentOrgRefs(ORG_ATREIDES_OID)
            .assertLiveLinks(0);

        assertUserAfter(USER_PAUL_ATREIDES_OID)
            .assertName(USER_PAUL_ATREIDES_NAME)
            .assertFullName(USER_PAUL_ATREIDES_FULL_NAME)
            .assignments()
                .assertOrg(ORG_ATREIDES_OID)
                .assertRole(ROLE_ATREIDES_END_USER_OID)
                .end()
            .assertTenantRef(ORG_ATREIDES_OID)
            .assertParentOrgRefs(ORG_ATREIDES_OID)
            .assertLiveLinks(0);

        // Spacing Guild

        assertOrgAfter(ORG_GUILD_OID)
            .assertTenant(null)
            .assertTenantRef(null)
            .assignments()
                .single()
                    .assertTargetOid(ORG_ROOT_OID)
                    .end()
                .end()
            .assertLiveLinks(0)
            .assertParentOrgRefs(ORG_ROOT_OID);

        assertUserAfter(USER_EDRIC_OID)
            .assertName(USER_EDRIC_NAME)
            .assertFullName(USER_EDRIC_FULL_NAME)
            .assignments()
                .assertOrg(ORG_GUILD_OID)
                .assertRole(ROLE_GUILD_BROKEN_ADMIN_OID)
                .end()
            .assertTenantRef(null)
            .assertParentOrgRefs(ORG_GUILD_OID)
            .assertLiveLinks(0);

        // House Corrino

        assertOrgAfter(ORG_CORRINO_OID)
            .assertIsTenant()
            .assertTenantRef(ORG_CORRINO_OID)
            .assignments()
                .single()
                    .assertTargetOid(ORG_ROOT_OID)
                    .end()
                .end()
            .assertLiveLinks(0)
            .assertParentOrgRefs(ORG_ROOT_OID);

        assertOrgAfter(ORG_KAITAIN_OID)
            .assertTenant(null)
            .assertTenantRef(ORG_CORRINO_OID)
            .assignments()
                .single()
                    .assertTargetOid(ORG_CORRINO_OID)
                    .end()
                .end()
            .assertLiveLinks(0)
            .assertParentOrgRefs(ORG_CORRINO_OID);

        assertOrgAfter(ORG_IMPERIAL_PALACE_OID)
            .assertTenant(null)
            .assertTenantRef(ORG_CORRINO_OID)
            .assignments()
                .single()
                    .assertTargetOid(ORG_KAITAIN_OID)
                    .end()
                .end()
            .assertLiveLinks(0)
            .assertParentOrgRefs(ORG_KAITAIN_OID);

        assertGlobalStateUntouched();
    }

    /**
     * Leto is Atreides admin. He can see all of House Atreides.
     * But nothing else.
     *
     * MID-4882
     */
    @Test
    public void test100AutzLetoRead() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertGetAllow(UserType.class, USER_LETO_ATREIDES_OID);
        assertGetAllow(UserType.class, USER_PAUL_ATREIDES_OID);
        assertGetAllow(OrgType.class, ORG_ATREIDES_OID);
        assertGetAllow(RoleType.class, ROLE_ATREIDES_ADMIN_OID);

        // Wrong tenant
        assertGetDeny(UserType.class, USER_VLADIMIR_HARKONNEN_OID);
        assertGetDeny(OrgType.class, ORG_HARKONNEN_OID);
        assertGetDeny(RoleType.class, ROLE_HARKONNEN_ADMIN_OID);

        // No tenant
        assertGetDeny(OrgType.class, ORG_GUILD_OID);
        assertGetDeny(RoleType.class, ROLE_TENANT_ADMIN_OID);
        assertGetDeny(UserType.class, USER_EDRIC_OID);

        // Search
        assertSearch(UserType.class, null, USER_LETO_ATREIDES_OID, USER_PAUL_ATREIDES_OID);
        assertSearch(RoleType.class, null, ROLE_ATREIDES_ADMIN_OID, ROLE_ATREIDES_END_USER_OID, ROLE_ATREIDES_ROLE_MANAGER_OID, ROLE_ATREIDES_SOLDIER_OID);
        assertSearch(OrgType.class, null, ORG_ATREIDES_OID, ORG_CALADAN_OID);

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * MID-4882
     */
    @Test
    public void test102AutzLetoAdd() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertAddAllow(USER_DUNCAN_FILE);

        // Wrong tenant
        assertAddDeny(USER_PITER);

        // No tenant
        assertAddDeny(USER_DMURR);

        // THEN
        then();

        login(USER_ADMINISTRATOR_USERNAME);

        assertUserAfter(USER_DUNCAN_OID)
            .assertName(USER_DUNCAN_NAME)
            .assertFullName(USER_DUNCAN_FULL_NAME)
            .assignments()
                .assertOrg(ORG_ATREIDES_OID)
                .assertNoRole()
                .end()
            .assertTenantRef(ORG_ATREIDES_OID)
            .assertParentOrgRefs(ORG_ATREIDES_OID)
            .assertLiveLinks(0);

        assertGlobalStateUntouched();
    }

    // TODO: add role with authorizations

    /**
     * MID-4882
     */
    @Test
    public void test104AutzLetoModify() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertModifyAllow(UserType.class, USER_PAUL_ATREIDES_OID, UserType.F_LOCALITY, PolyString.fromOrig("Arrakis"));

        // Wrong tenant
        assertModifyDeny(UserType.class, USER_VLADIMIR_HARKONNEN_OID, UserType.F_LOCALITY, PolyString.fromOrig("Deepest hell"));

        // No tenant
        assertModifyDeny(UserType.class, USER_EDRIC_OID, UserType.F_LOCALITY, PolyString.fromOrig("Whatever"));

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    // TODO: add authorizations to existing role

    /**
     * MID-4882
     */
    @Test
    public void test106AutzLetoAddResourceTask() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertAddDummyResourceAllow(RESOURCE_DUMMY_CASTLE_CALADAN_FILE);

        // Wrong tenant
        assertAddDummyResourceDeny(RESOURCE_DUMMY_BARONY_FILE);

        // No tenant
        assertAddDummyResourceDeny(RESOURCE_DUMMY_JUNCTION_FILE);

        // THEN
        then();

        login(USER_ADMINISTRATOR_USERNAME);

        assertGlobalStateUntouched();
    }

    private void assertAddDummyResourceAllow(File file) throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(file);
        resource.asObjectable()
            .connectorRef(dummyConnector.getOid(), ConnectorType.COMPLEX_TYPE);
        assertAddAllow(resource, null);
    }

    private void assertAddDummyResourceDeny(File file) throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(file);
        resource.asObjectable()
            .connectorRef(dummyConnector.getOid(), ConnectorType.COMPLEX_TYPE);
        assertAddDeny(resource, null);
    }

    /**
     * MID-4882
     */
    @Test
    public void test109AutzLetoDelete() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertDeleteAllow(UserType.class, USER_DUNCAN_OID);

        // Wrong tenant
        assertDeleteDeny(UserType.class, USER_PITER.oid);

        // No tenant
        assertDeleteDeny(UserType.class, USER_DMURR.oid);

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * MID-4882
     */
    @Test
    public void test110AutzLetoAddOrgs() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertAddAllow(ORG_ARRAKIS_FILE);
        assertAddAllow(ORG_CASTLE_CALADAN_FILE);

        // Wrong tenant
        assertAddDeny(ORG_GIEDI_PRIME);

        // No tenant
        assertAddDeny(ORG_JUNCTION);

        // THEN
        then();

        login(USER_ADMINISTRATOR_USERNAME);

        assertOrgAfter(ORG_ARRAKIS_OID)
            .assertName(ORG_ARRAKIS_NAME)
            .assertDisplayName(ORG_ARRAKIS_DISPLAY_NAME)
            .assignments()
                .assertOrg(ORG_ATREIDES_OID)
                .assertNoRole()
                .end()
            .assertTenantRef(ORG_ATREIDES_OID)
            .assertParentOrgRefs(ORG_ATREIDES_OID)
            .assertLiveLinks(0);

        assertOrgAfter(ORG_CASTLE_CALADAN_OID)
            .assertName(ORG_CASTLE_CALADAN_NAME)
            .assertDisplayName(ORG_CASTLE_CALADAN_DISPLAY_NAME)
            .assignments()
                .assertOrg(ORG_CALADAN_OID)
                .assertNoRole()
                .end()
            .assertTenantRef(ORG_ATREIDES_OID)
            .assertParentOrgRefs(ORG_CALADAN_OID)
            .assertLiveLinks(0);

        assertGlobalStateUntouched();
    }

    /**
     * Tenant admin must not be able to add, modify or delete a tenant.
     */
    @Test
    public void test112AutzLetoProtectTenant() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        // Matching tenant
        assertAddDeny(ORG_ATREIDES_SUBTENANT);
        assertModifyDeny(OrgType.class, ORG_ATREIDES_OID, OrgType.F_LOCALITY, PolyString.fromOrig("Arrakis"));
        assertModifyDeny(OrgType.class, ORG_ATREIDES_OID, OrgType.F_TENANT, false);
        assertModifyDeny(OrgType.class, ORG_ATREIDES_OID, OrgType.F_TENANT /* no value */);
        // Attempt to "move" tenant, make it a root node
        assertDeny("unassign root",
                (task, result) -> unassignOrg(OrgType.class, ORG_ATREIDES_OID, ORG_ROOT_OID, task, result));
        // Attempt to assign new org to tenant. Target of new assignment is org that we control.
        assertDeny("assign caladan",
                (task, result) -> assignOrg(OrgType.class, ORG_ATREIDES_OID, ORG_CALADAN_OID, task, result));
        // Attempt to assign new org to tenant. Target of new assignment is org that we do not control.
        assertDeny("assign kaitain",
                (task, result) -> assignOrg(OrgType.class, ORG_ATREIDES_OID, ORG_KAITAIN_OID, task, result));
        assertDeleteDeny(OrgType.class, ORG_ATREIDES_OID);

        // Wrong tenant
        assertAddDeny(ORG_HARKONNEN_SUBTENANT);
        assertModifyDeny(OrgType.class, ORG_HARKONNEN_OID, OrgType.F_LOCALITY, PolyString.fromOrig("Arrakis"));
        assertModifyDeny(OrgType.class, ORG_HARKONNEN_OID, OrgType.F_TENANT, false);
        assertModifyDeny(OrgType.class, ORG_HARKONNEN_OID, OrgType.F_TENANT /* no value */);
        // Attempt to "move" tenant, make it a root node
        assertDeny("unassign root",
                (task, result) -> unassignOrg(OrgType.class, ORG_HARKONNEN_OID, ORG_ROOT_OID, task, result));
        // Attempt to assign new org to tenant. Target of new assignment is org that we control.
        assertDeny("assign caladan",
                (task, result) -> assignOrg(OrgType.class, ORG_HARKONNEN_OID, ORG_CALADAN_OID, task, result));
        // Attempt to assign new org to tenant. Target of new assignment is org that we do not control.
        assertDeny("unassign root",
                (task, result) -> assignOrg(OrgType.class, ORG_HARKONNEN_OID, ORG_KAITAIN_OID, task, result));
        assertDeleteDeny(OrgType.class, ORG_HARKONNEN_OID);

        // No tenant
        assertAddDeny(ORG_GUILD_SUBTENANT);
        assertModifyDeny(OrgType.class, ORG_GUILD_OID, OrgType.F_LOCALITY, PolyString.fromOrig("Arrakis"));
        assertModifyDeny(OrgType.class, ORG_GUILD_OID, OrgType.F_TENANT, false);
        assertModifyDeny(OrgType.class, ORG_GUILD_OID, OrgType.F_TENANT /* no value */);
        // Attempt to "move" tenant, make it a root node
        assertDeny("unassign root",
                (task, result) -> unassignOrg(OrgType.class, ORG_GUILD_OID, ORG_ROOT_OID, task, result));
        // Attempt to assign new org to tenant. Target of new assignment is org that we control.
        assertDeny("assign caladan",
                (task, result) -> assignOrg(OrgType.class, ORG_GUILD_OID, ORG_CALADAN_OID, task, result));
        // Attempt to assign new org to tenant. Target of new assignment is org that we do not control.
        assertDeny("unassign root",
                (task, result) -> assignOrg(OrgType.class, ORG_GUILD_OID, ORG_KAITAIN_OID, task, result));
        assertDeleteDeny(OrgType.class, ORG_GUILD_OID);

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * Make sure that tenant admin cannot break tenant isolation.
     * E.g. that cannot move object outside of his domain of control.
     */
    @Test
    public void test114AutzLetoKeepWithinTenant() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        assertAddAllow(ROLE_ATREIDES_GUARD_FILE);

        assertAllow("assign guard to arrakis",
                (task, result) -> assignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ARRAKIS_OID, task, result));

        assertRoleAfter(ROLE_ATREIDES_GUARD_OID)
            .assertTenantRef(ORG_ATREIDES_OID);

        // Guard role is still in the same tenant, so this should go well.
        assertAllow("unassign guard from caladan",
                (task, result) -> unassignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_CALADAN_OID, task, result));

        // Last assignment that keeps guard in atreides tenant. We cannot remove it.
        assertDeny("unassign guard from arrakis",
                (task, result) -> unassignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ARRAKIS_OID, task, result));

        // Make sure direct assign and unassign of tenant works freely - as long as there is another assignment that keeps object within a tenant
        assertAllow("assign guard to house atreides",
                (task, result) -> assignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ATREIDES_OID, task, result));
        assertAllow("assign guard to house atreides",
                (task, result) -> unassignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ATREIDES_OID, task, result));

        assertAllow("assign guard to house atreides",
                (task, result) -> assignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ATREIDES_OID, task, result));

        // We can unassign arrakis now. Guard is directly under house Atreides
        assertAllow("unassign guard from arrakis",
                (task, result) -> unassignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ARRAKIS_OID, task, result));

        // But we cannot unassign tenant now. That would move guard outside of tenancy.
        assertDeny("unassign guard from atreides",
                (task, result) -> unassignOrg(RoleType.class, ROLE_ATREIDES_GUARD_OID, ORG_ATREIDES_OID, task, result));

        assertRoleAfter(ROLE_ATREIDES_GUARD_OID)
            .assertTenantRef(ORG_ATREIDES_OID);

        assertDeleteAllow(RoleType.class, ROLE_ATREIDES_GUARD_OID);

        // This would make Castle Caladan a root object - outside out tenant zone of control.
        assertDeny("unassign caladan castle from caladan",
                (task, result) -> unassignOrg(OrgType.class, ORG_CASTLE_CALADAN_OID, ORG_CALADAN_OID, task, result));

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * Make sure that tenant admin cannot break tenant admin role.
     */
    @Test
    public void test116AutzLetoProtectTenantAdminRole() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        // WHEN
        when();

        assertAddDeny(ROLE_ATREIDES_HACKER);

        AuthorizationType superuserAuthorization = new AuthorizationType()
                .action(AuthorizationConstants.AUTZ_ALL_URL);
        assertDeny("add authorizations to atreides admin",
                (task, result) -> modifyObjectAddContainer(RoleType.class, ROLE_ATREIDES_ADMIN_OID,
                        RoleType.F_AUTHORIZATION, task, result, superuserAuthorization));

        assertDeny("induce superuser",
                (task, result) -> induceRole(ROLE_ATREIDES_ADMIN_OID, ROLE_SUPERUSER_OID, task, result));

        assertDeny("add dummy account",
                (task, result) -> assignAccount(UserType.class, USER_PAUL_ATREIDES_OID, RESOURCE_DUMMY_OID, null, task, result));

        PolicyRuleType policyRule = new PolicyRuleType();
        policyRule
            .beginPolicyConstraints()
                .beginMinAssignees()
                    .multiplicity("1");

        assertDeny("assign policy rule",
                (task, result) -> assignPolicyRule(RoleType.class, ROLE_ATREIDES_ADMIN_OID, policyRule, task, result));

        AssignmentType policyExceptionAssignment = new AssignmentType();
        policyExceptionAssignment
            .beginPolicyException()
                .ruleName("foobar");

        assertDeny("assign policy exception",
                (task, result) -> assign(RoleType.class, ROLE_ATREIDES_ADMIN_OID, policyExceptionAssignment, task, result));

        PolicyExceptionType policyException = new PolicyExceptionType()
                .ruleName("foofoo");
        assertDeny("add policyException to atreides admin",
                (task, result) -> modifyObjectAddContainer(RoleType.class, ROLE_ATREIDES_ADMIN_OID,
                        RoleType.F_POLICY_EXCEPTION, task, result, policyException));

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * Make sure that tenant admin can manage business roles.
     */
    @Test
    public void test118AutzLetoBusinessRoles() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);

        assertAddAllow(ROLE_ATREIDES_GUARD_FILE);

        // WHEN
        when();

        assertAddAllow(ROLE_ATREIDES_SWORDMASTER_FILE);

        assertDeny("induce superuser",
                (task, result) -> induceRole(ROLE_ATREIDES_SWORDMASTER_OID, ROLE_SUPERUSER_OID, task, result));

        assertAllow("uninduce soldier from swordmaster",
                (task, result) -> uninduceRole(ROLE_ATREIDES_SWORDMASTER_OID, ROLE_ATREIDES_SOLDIER_OID, task, result));

        assertAllow("induce soldier to swordmaster",
                (task, result) -> induceRole(ROLE_ATREIDES_SWORDMASTER_OID, ROLE_ATREIDES_SOLDIER_OID, task, result));

        assertDeny("unassign swordmaster from atreides",
                (task, result) -> unassignOrg(RoleType.class, ROLE_ATREIDES_SWORDMASTER_OID, ORG_ATREIDES_OID, task, result));

        assertDeleteAllow(RoleType.class, ROLE_ATREIDES_SWORDMASTER_OID);

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * Make sure that Paul can use end-user priviliges.
     * In particular that he can assign requestable roles.
     */
    @Test
    public void test120AutzPaulEndUser() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        addObject(ROLE_ATREIDES_SWORDMASTER_FILE);

        login(USER_PAUL_ATREIDES_NAME);

        // WHEN
        when();

        // Requestable role
        assertAllow("assign guard to paul",
                (task, result) -> assignRole(USER_PAUL_ATREIDES_OID, ROLE_ATREIDES_GUARD_OID, task, result));

        // Non-requestable role
        assertDeny("assign swordmaster to paul",
                (task, result) -> assignRole(USER_PAUL_ATREIDES_OID, ROLE_ATREIDES_SWORDMASTER_OID, task, result));

        // Requestable role, no tenant
        assertDeny("assign swordmaster to paul",
                (task, result) -> assignRole(USER_PAUL_ATREIDES_OID, ROLE_GUILD_NAVIGATOR_OID, task, result));

        // Requestable role, wrong tenant
        assertDeny("assign swordmaster to paul",
                (task, result) -> assignRole(USER_PAUL_ATREIDES_OID, ROLE_CORRINO_EMPEROR_OID, task, result));

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    @Test
    public void test122AutzDuncanRoleManager() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        addObject(USER_DUNCAN_FILE);
        assignRole(USER_DUNCAN_OID, ROLE_ATREIDES_ROLE_MANAGER_OID);

        login(USER_DUNCAN_NAME);

        // WHEN
        when();

        assertDeny("assign guard to paul",
                (task, result) -> assignRole(USER_PAUL_ATREIDES_OID, ROLE_ATREIDES_GUARD_OID, task, result));

        assertAllow("induce swordmaster end user",
                (task, result) -> induceRole(ROLE_ATREIDES_SWORDMASTER_OID, ROLE_ATREIDES_END_USER_OID, task, result));

        // Outside of tenant
        assertDeny("induce superuser",
                (task, result) -> induceRole(ROLE_ATREIDES_SWORDMASTER_OID, ROLE_SUPERUSER_OID, task, result));

        // Only role inducements are allowed, not assignments
        assertDeny("assign swordmaster to admin",
                (task, result) -> assignRole(RoleType.class, ROLE_ATREIDES_SWORDMASTER_OID, ROLE_ATREIDES_ADMIN_OID, task, result));

        assertAllow("assign swordmaster to castle caladan",
                (task, result) -> assignOrg(RoleType.class, ROLE_ATREIDES_SWORDMASTER_OID, ORG_CASTLE_CALADAN_OID, task, result));

        // Only role assignments are allowed, not inducements
        assertDeny("induce caladan",
                (task, result) -> induceOrg(RoleType.class, ROLE_ATREIDES_SWORDMASTER_OID, ORG_CALADAN_OID, task, result));

        // THEN
        then();

        assertGlobalStateUntouched();
    }

    /**
     * Edric is part of Spacing Guild. But the Guild is not tenant.
     * Edric has a broken role that should work only for tenants.
     * Therefore the role should not work. Edric should not be
     * able to access anything.
     *
     * MID-4882
     */
    @Test
    public void test130AutzEdricRead() throws Exception {
        // GIVEN
        cleanupAutzTest(null);

        login(USER_EDRIC_NAME);

        // WHEN
        when();

        // Wrong tenant
        assertGetDeny(UserType.class, USER_LETO_ATREIDES_OID);
        assertGetDeny(UserType.class, USER_PAUL_ATREIDES_OID);
        assertGetDeny(OrgType.class, ORG_ATREIDES_OID);
        assertGetDeny(RoleType.class, ROLE_ATREIDES_ADMIN_OID);

        // No tenant
        assertGetDeny(OrgType.class, ORG_GUILD_OID);
        assertGetDeny(RoleType.class, ROLE_TENANT_ADMIN_OID);
        assertGetDeny(UserType.class, USER_EDRIC_OID);

        assertSearch(UserType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        assertSearch(OrgType.class, null, 0);

        // THEN
        then();

        assertGlobalStateUntouched();
    }




}
