/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchCorrelatorFactory;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.springframework.beans.factory.annotation.Autowired;

public class TestIdMatchCorrelationSimpleDummy extends AbstractSimpleIdMatchCorrelationTest {

    private final DummyIdMatchServiceImpl dummyIdMatchService = new DummyIdMatchServiceImpl();

    @Autowired private IdMatchCorrelatorFactory idMatchCorrelatorFactory;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        idMatchCorrelatorFactory.setServiceOverride(dummyIdMatchService);
    }
}
