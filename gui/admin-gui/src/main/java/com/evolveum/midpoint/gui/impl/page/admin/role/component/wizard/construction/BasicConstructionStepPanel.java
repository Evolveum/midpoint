/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "roleWizard-construction-basic")
@PanelInstance(identifier = "roleWizard-construction-basic",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.basic", icon = "fa fa-wrench"),
        containerPath = "empty")
public class BasicConstructionStepPanel extends AbstractValueFormResourceWizardStepPanel<ConstructionType, FocusDetailsModels<RoleType>> {

    private static final String PANEL_TYPE = "roleWizard-construction-basic";

    private final IModel<PrismContainerValueWrapper<ConstructionType>> valueModel;

    public BasicConstructionStepPanel(
            FocusDetailsModels<RoleType> model, IModel<PrismContainerValueWrapper<AssignmentType>> newValueModel) {
        super(model, null);
        valueModel = createNewValueModel(newValueModel, AssignmentType.F_CONSTRUCTION);
    }

    @Override
    public IModel<PrismContainerValueWrapper<ConstructionType>> getValueModel() {
        return valueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.construction.basic");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.basic.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.basic.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ConstructionType.F_RESOURCE_REF)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ConstructionType.F_KIND)) {
                return true;
            }
            return false;
        };
    }
}