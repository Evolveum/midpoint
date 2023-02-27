/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune;

public class PruneTools {

    public double confidenceConnection(double supportParent, double supportChild) {
        double confidenceR2R1 = supportChild / supportParent;
        return (Math.round(confidenceR2R1 * 100.0) / 100.0);
    }

}
