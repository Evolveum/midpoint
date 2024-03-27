/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Set;

public class OutliersDescription {
    private PrismObject<UserType> user;
    private Set<OutliersResult> outliersResults;

    public OutliersDescription(PrismObject<UserType> user, Set<OutliersResult> outliersResults) {
        this.user = user;
        this.outliersResults = outliersResults;
    }

    public PrismObject<UserType> getUser() {
        return user;
    }

    public void setUser(PrismObject<UserType> user) {
        this.user = user;
    }

    public Set<OutliersResult> getOutliersResults() {
        return outliersResults;
    }

    public void setOutliersResults(Set<OutliersResult> outliersResults) {
        this.outliersResults = outliersResults;
    }
}
