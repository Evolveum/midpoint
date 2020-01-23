/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.interaction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * @author skublik
 */
public class DashboardWidget {

    private DisplayType display;
    private String numberMessage;
    private DashboardWidgetType widget;

    public DashboardWidget() {
    }

    public DashboardWidget(DashboardWidgetType widget, DisplayType display, String numberMessage) {
        this.widget = widget;
        this.display = display;
        this.numberMessage = numberMessage;
    }

    public DisplayType getDisplay() {
        return display;
    }

    public void setDisplay(DisplayType display) {
        this.display = display;
    }

    public String getNumberMessage() {
        return numberMessage;
    }

    public void setNumberMessage(String numberMessage) {
        this.numberMessage = numberMessage;
    }

    public DashboardWidgetType getWidget() {
        return widget;
    }

    public void setWidget(DashboardWidgetType widget) {
        this.widget = widget;
    }

    public String getLabel() {
        if(getDisplay() != null && getDisplay().getLabel() != null) {
            return getDisplay().getLabel().toString();
        } else {
            return getWidget().getIdentifier();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{widgetIdentifier:").append(widget == null ? null : widget.getIdentifier())
        .append(", numberMessage:").append(numberMessage)
        .append(", display:").append(display).append("}");
        return sb.toString();
    }
}
