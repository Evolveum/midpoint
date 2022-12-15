/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.FailableProcessor;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Representation of Dummy Resource in tests.
 */
@Experimental
public class DummyTestResource extends TestResource<ResourceType> {

    public final String name;
    public final FailableProcessor<DummyResourceContoller> controllerInitLambda;
    public DummyResourceContoller controller;

    public DummyTestResource(File dir, String fileName, String oid, String name) {
        this(dir, fileName, oid, name, null);
    }

    public DummyTestResource(File dir, String fileName, String oid, String name, FailableProcessor<DummyResourceContoller> controllerInitLambda) {
        super(dir, fileName, oid);
        this.name = name;
        this.controllerInitLambda = controllerInitLambda;
    }

    @Override
    public void reload(OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        super.reload(result);
        controller.setResource(getObject());
    }

    public PrismObject<ResourceType> getResource() {
        return controller.getResource();
    }

    public ResourceType getResourceBean() {
        return controller.getResource().asObjectable();
    }

    // It's logical for this functionality to be invokable right on the DummyTestResource object. Hence this method.
    public void initAndTest(DummyTestResourceInitializer initializer, Task task, OperationResult result) throws Exception {
        initializer.initAndTestDummyResource(this, task, result);
    }
}
