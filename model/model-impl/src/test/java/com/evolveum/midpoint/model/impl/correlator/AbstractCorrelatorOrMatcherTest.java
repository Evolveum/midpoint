/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;

import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.impl.correlator.match.TestingAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

/**
 * Isolated testing of the correlators or matchers.
 *
 * Correlator tests check individual implementations of the {@link Correlator} interface.
 * Matcher tests check implementations of {@link IdMatchService} interface.
 *
 * The tests are based on {@link #FILE_ACCOUNTS} with source data plus expected correlation results
 * (including operator responses in case of uncertainty).
 *
 * TODO Only the matcher tests are ready - for the correlators we might need to enhance the file structure.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractCorrelatorOrMatcherTest extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator");

    // Note that name/uid attribute must be a pure number, as we sort retrieved accounts on it.
    public static final String ATTR_GIVEN_NAME = "givenName";
    public static final String ATTR_FAMILY_NAME = "familyName";
    public static final String ATTR_DATE_OF_BIRTH = "dateOfBirth";
    public static final String ATTR_NATIONAL_ID = "nationalId";
    private static final String ATTR_TEST = "test"; // used for the test itself
    public static final ItemName ATTR_TEST_QNAME = new ItemName(NS_RI, ATTR_TEST);

    private static final DummyTestResource RESOURCE_FUZZY = new DummyTestResource(TEST_DIR, "resource-dummy-fuzzy.xml",
            "12c070d2-c4f5-451f-942d-11675920fdd7", "fuzzy", controller -> {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_GIVEN_NAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_FAMILY_NAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_DATE_OF_BIRTH, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_NATIONAL_ID, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_TEST, String.class, false, false);
    });

    /**
     * The file contains 5 data columns (`uid`, `givenName`, `familyName`, `dateOfBirth`, and `nationalId`.
     * Note that `uid` has to be a pure integer. The accounts are processed in the order of their `uid`.
     *
     * The `test` column describes the expected identity of the account:
     *
     * - `new` means a new identity should be assigned (e.g. new user in higher-level tests, or
     * new reference ID in lower-level ones)
     * - `=N` means the account corresponds to the same identity as previously processed account with uid `N`;
     * - `?A,B,C:N` means that the correlator/matcher should provide options corresponding to accounts with `uid`
     * values of `A`, `B`, `C` (can be any positive number of them) and the operator will select the account
     * with `uid` of `N`
     * - `?A,B,C:new` means the same, but the operator will select "new person" as the response
     *
     * The automated tests processes these accounts and check if the correlator or matcher acts accordingly.
     */
    private static final File FILE_ACCOUNTS = new File(TEST_DIR, "accounts.csv");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_FUZZY, initTask, initResult);
        addAccountsFromCsvFile();
    }

    /**
     * To provide correct definitions, and easy access to {@link ShadowType} objects, the accounts from {@link #FILE_ACCOUNTS}
     * are first imported into {@link #RESOURCE_FUZZY}. They are then read from that
     * by {@link #getAllAccounts(Task, OperationResult)} method.
     */
    private void addAccountsFromCsvFile()
            throws IOException, ConflictException, SchemaViolationException, InterruptedException, ObjectAlreadyExistsException {
        CSVParser parser = CSVParser.parse(
                FILE_ACCOUNTS,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withDelimiter('|')
                        .withIgnoreSurroundingSpaces());
        List<String> headerNames = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();
        for (CSVRecord record : records) {
            String userId = record.get(0);
            DummyAccount account = RESOURCE_FUZZY.controller.addAccount(userId);
            for (int column = 1; column < record.size(); column++) {
                String headerName = headerNames.get(column);
                if (!headerName.startsWith("_")) {
                    account.addAttributeValue(headerName, record.get(column));
                }
            }
            displayDumpable("account", account);
        }
    }

    /**
     * Returns accounts sorted by uid (interpreted as integer).
     */
    protected List<TestingAccount> getAllAccounts(Task task, OperationResult result)
            throws CommonException {

        List<PrismObject<ShadowType>> resourceAccounts = provisioningService.searchObjects(
                ShadowType.class,
                ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_FUZZY.oid, RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME),
                null,
                task, result);

        return resourceAccounts.stream()
                .map(TestingAccount::new)
                .sorted(Comparator.comparing(TestingAccount::getNumber))
                .collect(Collectors.toList());
    }
}
