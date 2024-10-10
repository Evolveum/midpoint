/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.validator.ResourceObjectFocusTypeValidator;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katkav
 */
@Component
public class ResourceObjectFocusTypePanelFactory extends DropDownChoicePanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }
        if (wrapper.getPath() == null) {
            return false;
        }

        return ItemPath.create(
                        ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_FOCUS,
                        ResourceObjectFocusSpecificationType.F_TYPE)
                .equivalent(wrapper.getPath().namedSegmentsOnly());
    }

    protected List<QName> getTypesList(PrismPropertyPanelContext<QName> panelCtx) {
        return ObjectTypeListUtil.createFocusTypeList();
    }

    @Override
    public void configure(PrismPropertyPanelContext<QName> panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);
        InputPanel panel = (InputPanel) component;
        panel.getValidatableComponent().add(new ResourceObjectFocusTypeValidator(panelCtx.getItemWrapperModel()));
    }

    @Override
    public Integer getOrder() {
        return 9999;
    }

}
