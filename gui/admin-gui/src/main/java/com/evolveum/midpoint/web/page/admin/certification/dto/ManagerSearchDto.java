/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ManagerSearchType;

import java.io.Serializable;

/**
 * Created by Kate on 16.12.2015.
 */
public class ManagerSearchDto implements Serializable{
    public static final String F_ORG_TYPE = "orgType";
    public static final String F_ALLOW_SELF = "allowSelf";

    private String orgType;
    private boolean allowSelf;

    public ManagerSearchDto(ManagerSearchType manager) {
        if (manager != null) {
            orgType = manager.getOrgType();
            allowSelf = Boolean.TRUE.equals(manager.isAllowSelf());
        }
    }

    public String getOrgType() {
        return orgType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    public boolean isAllowSelf() {
        return allowSelf;
    }

    public void setAllowSelf(boolean allowSelf) {
        this.allowSelf = allowSelf;
    }
}
