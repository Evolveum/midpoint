/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Set;

public class UserSet {

    Set<PrismObject<UserType>> users;
    Set<String> roles;

    public UserSet(Set<PrismObject<UserType>> users, Set<String> roles) {
        this.users = users;
        this.roles = roles;
    }

    public Set<PrismObject<UserType>> getUsers() {
        return users;
    }
}
