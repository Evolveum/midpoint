/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType.*;

/**
 * @author lskublik
 */
@PanelType(name = "basicResourceObjectTypeWizard")
@PanelInstance(identifier = "basicResourceObjectTypeWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.basicResourceObjectTypeWizard", icon = "fa fa-circle"))
public class BasicSettingResourceObjectTypeStepPanel extends AbstractResourceWizardStepPanel {

    private static final String ID_HEADER = "header";
    private static final String ID_VALUE = "value";
    private static final List<ItemName> VISIBLE_ITEMS = List.of(
            F_DISPLAY_NAME,
            F_INTENT,
            F_KIND,
            F_DESCRIPTION,
            F_OBJECT_CLASS,
            F_SECURITY_POLICY_REF,
            F_DEFAULT);

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel;

    public BasicSettingResourceObjectTypeStepPanel(ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model);
        this.newValueModel = newValueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        VerticalFormContainerHeaderPanel header = new VerticalFormContainerHeaderPanel(ID_HEADER, getTitle()) {
            @Override
            protected String getIcon() {
                return BasicSettingResourceObjectTypeStepPanel.this.getIcon();
            }
        };
        add(header);

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler()).build();
        VerticalFormDefaultContainerablePanel<ResourceObjectTypeDefinitionType> panel
                = new VerticalFormDefaultContainerablePanel<ResourceObjectTypeDefinitionType>(ID_VALUE, newValueModel, settings);
        add(panel);
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (VISIBLE_ITEMS.contains(wrapper.getItemName())) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }

    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.basicSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.basicSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.basicSettings.subText");
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        getValuePanel().visitChildren(VerticalFormPrismPropertyValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismPropertyValuePanel) component).updateFeedbackPanel(target);
        });
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected VisibleBehaviour getExitVisibility() {
        return new VisibleBehaviour(() -> true);
    }

    private VerticalFormDefaultContainerablePanel getValuePanel() {
        return (VerticalFormDefaultContainerablePanel) get(ID_VALUE);
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return newValueModel;
    }
}
