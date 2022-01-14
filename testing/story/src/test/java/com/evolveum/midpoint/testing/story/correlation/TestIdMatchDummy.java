/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchCorrelatorFactory;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

public class TestIdMatchDummy extends AbstractIdMatchTest {

    private final DummyIdMatchServiceImpl dummyIdMatchService = new DummyIdMatchServiceImpl();

    @Autowired private IdMatchCorrelatorFactory idMatchCorrelatorFactory;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        idMatchCorrelatorFactory.setServiceOverride(dummyIdMatchService);
    }

    protected void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) {
        dummyIdMatchService.resolve(attributes, matchRequestId, referenceId, result);
    }
}
