/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import java.io.Serializable;
import java.util.List;

public class CostResult extends Selectable<CostResult> implements Serializable {

    public static final String F_ROLE_KEY = "roleKey";
    public static final String F_REDUCED_VALUE = "reduceValue";
    public static final String F_ROLE_COST = "roleCost";
    public static final String F_ROLE_DEGREE = "roleDegree";

    int roleKey;
    int roleDegree;
    double roleCost;
    double reduceValue;
    CandidateRole candidateRole;
    List<UrType> candidateUserRole;
    List<RoleType> equivalentRoles;

    public CostResult(int roleKey, int roleDegree, double roleCost, double reduceValue, List<UrType> candidateUserRole,
            CandidateRole candidateRole, List<RoleType> equivalentRoles) {
        this.roleKey = roleKey;
        this.roleDegree = roleDegree;
        this.roleCost = roleCost;
        this.reduceValue = reduceValue;
        this.candidateUserRole = candidateUserRole;
        this.candidateRole = candidateRole;
        this.equivalentRoles = equivalentRoles;
    }

    public List<RoleType> getEquivalentRoles() {
        return equivalentRoles;
    }

    public void setEquivalentRoles(List<RoleType> equivalentRoles) {
        this.equivalentRoles = equivalentRoles;
    }

    public CandidateRole getCandidateRole() {
        return candidateRole;
    }

    public void setCandidateRole(CandidateRole candidateRole) {
        this.candidateRole = candidateRole;
    }

    public List<UrType> getCandidateUserRole() {
        return candidateUserRole;
    }

    public void setCandidateUserRole(List<UrType> candidateUserRole) {
        this.candidateUserRole = candidateUserRole;
    }

    public int getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(int roleKey) {
        this.roleKey = roleKey;
    }

    public int getRoleDegree() {
        return roleDegree;
    }

    public void setRoleDegree(int roleDegree) {
        this.roleDegree = roleDegree;
    }

    public double getRoleCost() {
        return roleCost;
    }

    public void setRoleCost(double roleCost) {
        this.roleCost = roleCost;
    }

    public double getReduceValue() {
        return reduceValue;
    }

    public void setReduceValue(double reduceValue) {
        this.reduceValue = reduceValue;
    }

}
