/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;

public class OperationResultWrapper implements Serializable {

    private final OperationResult result;
    private final String fixPanelId;
    private final SerializableConsumer<AjaxRequestTarget> fixAction;
    private boolean expanded = false;

    public OperationResultWrapper(OperationResult result, String fixPanelId) {
        this(result, fixPanelId, null);
    }

    public OperationResultWrapper(OperationResult result, String fixPanelId, SerializableConsumer<AjaxRequestTarget> fixAction) {
        this.result = result;
        this.fixPanelId = fixPanelId;
        this.fixAction = fixAction;
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

    public SerializableConsumer<AjaxRequestTarget> getFixAction() {
        return fixAction;
    }
}
