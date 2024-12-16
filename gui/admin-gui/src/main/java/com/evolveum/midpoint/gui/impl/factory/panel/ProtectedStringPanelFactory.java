/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.password.ProtectedStringPanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

/***
 * Panel factory for protected strings.
 * Panel contains two fields for clear password and allow configuration of secret provider only when is already configured.
 */
@Component
public class ProtectedStringPanelFactory implements Serializable, GuiComponentFactory<PrismPropertyPanelContext<ProtectedStringType>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        QName type = wrapper.getTypeName();
        return ProtectedStringType.COMPLEX_TYPE.equals(type);
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<ProtectedStringType> panelCtx) {
        boolean useGlobalValuePolicy = useGlobalValuePolicy(panelCtx.getItemWrapperModel());
        ProtectedStringPanel panel = new ProtectedStringPanel(
                panelCtx.getComponentId(),
                (IModel<PrismPropertyValueWrapper<ProtectedStringType>>) panelCtx.getValueWrapperModel(),
                showProviderPanel(panelCtx.getRealValueModel()),
                isShowedOneLinePasswordPanel(),
                useGlobalValuePolicy);
        panel.setFeedback(panelCtx.getFeedback());
        panel.setOutputMarkupId(true);
        return panel;
    }

    protected boolean isShowedOneLinePasswordPanel() {
        return false;
    }

    protected boolean showProviderPanel(ItemRealValueModel<ProtectedStringType> realValueModel) {
        ProtectedStringType bean = realValueModel.getObject();
        if (bean == null) {
            return false;
        }

        if (bean.getExternalData() == null) {
            return false;
        }

        if (bean.getExternalData().isEmpty()) {
            return false;
        }

        return true;
    }

    private boolean useGlobalValuePolicy(IModel<PrismPropertyWrapper<ProtectedStringType>> wrapperModel) {
        ItemPath itemPath = wrapperModel == null || wrapperModel.getObject() == null ? null : wrapperModel.getObject().getPath();
        return itemPath == null || !itemPath.startsWith(ItemPath.create(ObjectType.F_EXTENSION));
    }

    @Override
    public Integer getOrder() {
        return 800;
    }

    @Override
    public void configure(PrismPropertyPanelContext<ProtectedStringType> panelCtx, org.apache.wicket.Component component) {
    }
}
