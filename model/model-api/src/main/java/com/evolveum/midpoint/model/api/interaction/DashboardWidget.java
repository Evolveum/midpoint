/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.interaction;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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

    public String getLabel(LocalizationService localizationService) {
        if(getDisplay() != null && getDisplay().getLabel() != null) {
            return localizationService.translate(getDisplay().getLabel().toPolyString(), getLocale(), true);
        } else {
            return getWidget().getIdentifier();
        }
    }

    @NotNull private Locale getLocale() {
        Locale locale = null;
        try {
            MidPointPrincipal principal = SecurityUtil.getPrincipal();
            if (principal != null) {
                locale = principal.getLocale();
            }
        } catch (SecurityViolationException ex) {
            // we can safely ignore this one, we only wanted locale if principal object is available
        }

        return locale != null ? locale : Locale.getDefault();
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
