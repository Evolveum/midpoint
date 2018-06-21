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
	protected static final String PASSWORD_POLICY_PERSONA_OID = "5d9e068a-b4df-11e7-be31-a7b0fef77d95";
	
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
	protected void assertPersonaInitialPassword(PrismObject<UserType> persona, String userPassword) throws Exception {
		assertUserNoPassword(persona);
	}
	
	@Override
	protected void assertPersonaAfterUserPasswordChange(PrismObject<UserType> persona, String oldPersonaPassword, String newUserPassword) throws Exception {
		assertUserNoPassword(persona);
	}
	
	@Test
	@Override
    public void test145ModifyPersonaPasswordBack() throws Exception {
		final String TEST_NAME = "test145ModifyPersonaPasswordBack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        PrismObject<UserType> personaBefore = assertLinkedPersona(userBefore, UserType.class, "admin");

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        try {
			// WHEN
	        displayWhen(TEST_NAME);
	        
	        modifyUserChangePassword(personaBefore.getOid(), USER_PASSWORD_2_CLEAR, task, result);
	        
	        assertNotReached();
        } catch (PolicyViolationException e) {
        	// expected
        	display("expected exception", e);
        }

		// THEN
        displayThen(TEST_NAME);
		assertFailure(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);

		assertUserPassword(userAfter, USER_PASSWORD_2_CLEAR);
		assertPasswordMetadata(userAfter, false, null, startCal);
		assertPasswordHistoryEntries(userAfter);
		
		PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
		assertPersonaAfterPersonaPasswordChange(persona, USER_PASSWORD_3_CLEAR, null, startCal);
	}

}
