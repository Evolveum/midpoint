/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingRangeStepPanel;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * Step configuring the range of a construction outbound mapping.
 *
 * An outbound attribute mapping aims at the attribute it is written under rather than at an item
 * named by a target path, so the choice is normally narrowed down to the whole set of values, see
 * {@link com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils}.
 */
@PanelType(name = "arw-construction-mapping-range")
@PanelInstance(identifier = "arw-construction-mapping-range",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.outbound.range", icon = "fa fa-circle"),
        containerPath = "empty")
public class ConstructionOutboundMappingRangeStepPanel<AHDM extends AssignmentHolderDetailsModel>
        extends OutboundMappingRangeStepPanel<AHDM> {

    @Serial private static final long serialVersionUID = 1L;

    public static final String PANEL_TYPE = "arw-construction-mapping-range";

    public ConstructionOutboundMappingRangeStepPanel(AHDM model, IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        super(model, valueModel);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
