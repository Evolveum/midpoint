/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class RoleAnalyseStructure implements Serializable {

    RoleType roleType;
    List<PrismObject<UserType>> members;
    List<AuthorizationType> permission;

    public RoleAnalyseStructure(RoleType roleType, List<PrismObject<UserType>> members, List<AuthorizationType> permission) {
        this.roleType = roleType;
        this.members = members;
        this.permission = permission;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public List<PrismObject<UserType>> getMembers() {
        return members;
    }

    public List<AuthorizationType> getPermission() {
        return permission;
    }

}
