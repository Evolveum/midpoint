/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.Serializable;

public class OperationResultWrapper implements Serializable {

    private final OperationResult result;
    private final String fixPanelId;
    private boolean expanded = false;

    public OperationResultWrapper(OperationResult result, String fixPanelId) {
        this.result = result;
        this.fixPanelId = fixPanelId;
    }

    public OperationResult getResult() {
        return result;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getFixPanelId() {
        return fixPanelId;
    }
}
