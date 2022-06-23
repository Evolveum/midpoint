/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests handling of object types delineation, both forward (translating boundaries into resource queries),
 * and backwards (classification of resource objects).
 *
 * It is separate from main {@link TestOpenDj} to keep the main test class of manageable size and complexity.
 *
 * See `resource-opendj-types.xml` for the description of individual types.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjObjectTypes extends AbstractOpenDjTest {

    protected static final File TEST_DIR = new File("src/test/resources/opendj/types");

    private static final File RESOURCE_OPENDJ_TYPES_FILE = new File(TEST_DIR, "resource-opendj-types.xml");

    private static final File ALICE_FILE = new File(TEST_DIR, "alice-employee.ldif");
    private static final File JIM_ADMIN = new File(TEST_DIR, "jim-admin.ldif");
    private static final File ANN_TESTER_FILE = new File(TEST_DIR, "ann-tester.ldif");

    private static final String INTENT_EMPLOYEE = "employee";
    private static final String INTENT_ADMIN = "admin";
    private static final String INTENT_TESTER = "tester";

    @BeforeClass
    public void startLdap() throws Exception {
        doStartLdap();
        openDJController.addEntry(
                "dn: ou=employees,dc=example,dc=com\n" +
                        "objectclass: organizationalUnit\n" +
                        "ou: employees\n");
        openDJController.addEntry(
                "dn: ou=special,dc=example,dc=com\n" +
                        "objectclass: organizationalUnit\n" +
                        "ou: special\n");

        openDJController.addEntryFromLdifFile(ALICE_FILE);
        openDJController.addEntryFromLdifFile(JIM_ADMIN);
        openDJController.addEntryFromLdifFile(ANN_TESTER_FILE);
    }

    @AfterClass
    public void stopLdap() {
        doStopLdap();
    }

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_TYPES_FILE;
    }

    @Test
    public void test010TestConnection() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task, result);

        then();
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);
    }

    @Test
    public void test100GetStandardPeople() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        List<PrismObject<ShadowType>> employees = provisioningService.searchObjects(
                ShadowType.class,
                ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ACCOUNT, INTENT_EMPLOYEE),
                null,
                task,
                result);

        then();
        display("employees", employees);
        assertThat(employees).as("employees").hasSize(1);
        assertShadowAfter(employees.get(0))
                .assertKind(ACCOUNT)
                .assertIntent(INTENT_EMPLOYEE)
                .assertName("uid=alice,ou=employees,dc=example,dc=com");
    }

    @Test
    public void test110GetAdmins() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        List<PrismObject<ShadowType>> admins = provisioningService.searchObjects(
                ShadowType.class,
                ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ACCOUNT, INTENT_ADMIN),
                null,
                task,
                result);

        then();
        display("admins", admins);
        assertThat(admins).as("admins").hasSize(1);
        assertShadowAfter(admins.get(0))
                .assertKind(ACCOUNT)
                .assertIntent(INTENT_ADMIN)
                .assertName("uid=jim,ou=special,dc=example,dc=com");
    }

    @Test
    public void test120GetTesters() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        List<PrismObject<ShadowType>> testers = provisioningService.searchObjects(
                ShadowType.class,
                ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ACCOUNT, INTENT_TESTER),
                null,
                task,
                result);

        then();
        display("testers", testers);
        assertThat(testers).as("testers").hasSize(1);
        assertShadowAfter(testers.get(0))
                .assertKind(ACCOUNT)
                .assertIntent(INTENT_TESTER)
                .assertName("uid=ann,ou=special,dc=example,dc=com");
    }
}
