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
        addObject(ROLE_PERSONA_ADMIN_FILE);

        // Persona full name is computed by using this ordinary user template
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_OID, initResult);

        rememberSteadyResources();
    }

    protected abstract File getPersonaObjectTemplateFile();

    @Test
    public void test100AssignRolePersonaAdminToJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);

        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        display("Persona", persona);
        userJackAdminPersonaOid = persona.getOid();
        // Full name is computed by using ordinary user template
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        assertSubtype(persona, "admin");
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

        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        display("Persona", persona);
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        assertSubtype(persona, "admin");
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
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, createPolyString(USER_JACK_GIVEN_NAME_NEW));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);

        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        display("Persona", persona);
        // Full name mapping in ordinary user template is weak, fullname is not changed
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertSubtype(persona, "admin");
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

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        assertPersonaAfterUserPasswordChange(persona, USER_JACK_PASSWORD, USER_PASSWORD_2_CLEAR);
    }

    protected abstract void assertPersonaAfterUserPasswordChange(PrismObject<UserType> persona, String oldPersonaPassword, String newUserPassword) throws Exception;

    @Test
    public void test142ModifyPersonaPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        PrismObject<UserType> personaBefore = assertLinkedPersona(userBefore, UserType.class, "admin");

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

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
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
        PrismObject<UserType> personaBefore = assertLinkedPersona(userBefore, UserType.class, "admin");

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

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
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
        unassignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);

        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 0);

        assertNoObject(UserType.class, userJackAdminPersonaOid);

        assertSteadyResources();
    }

    private String toAdminPersonaUsername(String username) {
        return "a-"+username;
    }

}
