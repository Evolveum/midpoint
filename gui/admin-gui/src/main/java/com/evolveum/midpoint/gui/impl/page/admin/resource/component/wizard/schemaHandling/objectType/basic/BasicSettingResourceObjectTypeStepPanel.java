/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-type-basic",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.objectType.basicSettings", icon = "fa fa-circle"))
public class BasicSettingResourceObjectTypeStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-type-basic";

    public BasicSettingResourceObjectTypeStepPanel(
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, newValueModel, null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        applyDefaultIfRequired(getValueModel().getObject());
    }

    /**
     * Ensures that a newly added {@link ResourceObjectTypeDefinitionType} is marked as default
     * if no other object type of the same {@link ShadowKindType} is already set as default.
     * <p>
     * This logic is only applied when reusing a value that already has a {@code kind}
     * (e.g. when created from object type suggestions). If a new object type is created
     * without a kind, this process is skipped.
     *
     * @param valueWrapper the container value wrapper for the resource object type
     */
    private void applyDefaultIfRequired(PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> valueWrapper) {
        if (valueWrapper == null
                || valueWrapper.getRealValue() == null
                || valueWrapper.getStatus() != ValueStatus.ADDED) {
            return;
        }

        ResourceObjectTypeDefinitionType current = valueWrapper.getRealValue();

        if (current.getKind() != null) {
            ResourceType resource = getDetailsModel().getObjectWrapper().getObject().asObjectable();
            boolean defaultAlreadyPresent = resource.getSchemaHandling() != null &&
                    resource.getSchemaHandling().getObjectType().stream()
                            .anyMatch(ot ->
                                    Boolean.TRUE.equals(ot.isDefault()) &&
                                            current.getKind().equals(ot.getKind()));

            if (!defaultAlreadyPresent) {
                try {
                    PrismPropertyWrapper<Boolean> property = valueWrapper.findProperty(ResourceObjectTypeDefinitionType.F_DEFAULT);
                    property.getValue().setRealValue(true);
                } catch (SchemaException e) {
                    throw new RuntimeException("Couldn't set default to resource object type.", e);
                }
            }
        }
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.objectType.basicSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.basicSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.basicSettings.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return this::checkMandatory;
    }

    protected boolean checkMandatory(@NotNull ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper.getItemName().equals(ResourceObjectTypeDefinitionType.F_KIND)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }
}
