/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lskublik
 */
public class TemplateTilePanel<O extends Serializable, T extends TemplateTile<O>> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;


    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    protected static final String ID_DESCRIPTION = "description";

    private static final String ID_TAG = "tag";

    public TemplateTilePanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        add(AttributeAppender.append(
                "class",
                "card selectable col-12 catalog-tile-panel d-flex flex-column align-items-center p-3 pb-5 pt-4 h-100 mb-0"));
        setOutputMarkupId(true);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        add(icon);

        IModel<String> titleModel = () -> {
            String titleText = getModelObject().getTitle();
            return titleText != null ? getString(titleText, null, titleText) : null;
        };

        Label title = new Label(ID_TITLE, titleModel);
        title.add(AttributeAppender.append("title", titleModel));
        title.add(new TooltipBehavior());
        add(title);

        IModel<String> descriptionModel = () -> {
            String description = getModelObject().getDescription();
            return description != null ? getString(description, null, description) : null;
        };

        Label descriptionPanel = new Label(ID_DESCRIPTION, descriptionModel);
        descriptionPanel.add(AttributeAppender.append("title", descriptionModel));
        descriptionPanel.add(new TooltipBehavior());
        descriptionPanel.add(new VisibleBehaviour(() -> getModelObject().getDescription() != null));
        add(descriptionPanel);

        Label tagPanel = new Label(ID_TAG, () -> {
            if (getModelObject().getTags().isEmpty()) {
                return null;
            }
            DisplayType tag = getModelObject().getTags().iterator().next();
            return tag != null ? WebComponentUtil.getTranslatedPolyString(tag.getLabel()) : null;
        });
        tagPanel.add(new VisibleBehaviour(() -> getModelObject().getTags() != null));
        add(tagPanel);

        if (addClickBehaviour()) {
            add(new AjaxEventBehavior("click") {

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    TemplateTilePanel.this.onClick(target);
                }
            });
        }
    }

    protected WebMarkupContainer getIcon() {
        return (WebMarkupContainer) get(ID_ICON);
    }

    protected boolean addClickBehaviour() {
        return true;
    }

    protected void onClick(AjaxRequestTarget target) {
    }
}
