/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.web.component.prism.ValueStatus;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;

@Component
public class LockoutStatusPanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<LockoutStatusType>> {

    @Autowired GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return wrapper instanceof PrismPropertyWrapper && ActivationType.F_LOCKOUT_STATUS.equals(wrapper.getItemName());
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<LockoutStatusType> panelCtx) {
        LockoutStatusPanel panel = new LockoutStatusPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void lockoutStatusResetPerformed(boolean resetToInitialState) {
                //todo hack to fix 9856: when lockout status is reset to Normal, also reset lockout expiration timestamp
                PrismPropertyWrapper<LockoutStatusType> lockoutStatusPW = panelCtx.unwrapWrapperModel();
                PrismContainerValueWrapper<?> activationVW = lockoutStatusPW.getParent();
                try {
                    PrismPropertyWrapper<XMLGregorianCalendar> lockoutExpirationPW =
                            activationVW.findProperty(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
                    if (lockoutExpirationPW == null) {
                        return;
                    }
                    PrismPropertyValueWrapper<XMLGregorianCalendar> value = lockoutExpirationPW.getValue();
                    if (value == null) {
                        return;
                    }
                    if (resetToInitialState) {
                        XMLGregorianCalendar oldValue = value.getOldValue() != null ? value.getOldValue().getValue() : null;
                        value.setRealValue(oldValue);
                        value.setStatus(ValueStatus.NOT_CHANGED);
                        lockoutStatusPW.getValue().setStatus(ValueStatus.NOT_CHANGED);
                    } else {
                        value.setRealValue(null);
                        value.setStatus(ValueStatus.MODIFIED);
                    }
                } catch (SchemaException e) {
                    //nothing to do
                }
            }
        };
        return panel;
    }
}
