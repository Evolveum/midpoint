/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qbean;

import com.evolveum.midpoint.repo.sqale.qmodel.QReport;

/**
 * Querydsl "row bean" type related to {@link QReport}.
 */
public class MReport extends MObject {
    public Integer export;
    public Integer orientation;
    public Boolean parent;
    public Boolean useHibernateSession;
}
