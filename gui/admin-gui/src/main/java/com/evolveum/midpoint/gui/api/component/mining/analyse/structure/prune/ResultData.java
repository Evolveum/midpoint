/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class ResultData extends Selectable<ResultData> implements Serializable {

    public static final String F_NAME_USER_TYPE = "userObjectType";
    public static final String F_ROLE_COST = "reduceValue";
    public static final String F_STATUS = "reduceValue";

    UserType userObjectType;
    List<AuthorizationType> userAuthorizations;
    List<RoleType> roleAssign;
    List<List<AuthorizationType>> candidatePermissionRole;
    List<List<RoleType>> candidateRoles;
    boolean correlateStatus;
    List<Integer> indices;
    double reduceValue;


    public ResultData(UserType userObjectType,double reduceValue, List<AuthorizationType> userAuthorizations, List<RoleType> roleAssign,
            List<List<AuthorizationType>> candidatePermissionRole,List<Integer> indices, boolean correlateStatus) {
        this.userObjectType = userObjectType;
        this.reduceValue = reduceValue;
        this.userAuthorizations = userAuthorizations;
        this.roleAssign = roleAssign;
        this.candidatePermissionRole = candidatePermissionRole;
        this.indices = indices;
        this.correlateStatus = correlateStatus;
    }

    public ResultData(UserType userObjectType,double reduceValue, List<AuthorizationType> userAuthorizations, List<RoleType> roleAssign,
            List<List<RoleType>> candidateRoles, boolean correlateStatus,List<Integer> indices) {
        this.userObjectType = userObjectType;
        this.reduceValue = reduceValue;
        this.userAuthorizations = userAuthorizations;
        this.roleAssign = roleAssign;
        this.candidateRoles = candidateRoles;
        this.indices = indices;
        this.correlateStatus = correlateStatus;

    }

    public double getReduceValue() {
        return reduceValue;
    }

    public void setReduceValue(double reduceValue) {
        this.reduceValue = reduceValue;
    }

    public List<List<RoleType>> getCandidateRoles() {
        return candidateRoles;
    }

    public void setCandidateRoles(List<List<RoleType>> candidateRoles) {
        this.candidateRoles = candidateRoles;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public void setIndices(List<Integer> indices) {
        this.indices = indices;
    }

    public UserType getUserObjectType() {
        return userObjectType;
    }

    public void setUserObjectType(UserType userObjectType) {
        this.userObjectType = userObjectType;
    }

    public List<AuthorizationType> getUserAuthorizations() {
        return userAuthorizations;
    }

    public void setUserAuthorizations(List<AuthorizationType> userAuthorizations) {
        this.userAuthorizations = userAuthorizations;
    }

    public List<RoleType> getRoleAssign() {
        return roleAssign;
    }

    public void setRoleAssign(List<RoleType> roleAssign) {
        this.roleAssign = roleAssign;
    }

    public List<List<AuthorizationType>> getCandidatePermissionRole() {
        return candidatePermissionRole;
    }

    public void setCandidatePermissionRole(List<List<AuthorizationType>> candidatePermissionRole) {
        this.candidatePermissionRole = candidatePermissionRole;
    }

    public boolean isCorrelateStatus() {
        return correlateStatus;
    }

    public void setCorrelateStatus(boolean correlateStatus) {
        this.correlateStatus = correlateStatus;
    }

}
