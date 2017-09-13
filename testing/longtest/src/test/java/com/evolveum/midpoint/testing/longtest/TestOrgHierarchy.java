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
package com.evolveum.midpoint.testing.longtest;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.File;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestOrgHierarchy extends AbstractModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestOrgHierarchy.class);

    private static final File SYSTEM_CONFIGURATION_FILE = new File( COMMON_DIR, "system-configuration.xml");
    private static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    private static final File USER_ADMINISTRATOR_FILENAME = new File( COMMON_DIR, "user-administrator.xml");
    private static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();
    private static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    private static final File ROLE_SUPERUSER_FILENAME = new File( COMMON_DIR, "role-superuser.xml");
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
    private static final int[] TREE_LEVELS = {2, 8};
    private static final int[] TREE_LEVELS_USER = {3, 5};

    private int count = 0;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

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
        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

        loadOrgStructure(null, TREE_LEVELS, TREE_LEVELS_USER, "", opResult);
        opResult.computeStatusIfUnknown();

        TestUtil.assertSuccess(opResult);
    }

    private void loadOrgStructure(String parentOid, int[] TREE_SIZE, int[] USER_SIZE, String oidPrefix,
                                  OperationResult result) throws Exception {
        if (TREE_SIZE.length == 0) {
            return;
        }

        for (int i = 0; i < TREE_SIZE[0]; i++) {
            String newOidPrefix = (TREE_SIZE[0] - i) + "a" + oidPrefix;
            PrismObject<OrgType> org = createOrg(parentOid, i, newOidPrefix);
            LOGGER.info("Creating {}, total {}", org, count);
            String oid = repositoryService.addObject(org, null, result);
            count++;

            for (int u = 0; u < USER_SIZE[0]; u++) {
                PrismObject<UserType> user = createUser(oid, i, u, newOidPrefix);
                LOGGER.info("Creating {}, total {}", user, count);
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
        String oid = StringUtils.rightPad(oidPrefix + Integer.toString(i), 31, 'a');

        StringBuilder sb = new StringBuilder();
        sb.append(oid.substring(0, 7));
        sb.append('-');
        sb.append(oid.substring(7, 11));
        sb.append('-');
        sb.append(oid.substring(11, 15));
        sb.append('-');
        sb.append(oid.substring(15, 19));
        sb.append('-');
        sb.append(oid.substring(19, 31));

        return sb.toString();
    }
}