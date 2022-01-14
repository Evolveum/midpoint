/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Testing identity matching using real ID Match implementation (currently COmanage Match).
 *
 * REQUIREMENTS:
 *
 * The COmanage Match runs as an external system. Therefore, this tests runs manually.
 */
public class TestIdMatchReal extends AbstractIdMatchTest {

    @Override
    protected void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) {
        throw new UnsupportedOperationException();
    }
}
