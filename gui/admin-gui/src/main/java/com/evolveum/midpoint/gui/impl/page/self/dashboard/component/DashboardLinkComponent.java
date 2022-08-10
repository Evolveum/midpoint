/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

import javax.servlet.ServletContext;

public class DashboardLinkComponent extends BasePanel<RichHyperlinkType> {

    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
    private static final String ICON_DEFAULT_CSS_CLASS = "fa fa-angle-double-right";


    public DashboardLinkComponent(String id, IModel<RichHyperlinkType> linkModel) {
        super(id, linkModel);
    }

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
                RichHyperlinkType link = DashboardLinkComponent.this.getModelObject();
                if (!isExternalLink()) {
                    ServletContext servletContext = MidPointApplication.get().getServletContext();
                    if (servletContext != null) {
                        rootContext = servletContext.getContextPath();
                    }
                }
                tag.put("href", rootContext + (link.getTargetUrl() == null ? "#" : link.getTargetUrl()));
            }
        };
        add(linkItem);

        linkItem.add(new Label(ID_IMAGE) {

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                String cssClass = ICON_DEFAULT_CSS_CLASS;
                RichHyperlinkType link = DashboardLinkComponent.this.getModelObject();
                if (link.getIcon() != null) {
                    cssClass = link.getIcon().getCssClass();
                }
                tag.put("class", "info-box-icon " + (link.getColor() != null ?
                        (link.getColor().startsWith("bg-") ? link.getColor() : "bg-" + link.getColor()) : "") + " "
                        + cssClass);
            }
        });

        linkItem.add(new Label(ID_LABEL, () -> {
            RichHyperlinkType link = DashboardLinkComponent.this.getModelObject();
            String key = link.getLabel();
            if (key == null) {
                return null;
            }
            return getString(key, null, key);
        }));

        Label description = new Label(ID_DESCRIPTION, () -> {
            RichHyperlinkType link = DashboardLinkComponent.this.getModelObject();
            String desc = link.getDescription();
            if (desc == null) {
                return null;
            }
            return getString(desc, null, desc);
        });
        description.setEnabled(false);
        linkItem.add(description);
    }

    private boolean isExternalLink() {
        return getModelObject().getTargetUrl() != null && new UrlValidator().isValid(getModelObject().getTargetUrl());
    }
}
