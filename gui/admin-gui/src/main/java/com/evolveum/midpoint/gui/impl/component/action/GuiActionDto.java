/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiParameterType;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GuiActionDto<C extends Containerable> implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    AbstractGuiAction<C> preAction;
    List<GuiParameterType> actionParameters;
    private boolean isVisible = true;
    DisplayType display;

    public GuiActionDto() {
    }

    public AbstractGuiAction<C> getPreAction() {
        return preAction;
    }

    public void setPreAction(AbstractGuiAction<C> preAction) {
        this.preAction = preAction;
    }

    public List<GuiParameterType> getActionParameters() {
        return actionParameters;
    }


    public void setActionParameters(List<GuiParameterType> actionParameters) {
        this.actionParameters = actionParameters;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public DisplayType getDisplay() {
        return display;
    }

    public void setDisplay(DisplayType display) {
        this.display = display;
    }
}
