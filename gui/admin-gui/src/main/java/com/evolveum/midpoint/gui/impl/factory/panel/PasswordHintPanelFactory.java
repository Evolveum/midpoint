/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.password.PasswordHintPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class PasswordHintPanelFactory extends AbstractGuiComponentFactory<String> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new PasswordHintPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(), getPasswordModel(panelCtx.getItemWrapperModel()),
                    panelCtx.unwrapWrapperModel().isReadOnly());
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return PasswordType.F_HINT.matches(wrapper.getItemName());
    }

    private static LoadableModel<ProtectedStringType> getPasswordModel(IModel<PrismPropertyWrapper<String>> hintValueWrapper) {
        return new LoadableModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected ProtectedStringType load() {
                if (hintValueWrapper == null) {
                    return null;
                }
                PrismContainerValueWrapper<PasswordType> passwordContainer = hintValueWrapper.getObject().getParentContainerValue((PasswordType.class));
                try {
                    PrismPropertyWrapper<ProtectedStringType> passwordWrapper = passwordContainer.findProperty(PasswordType.F_VALUE);
                    return passwordWrapper != null ? passwordWrapper.getValue().getRealValue() : null;
                } catch (SchemaException e) {
                    //nothing to do here
                }
                return null;
            }
        };
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 10000;
    }
}
