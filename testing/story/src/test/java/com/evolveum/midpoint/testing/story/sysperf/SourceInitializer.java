/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.List;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.ROLES_CONFIGURATION;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.SOURCES_CONFIGURATION;

public class SourceInitializer {

    private final TestSystemPerformance test;
    private final List<DummyTestResource> resources;
    private final Task initTask;

    private static final String ACCOUNT_NAME = "u-%08d";

    SourceInitializer(TestSystemPerformance test, List<DummyTestResource> resources, Task initTask) {
        this.test = test;
        this.resources = resources;
        this.initTask = initTask;
    }

    public void run(OperationResult result) throws Exception {
        boolean primary = true;
        for (DummyTestResource resource : resources) {
            initializeResource(resource, result);
            createAccounts(resource, primary);
            primary = false;
            SOURCES_CONFIGURATION.getOperationDelay().applyTo(resource);
        }
    }

    private void initializeResource(DummyTestResource resource, OperationResult result) throws Exception {
        test.initDummyResource(resource, initTask, result);
    }

    private void createAccounts(DummyTestResource resource, boolean primary)
            throws ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        for (int u = 0; u < SOURCES_CONFIGURATION.getNumberOfAccounts(); u++) {
            createAccount(u, resource, primary);
        }
    }

    private void createAccount(int u, DummyTestResource resource, boolean primary)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        String name = getAccountName(u);
        DummyAccount account = resource.controller.addAccount(name);
        if (primary) {
            addAttributes(account, SourcesConfiguration.A_SINGLE_NAME, SOURCES_CONFIGURATION.getSingleValuedMappings(), 1);
            addRoles(account);
        }
        addAttributes(account, SourcesConfiguration.A_MULTI_NAME, SOURCES_CONFIGURATION.getMultiValuedMappings(),
                SOURCES_CONFIGURATION.getAttributeValues());
    }

    private void addRoles(DummyAccount account) throws ConflictException, FileNotFoundException, SchemaViolationException,
            InterruptedException, ConnectException {
        account.addAttributeValues(SourcesConfiguration.A_ROLE, ROLES_CONFIGURATION.getRolesForAccount());
    }

    static String getAccountName(int u) {
        return String.format(ACCOUNT_NAME, u);
    }

    private void addAttributes(DummyAccount account, String namePattern, int attrs, int values) throws ConflictException,
            FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        for (int a = 0; a < attrs; a++) {
            String attrName = String.format(namePattern, a);
            for (int v = 0; v < values; v++) {
                account.addAttributeValue(
                        attrName,
                        RandomStringUtils.random(10, 0, 0, true, true, null, RandomSource.FIXED_RANDOM));
            }
        }
    }
}
