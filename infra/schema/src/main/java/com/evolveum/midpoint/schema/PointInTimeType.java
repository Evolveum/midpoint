/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PointInTimeTypeType;

/**
 * Specifies the point in time for the returned data. This option controls whether fresh or cached data will
 * be returned or whether future data projection will be returned. MidPoint usually deals with fresh data
 * that describe situation at the current point in time. But the client code may want to get data from the
 * cache that may be possibly stale. Or the client code may want a projection about the future state of the
 * data (e.g. taking running asynchronous operation into consideration).
 *
 * @author semancik
 */
public enum PointInTimeType {

    /**
     * Return cached data (if available).
     * Avoid fetching the data from external system.
     */
    CACHED,

    /**
     * Return current data. Fetch from external system if needed.
     * The "current" has to be understood in Einsteinean sense.
     * The returned data are as fresh as possible - but that still
     * may be hours or days old for some resources.
     * This is usually the default.
     */
    CURRENT,

    /**
     * Returns current data and applies all the available projections
     * about future state of the data. E.g. applies projected state of
     * pending asynchronous operations.
     */
    FUTURE;

    public static PointInTimeTypeType toPointInTimeTypeType(PointInTimeType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case CACHED: return PointInTimeTypeType.CACHED;
            case CURRENT: return PointInTimeTypeType.CURRENT;
            case FUTURE: return PointInTimeTypeType.FUTURE;
            default: throw new IllegalArgumentException("value: " + value);
        }
    }

    public static PointInTimeType toPointInTimeType(PointInTimeTypeType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case CACHED: return PointInTimeType.CACHED;
            case CURRENT: return PointInTimeType.CURRENT;
            case FUTURE: return PointInTimeType.FUTURE;
            default: throw new IllegalArgumentException("value: " + value);
        }
    }
}
