/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;

/**
 * @author lskublik
 */
@PanelType(name = "arw-focus-mapping-basic")
@PanelInstance(identifier = "arw-focus-mapping-basic",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.focusMapping.basicInformation", icon = "fa fa-wrench"),
        containerPath = "empty")
public class BasicFocusMappingStepPanel<AR extends AbstractRoleType> extends AbstractValueFormResourceWizardStepPanel<AssignmentType, FocusDetailsModels<AR>> {

    public static final String PANEL_TYPE = "arw-focus-mapping-basic";

    public BasicFocusMappingStepPanel(
            FocusDetailsModels<AR> model,
            IModel<PrismContainerValueWrapper<AssignmentType>> newValueModel) {
        super(model, newValueModel, null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
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
        return createStringResource("PageRole.wizard.step.focusMapping.basicInformation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.basicInformation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.basicInformation.subText");
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return this::checkMandatory;
    }

    private boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(AssignmentType.F_FOCUS_TYPE)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(AssignmentType.F_EFFECTIVE_MARK_REF)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    public String getStepId() {
        return super.getStepId();
    }
}
