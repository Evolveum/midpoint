/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractInboundStepPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "passwordInboundWizard")
@PanelInstance(identifier = "passwordInboundWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.password.step.inbound", icon = "fa fa-circle"),
        containerPath = "schemaHandling/objectType/credentials/password/inbound",
        expanded = true)
public class PasswordInboundStepPanel extends AbstractInboundStepPanel<ResourceObjectTypeDefinitionType> {

    private static final String PANEL_TYPE = "passwordInboundWizard";

    public PasswordInboundStepPanel(ResourceDetailsModel model,
                                            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected ItemPath getContainerPath() {
        return ItemPath.create(
                ResourceObjectTypeDefinitionType.F_CREDENTIALS,
                ResourceCredentialsDefinitionType.F_PASSWORD,
                ResourcePasswordDefinitionType.F_INBOUND);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.password.step.inbound");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.password.inbound.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.password.inbound.subText");
    }
}
