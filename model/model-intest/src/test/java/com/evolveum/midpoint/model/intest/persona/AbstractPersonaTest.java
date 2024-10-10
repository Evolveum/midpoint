/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.persona;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractPersonaTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/persona");

    protected static final String USER_JACK_GIVEN_NAME_NEW = "Jackie";

    protected static final String USER_PASSWORD_2_CLEAR = "bl4ckP3arl";
    protected static final String USER_PASSWORD_3_CLEAR = "wh3r3sTheRum";

    protected String userJackAdminPersonaOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        addObject(getPersonaObjectTemplateFile());
        ARCHETYPE_ADMIN.init(this, initTask, initResult);
        ARCHETYPE_PERSONA_ROLE.init(this, initTask, initResult);
        addObject(ROLE_PERSONA_ADMIN_FILE);

        // Persona full name is computed by using this ordinary user template
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_OID, initResult);

        rememberSteadyResources();
    }

    protected abstract File getPersonaObjectTemplateFile();

    /** Testing simulation of persona assignment. Personal links should not be created in the repo. MID-10080. */
    @Test
    public void test090AssignRolePersonaAdminToJackSimulated() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        skipIfNotNativeRepository();

        when("persona role is added in the simulation mode");
        var simResult =
                executeInProductionSimulationMode(
                null, task, result,
                () -> assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN.oid, task, result));

        then("no personaRef should be created");
        assertSuccess(result);
        assertPersonaLinks(getUser(USER_JACK_OID), 0); // simulations -> no persona refs should be added

        and("simulation result should be OK: 1 user added, 1 modified");
        assertProcessedObjects(simResult, "after")
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find(
                        a -> a.delta()
                                .objectToAdd()
                                .asUser()
                                .assertName("a-jack")
                                .assignments()
                                .single()
                                .assertTargetOid(ARCHETYPE_ADMIN.oid)
                                .assertTargetType(ArchetypeType.COMPLEX_TYPE)
                                .end().end().end().end().end())
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.delta()
                                .assertModified(UserType.F_ASSIGNMENT) // personaRef is not simulated now; see MID-10100
                                .end())
                .assertSize(2);
    }

    /**
     * As {@link #test090AssignRolePersonaAdminToJackSimulated()} but using preview changes (to work on generic repo as well).
     *
     * MID-10080.
     */
    @Test
    public void test095AssignRolePersonaAdminToJackPreview() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("persona role is added (preview changes)");
        previewChanges(
                deltaFor(UserType.class)
                        .asObjectDelta(USER_JACK_OID),
                null, task, result);

        then("no personaRef should be created");
        assertSuccess(result);
        assertPersonaLinks(getUser(USER_JACK_OID), 0); // preview -> no persona refs should be added

        // TODO we should somehow support two model contexts here (that we currently do not)
    }

    @Test
    public void test100AssignRolePersonaAdminToJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN.oid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);

        assertLiveLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        display("Persona", persona);
        userJackAdminPersonaOid = persona.getOid();
        // Full name is computed by using ordinary user template
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        assertHasArchetype(persona, ARCHETYPE_ADMIN.oid);
        assertPersonaInitialPassword(persona, USER_JACK_PASSWORD);

        assertSteadyResources();
    }

    protected abstract void assertPersonaInitialPassword(PrismObject<UserType> persona, String userPassword) throws Exception;

    @Test
    public void test102RecomputeUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserJack11x();
    }

    @Test
    public void test103ReconcileUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserJack11x();
    }

    @Test
    public void test104RecomputeJackAdminPersona() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(userJackAdminPersonaOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserJack11x();
    }

    @Test
    public void test105ReconcileJackAdminPersona() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(userJackAdminPersonaOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserJack11x();
    }

    private void assertUserJack11x() throws Exception {
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);

        assertLiveLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        display("Persona", persona);
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        assertHasArchetype(persona, ARCHETYPE_ADMIN.oid);
        assertPersonaInitialPassword(persona, USER_JACK_PASSWORD);

        assertSteadyResources();
    }

    @Test
    public void test120ModifyJackGivenName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, PolyString.fromOrig(USER_JACK_GIVEN_NAME_NEW));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);

        assertLiveLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        display("Persona", persona);
        // Full name mapping in ordinary user template is weak, fullname is not changed
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertHasArchetype(persona, ARCHETYPE_ADMIN.oid);
        assertPersonaInitialPassword(persona, USER_JACK_PASSWORD);

        assertSteadyResources();
    }

    // TODO: recompute, reconcile (both user and persona)

    // TODO: change password

    @Test
    public void test140ModifyUserJackPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_2_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);

        assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);
        assertPasswordMetadata(userAfter, false, startCal, endCal);
        assertPasswordHistoryEntries(userAfter);

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        assertPersonaAfterUserPasswordChange(persona, USER_JACK_PASSWORD, USER_PASSWORD_2_CLEAR);
    }

    protected abstract void assertPersonaAfterUserPasswordChange(PrismObject<UserType> persona, String oldPersonaPassword, String newUserPassword) throws Exception;

    @Test
    public void test142ModifyPersonaPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        PrismObject<UserType> personaBefore = assertLinkedPersona(userBefore, UserType.class, ARCHETYPE_ADMIN.oid);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserChangePassword(personaBefore.getOid(), USER_PASSWORD_3_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);

        assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);
        assertPasswordMetadata(userAfter, false, null, startCal);
        assertPasswordHistoryEntries(userAfter);

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        assertPersonaAfterPersonaPasswordChange(persona, USER_PASSWORD_3_CLEAR, startCal, endCal);
    }

    protected void assertPersonaAfterPersonaPasswordChange(PrismObject<UserType> persona, String newPersonaPassword, XMLGregorianCalendar startCal, XMLGregorianCalendar endCal) throws Exception {
        assertUserPassword(persona, newPersonaPassword);
        assertPasswordMetadata(persona, false, startCal, endCal);
    }

    @Test
    public void test145ModifyPersonaPasswordBack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        PrismObject<UserType> personaBefore = assertLinkedPersona(userBefore, UserType.class, ARCHETYPE_ADMIN.oid);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserChangePassword(personaBefore.getOid(), USER_PASSWORD_2_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);

        assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);
        assertPasswordMetadata(userAfter, false, null, startCal);
        assertPasswordHistoryEntries(userAfter);

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        assertPersonaAfterPersonaPasswordChange(persona, USER_PASSWORD_2_CLEAR, startCal, endCal);
    }


    // TODO: assign some accounts/roles to user and persona, make sure they are independent

    // TODO: independent change in the persona

    @Test
    public void test199UnassignRolePersonaAdminFromJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN.oid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);

        assertLiveLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 0);

        assertNoObject(UserType.class, userJackAdminPersonaOid);

        assertSteadyResources();
    }

    private String toAdminPersonaUsername(String username) {
        return "a-"+username;
    }

}
