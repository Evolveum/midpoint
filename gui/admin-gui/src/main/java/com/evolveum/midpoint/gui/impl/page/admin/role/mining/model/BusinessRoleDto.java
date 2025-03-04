/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.prism.PrismContainerValue;

import org.jetbrains.annotations.NotNull;

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

/**
 * The BusinessRoleDto class represents a Data Transfer Object (DTO) that holds information
 * about a user's assignment to a specific role and associated deltas.
 */
public class BusinessRoleDto implements Serializable {

    PrismObject<UserType> prismObjectUser;

    List<DeltaDto> deltaDtos;
    List<ObjectDelta<? extends ObjectType>> objectDeltas;
    int assignedCount;
    boolean include;
    int unassignedCount;

    private Set<PrismObject<RoleType>> candidateRoles;
    private PrismObject<RoleType> prismRoleObject;

    public BusinessRoleDto(PrismObject<UserType> prismObjectUser,
            PrismObject<RoleType> prismRoleObject,
            List<ObjectDelta<? extends ObjectType>> objectDeltas,
            int assignedCount, int unassignedCount, boolean include,
            Set<PrismObject<RoleType>> candidateRoles) {
        this.prismObjectUser = prismObjectUser;
        this.prismRoleObject = prismRoleObject;
        this.objectDeltas = objectDeltas;
        this.deltaDtos = prepareDeltaDtos(objectDeltas);
        this.assignedCount = assignedCount;
        this.unassignedCount = unassignedCount;
        this.include = include;
        this.candidateRoles = candidateRoles;
    }

    public BusinessRoleDto(
            @NotNull PrismObject<UserType> prismObjectUser,
            @NotNull PrismObject<RoleType> prismObjectRole,
            Set<PrismObject<RoleType>> candidateRoles,
            PageBase pageBase) {
        this.candidateRoles = candidateRoles;
        prepareUserDeltas(prismObjectUser, prismObjectRole, pageBase);
    }

    private List<DeltaDto> prepareDeltaDtos(List<ObjectDelta<? extends ObjectType>> objectDeltas) {
        List<DeltaDto> deltaDtoList = new ArrayList<>();
        for (ObjectDelta<? extends ObjectType> objectDelta : objectDeltas) {
            deltaDtoList.add(new DeltaDto(objectDelta));
        }
        return deltaDtoList;
    }

    /**
     * Updates the value of the BusinessRoleDto object for new inducements.
     *
     * @param inducements The list of inducements to be used for updating the value.
     * @param pageBase The pageBase object.
     */
    public void updateValue(List<PrismObject<RoleType>> inducements, PageBase pageBase) {
        this.candidateRoles = new HashSet<>(inducements);
        prepareUserDeltas(prismObjectUser, prismRoleObject, pageBase);
    }

    private void prepareUserDeltas(@NotNull PrismObject<UserType> prismObjectUser,
            @NotNull PrismObject<RoleType> businessRole, PageBase pageBase) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

        UserType userObject = prismObjectUser.asObjectable();
        String userOid = userObject.getOid();

        // TODO consider using methods from RoleManagementUtil here

        List<String> userRolesAssignmentOids = getRolesOidAssignment(userObject);
        List<String> roleRolesAssignmentOids = this.candidateRoles.stream()
                .map(PrismObject::getOid)
                .toList();

        Set<String> appliedRoles = new HashSet<>();
        int delete = 0;
        Collection<PrismContainerValue<AssignmentType>> unassignRoleCollection = new ArrayList<>();
        for (String assignmentOid : userRolesAssignmentOids) {
            if (roleRolesAssignmentOids.contains(assignmentOid)) {
                appliedRoles.add(assignmentOid);
                AssignmentType assignmentTo = createAssignmentTo(assignmentOid, ObjectTypes.ROLE, pageBase.getPrismContext());
                unassignRoleCollection.add(assignmentTo.asPrismContainerValue());
                delete++;
            }
        }

        AssignmentType assignmentRole = createAssignmentTo(businessRole);
        ObjectDelta<UserType> delta = calculateUserAssignDelta(userOid, unassignRoleCollection, assignmentRole, pageBase);
        deltas.add(delta);

        int roleAssignmentCount = roleRolesAssignmentOids.size();

        int extraAssignmentCount = roleAssignmentCount - appliedRoles.size();

        this.include = delete > 0;
        this.prismObjectUser = prismObjectUser;
        this.prismRoleObject = businessRole;
        this.objectDeltas = deltas;
        this.deltaDtos = prepareDeltaDtos(objectDeltas);
        this.assignedCount = extraAssignmentCount;
        this.unassignedCount = delete;
    }

    private ObjectDelta<UserType> calculateUserAssignDelta(
            @NotNull String userOid,
            Collection<PrismContainerValue<AssignmentType>> unasignCollection, AssignmentType assignRole, PageBase pageBase) {

        ObjectDelta<UserType> objectDelta;
        try {
            objectDelta = pageBase.getPrismContext()
                    .deltaFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT)
                    .delete(unasignCollection)
                    .add(assignRole)
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
