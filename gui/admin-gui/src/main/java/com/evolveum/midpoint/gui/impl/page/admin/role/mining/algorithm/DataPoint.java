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
    private final double[] vectorPoints;
    private final String element;
    List<String> elements;
    List<String> points;

    public DataPoint(double[] vectorPoints, String element, List<String> elements, List<String> points) {
        this.vectorPoints = vectorPoints;
        this.element = element;
        this.elements = elements;
        this.points = points;
    }

    @Override
    public double[] getPoint() {
        return vectorPoints;
    }

    public String getElement() {
        return element;
    }

    public List<String> getElements() {
        return elements;
    }

    public List<String> getPoints() {
        return points;
    }

}
