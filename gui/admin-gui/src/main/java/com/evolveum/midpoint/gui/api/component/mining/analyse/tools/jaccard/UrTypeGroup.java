/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class UrTypeGroup extends Selectable<UrTypeGroup> implements Serializable {

    public static final String F_NAME_USER_TYPE = "userObjectType";
    public static final String F_GROUP= "groupId";
    UserType userObjectType;
    List<RoleType> roleMembers;

    int groupId;

    public UrTypeGroup(UserType userObjectType, List<RoleType> roleMembers, int groupId) {
        this.userObjectType = userObjectType;
        this.roleMembers = roleMembers;
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }
    public UserType getUserObjectType() {
        return userObjectType;
    }
    public List<RoleType> getRoleMembers() {
        return roleMembers;
    }
}
