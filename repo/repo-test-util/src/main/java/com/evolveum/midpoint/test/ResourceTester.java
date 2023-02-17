/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.schema.util.SimpleObjectResolver;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Object that is able to test the resource. It may be e.g. an instance of the model integration test.
 */
public interface ResourceTester {

    OperationResult testResource(@NotNull String oid, @NotNull Task task, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException;

    /** Used for reloading the object. Different for provisioning- and model-level tests. */
    SimpleObjectResolver getResourceReloader();
}
