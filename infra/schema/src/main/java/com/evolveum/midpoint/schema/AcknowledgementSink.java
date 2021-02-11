/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Denotes an object capable of receiving an acknowledge that an item was processed.
 * It is placed in this module and package, because it is a universal concept, with a scope
 * similar to {@link ResultHandler}.
 */
@Experimental
public interface AcknowledgementSink {

    /**
     * Informs the receiver that a particular item was processed (successfully or not).
     *
     * @param release If true, the item can be forgotten. If false, we want to receive that item again, presumably
     * to be reprocessed later.
     *
     * @param result Operation result in context of which the acknowledgement should take place.
     * It is useful if the acknowledgement itself can take considerable time, e.g. when it involves a communication
     * with an external party.
     *
     * @implNote The item source may be limited in the re-processing options. Examples: (1) Live synchronization can either
     * release all events before specified given token value (in the case of precise token value capability), or it can
     * only hold/release all events retrieved (if this capability is missing). (2) Asynchronous update: Here we depend
     * on the capabilities of the async update source. But even when having the most selective message acknowledgements,
     * retrying a message can cause message ordering issues. So we have to be very careful here.
     */
    void acknowledge(boolean release, OperationResult result);
}
