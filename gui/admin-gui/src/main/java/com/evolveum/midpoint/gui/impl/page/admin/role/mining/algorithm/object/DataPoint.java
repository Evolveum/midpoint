/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class DataPoint implements Clusterable, Serializable {
    private final double[] vectorPoints;

    //(USER Type)
    List<String> elements;

    //(ROLE Type)
    List<String> points;

    public DataPoint(double[] vectorPoints, List<String> elements, List<String> points) {
        this.vectorPoints = vectorPoints;
        this.elements = elements;
        this.points = points;
    }

    @Override
    public double[] getPoint() {
        return vectorPoints;
    }

    public List<String> getElements() {
        return elements;
    }

    public List<String> getPoints() {
        return points;
    }

}
