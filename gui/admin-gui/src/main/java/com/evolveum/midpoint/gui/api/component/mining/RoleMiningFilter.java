/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.page.PageBase;
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
        }

        return roleMemberLists;
    }

    public List<RoleMiningUserStructure> filterUsersRoles(List<PrismObject<UserType>> users) {
        List<RoleMiningUserStructure> list = new ArrayList<>();

        for (PrismObject<UserType> user : users) {
            AssignmentHolderType assignmentHolderType = user.asObjectable();
            list.add(new RoleMiningUserStructure(user, roleObjectIdRefType(assignmentHolderType)));
        }
        return list;
    }

    private List<PrismObject<UserType>> getMembers(ModelService modelService,
            Task task, OperationResult result, String objectId) throws CommonException {
            ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(objectId).build();

        return modelService.searchObjects(UserType.class, query, null, task, result);
    }


    public List<PrismObject<UserType>> getRoleMembers(PageBase pageBase, String objectId) {
        String getMembers = DOT_CLASS + "getRolesMembers";
        OperationResult result = new OperationResult(getMembers);
        Task task = pageBase.createSimpleTask(getMembers);

        ObjectQuery query = pageBase.getPrismContext().queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(objectId).build();

        try {
            return pageBase.getModelService().searchObjects(UserType.class, query, null, task, result);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }
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


    protected PrismObject<RoleType> getRoleByOid(String oid, PageBase pageBase) throws CommonException {
        String getRole = DOT_CLASS + "getRole";
        OperationResult result = new OperationResult(getRole);
        Task task = pageBase.createSimpleTask(getRole);
        return pageBase.getModelService().getObject(RoleType.class, oid, null, task, result);
    }

    protected List<ObjectReferenceType> getRoleObjectReferenceTypes(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i)).collect(Collectors.toList());

    }

}
