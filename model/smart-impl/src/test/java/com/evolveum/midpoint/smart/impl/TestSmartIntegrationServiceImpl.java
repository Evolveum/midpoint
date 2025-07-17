/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.activities.StatisticsComputer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Unit tests for the Smart Integration Service implementation.
 *
 * It is unclear if this class will ever be used.
 */
@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationServiceImpl extends AbstractModelIntegrationTest {

    private static final File TEST_DIR = TEST_RESOURCES_DIR;

    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<UserType> USER_ADMINISTRATOR = TestObject.file(
            TEST_DIR, "user-administrator.xml", "00000000-0000-0000-0000-000000000002");
    private static final TestObject<RoleType> ROLE_SUPERUSER = TestObject.file(
            TEST_DIR, "role-superuser.xml", "00000000-0000-0000-0000-000000000004");

    static final QName OC_ACCOUNT_QNAME = new QName(NS_RI, "account");

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;

    private static DummyScenario dummyScenario;

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-object-types.xml", "4e673bd5-661e-4037-9e19-557ea485238b",
            "for-suggest-object-types",
            c -> dummyScenario = DummyScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // We want logging config from logback-test.xml and not from system config object (unless suppressed)
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        repoAdd(USER_ADMINISTRATOR, initResult);
        repoAdd(ROLE_SUPERUSER, initResult);

        modelService.postInit(initResult);
        login(USER_ADMINISTRATOR.getNameOrig());

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        createDummyAccounts();

        if (System.getProperty(MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE) == null) {
            // For tests without a real service, we have to use a mock service client.
            smartIntegrationService.setServiceClientSupplier(MockServiceClientImpl::new);
        }
    }

    private void createDummyAccounts() throws Exception {
        var c = dummyScenario.getController();
        c.addAccount("jack")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active");
        c.addAccount("jim")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10702222")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jim@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "suspended");
        // TODO add more accounts, with various attributes
    }

    /** Calls the remote service directly. */
    @Test
    public void test100SuggestObjectTypes() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var shadowObjectClassStatistics = new ShadowObjectClassStatisticsType()
                .size(1000)
                .coverage(1.0f)
                .attribute(new ShadowAttributeStatisticsType()
                        .ref(ICFS_NAME)
                        .missingValueCount(0));

        when("suggesting object types");
        var objectTypes = smartIntegrationService.suggestObjectTypes(
                RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME, shadowObjectClassStatistics, task, result);

        then("there is at least one suggested object type");
        assertSuccess(result);
        assertThat(objectTypes).isNotNull();
        assertThat(objectTypes.getObjectType()).isNotEmpty();
    }

    /** Tests the accounts statistics computer. */
    @Test
    public void test200ComputeAccountStatistics() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("computing statistics for accounts");
        given("account statistics computer");
        var resource = Resource.of(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.get());
        var accountDef = resource
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(OC_ACCOUNT_QNAME);
        var computer = new StatisticsComputer(accountDef);

        when("computing statistics given the accounts");
        var shadows = provisioningService.searchShadows(
                resource.queryFor(OC_ACCOUNT_QNAME).build(),
                null,
                task, result);
        for (var shadow : shadows) {
            computer.process(shadow.getBean());
        }
        computer.postProcessStatistics();
        var statistics = computer.getStatistics();

        then("the statistics are computed");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();
        // TODO add the assertions for the attributes
    }
}
