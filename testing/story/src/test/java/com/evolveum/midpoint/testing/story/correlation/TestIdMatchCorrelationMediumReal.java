/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.model.api.correlator.idmatch.MatchingRequest;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Testing identity matching using real ID Match implementation (currently COmanage Match).
 *
 * REQUIREMENTS:
 *
 * 1) The COmanage Match runs as an external system. See `resource-xxxxxx.xml` for URL and credentials.
 * 2) The COmanage Match is configured like {@link DummyIdMatchServiceImpl#executeMatch(MatchingRequest, OperationResult)}.
 * 3) The database of the service is initially empty (no records).
 *
 * This test runs manually.
 */
public class TestIdMatchCorrelationMediumReal extends AbstractMediumIdMatchCorrelationTest {

    // No ID Match Service override here, so the URL configured in the resource will be used.
}
