/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingRangeStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * Step configuring the range of a focus mapping, telling which of the existing target values the
 * mapping is in charge of.
 *
 * @author jjarabinec
 */
@PanelType(name = "arw-focusMapping-mapping-range")
@PanelInstance(identifier = "arw-focusMapping-mapping-range",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.focusMapping.mapping.range", icon = "fa fa-filter"),
        containerPath = "empty")
public class FocusMappingMappingRangeStepPanel<AHD extends AssignmentHolderDetailsModel> extends OutboundMappingRangeStepPanel<AHD> {

    public static final String PANEL_TYPE = "arw-focusMapping-mapping-range";

    public FocusMappingMappingRangeStepPanel(AHD model, IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        super(model, valueModel);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.range");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.range.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.range.subText");
    }
}
