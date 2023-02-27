/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningUserStructure;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleMiningFilter implements Serializable {

    public List<PrismObject<UserType>> filterUsers(PageBase pageBase) {

        String string = DOT_CLASS + "filterUsers";
        OperationResult result = new OperationResult(string);

        List<PrismObject<UserType>> users = null;
        try {
            users = pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(UserType.class, null, null, result);
        } catch (CommonException e) {
            e.printStackTrace();
        }

        if (users != null) {
            users.sort(Comparator.comparing(p -> p.getName().toString()));
        }

        return users;
    }

    public List<PrismObject<RoleType>> filterRoles(PageBase pageBase) {

        String string = DOT_CLASS + "filterRoles";
        OperationResult result = new OperationResult(string);

        List<PrismObject<RoleType>> roles = null;
        try {
            roles = pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(RoleType.class, null, null, result);
        } catch (CommonException e) {
            e.printStackTrace();
        }

        if (roles != null) {
            roles.sort(Comparator.comparing(p -> p.getName().toString()));
        }

        return roles;
    }

    public List<RoleMiningUserStructure> filterUsersRoles(List<PrismObject<UserType>> users) {
        List<RoleMiningUserStructure> list = new ArrayList<>();

        for (PrismObject<UserType> user : users) {
            AssignmentHolderType assignmentHolderType = user.asObjectable();
            list.add(new RoleMiningUserStructure(user, roleObjectIdRefType(assignmentHolderType)));
        }
        return list;
    }

    public List<PrismObject<UserType>> getRoleMembers(PageBase pageBase, String objectId) {
        String getMembers = DOT_CLASS + "getRolesMembers";
        OperationResult result = new OperationResult(getMembers);

        ObjectQuery query = pageBase.getPrismContext().queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(objectId).build();

        try {
            return pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(UserType.class, query, null, result);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }
    }

    public Integer getRoleMembersCount(PageBase pageBase, String objectId) {
        String getMembers = DOT_CLASS + "getRolesMembers";
        OperationResult result = new OperationResult(getMembers);

        ObjectQuery query = pageBase.getPrismContext().queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(objectId).build();

        try {
            return pageBase.getMidpointApplication().getRepositoryService()
                    .countObjects(UserType.class, query, null, result);
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
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i).getOid()).
                collect(Collectors.toList());

    }

    public List<RoleType> getUserRoles(UserType userObject, PageBase pageBase) {
        List<RoleType> roleTypeList = new ArrayList<>();
        List<String> rolesIds = roleObjectIdRefType(userObject);
        OperationResult result = new OperationResult(DOT_CLASS + "getRoles");
        for (String oid : rolesIds) {
            try {
                roleTypeList.add(pageBase.getMidpointApplication().getRepositoryService()
                        .getObject(RoleType.class, oid, null, result).asObjectable());
            } catch (CommonException e) {
                e.printStackTrace();
            }
        }

        return roleTypeList;
    }

    //O(K * N)
    public List<AuthorizationType> getUserAuthorizations(List<String> rolesIds, PageBase pageBase) {
        Set<AuthorizationType> authorizationTypeSet = new HashSet<>();
        OperationResult result = new OperationResult(DOT_CLASS + "getRoles");

        for (String oid : rolesIds) {
            try {
                List<AuthorizationType> authorization = pageBase.getMidpointApplication().getRepositoryService()
                        .getObject(RoleType.class, oid, null, result).asObjectable().getAuthorization();
                authorizationTypeSet.addAll(authorization);
            } catch (CommonException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>(authorizationTypeSet);
    }

    //O(K * N)
    public List<AuthorizationType> getUserAuthorizationsR(List<RoleType> roles, PageBase pageBase) {
        Set<AuthorizationType> authorizationTypeSet = new HashSet<>();
        OperationResult result = new OperationResult(DOT_CLASS + "getRoles");

        for (RoleType roleType : roles) {
            try {
                List<AuthorizationType> authorization = pageBase.getMidpointApplication().getRepositoryService()
                        .getObject(RoleType.class, roleType.getOid(), null, result).asObjectable().getAuthorization();
                authorizationTypeSet.addAll(authorization);
            } catch (CommonException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>(authorizationTypeSet);
    }

    public PrismObject<RoleType> getRoleByOid(String oid, PageBase pageBase) {
        String getRole = DOT_CLASS + "getRole";
        OperationResult result = new OperationResult(getRole);
        try {
            return pageBase.getMidpointApplication().getRepositoryService()
                    .getObject(RoleType.class, oid, null, result);
        } catch (CommonException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ObjectReferenceType> getRoleObjectReferenceTypes(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i)).collect(Collectors.toList());

    }

}
