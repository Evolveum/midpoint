/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complextype;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.PrismItemDefinitionsTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "schema-complexType-items",
        applicableForType = SchemaType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageSchema.wizard.step.complexType.items", icon = "fa fa-circle"))
public abstract class PrismItemDefinitionsTableWizardPanel
        extends AbstractWizardStepPanel<AssignmentHolderDetailsModel<SchemaType>> {

    public static final String PANEL_TYPE = "schema-complexType-items";

    protected static final String ID_PANEL = "panel";

    private final IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> valueModel;

    public PrismItemDefinitionsTableWizardPanel(
            AssignmentHolderDetailsModel<SchemaType> model,
            IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> valueModel) {
        super(model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new PrismItemDefinitionsTable(ID_PANEL, getValueModel(), getContainerConfiguration(PANEL_TYPE)));
    }

    protected IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageSchema.wizard.step.complexType.items");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageSchema.wizard.step.complexType.items.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageSchema.wizard.step.complexType.items.subText");
    }

    @Override
    public String getStepId() {
        return getPanelType();
    }
}
