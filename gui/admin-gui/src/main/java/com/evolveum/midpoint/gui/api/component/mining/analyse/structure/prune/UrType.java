/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UrType extends Selectable<UrType> implements Serializable{

    public static final String F_NAME_USER_TYPE = "userObjectType";
    UserType userObjectType;
    List<RoleType> roleMembers;

    public UrType(UserType userObjectType, List<RoleType> roleMembers) {
        this.userObjectType = userObjectType;
        this.roleMembers = roleMembers;
    }

    public UserType getUserObjectType() {
        return userObjectType;
    }

    public void setUserObjectType(UserType userObjectType) {
        this.userObjectType = userObjectType;
    }

    public List<RoleType> getRoleMembers() {
        return roleMembers;
    }

    public void setRoleMembers(List<RoleType> roleMembers) {
        this.roleMembers = roleMembers;
    }

}
