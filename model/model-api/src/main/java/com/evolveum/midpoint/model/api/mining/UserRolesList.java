/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.mining;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserRolesList implements Serializable {

    PrismObject<UserType> userObject;
    List<String> roleObjectId;

    public UserRolesList(PrismObject<UserType> userObject, List<String> roleObjectId) {
        this.userObject = userObject;
        this.roleObjectId = roleObjectId;
    }

    public PrismObject<UserType> getUserObject() {
        return userObject;
    }

    public List<String> getRoleObjectId() {
        return roleObjectId;
    }

}
