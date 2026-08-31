/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import java.io.Serial;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Step configuring the range of an outbound mapping.
 *
 * <p>An outbound attribute mapping aims at the attribute it is written under rather than at an item
 * named by a target path, so the choice is normally narrowed down to the whole set of values, see
 * {@link com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils}.
 *
 * @author jjarabinec
 */
@PanelInstance(identifier = "rw-attributes-outbound-range",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.outbound.range", icon = "fa fa-filter"),
        expanded = true)
public class OutboundMappingRangeStepPanel<AHDM extends AssignmentHolderDetailsModel>
        extends AbstractWizardStepPanel<AHDM> {

    @Serial private static final long serialVersionUID = 1L;

    public static final String PANEL_TYPE = "rw-attributes-outbound-range";

    private static final String ID_RANGE = "range";

    private final IModel<PrismContainerValueWrapper<MappingType>> valueModel;

    public OutboundMappingRangeStepPanel(AHDM model, IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        super(model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new MappingRangePanel(ID_RANGE, valueModel));
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.attributes.outbound.range");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.outbound.range.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.outbound.range.subText");
    }
}
