/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.structure;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class RoleMiningUserStructure implements Serializable {

    PrismObject<UserType> userObject;
    List<String> roleObjectId;

    public RoleMiningUserStructure(PrismObject<UserType> userObject, List<String> roleObjectId) {
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
