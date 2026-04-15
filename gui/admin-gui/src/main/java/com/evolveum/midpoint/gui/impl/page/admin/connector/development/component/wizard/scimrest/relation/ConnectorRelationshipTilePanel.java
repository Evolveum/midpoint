/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class ConnectorRelationshipTilePanel extends BasePanel<ConnectorRelationshipTile>{

    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_MODIFY_BUTTON = "modifyButton";
    private static final String ID_DELETE_BUTTON = "deleteButton";

    private static final String ID_SUBJECT = "subject";
    private static final String ID_SUBJECT_ATTRIBUTE = "subjectAttribute";
    private static final String ID_OBJECT = "object";
    private static final String ID_OBJECT_ATTRIBUTE = "objectAttribute";

    public ConnectorRelationshipTilePanel(String id, IModel<ConnectorRelationshipTile> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append(
                "class",
                "card col-12 tile d-flex flex-column  p-3 mb-0"));
        setOutputMarkupId(true);

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

        AjaxLink<?> modifyButton = new AjaxLink<>(ID_MODIFY_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                configureRelationshipPerformed(target);
            }
        };
        modifyButton.setOutputMarkupId(true);
        add(modifyButton);

        AjaxIconButton deleteButton = new AjaxIconButton(
                ID_DELETE_BUTTON,
                Model.of("fa fa-trash"),
                createStringResource("ConnectorRelationshipTilePanel.delete")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteRelationshipPerformed(target);
            }
        };
        deleteButton.setOutputMarkupId(true);
        add(deleteButton);

        Label subject = new Label(ID_SUBJECT, () -> getModelObject().getSubject());
        subject.setOutputMarkupId(true);
        add(subject);

        Label subjectAttribute = new Label(ID_SUBJECT_ATTRIBUTE, () -> getModelObject().getSubjectAttribute());
        subjectAttribute.setOutputMarkupId(true);
        add(subjectAttribute);

        Label object = new Label(ID_OBJECT, () -> getModelObject().getObject());
        object.setOutputMarkupId(true);
        add(object);

        Label objectAttribute = new Label(ID_OBJECT_ATTRIBUTE, () -> getModelObject().getObjectAttribute());
        objectAttribute.setOutputMarkupId(true);
        add(objectAttribute);
    }

    protected abstract void configureRelationshipPerformed(AjaxRequestTarget target);

    protected abstract void deleteRelationshipPerformed(AjaxRequestTarget target);
}
