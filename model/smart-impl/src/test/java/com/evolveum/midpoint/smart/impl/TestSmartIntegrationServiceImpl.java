/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.smart.impl.DescriptiveItemPath.asStringSimple;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationPrecisionType.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.DummyScenario.Account;
import com.evolveum.midpoint.smart.impl.activities.StatisticsComputer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Unit tests for the Smart Integration Service implementation.
 *
 * It is unclear if this class will ever be used.
 */
@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationServiceImpl extends AbstractSmartIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart");

    private static final File TEST_100_STATISTICS = new File(TEST_DIR, "test-100-statistics.xml");
    private static final File TEST_1XX_STATISTICS = new File(TEST_DIR, "test-1xx-statistics.xml");
    private static final File TEST_110_EXPECTED_OBJECT_TYPES = new File(TEST_DIR, "test-110-expected-object-types.xml");
    private static final File TEST_140_EXPECTED_OBJECT_TYPES = new File(TEST_DIR, "test-140-expected-object-types.xml");
    private static final File TEST_110_EXPECTED_REQUEST = new File(TEST_DIR, "test-110-expected-request.json");

    private static final TestObject<?> USER_JACK = TestObject.file(TEST_DIR, "user-jack.xml", "84d2ff68-9b32-4ef4-b87b-02536fd5e83c");
    private static final TestObject<?> USER_JIM = TestObject.file(TEST_DIR, "user-jim.xml", "8f433649-6cc4-401b-910f-10fa5449f14c");
    private static final TestObject<?> USER_ALICE = TestObject.file(TEST_DIR, "user-alice.xml", "79df4c1f-6480-4eb8-9db7-863e25d5b5fa");
    private static final TestObject<?> USER_BOB = TestObject.file(TEST_DIR, "user-bob.xml", "30cef119-71b6-42b3-9762-5c649b2a2b6a");

    private static final ResourceObjectTypeIdentification GENERIC_ORGANIZATIONAL_UNIT =
            ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, "organizationalUnit");

    private static DummyScenario dummyForObjectTypes;
    private static DummyScenario dummyForMappingsAndCorrelation;

    private static final DummyTestResource RESOURCE_DUMMY_FOR_COUNTING_NO_PAGING = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-counting-no-paging.xml", "66b5be3a-5ea8-4d4d-ba11-89b190815da7",
            "for-counting-no-paging",
            c -> DummyScenario.on(c).initialize());
    private static final DummyTestResource RESOURCE_DUMMY_FOR_COUNTING_WITH_PAGING = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-counting-with-paging.xml", "8032d4d4-bb93-4837-a35e-274407f00f36",
            "for-counting-with-paging",
            c -> DummyScenario.on(c).initialize());
    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-object-types.xml", "4e673bd5-661e-4037-9e19-557ea485238b",
            "for-suggest-object-types",
            c -> dummyForObjectTypes = DummyScenario.on(c).initialize());
    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-mappings-and-correlation.xml", "a51dac70-6fbb-4c9a-9827-465c844afdc6",
            "for-suggest-mappings-and-correlation",
            c -> dummyForMappingsAndCorrelation = DummyScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult, CommonInitialObjects.SERVICE_ORIGIN_ARTIFICIAL_INTELLIGENCE);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_COUNTING_NO_PAGING, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_COUNTING_WITH_PAGING, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION, initTask, initResult);

        createDummyAccounts();

        initTestObjects(initTask, initResult,
                USER_JACK, USER_JIM, USER_ALICE, USER_BOB);
        createAndLinkAccounts(initTask, initResult);
    }

    private void createDummyAccounts() throws Exception {
        var c = dummyForObjectTypes.getController();
        c.addAccount("jack")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "admin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601040027");

        c.addAccount("jim")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "admin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jim@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive");

        c.addAccount("alice")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Alice Wonderland")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "admin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "alice@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421900111222")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sales");

        c.addAccount("bob")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Bob Builder")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "admin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "bob@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421900333444")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Engineering");

        c.addAccount("eve")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Eve Adams")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "admin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "eve@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValue(DummyScenario.Account.AttributeNames.CREATED.local(), ZonedDateTime.parse("2023-09-01T12:00:00Z"));
    }

    private void createAndLinkAccounts(Task initTask, OperationResult initResult) throws Exception {
        var a = dummyForMappingsAndCorrelation.account;
        a.add("jack")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "a")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "e")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420-601-040-027");
        linkAccount(USER_JACK, initTask, initResult);
        a.add("jim")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "i")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+99-123-456-789");
        linkAccount(USER_JIM, initTask, initResult);
        a.add("alice")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Alice Wonderland")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421-900-111-222")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "a")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "c");
        linkAccount(USER_ALICE, initTask, initResult);
        a.add("bob")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Bob Builder")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421-900-333-444")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "i")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "c");
        linkAccount(USER_BOB, initTask, initResult);
    }

    private void linkAccount(TestObject<?> user, Task task, OperationResult result) throws CommonException, IOException {
        var shadow = findShadowRequest()
                .withResource(RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.getObjectable())
                .withDefaultAccountType()
                .withNameValue(user.getNameOrig())
                .build().findRequired(task, result);
        executeChanges(
                PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .add(shadow.getRef())
                        .asObjectDelta(user.oid),
                null, task, result);
    }

    private String pickWeightedRandom(String[] values, int[] weights, Random rand) {
        int totalWeight = 0;
        for (int w : weights) totalWeight += w;
        int r = rand.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < values.length; i++) {
            cumulative += weights[i];
            if (r < cumulative) {
                return values[i];
            }
        }
        return values[values.length - 1];
    }

    private void addDummyAccountsExceedingLimit() throws Exception {
        var c = dummyForObjectTypes.getController();
        String[] departments = {"HR", "Engineering", "Sales", "Marketing", "IT", "Finance", "Support"};
        int[] departmentWeights = {1, 5, 3, 2, 4, 2, 1};

        String[] types = {"employee", "manager", "contractor", "intern"};
        int[] typeWeights = {7, 1, 3, 1};

        String[] statuses = {"active", "inactive"};
        int[] statusWeights = {8, 2};

        Random rand = new Random();

        for (int i = 1; i <= 100; i++) {
            String username = "user" + i;
            String personalNumber = String.format("%08d", 10000000 + i);
            String department = pickWeightedRandom(departments, departmentWeights, rand);
            String type = pickWeightedRandom(types, typeWeights, rand);
            String status = pickWeightedRandom(statuses, statusWeights, rand);
            String phone = String.format("+1555%07d", i);
            String fullname = "User Fullname " + i;
            String description = "Test user number " + i;
            String email = username + "@example.com";

            if (c.getDummyResource().getAccountByName(username)==null) {
                c.addAccount(username)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), personalNumber)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), department)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), type)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), status)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), phone)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), description)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), fullname)
                        .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), email);
            }
        }
    }

    private void addDummyAccountsWithAffixes() throws Exception {
        var c = dummyForObjectTypes.getController();
        c.addAccount("carol")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Carol Danvers")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "adm_12šč3")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "carol.prod@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "IT");

        c.addAccount("dave")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Dave Grohl")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "456admin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "dave.priv@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Finance");

        c.addAccount("susan")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Susan Calvin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "789_prod")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "susan.adm@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Engineering");

        c.addAccount("tom")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Tom Sawyer")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "usr_321")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "tom.usr@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "intern")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Marketing");

        c.addAccount("greg")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Greg House")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "adm_654")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "greg.user@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Support");

        c.addAccount("lisa")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Lisa Simpson")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "ADM_987")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "lisa.eng@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Engineering");

        c.addAccount("mark")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Mark Watney")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "int-741")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "mark.ops@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "IT");

        c.addAccount("nina")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Nina Sharp")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "svc_852.int")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "nina.svc@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "intern")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR");

        c.addAccount("oliver")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Oliver Queen")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "int.963_int")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "oliver.int@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sales");

        c.addAccount("peter")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Peter Parker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "int_159")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "peter.ext@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Marketing");

        c.addAccount("quinn")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Quinn Fabray")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "INT.753")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "quinn.ro@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "intern")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Finance");

        c.addAccount("rachel")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Rachel Green")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "int-357")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "rachel.rw@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR");
    }

    private void addDummyAccountsWithDN() throws Exception {
        var c = dummyForObjectTypes.getController();
        c.addAccount("alex")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Alex Murphy")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "usr_001")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "alex.eng@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Engineering")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=alex\\,OU=Employees,DC=evolveum,DC=com");

        c.addAccount("brenda")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Brenda Starr")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "adm_002")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "brenda.it@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "IT")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN\\=brenda,OU\\=Employees,OU\\=IT,DC\\=evolveum,DC\\=com");

        c.addAccount("carl")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Carl Johnson")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "adm_003")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "carl.hr@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=carl,DC=evolveum,DC=com");

        c.addAccount("dina")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Dina Mendez")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "int_556")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "dina.sales@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "intern")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sales")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN\\=dina\\,OU\\=Employees\\,DC\\=evolveum\\,DC\\=com");

        c.addAccount("elena")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Elena Gilbert")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "int_110")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "elena.eng@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Engineering")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=elena,OU=Employees,DC=evolveum,DC=com");

        c.addAccount("felix")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Felix Leiter")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "emp_007")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "felix.it@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "IT")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=felix,OU=Employees,OU=IT,DC=evolveum,DC=com");

        c.addAccount("gina")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Gina Torres")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "svc_204")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "gina.hr@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=gina,OU=Employees,OU=HR,DC=evolveum,DC=com");

        c.addAccount("henry")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Henry Cavill")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "adm_888")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "henry.sales@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sales")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=henry,OU=Contractors,OU=IT,OU=Contractors,OU=IT,DC=evolveum,DC=com");

        c.addAccount("isabel")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Isabel Archer")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "usr_034")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "isabel.hr@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "intern")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=isabel,OU=Employees,OU=IT,DC=evolveum,DC=com");

        c.addAccount("jackie")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "emp_375")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jack.sales@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sales")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DN.local(), "CN=jack,OU=Employees,OU=HR,DC=evolveum,DC=com");
    }

    @Test
    public void test040DescriptiveItemPath() throws Exception {
        QName Q_GIVEN_NAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "givenName", "c");
        QName Q_EMAIL = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "email", "c");
        QName Q_VALUE = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "value", "c");

        // Test Empty
        DescriptiveItemPath path = DescriptiveItemPath.empty();
        assertThat(path).isNotNull();
        assertThat(path.asString()).isEqualTo("");
        assertThat(path.getItemPath().size()).isEqualTo(0);

        // Test Append
        path = path
                .append(Q_GIVEN_NAME, false)
                .append(Q_EMAIL, true)
                .append(Q_VALUE, false);
        assertThat(path).isNotNull();
        String str = path.asString();
        assertThat(str).contains("givenName");
        assertThat(str).contains("value");
        assertThat(str).contains("email[*]");

        // Test getItemPath
        ItemPath itemPath = path.getItemPath();
        assertThat(itemPath.size()).isEqualTo(3);
        assertThat(ItemPath.toName(itemPath.first())).isEqualTo(Q_GIVEN_NAME);
        assertThat(ItemPath.toName(itemPath.last())).isEqualTo(Q_VALUE);

        // Test asStringSimple
        ItemPath ip = ItemPath.create(Q_EMAIL, Q_VALUE);
        String s = DescriptiveItemPath.asStringSimple(ip);
        assertThat(s).contains("email");
        assertThat(s).contains("value");
        assertThat(s).doesNotContain("[*]");

        // Test without definition
        ItemPath ip2 = ItemPath.create(Q_EMAIL, Q_GIVEN_NAME);
        DescriptiveItemPath dip = DescriptiveItemPath.of(ip2, null);
        String s2 = dip.asString();
        assertThat(s2).contains("email");
        assertThat(s2).contains("givenName");
        assertThat(s2).doesNotContain("[*]");
    }

    @Test
    public void test050CountingAccountsNoPaging() throws Exception {
        executeCountingTest(
                RESOURCE_DUMMY_FOR_COUNTING_NO_PAGING,
                new ObjectClassSizeEstimationType().value(0).precision(EXACTLY),
                new ObjectClassSizeEstimationType().value(3).precision(EXACTLY),
                new ObjectClassSizeEstimationType().value(5).precision(AT_LEAST));
    }

    @Test
    public void test060CountingAccountsWithPaging() throws Exception {
        executeCountingTest(
                RESOURCE_DUMMY_FOR_COUNTING_WITH_PAGING,
                new ObjectClassSizeEstimationType().value(0).precision(EXACTLY),
                new ObjectClassSizeEstimationType().value(3).precision(EXACTLY),
                new ObjectClassSizeEstimationType().value(33).precision(APPROXIMATELY));
    }

    private void executeCountingTest(
            DummyTestResource resource,
            ObjectClassSizeEstimationType expected0,
            ObjectClassSizeEstimationType expected3,
            ObjectClassSizeEstimationType expected33) throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("counting accounts on the resource");
        var count0 = countAccounts(resource, task, result);

        then("the count is correct");
        displayDumpable("count", count0);
        assertThat(count0).isEqualTo(expected0);

        when("adding few accounts");
        createDummyAccounts(resource, 1, 3);

        when("counting accounts on the resource");
        var count3 = countAccounts(resource, task, result);

        then("the count is correct");
        displayDumpable("count", count3);
        assertThat(count3).isEqualTo(expected3);

        when("adding many accounts");
        createDummyAccounts(resource, 10, 30);

        when("counting accounts on the resource");
        var count33 = countAccounts(resource, task, result);

        then("the count is correct");
        displayDumpable("count", count33);
        assertThat(count33).isEqualTo(expected33);
    }

    private void createDummyAccounts(DummyTestResource resource, int from, int count) throws Exception {
        for (int i = from; i < from + count; i++) {
            resource.addAccount("account-%04d".formatted(i));
        }
    }

    private ObjectClassSizeEstimationType countAccounts(DummyTestResource resource, Task task, OperationResult result)
            throws CommonException {
        return smartIntegrationService.estimateObjectClassSize(
                resource.oid, OC_ACCOUNT_QNAME, 5, task, result);
    }

    /** Calls the remote service directly. */
    @Test
    public void test100SuggestObjectTypes() throws CommonException, IOException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            smartIntegrationService.setServiceClientSupplier(
                    () -> new MockServiceClientImpl(
                            new SiSuggestObjectTypesResponseType()
                                    .objectType(new SiSuggestedObjectTypeType()
                                            .kind("account")
                                            .intent("default"))));
        }

        var task = getTestTask();
        var result = task.getResult();

        var shadowObjectClassStatistics = parseStatistics(TEST_100_STATISTICS);

        when("suggesting object types");
        var objectTypes = smartIntegrationService.suggestObjectTypes(
                RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME, shadowObjectClassStatistics, task, result);

        then("there is at least one suggested object type");
        assertSuccess(result);
        assertThat(objectTypes).isNotNull();
        assertThat(objectTypes.getObjectType()).isNotEmpty();

        and("response is marked as generated by AI");
        objectTypes.getObjectType().forEach(
                o -> {
                    assertAiProvidedMarkPresentRequired(o,
                            ResourceObjectTypeDefinitionType.F_KIND,
                            ResourceObjectTypeDefinitionType.F_INTENT);
                    assertAiProvidedMarkPresent(o,
                            ResourceObjectTypeDefinitionType.F_DELINEATION.append(ResourceObjectTypeDelineationType.F_FILTER));
                    assertAiProvidedMarkAbsentRequired(o,
                            ResourceObjectTypeDefinitionType.F_DELINEATION.append(ResourceObjectTypeDelineationType.F_OBJECT_CLASS));
                });
    }

    /** All features: both filters and base context, plus multiple object types. */
    @Test(enabled = false) // MID-10872
    public void test110SuggestObjectTypesWithFiltersAndBaseContext() throws CommonException, IOException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(
                new SiSuggestObjectTypesResponseType()
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("employee")
                                .filter("attributes/type = 'employee'")
                                .baseContextObjectClassName("organizationalUnit")
                                .baseContextFilter("attributes/cn = 'evolveum'"))
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("other")
                                .filter("attributes/type != 'employee'")));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        var shadowObjectClassStatistics = parseStatistics(TEST_1XX_STATISTICS);

        when("suggesting object types");
        var objectTypes = smartIntegrationService.suggestObjectTypes(
                RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME, shadowObjectClassStatistics, task, result);

        then("there is at least one suggested object type");
        assertSuccess(result);
        assertThat(objectTypes)
                .as("suggested object types")
                .isEqualTo(parseObjectTypesSuggestion(TEST_110_EXPECTED_OBJECT_TYPES));

        var realRequest = normalizeSiSuggestObjectTypesRequest(mockClient.getLastRequest());
        var expectedRequest = normalizeSiSuggestObjectTypesRequest(
                parseFile(TEST_110_EXPECTED_REQUEST, SiSuggestObjectTypesRequestType.class));

        // Comparing ItemPathType is not straightforward, so let's compare the JSON serialization.
        var realRequestJson = PrismContext.get().jsonSerializer().serializeRealValueContent(realRequest);
        var expectedRequestJson = PrismContext.get().jsonSerializer().serializeRealValueContent(expectedRequest);

        assertThat(realRequestJson)
                .as("request (normalized)")
                .isEqualTo(expectedRequestJson);

        and("response is marked as generated by AI");
        objectTypes.getObjectType().forEach(
                o -> {
                    assertAiProvidedMarkPresentRequired(o,
                            ResourceObjectTypeDefinitionType.F_KIND,
                            ResourceObjectTypeDefinitionType.F_INTENT);
                            ResourceObjectTypeDefinitionType.F_DELINEATION.append(ResourceObjectTypeDelineationType.F_FILTER);
                    assertAiProvidedMarkPresent(o,
                            ItemPath.create(
                                    ResourceObjectTypeDefinitionType.F_DELINEATION,
                                    ResourceObjectTypeDelineationType.F_BASE_CONTEXT,
                                    ResourceObjectReferenceType.F_OBJECT_CLASS),
                            ItemPath.create(
                                    ResourceObjectTypeDefinitionType.F_DELINEATION,
                                    ResourceObjectTypeDelineationType.F_BASE_CONTEXT,
                                    ResourceObjectReferenceType.F_FILTER));
                    assertAiProvidedMarkAbsentRequired(o,
                            ResourceObjectTypeDefinitionType.F_DELINEATION.append(ResourceObjectTypeDelineationType.F_OBJECT_CLASS));
                });
    }

    private SiSuggestObjectTypesRequestType normalizeSiSuggestObjectTypesRequest(Object rawData) {
        var data = (SiSuggestObjectTypesRequestType) rawData;
        data.getSchema().getAttribute().sort(Comparator.comparing(a -> a.getName()));
        data.getStatistics().getAttribute().sort(Comparator.comparing(a -> a.getRef().toString()));
        return data;
    }

    /** What if the service returns an error in the filter? */
    @Test
    public void test120SuggestObjectTypesWithErrorInFilter() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(
                new SiSuggestObjectTypesResponseType()
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("employee")
                                .filter("attributes/unknown-attribute = 'employee'")));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting object types");
        try {
            smartIntegrationService.suggestObjectTypes(
                    RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME,
                    new ShadowObjectClassStatisticsType(), task, result);
            fail("unexpected success");
        } catch (SchemaException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Path attributes/unknown-attribute is not present");
        }
    }

    /** What if the service returns an error in the base context object class? */
    @Test
    public void test130SuggestObjectTypesWithErrorInBaseContextObjectClass() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(
                new SiSuggestObjectTypesResponseType()
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("employee")
                                .baseContextFilter("attributes/cn = 'evolveum'")
                                .baseContextObjectClassName("unknownObjectClass")));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting object types");
        try {
            smartIntegrationService.suggestObjectTypes(
                    RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME,
                    new ShadowObjectClassStatisticsType(), task, result);
            fail("unexpected success");
        } catch (SchemaException e) {
            assertExpectedException(e)
                    .hasMessageContaining("unknownObjectClass not found");
        }
    }

    /** All features: both filters and base context, plus multiple object types. */
    @Test(enabled = false) // MID-10872
    public void test140ConflictingObjectTypes() throws CommonException, IOException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(
                new SiSuggestObjectTypesResponseType()
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("employee")
                                .filter("attributes/type = 'employee1'")
                                .baseContextObjectClassName("organizationalUnit")
                                .baseContextFilter("attributes/cn = 'evolveum'"))
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("employee") // the same as above
                                .filter("attributes/type = 'employee2'")
                                .baseContextObjectClassName("organizationalUnit")
                                .baseContextFilter("attributes/cn = 'evolveum'"))
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind("account")
                                .intent("employee") // the same as above, but different base context
                                .filter("attributes/type = 'employee3'")));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        var shadowObjectClassStatistics = parseStatistics(TEST_1XX_STATISTICS);

        when("suggesting object types");
        var objectTypes = smartIntegrationService.suggestObjectTypes(
                RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME, shadowObjectClassStatistics, task, result);

        then("suggested types are correct");
        assertSuccess(result);
        displayValueAsXml("suggested object types", objectTypes);
        assertThat(objectTypes.getObjectType())
                .as("suggested object types")
                .containsExactlyInAnyOrderElementsOf(
                        parseObjectTypesSuggestion(TEST_140_EXPECTED_OBJECT_TYPES).getObjectType());
    }

    /** Tests the accounts statistics computer. */
    @Test
    public void test200ComputeAccountStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();
        assertThat(statistics.getSize()).isEqualTo(5);
        Set<String> attributeNames = statistics.getAttribute().stream()
                .map(attr -> attr.getRef().toString())
                .collect(Collectors.toSet());
        assertThat(attributeNames).contains(
                s(ICFS_NAME),
                s(ICFS_UID),
                s(Account.AttributeNames.LAST_LOGIN.q()),
                s(Account.AttributeNames.PHONE.q()),
                s(Account.AttributeNames.CREATED.q()),
                s(Account.AttributeNames.DESCRIPTION.q()),
                s(Account.AttributeNames.PERSONAL_NUMBER.q()),
                s(Account.AttributeNames.FULLNAME.q()),
                s(Account.AttributeNames.DEPARTMENT.q()),
                s(Account.AttributeNames.TYPE.q()),
                s(Account.AttributeNames.EMAIL.q()),
                s(Account.AttributeNames.STATUS.q())
        );

        for (QName attributeName : List.of(Account.AttributeNames.LAST_LOGIN.q(), Account.AttributeNames.DESCRIPTION.q())) {
            var emptyAttribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(s(attributeName)))
                    .findFirst().orElseThrow();
            assertThat(emptyAttribute.getMissingValueCount()).isEqualTo(5);
            assertThat(emptyAttribute.getUniqueValueCount()).isEqualTo(0);
            assertThat(emptyAttribute.getValueCount()).isEmpty();
        }

        for (QName attributeName : List.of(ICFS_UID, ICFS_NAME, Account.AttributeNames.FULLNAME.q(), Account.AttributeNames.EMAIL.q())) {
            var distinctAttribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(s(attributeName)))
                    .findFirst().orElseThrow();
            assertThat(distinctAttribute.getMissingValueCount()).isEqualTo(0);
            assertThat(distinctAttribute.getUniqueValueCount()).isEqualTo(5);
            assertThat(distinctAttribute.getValueCount()).isEmpty();
        }

        var personalNumberAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.PERSONAL_NUMBER.q())))
                .findFirst().orElseThrow();

        assertThat(personalNumberAttribute.getMissingValueCount()).isEqualTo(0);
        assertThat(personalNumberAttribute.getUniqueValueCount()).isEqualTo(1);
        assertThat(personalNumberAttribute.getValueCount().size()).isEqualTo(1);

        testStatusAttributeStatistics();
        testDepartmentAttributeStatistics();
        testPhoneAttributeStatistics();
        testCreatedAttributeStatistics();
        testTypeAttributeStatistics();
    }

    /** Returns the string representation of the attribute name. To be used */
    private String s(QName attrName) {
        return ShadowType.F_ATTRIBUTES.append(attrName).toBean().toString();
    }

    /** Tests status attribute statistics. */
    private void testStatusAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var statusAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.STATUS.q())))
                .findFirst().orElseThrow();

        assertThat(statusAttribute.getMissingValueCount()).isEqualTo(0);
        assertThat(statusAttribute.getUniqueValueCount()).isEqualTo(2);
        assertThat(statusAttribute.getValueCount().size()).isEqualTo(2);

        Map<String, Integer> valueCounts = statusAttribute.getValueCount().stream()
                .collect(Collectors.toMap(ShadowAttributeValueCountType::getValue, ShadowAttributeValueCountType::getCount));

        assertThat(valueCounts).containsEntry("active", 2);
        assertThat(valueCounts).containsEntry("inactive", 3);
    }

    /** Tests department attribute statistics. */
    private void testDepartmentAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var departmentAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.DEPARTMENT.q())))
                .findFirst().orElseThrow();

        assertThat(departmentAttribute.getMissingValueCount()).isEqualTo(3);
        assertThat(departmentAttribute.getUniqueValueCount()).isEqualTo(2);
        assertThat(departmentAttribute.getValueCount()).isEmpty();
    }

    /** Tests phone attribute statistics. */
    private void testPhoneAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var phoneAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.PHONE.q())))
                .findFirst().orElseThrow();

        assertThat(phoneAttribute.getMissingValueCount()).isEqualTo(2);
        assertThat(phoneAttribute.getUniqueValueCount()).isEqualTo(3);
        assertThat(phoneAttribute.getValueCount()).isEmpty();
    }

    /** Tests created attribute statistics. */
    private void testCreatedAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var createdAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.CREATED.q())))
                .findFirst().orElseThrow();

        assertThat(createdAttribute.getMissingValueCount()).isEqualTo(4);
        assertThat(createdAttribute.getUniqueValueCount()).isEqualTo(1);
        assertThat(createdAttribute.getValueCount()).isEmpty();
    }

    /** Tests type attribute statistics. */
    private void testTypeAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var typeAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.TYPE.q())))
                .findFirst().orElseThrow();

        assertThat(typeAttribute.getMissingValueCount()).isEqualTo(1);
        assertThat(typeAttribute.getUniqueValueCount()).isEqualTo(3);
        Map<String, Integer> valueCounts = typeAttribute.getValueCount().stream()
                .collect(Collectors.toMap(ShadowAttributeValueCountType::getValue, ShadowAttributeValueCountType::getCount));
        assertThat(valueCounts).containsEntry("employee", 2);
        assertThat(valueCounts).containsEntry("manager", 1);
        assertThat(valueCounts).containsEntry("contractor", 1);
    }

    private static <T, R extends Comparable<? super R>> boolean isSortedDesc(List<T> list, Function<T, R> f) {
        Comparator<T> comp = Comparator.comparing(f);
        for (int i = 0; i < list.size() - 1; ++i) {
            T left = list.get(i);
            T right = list.get(i + 1);
            if (comp.compare(left, right) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Tests the accounts statistics computer after adding more accounts, exceeding percentage limit for some attributes. */
    @Test
    public void test210ComputeAccountStatisticsExceedingTopNLimit() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("additional accounts are created, exceeding the percentage limit for unique attribute values");
        addDummyAccountsExceedingLimit();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();
        assertThat(statistics.getSize()).isEqualTo(105);
        for (var attribute : statistics.getAttribute()) {
            if (attribute.getMissingValueCount() < 105) {
                assertThat(attribute.getUniqueValueCount()).isGreaterThan(0);
            }
            assertThat(attribute.getValueCount().size()).isLessThanOrEqualTo(30);
            if (!attribute.getValueCount().isEmpty()) {
                assertThat(isSortedDesc(attribute.getValueCount(), ShadowAttributeValueCountType::getCount)).isTrue();
            }
        }
        for (QName attributeName : List.of(ICFS_NAME, ICFS_UID)) {
            var attribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(s(attributeName)))
                    .findFirst().orElseThrow();
            assertThat(attribute.getUniqueValueCount()).isEqualTo(105);
            assertThat(attribute.getValueCount()).isEmpty();
        }
    }

    @Test
    public void test220ComputeValueCountPairStatistics() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("additional accounts are created, exceeding the percentage limit for unique attribute values");
        addDummyAccountsExceedingLimit();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();

        Set<QName> attributes = statistics.getAttributeTuple().stream()
                .flatMap(attrTuple -> attrTuple.getRef().stream())
                .map(t -> t.getItemPath().rest().asSingleNameOrFail())
                .collect(Collectors.toSet());
        Set<QName> fields = Set.of(Account.AttributeNames.CREATED.q(), Account.AttributeNames.TYPE.q(), Account.AttributeNames.STATUS.q());
        assertThat(attributes).containsExactlyInAnyOrderElementsOf(fields);

        for (ShadowAttributeTupleStatisticsType tuple : statistics.getAttributeTuple()) {
            assertThat(tuple.getTupleCount()).isNotEmpty();
            for (ShadowAttributeTupleCountType tupleCount : tuple.getTupleCount()) {
                assertThat(tupleCount.getValue()).isNotEmpty();
                assertThat(tupleCount.getCount()).isGreaterThanOrEqualTo(1);
            }
        }
    }

    @Test
    public void test230ComputeAffixesStatistics() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("additional accounts are created, exceeding the percentage limit for unique attribute values");
        addDummyAccountsWithAffixes();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();
        for (var attribute : statistics.getAttribute()) {
            if (attribute.getRef().toString().equals(s(Account.AttributeNames.PERSONAL_NUMBER.q()))) {
                assertThat(attribute.getValuePatternCount()).isNotEmpty();
                assertThat(attribute.getValuePatternCount().size()).isEqualTo(17);
                for (ShadowAttributeValuePatternCountType patternCount : attribute.getValuePatternCount()) {
                    assertThat(patternCount.getValue()).isNotEmpty();
                    assertThat(patternCount.getType()).isNotNull();
                    assertThat(patternCount.getCount()).isGreaterThanOrEqualTo(1);
                }
            }
        }
    }

    @Test
    public void test240ComputeDNattributeStatistics() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("additional accounts are created, exceeding the percentage limit for unique attribute values");
        addDummyAccountsWithDN();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();
        var dnAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals(s(Account.AttributeNames.DN.q())))
                .findFirst().orElseThrow();
        assertThat(dnAttribute.getValuePatternCount()).isNotEmpty();
        assertThat(dnAttribute.getValuePatternCount().size()).isEqualTo(4);
        for (ShadowAttributeValuePatternCountType entry : dnAttribute.getValuePatternCount()) {
            assertThat(entry.getValue()).isNotEmpty();
            assertThat(entry.getValue()).startsWithIgnoringCase("ou=");
            assertThat(entry.getValue()).doesNotStartWithIgnoringCase("cn=");
            assertThat(entry.getCount()).isGreaterThanOrEqualTo(1);
            assertThat(entry.getType()).isNotNull();
            assertThat(entry.getType()).isEqualTo(ShadowValuePatternType.DN_SUFFIX);
        }
    }


    @SuppressWarnings("SameParameterValue")
    private ShadowObjectClassStatisticsType computeStatistics(QName objectClassName, Task task, OperationResult result)
            throws CommonException {
        var resource = Resource.of(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.get());
        var accountDef = resource
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(objectClassName);
        var computer = new StatisticsComputer(accountDef);
        var shadows = provisioningService.searchShadows(
                resource.queryFor(objectClassName).build(),
                null,
                task, result);
        for (var shadow : shadows) {
            computer.process(shadow.getBean());
        }
        computer.postProcessStatistics();
        return computer.getStatistics();
    }

    private static ShadowObjectClassStatisticsType parseStatistics(File file) throws IOException, SchemaException {
        return parseFile(file, ShadowObjectClassStatisticsType.class);
    }

    private static ObjectTypesSuggestionType parseObjectTypesSuggestion(File file) throws IOException, SchemaException {
        return parseFile(file, ObjectTypesSuggestionType.class);
    }

    private static <T> T parseFile(File file, Class<T> clazz) throws IOException, SchemaException {
        return PrismContext.get().parserFor(file).parseRealValue(clazz);
    }

    private void skipIfRealService() {
        skipTestIf(DefaultServiceClientImpl.hasServiceUrlOverride(), "Not applicable with a real service");
    }

    @Test
    public void test300SuggestMappings() throws CommonException, ActivityInterruptedException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(
                new SiMatchSchemaResponseType()
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(ICFS_NAME_PATH))
                                .midPointAttribute(asStringSimple(UserType.F_NAME)))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(Account.AttributeNames.FULLNAME.path()))
                                .midPointAttribute(asStringSimple(UserType.F_FULL_NAME)))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(Account.AttributeNames.TYPE.path()))
                                .midPointAttribute(asStringSimple(UserType.F_DESCRIPTION)))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(Account.AttributeNames.PHONE.path()))
                                .midPointAttribute(asStringSimple(UserType.F_TELEPHONE_NUMBER))),
                        // No mapping for status -> activation, as non-attribute mappings are not supported yet
                // icfs:name -> name and ri:fullName -> fullName are as-is, LLM microservice should not be called
                new SiSuggestMappingResponseType().transformationScript("if (input == 'e') { 'employee' } else if (input == 'c') { 'contractor' } else { null }"),
                new SiSuggestMappingResponseType().transformationScript("input.replaceAll('-', '')")
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting mappings");
        var suggestedMappings = smartIntegrationService.suggestMappings(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                ACCOUNT_DEFAULT,
                null, null, null, task, result);

        then("suggestion is correct");
        displayValueAsXml("suggested mappings", suggestedMappings);
        assertThat(suggestedMappings.getExtensionItem()).isEmpty(); // not implemented yet
        var attrMappings = suggestedMappings.getAttributeMappings();
        assertThat(attrMappings).as("attribute mappings").hasSize(4);
        assertSuggestion(attrMappings, ICFS_NAME, UserType.F_NAME);
        assertSuggestion(attrMappings, Account.AttributeNames.FULLNAME.q(), UserType.F_FULL_NAME);
        assertSuggestion(attrMappings, Account.AttributeNames.TYPE.q(), UserType.F_DESCRIPTION);
        assertSuggestion(attrMappings, Account.AttributeNames.PHONE.q(), UserType.F_TELEPHONE_NUMBER);
        // TODO asserting scripts
    }

    private void assertSuggestion(List<AttributeMappingsSuggestionType> attrMappings, ItemName attrName, ItemName focusItemName) {
        var def = findAttributeMappings(attrMappings, attrName).getDefinition();
        var inboundAsserter = assertThat(def.getInbound()).as("inbounds")
                .hasSize(1)
                .element(0);
        inboundAsserter
                .extracting(a -> a.getTarget().getPath().getItemPath())
                .satisfies(p -> focusItemName.equivalent(p));
        var inbound = inboundAsserter.actual();

        assertAiProvidedMarkPresentRequired(def, ResourceAttributeDefinitionType.F_REF); // selected by AI
        assertAiProvidedMarkPresent(inbound, InboundMappingType.F_EXPRESSION); // script code created by AI
        assertAiProvidedMarkPresentRequired(inbound,
                ItemPath.create(InboundMappingType.F_TARGET, VariableBindingDefinitionType.F_PATH)); // selected by AI

//        assertThat(def.getOutbound()).as("outbound")
//                .extracting(a -> a.getSource(), listAsserterFactory(VariableBindingDefinitionType.class))
//                .hasSize(1)
//                .element(0)
//                .extracting(v -> v.getPath().getItemPath())
//                .satisfies(p -> focusItemName.equivalent(p));
    }

    private @NotNull AttributeMappingsSuggestionType findAttributeMappings(
            List<AttributeMappingsSuggestionType> suggestions, ItemPath path) {
        return suggestions.stream()
                .filter(s ->
                        path.equivalent(s.getDefinition().getRef().getItemPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No suggestion found for " + path));
    }

    /**
     * This is to test whether the matching request for an org has appropriate number of attributes, i.e. that unimportant
     * ones are avoided.
     */
    @Test
    public void test310SuggestMappingsForOrg() throws CommonException, ActivityInterruptedException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(new SiMatchSchemaResponseType()); // not important for this test
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting mappings");
        smartIntegrationService.suggestMappings(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                GENERIC_ORGANIZATIONAL_UNIT,
                null, null, null, task, result);

        then("the number of attributes in the request is appropriate");
        var request = (SiMatchSchemaRequestType) mockClient.getLastRequest();
        displayValueAsXml("match schema request", request);
        var midPointAttributesNumber = request.getMidPointSchema().getAttribute().size();
        displayValue("midpoint attributes number", midPointAttributesNumber);
        assertThat(midPointAttributesNumber)
                .as("number of midPoint attributes in request")
                .isLessThanOrEqualTo(100);
    }

    @Test
    public void test400SuggestCorrelationRules() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl(
                new SiMatchSchemaResponseType()
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(Account.AttributeNames.FULLNAME.path()))
                                .midPointAttribute(asStringSimple(UserType.F_FULL_NAME)))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(Account.AttributeNames.EMAIL.path()))
                                .midPointAttribute(asStringSimple(UserType.F_EMAIL_ADDRESS)))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(asStringSimple(ICFS_NAME_PATH))
                                .midPointAttribute(asStringSimple(UserType.F_NAME))));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting correlation rules");
        var suggestedCorrelations = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                ACCOUNT_DEFAULT,
                null, task, result);

        then("suggestions are correct");
        displayValueAsXml("suggested correlations", suggestedCorrelations);
        var suggestion1 = suggestedCorrelations.getSuggestion().get(0);
        var attrMappings1 = suggestion1.getAttributes();
        assertThat(attrMappings1).as("attribute mappings").hasSize(1);
        assertCorrAttrSuggestion(attrMappings1, ICFS_NAME, UserType.F_NAME);
        var correlation1 = suggestion1.getCorrelation();
        assertThat(correlation1).as("correlation definition").isNotNull();
        CompositeCorrelatorType correlators1 = correlation1.getCorrelators();
        assertThat(correlators1).as("correlators").isNotNull();
        assertThat(correlators1.asPrismContainerValue().getItems()).as("correlators items").hasSize(1);
        assertThat(correlators1.getItems()).as("items correlators definitions").hasSize(1);
        var itemsCorrelator1 = correlators1.getItems().get(0);
        assertThat(itemsCorrelator1.getItem()).as("items correlators items").hasSize(1);
        var correlationItem1 = itemsCorrelator1.getItem().get(0);
        var correlationItemRef1 = correlationItem1.getRef();
        assertThat(correlationItemRef1).as("item correlators item ref").isNotNull();
        assertThat(correlationItemRef1.getItemPath().asSingleName()).as("correlator item").isEqualTo(UserType.F_NAME);

        var suggestion2 = suggestedCorrelations.getSuggestion().get(1);
        var attrMappings2 = suggestion2.getAttributes();
        assertThat(attrMappings2).as("attribute mappings").hasSize(1);
        assertCorrAttrSuggestion(attrMappings2, Account.AttributeNames.EMAIL.q(), UserType.F_EMAIL_ADDRESS);
        var correlation2 = suggestion2.getCorrelation();
        assertThat(correlation2).as("correlation definition").isNotNull();
        CompositeCorrelatorType correlators2 = correlation2.getCorrelators();
        assertThat(correlators2).as("correlators").isNotNull();
        assertThat(correlators2.asPrismContainerValue().getItems()).as("correlators items").hasSize(1);
        assertThat(correlators2.getItems()).as("items correlators definitions").hasSize(1);
        var itemsCorrelator2 = correlators2.getItems().get(0);
        assertThat(itemsCorrelator2.getItem()).as("items correlators items").hasSize(1);
        var correlationItem2 = itemsCorrelator2.getItem().get(0);
        var correlationItemRef2 = correlationItem2.getRef();
        assertThat(correlationItemRef2).as("item correlators item ref").isNotNull();
        assertThat(correlationItemRef2.getItemPath().asSingleName()).as("correlator item").isEqualTo(UserType.F_EMAIL_ADDRESS);

        and("response is marked as generated by AI");
        assertAiProvidedMarkPresentRequired(correlationItem1, CorrelationItemType.F_REF); // selected by AI
        assertAiProvidedMarkPresentRequired(correlationItem2, CorrelationItemType.F_REF); // selected by AI
    }

    private void assertCorrAttrSuggestion(
            List<ResourceAttributeDefinitionType> definitions, ItemName attrName, ItemName focusItemName) {
        var def = findAttributeDefinition(definitions, attrName);
        var inbound = assertThat(def.getInbound()).as("inbounds")
                .hasSize(1)
                .element(0)
                .actual();
        assertThat(inbound.getUse())
                .as("inbound mapping use")
                .isEqualTo(InboundMappingUseType.CORRELATION);
        assertThat(inbound.getTarget().getPath().getItemPath())
                .as("target item path")
                .satisfies(p -> focusItemName.equivalent(p));
        assertThat(def.getOutbound()).as("outbound").isNull();

        assertAiProvidedMarkPresentRequired(def, ResourceAttributeDefinitionType.F_REF); // selected by AI
        assertAiProvidedMarkPresent(inbound, InboundMappingType.F_EXPRESSION); // script code created by AI
        assertAiProvidedMarkPresentRequired(inbound,
                ItemPath.create(InboundMappingType.F_TARGET, VariableBindingDefinitionType.F_PATH)); // selected by AI
        assertAiProvidedMarkAbsentRequired(inbound, InboundMappingType.F_USE); // This is a constant value => no AI mark
    }

    private @NotNull ResourceAttributeDefinitionType findAttributeDefinition(
            List<ResourceAttributeDefinitionType> definitions, ItemPath path) {
        return definitions.stream()
                .filter(s -> path.equivalent(s.getRef().getItemPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No definition found for " + path));
    }

    @Test
    public void test410ItemStatisticsGetScore_UniqueComplete() throws Exception {
        // Create ItemStatistics via reflection
        Class<?> itemStatsClass = Class.forName("com.evolveum.midpoint.smart.impl.CorrelatorEvaluator$ItemStatistics");
        Constructor con = itemStatsClass.getDeclaredConstructor();
        con.setAccessible(true);
        Object itemStats = con.newInstance();

        Field objectsF = itemStatsClass.getDeclaredField("objects");
        Field missingValuesF = itemStatsClass.getDeclaredField("missingValues");
        Field skippedMultiValuedF = itemStatsClass.getDeclaredField("skippedMultiValued");
        Field distinctValuesF = itemStatsClass.getDeclaredField("distinctValues");
        objectsF.setAccessible(true);
        missingValuesF.setAccessible(true);
        skippedMultiValuedF.setAccessible(true);
        distinctValuesF.setAccessible(true);

        // 10 objects, all have value, all unique
        objectsF.setInt(itemStats, 10);
        missingValuesF.setInt(itemStats, 0);
        skippedMultiValuedF.setBoolean(itemStats, false);
        Set<Object> vals = new HashSet<>();
        for (int i = 0; i < 10; i++) vals.add("v"+i);
        ((Set<Object>) distinctValuesF.get(itemStats)).addAll(vals);

        Method getScore = itemStatsClass.getDeclaredMethod("getScore");
        Double score = (Double) getScore.invoke(itemStats);
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    public void test411ItemStatisticsGetScore_MissingValues() throws Exception {
        Class<?> itemStatsClass = Class.forName("com.evolveum.midpoint.smart.impl.CorrelatorEvaluator$ItemStatistics");
        Constructor con = itemStatsClass.getDeclaredConstructor();
        con.setAccessible(true);
        Object itemStats = con.newInstance();

        Field objectsF = itemStatsClass.getDeclaredField("objects");
        Field missingValuesF = itemStatsClass.getDeclaredField("missingValues");
        Field skippedMultiValuedF = itemStatsClass.getDeclaredField("skippedMultiValued");
        Field distinctValuesF = itemStatsClass.getDeclaredField("distinctValues");
        objectsF.setAccessible(true);
        missingValuesF.setAccessible(true);
        skippedMultiValuedF.setAccessible(true);
        distinctValuesF.setAccessible(true);

        // 10 objects, 5 missing, 5 with unique value
        objectsF.setInt(itemStats, 10);
        missingValuesF.setInt(itemStats, 5);
        skippedMultiValuedF.setBoolean(itemStats, false);
        Set<Object> vals = new HashSet<>();
        for (int i = 0; i < 5; i++) vals.add("v"+i);
        ((Set<Object>) distinctValuesF.get(itemStats)).addAll(vals);

        Method getScore = itemStatsClass.getDeclaredMethod("getScore");
        Double score = (Double) getScore.invoke(itemStats);

        // Uniqueness = 5/5 = 1, Coverage = 5/10 = 0.5, Harmonic mean = (2*1*0.5)/(1+0.5)=0.666...
        assertThat(score).isEqualTo(0.666, Offset.offset(0.01));
    }

    @Test
    public void test412ItemStatisticsGetScore_MultivaluedReturnsZero() throws Exception {
        Class<?> itemStatsClass = Class.forName("com.evolveum.midpoint.smart.impl.CorrelatorEvaluator$ItemStatistics");
        Constructor con = itemStatsClass.getDeclaredConstructor();
        con.setAccessible(true);
        Object itemStats = con.newInstance();

        Field objectsF = itemStatsClass.getDeclaredField("objects");
        Field missingValuesF = itemStatsClass.getDeclaredField("missingValues");
        Field skippedMultiValuedF = itemStatsClass.getDeclaredField("skippedMultiValued");
        Field distinctValuesF = itemStatsClass.getDeclaredField("distinctValues");
        objectsF.setAccessible(true);
        missingValuesF.setAccessible(true);
        skippedMultiValuedF.setAccessible(true);
        distinctValuesF.setAccessible(true);

        objectsF.setInt(itemStats, 10);
        missingValuesF.setInt(itemStats, 0);
        skippedMultiValuedF.setBoolean(itemStats, true); // multivalued
        Set<Object> vals = new HashSet<>();
        for (int i = 0; i < 10; i++) vals.add("v"+i);
        ((Set<Object>) distinctValuesF.get(itemStats)).addAll(vals);

        Method getScore = itemStatsClass.getDeclaredMethod("getScore");
        Double score = (Double) getScore.invoke(itemStats);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    public void test420ComputeLinkCoverage_PerfectMapping() throws Exception {
        // Get computeLinkCoverage method
        Class<?> ceClass = Class.forName("com.evolveum.midpoint.smart.impl.CorrelatorEvaluator");
        Method computeLinkCoverage = ceClass.getDeclaredMethod("computeLinkCoverage", Map.class);
        computeLinkCoverage.setAccessible(true);

        // 3 focus, each maps to one unique shadow
        Map<String, Set<String>> links = Map.of(
                "f1", Set.of("s1"),
                "f2", Set.of("s2"),
                "f3", Set.of("s3")
        );
        Object result = computeLinkCoverage.invoke(null, links);
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    public void test421ComputeLinkCoverage_AmbiguousMapping() throws Exception {
        Class<?> ceClass = Class.forName("com.evolveum.midpoint.smart.impl.CorrelatorEvaluator");
        Method computeLinkCoverage = ceClass.getDeclaredMethod("computeLinkCoverage", Map.class);
        computeLinkCoverage.setAccessible(true);

        // 2 out of 3 focus map to multiple shadows
        Map<String, Set<String>> links = Map.of(
                "f1", Set.of("s1", "s2"),
                "f2", Set.of("s3"),
                "f3", Set.of()
        );
        // f1 has 2 (ambiguity 1), f2 has 1, f3 has none.
        // linked=2, focusCount=3, avgAmbiguity=1/2=0.5, penalty=1/(1+0.5)=0.666, coverage=2/3, total=0.444
        Object result = computeLinkCoverage.invoke(null, links);
        assertThat((Double) result).isEqualTo(0.444, Offset.offset(0.01));
    }
}
