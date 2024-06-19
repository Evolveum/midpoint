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

public abstract class AbstractGuiAction<C extends Containerable> implements Serializable {

    public AbstractGuiAction() {
    }

    public abstract void onActionPerformed(C obj, PageBase pageBase, AjaxRequestTarget target);

    public boolean isButton() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        return actionType != null && actionType.button();
    }

    public DisplayType getActionDisplayType() {
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
}
