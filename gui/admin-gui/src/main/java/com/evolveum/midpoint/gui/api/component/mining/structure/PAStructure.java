/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.structure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PAStructure extends Selectable<RoleMiningStructureList> implements Serializable {


    public static final String F_NAME = "roleObject";
    public static final String F_TOTAL_RESULT = "objectTotalResult";

    PrismObject<RoleType> roleObject;
    List<AuthorizationType> authorizationTypeList;
    List<PrismObject<UserType>> roleMembers;
    double objectTotalResult;
    ArrayList<Double> objectPartialResult;
    int staticIndex;

    public PAStructure(PrismObject<RoleType> roleObject, List<PrismObject<UserType>> roleMembers,
            double objectTotalResult, ArrayList<Double> objectPartialResult,List<AuthorizationType> authorizationTypeList, int staticIndex) {
        this.roleObject = roleObject;
        this.roleMembers = roleMembers;
        this.objectTotalResult = objectTotalResult;
        this.objectPartialResult = objectPartialResult;
        this.authorizationTypeList = authorizationTypeList;
        this.staticIndex = staticIndex;
    }

    public PrismObject<RoleType> getRoleObject() {
        return roleObject;
    }

    public void setRoleObject(PrismObject<RoleType> roleObject) {
        this.roleObject = roleObject;
    }

    public List<PrismObject<UserType>> getRoleMembers() {
        return roleMembers;
    }

    public void setRoleMembers(List<PrismObject<UserType>> roleMembers) {
        this.roleMembers = roleMembers;
    }

    public double getObjectTotalResult() {
        return objectTotalResult;
    }

    public void setObjectTotalResult(double objectTotalResult) {
        this.objectTotalResult = objectTotalResult;
    }

    public ArrayList<Double> getObjectPartialResult() {
        return objectPartialResult;
    }

    public void setObjectPartialResult(ArrayList<Double> objectPartialResult) {
        this.objectPartialResult = objectPartialResult;
    }

    public int getStaticIndex() {
        return staticIndex;
    }

    public void setStaticIndex(int staticIndex) {
        this.staticIndex = staticIndex;
    }

    public List<AuthorizationType> getAuthorizationTypeList() {
        return authorizationTypeList;
    }

    public void setAuthorizationTypeList(List<AuthorizationType> authorizationTypeList) {
        this.authorizationTypeList = authorizationTypeList;
    }

}
