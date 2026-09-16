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
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Step configuring the range of an inbound mapping, telling which of the existing target values the
 * mapping is in charge of.
 *
 * <p>The range lives in the set of the mapping target, which is a bean inside a property rather than
 * a container of its own. It therefore cannot be rendered by the usual value form of the other steps
 * and the step brings its own panel instead.
 *
 * @author jjarabinec
 */
@PanelInstance(identifier = "rw-attributes-inbound-range",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.inbound.range", icon = "fa fa-filter"),
        expanded = true)
public class InboundMappingRangeStepPanel extends AbstractWizardStepPanel<ResourceDetailsModel> {

    @Serial private static final long serialVersionUID = 1L;

    public static final String PANEL_TYPE = "rw-attributes-inbound-range";

    private static final String ID_RANGE = "range";

    private final IModel<PrismContainerValueWrapper<MappingType>> valueModel;

    public InboundMappingRangeStepPanel(
            ResourceDetailsModel model, IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
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
        return createStringResource("PageResource.wizard.step.attributes.inbound.range");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.inbound.range.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.inbound.range.subText");
    }
}
