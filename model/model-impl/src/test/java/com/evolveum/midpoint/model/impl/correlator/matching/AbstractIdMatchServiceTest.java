/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.matching;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.util.SchemaTestConstants.ICFS_UID;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;

import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.api.correlator.idmatch.MatchingResult;
import com.evolveum.midpoint.model.api.correlator.idmatch.PotentialMatch;
import com.evolveum.midpoint.model.impl.correlator.matching.ExpectedMatchingResult.UncertainWithResolution;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

/**
 * Isolated testing of the ID Match service: either "real" or the dummy one.
 *
 * The tests are based on {@link #FILE_ACCOUNTS} with source data plus expected matching results
 * (including operator responses in case of uncertainty).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIdMatchServiceTest extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/matching");

    private static final DummyTestResource RESOURCE_FUZZY = new DummyTestResource(
            TEST_DIR, "resource-dummy-matching.xml",
            "12c070d2-c4f5-451f-942d-11675920fdd7", "matching", CorrelatorTestUtil::createAttributeDefinitions);

    /**
     * The automated tests processes these accounts and check if the correlator or matcher acts accordingly.
     * Please see comments in the file itself.
     */
    private static final File FILE_ACCOUNTS = new File(TEST_DIR, "accounts.csv");

    /** Service that we want to test (real or dummy one). */
    private IdMatchService service;

    /** Accounts that we want to push to the service. Taken from {@link #FILE_ACCOUNTS}. */
    private List<MatchingTestingAccount> accounts;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_FUZZY, initTask, initResult);
        addAccountsFromCsvFile();

        service = createService();
        accounts = getAllAccounts(initTask, initResult);
    }

    /**
     * To provide correct definitions, and easy access to {@link ShadowType} objects, the accounts from {@link #FILE_ACCOUNTS}
     * are first imported into {@link #RESOURCE_FUZZY}. They are then read from that
     * by {@link #getAllAccounts(Task, OperationResult)} method.
     */
    private void addAccountsFromCsvFile()
            throws IOException, ConflictException, SchemaViolationException, InterruptedException, ObjectAlreadyExistsException {
        CorrelatorTestUtil.addAccountsFromCsvFile(this, FILE_ACCOUNTS, RESOURCE_FUZZY);
    }

    /**
     * Returns accounts sorted by uid (interpreted as integer).
     */
    private List<MatchingTestingAccount> getAllAccounts(Task task, OperationResult result)
            throws CommonException {
        return CorrelatorTestUtil.getAllAccounts(this, RESOURCE_FUZZY, MatchingTestingAccount::new, task, result);
    }

    /** Creates {@link IdMatchService} instance that is to be tested. */
    abstract protected IdMatchService createService();

    /**
     * Sequentially processes all accounts, pushing them to matcher and checking its response.
     */
    @Test
    public void test100ProcessAccounts() throws SchemaException, CommunicationException {
        for (int i = 0; i < accounts.size(); i++) {
            processAccount(i);
        }
    }

    private void processAccount(int i) throws SchemaException, CommunicationException {
        //Here we can set account from csv


        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("account #" + (i+1));

        // 1. Push the account to the matcher

        MatchingTestingAccount account = accounts.get(i);
        ShadowAttributesType attributes = account.getAttributes();
        displayDumpable("Account attributes", attributes);

        MatchingResult matchingResult = service.executeMatch(attributes, result);

        then("account #" + (i+1));

        // 2. Assert that the result is correct (including operator response when needed)

        processMatchingResult(i, matchingResult);
    }

    private void processMatchingResult(int i, MatchingResult matchingResult) throws SchemaException, CommunicationException {
        MatchingTestingAccount account = accounts.get(i);
        ExpectedMatchingResult expectedResult = account.getExpectedMatchingResult();
        displayDumpable("Matching result obtained", matchingResult);
        displayValue("Expected result", expectedResult);
        if (expectedResult.isNew()) {
            assertNewIdentifier(matchingResult.getReferenceId());
            account.setReferenceId(matchingResult.getReferenceId());
        } else if (expectedResult.isEqualTo()) {
            assertExistingIdentifier(matchingResult.getReferenceId(), expectedResult.getEqualTo());
            account.setReferenceId(matchingResult.getReferenceId());
        } else if (expectedResult.isUncertain()) {
            processUncertainAnswer(i, matchingResult, expectedResult.uncertainWithResolution);
            // reference ID is set in the method above
        }
    }

    private void assertNewIdentifier(String referenceId) {
        assertThat(referenceId).as("reference ID").isNotNull();
        List<MatchingTestingAccount> accountsWithReferenceId = accounts.stream()
                .filter(a -> referenceId.equals(a.getReferenceId()))
                .collect(Collectors.toList());
        assertThat(accountsWithReferenceId).as("accounts with presumably new reference ID").isEmpty();
    }

    private void assertExistingIdentifier(String referenceId, int accountId) {
        MatchingTestingAccount account = getAccountWithId(accountId);
        assertThat(referenceId)
                .withFailMessage(() -> "Expected reference ID matching account #" + accountId +
                        " (" + account.referenceId + "), got " + referenceId)
                .isEqualTo(account.referenceId);
    }

    private MatchingTestingAccount getAccountWithId(int accountId) {
        return accounts.stream()
                .filter(a -> a.getNumber() == accountId)
                .findFirst().orElseThrow(() -> new AssertionError("No account with ID " + accountId));
    }

    /**
     * Checks the "uncertain" response, and invokes required operation reaction.
     * Then checks the effect of that reaction.
     *
     * @param i the number of the account
     * @param matchingResult Result containing the uncertainty (and potential matches)
     * @param uncertainWithResolution Expected uncertain result plus resolution that should be provided (from accounts file)
     */
    private void processUncertainAnswer(int i, MatchingResult matchingResult, UncertainWithResolution uncertainWithResolution) throws CommunicationException, SchemaException {
        OperationResult result = getTestOperationResult();

        checkUncertainAnswer(matchingResult, uncertainWithResolution);
        Integer operatorResponse = sendOperatorResponse(i, matchingResult, uncertainWithResolution, result);
        String referenceIdAfterRetry = checkResponseApplied(i, operatorResponse, result);

        // Remember acquired reference ID
        MatchingTestingAccount account = accounts.get(i);
        account.setReferenceId(referenceIdAfterRetry);
        displayDumpable("Updated account after resolution", account);
    }

    private void checkUncertainAnswer(MatchingResult matchingResult, UncertainWithResolution uncertainWithResolution) {
        assertThat(matchingResult.getReferenceId()).as("reference ID returned").isNull();
        Set<Integer> receivedUidValues = matchingResult.getPotentialMatches().stream()
                .map(this::getUid)
                .collect(Collectors.toSet());
        assertThat(receivedUidValues)
                .as("UIDs of potential matches")
                .hasSameElementsAs(uncertainWithResolution.getOptions());
    }

    private Integer getUid(PotentialMatch potentialMatch) {
        PrismProperty<?> attribute = potentialMatch.getAttributes().asPrismContainerValue().findProperty(ICFS_UID);
        return attribute != null ?
                Integer.valueOf(attribute.getRealValue(String.class)) :
                null;
    }

    @Nullable
    private Integer sendOperatorResponse(int i, MatchingResult matchingResult, UncertainWithResolution uncertainWithResolution,
            OperationResult result) throws CommunicationException, SchemaException {
        String resolvedId;
        Integer operatorResponse = uncertainWithResolution.getOperatorResponse();
        if (operatorResponse == null) {
            resolvedId = null;
        } else {
            MatchingTestingAccount designatedAccount = getAccountWithId(operatorResponse);
            resolvedId = designatedAccount.referenceId;
            assertThat(resolvedId)
                    .withFailMessage(() -> "No reference ID in account pointed to by operator response: " + designatedAccount)
                    .isNotNull();
        }
        service.resolve(
                accounts.get(i).getAttributes(),
                matchingResult.getMatchRequestId(),
                resolvedId,
                result);
        return operatorResponse;
    }

    /**
     * Checks that the resolution was correctly processed - by retrying the query and checking the result.
     */
    @NotNull
    private String checkResponseApplied(int i, Integer operatorResponse, OperationResult result)
            throws SchemaException, CommunicationException {
        MatchingTestingAccount account = accounts.get(i);
        MatchingResult reMatchingResult = service.executeMatch(account.getAttributes(), result);
        displayDumpable("Matching result after operator decision", reMatchingResult);

        String referenceIdAfterRetry = reMatchingResult.getReferenceId();
        assertThat(referenceIdAfterRetry).as("reference ID after retry").isNotNull();

        if (operatorResponse == null) {
            assertNewIdentifier(referenceIdAfterRetry);
        } else {
            assertExistingIdentifier(referenceIdAfterRetry, operatorResponse);
        }
        return referenceIdAfterRetry;
    }
}
