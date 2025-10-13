/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.role;

import com.evolveum.midpoint.repo.sqale.qmodel.focus.MFocus;

/**
 * Querydsl "row bean" type related to {@link QAbstractRole}.
 */
public class MAbstractRole extends MFocus {

    public Boolean autoAssignEnabled; // autoassign/enabled
    public String displayNameOrig;
    public String displayNameNorm;
    public String identifier;
    public Boolean requestable;
    public String riskLevel;
}
