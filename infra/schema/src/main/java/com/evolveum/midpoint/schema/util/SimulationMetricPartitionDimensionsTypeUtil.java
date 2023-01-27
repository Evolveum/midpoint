/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
