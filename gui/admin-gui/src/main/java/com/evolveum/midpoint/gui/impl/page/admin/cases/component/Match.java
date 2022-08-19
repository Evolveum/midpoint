/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

public enum Match {

    NOT_APPLICABLE(""),
    NONE("bg-danger disabled color-palette"),
    PARTIAL("bg-warning disabled color-palette"),
    FULL("bg-success disabled color-palette");

    private final String css;

    Match(String css) {
        this.css = css;
    }

    public String getCss() {
        return css;
    }
}
