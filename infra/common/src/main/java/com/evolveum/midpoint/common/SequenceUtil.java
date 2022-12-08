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

import java.util.List;
import java.util.Objects;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SequenceUtil {

    private static final Trace LOGGER = TraceManager.getTrace(SequenceUtil.class);

    public static long advanceSequence(SequenceType sequence) {
        List<Long> unusedValues = sequence.getUnusedValues();
        if (!unusedValues.isEmpty()) {
            return unusedValues.remove(0);
        } else {
            return reallyAdvanceSequence(sequence);
        }
    }

    private static long reallyAdvanceSequence(SequenceType sequence) {
        long returnValue;
        long counter = Objects.requireNonNullElse(sequence.getCounter(), 0L);
        long maxCounter = Objects.requireNonNullElse(sequence.getMaxCounter(), Long.MAX_VALUE);
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
        } else {
            assert counter > maxCounter;
            if (allowRewind) { // shouldn't occur but...
                LOGGER.warn("Sequence {} overflown with allowRewind set to true. Rewinding.", sequence.getOid());
                returnValue = 0;
                sequence.setCounter(1L);
            } else {
                throw new SystemException(
                        String.format("No (next) value available from sequence %s. Current counter = %d, max value = %d.",
                                sequence.getOid(), sequence.getCounter(), sequence.getMaxCounter()));
            }
        }
        return returnValue;
    }
}
