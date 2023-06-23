/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */

@Experimental
public class ActivationsWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public ActivationsWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        PrismContainerWrapperModel<ResourceObjectTypeDefinitionType, ResourceActivationDefinitionType> containerModel =
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ResourceObjectTypeDefinitionType.F_ACTIVATION);
        add(createChoiceFragment(
                new ActivationMappingWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel(), containerModel)));
    }
}
