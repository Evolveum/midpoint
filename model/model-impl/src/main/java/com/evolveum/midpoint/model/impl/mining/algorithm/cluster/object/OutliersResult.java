/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import java.io.Serializable;

public class OutliersResult implements Serializable {
    private PrismObject<RoleType> roleOid;
    private double confidence;
    private OutlierStatus outlierStatus;

    public OutliersResult(PrismObject<RoleType> roleOid, double confidence, OutlierStatus outlierStatus) {
        this.roleOid = roleOid;
        this.confidence = confidence;
        this.outlierStatus = outlierStatus;
    }

    public PrismObject<RoleType> getRoleOid() {
        return roleOid;
    }

    public void setRoleOid(PrismObject<RoleType> roleOid) {
        this.roleOid = roleOid;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public OutlierStatus getOutlierStatus() {
        return outlierStatus;
    }

    public void setOutlierStatus(OutlierStatus outlierStatus) {
        this.outlierStatus = outlierStatus;
    }

    // Enum to represent outlier status
    public enum OutlierStatus {
        OUTLIER,
        NON_OUTLIER,
        GOLDEN_PROTECTED
    }
}
