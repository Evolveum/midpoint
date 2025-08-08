/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page.ResourceObjectClassTableWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

public class SmartSuggestionWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractWizardPanel<P, ResourceDetailsModel> {

    private static final String CLASS_DOT = SmartSuggestionWizardPanel.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(SmartSuggestionWizardPanel.class);

    public SmartSuggestionWizardPanel(String id, WizardPanelHelper<P, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(getIdOfChoicePanel())));
    }

    protected ResourceObjectClassTableWizardPanel<ResourceObjectTypeDefinitionType, P> createTablePanel(String idOfChoicePanel) {
        return new ResourceObjectClassTableWizardPanel<>(idOfChoicePanel, getHelper()) {

            @Override
            protected void onContinueWithSelected(IModel<SelectableBean<ObjectClassWrapper>> model, AjaxRequestTarget target) {

            }
        };
    }

}
