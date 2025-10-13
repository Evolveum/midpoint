/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * Querydsl "row bean" type related to {@link QFocus}.
 */
public class MFocus extends MObject {

    public String costCenter;
    public String emailAddress;
    public byte[] photo;
    public String locale;
    public String localityOrig;
    public String localityNorm;
    public String preferredLanguage;
    public String telephoneNumber;
    public String timezone;
    // credential/password/metadata
    public Instant passwordCreateTimestamp;
    public Instant passwordModifyTimestamp;
    // activation
    public ActivationStatusType administrativeStatus;
    public ActivationStatusType effectiveStatus;
    public Instant enableTimestamp;
    public Instant disableTimestamp;
    public String disableReason;
    public TimeIntervalStatusType validityStatus;
    public Instant validFrom;
    public Instant validTo;
    public Instant validityChangeTimestamp;
    public Instant archiveTimestamp;
    public LockoutStatusType lockoutStatus;
    public Jsonb normalizedData;
}
