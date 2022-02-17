/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;

public class ResourceVisualizationDto implements Serializable {

    public static final String F_DOT = "dot";
    public static final String F_SVG = "svg";
    public static final String F_EXCEPTION_AS_STRING = "exceptionAsString";

    private final String dot;
    private final String svg;
    private final Exception exception;

    public ResourceVisualizationDto(String dot, String svg, Exception exception) {
        this.dot = dot;
        this.svg = svg;
        this.exception = exception;
    }

    public String getDot() {
        return dot;
    }

    public String getSvg() {
        return svg;
    }

    public Exception getException() {
        return exception;
    }

    public String getExceptionAsString() {
        return exception != null ? exception.getMessage() : null;
    }
}
