/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PruneOptimizations {

    public double jaccardIndex(List<PrismObject<UserType>> roleMembersA, List<PrismObject<UserType>> roleMembersB,
            int minUsersOccupations) {

        if (roleMembersA == null || roleMembersB == null) {
            return 0.0;
        }
        int totalSum = roleMembersA.size() + roleMembersB.size();
        if (totalSum == 0) {
            return 0.0;
        }
        int intersectionSum = 0;

        for (PrismObject<UserType> user : roleMembersA) {
            if (roleMembersB.contains(user)) {
                intersectionSum++;
            }
        }

        if (intersectionSum < minUsersOccupations) {
            return 0.0;
        }

        return (double) (intersectionSum) / (totalSum - intersectionSum);
    }

}
