/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.model.impl.correlator.TestingAccount;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
public class CorrelationServiceTest extends AbstractEmptyInternalModelTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR,
            "correlator/correlation/task");
    private static final DummyTestResource DUMMY_RESOURCE = new DummyTestResource(
            TEST_DIR, "dummy-resource.xml", "4a7f6b3e-64cc-4cd9-b5ba-64ecc47d7d10", "correlation",
            CorrelatorTestUtil::createAttributeDefinitions);
    private static final File USERS = new File(TEST_DIR, "users.xml");
    private static final File ACCOUNT = new File(TEST_DIR, "account.csv");
    private static final File CORRELATOR = new File(TEST_DIR, "item-correlator.xml");

    @Autowired
    private CorrelationService correlationService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(DUMMY_RESOURCE, initTask, initResult);
    }

    @Test
    void ShadowHasOneFocusCounterpart_correlateShadow_focusShouldBeInCandidateOwners()
            throws ConflictException, ObjectDoesNotExistException, IOException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException, CommonException {
        final Task task = getTestTask();
        final OperationResult result = getTestOperationResult();

        given("Resource contains account.");
        DUMMY_RESOURCE.controller.getDummyResource().clear();
        CorrelatorTestUtil.addAccountsFromCsvFile(this, ACCOUNT, DUMMY_RESOURCE);
        final List<TestingAccount> allAccounts = CorrelatorTestUtil.getAllAccounts(this, DUMMY_RESOURCE,
                TestingAccount::new, task, result);

        and("Users matching correlation rule exists.");
        importObjectsFromFileNotRaw(USERS, task, result);

        when("Correlation with particular definition is run on the account's shadow.");
        final ItemsSubCorrelatorType correlator = this.prismContext.parserFor(CORRELATOR)
                .parseRealValue(ItemsSubCorrelatorType.class);
        final CorrelationDefinitionType correlationDefinition = new CorrelationDefinitionType().correlators(
                new CompositeCorrelatorType().items(correlator));

        final CompleteCorrelationResult correlationResult = this.correlationService.correlate(
                allAccounts.get(0).getShadow(), correlationDefinition, task, result);

        then("User should be correlated as shadow's candidate owner.");
        final List<UserType> candidates = correlationResult.getAllCandidates(UserType.class);
        assertEquals(candidates.size(), 1);
        assertEquals(candidates.get(0).getName().getOrig(), "smith1");
    }
}
