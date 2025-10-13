/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.test.CsvTestResource;

public class TestInternalCorrelationSimpleSimplified extends AbstractSimpleInternalCorrelationTest {

    @Override
    CsvTestResource getTargetResource() {
        return RESOURCE_TARGET_SIMPLIFIED;
    }
}
