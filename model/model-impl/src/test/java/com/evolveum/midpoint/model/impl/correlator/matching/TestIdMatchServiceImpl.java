/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.matching;

import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchServiceImpl;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests {@link IdMatchServiceImpl} class directly i.e. on very low level.
 *
 * Not a part of test suite. Expects external ID Match service being available.
 *
 * TODO The ID Match service must be configured to behave just like {@link DummyIdMatchServiceImpl#executeMatch
 *  (ShadowAttributesType, OperationResult)} (see the description there)
 *
 * The answers should be like described in {@link AbstractIdMatchServiceTest#FILE_ACCOUNTS}. If that would not be
 * possible, please create a copy of that file, and adapt it accordingly. We'll maintain both, or (even better) we'll
 * update the {@link DummyIdMatchServiceImpl} to match the actual behavior of COmanage Match.
 */
public class TestIdMatchServiceImpl extends AbstractIdMatchServiceTest {

    private static final String URL = "todo";
    private static final String USERNAME = "todo";
    private static final String PASSWORD = "todo";

    protected IdMatchService createService() {
        return IdMatchServiceImpl.instantiate(URL, USERNAME, ProtectedStringType.fromClearValue(PASSWORD));
    }
}
