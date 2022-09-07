/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ResourceTilePanel<O> extends BasePanel<TemplateTile<O>> {

    private static final long serialVersionUID = 1L;


    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";

    private static final String ID_TAG = "tag";

    public ResourceTilePanel(String id, IModel<TemplateTile<O>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append(
                "class",
                "card selectable col-12 catalog-tile-panel d-flex flex-column align-items-center p-3 pb-5 pt-4 h-100 mb-0 btn"));
        setOutputMarkupId(true);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        add(icon);

        add(new Label(ID_TITLE, () -> {
            String title = getModelObject().getTitle();
            return title != null ? getString(title, null, title) : null;
        }));

        Label descriptionPanel = new Label(ID_DESCRIPTION, () -> {
            String description = getModelObject().getDescription();
            return description != null ? getString(description, null, description) : null;
        });
        descriptionPanel.add(new VisibleBehaviour(() -> getModelObject().getDescription() != null));
        add(descriptionPanel);

        Label tagPanel = new Label(ID_TAG, () -> {
            String tag = getModelObject().getTag();
            return tag != null ? getString(tag, null, tag) : null;
        });
        tagPanel.add(new VisibleBehaviour(() -> getModelObject().getTag() != null));
        add(tagPanel);

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                ResourceTilePanel.this.onClick(target);
            }
        });
    }

    protected void onClick(AjaxRequestTarget target) {
    }
}
