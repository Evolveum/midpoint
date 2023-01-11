/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public interface DummyTestResourceInitializer {

    void initAndTestDummyResource(DummyTestResource resource, Task task, OperationResult result) throws Exception;

    DummyResourceContoller initDummyResource(DummyTestResource resource, Task task, OperationResult result) throws Exception;
}
