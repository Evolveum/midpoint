/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.component.form.CreateObjectForReferencePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.CreateObjectForReferenceValueWrapper;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.Serializable;

/**
 * Factory for reference values that support creating of new object.
 * Used for ResourceType/schemaHandling/objectType/focus/archetypeRef.
 */
@Component
public class CreateObjectForReferencePanelFactory
        implements GuiComponentFactory<PrismReferencePanelContext<ObjectReferenceType>>, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(CreateObjectForReferencePanelFactory.class);

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 999;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return valueWrapper instanceof CreateObjectForReferenceValueWrapper<?>;
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismReferencePanelContext<ObjectReferenceType> panelCtx) {
        CreateObjectForReferencePanel panel = new CreateObjectForReferencePanel(
                panelCtx.getComponentId(),
                panelCtx.getValueWrapperModel(),
                createContainerConfiguration(panelCtx.getValueWrapperModel().getObject())) {

            protected boolean isHeaderOfCreateObjectVisible() {
                if (panelCtx.getValueWrapperModel().getObject() instanceof CreateObjectForReferenceValueWrapper<?> createObjectForReferenceWrapper) {
                    return createObjectForReferenceWrapper.isHeaderOfCreateObjectVisible();
                }
                return false;
            }
        };

        panel.setFeedback(panelCtx.getFeedback());
        panel.setOutputMarkupId(true);
        return panel;
    }
    private ContainerPanelConfigurationType createContainerConfiguration(PrismValueWrapper<ObjectReferenceType> valueWrapper) {

        if (valueWrapper instanceof CreateObjectForReferenceValueWrapper<?> createObjectForReferenceWrapper) {
            return createObjectForReferenceWrapper.createContainerConfiguration();
        }

        return new ContainerPanelConfigurationType();
    }


}
