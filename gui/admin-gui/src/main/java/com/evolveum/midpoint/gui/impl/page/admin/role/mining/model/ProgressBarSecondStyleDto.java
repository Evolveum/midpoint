/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.io.Serializable;

public class ProgressBarSecondStyleDto implements Serializable {

    public static final String F_ACTUAL_VALUE = "actualValue";
    public static final String F_PROGRESS_COLOR = "minValue";
    public static final String F_BAR_TITLE = "barTitle";
    public static final String F_MIN_VALUE = "minValue";
    public static final String F_MAX_VALUE = "maxValue";

    private double minValue = 0;
    private double maxValue = 100;
    private double actualValue = 100;
    private String barTitle = "";

    private String progressColor;

    public ProgressBarSecondStyleDto() {

    }

    public ProgressBarSecondStyleDto(double actualValue, String progressColor) {
        this.actualValue = actualValue;
        this.progressColor = progressColor;
    }

    public String getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(String progressColor) {
        this.progressColor = progressColor;
    }

    public double getActualValue() {
        return actualValue;
    }

    public void setActualValue(double actualValue) {
        this.actualValue = actualValue;
    }

    public void setBarTitle(String barTitle) {
        this.barTitle = barTitle;
    }

    public String getBarTitle() {
        return barTitle;
    }
}
