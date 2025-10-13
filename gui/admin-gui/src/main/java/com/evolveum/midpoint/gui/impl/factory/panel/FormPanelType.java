/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

/**
 * @author lskublik
 */
public enum FormPanelType {
    VERTICAL,
    HORIZONTAL;

    public static FormPanelType getDefault() {
        return HORIZONTAL;
    }
}
