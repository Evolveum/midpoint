/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.enumerationtype;

import com.evolveum.midpoint.gui.impl.page.admin.schema.component.EnumerationValueDefinitionsTable;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.PrismItemDefinitionsTable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "schema-enumerationType-values",
        applicableForType = SchemaType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageSchema.wizard.step.enumerationType.values", icon = "fa fa-circle"))
public abstract class EnumValueDefinitionsTableWizardPanel
        extends AbstractWizardStepPanel<AssignmentHolderDetailsModel<SchemaType>> {

    public static final String PANEL_TYPE = "schema-enumerationType-values";

    protected static final String ID_PANEL = "panel";

    private final IModel<PrismContainerValueWrapper<EnumerationTypeDefinitionType>> valueModel;

    public EnumValueDefinitionsTableWizardPanel(
            AssignmentHolderDetailsModel<SchemaType> model,
            IModel<PrismContainerValueWrapper<EnumerationTypeDefinitionType>> valueModel) {
        super(model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new EnumerationValueDefinitionsTable(ID_PANEL, getValueModel(), getContainerConfiguration(PANEL_TYPE)));
    }

    protected IModel<PrismContainerValueWrapper<EnumerationTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageSchema.wizard.step.enumerationType.values");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageSchema.wizard.step.enumerationType.values.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageSchema.wizard.step.enumerationType.values.subText");
    }

    @Override
    public String getStepId() {
        return getPanelType();
    }
}
