/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
