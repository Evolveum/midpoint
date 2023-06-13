/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard;


import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.List;

public class UniqueRoleSet {

    String oId;
    List<String> roles;
    List<PrismObject<UserType>> users;

    public UniqueRoleSet(String oId, List<String> roles, List<PrismObject<UserType>> users) {
        this.oId = oId;
        this.roles = roles;
        this.users = users;
    }

    public String getoId() {
        return oId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<PrismObject<UserType>> getUsers() {
        return users;
    }
}
