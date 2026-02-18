/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.smart.impl.DummyScenario.Account.AttributeNames.EMAIL;
import static com.evolveum.midpoint.smart.impl.DummyScenario.Account.AttributeNames.PERSONAL_NUMBER;
import static com.evolveum.midpoint.smart.impl.DummyScenario.on;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.setExtensionPropertyRealValues;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.activities.ObjectClassStatisticsComputer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Date;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

@ContextConfiguration(locations = {"classpath:ctx-smart-integration-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestObjectTypesSuggestionOperation extends AbstractSmartIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart/object-types-suggestion");

    private static DummyScenario dummyScenario;

    private static final TestObject<UserType> USER1 =
            TestObject.file(TEST_DIR, "user1.xml", "00000000-0000-0000-0000-999000001001");
    private static final TestObject<UserType> USER2 =
            TestObject.file(TEST_DIR, "user2.xml", "00000000-0000-0000-0000-999000001002");
    private static final TestObject<UserType> USER3 =
            TestObject.file(TEST_DIR, "user3.xml", "00000000-0000-0000-0000-999000001003");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-object-types-suggestion.xml", "10000000-0000-0000-0000-999000000002",
            "for-object-types-suggestion", c -> dummyScenario = on(c).initialize());

    @Autowired private ObjectTypesSuggestionOperationFactory objectTypesSuggestionOperationFactory;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, CommonInitialObjects.SERVICE_ORIGIN_ARTIFICIAL_INTELLIGENCE);
        initAndTestDummyResource(RESOURCE_DUMMY, initTask, initResult);

        initTestObjects(initTask, initResult, USER1, USER2, USER3);

        var a = dummyScenario.account;
        a.add("user1")
                .addAttributeValues(PERSONAL_NUMBER.local(), "11111")
                .addAttributeValues(EMAIL.local(), "user1@acme.com");
        linkAccount(USER1, initTask, initResult);
        a.add("user2")
                .addAttributeValues(PERSONAL_NUMBER.local(), "22222")
                .addAttributeValues(EMAIL.local(), "user2@acme.com");
        linkAccount(USER2, initTask, initResult);
        a.add("user3")
                .addAttributeValues(PERSONAL_NUMBER.local(), "33333")
                .addAttributeValues(EMAIL.local(), "user3@acme.com");
        linkAccount(USER3, initTask, initResult);
    }

    private void refreshShadows() throws Exception {
        provisioningService.searchShadows(
                Resource.of(RESOURCE_DUMMY.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null, getTestTask(), getTestOperationResult());
    }

    private void linkAccount(TestObject<?> user, Task task, OperationResult result) throws CommonException, IOException {
        var shadow = findShadowRequest()
                .withResource(RESOURCE_DUMMY.getObjectable())
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

    private ServiceClient createClient(SiSuggestedObjectTypeType... objectTypes) {
        SiSuggestObjectTypesResponseType response = new SiSuggestObjectTypesResponseType();
        for (SiSuggestedObjectTypeType objectType : objectTypes) {
            response.objectType(objectType);
        }
        return new MockServiceClientImpl(response);
    }

    private ServiceClient createClientWithResponses(Object... responses) {
        return new MockServiceClientImpl(responses);
    }

    private ShadowObjectClassStatisticsType computeStatistics(QName objectClassName, Task task, OperationResult result)
            throws CommonException {
        var resource = Resource.of(RESOURCE_DUMMY.get());
        var accountDef = resource
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(objectClassName);
        var computer = new ObjectClassStatisticsComputer(accountDef);
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

    @Test
    public void test001BasicSuggestionCreatesDelineation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();
        var mockClient = createClient(
                new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("employee")
                        .filter("attributes/type = 'employee'")
                        .baseContextObjectClassName("organizationalUnit")
                        .baseContextFilter("attributes/cn = 'evolveum'")
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);
        var op = objectTypesSuggestionOperationFactory.create(
                mockClient, RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, task, result);
        ObjectTypesSuggestionType suggestion = op.suggestObjectTypes(statistics, result);

        assertThat(suggestion.getObjectType()).hasSize(1);
        ResourceObjectTypeDefinitionType objectType = suggestion.getObjectType().get(0);
        assertThat(objectType.getKind()).isEqualTo(ShadowKindType.ACCOUNT);
        assertThat(objectType.getIntent()).isEqualTo("employee");

        var delineation = objectType.getDelineation();
        assertThat(delineation).isNotNull();
        assertThat(delineation.getFilter()).hasSize(1);
        assertThat(delineation.getFilter().get(0)).isNotNull();

        assertThat(delineation.getBaseContext()).isNotNull();
        assertThat(delineation.getBaseContext().getObjectClass())
                .isEqualTo(new QName(
                        com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI,
                        "organizationalUnit"));
        assertThat(delineation.getBaseContext().getFilter()).isNotNull();
    }

    @Test
    public void test010MultipleObjectTypes() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();
        var mockClient = createClient(
                new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("employee")
                        .filter("attributes/type = 'employee'"),
                new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("contractor")
                        .filter("attributes/type = 'contractor'")
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);
        var op = objectTypesSuggestionOperationFactory.create(
                mockClient, RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, task, result);
        var suggestion = op.suggestObjectTypes(statistics, result);

        assertThat(suggestion.getObjectType()).hasSize(2);
        assertThat(suggestion.getObjectType())
                .anySatisfy(t -> {
                    assertThat(t.getIntent()).isEqualTo("employee");
                    assertThat(t.getDelineation().getFilter()).hasSize(1);
                })
                .anySatisfy(t -> {
                    assertThat(t.getIntent()).isEqualTo("contractor");
                    assertThat(t.getDelineation().getFilter()).hasSize(1);
                });
    }

    @Test
    public void test020DuplicateTypeIdsIgnored() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();
        // Two identical suggestions (same kind+intent) => second should be ignored
        var mockClient = createClient(
                new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("employee")
                        .filter("attributes/type = 'employee'"),
                new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("employee")
                        .filter("attributes/type = 'employee'")
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);
        var op = objectTypesSuggestionOperationFactory.create(
                mockClient, RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, task, result);
        var suggestion = op.suggestObjectTypes(statistics, result);

        assertThat(suggestion.getObjectType()).hasSize(1);
        assertThat(suggestion.getObjectType().get(0).getIntent()).isEqualTo("employee");
    }

    @Test
    public void test030InvalidFilterWithRetryProducesValidResult() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        // First response contains invalid filter (parser should fail), second response corrects it
        var first = new SiSuggestObjectTypesResponseType()
                .objectType(new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("employee")
                        .filter("attributes/type = 'employee' INVALID"));
        var second = new SiSuggestObjectTypesResponseType()
                .objectType(new SiSuggestedObjectTypeType()
                        .kind("account")
                        .intent("employee")
                        .filter("attributes/type = 'employee'"));
        var mockClient = createClientWithResponses(first, second);

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);
        var op = objectTypesSuggestionOperationFactory.create(
                mockClient, RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, task, result);
        var suggestion = op.suggestObjectTypes(statistics, result);

        assertThat(suggestion.getObjectType()).hasSize(1);
        var t = suggestion.getObjectType().get(0);
        assertThat(t.getIntent()).isEqualTo("employee");
        assertThat(t.getDelineation().getFilter()).hasSize(1);
    }

    @Test
    public void test100StatisticsTTL_ExpiredStatisticsAreDeleted() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var expiredTimestamp = XmlTypeConverter.createXMLGregorianCalendar(
                new Date(System.currentTimeMillis() - 25 * 60 * 60 * 1000));
        var statistics = new ShadowObjectClassStatisticsType()
                .timestamp(expiredTimestamp)
                .size(100)
                .coverage(1.0f);

        var statisticsObject = new GenericObjectType()
                .name("Expired Statistics");
        var holderPcv = statisticsObject.asPrismContainerValue();
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_STATISTICS, statistics);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, RESOURCE_DUMMY.oid);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME, "account");

        String oid = repositoryService.addObject(statisticsObject.asPrismObject(), null, result);
        assertThat(oid).isNotNull();

        var retrieved = smartIntegrationService.getLatestStatistics(
                RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, result);

        assertThat(retrieved).isNull();
        assertNoRepoObject(GenericObjectType.class, oid);
    }

    @Test
    public void test101StatisticsTTL_FreshStatisticsAreRetained() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var freshTimestamp = XmlTypeConverter.createXMLGregorianCalendar(
                new Date(System.currentTimeMillis() - 1 * 60 * 60 * 1000));
        var statistics = new ShadowObjectClassStatisticsType()
                .timestamp(freshTimestamp)
                .size(100)
                .coverage(1.0f);

        var statisticsObject = new GenericObjectType()
                .name("Fresh Statistics");
        var holderPcv = statisticsObject.asPrismContainerValue();
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_STATISTICS, statistics);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, RESOURCE_DUMMY.oid);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME, "account");

        String oid = repositoryService.addObject(statisticsObject.asPrismObject(), null, result);

        var retrieved = smartIntegrationService.getLatestStatistics(
                RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, result);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getOid()).isEqualTo(oid);
        var retrievedStats = ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(retrieved.asPrismObject());
        assertThat(retrievedStats.getSize()).isEqualTo(100);
    }

    @Test
    public void test120ManualDeletion_DeleteStatisticsForResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Create multiple statistics objects for the same resource
        var timestamp = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        for (int i = 0; i < 3; i++) {
            var statistics = new ShadowObjectClassStatisticsType()
                    .timestamp(timestamp)
                    .size(100 + i)
                    .coverage(1.0f);

            var statisticsObject = new GenericObjectType()
                    .name("Statistics " + i);
            var holderPcv = statisticsObject.asPrismContainerValue();
            setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_STATISTICS, statistics);
            setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, RESOURCE_DUMMY.oid);
            setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME, "account");

            repositoryService.addObject(statisticsObject.asPrismObject(), null, result);
        }

        // Verify statistics exist
        var before = smartIntegrationService.getLatestStatistics(
                RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, result);
        assertThat(before).isNotNull();

        // Delete all statistics for this resource and object class
        smartIntegrationService.deleteStatisticsForResource(
                RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, result);

        // Verify all statistics were deleted
        var after = smartIntegrationService.getLatestStatistics(
                RESOURCE_DUMMY.oid, OC_ACCOUNT_QNAME, result);
        assertThat(after).isNull();
    }

    @Test
    public void test130Filter_OnlyObjectsWithStatisticsAreConsidered() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var timestamp = XmlTypeConverter.createXMLGregorianCalendar(new Date());

        // Create an object WITHOUT statistics extension
        var objectWithoutStats = new GenericObjectType()
                .name("Object Without Statistics");
        var pcvWithoutStats = objectWithoutStats.asPrismContainerValue();
        setExtensionPropertyRealValues(pcvWithoutStats, MODEL_EXTENSION_RESOURCE_OID, RESOURCE_DUMMY.oid);
        setExtensionPropertyRealValues(pcvWithoutStats, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME, "group");

        repositoryService.addObject(objectWithoutStats.asPrismObject(), null, result);

        // Create an object WITH statistics extension
        var statistics = new ShadowObjectClassStatisticsType()
                .timestamp(timestamp)
                .size(200)
                .coverage(1.0f);

        var objectWithStats = new GenericObjectType()
                .name("Object With Statistics");
        var pcvWithStats = objectWithStats.asPrismContainerValue();
        setExtensionPropertyRealValues(pcvWithStats, MODEL_EXTENSION_STATISTICS, statistics);
        setExtensionPropertyRealValues(pcvWithStats, MODEL_EXTENSION_RESOURCE_OID, RESOURCE_DUMMY.oid);
        setExtensionPropertyRealValues(pcvWithStats, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME, "group");

        String oidWithStats = repositoryService.addObject(objectWithStats.asPrismObject(), null, result);

        // Retrieve statistics - should only return the object WITH statistics
        var retrieved = smartIntegrationService.getLatestStatistics(
                RESOURCE_DUMMY.oid, new QName(NS_RI, "group"), result);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getOid()).isEqualTo(oidWithStats);
        var retrievedStats = ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(retrieved.asPrismObject());
        assertThat(retrievedStats.getSize()).isEqualTo(200);
    }

    @Test
    public void test200StatisticsComputer_ValueCountsWithRepeats() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user1").addAttributeValues("department", "IT");
        a.add("stat-user2").addAttributeValues("department", "IT");
        a.add("stat-user3").addAttributeValues("department", "IT");
        a.add("stat-user4").addAttributeValues("department", "HR");
        a.add("stat-user5").addAttributeValues("department", "HR");
        a.add("stat-user6").addAttributeValues("department", "Sales");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        assertThat(statistics.getSize()).isGreaterThanOrEqualTo(6);

        var deptStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("department"))
                .findFirst();

        assertThat(deptStats).isPresent();
        var stats = deptStats.get();

        assertThat(stats.getUniqueValueCount()).isEqualTo(3);

        var valueCounts = stats.getValueCount();
        assertThat(valueCounts).hasSize(3);
        assertThat(valueCounts)
                .anySatisfy(vc -> {
                    assertThat(vc.getValue()).isEqualTo("IT");
                    assertThat(vc.getCount()).isEqualTo(3);
                })
                .anySatisfy(vc -> {
                    assertThat(vc.getValue()).isEqualTo("HR");
                    assertThat(vc.getCount()).isEqualTo(2);
                })
                .anySatisfy(vc -> {
                    assertThat(vc.getValue()).isEqualTo("Sales");
                    assertThat(vc.getCount()).isEqualTo(1);
                });
    }

    @Test
    public void test201StatisticsComputer_ValueCountsMinRepeatFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user10").addAttributeValues("cn", "A");
        a.add("stat-user11").addAttributeValues("cn", "B");
        a.add("stat-user12").addAttributeValues("cn", "C");
        a.add("stat-user13").addAttributeValues("cn", "D");
        a.add("stat-user14").addAttributeValues("cn", "E");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var cnStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("cn"))
                .findFirst();

        cnStats.ifPresent(shadowAttributeStatisticsType -> assertThat(shadowAttributeStatisticsType.getValueCount()).isEmpty());
    }

    @Test
    public void test202StatisticsComputer_ValueCountsTopNLimit() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user-topn-0").addAttributeValues("type", "A");
        a.add("stat-user-topn-1").addAttributeValues("type", "A");
        a.add("stat-user-topn-2").addAttributeValues("type", "A");
        a.add("stat-user-topn-3").addAttributeValues("type", "B");
        a.add("stat-user-topn-4").addAttributeValues("type", "B");
        a.add("stat-user-topn-5").addAttributeValues("type", "C");
        a.add("stat-user-topn-6").addAttributeValues("type", "C");
        a.add("stat-user-topn-7").addAttributeValues("type", "unique1");
        a.add("stat-user-topn-8").addAttributeValues("type", "unique2");
        a.add("stat-user-topn-9").addAttributeValues("type", "unique3");
        a.add("stat-user-topn-10").addAttributeValues("type", "unique4");
        a.add("stat-user-topn-11").addAttributeValues("type", "unique5");
        a.add("stat-user-topn-12").addAttributeValues("type", "unique6");
        a.add("stat-user-topn-13").addAttributeValues("type", "unique7");
        a.add("stat-user-topn-14").addAttributeValues("type", "unique8");
        a.add("stat-user-topn-15").addAttributeValues("type", "unique9");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var typeStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("type"))
                .findFirst();

        assertThat(typeStats).isPresent();
        var stats = typeStats.get();

        assertThat(stats.getUniqueValueCount()).isEqualTo(12);
        assertThat(stats.getValueCount()).hasSize(3);
        assertThat(stats.getValueCount()).allSatisfy(vc ->
                assertThat(vc.getCount()).isGreaterThanOrEqualTo(2));
    }

    @Test
    public void test203StatisticsComputer_FirstTokenPattern() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user-token1").addAttributeValues("cn", "prod-server01");
        a.add("stat-user-token2").addAttributeValues("cn", "prod-server02");
        a.add("stat-user-token3").addAttributeValues("cn", "prod-app01");
        a.add("stat-user-token4").addAttributeValues("cn", "dev-server01");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var cnStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("cn"))
                .findFirst();

        assertThat(cnStats).isPresent();
        var stats = cnStats.get();

        var firstTokenPatterns = stats.getValuePatternCount().stream()
                .filter(vpc -> vpc.getType() == ShadowValuePatternType.FIRST_TOKEN)
                .toList();

        assertThat(firstTokenPatterns).isNotEmpty();
        assertThat(firstTokenPatterns)
                .anySatisfy(vpc -> {
                    assertThat(vpc.getValue()).isEqualTo("prod-");
                    assertThat(vpc.getCount()).isEqualTo(3);
                })
                .anySatisfy(vpc -> {
                    assertThat(vpc.getValue()).isEqualTo("dev-");
                    assertThat(vpc.getCount()).isEqualTo(1);
                });
    }

    @Test
    public void test204StatisticsComputer_LastTokenPattern() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user-last1").addAttributeValues("description", "user-admin");
        a.add("stat-user-last2").addAttributeValues("description", "service-admin");
        a.add("stat-user-last3").addAttributeValues("description", "app-admin");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var descStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("description"))
                .findFirst();

        assertThat(descStats).isPresent();
        var stats = descStats.get();

        var lastTokenPatterns = stats.getValuePatternCount().stream()
                .filter(vpc -> vpc.getType() == ShadowValuePatternType.LAST_TOKEN)
                .toList();

        assertThat(lastTokenPatterns).isNotEmpty();
        assertThat(lastTokenPatterns)
                .anySatisfy(vpc -> {
                    assertThat(vpc.getValue()).isEqualTo("-admin");
                    assertThat(vpc.getCount()).isEqualTo(3);
                });
    }

    @Test
    public void test205StatisticsComputer_SkipUrl() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user-skip1").addAttributeValues("fullname", "http://example.com");
        a.add("stat-user-skip2").addAttributeValues("fullname", "http://example.com");
        a.add("stat-user-skip3").addAttributeValues("fullname", "https://example.com/path");
        a.add("stat-user-skip4").addAttributeValues("fullname", "https://example.com/path");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var fullnameStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("fullname"))
                .findFirst();

        assertThat(fullnameStats).isPresent();
        var stats = fullnameStats.get();

        assertThat(stats.getValueCount()).hasSize(2);

        var tokenPatterns = stats.getValuePatternCount().stream()
                .filter(vpc -> vpc.getType() == ShadowValuePatternType.FIRST_TOKEN
                        || vpc.getType() == ShadowValuePatternType.LAST_TOKEN)
                .toList();

        assertThat(tokenPatterns).isEmpty();
    }

    @Test
    public void test206StatisticsComputer_DnSuffixPattern() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var a = dummyScenario.account;
        a.add("stat-user-dn1").addAttributeValues("distinguishedName", "cn=user1,ou=users,dc=example,dc=com");
        a.add("stat-user-dn2").addAttributeValues("distinguishedName", "cn=user2,ou=users,dc=example,dc=com");
        a.add("stat-user-dn3").addAttributeValues("distinguishedName", "cn=user3,ou=users,dc=example,dc=com");

        var statistics = computeStatistics(OC_ACCOUNT_QNAME, task, result);

        var dnStats = statistics.getAttribute().stream()
                .filter(attr -> attr.getRef().getItemPath().lastName().getLocalPart().equals("distinguishedName"))
                .findFirst();

        assertThat(dnStats).isPresent();
        var stats = dnStats.get();

        var dnSuffixPatterns = stats.getValuePatternCount().stream()
                .filter(vpc -> vpc.getType() == ShadowValuePatternType.DN_SUFFIX)
                .toList();

        assertThat(dnSuffixPatterns).hasSize(1);
        assertThat(dnSuffixPatterns.get(0).getValue()).isEqualTo("ou=users,dc=example,dc=com");
        assertThat(dnSuffixPatterns.get(0).getCount()).isEqualTo(3);
    }

}
