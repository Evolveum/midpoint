/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

import java.util.HashMap;
import java.util.Map;

public class ProbabilityGenerator {

    private final Map<Integer, Double> probability;
    private double probabilitySum;

    public ProbabilityGenerator() {
        probability = new HashMap<>();
    }

    public void addGroupProbability(int value, double probability) {
        if (this.probability.get(value) != null) {
            this.probabilitySum -= this.probability.get(value);
        }
        this.probability.put(value, probability);
        probabilitySum = probabilitySum + probability;
    }

    public int getRandomNumber() {
        double random = Math.random();
        double ratio = 1.0f / probabilitySum;
        double tempProbability = 0;
        for (Integer i : probability.keySet()) {
            tempProbability += probability.get(i);
            if (random / ratio <= tempProbability) {
                return i;
            }
        }
        return 0;
    }

}
