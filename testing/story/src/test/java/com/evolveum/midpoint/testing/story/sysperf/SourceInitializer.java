/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.FileNotFoundException;
import java.net.ConnectException;

public class SourceInitializer {

    private final TestSystemPerformance test;
    private final DummyTestResource resource;
    private final SourceVariant sourceVariant;
    private final PopulationVariant populationVariant;
    private final Task initTask;

    private static final String ACCOUNT_NAME = "u-%08d";

    SourceInitializer(TestSystemPerformance test, DummyTestResource resource, SourceVariant sourceVariant,
            PopulationVariant populationVariant, Task initTask) {
        this.test = test;
        this.resource = resource;
        this.sourceVariant = sourceVariant;
        this.populationVariant = populationVariant;
        this.initTask = initTask;
    }

    public void run(OperationResult result) throws Exception {
        initializeResource(result);
        createAccounts();
    }

    private void initializeResource(OperationResult result) throws Exception {
        test.initDummyResource(resource, initTask, result);
    }

    private void createAccounts() throws ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException {
        for (int u = 0; u < populationVariant.getAccounts(); u++) {
            createAccount(u);
        }
    }

    private void createAccount(int u) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException,
            FileNotFoundException, ConflictException, InterruptedException {
        String name = getAccountName(u);
        DummyAccount account = resource.controller.addAccount(name);
        addAttributes(account, SourceVariant.A_SINGLE_NAME, sourceVariant.getSingleValuedAttributes(), 1);
        addAttributes(account, SourceVariant.A_MULTI_NAME, sourceVariant.getMultiValuedAttributes(),
                sourceVariant.getAttributeValues());
    }

    String getAccountName(int u) {
        return String.format(ACCOUNT_NAME, u);
    }

    private void addAttributes(DummyAccount account, String namePattern, int attrs, int values) throws ConflictException,
            FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        for (int a = 0; a < attrs; a++) {
            String attrName = String.format(namePattern, a);
            for (int v = 0; v < values; v++) {
                account.addAttributeValue(attrName, RandomStringUtils.random(10, true, true));
            }
        }
    }
}
