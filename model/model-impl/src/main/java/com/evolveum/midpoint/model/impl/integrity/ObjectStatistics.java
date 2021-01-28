/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 */
public class ObjectStatistics {

    private int errors = 0; // TODO use standard mechanisms instead
    private final Map<String,ObjectTypeStatistics> statisticsMap = new HashMap<>();        // key is object class full name

    public Map<String, ObjectTypeStatistics> getStatisticsMap() {
        return statisticsMap;
    }

    public int getErrors() {
        return errors;
    }

    public void record(PrismObject<ObjectType> object) {
        String key = object.asObjectable().getClass().getName();
        ObjectTypeStatistics typeStatistics = statisticsMap.computeIfAbsent(key, (k) -> new ObjectTypeStatistics());
        typeStatistics.register(object);
    }

    public void incrementObjectsWithErrors() {
        errors++;
    }
}
