/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Map;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.FailableProcessor;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Representation of Dummy Resource in tests.
 */
@Experimental
public class DummyTestResource extends TestResource {

    public final String name;
    public final FailableProcessor<DummyResourceContoller> controllerInitLambda;
    public DummyResourceContoller controller;

    /**
     * TODO change to static factory method
     */
    public DummyTestResource(File dir, String fileName, String oid, String name) {
        this(dir, fileName, oid, name, null);
    }

    /**
     * TODO change to static factory method
     */
    public DummyTestResource(File dir, String fileName, String oid, String name, FailableProcessor<DummyResourceContoller> controllerInitLambda) {
        this(new FileBasedTestObjectSource(dir, fileName), oid, name, controllerInitLambda);
    }

    public DummyTestResource(
            TestObjectSource source, String oid, String name, FailableProcessor<DummyResourceContoller> controllerInitLambda) {
        super(source, oid);
        this.name = name;
        this.controllerInitLambda = controllerInitLambda;
    }

    public static DummyTestResourceWithoutAccounts fromFile(File resourceDir, String resourceFileName, String oid,
            String name) {
        return (accountsFile) -> {
            final AccountsCsvParser accountsParser = new AccountsCsvParser(accountsFile);
            final FailableProcessor<DummyResourceContoller> resourceInitializer = controller -> {
                addAttributesToSchema(controller, accountsParser.readAttributesNames());
                addAccounts(controller, accountsParser.readAccounts());
            };
            return new DummyTestResource(new FileBasedTestObjectSource(resourceDir, resourceFileName), oid, name,
                    resourceInitializer);
        };
    }

    public static DummyTestResource fromTestObject(
            TestObject<?> object, String instanceName, FailableProcessor<DummyResourceContoller> controllerInitLambda) {
        return new DummyTestResource(object.source, object.oid, instanceName, controllerInitLambda);
    }

    @Override
    protected void afterReload(OperationResult result) {
        controller.setResource(get());
    }

    @Deprecated // use .get()
    public PrismObject<ResourceType> getResource() {
        return controller.getResource();
    }

    @Deprecated // use .getObjectable()
    public ResourceType getResourceBean() {
        return controller.getResource().asObjectable();
    }

    public DummyResource getDummyResource() {
        return controller.getDummyResource();
    }

    // It's logical for this functionality to be invokable right on the DummyTestResource object. Hence this method.
    public void initAndTest(DummyTestResourceInitializer initializer, Task task, OperationResult result) throws Exception {
        initializer.initAndTestDummyResource(this, task, result);
    }

    // It's logical for this functionality to be invokable right on the DummyTestResource object. Hence this method.
    public void init(AbstractIntegrationTest test, Task task, OperationResult result) throws Exception {
        test.registerTestObjectUsed(this);
        test.initDummyResource(this, task, result);
    }

    public void initWithOverwrite(AbstractIntegrationTest test, Task task, OperationResult result) throws Exception {
        test.deleteIfPresent(this, result);
        reset();
        test.initDummyResource(this, task, result);
    }

    public String addAccount(DummyAccount account) throws ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        return getDummyResource().addAccount(account);
    }

    public DummyAccount addAccount(String name) throws ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        return controller.addAccount(name);
    }

    public DummyAccount addAccount(String name, String fullName) throws ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        return controller.addAccount(name, fullName);
    }

    public TestResourceAccounts getAccounts(AbstractIntegrationTest test, ShadowReader shadowReader) {
        return new TestResourceAccounts(shadowReader, this.get(), test.prismContext);
    }

    private static void addAccounts(DummyResourceContoller controller, Collection<AccountsCsvParser.Account> accounts)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        for (AccountsCsvParser.Account csvAccount : accounts) {
            final DummyAccount account = controller.addAccount(csvAccount.id());
            for (Map.Entry<String, String> entry : csvAccount.attributes().entrySet()) {
                account.addAttributeValue(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void addAttributesToSchema(DummyResourceContoller controller, Collection<String> attributes) {
        attributes.forEach(
                attr -> controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        attr, String.class, false, false));
    }

    /**
     * DummyTestResource which you can populate with accounts from CSV.
     */
    @FunctionalInterface
    public interface DummyTestResourceWithoutAccounts {

        /**
         * Adds accounts defined in the CSV file to the dummy resource.
         *
         * This method adds also all necessary attributes definitions, based on the header row in the CSV.
         * All attributes will be however defined as strings.
         *
         * @see AccountsCsvParser for details about the CSV format.
         *
         * @param fileName The CSV file with the accounts.
         * @return The new instance of the dummy test resource, which will be populated with accounts after its
         * initialization.
         */
        DummyTestResource withAccountsFromCsv(File fileName) throws IOException;
    }

}
