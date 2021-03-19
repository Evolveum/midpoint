/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartExecutionRecordType;

import com.google.common.annotations.VisibleForTesting;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes real wall clock time from a set of execution records ("from - to").
 */
public class WallClockTimeComputer {

    private static final Trace LOGGER = TraceManager.getTrace(WallClockTimeComputer.class);

    private final Set<Interval> intervals;

    WallClockTimeComputer(List<TaskPartExecutionRecordType> executionRecords) {
        intervals = executionRecords.stream()
                .map(Interval::create)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    public WallClockTimeComputer(long[][] inputs) {
        intervals = Arrays.stream(inputs)
                .map(pair -> new Interval(pair[0], pair[1]))
                .collect(Collectors.toSet());
    }

    public long compute() {
        LOGGER.trace("Intervals at input: {}", intervals);

        Set<Interval> nonOverlapping = eliminateOverlaps(this.intervals);
        LOGGER.trace("Non-overlapping intervals: {}", nonOverlapping);

        long time = summarize(nonOverlapping);
        LOGGER.trace("Summary time: {}", time);

        return time;
    }

    private static long summarize(Set<Interval> intervals) {
        return intervals.stream()
                .mapToLong(Interval::getTime)
                .sum();
    }

    /**
     * Eliminates overlaps.
     */
    private static Set<Interval> eliminateOverlaps(Set<Interval> intervals) {
        Set<Interval> newIntervals = new HashSet<>();
        for (Interval interval : intervals) {
            for (;;) {
                Interval overlapping = findOverlapping(newIntervals, interval);
                if (overlapping == null) {
                    newIntervals.add(interval);
                    break;
                } else {
                    newIntervals.remove(overlapping);
                    interval = interval.mergeWith(overlapping);
                }
            }
        }
        return newIntervals;
    }

    private static Interval findOverlapping(Collection<Interval> intervals, Interval tested) {
        return intervals.stream()
                .filter(interval -> interval.overlapsWith(tested))
                .findFirst().orElse(null);
    }

    private static class Interval {
        private final long from;
        private final long to;

        Interval(long from, long to) {
            this.from = from;
            this.to = to;
        }

        private static Interval create(TaskPartExecutionRecordType record) {
            if (record.getStartTimestamp() != null && record.getEndTimestamp() != null) {
                Interval interval = new Interval(
                        XmlTypeConverter.toMillis(record.getStartTimestamp()),
                        XmlTypeConverter.toMillis(record.getEndTimestamp()));
                if (interval.isValid()) {
                    return interval;
                } else {
                    LOGGER.warn("Malformed execution record: {} -> {}", record, interval);
                    return null;
                }
            } else {
                LOGGER.warn("Malformed execution record: {}", record);
                return null;
            }
        }

        private boolean isValid() {
            return from <= to;
        }

        public long getTime() {
            return to - from;
        }

        public boolean contains(long point) {
            return point >= from && point <= to;
        }

        @Override
        public String toString() {
            return "<" + from + ", " + to + ">";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Interval interval = (Interval) o;
            return from == interval.from && to == interval.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        private boolean overlapsWith(Interval other) {
            return contains(other.from) || contains(other.to) ||
                    other.contains(from) || other.contains(to);
        }

        public Interval mergeWith(Interval other) {
            return new Interval(Math.min(from, other.from), Math.max(to, other.to));
        }
    }
}
