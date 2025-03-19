
/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierNoiseCategoryType;

import java.util.Set;

/**
 * An interface representing data points that can be used in clustering algorithms.
 */
public interface Clusterable {

    Set<String> getPoint();

    Set<String> getMembers();

    Set<String> getCloseNeighbors();

    void addCloseNeighbor(String neighbor);

    ExtensionProperties getExtensionProperties();

    int getMembersCount();

    OutlierNoiseCategoryType getPointStatus();

    void setPointStatus(OutlierNoiseCategoryType pointStatus);
}
