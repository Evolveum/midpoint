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
    public static final String F_NON_ACC_SHADOW = "deleteNonAccountShadow";
//    public static final String F_ORG_SHADOW = "deleteOrgShadow";
//    public static final String F_ROLE_SHADOW = "deleteRoleShadow";

    private boolean deleteUsers = false;
    private boolean deleteOrgs = false;
    private boolean deleteAccountShadow = false;
    private boolean deleteNonAccountShadow = false;
//    private boolean deleteRoleShadow = false;
//    private boolean deleteOrgShadow = false;

    private int objectsToDelete = 0;
    private int accountShadowCount = 0;
    private int nonAccountShadowCount = 0;
    private int orgUnitCount = 0;
    private int userCount = 0;
//    private int orgShadowCount = 0;
//    private int roleShadowCount = 0;

    private Map<String, String> resourceFocusMap = new HashMap<>();

    public int getNonAccountShadowCount() {
        return nonAccountShadowCount;
    }

    public void setNonAccountShadowCount(int nonAccountShadowCount) {
        this.nonAccountShadowCount = nonAccountShadowCount;
    }

    public boolean getDeleteNonAccountShadow() {
        return deleteNonAccountShadow;
    }

    public void setDeleteNonAccountShadow(boolean deleteNonAccountShadow) {
        this.deleteNonAccountShadow = deleteNonAccountShadow;
    }

    public Map<String, String> getResourceFocusMap() {
        return resourceFocusMap;
    }

    public void setResourceFocusMap(Map<String, String> resourceFocusMap) {
        this.resourceFocusMap = resourceFocusMap;
    }

    public boolean getDeleteUsers() {
        return deleteUsers;
    }

    public void setDeleteUsers(boolean deleteUsers) {
        this.deleteUsers = deleteUsers;
    }

    public boolean getDeleteOrgs() {
        return deleteOrgs;
    }

    public void setDeleteOrgs(boolean deleteOrgs) {
        this.deleteOrgs = deleteOrgs;
    }

    public boolean getDeleteAccountShadow() {
        return deleteAccountShadow;
    }

    public void setDeleteAccountShadow(boolean deleteAccountShadow) {
        this.deleteAccountShadow = deleteAccountShadow;
    }

//    public boolean getDeleteRoleShadow() {
//        return deleteRoleShadow;
//    }

//    public void setDeleteRoleShadow(boolean deleteRoleShadow) {
//        this.deleteRoleShadow = deleteRoleShadow;
//    }

//    public boolean getDeleteOrgShadow() {
//        return deleteOrgShadow;
//    }

//    public void setDeleteOrgShadow(boolean deleteOrgShadow) {
//        this.deleteOrgShadow = deleteOrgShadow;
//    }

    public int getObjectsToDelete() {
        return objectsToDelete;
    }

    public void setObjectsToDelete(int objectsToDelete) {
        this.objectsToDelete = objectsToDelete;
    }

    public int getAccountShadowCount() {
        return accountShadowCount;
    }

    public void setAccountShadowCount(int accountShadowCount) {
        this.accountShadowCount = accountShadowCount;
    }

    public int getOrgUnitCount() {
        return orgUnitCount;
    }

    public void setOrgUnitCount(int orgUnitCount) {
        this.orgUnitCount = orgUnitCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

//    public int getOrgShadowCount() {
//        return orgShadowCount;
//    }

//    public void setOrgShadowCount(int orgShadowCount) {
//        this.orgShadowCount = orgShadowCount;
//    }

//    public int getRoleShadowCount() {
//        return roleShadowCount;
//    }

//    public void setRoleShadowCount(int roleShadowCount) {
//        this.roleShadowCount = roleShadowCount;
//    }
}
