/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "cdw-association-basic",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.associationBasic", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ApplicationBasicConnectorStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ConnectorDevelopmentScriptType, ConnectorDevelopmentDetailsModel> {

    public static final String PANEL_TYPE = "cdw-association-basic";

    public ApplicationBasicConnectorStepPanel(
            ConnectorDevelopmentDetailsModel model,
            IModel<PrismContainerValueWrapper<ConnectorDevelopmentScriptType>> newValueModel) {
        super(model, newValueModel, null);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.associationBasic");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.associationBasic.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.associationBasic.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return this::checkMandatory;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ResourceObjectTypeDefinitionType.F_KIND)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }
}
