/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lazyman
 */
public class PersonalInfoDto implements Serializable {

    public static final String F_SEQUENCE_IDENTIFIER = "sequenceIdentifier";
    public static final String F_MODULE_IDENTIFIER = "moduleIdentifier";
    public static final String F_CHANNEL_ID = "channelId";
    public static final String F_LAST_LOGIN_DATE = "lastLoginDate";
    public static final String F_LAST_LOGIN_FROM = "lastLoginFrom";
    public static final String F_LAST_FAIL_DATE = "lastFailDate";
    public static final String F_LAST_FAIL_FROM = "lastFailFrom";
    public static final String F_PASSWORD_EXP = "passwordExp";

    private String sequenceIdentifier;
    private Date lastLoginDate;
    private String lastLoginFrom;
    private Date lastFailDate;
    private String lastFailFrom;
    private Date passwordExp;

    public Date getLastFailDate() {
        return lastFailDate;
    }

    public String getLastFailFrom() {
        return lastFailFrom;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public String getLastLoginFrom() {
        return lastLoginFrom;
    }

    public Date getPasswordExp() {
        return passwordExp;
    }

    public void setLastFailDate(Date lastFailDate) {
        this.lastFailDate = lastFailDate;
    }

    public void setLastFailFrom(String lastFailFrom) {
        this.lastFailFrom = lastFailFrom;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public void setLastLoginFrom(String lastLoginFrom) {
        this.lastLoginFrom = lastLoginFrom;
    }

    public void setPasswordExp(Date passwordExp) {
        this.passwordExp = passwordExp;
    }

    public String getSequenceIdentifier() {
        return sequenceIdentifier;
    }

    public void setSequenceIdentifier(String sequenceIdentifier) {
        this.sequenceIdentifier = sequenceIdentifier;
    }

}
