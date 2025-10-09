/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlator.matching;

import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;

/**
 * Tests {@link DummyIdMatchServiceImpl}.
 *
 * Used to develop the tests for real {@link TestIdMatchServiceImpl} and other tests in this package.
 */
public class TestDummyIdMatchServiceImpl extends AbstractIdMatchServiceTest {

    @Override
    protected IdMatchService createService() {
        return new DummyIdMatchServiceImpl();
    }
}
