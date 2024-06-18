/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complextype;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "schema-complexType-basic",
        applicableForType = SchemaType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageSchema.wizard.step.complexType.basicSettings", icon = "fa fa-circle"))
public class BasicSettingComplexTypeStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ComplexTypeDefinitionType, AssignmentHolderDetailsModel<SchemaType>> {

    public static final String PANEL_TYPE = "schema-complexType-basic";

    public BasicSettingComplexTypeStepPanel(
            AssignmentHolderDetailsModel<SchemaType> model,
            IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> newValueModel) {
        super(model, newValueModel, null);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageSchema.wizard.step.complexType.basicSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageSchema.wizard.step.complexType.basicSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageSchema.wizard.step.complexType.basicSettings.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ComplexTypeDefinitionType.F_LIFECYCLE_STATE)
                    || wrapper.getItemName().equals(ComplexTypeDefinitionType.F_DISPLAY_HINT)
                    || wrapper.getItemName().equals(ComplexTypeDefinitionType.F_DISPLAY_ORDER)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }
}
