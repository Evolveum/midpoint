/*
 * Copyright (c) 2016-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.story;


import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Semi-flat LDAP structure. All accounts in ou=people. The organizational structure is
 * reflected to nested LDAP groups. Users are members of the groups to reflect
 * their direct membership in orgstruct. Group are member of other groups to reflect
 * the org tree. Not there is no structure of OUs.
 *  
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapNested extends AbstractLdapHierarchyTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap-nested");
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@Override
	protected File getTestDir() {
		return TEST_DIR;
	}
	
	@Override
	protected PrismObject<UserType> getAndAssertUser(String username, String directOrgGroupname, String... indirectGroupNames) throws SchemaException, CommonException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		PrismObject<UserType> user = super.getAndAssertUser(username, directOrgGroupname, indirectGroupNames);
		Entry accountEntry = openDJController.searchSingle("uid="+username);

		Entry groupEntry = openDJController.searchSingle("cn="+directOrgGroupname);
		assertNotNull("No group LDAP entry for "+directOrgGroupname, groupEntry);
		openDJController.assertUniqueMember(groupEntry, accountEntry.getDN().toString());
		
		return user;
	}
	
	@Override
	protected PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName, String directParentOrgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		PrismObject<OrgType> org = super.getAndAssertFunctionalOrg(orgName, directParentOrgOid);
		if (directParentOrgOid != null && !ORG_TOP_OID.equals(directParentOrgOid)) {
			Entry groupEntry = openDJController.searchSingle("cn="+orgName);
			PrismObject<OrgType> parentOrg = getObject(OrgType.class, directParentOrgOid);
			Entry parentGroupEntry = openDJController.searchSingle("cn="+parentOrg.getName());
			assertNotNull("No group LDAP entry for "+parentOrg.getName(), parentGroupEntry);
			openDJController.assertUniqueMember(parentGroupEntry, groupEntry.getDN().toString());
		}
		return org;
	}
}
