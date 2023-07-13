/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCluster;

public class ClusteringExecutor {

    private Clusterable clusterable;

    public ClusteringExecutor(ClusterObjectUtils.Mode mode) {
        if (mode.equals(ClusterObjectUtils.Mode.USER)) {
            this.clusterable = new UserBasedClustering();
        } else if (mode.equals(ClusterObjectUtils.Mode.ROLE)) {
            this.clusterable = new RoleBasedClustering();
        }
    }

    public List<PrismObject<RoleAnalysisCluster>> execute(ClusterOptions clusterOptions) {
        return clusterable.executeClustering(clusterOptions);
    }
}
