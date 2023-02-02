/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.structure;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMembersList implements Serializable {

    PrismObject<RoleType> role;
    List<PrismObject<UserType>> members;

    public RoleMembersList(PrismObject<RoleType> role, List<PrismObject<UserType>> members) {
        this.role = role;
        this.members = members;
    }

    public PrismObject<RoleType> getRole() {
        return role;
    }

    public void setRole(PrismObject<RoleType> role) {
        this.role = role;
    }

    public List<PrismObject<UserType>> getMembers() {
        return members;
    }

    public void setMembers(List<PrismObject<UserType>> members) {
        this.members = members;
    }

}
