/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingMainConfigurationStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

import org.apache.wicket.model.IModel;

@PanelType(name = "arw-construction-mapping-main")
@PanelInstance(identifier = "arw-construction-mapping-main",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.outbound.main", icon = "fa fa-circle"),
        containerPath = "empty")
public class ConstructionOutboundMainStepPanel<AHD extends AssignmentHolderDetailsModel> extends OutboundMappingMainConfigurationStepPanel<AHD> {

    private static final String PANEL_TYPE = "arw-construction-mapping-main";

    public ConstructionOutboundMainStepPanel(AHD model, IModel newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
