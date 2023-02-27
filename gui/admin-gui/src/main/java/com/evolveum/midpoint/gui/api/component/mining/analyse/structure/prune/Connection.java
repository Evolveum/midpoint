/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune;

import java.io.Serializable;

public class Connection implements Serializable {

    int targetKey;
    int degree;
    double confidence;

    public Connection(int targetKey, double confidence, int degree) {
        this.targetKey = targetKey;
        this.confidence = confidence;
        this.degree = degree;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    public int getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(int targetKey) {
        this.targetKey = targetKey;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
