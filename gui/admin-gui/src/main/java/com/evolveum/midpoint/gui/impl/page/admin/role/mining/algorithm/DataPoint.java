/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.List;

public class DataPoint implements Clusterable {
    private final double[] point;
    private final String label;
    List<String> members;
    List<String> roles;

    public DataPoint(double[] point, String label, List<String> members, List<String> roles) {
        this.point = point;
        this.label = label;
        this.members = members;
        this.roles = roles;
    }

    @Override
    public double[] getPoint() {
        return point;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getMembers() {
        return members;
    }

    public List<String> getRoles() {
        return roles;
    }

}
