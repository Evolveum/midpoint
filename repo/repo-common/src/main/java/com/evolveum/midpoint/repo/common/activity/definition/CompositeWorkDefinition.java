/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;

import org.jetbrains.annotations.NotNull;

/**
 * Definition for pure composite activity.
 */
public class CompositeWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final ActivityCompositionType composition;

    CompositeWorkDefinition(@NotNull ActivityCompositionType composition, @NotNull ConfigurationItemOrigin origin) {
        super(origin);
        this.composition = composition;
    }

    public @NotNull ActivityCompositionType getComposition() {
        return composition;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "composition", composition, indent+1);
    }
}
