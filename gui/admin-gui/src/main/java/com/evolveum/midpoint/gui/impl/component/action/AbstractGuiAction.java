/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractGuiAction<C extends Containerable> implements Serializable {

    AbstractGuiAction<C> preAction;
    private boolean isVisible = true;
    private DisplayType actionDisplayType = null;
    Map<String, Object> actionParametersMap = new HashMap<>();
    List<GuiParameterType> actionParameters = new ArrayList<>();

    public AbstractGuiAction() {
    }

    public AbstractGuiAction(@NotNull GuiActionDto<C> guiActionDto) {
        this.preAction = guiActionDto.getPreAction();
        this.actionParameters = guiActionDto.getActionParameters();
        this.isVisible = guiActionDto.isVisible();
        this.actionDisplayType = guiActionDto.getDisplay();
    }

    public void onActionPerformed(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        onActionPerformed(objectsToProcess, true, pageBase, target);
    }

    /**
     * Executes the action. If preAction is set, it will be executed first.
     * This method should be called for main action with preActionPerform set to false (after pre-action was performed)
     * @param objectsToProcess
     * @param preActionPerform
     * @param pageBase
     * @param target
     */
    public void onActionPerformed(List<C> objectsToProcess, boolean preActionPerform, PageBase pageBase, AjaxRequestTarget target) {
        if (preAction != null && preActionPerform) {
            if (preAction instanceof PreAction) {
                PreAction<C, AbstractGuiAction<C>> preAction = (PreAction<C, AbstractGuiAction<C>>) this.preAction;
                preAction.onActionPerformed(this, objectsToProcess, pageBase, target);
            } else {
                preAction.onActionPerformed(objectsToProcess, pageBase, target);
            }
        } else {
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

    public boolean isVisible() {
        return isVisible;
    }

    public Map<String, Object> getActionParametersMap() {
        return actionParametersMap;
    }

    public void addParameterValue(String parameterName, Object parameterValue) {
        if (actionParametersMap.containsKey(parameterName)) {
            actionParametersMap.replace(parameterName, parameterValue);
        } else {
            actionParametersMap.put(parameterName, parameterValue);
        }
    }

    public List<String> getParameterNameList() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        if (actionType!= null && actionType.parameterName() != null) {
            return List.of(actionType.parameterName());
        }
        return new ArrayList<>();
    }

    public Map<String, Object> getPreActionParametersMap() {
        return preAction != null ? preAction.getActionParametersMap() : null;
    }

    public boolean isParameterMandatory(String parameterName) {
        if (actionParameters == null) {
            return false;
        }
        return actionParameters.stream()
                .anyMatch(param -> parameterName.equals(param.getName()) && param.isMandatory());
    }

    public boolean isBulkAction() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        return actionType != null && actionType.bulkAction();
    }

}
