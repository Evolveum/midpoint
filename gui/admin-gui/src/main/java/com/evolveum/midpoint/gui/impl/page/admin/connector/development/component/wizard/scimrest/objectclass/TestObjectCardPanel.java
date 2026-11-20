/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Card of a test object showing its object class and primary identifiers, used in the testing steps
 * when choosing, modifying and verifying an object.
 */
public class TestObjectCardPanel extends BasePanel<ShadowType> {

    private static final String ID_LABEL = "label";
    private static final String ID_IDENTIFIER = "identifier";

    /**
     * Creates the card of a test object.
     *
     * @param id component id
     * @param model the test object
     */
    public TestObjectCardPanel(String id, IModel<ShadowType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createLabel());
        add(createIdentifier());
    }

    private Label createLabel() {
        return new Label(ID_LABEL, getModel().map(
                object -> getString("TestObjectCardPanel.label", object.getObjectClass().getLocalPart())));
    }

    private Label createIdentifier() {
        return new Label(ID_IDENTIFIER, getModel().map(ConnectorDevelopmentWizardUtil::formatPrimaryIdentifiers));
    }
}
