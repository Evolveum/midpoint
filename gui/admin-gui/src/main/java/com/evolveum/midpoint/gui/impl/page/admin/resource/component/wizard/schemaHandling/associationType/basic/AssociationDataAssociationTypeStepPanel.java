/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.LinkedReferencePanelFactory;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-association-data",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.associationType.data", icon = "fa fa-circle"),
        expanded = true)
public class AssociationDataAssociationTypeStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationDataAssociationTypeStepPanel.class);

    public static final String PANEL_TYPE = "rw-association-data";

    private final IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel;

    public AssociationDataAssociationTypeStepPanel(ResourceDetailsModel model,
                                            IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> newValueModel) {
        super(model, null, null);
        this.valueModel = createNewValueModel(
                newValueModel,
                ItemPath.create(
                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (valueModel.getObject() != null) {
            valueModel.getObject().setExpanded(true);
        }
    }

    @Override
    public IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.associationType.data");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.data.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.data.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ShadowAssociationDefinitionType.F_SOURCE_ATTRIBUTE_REF)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }
}
