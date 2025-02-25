/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCandidateRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

public class RoleAnalysisCandidateRolesDto implements Serializable {

    List<RoleType> roles = new ArrayList<>();
    HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate = new HashMap<>();
    ObjectReferenceType clusterRef;
    ObjectReferenceType sessionRef;

    public RoleAnalysisCandidateRolesDto(RoleAnalysisService roleAnalysisService,
                                         RoleAnalysisClusterType cluster,
                                         Task task, OperationResult result) {

        init(roleAnalysisService, cluster, task, result);
    }

    public void init(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
        loadCandidateRoles(roleAnalysisService, candidateRoles, cacheCandidate, roles, task, result);

        this.sessionRef = cluster.getRoleAnalysisSessionRef();
        this.clusterRef = createObjectRef(cluster);
    }

    private static void loadCandidateRoles(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<RoleAnalysisCandidateRoleType> candidateRoles,
            @NotNull HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            @NotNull List<RoleType> roles,
            @NotNull Task task,
            @NotNull OperationResult result) {
        for (RoleAnalysisCandidateRoleType candidateRoleType : candidateRoles) {
            ObjectReferenceType candidateRoleRef = candidateRoleType.getCandidateRoleRef();
            PrismObject<RoleType> role = roleAnalysisService.getRoleTypeObject(candidateRoleRef.getOid(), task, result);
            if (Objects.nonNull(role)) {
                cacheCandidate.put(candidateRoleRef.getOid(), candidateRoleType);
                roles.add(role.asObjectable());
            }
        }
    }

    public List<RoleType> getRoles() {
        return roles;
    }

    public RoleAnalysisCandidateRoleType getCandidateRole(String oid) {
        return cacheCandidate.get(oid);
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }

}
