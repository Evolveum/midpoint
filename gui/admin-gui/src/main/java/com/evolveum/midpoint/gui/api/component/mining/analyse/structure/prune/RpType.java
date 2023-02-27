/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import java.io.Serializable;
import java.util.List;

public class RpType extends Selectable<RpType> implements Serializable {

    public static final String F_NAME_ROLE_TYPE = "roleObjectType";

    RoleType roleObjectType;
    List<AuthorizationType> permission;

    public RpType(RoleType roleObjectType, List<AuthorizationType> permission) {
        this.roleObjectType = roleObjectType;
        this.permission = permission;
    }

    public RoleType getRoleObjectType() {
        return roleObjectType;
    }

    public void setRoleObjectType(RoleType roleObjectType) {
        this.roleObjectType = roleObjectType;
    }

    public List<AuthorizationType> getPermission() {
        return permission;
    }

    public void setPermission(List<AuthorizationType> permission) {
        this.permission = permission;
    }

}
