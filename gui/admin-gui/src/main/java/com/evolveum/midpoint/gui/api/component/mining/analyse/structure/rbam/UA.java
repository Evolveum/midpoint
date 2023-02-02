/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam;

import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningStructureList;
import com.evolveum.midpoint.gui.api.component.mining.structure.UPStructure;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class UA extends Selectable<RoleMiningStructureList> implements Serializable {

    UPStructure userUP;
    HashMap<Integer, List<AuthorizationType>> candidateRoleSet;
    HashMap<Integer, List<AuthorizationType>> supportRole;
    boolean correlateStatus;

    public UA(UPStructure userUP, HashMap<Integer, List<AuthorizationType>> candidateRoleSet,HashMap<Integer, List<AuthorizationType>> supportRole,boolean correlateStatus) {
        this.userUP = userUP;
        this.candidateRoleSet = candidateRoleSet;
        this.supportRole = supportRole;
        this.correlateStatus = correlateStatus;
    }

    public UPStructure getUserUP() {
        return userUP;
    }

    public void setUserUP(UPStructure userUP) {
        this.userUP = userUP;
    }

    public HashMap<Integer, List<AuthorizationType>> getCandidateRoleSet() {
        return candidateRoleSet;
    }

    public void setCandidateRoleSet(HashMap<Integer, List<AuthorizationType>> candidateRoleSet) {
        this.candidateRoleSet = candidateRoleSet;
    }

    public HashMap<Integer, List<AuthorizationType>> getSupportRole() {
        return supportRole;
    }

    public void setSupportRole(HashMap<Integer, List<AuthorizationType>> supportRole) {
        this.supportRole = supportRole;
    }

    public boolean isCorrelateStatus() {
        return correlateStatus;
    }

    public void setCorrelateStatus(boolean correlateStatus) {
        this.correlateStatus = correlateStatus;
    }

}
