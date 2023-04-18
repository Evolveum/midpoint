/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "classpath:ctx-longtest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestGenericSynchronization extends AbstractLongTest {

    private static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

    private static final File RESOURCE_OPENDJ_FILE = new File(COMMON_DIR, "resource-opendj-generic-sync.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000030";
    private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    private static final File OBJECT_TEMPLATE_ORG_FILE = new File(COMMON_DIR, "object-template-org.xml");
    private static final String OBJECT_TEMPLATE_ORG_OID = "10000000-0000-0000-0000-000000000231";

    //222 org. units, 2160 users
//    private static final int[] TREE_LEVELS = {2, 5, 7, 2};
//    private static final int[] TREE_LEVELS_USER = {5, 5, 20, 5};

    //1378 org. units, 13286 users
//    private static final int[] TREE_LEVELS = {2, 8, 5, 16};
//    private static final int[] TREE_LEVELS_USER = {3, 5, 5, 10};

    //86 org. units, 636 users
    private static final int[] TREE_LEVELS = { 2, 7, 5 };
    private static final int[] TREE_LEVELS_USER = { 3, 5, 8 };

    //18 org. units, 86 users
//    private static final int[] TREE_LEVELS = {2, 8};
//    private static final int[] TREE_LEVELS_USER = {3, 5};

    // We already have some users in LDAP instance
    private int ldapdUserCount = 4;
    private int ldapOrgCount = 0;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        loadOpenDJWithData();

        importObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
        setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);

        // Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(
                ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
    }

    private void loadOpenDJWithData() throws IOException, LDIFException {
        long ldapPopStart = System.currentTimeMillis();
        int count = loadOpenDJ(TREE_LEVELS, TREE_LEVELS_USER, openDJController.getSuffixPeople(), 0);
        long ldapPopEnd = System.currentTimeMillis();

        IntegrationTestTools.display("Loaded " + count + " LDAP entries in " + ((ldapPopEnd - ldapPopStart) / 1000) + " seconds");
    }

    @SuppressWarnings("SameParameterValue")
    private int loadOpenDJ(int[] TREE_SIZE, int[] USER_COUNT, String dnSuffix, int count)
            throws IOException, LDIFException {

        if (TREE_SIZE.length == 0) {
            return count;
        }

        for (int i = 0; i < TREE_SIZE[0]; i++) {
            String ou = "L" + TREE_SIZE.length + "o" + i;
            String newSuffix = "ou=" + ou + ',' + dnSuffix;

            Entry org = createOrgEntry(ou, dnSuffix);
            logCreateEntry(org);
            ldapOrgCount++;
            openDJController.addEntry(org);
            count++;

            for (int u = 0; u < USER_COUNT[0]; u++) {
                // We have to make uid globally unique. Otherwise correlation takes place and it will
                // "collapse" several accounts into one user
                String uid = "L" + TREE_SIZE.length + "o" + i + "u" + u + "c" + ldapdUserCount;
                String sn = "Doe" + uid;

                Entry user = createUserEntry(uid, newSuffix, sn);
                logCreateEntry(user);
                openDJController.addEntry(user);
                ldapdUserCount++;
                count++;
            }

            count += loadOpenDJ(ArrayUtils.remove(TREE_SIZE, 0), ArrayUtils.remove(USER_COUNT, 0), newSuffix, 0);
        }

        return count;
    }

    private void logCreateEntry(Entry entry) {
        boolean logCreateEntry = true;
        //noinspection ConstantValue
        if (logCreateEntry) {
            System.out.println("Creating LDAP entry: " + entry.getDN());
            logger.trace("Creating LDAP entry: {}", entry.getDN());
        }
    }

    private Entry createUserEntry(String uid, String suffix, String sn) throws IOException, LDIFException {
        String dn = "uid=" + uid + "," + suffix;
        String sb = "dn: " + dn + '\n'
                + "objectClass: inetOrgPerson\n"
                + "uid: " + uid + '\n'
                + "givenName: " + "John" + '\n'
                + "cn: " + "John " + sn + '\n'
                + "sn: " + sn + '\n';
        LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb, StandardCharsets.UTF_8));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        return ldifReader.readEntry();
    }

    private Entry createOrgEntry(String ou, String suffix) throws IOException, LDIFException {
        String dn = "ou=" + ou + "," + suffix;
        String sb = "dn: " + dn + "\n"
                + "objectClass: organizationalUnit\n"
                + "ou: " + ou + "\n"
                + "description: " + "This is sparta! ...or " + ou + "\n";
        LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb, StandardCharsets.UTF_8));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        return ldifReader.readEntry();
    }

    @Test
    public void test100TreeImport() throws Exception {
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        // WHEN
        modelService.importFromResource(RESOURCE_OPENDJ_OID,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 20000 + (ldapdUserCount + ldapOrgCount) * 2000);

        // THEN
        then();

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        AssertJUnit.assertEquals("Unexpected number of users", ldapdUserCount, userCount);
    }

    @Test
    public void test200MoveRootChild() {
        //todo move one child of one root to root position
    }
}
