/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

/**
 * Object capable of receiving updates on structured progress.
 *
 * NOTE: These methods do NOT update information in `task.prism`. They only affect `task.statistics`. Do not forget
 * to call appropriate "update" methods afterwards.
 *
 * NOTE: Structured progress is NOT maintained in worker LATs.
 */
@Experimental
public interface StructuredProgressCollector {

    /**
     * Signals to structured progress that we entered specified task part.
     * This clears the 'open' progress items.
     */
    void setStructuredProgressPartInformation(String partUri, Integer partNumber, Integer expectedParts);

    /**
     * Increments structured progress in given part.
     */
    void incrementStructuredProgress(String partUri, QualifiedItemProcessingOutcomeType outcome);

    /**
     * Marks structured progress for given part as complete.
     */
    void markStructuredProgressAsComplete();

    /**
     * Adapts structured progress when a bucket is complete. This moves all 'open' items to 'closed'.
     */
    void changeStructuredProgressOnWorkBucketCompletion();

    /**
     * Temporary method: all "open" counters are switched to "closed" state.
     */
    void markAllStructuredProgressClosed();
}
