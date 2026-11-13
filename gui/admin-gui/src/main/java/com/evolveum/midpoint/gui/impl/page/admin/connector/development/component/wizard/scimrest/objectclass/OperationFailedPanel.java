/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.impl.event.ShowOperationResultsEvent;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Alert telling that an operation of a wizard step failed, with a link opening the details in the wizard drawer.
 */
public class OperationFailedPanel extends BasePanel<String> {

    private static final String ID_TEXT = "text";
    private static final String ID_VIEW_DETAILS = "viewDetails";

    /**
     * Wizard of the step that ran the operation; it owns the drawer opened by the details link.
     */
    private final IModel<WizardModel> wizardModel;

    public OperationFailedPanel(String id, IModel<String> textModel, IModel<WizardModel> wizardModel) {
        super(id, textModel);
        this.wizardModel = wizardModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupPlaceholderTag(true);
        add(new VisibleBehaviour(() -> getModelObject() != null));
        add(createText());
        add(createViewDetailsLink());
    }

    private Label createText() {
        return new Label(ID_TEXT, getModel());
    }

    private AjaxLink<Void> createViewDetailsLink() {
        AjaxLink<Void> link = new AjaxLink<>(ID_VIEW_DETAILS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                send(this, Broadcast.BUBBLE, new ShowOperationResultsEvent(target));
            }
        };
        link.add(new VisibleBehaviour(() -> ConnectorDevelopmentWizardUtil.hasOperationResults(wizardModel.getObject())));
        return link;
    }
}
