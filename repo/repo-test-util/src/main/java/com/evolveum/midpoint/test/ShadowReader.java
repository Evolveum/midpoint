/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.test;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Functional interface, intended for use by various TestObject, to allows usage of classes outside of this maven
 * module.
 *
 * It for example allows to use ProvisioningService to read shadows in the {@link DummyTestResource}, even though it
 * is not directly accessible from this module.
 *
 * NOTE: This interface should not be necessary anymore, if the {@link DummyTestResource} (and similar) are ever
 * moved e.g. to provisioning module.
 */
@FunctionalInterface
public interface ShadowReader {
    Collection<PrismObject<ShadowType>> readShadows(PrismObject<ResourceType> resource, Task task,
            OperationResult result) throws CommonException;
}
