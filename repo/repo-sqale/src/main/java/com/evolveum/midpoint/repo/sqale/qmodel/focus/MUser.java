/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;

/**
 * Querydsl "row bean" type related to {@link QUser}.
 */
public class MUser extends MFocus {

    public String additionalNameOrig;
    public String additionalNameNorm;
    public String employeeNumber;
    public String familyNameOrig;
    public String familyNameNorm;
    public String fullNameOrig;
    public String fullNameNorm;
    public String givenNameOrig;
    public String givenNameNorm;
    public String honorificPrefixOrig;
    public String honorificPrefixNorm;
    public String honorificSuffixOrig;
    public String honorificSuffixNorm;
    public String nickNameOrig;
    public String nickNameNorm;
    public String personalNumber;
    public String titleOrig;
    public String titleNorm;
    public Jsonb organizations;
    public Jsonb organizationUnits;
}
