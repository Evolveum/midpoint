/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class RuType extends Selectable<RuType> implements Serializable {

    public static final String F_NAME_ROLE_TYPE = "roleObjectType";

    RoleType roleObjectType;
    List<PrismObject<UserType>> roleUserMembers;

    public RuType(RoleType roleObjectType, List<PrismObject<UserType>> roleUserMembers) {
        this.roleObjectType = roleObjectType;
        this.roleUserMembers = roleUserMembers;
    }

    public RoleType getRoleObjectType() {
        return roleObjectType;
    }

    public void setRoleObjectType(RoleType roleObjectType) {
        this.roleObjectType = roleObjectType;
    }

    public List<PrismObject<UserType>> getRoleUserMembers() {
        return roleUserMembers;
    }

    public void setRoleUserMembers(List<PrismObject<UserType>>  roleUserMembers) {
        this.roleUserMembers = roleUserMembers;
    }

}
