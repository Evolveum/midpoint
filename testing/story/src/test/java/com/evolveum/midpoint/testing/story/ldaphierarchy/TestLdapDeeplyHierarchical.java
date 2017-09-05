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

package com.evolveum.midpoint.testing.story.ldaphierarchy;


import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Deeply hierarchical LDAP structure. The accounts are distributed around the tree in OUs.
 * The organizational structure is
 * reflected to hierachical OUs (OUs inside OUs). Each OU contains groups. Users are members of
 * the groups to reflect their direct membership in orgstruct. Groups are members of parent OU
 * groups.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapDeeplyHierarchical extends AbstractLdapHierarchyTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap-deeply-hierarchical");
	private static final String LDAP_OU_INTENT = "ou";

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

		String expectedDn = getAccountDn(user);
		assertEquals("Wrong account DN", expectedDn, accountEntry.getDN().toString().toLowerCase());

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
			display("parent group entry", openDJController.toHumanReadableLdifoid(parentGroupEntry));
			openDJController.assertUniqueMember(parentGroupEntry, groupEntry.getDN().toString());
		}

		String ouOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, LDAP_OU_INTENT);
		PrismObject<ShadowType> ouShadow = getShadowModel(ouOid);
		display("Org "+orgName+" ou shadow", ouShadow);

		Entry groupEntry = openDJController.searchSingle("ou="+orgName);
		assertNotNull("No UO LDAP entry for "+orgName, groupEntry);
		display("OU entry", openDJController.toHumanReadableLdifoid(groupEntry));
		openDJController.assertObjectClass(groupEntry, "organizationalUnit");

		String expectedDn = getOuDn(org);
		assertEquals("Wrong OU DN", expectedDn, groupEntry.getDN().toString().toLowerCase());

		return org;
	}

	private String getOuDn(PrismObject<OrgType> org) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			sb.append("ou=");
			sb.append(org.getName().getOrig().toLowerCase());
			sb.append(",");
			List<ObjectReferenceType> parentOrgRefs = org.asObjectable().getParentOrgRef();
			if (parentOrgRefs.isEmpty()) {
				break;
			}
			String parentOid = parentOrgRefs.get(0).getOid();
			if (ORG_TOP_OID.equals(parentOid)) {
				break;
			}
			org = getObject(OrgType.class, parentOid);
		}
		sb.append("dc=example,dc=com");
		return sb.toString();
	}

	private String getAccountDn(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		StringBuilder sb = new StringBuilder();
		sb.append("uid=").append(user.getName().getOrig()).append(",");
		PrismObject<FocusType> org = (PrismObject)user;
		while (true) {
			List<ObjectReferenceType> parentOrgRefs = org.asObjectable().getParentOrgRef();
			if (parentOrgRefs.isEmpty()) {
				break;
			}
			String parentOid = parentOrgRefs.get(0).getOid();
			if (ORG_TOP_OID.equals(parentOid)) {
				break;
			}
			org = (PrismObject)getObject(OrgType.class, parentOid);
			sb.append("ou=");
			sb.append(org.getName().getOrig().toLowerCase());
			sb.append(",");
		}
		sb.append("dc=example,dc=com");
		return sb.toString();
	}

	@Override
	protected void recomputeIfNeeded(String changedOrgOid) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		reconcileAllOrgs();
		reconcileAllUsers();
	}
}
