/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractOutboundStepPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "activationOutboundWizard")
@PanelInstance(identifier = "activationOutboundWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.password.step.outbound", icon = "fa fa-circle"),
        containerPath = "schemaHandling/objectType/credentials/password/outbound")
public class PasswordOutboundStepPanel extends AbstractOutboundStepPanel<ResourceObjectTypeDefinitionType> {

    private static final String PANEL_TYPE = "activationOutboundWizard";

    public PasswordOutboundStepPanel(ResourceDetailsModel model,
                                       IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected ItemPath getContainerPath() {
        return ItemPath.create(
                ResourceObjectTypeDefinitionType.F_CREDENTIALS,
                ResourceCredentialsDefinitionType.F_PASSWORD,
                ResourcePasswordDefinitionType.F_OUTBOUND);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.password.step.outbound");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.password.outbound.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.password.outbound.subText");
    }
}
