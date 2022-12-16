/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * This class does nothing. It just takes place when no real Lens Context is available.
 * @see ModelExpressionThreadLocalHolder
 *
 * @author semancik
 *
 */
public class LensContextPlaceholder<F extends ObjectType> extends LensContext<F> {

    public LensContextPlaceholder(@NotNull PrismObject<F> focus, TaskExecutionMode taskExecutionMode) {
        super(taskExecutionMode);
        //noinspection unchecked
        createFocusContext((Class<F>) focus.asObjectable().getClass());
        getFocusContext().setInitialObject(focus);
    }

    @Override
    public String toString() {
        return "LensContextPlaceholder()";
    }

    @Override
    public String dump(boolean showTriples) {
        return "LensContextPlaceholder()";
    }

    @Override
    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("LensContextPlaceholder");
        return sb.toString();
    }
}
