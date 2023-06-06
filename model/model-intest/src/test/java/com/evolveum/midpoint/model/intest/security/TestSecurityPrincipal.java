/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityPrincipal extends AbstractSecurityTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test025GetUserAdministrator() throws Exception {
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_ADMINISTRATOR_USERNAME, UserType.class);

        // THEN
        displayDumpable("Administrator principal", principal);
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthorizationAllow(principal.getAuthorities().iterator().next(), AuthorizationConstants.AUTZ_ALL_URL);

        assertAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
    }

    @Test
    public void test050GetUserJack() throws Exception {
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        assertNoAuthentication();
        assertJack(principal);
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());

        assertNoAuthentication();
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNoAuthentication();
    }

    @Test
    public void test051GetUserBarbossa() throws Exception {
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_BARBOSSA_USERNAME, UserType.class);

        // THEN
        displayDumpable("Principal barbossa", principal);
        assertNotNull("No principal for username "+USER_BARBOSSA_USERNAME, principal);
        assertEquals("wrong username", USER_BARBOSSA_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_BARBOSSA_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal barbossa", principal.getFocus().asPrismObject());

        principal.getFocus().asPrismObject().checkConsistence(true, true);

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
    }

    @Test
    public void test052GetUserGuybrush() throws Exception {
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_GUYBRUSH_USERNAME, UserType.class);

        // THEN
        displayDumpable("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getFocus().asPrismObject());

        principal.getFocus().asPrismObject().checkConsistence(true, true);

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
    }

    @Test
    public void test060GuybrushConditionalRoleFalse() throws Exception {
        login(USER_ADMINISTRATOR_USERNAME);

        assignRole(USER_GUYBRUSH_OID, ROLE_CONDITIONAL.oid);

        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_GUYBRUSH_USERNAME, UserType.class);

        // THEN
        displayDumpable("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getFocus().asPrismObject());

        principal.getFocus().asPrismObject().checkConsistence(true, true);

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNotAuthorized(principal, AUTZ_SUPERSPECIAL_URL);
        assertNotAuthorized(principal, AUTZ_NONSENSE_URL);
    }

    @Test
    public void test061GuybrushConditionalRoleTrue() throws Exception {
        login(USER_ADMINISTRATOR_USERNAME);

        Task task = getTestTask();
        OperationResult result = task.getResult();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, "special");

        resetAuthentication();

        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_GUYBRUSH_USERNAME, UserType.class);

        // THEN
        then();
        displayDumpable("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        display("User in principal guybrush", principal.getFocus().asPrismObject());

        principal.getFocus().asPrismObject().checkConsistence(true, true);

        assertAuthorized(principal, AUTZ_SUPERSPECIAL_URL);
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNotAuthorized(principal, AUTZ_CAPSIZE_URL);
        assertNotAuthorized(principal, AUTZ_NONSENSE_URL);
    }

    @Test
    public void test062GuybrushConditionalRoleUnassign() throws Exception {
        login(USER_ADMINISTRATOR_USERNAME);

        unassignRole(USER_GUYBRUSH_OID, ROLE_CONDITIONAL.oid);

        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_GUYBRUSH_USERNAME, UserType.class);

        // THEN
        displayDumpable("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getFocus().asPrismObject());

        principal.getFocus().asPrismObject().checkConsistence(true, true);

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
    }

    @Test
    public void test100JackRolePirate() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        assertJack(principal);

        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthorizationAllow(principal.getAuthorities().iterator().next(), AUTZ_LOOT_URL);

        assertAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.EXECUTION);
        assertNotAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.REQUEST);
        assertNotAuthorized(principal, AUTZ_LOOT_URL, null);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);

        assertCompiledGuiProfile(principal)
            .assertAdditionalMenuLinks(1)
            .assertUserDashboardLinks(2)
            .assertObjectCollectionViews(3)
            .assertUserDashboardWidgets(2);
    }

    @Test
    public void test109JackUnassignRolePirate() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = getTestTask();
        OperationResult result = task.getResult();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        assertJack(principal);

        assertEquals("Wrong number of authorizations", 0, principal.getAuthorities().size());

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);

        assertCompiledGuiProfile(principal)
            .assertAdditionalMenuLinks(0)
            .assertUserDashboardLinks(1)
            .assertObjectCollectionViews(3)
            .assertUserDashboardWidgets(0);
    }

    @Test
    public void test110GuybrushRoleNicePirate() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_NICE_PIRATE_OID, task, result);

        resetAuthentication();

        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_GUYBRUSH_USERNAME, UserType.class);

        // THEN
        then();
        displayDumpable("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 2, principal.getAuthorities().size());

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
    }

    @Test
    public void test111GuybrushRoleCaptain() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);

        resetAuthentication();

        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_GUYBRUSH_USERNAME, UserType.class);

        // THEN
        then();
        displayDumpable("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 3, principal.getAuthorities().size());

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
    }

    @Test
    public void test119GuybrushUnassignRoles() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = getTestTask();
        OperationResult result = task.getResult();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        unassignRole(USER_JACK_OID, ROLE_CAPTAIN_OID, task, result);

        resetAuthentication();

        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        then();
        assertEquals("Wrong number of authorizations", 0, principal.getAuthorities().size());

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
    }

    /**
     * MID-3650
     */
    @Test
    public void test120JackRoleIndirectPirate() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        assignRole(USER_JACK_OID, ROLE_INDIRECT_PIRATE.oid);

        resetAuthentication();

        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        then();
        displayDumpable("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());

        assertAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.EXECUTION);

        login(USER_ADMINISTRATOR_USERNAME);
        unassignRole(USER_JACK_OID, ROLE_INDIRECT_PIRATE.oid);
    }

    /**
     * MID-3650
     */
    @Test
    public void test122JackOrgIndirectPirate() throws Exception {
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        assignOrg(USER_JACK_OID, ORG_INDIRECT_PIRATE.oid);

        resetAuthentication();

        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        then();
        displayDumpable("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());

        assertAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.EXECUTION);

        login(USER_ADMINISTRATOR_USERNAME);
        unassignOrg(USER_JACK_OID, ORG_INDIRECT_PIRATE.oid);
    }
}
