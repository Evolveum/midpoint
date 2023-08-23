/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class BusinessRoleApplicationDto implements Serializable {

    PrismObject<RoleAnalysisClusterType> cluster;
    PrismObject<RoleType> businessRole;
    List<BusinessRoleDto> businessRoleDtos;

    public BusinessRoleApplicationDto(PrismObject<RoleAnalysisClusterType> cluster, PrismObject<RoleType> businessRole,
            List<BusinessRoleDto> businessRoleDtos) {
        this.cluster = cluster;
        this.businessRole = businessRole;
        this.businessRoleDtos = businessRoleDtos;
    }


    public void updateValue(List<AssignmentType> inducements) {
        Set<String> inducementsOidSet = new HashSet<>();
        for (AssignmentType inducement : inducements) {
            String oid = inducement.getTargetRef().getOid();
            inducementsOidSet.add(oid);
        }
        PrismObject<RoleType> prismRoleObject = getBusinessRole();
        RoleType role = prismRoleObject.asObjectable();
        role.getInducement().removeIf(r -> !inducementsOidSet.contains(r.getTargetRef().getOid()));
    }

    public PrismObject<RoleAnalysisClusterType> getCluster() {
        return cluster;
    }

    public void setCluster(PrismObject<RoleAnalysisClusterType> cluster) {
        this.cluster = cluster;
    }

    public PrismObject<RoleType> getBusinessRole() {
        return businessRole;
    }

    public void setBusinessRole(PrismObject<RoleType> businessRole) {
        this.businessRole = businessRole;
    }

    public List<BusinessRoleDto> getBusinessRoleDtos() {
        return businessRoleDtos;
    }

    public void setBusinessRoleDtos(List<BusinessRoleDto> businessRoleDtos) {
        this.businessRoleDtos = businessRoleDtos;
    }

}
