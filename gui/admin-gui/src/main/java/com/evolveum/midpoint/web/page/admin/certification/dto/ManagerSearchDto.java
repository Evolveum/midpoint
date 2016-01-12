package com.evolveum.midpoint.web.page.admin.certification.dto;

import java.io.Serializable;

/**
 * Created by Kate on 16.12.2015.
 */
public class ManagerSearchDto implements Serializable{
    public static final String F_ORG_TYPE = "orgType";
    public static final String F_ALLOW_SELF = "allowSelf";

    private String orgType;
    private boolean allowSelf;

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
