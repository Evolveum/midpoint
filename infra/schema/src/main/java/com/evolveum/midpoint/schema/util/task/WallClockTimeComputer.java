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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRunRecordType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes real wall clock time from a set of execution records ("from - to").
 */
public class WallClockTimeComputer {

    private static final Trace LOGGER = TraceManager.getTrace(WallClockTimeComputer.class);

    private final Set<Interval> intervals;
    private final Set<Interval> nonOverlappingIntervals;

    @SafeVarargs
    WallClockTimeComputer(List<ActivityRunRecordType>... lists) {
        intervals = Arrays.stream(lists)
                .flatMap(Collection::stream)
                .map(Interval::create)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        nonOverlappingIntervals = eliminateOverlaps(this.intervals);
    }

    @VisibleForTesting
    public WallClockTimeComputer(long[][] inputs) {
        intervals = Arrays.stream(inputs)
                .map(pair -> Interval.create(pair[0], pair[1]))
                .collect(Collectors.toSet());
        nonOverlappingIntervals = eliminateOverlaps(this.intervals);
    }

    @SafeVarargs
    public static WallClockTimeComputer create(@NotNull List<ActivityRunRecordType>... lists) {
        return new WallClockTimeComputer(lists);
    }

    public long getSummaryTime() {
        return summarize(nonOverlappingIntervals);
    }

    public XMLGregorianCalendar getEarliestStartTime() {
        return nonOverlappingIntervals.stream()
                .filter(Objects::nonNull)
                .map(Interval::getStartTime)
                .min(Comparator.comparing(XmlTypeConverter::toMillis))
                .orElse(null);
    }

    public List<ActivityRunRecordType> getNonOverlappingRecords() {
        return nonOverlappingIntervals.stream()
                .map(Interval::getRecord)
                .collect(Collectors.toList());
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
        LOGGER.trace("Intervals at input: {}", intervals);
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
        LOGGER.trace("Non-overlapping intervals: {}", newIntervals);
        return newIntervals;
    }

    private static Interval findOverlapping(Collection<Interval> intervals, Interval tested) {
        return intervals.stream()
                .filter(interval -> interval.overlapsWith(tested))
                .findFirst().orElse(null);
    }

    private static class Interval {
        private final long fromMillis;
        private final long toMillis;
        private final ActivityRunRecordType record;

        Interval(long fromMillis, long toMillis, ActivityRunRecordType record) {
            this.fromMillis = fromMillis;
            this.toMillis = toMillis;
            this.record = record;
        }

        private static Interval create(ActivityRunRecordType record) {
            if (record.getStartTimestamp() != null && record.getEndTimestamp() != null) {
                Interval interval = new Interval(
                        XmlTypeConverter.toMillis(record.getStartTimestamp()),
                        XmlTypeConverter.toMillis(record.getEndTimestamp()),
                        record);
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

        @VisibleForTesting
        private static Interval create(long fromMillis, long toMillis) {
            return new Interval(fromMillis, toMillis, null);
        }

        private boolean isValid() {
            return fromMillis <= toMillis;
        }

        public long getTime() {
            return toMillis - fromMillis;
        }

        public ActivityRunRecordType getRecord() {
            return record;
        }

        private boolean contains(long point) {
            return point >= fromMillis && point <= toMillis;
        }

        @Override
        public String toString() {
            return "<" + fromMillis + ", " + toMillis + ">";
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
            return fromMillis == interval.fromMillis && toMillis == interval.toMillis;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromMillis, toMillis);
        }

        private boolean overlapsWith(Interval other) {
            return contains(other.fromMillis) || contains(other.toMillis) ||
                    other.contains(fromMillis) || other.contains(toMillis);
        }

        public Interval mergeWith(Interval other) {
            boolean useRecords;
            if (record == null && other.record == null) {
                useRecords = false;
            } else if (record != null && other.record != null) {
                useRecords = true;
            } else {
                throw new IllegalStateException("Mixing test and non-test intervals: " + this + " vs " + other);
            }

            long newFromMillis;
            long newToMillis;
            XMLGregorianCalendar newFromXml;
            XMLGregorianCalendar newToXml;
            if (fromMillis <= other.fromMillis) {
                newFromMillis = fromMillis;
                newFromXml = useRecords ? record.getStartTimestamp() : null;
            } else {
                newFromMillis = other.fromMillis;
                newFromXml = useRecords ? other.record.getStartTimestamp() : null;
            }
            if (toMillis >= other.toMillis) {
                newToMillis = toMillis;
                newToXml = useRecords ? record.getEndTimestamp() : null;
            } else {
                newToMillis = other.toMillis;
                newToXml = useRecords ? other.record.getEndTimestamp() : null;
            }

            return new Interval(newFromMillis, newToMillis, useRecords ?
                    new ActivityRunRecordType()
                        .startTimestamp(newFromXml)
                        .endTimestamp(newToXml) : null);
        }

        public XMLGregorianCalendar getStartTime() {
            if (record != null) {
                return record.getStartTimestamp();
            } else {
                // Record is null only in low-level tests. But let us provide reasonable info also in that case.
                return XmlTypeConverter.createXMLGregorianCalendar(fromMillis);
            }
        }
    }
}
