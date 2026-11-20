/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Object class badge with the attribute of one relationship side, highlighting the parts not configured in the connector.
 */
public class RelationshipSidePanel extends BasePanel<RelationshipSideDto> {

    private static final String ID_OBJECT_CLASS_BADGE = "objectClassBadge";
    private static final String ID_OBJECT_CLASS_WARNING = "objectClassWarning";
    private static final String ID_OBJECT_CLASS_NAME = "objectClassName";
    private static final String ID_ATTRIBUTE_LINE = "attributeLine";
    private static final String ID_ATTRIBUTE_WARNING = "attributeWarning";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";

    public RelationshipSidePanel(String id, IModel<RelationshipSideDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(createObjectClassBadge());
        add(createAttributeLine());
    }

    private WebMarkupContainer createObjectClassBadge() {
        WebMarkupContainer objectClassBadge = new WebMarkupContainer(ID_OBJECT_CLASS_BADGE);
        objectClassBadge.add(AttributeAppender.append("class",
                () -> getModelObject().getObjectClass().isConfigured() ? "bg-body border-secondary-subtle text-body" : "bg-warning-subtle border-warning-subtle text-warning-emphasis"));
        objectClassBadge.add(createWarningIcon(ID_OBJECT_CLASS_WARNING, () -> !getModelObject().getObjectClass().isConfigured()));
        objectClassBadge.add(createObjectClassNameLabel());
        return objectClassBadge;
    }

    private WebMarkupContainer createAttributeLine() {
        WebMarkupContainer attributeLine = new WebMarkupContainer(ID_ATTRIBUTE_LINE);
        attributeLine.add(AttributeAppender.append("class",
                () -> getModelObject().getAttribute().isConfigured() ? "text-secondary" : "text-warning-emphasis"));
        attributeLine.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getAttribute().getName())));
        attributeLine.add(createWarningIcon(ID_ATTRIBUTE_WARNING, () -> !getModelObject().getAttribute().isConfigured()));
        attributeLine.add(createAttributeNameLabel());
        return attributeLine;
    }

    private Label createObjectClassNameLabel() {
        return new Label(ID_OBJECT_CLASS_NAME, () -> getModelObject().getObjectClass().getName());
    }

    private Label createAttributeNameLabel() {
        return new Label(ID_ATTRIBUTE_NAME, () -> getModelObject().getAttribute().getName());
    }

    private WebMarkupContainer createWarningIcon(String id, IModel<Boolean> visible) {
        WebMarkupContainer icon = new WebMarkupContainer(id);
        icon.add(new VisibleBehaviour(visible::getObject));
        return icon;
    }
}
