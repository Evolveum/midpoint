/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class UpType extends Selectable<UpType> implements Serializable {
    public static final String F_NAME_USER_TYPE = "userObjectType";

    UserType userObjectType;
    List<AuthorizationType> permission;

    public UpType(UserType userObjectType, List<AuthorizationType> permission) {
        this.userObjectType = userObjectType;
        this.permission = permission;
    }


    public UserType getUserObjectType() {
        return userObjectType;
    }

    public void setUserObjectType(UserType userObjectType) {
        this.userObjectType = userObjectType;
    }

    public List<AuthorizationType> getPermission() {
        return permission;
    }

    public void setPermission(List<AuthorizationType> permission) {
        this.permission = permission;
    }

}
