/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-type-delineation",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.objectType.delineation", icon = "fa fa-circle"),
        expanded = true)
public class DelineationResourceObjectTypeStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ResourceObjectTypeDelineationType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-type-delineation";
    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDelineationType>> valueModel;

    public DelineationResourceObjectTypeStepPanel(ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, null, null);
        this.valueModel = createNewValueModel(newValueModel, ResourceObjectTypeDefinitionType.F_DELINEATION);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getValueModel().getObject().setShowEmpty(false);
    }

    @Override
    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDelineationType>> getValueModel() {
        return valueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.objectType.delineation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.delineation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.delineation.subText");
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return this::checkMandatory;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getPath() != null && itemWrapper.getPath().endsWith(
                ItemPath.create(
                        ResourceObjectTypeDefinitionType.F_DELINEATION,
                        ResourceObjectTypeDelineationType.F_OBJECT_CLASS))) {
            return true;
        }
        return itemWrapper.isMandatory();
    }
}
