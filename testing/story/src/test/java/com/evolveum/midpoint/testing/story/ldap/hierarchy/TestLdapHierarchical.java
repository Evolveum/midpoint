/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap.hierarchy;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Hierarchical LDAP structure. All accounts are in ou=people. The organizational structure is
 * reflected to hierarchical OUs (OUs inside OUs). Each OU contains groups. Users are members of
 * the groups to reflect their direct membership in orgstruct. Groups are members of parent OU
 * groups.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapHierarchical extends AbstractLdapHierarchyTest {

    public static final File TEST_DIR = new File(LDAP_HIERARCHY_TEST_DIR, "hierarchical");
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
            displayValue("parent group entry", openDJController.toHumanReadableLdifoid(parentGroupEntry));
            openDJController.assertUniqueMember(parentGroupEntry, groupEntry.getDN().toString());
        }

        String ouOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, LDAP_OU_INTENT);
        PrismObject<ShadowType> ouShadow = getShadowModel(ouOid);
        display("Org "+orgName+" ou shadow", ouShadow);

        Entry groupEntry = openDJController.searchSingle("ou="+orgName);
        assertNotNull("No UO LDAP entry for "+orgName, groupEntry);
        displayValue("OU entry", openDJController.toHumanReadableLdifoid(groupEntry));
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

    @Override
    protected void recomputeIfNeeded(String changedOrgOid) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        reconcileAllOrgs();
        reconcileAllUsers();
    }
}
