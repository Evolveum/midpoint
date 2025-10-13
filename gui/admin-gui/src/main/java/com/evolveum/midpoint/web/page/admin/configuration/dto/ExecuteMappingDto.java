/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;

public class ExecuteMappingDto implements Serializable {

    public static final String F_MAPPING = "mapping";
    public static final String F_REQUEST = "request";
    public static final String F_RESULT_TEXT = "resultText";

    private String mapping = "";
    private String request = "";
    private String resultText = "";

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }
}
