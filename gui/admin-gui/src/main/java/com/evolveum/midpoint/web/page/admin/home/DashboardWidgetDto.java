/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class DashboardWidgetDto implements Serializable {

    public static final String F_NUMBER_LABEL = "numberLabel";
    public static final String F_MESSAGE = "message";
    public static final String F_STYLE_COLOR = "styleColor";
    public static final String F_STYLE_CSS_STYLE = "styleCssStyle";
    public static final String D_ICON_CSS_CLASS = "iconCssClass";

    private String numberLabel;
    private String message;
    private String styleColor;
    private String styleCssStyle;
    private String iconCssClass;

    private static final String NUMBER_MESSAGE_UNKNOWN = "InfoBoxPanel.message.unknown";
    private static final String DEFAULT_BACKGROUND_COLOR = "background-color:#00a65a;";
    private static final String DEFAULT_COLOR = "color: #fff !important;";
    private static final String DEFAULT_ICON = "fa fa-question";

    public DashboardWidgetDto(DashboardWidget dashboardWidget, PageBase pageBase) {
        this.numberLabel = createNumberLabel(dashboardWidget, pageBase);
        this.message = createMessage(dashboardWidget);
        this.styleColor = createStyleColor(dashboardWidget);
        this.styleCssStyle = createStyleCssStyle(dashboardWidget);
        this.iconCssClass = createIconCssClass(dashboardWidget);
    }

    private String createNumberLabel(DashboardWidget widget, PageBase pageBase) {
        if (widget == null) {
            return pageBase.createStringResource(NUMBER_MESSAGE_UNKNOWN).getString();
        }

        String numberMessage = widget.getNumberMessage();
        if (numberMessage == null) {
            return pageBase.createStringResource(NUMBER_MESSAGE_UNKNOWN).getString();
        }

        return numberMessage; //number message have to add before icon because is needed evaluate variation
    }

    private String createMessage(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return null;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType != null && displayType.getLabel() != null) {
            return WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
        }

        DashboardWidgetType dashboardWidgetType = dashboardWidget.getWidget();
        if (dashboardWidgetType == null) {
            return null;
        }
        return dashboardWidgetType.getIdentifier();
    }

    private String createStyleColor(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return DEFAULT_BACKGROUND_COLOR;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType != null && StringUtils.isNoneBlank(displayType.getColor())) {
            return "background-color:" + GuiDisplayTypeUtil.removeStringAfterSemicolon(displayType.getColor()) + ";";
        }
        return DEFAULT_BACKGROUND_COLOR;
    }

    private String createStyleCssStyle(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return DEFAULT_COLOR;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType != null && StringUtils.isNoneBlank(displayType.getCssStyle())) {
            String style = displayType.getCssStyle();
            if(!style.toLowerCase().contains(" color:") && !style.toLowerCase().startsWith("color:")) {
                style += DEFAULT_COLOR;
            }
            return style;
        }
        return DEFAULT_COLOR;
    }

    private String createIconCssClass(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return DEFAULT_ICON;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType == null) {
            return DEFAULT_ICON;
        }

        IconType icon = displayType.getIcon();
        if (icon == null) {
            return DEFAULT_ICON;
        }

        String cssClass = icon.getCssClass();
        if (StringUtils.isNoneBlank(cssClass)) {
            return cssClass;
        }

        return DEFAULT_ICON;
    }

    public String getNumberLabel() {
        return numberLabel;
    }

    public String getMessage() {
        return message;
    }

    public String getStyleColor() {
        return styleColor;
    }

    public String getStyleCssStyle() {
        return styleCssStyle;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
