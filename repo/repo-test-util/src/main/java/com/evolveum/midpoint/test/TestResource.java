/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;

import java.io.File;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Represents arbitrary {@link ResourceType} resource to be used in tests.
 *
 * @see CsvTestResource
 * @see DummyTestResource
 */
public class TestResource extends TestObject<ResourceType> {

    TestResource(TestObjectSource source, String oid) {
        super(source, oid);
    }

    public static TestResource file(@NotNull File dir, @NotNull String name, String oid) {
        return new TestResource(
                new FileBasedTestObjectSource(dir, name),
                oid);
    }

    /**
     * Imports the resource, tests it, and reloads it (to have e.g. the schema).
     */
    public void initAndTest(ResourceTester tester, Task task, OperationResult result) throws CommonException, IOException {
        importObject(task, result);
        assertSuccess(
                tester.testResource(oid, task, result));
        reload(tester.getResourceReloader(), result);
    }
}
