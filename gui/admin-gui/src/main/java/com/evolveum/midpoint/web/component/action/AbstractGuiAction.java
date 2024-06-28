/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class AbstractGuiAction<C extends Containerable> implements Serializable {

    AbstractGuiAction<C> preAction;
    private boolean isVisible = true;
    private boolean isExecuted = false;
    private DisplayType actionDisplayType = null;

    public AbstractGuiAction() {
    }

    public AbstractGuiAction(AbstractGuiAction<C> preAction) {
        this.preAction = preAction;
    }

    public void onActionPerformed(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        if (preAction != null && !preAction.isExecuted()) {
            if (preAction instanceof PreAction) {
                PreAction<C, AbstractGuiAction<C>> preAction = (PreAction<C, AbstractGuiAction<C>>) this.preAction;
                preAction.executePreActionAndMainAction(this, objectsToProcess, pageBase, target);
            } else {
                preAction.onActionPerformed(objectsToProcess, pageBase, target);
            }
            preAction.setExecuted(true);
        } else {
            Map<String, Object> preActionParametersMap = preAction != null && preAction instanceof PreAction ?
                    ((PreAction<C, AbstractGuiAction<C>>) preAction).getActionResultParametersMap() : null;
            processPreActionParametersValues(preActionParametersMap);
            executeAction(objectsToProcess, pageBase, target);
        }
    }

    protected abstract void executeAction(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target);

    public boolean isButton() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        return actionType != null && actionType.button();
    }

    public DisplayType getActionDisplayType() {
        if (actionDisplayType != null) {
            return actionDisplayType;
        }
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        PanelDisplay display = actionType != null ? actionType.display() : null;
        if (display == null) {
            return null;
        }
        return new DisplayType()
                .label(display.label())
                .icon(new IconType()
                        .cssClass(display.icon()));
    }

    public void setActionDisplayType(DisplayType actionDisplayType) {
        this.actionDisplayType = actionDisplayType;
    }

    /**
     * the idea is to move some values from the preAction to the main action
     * (e.g. comment in case CommentAction is executed as preAction)
     * @param preActionParametersMap
     */
    protected void processPreActionParametersValues(Map<String, Object> preActionParametersMap) {
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean isExecuted() {
        return isExecuted;
    }

    public void setExecuted(boolean isExecuted) {
        this.isExecuted = isExecuted;
    }
}
