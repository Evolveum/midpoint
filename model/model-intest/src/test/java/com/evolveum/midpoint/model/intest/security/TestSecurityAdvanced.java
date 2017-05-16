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
package com.evolveum.midpoint.model.intest.security;

import static org.testng.AssertJUnit.assertEquals;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityAdvanced extends AbstractSecurityTest {
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		assignRole(userRumRogersOid, ROLE_ORDINARY_OID, initTask, initResult);
		assignRole(userCobbOid, ROLE_ORDINARY_OID, initTask, initResult);

	}


	@Test
    public void test100AutzJackPersonaManagement() throws Exception {
		final String TEST_NAME = "test100AutzJackPersonaManagement";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        assertGetDeny(UserType.class, USER_CHARLES_OID);
        
        assertSearch(UserType.class, null, 1);
        assertSearch(ObjectType.class, null, 1);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}

    @Test
    public void test102AutzLechuckPersonaManagement() throws Exception {
		final String TEST_NAME = "test102AutzLechuckPersonaManagement";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_LECHUCK_OID);
        assignRole(USER_LECHUCK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_LECHUCK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        assertGetAllow(UserType.class, USER_CHARLES_OID);

//        TODO: MID-3899
//        assertSearch(UserType.class, null, 2);
//        assertSearch(ObjectType.class, null, 2);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}
    
    @Test
    public void test110AutzJackPersonaAdmin() throws Exception {
		final String TEST_NAME = "test110AutzJackAddPersonaAdmin";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertAllow("assign application role 1 to jack", 
        		(task,result) -> assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result));
        
        PrismObject<UserType> userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        assertGetDeny(UserType.class, USER_CHARLES_OID);
        
        assertPersonaLinks(userJack, 1);
        String personaJackOid = userJack.asObjectable().getPersonaRef().get(0).getOid();
        
        PrismObject<UserType> personaJack = assertGetAllow(UserType.class, personaJackOid);
        assertEquals("Wrong jack persona givenName before change", USER_JACK_GIVEN_NAME, personaJack.asObjectable().getGivenName().getOrig());
        
//      TODO: MID-3899
//      assertSearch(UserType.class, null, 2);
//      assertSearch(ObjectType.class, null, 2);
        assertSearch(OrgType.class, null, 0);
        
        assertAllow("modify jack givenName", 
        		(task,result) -> modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, 
        				createPolyString(USER_JACK_GIVEN_NAME_NEW)));
        
        userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertEquals("Wrong jack givenName after change", USER_JACK_GIVEN_NAME_NEW, userJack.asObjectable().getGivenName().getOrig());

        personaJack = assertGetAllow(UserType.class, personaJackOid);
        assertEquals("Wrong jack persona givenName after change", USER_JACK_GIVEN_NAME_NEW, personaJack.asObjectable().getGivenName().getOrig());

        assertAllow("unassign application role 1 to jack", 
        		(task,result) -> unassignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result));
        
        userJack = assertGetAllow(UserType.class, USER_JACK_OID);
        assertPersonaLinks(userJack, 0);
        
        assertNoObject(UserType.class, personaJackOid);
        
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}
    
    @Test(enabled=false)
    public void test150AutzJackApproverUnassignRoles() throws Exception {
		final String TEST_NAME = "test150AutzJackApproverUnassignRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_APPROVER_UNASSIGN_ROLES_OID);
        assignRole(USER_JACK_OID, ROLE_ORDINARY_OID, SchemaConstants.ORG_APPROVER);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(RoleType.class, ROLE_ORDINARY_OID);
        assertGetDeny(RoleType.class, ROLE_PERSONA_ADMIN_OID); // no assignment
        assertGetDeny(RoleType.class, ROLE_APPROVER_UNASSIGN_ROLES_OID); // assignment exists, but wrong relation
        
        assertGetAllow(UserType.class, userRumRogersOid); // member of ROLE_ORDINARY_OID
        assertGetAllow(UserType.class, userCobbOid);      // member of ROLE_ORDINARY_OID
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(RoleType.class, null, 1);
        assertSearch(OrgType.class, null, 0);

        // TODO: assign role
        
        // TODO: list role members

        // TODO: unassign role
        
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        assertGlobalStateUntouched();
	}
}
