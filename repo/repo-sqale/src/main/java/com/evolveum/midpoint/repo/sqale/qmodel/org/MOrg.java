/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.org;

import com.evolveum.midpoint.repo.sqale.qmodel.role.MAbstractRole;

/**
 * Querydsl "row bean" type related to {@link QOrg}.
 */
public class MOrg extends MAbstractRole {

    public Integer displayOrder;
    public Boolean tenant;
}
