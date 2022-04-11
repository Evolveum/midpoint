/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ObjectImporter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static org.testng.AssertJUnit.assertFalse;

/**
 * Imports objects into repository via model API. References in the objects are resolved in this mode.
 */
@Component
public class ModelObjectImporterImpl implements ObjectImporter {

    @Autowired private ModelService modelService;

    @Override
    public <O extends ObjectType> void importObject(PrismObject<O> object, Task task, OperationResult result)
            throws CommonException {
        ObjectDelta<O> addDelta = object.createAddDelta();
        assertFalse("Immutable object provided?", addDelta.getObjectToAdd().isImmutable());
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas =
                modelService.executeChanges(
                        List.of(addDelta),
                        ModelExecuteOptions.create().setIsImport(),
                        task, result);
        object.setOid(ObjectDeltaOperation.findAddDeltaOid(executedDeltas, object));
    }
}
