/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionDimensionsType;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;

public class SimulationMetricPartitionDimensionsTypeUtil {

    public static Set<QName> getDimensions(SimulationMetricPartitionDimensionsType bean) {
        if (bean == null) {
            return ALL_DIMENSIONS;
        } else {
            return new HashSet<>(bean.getDimension());
        }
    }

    public static SimulationMetricPartitionDimensionsType toBean(Set<QName> dimensions) {
        var bean = new SimulationMetricPartitionDimensionsType();
        bean.getDimension().addAll(dimensions);
        return bean;
    }
}
