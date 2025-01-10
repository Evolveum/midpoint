/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingOptionalConfigurationStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

import org.apache.wicket.model.IModel;

@PanelType(name = "arw-construction-mapping-optional")
@PanelInstance(identifier = "arw-construction-mapping-optional",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.outbound.optional", icon = "fa fa-circle"),
        containerPath = "empty")
public class ConstructionOutboundOptionalStepPanel<AHD extends AssignmentHolderDetailsModel> extends OutboundMappingOptionalConfigurationStepPanel<AHD> {

    private static final String PANEL_TYPE = "arw-construction-mapping-optional";

    public ConstructionOutboundOptionalStepPanel(AHD model, IModel newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
