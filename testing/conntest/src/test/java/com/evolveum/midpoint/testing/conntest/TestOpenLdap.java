/**
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.text.ParseException;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.util.GeneralizedTime;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;

/**
 * @author semancik
 *
 */
public class TestOpenLdap extends AbstractLdapConnTest {

	@Override
	protected String getResourceOid() {
		return "2a7c7130-7a34-11e4-bdf6-001e8c717e5b";
	}

	@Override
	protected File getBaseDir() {
		return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "openldap");
	}

	@Override
	public String getStartSystemCommand() {
		return "sudo "+getScriptDirectoryName()+"/openldap-start";
	}

	@Override
	public String getStopSystemCommand() {
		return "sudo "+getScriptDirectoryName()+"/openldap-stop";
	}

	@Override
	protected String getLdapServerHost() {
		return "localhost";
	}

	@Override
	protected int getLdapServerPort() {
		return 11389;
	}

	@Override
	protected String getLdapBindDn() {
		return "cn=admin,dc=example,dc=com";
	}

	@Override
	protected String getLdapBindPassword() {
		return "secret";
	}

	@Override
	protected String getAccount0Cn() {
		return "Riwibmix Juvotut (00000000)";
	}

	@Override
	protected int getSearchSizeLimit() {
		return 500;
	}
	
	@Override
	protected String getPeopleLdapSuffix() {
		return "ou=people,"+getLdapSuffix();
	}

	@Override
	protected String getGroupsLdapSuffix() {
		return "ou=groups,"+getLdapSuffix();
	}

	
	@Override
	protected String getLdapGroupObjectClass() {
		return "groupOfNames";
	}

	@Override
	protected String getLdapGroupMemberAttribute() {
		return "member";
	}

	@Override
	protected String getSyncTaskOid() {
		return "cd1e0ff2-0099-11e5-9e22-001e8c717e5b";
	}
	
	@Override
	protected boolean syncCanDetectDelete() {
		return false;
	}
	
	@Override
	protected boolean needsGroupFakeMemeberEntry() {
		return true;
	}

	@Override
	protected void assertActivationCapability(ActivationCapabilityType activationCapabilityType) {
		assertNotNull("No activation capability", activationCapabilityType);
		
		ActivationLockoutStatusCapabilityType lockoutCapability = CapabilityUtil.getEffectiveActivationLockoutStatus(activationCapabilityType);
		assertNotNull("No lockout capability", lockoutCapability);
		display("Lockout capability", lockoutCapability);
	}
	
	@Override
	protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd)
			throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertSyncToken");
		Task task = taskManager.getTask(syncTaskOid, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismProperty<String> syncTokenProperty = task.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
		assertNotNull("No sync token in "+task, syncTokenProperty);
		String syncToken = syncTokenProperty.getRealValue();
		assertNotNull("No sync token in "+task, syncToken);
		IntegrationTestTools.display("Sync token", syncToken);
		
		GeneralizedTime syncTokenGt;
		try {
			syncTokenGt = new GeneralizedTime(syncToken);
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		TestUtil.assertBetween("Wrong time in sync token: "+syncToken, roundTsDown(tsStart), roundTsUp(tsEnd), syncTokenGt.getCalendar().getTimeInMillis());
		
	}
	
	@Test
    public void test700CheckBarbossaLockoutStatus() throws Exception {
		final String TEST_NAME = "test700CheckBarbossaLockoutStatus";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<ShadowType> shadow = getShadowModel(accountBarbossaOid);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        display("Shadow (model)", shadow);
        ActivationType activation = shadow.asObjectable().getActivation();
        if (activation != null) {
	        LockoutStatusType lockoutStatus = shadow.asObjectable().getActivation().getLockoutStatus();
	        if (lockoutStatus != null && lockoutStatus != LockoutStatusType.NORMAL) {
	        	AssertJUnit.fail("Barbossa is locked!");
	        }
        }

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD_2);
	}
	
	@Test
    public void test702LockOutBarbossa() throws Exception {
		final String TEST_NAME = "test702LockOutBarbossa";
        TestUtil.displayTestTitle(this, TEST_NAME);
        
        Entry entry = getLdapAccountByUid(USER_BARBOSSA_USERNAME);
        display("LDAP Entry before", entry);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        for (int i = 0; i < 10; i++) {
        	LdapNetworkConnection conn;
        	try {
        		conn = ldapConnect(null, entry.getDn().toString(), "this password is wrong");
        	} catch (SecurityException e) {
        		// Good bad attempt
        		continue;
        	}
        	assertNotReached();
        }
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("LDAP Entry after", entry);
        
        PrismObject<ShadowType> shadow = getShadowModel(accountBarbossaOid);
        display("Shadow (model)", shadow);
        ActivationType activation = shadow.asObjectable().getActivation();
        assertNotNull("No activation", activation);
        LockoutStatusType lockoutStatus = shadow.asObjectable().getActivation().getLockoutStatus();
        assertEquals("Wrong lockout status", LockoutStatusType.LOCKED, lockoutStatus);
	}
	

	@Test
    public void test705UnlockBarbossaAccount() throws Exception {
		final String TEST_NAME = "test705UnlockBarbossaAccount";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountBarbossaOid, null, 
        		SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, LockoutStatusType.NORMAL);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        executeChanges(accountDelta, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<ShadowType> shadow = getShadowModel(accountBarbossaOid);
        display("Shadow (model)", shadow);
        
        ActivationType activation = shadow.asObjectable().getActivation();
        if (activation != null) {
	        LockoutStatusType lockoutStatus = shadow.asObjectable().getActivation().getLockoutStatus();
	        if (lockoutStatus != null && lockoutStatus != LockoutStatusType.NORMAL) {
	        	AssertJUnit.fail("Barbossa is locked!");
	        }
        }
        
        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("LDAP Entry", entry);
        assertNoAttribute(entry, "pwdAccountLockedTime");
        
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD_2);
	}

}
