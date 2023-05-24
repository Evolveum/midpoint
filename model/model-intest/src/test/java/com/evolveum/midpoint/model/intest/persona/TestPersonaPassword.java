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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestPersonaPassword extends AbstractPersonaTest {

    protected static final File OBJECT_TEMPLATE_PERSONA_ADMIN_NO_PASSWORD_FILE = new File(TEST_DIR, "object-template-persona-admin-no-password.xml");

    protected static final File SECURITY_POLICY_PERSONA_FILE = new File(TEST_DIR, "security-policy-persona.xml");
    protected static final String SECURITY_POLICY_PERSONA_OID = "51545f14-b4df-11e7-a37a-d37b7c2b3f4c";

    protected static final File PASSWORD_POLICY_PERSONA_FILE = new File(TEST_DIR, "password-policy-persona.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        importObjectFromFile(PASSWORD_POLICY_PERSONA_FILE);
        importObjectFromFile(SECURITY_POLICY_PERSONA_FILE);

        setGlobalSecurityPolicy(SECURITY_POLICY_PERSONA_OID, initResult);
    }

    @Override
    protected File getPersonaObjectTemplateFile() {
        return OBJECT_TEMPLATE_PERSONA_ADMIN_NO_PASSWORD_FILE;
    }

    @Override
    protected void assertPersonaInitialPassword(PrismObject<UserType> persona, String userPassword) {
        assertUserNoPassword(persona);
    }

    @Override
    protected void assertPersonaAfterUserPasswordChange(
            PrismObject<UserType> persona, String oldPersonaPassword, String newUserPassword) {
        assertUserNoPassword(persona);
    }

    @Test
    @Override
    public void test145ModifyPersonaPasswordBack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        PrismObject<UserType> personaBefore = assertLinkedPersona(userBefore, UserType.class, ARCHETYPE_ADMIN.oid);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        try {
            // WHEN
            when();

            modifyUserChangePassword(personaBefore.getOid(), USER_PASSWORD_2_CLEAR, task, result);

            assertNotReached();
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);

        assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);
        assertPasswordMetadata(userAfter, false, null, startCal);
        assertPasswordHistoryEntries(userAfter);

        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, ARCHETYPE_ADMIN.oid);
        assertPersonaAfterPersonaPasswordChange(persona, USER_PASSWORD_3_CLEAR, null, startCal);
    }

}
