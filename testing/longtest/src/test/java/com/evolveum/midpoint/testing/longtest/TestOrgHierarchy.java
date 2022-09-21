/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "classpath:ctx-longtest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestOrgHierarchy extends AbstractModelIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    private static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    private static final File USER_ADMINISTRATOR_FILENAME = new File(COMMON_DIR, "user-administrator.xml");
    private static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();
    private static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    private static final File ROLE_SUPERUSER_FILENAME = new File(COMMON_DIR, "role-superuser.xml");
    private static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    //222 org. units, 2160 users
//    private static final int[] TREE_LEVELS = {2, 5, 7, 2};
//    private static final int[] TREE_LEVELS_USER = {5, 5, 20, 5};

    //1378 org. units, 13286 users
//    private static final int[] TREE_LEVELS = {2, 8, 5, 16};
//    private static final int[] TREE_LEVELS_USER = {3, 5, 5, 10};

    //98 org. units, 886 users
//    private static final int[] TREE_LEVELS = {2, 8, 5};
//    private static final int[] TREE_LEVELS_USER = {3, 5, 10};

    //18 org. units, 86 users
    private static final int[] TREE_LEVELS = { 2, 8 };
    private static final int[] TREE_LEVELS_USER = { 3, 5 };

    private int count = 0;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        provisioningService.postInit(initResult);
        modelService.postInit(initResult);

        // System Configuration and administrator
        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, initResult);
        login(userAdministrator);

        // Resources

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
    }

    @Test
    public void test100ImportOrgStructure() throws Exception {
        OperationResult opResult = getTestOperationResult();

        loadOrgStructure(null, TREE_LEVELS, TREE_LEVELS_USER, "", opResult);
        opResult.computeStatusIfUnknown();

        TestUtil.assertSuccess(opResult);
    }

    private void loadOrgStructure(String parentOid,
            int[] TREE_SIZE, int[] USER_SIZE, String oidPrefix, OperationResult result)
            throws Exception {
        if (TREE_SIZE.length == 0) {
            return;
        }

        for (int i = 0; i < TREE_SIZE[0]; i++) {
            String newOidPrefix = (TREE_SIZE[0] - i) + "a" + oidPrefix;
            PrismObject<OrgType> org = createOrg(parentOid, i, newOidPrefix);
            logger.info("Creating {}, total {}", org, count);
            String oid = repositoryService.addObject(org, null, result);
            count++;

            for (int u = 0; u < USER_SIZE[0]; u++) {
                PrismObject<UserType> user = createUser(oid, i, u, newOidPrefix);
                logger.info("Creating {}, total {}", user, count);
                repositoryService.addObject(user, null, result);
                count++;
            }

            loadOrgStructure(oid, ArrayUtils.remove(TREE_SIZE, 0), ArrayUtils.remove(USER_SIZE, 0),
                    newOidPrefix + i, result);
        }
    }

    private PrismObject<UserType> createUser(String parentOid, int i, int u, String oidPrefix)
            throws Exception {
        UserType user = new UserType();
        user.setOid("1" + createOid(u, oidPrefix + i));
        user.setName(createPolyStringType("u" + oidPrefix + i + u));
        user.setFullName(createPolyStringType("fu" + oidPrefix + i + u));
        user.setFamilyName(createPolyStringType("fa" + oidPrefix + i + u));
        user.setGivenName(createPolyStringType("gi" + oidPrefix + i + u));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            user.getParentOrgRef().add(ref);
        }

        prismContext.adopt(user);
        return user.asPrismContainer();
    }

    private PrismObject<OrgType> createOrg(String parentOid, int i, String oidPrefix)
            throws Exception {
        OrgType org = new OrgType();
        org.setOid("2" + createOid(i, oidPrefix));
        org.setDisplayName(createPolyStringType("o" + oidPrefix + i));
        org.setName(createPolyStringType("o" + oidPrefix + i));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            org.getParentOrgRef().add(ref);
        }

        prismContext.adopt(org);
        return org.asPrismContainer();
    }

    private String createOid(int i, String oidPrefix) {
        String oid = StringUtils.rightPad(oidPrefix + i, 31, 'a');
        return oid.substring(0, 7)
                + '-'
                + oid.substring(7, 11)
                + '-'
                + oid.substring(11, 15)
                + '-'
                + oid.substring(15, 19)
                + '-'
                + oid.substring(19, 31);
    }
}
