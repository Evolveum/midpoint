/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SequenceHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SequenceHelper.class);

    public static long advanceSequence(SequenceType sequence) {
        long returnValue;
        if (!sequence.getUnusedValues().isEmpty()) {
            returnValue = sequence.getUnusedValues().remove(0);
        } else {
            returnValue = advanceSequence(sequence, sequence.getOid());
        }

        return returnValue;
    }

    private static long advanceSequence(SequenceType sequence, String oid) {
        long returnValue;
        long counter = sequence.getCounter() != null ? sequence.getCounter() : 0L;
        long maxCounter = sequence.getMaxCounter() != null
                ? sequence.getMaxCounter() : Long.MAX_VALUE;
        boolean allowRewind = Boolean.TRUE.equals(sequence.isAllowRewind());

        if (counter < maxCounter) {
            returnValue = counter;
            sequence.setCounter(counter + 1);
        } else if (counter == maxCounter) {
            returnValue = counter;
            if (allowRewind) {
                sequence.setCounter(0L);
            } else {
                sequence.setCounter(counter + 1); // will produce exception during next run
            }
        } else { // i.e. counter > maxCounter
            if (allowRewind) { // shouldn't occur but...
                LOGGER.warn("Sequence {} overflown with allowRewind set to true. Rewinding.", oid);
                returnValue = 0;
                sequence.setCounter(1L);
            } else {
                throw new SystemException("No (next) value available from sequence " + oid
                        + ". Current counter = " + sequence.getCounter()
                        + ", max value = " + sequence.getMaxCounter());
            }
        }
        return returnValue;
    }
}
