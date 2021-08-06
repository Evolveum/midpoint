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
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Flat LDAP structure. All accounts in ou=people. The organizational structure is
 * reflected to (non-nested) LDAP groups. Users are members of the groups to reflect
 * the orgstruct.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapFlat extends AbstractLdapHierarchyTest {

    public static final File TEST_DIR = new File(LDAP_HIERARCHY_TEST_DIR, "flat");

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

        if (indirectGroupNames != null) {
            for (String expectedGroupName : indirectGroupNames) {
                groupEntry = openDJController.searchSingle("cn=" + expectedGroupName);
                assertNotNull("No group LDAP entry for " + expectedGroupName, groupEntry);
                openDJController.assertUniqueMember(groupEntry, accountEntry.getDN().toString());
            }
        }

        return user;
    }

    @Override
    protected void recomputeIfNeeded(String changedOrgOid)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        reconcileAllUsers();
    }
}
