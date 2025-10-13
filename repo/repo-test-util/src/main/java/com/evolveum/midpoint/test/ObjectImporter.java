/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface ObjectImporter {

    /**
     * Imports object into the repository (typically using model API, if available).
     *
     * Should set object.oid if the import was successful.
     */
    <O extends ObjectType> void importObject(PrismObject<O> object, Task task, OperationResult result) throws CommonException;
}
