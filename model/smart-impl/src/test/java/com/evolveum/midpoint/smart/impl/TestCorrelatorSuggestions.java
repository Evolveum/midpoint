package com.evolveum.midpoint.smart.impl;


import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.smart.impl.DummyScenario.Account.AttributeNames.*;
import static com.evolveum.midpoint.smart.impl.DummyScenario.on;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelatorSuggestions extends AbstractSmartIntegrationTest {
    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart/correlator-evaluator");

    private static DummyScenario dummyScenario;

    private static final TestObject<UserType> USER1 = TestObject.file(TEST_DIR, "user1.xml", "00000000-0000-0000-0000-990000000001");
    private static final TestObject<UserType> USER2 = TestObject.file(TEST_DIR, "user2.xml", "00000000-0000-0000-0000-990000000002");
    private static final TestObject<UserType> USER3 = TestObject.file(TEST_DIR, "user3.xml", "00000000-0000-0000-0000-990000000003");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-correlator-evaluator.xml", "10000000-0000-0000-0000-990000000001",
                    "for-correlator-evaluator", c -> dummyScenario = on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult, CommonInitialObjects.SERVICE_ORIGIN_ARTIFICIAL_INTELLIGENCE);

        initAndTestDummyResource(RESOURCE_DUMMY, initTask, initResult);

        // Add dummy accounts with correlatable attributes
        // User1 <-> account1, User2 <-> account2, User3 <-> account3
        dummyScenario.getController().addAccount("account1")
                .addAttributeValues(PERSONAL_NUMBER.local(), "11111")
                .addAttributeValues(EMAIL.local(), "account1@acme.com");
        dummyScenario.getController().addAccount("account2")
                .addAttributeValues(PERSONAL_NUMBER.local(), "22222")
                .addAttributeValues(EMAIL.local(), "account2@acme.com");
        dummyScenario.getController().addAccount("account3")
                .addAttributeValues(PERSONAL_NUMBER.local(), "33333")
                .addAttributeValues(EMAIL.local(), "account3@acme.com");

        initTestObjects(initTask, initResult, USER1, USER2, USER3);
        createAndLinkAccounts(initTask, initResult);
    }

    private void createAndLinkAccounts(Task initTask, OperationResult initResult) throws Exception {
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
        Collection<DummyAccount> cc = RESOURCE_DUMMY.getDummyResource().listAccounts();
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

    @Test
    public void test010UniquePersonalNumberCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = PERSONAL_NUMBER.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        List<CorrelationSuggestionOperation.CorrelatorSuggestion> suggestions = List.of(suggestion);

        var ctx = TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, task, result);
        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, suggestions);
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for unique personalNumber correlation should be high (>0.95)")
                .isGreaterThan(0.95);
    }

}
