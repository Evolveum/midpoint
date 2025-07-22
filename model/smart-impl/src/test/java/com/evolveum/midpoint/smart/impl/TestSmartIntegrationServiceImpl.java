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
import java.util.Arrays;
import java.util.Comparator;

import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.smart.impl.DummyScenario.Account;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        c.addAccount("jack1")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee");
        c.addAccount("jack2")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee");
        c.addAccount("jack3")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee");
        c.addAccount("jack4")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee");
        c.addAccount("jack5")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee");

    }

    private void addDummyAccountsExceedingPercentageLimit() throws Exception {
        var c = dummyForObjectTypes.getController();
        c.addAccount("ada")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Ada Lovelace")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Mathematician and programmer")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010027")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "ada@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000027")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "R&D")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2016-12-10T09:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-20T09:30:00Z")));

        c.addAccount("brian")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Brian Cox")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Physicist")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010028")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "brian@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000028")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Science")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2015-05-21T11:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-21T10:00:00Z")));

        c.addAccount("clara")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Clara Oswald")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "English teacher")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010029")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "clara@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000029")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Education")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-09-01T08:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-22T11:00:00Z")));

        c.addAccount("diego")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Diego Maradona")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Football coach")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010030")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "diego@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000030")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "locked")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Sports")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2017-07-01T07:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-23T12:00:00Z")));

        c.addAccount("elena")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Elena Gilbert")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Student")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010031")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "elena@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000031")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Student Affairs")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2021-01-01T10:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-24T13:00:00Z")));

        c.addAccount("felix")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Felix Leiter")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "CIA operative")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010032")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "felix@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000032")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Intelligence")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2019-03-01T12:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-25T14:00:00Z")));

        c.addAccount("greta")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Greta Thunberg")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Climate activist")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010033")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "greta@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000033")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Environment")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2020-02-01T13:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-26T15:00:00Z")));

        c.addAccount("hank")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Hank Schrader")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "DEA agent")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010034")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "hank@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000034")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Law Enforcement")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2015-11-01T14:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-27T16:00:00Z")));

        c.addAccount("irene")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Irene Adler")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Consultant")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010035")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "irene@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000035")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Consulting")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-04-01T15:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-28T17:00:00Z")));

        c.addAccount("jon")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jon Snow")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Lord Commander")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010036")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jon@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000036")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Night Watch")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2017-10-01T16:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-29T18:00:00Z")));

        c.addAccount("jim")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10702222")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "jim@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive");

        c.addAccount("alice")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Alice Wonderland")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10505555")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "alice@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+421900111222")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "HR");

        c.addAccount("bob")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Bob Builder")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10909999")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "bob@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+421900333444")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Engineering");

        c.addAccount("eve")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Eve Adams")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10303333")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "eve@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "locked")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-09-01T12:00:00Z")));

        c.addAccount("carol")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Carol Danvers")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "10808888")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "carol@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+421900555666")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Security")
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Security specialist")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2022-01-05T09:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-05-10T08:15:00Z")));

        c.addAccount("dave")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Dave Lister")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "11001111")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "dave@evolveum.com")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.now().minusDays(10)))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.now().minusDays(10)));
        c.addAccount("leo")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Leo Messi")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010012")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "leo@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000012")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Sports")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2022-06-01T12:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-06T14:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Athlete");

        c.addAccount("mia")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Mia Wallace")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010013")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "mia@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000013")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Legal")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2019-04-10T15:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-03-10T16:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Legal Advisor");

        c.addAccount("nina")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Nina Simone")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010014")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "nina@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000014")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Music")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-01-01T14:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-07T15:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Musician");

        c.addAccount("oliver")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Oliver Twist")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010015")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "oliver@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000015")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Operations")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2024-05-15T10:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-08T11:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Operations Intern");

        c.addAccount("peter")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Peter Parker")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010016")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "peter@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000016")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Photography")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-08-01T09:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-09T12:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Photographer");

        c.addAccount("quinn")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Quinn Fabray")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010017")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "quinn@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000017")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Music")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2021-09-01T10:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-10T13:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Singer");

        c.addAccount("rachel")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Rachel Green")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010018")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "rachel@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000018")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Fashion")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2022-04-01T11:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-11T14:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Fashion Manager");

        c.addAccount("sam")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Samwise Gamgee")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010019")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "sam@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000019")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Gardening")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2021-03-01T12:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-12T15:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Gardener");

        c.addAccount("tom")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Tom Riddle")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010020")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "tom@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000020")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Magic")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2020-08-01T13:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-13T16:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Wizard");

        c.addAccount("ursula")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Ursula K Le Guin")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010021")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "ursula@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000021")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "contractor")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Writing")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2017-10-01T14:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-14T17:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Author");

        c.addAccount("victor")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Victor Frankenstein")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010022")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "victor@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000022")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Research")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2018-02-01T15:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-15T18:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Scientist");

        c.addAccount("wendy")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Wendy Darling")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010023")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "wendy@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000023")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Childcare")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2024-01-01T16:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-16T19:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Nanny");

        c.addAccount("xander")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Xander Harris")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010024")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "xander@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000024")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Maintenance")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-07-01T17:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-17T20:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Janitor");

        c.addAccount("yara")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Yara Greyjoy")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010025")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "yara@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000025")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "manager")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Shipping")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2019-06-01T18:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-18T21:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Fleet Manager");

        c.addAccount("zane")
                .addAttributeValues(Account.AttributeNames.FULLNAME.local(), "Zane Malik")
                .addAttributeValues(Account.AttributeNames.PERSONAL_NUMBER.local(), "20010026")
                .addAttributeValues(Account.AttributeNames.EMAIL.local(), "zane@evolveum.com")
                .addAttributeValues(Account.AttributeNames.PHONE.local(), "+420601000026")
                .addAttributeValues(Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(Account.AttributeNames.DEPARTMENT.local(), "Investigation")
                .addAttributeValues(Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2020-09-01T19:00:00Z")))
                .addAttributeValues(Account.AttributeNames.LAST_LOGIN.local(), Arrays.asList(ZonedDateTime.parse("2024-06-19T22:00:00Z")))
                .addAttributeValues(Account.AttributeNames.DESCRIPTION.local(), "Detective");
    }

    private void addDummyAccountsExceedingHardLimit() throws Exception {

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
        assertThat(realRequest)
                .as("request (normalized)")
                .isEqualTo(
                        normalizeSiSuggestObjectTypesRequest(
                                parseFile(TEST_110_EXPECTED_REQUEST, SiSuggestObjectTypesRequestType.class)));
    }

    private SiSuggestObjectTypesRequestType normalizeSiSuggestObjectTypesRequest(Object rawData) {
        var qNameComparator =
                Comparator
                        .comparing((QName qName) -> qName.getNamespaceURI())
                        .thenComparing(qName -> qName.getLocalPart());

        var data = (SiSuggestObjectTypesRequestType) rawData;
        data.getSchema().getAttribute().sort(Comparator.comparing(a -> a.getName(), qNameComparator));
        data.getStatistics().getAttribute().sort(Comparator.comparing(a -> a.getRef(), qNameComparator));
        return data;
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
        // TODO add the assertions for the attributes
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
        // TODO add the assertions for the attributes
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
        // TODO add the assertions for the attributes
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
                UserType.COMPLEX_TYPE,
                null, null, task, result);

        then("suggestion is correct");
        displayValue("request",
                prismContext.jsonSerializer().serializeRealValueContent(mockClient.getLastRequest()));
        // TODO add assertions
    }
}
