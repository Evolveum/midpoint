/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

import org.apache.wicket.model.IModel;

import java.util.List;

@PanelType(name = "arw-focusMapping-mapping-main")
@PanelInstance(identifier = "arw-focusMapping-mapping-main",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.focusMapping.mapping.main", icon = "fa fa-screwdriver-wrench"),
        containerPath = "empty")
public class FocusMappingMappingMainStepPanel<AHD extends AssignmentHolderDetailsModel> extends AbstractValueFormResourceWizardStepPanel<MappingType, AHD> {

    public static final String PANEL_TYPE = "arw-focusMapping-mapping-main";

    private static final List<ItemName> VISIBLE_ITEMS = List.of(
            MappingType.F_NAME,
            MappingType.F_SOURCE,
            MappingType.F_TARGET,
            MappingType.F_STRENGTH,
            MappingType.F_EXPRESSION,
            MappingType.F_CONDITION
    );

    public FocusMappingMappingMainStepPanel(AHD model,
            IModel<PrismContainerValueWrapper<MappingType>> newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected String getIcon() {
        return "fa fa-screwdriver-wrench";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.main");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.main.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.main.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (VISIBLE_ITEMS.stream().anyMatch(item -> item.equivalent(wrapper.getItemName()))) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }
}
