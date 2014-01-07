/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * @author shood
 */
public class SimplePieChartDto implements Serializable {

    public static final String F_UNIT = "unit";

    private String label;
    private double base;
    private double value;
    private String unit;

    public SimplePieChartDto(String label, double base, double value) {
        this(label, base, value, null);
    }

    public SimplePieChartDto(String label, double base, double value, String unit) {
        this.label = label;
        this.value = value;
        this.base = base;
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public String getLabel() {
        return label;
    }

    public double getBase() {
        return base;
    }

    public double getValue() {
        return value;
    }

    public String getPercent() {
        double percent = calculatePercentage();

        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(percent);
    }

    private double calculatePercentage() {
        if (base == 0) {
            return 0;
        }

        return (value * 100) / base;
    }
}
