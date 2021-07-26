/*
 * Copyright (C) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.ldap.hierarchy;

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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Semi-flat LDAP structure. All accounts in ou=people. The organizational structure is
 * reflected to nested LDAP groups. Users are members of the groups to reflect
 * their direct membership in orgstruct. Group are member of other groups to reflect
 * the org tree. Not there is no structure of OUs.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapNested extends AbstractLdapHierarchyTest {

    public static final File TEST_DIR = new File(LDAP_HIERARCHY_TEST_DIR, "nested");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected File getTestDir() {
        return TEST_DIR;
    }

    @Override
    protected PrismObject<UserType> getAndAssertUser(
            String username, String directOrgGroupName, String... indirectGroupNames)
            throws CommonException, DirectoryException {
        PrismObject<UserType> user = super.getAndAssertUser(username, directOrgGroupName, indirectGroupNames);
        Entry accountEntry = openDJController.searchSingle("uid=" + username);

        Entry groupEntry = openDJController.searchSingle("cn=" + directOrgGroupName);
        assertNotNull("No group LDAP entry for " + directOrgGroupName, groupEntry);
        openDJController.assertUniqueMember(groupEntry, accountEntry.getDN().toString());

        return user;
    }

    @Override
    protected PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName, String directParentOrgOid)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
        PrismObject<OrgType> org = super.getAndAssertFunctionalOrg(orgName, directParentOrgOid);
        if (directParentOrgOid != null && !ORG_TOP_OID.equals(directParentOrgOid)) {
            Entry groupEntry = openDJController.searchSingle("cn=" + orgName);
            PrismObject<OrgType> parentOrg = getObject(OrgType.class, directParentOrgOid);
            Entry parentGroupEntry = openDJController.searchSingle("cn=" + parentOrg.getName());
            assertNotNull("No group LDAP entry for " + parentOrg.getName(), parentGroupEntry);
            openDJController.assertUniqueMember(parentGroupEntry, groupEntry.getDN().toString());
        }
        return org;
    }
}
