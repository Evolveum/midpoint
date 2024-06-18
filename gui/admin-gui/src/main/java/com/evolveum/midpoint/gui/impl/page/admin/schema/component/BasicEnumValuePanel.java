/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationValueTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "schema-enumerationType-value-basic",
        applicableForType = SchemaType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageSchema.wizard.step.enumerationType.value.basicSettings", icon = "fa fa-circle"))
public class BasicEnumValuePanel extends BasicDefinitionPanel<EnumerationValueTypeDefinitionType> {

    public static final String PANEL_TYPE = "schema-enumerationType-value-basic";

    public BasicEnumValuePanel(
            AssignmentHolderDetailsModel<SchemaType> model,
            IModel<PrismContainerValueWrapper<EnumerationValueTypeDefinitionType>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageSchema.wizard.step.enumerationType.value.basicSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageSchema.wizard.step.enumerationType.value.basicSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageSchema.wizard.step.enumerationType.value.basicSettings.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(EnumerationValueTypeDefinitionType.F_CONSTANT_NAME)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }
}
