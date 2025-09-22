/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.CheckPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDocumentationSourceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import org.apache.wicket.model.PropertyModel;

public class DocumentationTilePanel extends TilePanel<DocumentationTile, PrismContainerValueWrapper<ConnDevDocumentationSourceType>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "check";
    private static final String ID_ICON = "icon";
    private static final String ID_CLICKABLE_AREA = "clickableArea";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_URI = "uri";
    private static final String ID_SHOW_DETAILS = "showDetails";
    private static final String ID_DELETE = "delete";
    private static final String ID_AI_TAG = "aiTag";

    public DocumentationTilePanel(String id, IModel<DocumentationTile> model) {
        super(id, model);
    }

    protected void initLayout() {
        add(AttributeAppender.append("class", () -> "mb-2 tile-panel d-flex flex-row vertical align-items-center rounded justify-content-left selectable"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
        add(AttributeAppender.append("aria-checked", () -> getModelObject().isSelected() ? "true" : "false"));
        setOutputMarkupId(true);

        CheckPanel check = new CheckPanel(ID_CHECK, new PropertyModel<>(getModel(), "selected"));
        check.setOutputMarkupId(true);
        add(check);

        Component icon = createIconPanel(ID_ICON);
        add(icon);

        WebMarkupContainer clickableArea = new WebMarkupContainer(ID_CLICKABLE_AREA);
        clickableArea.setOutputMarkupId(true);
        add(clickableArea);
        clickableArea.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                getModelObject().setSelected(!getModelObject().isSelected());
                target.add(DocumentationTilePanel.this);
            }
        });

        Label title = new Label(ID_TITLE, () -> getModelObject().getTitle());
        title.setOutputMarkupId(true);
        clickableArea.add(title);

        WebMarkupContainer aiTag = new WebMarkupContainer(ID_AI_TAG);
        aiTag.setOutputMarkupId(true);
        clickableArea.add(aiTag);
        aiTag.add(new VisibleBehaviour(() -> getModelObject().isAIMarked()));

        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.setOutputMarkupId(true);
        clickableArea.add(description);

        Label uri = new Label(ID_URI, () -> getModelObject().getUri());
        uri.setOutputMarkupId(true);
        clickableArea.add(uri);
        uri.add(new VisibleBehaviour(() -> getModelObject().existUri()));

        AjaxSubmitButton showDetails = new AjaxSubmitButton(ID_SHOW_DETAILS) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
            }
        };
        add(showDetails);

        AjaxSubmitButton delete = new AjaxSubmitButton(ID_DELETE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onDelete(getModelObject(), target);
            }
        };
        add(delete);
    }

    protected void onDelete(DocumentationTile modelObject, AjaxRequestTarget target) {
    }
}
