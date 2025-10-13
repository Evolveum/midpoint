/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public interface DummyTestResourceInitializer extends ResourceTester {

    void initAndTestDummyResource(DummyTestResource resource, Task task, OperationResult result) throws Exception;

    DummyResourceContoller initDummyResource(DummyTestResource resource, Task task, OperationResult result) throws Exception;
}
