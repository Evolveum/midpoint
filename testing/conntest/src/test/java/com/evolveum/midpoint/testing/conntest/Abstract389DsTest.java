/**
 * Copyright (c) 2014-2016 Evolveum
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

import java.io.File;

import org.testng.annotations.AfterClass;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class Abstract389DsTest extends AbstractLdapConnTest {

	@Override
	protected File getBaseDir() {
		return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "389ds");
	}

	@Override
	protected String getResourceOid() {
		return "eaee8a88-ce54-11e4-a311-001e8c717e5b";
	}

	@Override
	protected int getLdapServerPort() {
		return 2389;
	}

	@Override
	protected String getLdapBindDn() {
		return "cn=directory manager";
	}

	@Override
	protected String getLdapBindPassword() {
		return "qwe12345";
	}

	@Override
	protected String getPeopleLdapSuffix() {
		// The capitalization that 389ds is using
		return "ou=People,"+getLdapSuffix();
	}

	@Override
	protected String getAccount0Cn() {
		return "Warlaz Kunjegjul (00000000)";
	}

	@Override
	protected int getSearchSizeLimit() {
		return 2000;
	}

	@Override
	protected String getLdapGroupObjectClass() {
		return "groupOfUniqueNames";
	}

	@Override
	protected String getLdapGroupMemberAttribute() {
		return "uniqueMember";
	}

	@Override
	protected String getSyncTaskOid() {
		return "cd1e0ff2-0099-11e5-9e22-001e8c717e5b";
	}

	@Override
	protected boolean isIdmAdminInteOrgPerson() {
		return false;
	}

	@Override
	public String getPrimaryIdentifierAttributeName() {
		return "nsUniqueId";
	}

	@Override
	protected boolean syncCanDetectDelete() {
		return false;
	}

	@Override
	protected boolean isVlvSearchBeyondEndResurnsLastEntry() {
		return true;
	}

	@Override
	protected boolean hasAssociationShortcut() {
		return false;
	}

	@Override
	protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd)
			throws ObjectNotFoundException, SchemaException {
		// TODO Auto-generated method stub

	}

}
