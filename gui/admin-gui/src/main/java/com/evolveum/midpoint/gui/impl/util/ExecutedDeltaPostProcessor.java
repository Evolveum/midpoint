/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;

/**
 * Processor with deltas and post process after successful executing of these deltas.
 */
public interface ExecutedDeltaPostProcessor {

    /**
     * Return deltas for executing.
     */
    Collection<ObjectDelta<? extends ObjectType>> getObjectDeltas();

    /**
     * Processing after successful executing of deltas
     */
    void processExecutedDelta(Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, PageBase pageBase);
}
