/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import com.evolveum.midpoint.repo.sqale.qmodel.focus.MFocus;

/**
 * Querydsl "row bean" type related to {@link QAbstractRole}.
 */
public class MAbstractRole extends MFocus {

    public Boolean autoAssignEnabled;
    public String displayNameOrig;
    public String displayNameNorm;
    public String identifier;
    public Boolean requestable;
    public String riskLevel;
}
