/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ClusterUtils {

    public static @NotNull HashMap<String, Double> generateFrequencyMap(@NotNull List<ClusteringObjectMapped> users,
            List<String> occupiedRoles) {

        HashMap<String, Double> roleFrequencyMap = new HashMap<>();
        HashSet<String> roleIds = new HashSet<>(occupiedRoles);

        HashMap<String, Integer> roleCountMap = new HashMap<>();
        int totalMiningTypeObjects = 0;

        for (ClusteringObjectMapped user : users) {
            totalMiningTypeObjects += user.getElements().size();
            List<String> roles = user.getPoints();
            for (String roleId : roles) {
                if (roleIds.contains(roleId)) {
                    roleCountMap.put(roleId, roleCountMap.getOrDefault(roleId, 0) + user.getElements().size());
                }
            }
        }

        for (Map.Entry<String, Integer> entry : roleCountMap.entrySet()) {
            String roleId = entry.getKey();
            int frequency = entry.getValue();
            double percentage = (double) frequency / totalMiningTypeObjects;
            roleFrequencyMap.put(roleId, percentage);
        }

        return roleFrequencyMap;
    }
}
