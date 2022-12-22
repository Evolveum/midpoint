/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMiningStructureList extends Selectable<RoleMiningStructureList> implements Serializable {

    public static final String F_NAME = "userObject";
    public static final String F_ROLES_OID_LIST = "roleObjectRef";
    public static final String F_SUM_RESULT = "objectTotalResult";
    public static final String F_RESULT_ARRAY = "objectPartialResult";

    PrismObject<UserType> userObject;
    List<String> roleObjectRef;
    double objectTotalResult;
    ArrayList<Double> objectPartialResult;
    int staticIndex;


    public RoleMiningStructureList(PrismObject<UserType> userObject, ArrayList<Double> objectPartialResult, double objectTotalResult, List<String> roleObjectRef, int staticIndex) {
        this.userObject = userObject;
        this.objectPartialResult = objectPartialResult;
        this.roleObjectRef = roleObjectRef;
        this.objectTotalResult = objectTotalResult;
        this.staticIndex = staticIndex;
        setSelected(false);
    }

    public ArrayList<Double> getObjectPartialResult() {
        return objectPartialResult;
    }

    public void setObjectPartialResult(ArrayList<Double> objectPartialResult) {
        this.objectPartialResult = objectPartialResult;
    }

    public List<String> getRoleObjectRef() {
        return roleObjectRef;
    }

    public void setRoleObjectRef(List<String> roleObjectRef) {
        this.roleObjectRef = roleObjectRef;
    }

    public double getObjectTotalResult() {
        return objectTotalResult;
    }

    public void setObjectTotalResult(double objectTotalResult) {
        this.objectTotalResult = objectTotalResult;
    }

    public int getStaticIndex() {
        return staticIndex;
    }

    public void setStaticIndex(int staticIndex) {
        this.staticIndex = staticIndex;
    }

    public PrismObject<UserType> getUserObject() {
        return userObject;
    }

    public void setUserObject(PrismObject<UserType> userObject) {
        this.userObject = userObject;
    }


}

