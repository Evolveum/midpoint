/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.MObjectType;

/**
 * Querydsl "row bean" type related to {@link QReference} and its subtypes.
 * This also works as "MObjectReference" as it does not need additional attributes.
 */
public class MReference {

    public UUID ownerOid;
    public MReferenceType referenceType;
    public UUID targetOid;
    public MObjectType targetType;
    public Integer relationId;

    @Override
    public String toString() {
        return "MReference{" +
                "ownerOid=" + ownerOid +
                ", referenceType=" + referenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                '}';
    }
}
