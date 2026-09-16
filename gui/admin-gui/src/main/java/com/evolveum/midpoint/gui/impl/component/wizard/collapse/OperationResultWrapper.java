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
    private final String fixButtonLabelKey;
    private final String fixButtonIcon;
    private boolean expanded = false;

    public OperationResultWrapper(OperationResult result, String fixPanelId) {
        this(result, fixPanelId, null);
    }

    public OperationResultWrapper(OperationResult result, String fixPanelId, SerializableConsumer<AjaxRequestTarget> fixAction) {
        this(result, fixPanelId, fixAction, null, null);
    }

    /**
     * @param result             the validation/operation failure this drawer entry reports
     * @param fixPanelId         wizard step to navigate to on fix-button click, used only when
     *                           {@code fixAction} is null
     * @param fixAction          runs instead of step navigation on fix-button click, if set
     * @param fixButtonLabelKey  overrides the button's default "Fix it" localization key - lets a
     *                           caller repurpose the single fix button for a different action (e.g.
     *                           "Disable operation") instead of adding a second button next to it
     * @param fixButtonIcon      overrides the button's default "fa fa-wrench" icon class
     */
    public OperationResultWrapper(
            OperationResult result, String fixPanelId, SerializableConsumer<AjaxRequestTarget> fixAction,
            String fixButtonLabelKey, String fixButtonIcon) {
        this.result = result;
        this.fixPanelId = fixPanelId;
        this.fixAction = fixAction;
        this.fixButtonLabelKey = fixButtonLabelKey;
        this.fixButtonIcon = fixButtonIcon;
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

    public String getFixButtonLabelKey() {
        return fixButtonLabelKey;
    }

    public String getFixButtonIcon() {
        return fixButtonIcon;
    }
}
