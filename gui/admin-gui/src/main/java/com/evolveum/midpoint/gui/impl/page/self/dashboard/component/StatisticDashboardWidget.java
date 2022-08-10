/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

import javax.servlet.ServletContext;

@PanelType(name = "statisticWidget")
public class StatisticDashboardWidget extends BasePanel<ContainerPanelConfigurationType> {

    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
    private static final String ICON_DEFAULT_CSS_CLASS = "fa fa-angle-double-right";

    public StatisticDashboardWidget(String id, IModel<ContainerPanelConfigurationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Link<Void> linkItem = new Link<>(ID_LINK) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                // TODO Auto-generated method stub

            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);

                String rootContext = "";
                ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
                if (!isExternalLink()) {
                    ServletContext servletContext = MidPointApplication.get().getServletContext();
                    if (servletContext != null) {
                        rootContext = servletContext.getContextPath();
                    }
                }
                //todo later
//                tag.put("href", rootContext + (panel.getTargetUrl() == null ? "#" : panel.getTargetUrl()));
            }
        };
        add(linkItem);

        Label icon = new Label(ID_IMAGE);
        icon.add(AttributeAppender.append("class", getIconClassModel()));
        linkItem.add(icon);

        linkItem.add(new Label(ID_LABEL, () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            return WebComponentUtil.getCollectionLabel(panel.getDisplay());
        }));

        Label description = new Label(ID_DESCRIPTION, () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            return WebComponentUtil.getHelp(panel.getDisplay());
        });
        description.setEnabled(false);
        linkItem.add(description);
    }

    private IModel<String> getIconClassModel() {
        return () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            String cssClass = WebComponentUtil.getIconCssClass(panel.getDisplay());
            if (StringUtils.isEmpty(cssClass)) {
                cssClass = ICON_DEFAULT_CSS_CLASS;
            }
            return "info-box-icon " + getIconColor() + cssClass;
        };
    }

    private String getIconColor() {
        String iconColor = WebComponentUtil.getIconColor(getModelObject().getDisplay());
        if (StringUtils.isNotEmpty(iconColor)) {
            return iconColor.startsWith("bg-") ? iconColor : "bg-" + iconColor + " ";
        }
        return "";
    }

    private boolean isExternalLink() {
//        return getModelObject().getTargetUrl() != null && new UrlValidator().isValid(getModelObject().getTargetUrl());
        return false;
    }
}
