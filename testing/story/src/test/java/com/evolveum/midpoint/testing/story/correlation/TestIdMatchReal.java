/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

/**
 * Testing identity matching using real ID Match implementation (currently COmanage Match).
 *
 * REQUIREMENTS:
 *
 * 1) The COmanage Match runs as an external system. See `resource-ais.xml` for URL and credentials.
 * 2) The COmanage Match is configured like {@link DummyIdMatchServiceImpl#executeMatch(ShadowAttributesType, OperationResult)}.
 * 3) The database of the service is initially empty (no records).
 *
 * This test runs manually.
 */
public class TestIdMatchReal extends AbstractIdMatchTest {

}
