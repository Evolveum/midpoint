/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.mining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleMiningFilter implements Serializable {

    public List<PrismObject<UserType>> filterUsers(ModelService modelService,
            Task task, OperationResult result) throws CommonException {

        ObjectQuery query = PrismContext.get().queryFor(UserType.class).build();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        users.sort(Comparator.comparing(p -> p.getName().toString()));

        return users;
    }

    public List<PrismObject<RoleType>> filterRoles(ModelService modelService,
            Task task, OperationResult result) throws CommonException {

        ObjectQuery query = PrismContext.get().queryFor(RoleType.class).build();

        List<PrismObject<RoleType>> roles = modelService.searchObjects(RoleType.class, query, null, task, result);

        roles.sort(Comparator.comparing(p -> p.getName().toString()));

        return roles;
    }

    public List<RoleMembersList> filterRolesMembers(List<PrismObject<RoleType>> roles,
            ModelService modelService, Task task, OperationResult result) throws CommonException {

        List<RoleMembersList> roleMemberLists = new ArrayList<>();

        for (PrismObject<RoleType> role : roles) {
            roleMemberLists.add(new RoleMembersList(role, getMembers(modelService, task, result, role.getOid())));
            System.out.println(role);
        }

        return roleMemberLists;
    }

    public List<UserRolesList> filterUsersRoles(List<PrismObject<UserType>> users) {
        List<UserRolesList> list = new ArrayList<>();

        for (PrismObject<UserType> user : users) {
            AssignmentHolderType assignmentHolderType = user.asObjectable();
            list.add(new UserRolesList(user, roleObjectIdRefType(assignmentHolderType)));
        }
        return list;
    }

    private List<PrismObject<UserType>> getMembers(ModelService modelService,
            Task task, OperationResult result, String objectId) throws CommonException {
        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(objectId).build();

        return modelService.searchObjects(UserType.class, query, null, task, result);
    }

    public List<AuthorizationType> getAuthorization(RoleType roleTypePrismObject) {
        return roleTypePrismObject.getAuthorization();
    }

    public ArrayList<Integer> coordinates(int xCoordinate, int yCoordinate) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(xCoordinate);
        arrayList.add(yCoordinate);
        return arrayList;
    }

    public List<String> roleObjectIdRefType(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i).getOid()).collect(Collectors.toList());

    }

}
