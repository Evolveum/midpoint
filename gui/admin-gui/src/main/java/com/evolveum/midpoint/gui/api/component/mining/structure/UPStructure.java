/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.structure;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class UPStructure extends Selectable<RoleMiningStructureList> implements Serializable {

    public static final String F_NAME = "userObject";

    PrismObject<UserType> userObject;
    List<String> assignRolesObjectIds;
    List<AuthorizationType> assignPermission;
    int staticIndex;

    public UPStructure(PrismObject<UserType> userObject, List<String> assignRolesObjectIds,
            List<AuthorizationType> assignPermission, int staticIndex) {
        this.userObject = userObject;
        this.assignRolesObjectIds = assignRolesObjectIds;
        this.assignPermission = assignPermission;
        this.staticIndex = staticIndex;
    }

    public PrismObject<UserType> getUserObject() {
        return userObject;
    }

    public void setUserObject(PrismObject<UserType> userObject) {
        this.userObject = userObject;
    }

    public List<String> getAssignRolesObjectIds() {
        return assignRolesObjectIds;
    }

    public void setAssignRolesObjectIds(List<String> assignRolesObjectIds) {
        this.assignRolesObjectIds = assignRolesObjectIds;
    }

    public List<AuthorizationType> getAssignPermission() {
        return assignPermission;
    }

    public int getStaticIndex() {
        return staticIndex;
    }

    public void setStaticIndex(int staticIndex) {
        this.staticIndex = staticIndex;
    }

}
