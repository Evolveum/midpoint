/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getRolesOidInducements;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

public class BusinessRoleApplicationDto implements Serializable {

    PrismObject<UserType> prismObjectUser;
    PrismObject<RoleType> prismRoleObject;
    List<DeltaDto> deltaDtos;
    List<ObjectDelta<? extends ObjectType>> objectDeltas;
    int assignedCount;
    boolean include;
    int unassignedCount;

    public String getClusterOid() {
        return clusterOid;
    }

    String clusterOid;

    public BusinessRoleApplicationDto(String clusterOid, PrismObject<UserType> prismObjectUser, PrismObject<RoleType> prismRoleObject,
            List<ObjectDelta<? extends ObjectType>> objectDeltas,
            int assignedCount, int unassignedCount, boolean include) {
        this.prismObjectUser = prismObjectUser;
        this.prismRoleObject = prismRoleObject;
        this.objectDeltas = objectDeltas;
        this.deltaDtos = prepareDeltaDtos(objectDeltas);
        this.assignedCount = assignedCount;
        this.unassignedCount = unassignedCount;
        this.include = include;
    }

    public BusinessRoleApplicationDto(String clusterOid, @NotNull PrismObject<UserType> prismObjectUser,
            @NotNull PrismObject<RoleType> prismObjectRole, PageBase pageBase) {
        prepareUserDeltas(prismObjectUser, prismObjectRole, pageBase);
        this.clusterOid = clusterOid;
    }

    public BusinessRoleApplicationDto(@NotNull PrismObject<UserType> prismObjectUser,
            @NotNull PrismObject<RoleType> prismObjectRole, PageBase pageBase) {
        prepareUserDeltas(prismObjectUser, prismObjectRole, pageBase);
    }

    private List<DeltaDto> prepareDeltaDtos(List<ObjectDelta<? extends ObjectType>> objectDeltas) {
        List<DeltaDto> deltaDtoList = new ArrayList<>();
        for (ObjectDelta<? extends ObjectType> objectDelta : objectDeltas) {
            deltaDtoList.add(new DeltaDto(objectDelta));
        }
        return deltaDtoList;
    }

    public void updateValue(List<AssignmentType> inducements, PageBase pageBase) {
        Set<String> inducementsOidSet = new HashSet<>();
        for (AssignmentType inducement : inducements) {
            String oid = inducement.getTargetRef().getOid();
            inducementsOidSet.add(oid);
        }
        PrismObject<RoleType> prismRoleObject = getPrismRoleObject();
        RoleType role = prismRoleObject.asObjectable();
        role.getInducement().removeIf(r -> !inducementsOidSet.contains(r.getTargetRef().getOid()));
        prepareUserDeltas(prismObjectUser, prismRoleObject, pageBase);
    }

    private void prepareUserDeltas(@NotNull PrismObject<UserType> prismObjectUser,
            @NotNull PrismObject<RoleType> prismObjectRole, PageBase pageBase) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

        UserType userObject = prismObjectUser.asObjectable();
        String userOid = userObject.getOid();

        ObjectDelta<UserType> addDelta = addRoleAssignment(userOid, prismObjectRole, pageBase);
        deltas.add(addDelta);

        // TODO consider using methods from RoleManagementUtil here

        List<String> userRolesAssignmentOids = getRolesOidAssignment(userObject);
        List<String> roleRolesAssignmentOids = getRolesOidInducements(prismObjectRole);

        int delete = 0;
        for (String assignmentOid : userRolesAssignmentOids) {
            if (roleRolesAssignmentOids.contains(assignmentOid)) {
                ObjectDelta<UserType> deleteDelta = deleteRoleAssignment(userOid, assignmentOid, pageBase);
                deltas.add(deleteDelta);
                delete++;
            }
        }
        int roleAssignmentCount = roleRolesAssignmentOids.size();

        int extraAssignmentCount = Math.max(0, roleAssignmentCount - delete);

        this.include = delete > 0;
        this.prismObjectUser = prismObjectUser;
        this.prismRoleObject = prismObjectRole;
        this.objectDeltas = deltas;
        this.deltaDtos = prepareDeltaDtos(objectDeltas);
        this.assignedCount = extraAssignmentCount;
        this.unassignedCount = delete;
    }

    private ObjectDelta<UserType> deleteRoleAssignment(
            @NotNull String userOid,
            @NotNull String roleOid, PageBase pageBase) {

        AssignmentType assignmentTo = createAssignmentTo(roleOid, ObjectTypes.ROLE, pageBase.getPrismContext());
        ObjectDelta<UserType> objectDelta = null;
        try {
            objectDelta = pageBase.getPrismContext()
                    .deltaFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT)
                    .delete(assignmentTo)
                    .asObjectDelta(userOid);
            return objectDelta;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectDelta<UserType> addRoleAssignment(@NotNull String userOid,
            @NotNull PrismObject<RoleType> prismObjectRole, PageBase pageBase) {

        AssignmentType assignmentTo = createAssignmentTo(prismObjectRole);
        ObjectDelta<UserType> objectDelta = null;
        try {
            objectDelta = pageBase.getPrismContext()
                    .deltaFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT)
                    .add(assignmentTo)
                    .asObjectDelta(userOid);
            return objectDelta;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public PrismObject<UserType> getPrismObjectUser() {
        return prismObjectUser;
    }

    public void setPrismObjectUser(PrismObject<UserType> prismObjectUser) {
        this.prismObjectUser = prismObjectUser;
    }

    public PrismObject<RoleType> getPrismRoleObject() {
        return prismRoleObject;
    }

    public void setPrismRoleObject(PrismObject<RoleType> prismRoleObject) {
        this.prismRoleObject = prismRoleObject;
    }

    public List<ObjectDelta<? extends ObjectType>> getObjectDeltas() {
        return objectDeltas;
    }

    public void setObjectDeltas(List<ObjectDelta<? extends ObjectType>> objectDeltas) {
        this.objectDeltas = objectDeltas;
    }

    public List<DeltaDto> getDeltaDtos() {
        return deltaDtos;
    }

    public void setDeltaDtos(List<DeltaDto> deltaDtos) {
        this.deltaDtos = deltaDtos;
    }

    public int getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
    }

    public int getUnassignedCount() {
        return unassignedCount;
    }

    public void setUnassignedCount(int unassignedCount) {
        this.unassignedCount = unassignedCount;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

}
