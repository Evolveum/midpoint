/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Element state that we can return to e.g. in the new iteration.
 *
 * Currently comprises only the secondary delta - all other things (current delta, summary delta, new object)
 * will be recomputed when needed.
 */
public class RememberedElementState<O extends ObjectType> implements Serializable, DebugDumpable {

    @Nullable private final ObjectDelta<O> secondaryDelta;

    public RememberedElementState(@Nullable ObjectDelta<O> secondaryDelta) {
        this.secondaryDelta = secondaryDelta;
    }

    public @Nullable ObjectDelta<O> getSecondaryDelta() {
        return secondaryDelta;
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(secondaryDelta, indent);
    }
}
