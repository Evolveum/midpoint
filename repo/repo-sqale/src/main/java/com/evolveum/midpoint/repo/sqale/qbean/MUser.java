/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qbean;

import com.evolveum.midpoint.repo.sqale.qmodel.QUser;

/**
 * Querydsl "row bean" type related to {@link QUser}.
 */
public class MUser extends MFocus {

    public String additionalNameNorm;
    public String additionalNameOrig;
    public String employeeNumber;
    public String familyNameNorm;
    public String familyNameOrig;
    public String fullNameNorm;
    public String fullNameOrig;
    public String givenNameNorm;
    public String givenNameOrig;
    public String honorificPrefixNorm;
    public String honorificPrefixOrig;
    public String honorificSuffixNorm;
    public String honorificSuffixOrig;
    public String nickNameNorm;
    public String nickNameOrig;
    public String titleNorm;
    public String titleOrig;
}
