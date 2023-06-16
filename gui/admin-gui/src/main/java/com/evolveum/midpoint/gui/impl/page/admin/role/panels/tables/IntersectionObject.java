/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import java.io.Serializable;
import java.util.Set;

public class IntersectionObject implements Serializable {

    public static final String F_METRIC = "metric";

    public static final String F_TYPE = "type";

    Set<String> rolesId;
    String type;
    double metric;

    public IntersectionObject(Set<String> rolesId, double metric, String type) {
        this.rolesId = rolesId;
        this.metric = metric;
        this.type = type;
    }

    public Set<String> getRolesId() {
        return rolesId;
    }

    public double getMetric() {
        return metric;
    }

    public String getType() {
        return type;
    }

}
