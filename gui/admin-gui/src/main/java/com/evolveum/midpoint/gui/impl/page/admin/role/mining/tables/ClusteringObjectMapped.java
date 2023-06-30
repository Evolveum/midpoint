/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ClusteringObjectMapped implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    String targetOid;
    List<String> points;
    List<String> elements;

    public ClusteringObjectMapped(String targetOid, List<String> points,List<String> elements) {
        this.targetOid = targetOid;
        this.points = points;
        this.elements = elements;
    }

    public List<String> getPoints() {
        return points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusteringObjectMapped clusteringObjectMapped = (ClusteringObjectMapped) o;
        return Objects.equals(targetOid, clusteringObjectMapped.targetOid) &&
                Objects.equals(points, clusteringObjectMapped.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetOid, points);
    }


    public List<String> getElements() {
        return elements;
    }

    public String getTargetOid() {
        return targetOid;
    }
}
