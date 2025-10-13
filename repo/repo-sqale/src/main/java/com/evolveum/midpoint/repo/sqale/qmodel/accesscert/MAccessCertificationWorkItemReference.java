/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

/**
 * Querydsl "row bean" type related to {@link QAccessCertificationWorkItemReference}.
 */
public class MAccessCertificationWorkItemReference extends MReference {

    public Long accessCertCaseCid;
    public Long accessCertWorkItemCid;

    @Override
    public String toString() {
        return "MCaseWorkItemReference{" +
                "ownerOid=" + ownerOid +
                ", accessCertCaseCid=" + accessCertCaseCid +
                ", accessCertWorkItemCid=" + accessCertWorkItemCid +
                ", referenceType=" + referenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                '}';
    }
}
