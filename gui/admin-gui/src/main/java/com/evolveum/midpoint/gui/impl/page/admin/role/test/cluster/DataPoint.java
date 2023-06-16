/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.test.cluster;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class DataPoint implements Clusterable {
        private final double[] point;
        private final String label;
        private boolean noisePoint;

        public DataPoint(double[] point, String label) {
            this.point = point;
            this.label = label;
            this.noisePoint = false;
        }

        @Override
        public double[] getPoint() {
            return point;
        }

        public String getLabel() {
            return label;
        }

        public boolean isNoisePoint() {
            return noisePoint;
        }

        public void setNoisePoint(boolean noisePoint) {
            this.noisePoint = noisePoint;
        }
    }
