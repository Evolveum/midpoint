/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.structure;

import java.io.Serializable;
import java.util.ArrayList;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class JaccardDataStructure implements Serializable {

    public static final String F_NAME = "userObject";

    public PrismObject<UserType> getUserObject() {
        return userObject;
    }

    public void setUserObject(PrismObject<UserType> userObject) {
        this.userObject = userObject;
    }

    PrismObject<UserType> userObject;
    ArrayList<Double> objectPartialResult;
    double objectTotalResult;

    public JaccardDataStructure(PrismObject<UserType> userObject, double objectTotalResult, ArrayList<Double> objectPartialResult) {
        this.userObject = userObject;
        this.objectPartialResult = objectPartialResult;
        this.objectTotalResult = objectTotalResult;
    }

    public ArrayList<Double> getObjectPartialResult() {
        return objectPartialResult;
    }

    public void setObjectPartialResult(ArrayList<Double> objectPartialResult) {
        this.objectPartialResult = objectPartialResult;
    }

    public double getObjectTotalResult() {
        return objectTotalResult;
    }

    public void setObjectTotalResult(double objectTotalResult) {
        this.objectTotalResult = objectTotalResult;
    }
}
