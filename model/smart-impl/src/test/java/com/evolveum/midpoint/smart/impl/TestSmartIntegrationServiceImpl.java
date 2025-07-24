/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationPrecisionType.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.smart.impl.DummyScenario.Account;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.activities.StatisticsComputer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import javax.xml.namespace.QName;

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
    private static final File TEST_110_STATISTICS = new File(TEST_DIR, "test-110-statistics.xml");
    private static final File TEST_110_EXPECTED_OBJECT_TYPES = new File(TEST_DIR, "test-110-expected-object-types.xml");
    private static final File TEST_110_EXPECTED_REQUEST = new File(TEST_DIR, "test-110-expected-request.json");

    private static DummyScenario dummyForObjectTypes;

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
            c -> DummyScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_COUNTING_NO_PAGING, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_COUNTING_WITH_PAGING, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION, initTask, initResult);
        createDummyAccounts();
    }

    private void createDummyAccounts() throws Exception {
        var c = dummyForObjectTypes.getController();
        c.addAccount("jack")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601040027");

        c.addAccount("jim")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10702222")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jim@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive");

        c.addAccount("alice")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Alice Wonderland")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10505555")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "alice@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421900111222")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR");

        c.addAccount("bob")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Bob Builder")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10909999")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "bob@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421900333444")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Engineering");

        c.addAccount("eve")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Eve Adams")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10303333")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "eve@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "locked")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-09-01T12:00:00Z")));

    }

    private void addDummyAccountsExceedingPercentageLimit() throws Exception {
        int numberOfAccounts = 200;
        var c = dummyForObjectTypes.getController();
        for (int i = 0; i < numberOfAccounts; i++) {
            c.addAccount("user" + i)
                    .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), String.valueOf(10000000 + i%5))
                    .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420600000" + String.format("%03d", i%10))
                    .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR");
        }
    }

    private void addDummyAccountsExceedingHardLimit() throws Exception {
        var c = dummyForObjectTypes.getController();
        c.addAccount("ada")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Ada Lovelace")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Mathematician and programmer")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010027")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "ada@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000027")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "R&D")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2016-12-10T09:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-20T09:30:00Z")));

        c.addAccount("brian")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Brian Cox")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Physicist")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010028")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "brian@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000028")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Science")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2015-05-21T11:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-21T10:00:00Z")));

        c.addAccount("clara")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Clara Oswald")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "English teacher")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010029")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "clara@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000029")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Education")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-09-01T08:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-22T11:00:00Z")));

        c.addAccount("diego")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Diego Maradona")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Football coach")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010030")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "diego@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000030")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "locked")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sports")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2017-07-01T07:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-23T12:00:00Z")));

        c.addAccount("elena")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Elena Gilbert")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Student")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010031")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "elena@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000031")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Student Affairs")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2021-01-01T10:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-24T13:00:00Z")));

        c.addAccount("felix")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Felix Leiter")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "CIA operative")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010032")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "felix@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000032")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Intelligence")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2019-03-01T12:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-25T14:00:00Z")));

        c.addAccount("greta")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Greta Thunberg")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Climate activist")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010033")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "greta@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000033")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Environment")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2020-02-01T13:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-26T15:00:00Z")));

        c.addAccount("hank")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Hank Schrader")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "DEA agent")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010034")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "hank@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000034")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Law Enforcement")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2015-11-01T14:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-27T16:00:00Z")));

        c.addAccount("irene")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Irene Adler")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Consultant")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010035")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "irene@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000035")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Consulting")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-04-01T15:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-28T17:00:00Z")));

        c.addAccount("jon")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jon Snow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Lord Commander")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010036")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jon@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000036")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Night Watch")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2017-10-01T16:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-29T18:00:00Z")));

        c.addAccount("carol")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Carol Danvers")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10808888")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "carol@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+421900555666")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Security")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Security specialist")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2022-01-05T09:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-05-10T08:15:00Z")));

        c.addAccount("dave")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Dave Lister")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "11001111")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "dave@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.now().minusDays(10)))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.now().minusDays(10)));
        c.addAccount("leo")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Leo Messi")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010012")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "leo@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000012")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Sports")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2022-06-01T12:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-06T14:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Athlete");

        c.addAccount("mia")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Mia Wallace")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010013")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "mia@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000013")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Legal")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2019-04-10T15:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-03-10T16:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Legal Advisor");

        c.addAccount("nina")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Nina Simone")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010014")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "nina@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000014")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Music")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-01-01T14:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-07T15:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Musician");

        c.addAccount("oliver")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Oliver Twist")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010015")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "oliver@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000015")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Operations")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2024-05-15T10:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-08T11:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Operations Intern");

        c.addAccount("peter")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Peter Parker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010016")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "peter@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000016")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Photography")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-08-01T09:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-09T12:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Photographer");

        c.addAccount("quinn")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Quinn Fabray")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010017")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "quinn@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000017")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Music")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2021-09-01T10:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-10T13:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Singer");

        c.addAccount("rachel")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Rachel Green")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010018")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "rachel@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000018")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Fashion")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2022-04-01T11:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-11T14:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Fashion Manager");

        c.addAccount("sam")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Samwise Gamgee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010019")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "sam@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000019")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Gardening")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2021-03-01T12:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-12T15:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Gardener");

        c.addAccount("tom")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Tom Riddle")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010020")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "tom@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000020")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Magic")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2020-08-01T13:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-13T16:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Wizard");

        c.addAccount("ursula")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Ursula K Le Guin")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010021")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "ursula@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000021")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Writing")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2017-10-01T14:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-14T17:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Author");

        c.addAccount("victor")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Victor Frankenstein")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010022")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "victor@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000022")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Research")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-02-01T15:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-15T18:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Scientist");

        c.addAccount("wendy")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Wendy Darling")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010023")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "wendy@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000023")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Childcare")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2024-01-01T16:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-16T19:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Nanny");

        c.addAccount("xander")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Xander Harris")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010024")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "xander@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000024")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Maintenance")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-07-01T17:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-17T20:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Janitor");

        c.addAccount("yara")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Yara Greyjoy")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010025")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "yara@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000025")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Shipping")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2019-06-01T18:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-18T21:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Fleet Manager");

        c.addAccount("zane")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Zane Malik")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "20010026")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "zane@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PHONE.local(), "+420601000026")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "Investigation")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2020-09-01T19:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-19T22:00:00Z")))
                .addAttributeValues(DummyScenario.Account.AttributeNames.DESCRIPTION.local(), "Detective");
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
                    () -> new MockServiceClientImpl<>(
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
    }

    /** All features: both filters and base context, plus multiple object types. */
    @Test
    public void test110SuggestObjectTypesWithFiltersAndBaseContext() throws CommonException, IOException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
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

        var shadowObjectClassStatistics = parseStatistics(TEST_110_STATISTICS);

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

        System.out.println(realRequest.equals(expectedRequest));

        assertThat(realRequest)
                .as("request (normalized)")
                .isEqualTo(expectedRequest);
    }

    private SiSuggestObjectTypesRequestType normalizeSiSuggestObjectTypesRequest(Object rawData) {
        var qNameComparator =
                Comparator
                        .comparing((QName qName) -> qName.getNamespaceURI())
                        .thenComparing(qName -> qName.getLocalPart());

        var data = (SiSuggestObjectTypesRequestType) rawData;
        for (var attrDef : data.getSchema().getAttribute()) {
            attrDef.setName(normalizeItemPathType(attrDef.getName()));
        }
        data.getSchema().getAttribute().sort(Comparator.comparing(a -> a.getName().toString()));
        data.getStatistics().getAttribute().sort(Comparator.comparing(a -> a.getRef(), qNameComparator));
        return data;
    }

    private ItemPathType normalizeItemPathType(ItemPathType pathType) {
        var string = PrismContext.get().itemPathSerializer().serializeStandalone(pathType.getItemPath());
        return PrismContext.get().itemPathParser().asItemPathType(string);
    }

    /** What if the service returns an error in the filter? */
    @Test
    public void test120SuggestObjectTypesWithErrorInFilter() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
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
        var mockClient = new MockServiceClientImpl<>(
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
                "lastLogin", "uid", "phone", "created", "name", "description", "personalNumber", "fullname", "department", "type", "email", "status"
        );

        for (String attributeName : List.of("lastLogin", "description")) {
            var emptyAttribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(attributeName))
                    .findFirst().orElseThrow();
            assertThat(emptyAttribute.getMissingValueCount()).isEqualTo(5);
            assertThat(emptyAttribute.getUniqueValueCount()).isEqualTo(0);
            assertThat(emptyAttribute.getValueCount()).isEmpty();
        }

        for (String attributeName : List.of("uid", "name", "fullname", "email", "personalNumber")) {
            var distinctAttribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(attributeName))
                    .findFirst().orElseThrow();
            assertThat(distinctAttribute.getMissingValueCount()).isEqualTo(0);
            assertThat(distinctAttribute.getUniqueValueCount()).isEqualTo(5);
            assertThat(distinctAttribute.getValueCount()).hasSize(5);
            for (var vc : distinctAttribute.getValueCount()) {
                assertThat(vc.getCount()).isEqualTo(1);
            }
        }
    }

    /** Tests status attribute statistics. */
    @Test
    public void test201StatusAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var statusAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("status"))
                .findFirst().orElseThrow();

        assertThat(statusAttribute.getMissingValueCount()).isEqualTo(0);
        assertThat(statusAttribute.getUniqueValueCount()).isEqualTo(3);
        assertThat(statusAttribute.getValueCount().size()).isEqualTo(3);

        Map<String, Integer> valueCounts = statusAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));

        assertThat(valueCounts).containsEntry("active", 2);
        assertThat(valueCounts).containsEntry("inactive", 2);
        assertThat(valueCounts).containsEntry("locked", 1);
    }

    /** Tests department attribute statistics. */
    @Test
    public void test202DepartmentAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var departmentAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("department"))
                .findFirst().orElseThrow();

        assertThat(departmentAttribute.getMissingValueCount()).isEqualTo(3);
        assertThat(departmentAttribute.getUniqueValueCount()).isEqualTo(2);
        Map<String, Integer> valueCounts = departmentAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));
        assertThat(valueCounts).containsEntry("HR", 1);
        assertThat(valueCounts).containsEntry("Engineering", 1);
    }

    /** Tests phone attribute statistics. */
    @Test
    public void test203PhoneAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var phoneAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("phone"))
                .findFirst().orElseThrow();

        assertThat(phoneAttribute.getMissingValueCount()).isEqualTo(2);
        assertThat(phoneAttribute.getUniqueValueCount()).isEqualTo(3);
        Map<String, Integer> valueCounts = phoneAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));
        assertThat(valueCounts).containsEntry("+420601040027", 1);
        assertThat(valueCounts).containsEntry("+421900111222", 1);
        assertThat(valueCounts).containsEntry("+421900333444", 1);
    }

    /** Tests created attribute statistics. */
    @Test
    public void test204CreatedAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var createdAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("created"))
                .findFirst().orElseThrow();

        assertThat(createdAttribute.getMissingValueCount()).isEqualTo(4);
        assertThat(createdAttribute.getUniqueValueCount()).isEqualTo(1);
        assertThat(createdAttribute.getValueCount()).hasSize(1);
        assertThat(createdAttribute.getValueCount().get(0).getCount()).isEqualTo(1);
        assertThat(createdAttribute.getValueCount().get(0).getValue().toString()).isEqualTo("2023-09-01T12:00:00.000Z");
    }

    /** Tests type attribute statistics. */
    @Test
    public void test205TypeAttributeStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var typeAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("type"))
                .findFirst().orElseThrow();

        assertThat(typeAttribute.getMissingValueCount()).isEqualTo(1);
        assertThat(typeAttribute.getUniqueValueCount()).isEqualTo(3);
        Map<String, Integer> valueCounts = typeAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));
        assertThat(valueCounts).containsEntry("employee", 2);
        assertThat(valueCounts).containsEntry("manager", 1);
        assertThat(valueCounts).containsEntry("contractor", 1);
    }

    /** Tests the accounts statistics computer after adding more accounts, exceeding percentage limit for some attributes. */
    @Test
    public void test210ComputeAccountStatisticsExceedingPercentageLimit() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("additional accounts are created, exceeding the percentage limit for unique attribute values");
        addDummyAccountsExceedingPercentageLimit();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();

        // Exceeding percentage limit 10.25, but not exceeding hard limit 30
        var phoneAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("phone"))
                .findFirst().orElseThrow();
        assertThat(phoneAttribute.getUniqueValueCount()).isEqualTo(-1);
        assertThat(phoneAttribute.getValueCount()).isEmpty();

        // Not exceedint percentage limit 10.25
        var personalNumberAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("personalNumber"))
                .findFirst().orElseThrow();
        assertThat(personalNumberAttribute.getUniqueValueCount()).isEqualTo(10);
        assertThat(personalNumberAttribute.getValueCount()).isNotEmpty();

        var departmentAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("department"))
                .findFirst().orElseThrow();
        assertThat(departmentAttribute.getValueCount()).isNotEmpty();
        assertThat(departmentAttribute.getUniqueValueCount()).isEqualTo(2);
        Map<String, Integer> valueCounts = departmentAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));
        assertThat(valueCounts).containsEntry("HR", 201);
        assertThat(valueCounts).containsEntry("Engineering", 1);
    }

    /** Tests the accounts statistics computer after adding more accounts, exceeding hard limit for some attributes. */
    @Test
    public void test220ComputeAccountStatisticsExceedingHardLimit() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("additional accounts are created, exceeding the hard limit for unique attribute values");
        addDummyAccountsExceedingHardLimit();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();

        for (String attributeName : List.of("lastLogin", "uid", "phone", "created", "name", "description", "personalNumber", "fullname", "department", "email")) {
            var attribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(attributeName))
                    .findFirst().orElseThrow();
            assertThat(attribute.getUniqueValueCount()).isEqualTo(-1);
            assertThat(attribute.getValueCount()).isEmpty();
        }

        var typeAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("type"))
                .findFirst().orElseThrow();
        assertThat(typeAttribute.getValueCount()).isNotEmpty();
        assertThat(typeAttribute.getUniqueValueCount()).isEqualTo(3);
        assertThat(typeAttribute.getMissingValueCount()).isEqualTo(201);
        Map<String, Integer> typeValueCounts = typeAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));
        assertThat(typeValueCounts).containsEntry("employee", 21);
        assertThat(typeValueCounts).containsEntry("manager", 4);
        assertThat(typeValueCounts).containsEntry("contractor", 6);

        var statusAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("status"))
                .findFirst().orElseThrow();
        assertThat(statusAttribute.getValueCount()).isNotEmpty();
        assertThat(statusAttribute.getUniqueValueCount()).isEqualTo(3);
        assertThat(statusAttribute.getMissingValueCount()).isEqualTo(200);
        Map<String, Integer> statusValueCounts = statusAttribute.getValueCount().stream()
                .collect(Collectors.toMap(vc -> vc.getValue().toString(), vc -> vc.getCount()));
        assertThat(statusValueCounts).containsEntry("active", 23);
        assertThat(statusValueCounts).containsEntry("inactive", 7);
        assertThat(statusValueCounts).containsEntry("locked", 2);
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
    public void test300SuggestMappings() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
                new SiMatchSchemaResponseType()
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(Account.AttributeNames.FULLNAME.q())
                                .midPointAttribute(UserType.F_FULL_NAME)));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting mappings");
        var suggestedMappings = smartIntegrationService.suggestMappings(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                ACCOUNT_DEFAULT,
                null, null, task, result);

        then("suggestion is correct");
        displayValue("request",
                prismContext.jsonSerializer().serializeRealValueContent(mockClient.getLastRequest()));
        // TODO add assertions
    }

    @Test
    public void test400SuggestCorrelationRules() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
                new SiMatchSchemaResponseType()
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(Account.AttributeNames.FULLNAME.q())
                                .midPointAttribute(UserType.F_FULL_NAME)));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting correlation rules");
        var suggestedCorrelation = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                ACCOUNT_DEFAULT,
                null, task, result);

        then("suggestion is correct");
        displayDumpable("suggested correlation", suggestedCorrelation);
        // TODO add assertions
    }
}
