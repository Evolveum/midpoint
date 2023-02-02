/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.temp.RM;
import com.evolveum.midpoint.gui.api.component.mining.temp.US;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractMutableObjectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class JaccardSortingMethod {
    PageBase pageBase;

    public JaccardSortingMethod(PageBase pageBase) {
        this.pageBase = pageBase;
        fillDataStructures(pageBase);
    }

    List<RM> rolesStructure = new ArrayList<>();
    List<US> usersStructure = new ArrayList<>();
    List<UserType> userObjectList = new ArrayList<>();

    private void fillDataStructures(PageBase pageBase) {
        rolesStructure = new ArrayList<>();
        List<PrismObject<RoleType>> prismObjectsRoles = new RoleMiningFilter().filterRoles(pageBase);

        for (int i = 0; i < prismObjectsRoles.size(); i++) {
            RoleType roleType = prismObjectsRoles.get(i).asObjectable();
            List<PrismObject<UserType>> roleMembers = new RoleMiningFilter().getRoleMembers(pageBase, roleType.getOid());
            List<UserType> roleMembersObject = roleMembers.stream()
                    .map(roleMember -> roleMember.asObjectable())
                    .collect(Collectors.toList());

            List<AuthorizationType> authorization = roleType.getAuthorization();
            rolesStructure.add(new RM(roleType, roleMembersObject, authorization));
        }

        usersStructure = new ArrayList<>();

        List<PrismObject<UserType>> prismObjectsUsers = new RoleMiningFilter().filterUsers(pageBase);

        for (int i = 0; i < prismObjectsUsers.size(); i++) {
            userObjectList.add(prismObjectsUsers.get(i).asObjectable());
        }

        for (int i = 0; i < prismObjectsUsers.size(); i++) {
            UserType userType = prismObjectsUsers.get(i).asObjectable();
            List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userType, pageBase);
            List<AuthorizationType> authorizationTypes = new ArrayList<>();
            for (int j = 0; j < userRoles.size(); j++) {
                List<AuthorizationType> authorization = userRoles.get(j).getAuthorization();
                for (int a = 0; a < authorization.size(); a++) {
                    if (!authorizationTypes.contains(authorization.get(a))) {
                        authorizationTypes.add(authorization.get(a));
                    }
                }
            }

            usersStructure.add(new US(userType, userRoles, authorizationTypes));
        }

    }

    public HashMap<RoleType, RM> hashSetRm() {
        HashMap<RoleType, RM> hashMap = new HashMap<>();

        int rolesStructureBound = rolesStructure.size();
        for (int i = 0; i < rolesStructureBound; i++) {
            RM rm = rolesStructure.get(i);
            hashMap.put(rm.getRoleObject(), rm);
        }

        return hashMap;
    }

    public HashMap<UserType, US> hashSetUs() {
        HashMap<UserType, US> hashMap = new HashMap<>();

        int usersStructureBound = usersStructure.size();
        for (int i = 0; i < usersStructureBound; i++) {
            US us = usersStructure.get(i);
            hashMap.put(us.getUserObject(), us);
        }

        return hashMap;
    }

    public HashMap<UserType, HashMap<List<RoleType>, List<US>>> sortDescending() {

        HashMap<UserType, HashMap<List<RoleType>, List<US>>> hashMapSortingData = new HashMap<>();

        int userObjectListBound = userObjectList.size();
        for (int u = 0; u < userObjectListBound; u++) {
            UserType userObject = userObjectList.get(u);

            HashMap<RoleType, RM> hashSetRm = hashSetRm();
            HashMap<UserType, US> hashSetUs = hashSetUs();
            US us = hashSetUs.get(userObject);

            if (us == null) {
                continue;
            }
            List<RoleType> usAssignRoles = us.getAssignRoles();
            if (usAssignRoles.isEmpty()) {
                continue;
            }

            int userRolesBound = usAssignRoles.size();

            HashMap<List<RoleType>, List<US>> helperHash = new HashMap<>();

            for (int r = 0; r < userRolesBound; r++) {

                RM rm = hashSetRm.get(usAssignRoles.get(r));
                List<UserType> roleUsersMembers = rm.getRoleUsersMembers();

                for (int m = 0; m < roleUsersMembers.size(); m++) {
                    UserType rmMember = roleUsersMembers.get(m);
                    US usChecked = hashSetUs.get(rmMember);

                    List<US> checkExist = helperHash.get(usChecked.getAssignRoles());
                    if (checkExist != null) {
                        if (!checkExist.contains(usChecked)) {
                            checkExist.add(usChecked);
                        }
                    } else {
                        checkExist = new ArrayList<>();
                        checkExist.add(usChecked);
                    }
                    usChecked.getAssignRoles().sort(Comparator.comparing(AbstractMutableObjectable::getOid));
                    helperHash.put(usChecked.getAssignRoles(), checkExist);
                }
            }

            helperHash = helperHash.entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Comparator.comparingInt(entry -> entry.getKey().size())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            hashMapSortingData.put(userObject, helperHash);

        }
        return hashMapSortingData;
    }

    public List<RM> getRolesStructure() {
        return rolesStructure;
    }

    public void setRolesStructure(List<RM> rolesStructure) {
        this.rolesStructure = rolesStructure;
    }

    public List<US> getUsersStructure() {
        return usersStructure;
    }

    public void setUsersStructure(List<US> usersStructure) {
        this.usersStructure = usersStructure;
    }
}
