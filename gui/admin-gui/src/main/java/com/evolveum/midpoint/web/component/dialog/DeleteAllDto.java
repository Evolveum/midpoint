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
package com.evolveum.midpoint.web.component.dialog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *  @author shood
 * */
public class DeleteAllDto implements Serializable{

    public static final String F_USERS = "deleteUsers";
    public static final String F_ORGS = "deleteOrgs";
    public static final String F_ACC_SHADOW = "deleteAccountShadow";
    public static final String F_ORG_SHADOW = "deleteOrgShadow";
    public static final String F_ROLE_SHADOW = "deleteRoleShadow";

    private Boolean deleteUsers = false;
    private Boolean deleteOrgs = false;
    private Boolean deleteAccountShadow = false;
    private Boolean deleteRoleShadow = false;
    private Boolean deleteOrgShadow = false;

    private Map<String, String> resourceFocusMap = new HashMap<>();

    public Map<String, String> getResourceFocusMap() {
        return resourceFocusMap;
    }

    public void setResourceFocusMap(Map<String, String> resourceFocusMap) {
        this.resourceFocusMap = resourceFocusMap;
    }

    public Boolean getDeleteUsers() {
        return deleteUsers;
    }

    public void setDeleteUsers(Boolean deleteUsers) {
        this.deleteUsers = deleteUsers;
    }

    public Boolean getDeleteOrgs() {
        return deleteOrgs;
    }

    public void setDeleteOrgs(Boolean deleteOrgs) {
        this.deleteOrgs = deleteOrgs;
    }

    public Boolean getDeleteAccountShadow() {
        return deleteAccountShadow;
    }

    public void setDeleteAccountShadow(Boolean deleteAccountShadow) {
        this.deleteAccountShadow = deleteAccountShadow;
    }

    public Boolean getDeleteRoleShadow() {
        return deleteRoleShadow;
    }

    public void setDeleteRoleShadow(Boolean deleteRoleShadow) {
        this.deleteRoleShadow = deleteRoleShadow;
    }

    public Boolean getDeleteOrgShadow() {
        return deleteOrgShadow;
    }

    public void setDeleteOrgShadow(Boolean deleteOrgShadow) {
        this.deleteOrgShadow = deleteOrgShadow;
    }
}
