/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class RoleAnalysisProgressBarDto implements Serializable {

    public static final String F_ACTUAL_VALUE = "actualValue";
    public static final String F_PROGRESS_COLOR = "minValue";
    public static final String F_BAR_TITLE = "barTitle";
    public static final String F_MIN_VALUE = "minValue";
    public static final String F_MAX_VALUE = "maxValue";

    protected double minValue = 0;
    protected double maxValue = 100;
    protected double actualValue = 100;
    protected String barTitle = "";

    protected String progressColor;

    public RoleAnalysisProgressBarDto() {

    }

    public RoleAnalysisProgressBarDto(double actualValue, String progressColor) {
        BigDecimal bd = new BigDecimal(actualValue);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        this.actualValue = bd.doubleValue();
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
