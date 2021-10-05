/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

/**
 * Operation being recorded: represents an object to which the client reports the end of the operation.
 * It is called simply {@link Operation} to avoid confusing the clients.
 */
public interface Operation {

    default void succeeded() {
        done(ItemProcessingOutcomeType.SUCCESS, null);
    }

    default void skipped() {
        done(ItemProcessingOutcomeType.SKIP, null);
    }

    default void failed(Throwable t) {
        done(ItemProcessingOutcomeType.FAILURE, t);
    }

    default void done(ItemProcessingOutcomeType outcome, Throwable exception) {
    }

    default void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        // no-op
    }

    double getDurationRounded();

    long getEndTimeMillis();
}
