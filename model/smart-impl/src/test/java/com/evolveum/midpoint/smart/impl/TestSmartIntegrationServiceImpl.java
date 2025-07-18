/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationPrecisionType.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
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

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_COUNTING_NO_PAGING, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_COUNTING_WITH_PAGING, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        createDummyAccounts();
    }

    private void createDummyAccounts() throws Exception {
        var c = dummyForObjectTypes.getController();
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

    private void addDummyAccountsExceedingPercentageLimit() throws Exception {
        // TODO
    }

    private void addDummyAccountsExceedingHardLimit() throws Exception {
        // TODO
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
}
