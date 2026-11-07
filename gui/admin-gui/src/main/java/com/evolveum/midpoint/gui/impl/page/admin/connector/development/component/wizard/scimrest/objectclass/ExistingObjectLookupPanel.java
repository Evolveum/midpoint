/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Field for the primary identifier of an existing object of the external system with the card of the found object.
 */
public class ExistingObjectLookupPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_IDENTIFIER_CARD = "identifierCard";
    private static final String ID_IDENTIFIER_LABEL = "identifierLabel";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_CHECK_OBJECT = "checkObject";
    private static final String ID_IDENTIFIER_HINT = "identifierHint";
    private static final String ID_FOUND_OBJECT = "foundObject";
    private static final String ID_FOUND_OBJECT_TEXT = "foundObjectText";
    private static final String ID_FOUND_OBJECT_CARD = "foundObjectCard";
    private static final String ID_CHANGE_OBJECT = "changeObject";

    /**
     * Object found in the external system for the typed identifier; empty until the object is looked up.
     */
    private final IModel<ShadowType> foundObjectModel;

    /**
     * Definition of the object class, used to name the primary identifier.
     */
    private final IModel<ResourceObjectDefinition> objectClassDefinitionModel;

    /**
     * Name of the tested operation, shown in the hint and in the message about the found object.
     */
    private final String operationName;

    /**
     * Creates the lookup of an existing object.
     *
     * @param id component id
     * @param identifierModel primary identifier typed by the user
     * @param foundObjectModel object found for the identifier
     * @param objectClassDefinitionModel definition of the object class
     * @param operationName name of the tested operation, e.g. UPDATE
     */
    public ExistingObjectLookupPanel(String id, IModel<String> identifierModel, IModel<ShadowType> foundObjectModel,
            IModel<ResourceObjectDefinition> objectClassDefinitionModel, String operationName) {
        super(id, identifierModel);
        this.foundObjectModel = foundObjectModel;
        this.objectClassDefinitionModel = objectClassDefinitionModel;
        this.operationName = operationName;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        add(createIdentifierCard());
        add(createFoundObjectPanel());
    }

    /**
     * Called when the user asks to find the object with the typed identifier in the external system.
     *
     * @param target the request target
     */
    protected void onFindObject(AjaxRequestTarget target) {
    }

    /**
     * Called after the user discarded the found object to type another identifier.
     *
     * @param target the request target
     */
    protected void onObjectChanged(AjaxRequestTarget target) {
    }

    private WebMarkupContainer createIdentifierCard() {
        WebMarkupContainer card = new WebMarkupContainer(ID_IDENTIFIER_CARD);
        card.add(new VisibleBehaviour(() -> foundObjectModel.getObject() == null));

        AjaxLink<Void> checkObject = createCheckObjectButton();
        TextField<String> identifier = createIdentifierField(checkObject);
        card.add(createIdentifierLabel(identifier));
        card.add(identifier);
        card.add(checkObject);
        card.add(createIdentifierHint());
        return card;
    }

    private Label createIdentifierHint() {
        return new Label(ID_IDENTIFIER_HINT, () -> getString("ExistingObjectLookupPanel.objectIdentifierHint", operationName));
    }

    private TextField<String> createIdentifierField(AjaxLink<Void> checkObject) {
        TextField<String> identifier = new TextField<>(ID_IDENTIFIER, getModel());
        identifier.setOutputMarkupId(true);
        identifier.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.appendJavaScript("jQuery('#" + checkObject.getMarkupId() + "').toggleClass('disabled', "
                        + StringUtils.isBlank(getModelObject()) + ");");
            }
        });
        return identifier;
    }

    private Label createIdentifierLabel(TextField<String> identifier) {
        Label label = new Label(ID_IDENTIFIER_LABEL, () -> getPrimaryIdentifierTitle() == null
                ? getString("ExistingObjectLookupPanel.objectIdentifier")
                : getString("ExistingObjectLookupPanel.objectIdentifierNamed", getPrimaryIdentifierTitle()));
        label.add(AttributeModifier.replace("for", identifier::getMarkupId));
        return label;
    }

    private String getPrimaryIdentifierTitle() {
        ResourceObjectDefinition objectClassDefinition = objectClassDefinitionModel.getObject();
        return objectClassDefinition == null ? null : objectClassDefinition.getPrimaryIdentifiers().stream()
                .findFirst()
                .map(ConnectorDevelopmentWizardUtil::getAttributeTitle)
                .orElse(null);
    }

    private AjaxLink<Void> createCheckObjectButton() {
        AjaxLink<Void> button = new AjaxLink<>(ID_CHECK_OBJECT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (StringUtils.isBlank(ExistingObjectLookupPanel.this.getModelObject())) {
                    return;
                }
                onFindObject(target);
                target.add(ExistingObjectLookupPanel.this);
            }
        };
        button.setOutputMarkupId(true);
        button.add(AttributeAppender.append("class", () -> StringUtils.isBlank(getModelObject()) ? "disabled" : null));
        return button;
    }

    private WebMarkupContainer createFoundObjectPanel() {
        WebMarkupContainer panel = new WebMarkupContainer(ID_FOUND_OBJECT);
        panel.add(new VisibleBehaviour(() -> foundObjectModel.getObject() != null));
        panel.add(createFoundObjectText());
        panel.add(createFoundObjectCard());
        panel.add(createChangeObjectLink());
        return panel;
    }

    private Label createFoundObjectText() {
        return new Label(ID_FOUND_OBJECT_TEXT, () -> getString("ExistingObjectLookupPanel.objectFoundText", operationName));
    }

    private TestObjectCardPanel createFoundObjectCard() {
        return new TestObjectCardPanel(ID_FOUND_OBJECT_CARD, foundObjectModel);
    }

    private AjaxLink<Void> createChangeObjectLink() {
        return new AjaxLink<>(ID_CHANGE_OBJECT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                foundObjectModel.setObject(null);
                target.add(ExistingObjectLookupPanel.this);
                onObjectChanged(target);
            }
        };
    }
}
