/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
