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

import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMembersList;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningUserStructure;
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

    public List<PrismObject<UserType>> filterUsers(PageBase pageBase) {

        String string = DOT_CLASS + "filterUsers";
        OperationResult result = new OperationResult(string);
        Task task = pageBase.createSimpleTask(string);

        ObjectQuery query = PrismContext.get().queryFor(UserType.class).build();

        List<PrismObject<UserType>> users = null;
        try {
            users = pageBase.getModelService().searchObjects(UserType.class, query, null, task, result);
        } catch (CommonException e) {
            e.printStackTrace();
        }

        if (users != null) {
            users.sort(Comparator.comparing(p -> p.getName().toString()));
        }

        return users;
    }

    public List<PrismObject<RoleType>> filterRoles(PageBase pageBase) {

        String string = DOT_CLASS + "filterROLES";
        OperationResult result = new OperationResult(string);
        Task task = pageBase.createSimpleTask(string);
        ObjectQuery query = PrismContext.get().queryFor(RoleType.class).build();

        List<PrismObject<RoleType>> roles = null;
        try {
            roles = pageBase.getModelService().searchObjects(RoleType.class, query, null, task, result);
        } catch (CommonException e) {
            e.printStackTrace();
        }

        if (roles != null) {
            roles.sort(Comparator.comparing(p -> p.getName().toString()));
        }

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

    public List<String> roleObjectIdRefType(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i).getOid()).collect(Collectors.toList());

    }

    public List<RoleType> getUserRoles(UserType userObject, PageBase pageBase) {
        List<RoleType> roleTypeList = new ArrayList<>();
        List<String> rolesIds = roleObjectIdRefType(userObject);
        for (int i = 0; i < rolesIds.size(); i++) {
            String oid = rolesIds.get(i);
            OperationResult result = new OperationResult(DOT_CLASS + "getRoles");
            Task task = pageBase.createSimpleTask(DOT_CLASS + "getRoles");
            try {
                roleTypeList.add(pageBase.getModelService().getObject(RoleType.class, oid, null, task, result).asObjectable());
            } catch (CommonException e) {
                e.printStackTrace();
            }
        }

        return roleTypeList;
    }

    public PrismObject<RoleType> getRoleByOid(String oid, PageBase pageBase) throws CommonException {
        String getRole = DOT_CLASS + "getRole";
        OperationResult result = new OperationResult(getRole);
        Task task = pageBase.createSimpleTask(getRole);
        return pageBase.getModelService().getObject(RoleType.class, oid, null, task, result);
    }

    public List<ObjectReferenceType> getRoleObjectReferenceTypes(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i)).collect(Collectors.toList());

    }

}
