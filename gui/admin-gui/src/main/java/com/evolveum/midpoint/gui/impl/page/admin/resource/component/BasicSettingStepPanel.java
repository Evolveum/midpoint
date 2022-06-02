/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lskublik
 */
public class BasicSettingStepPanel extends BasicWizardPanel {

    private final IModel<PrismObjectWrapper<ResourceType>> resourceModel;

    public BasicSettingStepPanel(IModel<PrismObjectWrapper<ResourceType>> model) {
        this.resourceModel = model;
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
    protected Component createContentPanel(String id) {
        return new QuickFormPanel(id, () -> this.resourceModel.getObject().getValue()) {
            @Override
            protected String getIcon() {
                return "fa fa-wrench";
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getPageBase().createStringResource("PageResource.wizard.step.basicSettings");
            }

            @Override
            protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
                if(itemWrapper.getItemName().equals(ResourceType.F_NAME)
                        || itemWrapper.getItemName().equals(ResourceType.F_DESCRIPTION)) {
                    return ItemVisibility.AUTO;
                }
                return ItemVisibility.HIDDEN;
            }
        };
    }
}
