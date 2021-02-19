/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

/**
 * Querydsl "row bean" type related to {@link QFocus}.
 */
public class MFocus extends MObject {

    public String costCenter;
    public String emailAddress;
    public byte[] photo;
    public String locale;
    public String localityNorm;
    public String localityOrig;
    public String preferredLanguage;
    public String telephoneNumber;
    public String timezone;
    public Instant passwordCreateTimestamp;
    public Instant passwordModifyTimestamp;
    // activation
    public Integer administrativeStatus;
    public Integer effectiveStatus;
    public Instant enableTimestamp;
    public Instant disableTimestamp;
    public String disableReason;
    public Integer validityStatus;
    public Instant validFrom;
    public Instant validTo;
    public Instant validityChangeTimestamp;
    public Instant archiveTimestamp;
}
