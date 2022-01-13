/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.matching;

import com.evolveum.midpoint.model.impl.correlator.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchService;

/**
 * Tests {@link DummyIdMatchServiceImpl}. Used to develop the testing data for real {@link TestIdMatchServiceImpl}
 * and other tests in this package.
 */
public class TestDummyIdMatchServiceImpl extends AbstractIdMatchServiceTest {

    @Override
    protected IdMatchService createService() {
        return new DummyIdMatchServiceImpl();
    }
}
