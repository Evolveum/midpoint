/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationPrecisionType.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.impl.DummyScenario.Account;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
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
    private static final File TEST_1XX_STATISTICS = new File(TEST_DIR, "test-1xx-statistics.xml");
    private static final File TEST_110_EXPECTED_OBJECT_TYPES = new File(TEST_DIR, "test-110-expected-object-types.xml");
    private static final File TEST_140_EXPECTED_OBJECT_TYPES = new File(TEST_DIR, "test-140-expected-object-types.xml");
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
                .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), "HR");

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
                .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), "locked")
                .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), "employee")
                .addAttributeValues(DummyScenario.Account.AttributeNames.CREATED.local(), Arrays.asList(ZonedDateTime.parse("2023-09-01T12:00:00Z")));

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

    private void addDummyAccountsExceedingTopNLimit() throws Exception {
        var c = dummyForObjectTypes.getController();
        String[] departments = {"HR", "Engineering", "Sales", "Marketing", "IT", "Finance", "Support"};
        int[] departmentWeights = {1, 5, 3, 2, 4, 2, 1};

        String[] types = {"employee", "manager", "contractor", "intern"};
        int[] typeWeights = {7, 2, 3, 1};

        String[] statuses = {"active", "inactive", "locked", "pending"};
        int[] statusWeights = {8, 2, 1, 1};

        Random rand = new Random();

        for (int i = 1; i <= 100; i++) {
            String username = "user" + i;
            String personalNumber = String.format("%08d", 10000000 + i);
            String department = pickWeightedRandom(departments, departmentWeights, rand);
            String type = pickWeightedRandom(types, typeWeights, rand);
            String status = pickWeightedRandom(statuses, statusWeights, rand);

            c.addAccount(username)
                    .addAttributeValues(DummyScenario.Account.AttributeNames.PERSONAL_NUMBER.local(), personalNumber)
                    .addAttributeValues(DummyScenario.Account.AttributeNames.DEPARTMENT.local(), department)
                    .addAttributeValues(DummyScenario.Account.AttributeNames.TYPE.local(), type)
                    .addAttributeValues(DummyScenario.Account.AttributeNames.STATUS.local(), status);
        }
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

    /** All features: both filters and base context, plus multiple object types. */
    @Test
    public void test140ConflictingObjectTypes() throws CommonException, IOException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
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

        for (String attributeName : List.of("uid", "name", "fullname", "email")) {
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

        var personalNumberAttribute = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().toString().equals("personalNumber"))
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

    /** Tests status attribute statistics. */
    private void testStatusAttributeStatistics() throws CommonException {
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
    private void testDepartmentAttributeStatistics() throws CommonException {
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
    private void testPhoneAttributeStatistics() throws CommonException {
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
    private void testCreatedAttributeStatistics() throws CommonException {
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
    private void testTypeAttributeStatistics() throws CommonException {
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
        addDummyAccountsExceedingTopNLimit();

        when("computing statistics for accounts");
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        then("the statistics are OK, value stats for particular attributes are eliminated");
        displayValue("statistics", PrismContext.get().jsonSerializer().serializeRealValueContent(statistics));
        assertThat(statistics).isNotNull();
        assertThat(statistics.getAttribute()).isNotEmpty();
        assertThat(statistics.getSize()).isEqualTo(105);
        for (var attribute : statistics.getAttribute()) {
            if (attribute.getMissingValueCount() < 105) {
                assertThat(attribute.getValueCount()).isNotEmpty();
                assertThat(attribute.getUniqueValueCount()).isGreaterThan(0);
            }
            assertThat(attribute.getValueCount().size()).isLessThanOrEqualTo(30);
            assertThat(isSortedDesc(attribute.getValueCount(), ShadowAttributeValueCountType::getCount)).isTrue();
        }
        for (String attributeName : List.of("name", "uid")) {
            var attribute = statistics.getAttribute().stream()
                    .filter(attr -> attr.getRef().toString().equals(attributeName))
                    .findFirst().orElseThrow();
            assertThat(attribute.getUniqueValueCount()).isEqualTo(105);
            assertThat(attribute.getValueCount()).isNotEmpty();
            assertThat(attribute.getValueCount().size()).isLessThanOrEqualTo(30);
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
    public void test300SuggestMappings() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
                new SiMatchSchemaResponseType()
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(Account.AttributeNames.FULLNAME.q().toBean())
                                .midPointAttribute(UserType.F_FULL_NAME.toBean()))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(ICFS_NAME.toBean())
                                .midPointAttribute(UserType.F_NAME.toBean())));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting mappings");
        var suggestedMappings = smartIntegrationService.suggestMappings(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                ACCOUNT_DEFAULT,
                null, null, task, result);

        then("suggestion is correct");
        displayValueAsXml("suggested mappings", suggestedMappings);
        assertThat(suggestedMappings.getExtensionItem()).isEmpty(); // not implemented yet
        var attrMappings = suggestedMappings.getAttributeMappings();
        assertThat(attrMappings).as("attribute mappings").hasSize(2);
        assertSuggestion(attrMappings, ICFS_NAME, UserType.F_NAME);
        assertSuggestion(attrMappings, Account.AttributeNames.FULLNAME.q(), UserType.F_FULL_NAME);
    }

    private void assertSuggestion(List<AttributeMappingsSuggestionType> attrMappings, ItemName attrName, ItemName focusItemName) {
        var def = findAttributeMappings(attrMappings, attrName).getDefinition();
        assertThat(def.getInbound()).as("inbounds")
                .hasSize(1)
                .element(0)
                .extracting(a -> a.getTarget().getPath().getItemPath())
                .satisfies(p -> focusItemName.equivalent(p));
        assertThat(def.getOutbound()).as("outbound")
                .extracting(a -> a.getSource(), listAsserterFactory(VariableBindingDefinitionType.class))
                .hasSize(1)
                .element(0)
                .extracting(v -> v.getPath().getItemPath())
                .satisfies(p -> focusItemName.equivalent(p));
    }

    private @NotNull AttributeMappingsSuggestionType findAttributeMappings(
            List<AttributeMappingsSuggestionType> suggestions, ItemPath path) {
        return suggestions.stream()
                .filter(s ->
                        path.equivalent(s.getDefinition().getRef().getItemPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No suggestion found for " + path));
    }

    @Test
    public void test400SuggestCorrelationRules() throws CommonException {
        skipIfRealService();

        //noinspection resource
        var mockClient = new MockServiceClientImpl<>(
                new SiMatchSchemaResponseType()
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(Account.AttributeNames.FULLNAME.q().toBean())
                                .midPointAttribute(UserType.F_FULL_NAME.toBean()))
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(Account.AttributeNames.EMAIL.q().toBean())
                                .midPointAttribute(UserType.F_EMAIL_ADDRESS.toBean())) // to confuse the test
                        .attributeMatch(new SiAttributeMatchSuggestionType()
                                .applicationAttribute(ICFS_NAME.toBean())
                                .midPointAttribute(UserType.F_NAME.toBean())));
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting correlation rules");
        var suggestedCorrelation = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY_FOR_SUGGEST_MAPPINGS_AND_CORRELATION.oid,
                ACCOUNT_DEFAULT,
                null, task, result);

        then("suggestion is correct");
        displayValueAsXml("suggested correlation", suggestedCorrelation);
        var attrMappings = suggestedCorrelation.getAttributes();
        assertThat(attrMappings).as("attribute mappings").hasSize(1);
        assertCorrAttrSuggestion(attrMappings, ICFS_NAME, UserType.F_NAME);

        // There should be only one correlator, although there are two candidates (name and emailAddress).
        var correlation = suggestedCorrelation.getCorrelation();
        assertThat(correlation).as("correlation definition").isNotNull();
        CompositeCorrelatorType correlators = correlation.getCorrelators();
        assertThat(correlators).as("correlators").isNotNull();
        assertThat(correlators.asPrismContainerValue().getItems()).as("correlators items").hasSize(1);
        assertThat(correlators.getItems()).as("items correlators definitions").hasSize(1);
        var itemsCorrelator = correlators.getItems().get(0);
        assertThat(itemsCorrelator.getItem()).as("items correlators items").hasSize(1);
        var itemCorrelatorRef = itemsCorrelator.getItem().get(0).getRef();
        assertThat(itemCorrelatorRef).as("item correlators item ref").isNotNull();
        assertThat(itemCorrelatorRef.getItemPath().asSingleName()).as("correlator item").isEqualTo(UserType.F_NAME);
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
    }

    private @NotNull ResourceAttributeDefinitionType findAttributeDefinition(
            List<ResourceAttributeDefinitionType> definitions, ItemPath path) {
        return definitions.stream()
                .filter(s -> path.equivalent(s.getRef().getItemPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No definition found for " + path));
    }
}
