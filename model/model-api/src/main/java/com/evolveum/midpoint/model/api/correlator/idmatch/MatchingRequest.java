/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator.idmatch;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * A request for matching to be sent to an external ID Match service.
 */
public class MatchingRequest implements DebugDumpable {

    @NotNull private final IdMatchObject object;

    public MatchingRequest(@NotNull IdMatchObject object) {
        this.object = object;
    }

    public @NotNull IdMatchObject getObject() {
        return object;
    }

    @Override
    public String toString() {
        return "MatchingRequest{" +
                "object=" + object +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "object", object, indent + 1);
        return sb.toString();
    }
}
