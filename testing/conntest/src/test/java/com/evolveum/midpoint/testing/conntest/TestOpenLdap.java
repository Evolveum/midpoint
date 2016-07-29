/**
 * Copyright (c) 2014-2015 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.text.ParseException;

import org.apache.directory.api.util.GeneralizedTime;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
	
}
