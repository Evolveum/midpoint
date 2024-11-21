/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.FailableProcessor;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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
        super(new FileBasedTestObjectSource(dir, fileName), oid);
        this.name = name;
        this.controllerInitLambda = controllerInitLambda;
    }

    public DummyTestResource(
            TestObjectSource source, String oid, String name, FailableProcessor<DummyResourceContoller> controllerInitLambda) {
        super(source, oid);
        this.name = name;
        this.controllerInitLambda = controllerInitLambda;
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
}
