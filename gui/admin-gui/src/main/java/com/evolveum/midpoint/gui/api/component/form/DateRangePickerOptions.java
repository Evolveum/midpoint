/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.form;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Viliam Repan (lazyman).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DateRangePickerOptions implements Serializable {

    private Date minDate;

    private Date maxDate;

    private Boolean showDropdowns;

    private Integer minYear;

    private Integer maxYear;

    private Boolean timePicker;

    private Integer timePickerIncrement;

    private Boolean timePicker24Hour;

    private Boolean timePickerSeconds;

    private Boolean singleDatePicker;

    public Date getMinDate() {
        return minDate;
    }

    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public Boolean getShowDropdowns() {
        return showDropdowns;
    }

    public void setShowDropdowns(Boolean showDropdowns) {
        this.showDropdowns = showDropdowns;
    }

    public Integer getMinYear() {
        return minYear;
    }

    public void setMinYear(Integer minYear) {
        this.minYear = minYear;
    }

    public Integer getMaxYear() {
        return maxYear;
    }

    public void setMaxYear(Integer maxYear) {
        this.maxYear = maxYear;
    }

    public Boolean getTimePicker() {
        return timePicker;
    }

    public void setTimePicker(Boolean timePicker) {
        this.timePicker = timePicker;
    }

    public Integer getTimePickerIncrement() {
        return timePickerIncrement;
    }

    public void setTimePickerIncrement(Integer timePickerIncrement) {
        this.timePickerIncrement = timePickerIncrement;
    }

    public Boolean getTimePicker24Hour() {
        return timePicker24Hour;
    }

    public void setTimePicker24Hour(Boolean timePicker24Hour) {
        this.timePicker24Hour = timePicker24Hour;
    }

    public Boolean getTimePickerSeconds() {
        return timePickerSeconds;
    }

    public void setTimePickerSeconds(Boolean timePickerSeconds) {
        this.timePickerSeconds = timePickerSeconds;
    }

    public Boolean getSingleDatePicker() {
        return singleDatePicker;
    }

    public void setSingleDatePicker(Boolean singleDatePicker) {
        this.singleDatePicker = singleDatePicker;
    }
}
