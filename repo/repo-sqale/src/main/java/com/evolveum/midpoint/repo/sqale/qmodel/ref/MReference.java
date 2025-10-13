/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

/**
 * Querydsl "row bean" type related to {@link QReference} and its subtypes.
 * This also works as "MObjectReference" as it does not need additional attributes.
 */
public class MReference {

    public UUID ownerOid;
    public MObjectType ownerType;
    public MReferenceType referenceType;
    public UUID targetOid;
    public MObjectType targetType;
    public Integer relationId;

    @Override
    public String toString() {
        return SqaleUtils.toString(this);
    }
}
