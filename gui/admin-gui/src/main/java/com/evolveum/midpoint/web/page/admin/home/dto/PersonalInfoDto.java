/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lazyman
 */
public class PersonalInfoDto implements Serializable {

    public static final String F_LAST_LOGIN_DATE = "lastLoginDate";
    public static final String F_LAST_LOGIN_FROM = "lastLoginFrom";
    public static final String F_LAST_FAIL_DATE = "lastFailDate";
    public static final String F_LAST_FAIL_FROM = "lastFailFrom";
    public static final String F_PASSWORD_EXP = "passwordExp";

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
}
