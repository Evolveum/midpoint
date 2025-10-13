/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.org;

import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QOrgClosure}.
 */
public class MOrgClosure {

    public UUID ancestorOid;
    public UUID descendantOid;

    @Override
    public String toString() {
        return "MOrgClosure{" + ancestorOid + " -> " + descendantOid + '}';
    }
}
