/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

public class MClusterObject extends MObject {

    public String identifier;
    public String riskLevel;
    public String mean;
    public String density;
    public Integer minOccupation;
    public Integer maxOccupation;
    public String parentRef;
    public String[] points;
    public Integer pointCount;
    public Integer elementCount;
    public String[] elements;
    public String[] defaultDetection;

}
