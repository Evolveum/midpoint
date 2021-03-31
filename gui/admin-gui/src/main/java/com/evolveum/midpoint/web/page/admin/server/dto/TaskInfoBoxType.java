/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.web.component.box.InfoBoxType;

public class TaskInfoBoxType extends InfoBoxType {

    public static final String F_DURATION = "duration";
    public static final String F_ERROR_MESSAGE = "errorMessage";

    private String timestamp;
    private Long duration;
    private String errorMessage;

    public TaskInfoBoxType(String boxBackgroundColor, String imageId, String message) {
        super(boxBackgroundColor, imageId, message);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
