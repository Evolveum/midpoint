/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.impl.correlator.correlation.CorrelationTestingAccount;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest.RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME;

public class CorrelatorTestUtil {

    /**
     * To provide correct definitions, and easy access to {@link ShadowType} objects, the accounts from `accountsFile`
     * are first imported into `resource`. They are then read from that
     * by {@link #getAllAccounts(AbstractModelIntegrationTest, DummyTestResource, Function, Task, OperationResult)} method.
     */
    public static void addAccountsFromCsvFile(AbstractIntegrationTest test, File accountsFile, DummyTestResource resource)
            throws IOException, ConflictException, SchemaViolationException, InterruptedException, ObjectAlreadyExistsException,
            ObjectDoesNotExistException {
        CSVParser parser = CSVParser.parse(
                accountsFile,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT
                        .withCommentMarker('#')
                        .withFirstRecordAsHeader()
                        .withDelimiter('|')
                        .withIgnoreSurroundingSpaces());
        List<String> headerNames = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();
        for (CSVRecord record : records) {
            String userId = record.get(0);
            DummyAccount account = resource.controller.addAccount(userId);
            for (int column = 1; column < record.size(); column++) {
                String headerName = headerNames.get(column);
                if (!headerName.startsWith("_")) {
                    String value = record.get(column);
                    if (StringUtils.isNotBlank(value)) {
                        account.addAttributeValue(headerName, value);
                    } else {
                        // skipping empty strings (shadow caching on Oracle cannot store empty values)
                    }
                }
            }
            test.displayDumpable("account", account);
        }
    }

    /**
     * Returns accounts sorted by uid (interpreted as integer).
     */
    public static <TA extends TestingAccount> List<TA> getAllAccounts(
            AbstractModelIntegrationTest test, DummyTestResource resource, Function<PrismObject<ShadowType>, TA> creator,
            Task task, OperationResult result)
            throws CommonException {

        List<PrismObject<ShadowType>> resourceAccounts = test.getProvisioningService().searchObjects(
                ShadowType.class,
                ObjectQueryUtil.createResourceAndObjectClassQuery(resource.oid, RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME),
                null,
                task, result);

        return resourceAccounts.stream()
                .map(creator)
                .sorted(Comparator.comparing(TestingAccount::getNumber))
                .collect(Collectors.toList());
    }

    public static @NotNull TestingAccount findAccount(Collection<? extends TestingAccount> accounts, int number) {
        return accounts.stream()
                .filter(a -> a.getNumber() == number)
                .findAny()
                .orElseThrow(() -> new AssertionError("No account #" + number + " found"));
    }

    /**
     * Note: not all attributes are needed in all testing scenarios.
     */
    public static void createAttributeDefinitions(DummyResourceContoller controller)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_CORRELATOR, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_EMPLOYEE_NUMBER, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_COST_CENTER, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_GIVEN_NAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_FAMILY_NAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_HONORIFIC_PREFIX, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_DATE_OF_BIRTH, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_NATIONAL_ID, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                TestingAccount.ATTR_TEST, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                CorrelationTestingAccount.ATTR_EXP_CANDIDATES, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                CorrelationTestingAccount.ATTR_EXP_RESULT, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                CorrelationTestingAccount.ATTR_EXP_MATCH, String.class, false, false);
    }
}
