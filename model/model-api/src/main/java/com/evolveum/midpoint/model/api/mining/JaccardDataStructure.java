/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.mining;

import java.io.Serializable;
import java.util.ArrayList;

public class JaccardDataStructure implements Serializable {

    public static final String F_NAME = "objectName";
    public static final String F_RESULT_ARRAY = "objectPartialResult";
    public static final String F_SUM_RESULT = "objectTotalResult";

    String objectName;
    ArrayList<Double> objectPartialResult;
    double objectTotalResult;

    public JaccardDataStructure(String objectName, double objectTotalResult, ArrayList<Double> objectPartialResult) {
        this.objectName = objectName;
        this.objectPartialResult = objectPartialResult;
        this.objectTotalResult = objectTotalResult;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
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
