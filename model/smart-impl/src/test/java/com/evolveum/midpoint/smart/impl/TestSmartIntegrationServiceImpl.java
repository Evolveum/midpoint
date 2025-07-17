/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
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
import com.evolveum.midpoint.util.exception.CommonException;

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

    private static DummyScenario dummyScenario;

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-object-types.xml", "4e673bd5-661e-4037-9e19-557ea485238b",
            "for-suggest-object-types",
            c -> dummyScenario = DummyScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        createDummyAccounts();
    }

    private void createDummyAccounts() throws Exception {
        var c = dummyScenario.getController();
        c.addAccount("jack")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10104444")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee");
        c.addAccount("jim")
                .addAttributeValues(DummyScenario.Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), "10702222")
                .addAttributeValues(DummyScenario.Account.AttributeNames.EMAIL.local(), "jim@evolveum.com")
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "suspended");
        // TODO add more accounts, with various attributes
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
}
