/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class CostResultSingle extends Selectable<CostResultSingle> implements Serializable {

    public static final String F_NAME_USER_TYPE = "userObjectType";
    public static final String F_ROLE_COST = "reduceValue";

    UserType userObjectType;
    List<RoleType> userOriginalRoles;
    List<String> userPossibleRoles;
    CandidateRole candidateRole;
    double reduceValue;
    boolean applied;

    public CostResultSingle(UserType userObjectType, List<RoleType> userOriginalRoles,
            List<String> userPossibleRoles, double reduceValue, CandidateRole candidateRole, boolean applied) {
        this.userObjectType = userObjectType;
        this.userOriginalRoles = userOriginalRoles;
        this.userPossibleRoles = userPossibleRoles;
        this.reduceValue = reduceValue;
        this.candidateRole = candidateRole;
        this.applied = applied;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public CandidateRole getCandidateRole() {
        return candidateRole;
    }

    public void setCandidateRole(CandidateRole candidateRole) {
        this.candidateRole = candidateRole;
    }

    public UserType getUserObjectType() {
        return userObjectType;
    }

    public void setUserObjectType(UserType userObjectType) {
        this.userObjectType = userObjectType;
    }

    public List<RoleType> getUserOriginalRoles() {
        return userOriginalRoles;
    }

    public void setUserOriginalRoles(List<RoleType> userOriginalRoles) {
        this.userOriginalRoles = userOriginalRoles;
    }

    public List<String> getUserPossibleRoles() {
        return userPossibleRoles;
    }

    public void setUserPossibleRoles(List<String> userPossibleRoles) {
        this.userPossibleRoles = userPossibleRoles;
    }

    public double getReduceValue() {
        return reduceValue;
    }

    public void setReduceValue(double reduceValue) {
        this.reduceValue = reduceValue;
    }

}
